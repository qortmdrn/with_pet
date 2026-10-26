package com.withpet.car.ui

import com.withpet.car.CITY_HALL
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.Hospital
import com.withpet.car.domain.repository.HospitalRepository
import com.withpet.car.domain.usecase.CallHospitalAndNavigateUseCase
import com.withpet.car.domain.usecase.FindEmergencyHospitalsUseCase
import com.withpet.car.hospital
import com.withpet.car.north
import com.withpet.car.platform.location.LocationProvider
import com.withpet.car.platform.navigation.NavigationController
import com.withpet.car.platform.phone.PhoneDialer
import com.withpet.car.ui.emergency.EmergencyListState
import com.withpet.car.ui.emergency.EmergencyViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val navigation = mockk<NavigationController> {
        coEvery { estimateEtaMinutes(any()) } returns Result.failure(IllegalStateException())
        coEvery { startGuidance(any(), any()) } returns Result.success(Unit)
    }
    private val dialer = mockk<PhoneDialer> {
        every { isCallCapable() } returns flowOf(true)
        every { opensDialerOnly } returns false
        coEvery { call(any()) } returns Result.success(Unit)
    }
    private val location = mockk<LocationProvider> {
        every { currentLocation() } returns flowOf(CITY_HALL)
    }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(vararg hospitals: Hospital): EmergencyViewModel {
        val repo = object : HospitalRepository {
            override suspend fun getAll() = hospitals.toList()
        }
        return EmergencyViewModel(
            location,
            FindEmergencyHospitalsUseCase(repo, navigation, Clock.systemDefaultZone()),
            CallHospitalAndNavigateUseCase(dialer, navigation),
        )
    }

    @Test
    fun `진료 중 병원이 있으면 목록 없이 가장 빨리 도착하는 곳으로 바로 안내한다`() = runTest(dispatcher) {
        val near = hospital("near", CITY_HALL.north(2.0), is24Hours = true)
        val far = hospital("far", CITY_HALL.north(8.0), is24Hours = true)
        val vm = viewModel(far, near)
        advanceUntilIdle()

        val selection = vm.state.value.selection!!
        assertEquals(near, selection.candidate.hospital)
        assertTrue(selection.auto)
        assertEquals(true, selection.result?.guidanceStarted)
        coVerify(exactly = 1) { navigation.startGuidance(near.location, near.name) }
        coVerify(exactly = 1) { dialer.call(near.phone!!) }
    }

    @Test
    fun `진료 시간 미확인 병원뿐이면 자동으로 고르지 않고 목록을 보여준다`() = runTest(dispatcher) {
        val vm = viewModel(hospital("unknown", CITY_HALL.north(2.0)))
        advanceUntilIdle()

        assertNull(vm.state.value.selection)
        assertTrue(vm.state.value.list is EmergencyListState.Loaded)
        coVerify(exactly = 0) { navigation.startGuidance(any<GeoPoint>(), any()) }
    }

    @Test
    fun `다른 병원 보기를 누르면 목록으로 돌아가고 다시 자동 선택하지 않는다`() = runTest(dispatcher) {
        val vm = viewModel(hospital("a", CITY_HALL.north(2.0), is24Hours = true), hospital("b", CITY_HALL.north(4.0), is24Hours = true))
        advanceUntilIdle()

        vm.clearSelection()
        advanceUntilIdle()

        assertNull(vm.state.value.selection)
        coVerify(exactly = 1) { navigation.startGuidance(any(), any()) }
    }
}
