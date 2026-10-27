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
    /** 펫 모드를 껐지만 펫 모드가 연 뒷창문이 아직 열려 있음. 정차 중 [PetModeManager.closeRearWindow]로 닫는다. */
    val rearWindowLeftOpen: Boolean = false,
)

/** 펫 모드 ON/OFF 상태와 창문 복원값을 보관한다. 홈 토글과 음성 명령이 함께 쓴다. */
@Singleton
class PetModeManager @Inject constructor(
    private val startPetMode: StartPetModeUseCase,
    private val stopPetMode: StopPetModeUseCase,
    private val restoreRearWindow: RestoreRearWindowUseCase,
    private val vehicle: VehicleControl,
    private val settingsRepository: SettingsRepository,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(PetModeState())
    val state: StateFlow<PetModeState> = _state.asStateFlow()

    private var windowBeforePetMode: Int? = null
    private var lockBeforePetMode: Boolean? = null
    private var windowOpenedByPetMode = false

    suspend fun turnOn() = mutex.withLock {
        if (_state.value.isOn) return@withLock
        // 지난 펫 모드가 연 창문이 아직 열려 있으면 그 전 값을 그대로 복원 기준으로 둔다
        if (!windowOpenedByPetMode) {
            windowBeforePetMode = withTimeoutOrNull(READ_TIMEOUT_MS) {
                vehicle.rearWindowOpenPercent().firstOrNull()
            }
            lockBeforePetMode = withTimeoutOrNull(READ_TIMEOUT_MS) { vehicle.rearWindowLocked() }
        }
        val report = startPetMode(settingsRepository.petModeSettings().first())
        if (report.steps.any { it.step == PetModeStep.WINDOW && it.result.isSuccess }) windowOpenedByPetMode = true
        _state.value = PetModeState(isOn = true, lastReport = report.summary(), pendingSafetyCheck = true)
    }

    suspend fun turnOff() = mutex.withLock {
        if (!_state.value.isOn) return@withLock
        val report = stopPetMode()
        val summary = report.summary() + if (windowOpenedByPetMode) " · 뒷창문은 열려 있어요" else ""
        _state.value = PetModeState(isOn = false, lastReport = summary, rearWindowLeftOpen = windowOpenedByPetMode)
    }

    /** 펫 모드 OFF 뒤 남은 뒷창문을 진입 전 값으로 되돌린다. 사용자가 반려동물 위치를 확인하고 누른다. */
    suspend fun closeRearWindow() = mutex.withLock {
        if (!_state.value.rearWindowLeftOpen) return@withLock
        val result = restoreRearWindow(windowBeforePetMode, lockBeforePetMode)
        if (result.isSuccess) {
            val message = if ((windowBeforePetMode ?: 0) > 0) "뒷창문을 원래 위치로 되돌렸어요" else "뒷창문을 닫았어요"
            windowOpenedByPetMode = false
            windowBeforePetMode = null
            lockBeforePetMode = null
            _state.value = PetModeState(isOn = false, lastReport = message)
        } else {
            _state.update { it.copy(lastReport = "뒷창문을 닫지 못했어요") }
        }
    }

    fun acknowledgeSafetyCheck() {
        _state.update { it.copy(pendingSafetyCheck = false) }
    }

    private companion object {
        const val READ_TIMEOUT_MS = 500L
    }
}
