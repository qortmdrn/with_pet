package com.withpet.car.domain.usecase

import com.withpet.car.domain.model.CalmPlaybackState
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.model.PetModeSettings
import com.withpet.car.domain.model.RestAlertSettings
import com.withpet.car.domain.model.UnsupportedFeature
import com.withpet.car.domain.repository.CalmSoundPlayer
import com.withpet.car.domain.repository.SettingsRepository
import com.withpet.car.platform.audio.MediaVolumeControl
import com.withpet.car.platform.vehicle.VehicleControl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PetModeManagerTest {

    private class Vehicle(var windowSupported: Boolean = true, var reportWindow: Boolean = true) : VehicleControl {
        var temp: Float? = 25f
        var fan = 1
        val window = MutableStateFlow<Int?>(5)
        val windowCalls = mutableListOf<Int>()

        override suspend fun setCabinTemperature(celsius: Float) = Result.success(Unit).also { temp = celsius }
        override suspend fun setFanLevel(level: Int) = Result.success(Unit).also { fan = level }
        override suspend fun setRearWindowVentilation(openPercent: Int): Result<Unit> {
            if (!windowSupported) return Result.failure(UnsupportedFeature("no"))
            windowCalls += openPercent
            window.value = openPercent
            return Result.success(Unit)
        }
        override fun cabinTemperature(): Flow<Float?> = flowOf(temp)
        override fun rearWindowOpenPercent(): Flow<Int?> = if (reportWindow) window else flowOf(null)
    }

    private class Volume : MediaVolumeControl {
        var volume: Int? = null
        override suspend fun setMediaVolumePercent(percent: Int) = Result.success(Unit).also { volume = percent }
    }

    private class Player : CalmSoundPlayer {
        var playing: CalmSituation? = null
        var stopped = false
        override val state: StateFlow<CalmPlaybackState> = MutableStateFlow(CalmPlaybackState())
        override suspend fun play(situation: CalmSituation) = Result.success(Unit).also { playing = situation }
        override suspend fun stop() { stopped = true; playing = null }
    }

    private class Settings(private val petMode: PetModeSettings) : SettingsRepository {
        override fun petModeSettings() = flowOf(petMode)
        override fun restAlertSettings() = flowOf(RestAlertSettings())
        override suspend fun updatePetModeSettings(settings: PetModeSettings) = Unit
        override suspend fun updateRestAlertSettings(settings: RestAlertSettings) = Unit
        override fun lastCalmSituation() = flowOf<CalmSituation?>(null)
        override suspend fun setLastCalmSituation(situation: CalmSituation) = Unit
    }

    private val vehicle = Vehicle()
    private val volume = Volume()
    private val player = Player()

    private fun manager(settings: PetModeSettings = PetModeSettings()) = PetModeManager(
        StartPetModeUseCase(vehicle, volume, player),
        StopPetModeUseCase(vehicle, player),
        vehicle,
        Settings(settings),
    )

    @Test
    fun `ON이면 설정값대로 공조·창문·볼륨·사운드를 실행하고 안전 체크를 요청한다`() = runTest {
        val m = manager(PetModeSettings(targetTempC = 21.5f, fanLevel = 3, rearWindowOpenPercent = 15, mediaVolumePercent = 25))
        m.turnOn()

        assertEquals(21.5f, vehicle.temp)
        assertEquals(3, vehicle.fan)
        assertEquals(15, vehicle.window.value)
        assertEquals(25, volume.volume)
        assertEquals(CalmSituation.DEPARTURE, player.playing)
        assertTrue(m.state.value.isOn)
        assertTrue(m.state.value.pendingSafetyCheck)
        assertEquals("펫 모드를 켰어요", m.state.value.lastReport)
    }

    @Test
    fun `자동 재생이 꺼져 있으면 사운드를 틀지 않는다`() = runTest {
        manager(PetModeSettings(autoPlayCalmSound = false)).turnOn()
        assertNull(player.playing)
    }

    @Test
    fun `OFF이면 창문을 진입 전 값으로 복원하고 사운드를 멈추며 공조는 그대로 둔다`() = runTest {
        val m = manager()
        m.turnOn()
        m.turnOff()

        assertEquals(listOf(10, 5), vehicle.windowCalls)
        assertEquals(5, vehicle.window.value)
        assertTrue(player.stopped)
        assertEquals(22f, vehicle.temp)
        assertFalse(m.state.value.isOn)
        assertFalse(m.state.value.pendingSafetyCheck)
    }

    @Test
    fun `진입 전 창문 값을 모르면 닫는다`() = runTest {
        vehicle.reportWindow = false
        val m = manager()
        m.turnOn()
        m.turnOff()
        assertEquals(listOf(10, 0), vehicle.windowCalls)
    }

    @Test
    fun `지원하지 않는 기능은 건너뛰고 한 줄로 안내한다`() = runTest {
        vehicle.windowSupported = false
        val m = manager()
        m.turnOn()

        assertTrue(m.state.value.isOn)
        assertEquals(22f, vehicle.temp)
        assertEquals(30, volume.volume)
        assertEquals("펫 모드를 켰어요 · 이 차량에서는 창문 자동 조절을 지원하지 않아요", m.state.value.lastReport)
    }

    @Test
    fun `이미 켜져 있으면 다시 실행하지 않는다`() = runTest {
        val m = manager()
        m.turnOn()
        m.acknowledgeSafetyCheck()
        m.turnOn()
        assertEquals(listOf(10), vehicle.windowCalls)
        assertFalse(m.state.value.pendingSafetyCheck)
    }
}
