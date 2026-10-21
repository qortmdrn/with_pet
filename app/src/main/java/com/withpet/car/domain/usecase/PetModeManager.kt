package com.withpet.car.domain.usecase

import com.withpet.car.domain.repository.SettingsRepository
import com.withpet.car.platform.vehicle.VehicleControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class PetModeState(
    val isOn: Boolean = false,
    /** 마지막 ON/OFF 결과 한 줄 요약 */
    val lastReport: String? = null,
    /** ON 직후 안전 탑승 체크(F6) 안내를 아직 확인하지 않음 */
    val pendingSafetyCheck: Boolean = false,
)

/** 펫 모드 ON/OFF 상태와 창문 복원값을 보관한다. 홈 토글과 음성 명령이 함께 쓴다. */
@Singleton
class PetModeManager @Inject constructor(
    private val startPetMode: StartPetModeUseCase,
    private val stopPetMode: StopPetModeUseCase,
    private val vehicle: VehicleControl,
    private val settingsRepository: SettingsRepository,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(PetModeState())
    val state: StateFlow<PetModeState> = _state.asStateFlow()

    private var windowBeforePetMode: Int? = null

    suspend fun turnOn() = mutex.withLock {
        if (_state.value.isOn) return@withLock
        windowBeforePetMode = withTimeoutOrNull(READ_TIMEOUT_MS) {
            vehicle.rearWindowOpenPercent().firstOrNull()
        }
        val report = startPetMode(settingsRepository.petModeSettings().first())
        _state.value = PetModeState(isOn = true, lastReport = report.summary(), pendingSafetyCheck = true)
    }

    suspend fun turnOff() = mutex.withLock {
        if (!_state.value.isOn) return@withLock
        val report = stopPetMode(windowBeforePetMode)
        windowBeforePetMode = null
        _state.value = PetModeState(isOn = false, lastReport = report.summary())
    }

    fun acknowledgeSafetyCheck() {
        _state.update { it.copy(pendingSafetyCheck = false) }
    }

    private companion object {
        const val READ_TIMEOUT_MS = 500L
    }
}
