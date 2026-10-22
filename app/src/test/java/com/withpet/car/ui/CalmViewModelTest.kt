package com.withpet.car.ui

import com.withpet.car.domain.model.CalmPlaybackState
import com.withpet.car.domain.model.CalmPlaylist
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.model.DrivingState
import com.withpet.car.domain.model.PetModeSettings
import com.withpet.car.domain.model.RestAlertSettings
import com.withpet.car.domain.repository.CalmSoundPlayer
import com.withpet.car.domain.repository.PlaylistRepository
import com.withpet.car.domain.repository.SettingsRepository
import com.withpet.car.domain.usecase.DrivingRestrictionMonitor
import com.withpet.car.platform.driving.DrivingStateProvider
import com.withpet.car.platform.driving.UxRestrictionsSource
import com.withpet.car.ui.calm.CalmViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalmViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class Settings(initial: CalmSituation?) : SettingsRepository {
        val last = MutableStateFlow(initial)
        override fun petModeSettings() = flowOf(PetModeSettings())
        override fun restAlertSettings() = flowOf(RestAlertSettings())
        override suspend fun updatePetModeSettings(settings: PetModeSettings) = Unit
        override suspend fun updateRestAlertSettings(settings: RestAlertSettings) = Unit
        override fun lastCalmSituation() = last
        override suspend fun setLastCalmSituation(situation: CalmSituation) {
            last.value = situation
        }
    }

    private class Player : CalmSoundPlayer {
        override val state: StateFlow<CalmPlaybackState> = MutableStateFlow(CalmPlaybackState())
        val played = mutableListOf<CalmSituation>()
        override suspend fun play(situation: CalmSituation) = Result.success(Unit).also { played += situation }
        override suspend fun stop() = Unit
    }

    private object Playlists : PlaylistRepository {
        override suspend fun getAll() = emptyList<CalmPlaylist>()
        override suspend fun get(situation: CalmSituation): CalmPlaylist? = null
    }

    private fun restriction(scope: TestScope) = DrivingRestrictionMonitor(
        object : DrivingStateProvider {
            override fun state() = flowOf(DrivingState.PARKED)
            override fun continuousDrivingMinutes() = flowOf(0)
        },
        object : UxRestrictionsSource {
            override fun requiresDistractionOptimization() = flowOf(false)
        },
        scope.backgroundScope,
    )

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `저장된 마지막 상황을 기본 선택으로 보여 준다`() = runTest(dispatcher) {
        val vm = CalmViewModel(Playlists, restriction(this), Player(), Settings(CalmSituation.RETURN_HOME))
        backgroundScope.launchCollect(vm)
        advanceUntilIdle()
        assertEquals(CalmSituation.RETURN_HOME, vm.state.value.selected)
    }

    @Test
    fun `저장된 값이 없으면 출발을 기본으로 한다`() = runTest(dispatcher) {
        val vm = CalmViewModel(Playlists, restriction(this), Player(), Settings(null))
        backgroundScope.launchCollect(vm)
        advanceUntilIdle()
        assertEquals(CalmSituation.DEPARTURE, vm.state.value.selected)
    }

    @Test
    fun `상황을 고르면 재생하고 마지막 상황으로 저장한다`() = runTest(dispatcher) {
        val settings = Settings(CalmSituation.DEPARTURE)
        val player = Player()
        val vm = CalmViewModel(Playlists, restriction(this), player, settings)
        backgroundScope.launchCollect(vm)

        vm.play(CalmSituation.TO_HOSPITAL)
        advanceUntilIdle()

        assertEquals(listOf(CalmSituation.TO_HOSPITAL), player.played)
        assertEquals(CalmSituation.TO_HOSPITAL, settings.last.value)
        assertEquals(CalmSituation.TO_HOSPITAL, vm.state.value.selected)
    }

    // state는 구독 중일 때만 갱신된다 (WhileSubscribed)
    private fun CoroutineScope.launchCollect(vm: CalmViewModel) = launch { vm.state.collect {} }
}
