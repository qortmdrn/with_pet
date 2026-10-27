package com.withpet.car.domain.usecase

import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.model.PetModeSettings
import com.withpet.car.domain.model.StepSkipped
import com.withpet.car.domain.model.UnsupportedFeature
import com.withpet.car.domain.repository.CalmSoundPlayer
import com.withpet.car.platform.audio.MediaVolumeControl
import com.withpet.car.platform.vehicle.VehicleControl
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

enum class PetModeStep(val label: String) {
    TEMPERATURE("온도 조절"),
    FAN("풍량 조절"),
    WINDOW("창문 자동 조절"),
    VOLUME("안정 사운드 볼륨"),
    CALM_SOUND("안정 사운드 재생"),
}

data class PetModeStepResult(val step: PetModeStep, val result: Result<Unit>)

data class PetModeReport(val title: String, val steps: List<PetModeStepResult>) {

    val failures: List<PetModeStepResult> get() = steps.filter { it.result.isFailure }

    /** 결과를 한 줄로 요약한다. 예: "펫 모드를 켰어요 · 이 차량에서는 창문 자동 조절을 지원하지 않아요" */
    fun summary(): String {
        val failures = failures
        if (failures.isEmpty()) return title
        val first = failures.first().let { (step, result) ->
            when (val error = result.exceptionOrNull()) {
                is StepSkipped -> error.message
                is UnsupportedFeature -> "이 차량에서는 ${step.label}을 지원하지 않아요"
                else -> "${step.label}에 실패했어요"
            }
        }
        val more = if (failures.size > 1) " 외 ${failures.size - 1}건" else ""
        return "$title · $first$more"
    }
}

/** 펫 모드 ON (명세 7-6). 각 단계 실패는 무시하고 결과만 모은다. */
class StartPetModeUseCase @Inject constructor(
    private val vehicle: VehicleControl,
    private val volume: MediaVolumeControl,
    private val player: CalmSoundPlayer,
) {
    suspend operator fun invoke(settings: PetModeSettings): PetModeReport {
        val steps = buildList {
            add(runStep(PetModeStep.TEMPERATURE) { vehicle.setCabinTemperature(settings.targetTempC) })
            add(runStep(PetModeStep.FAN) { vehicle.setFanLevel(settings.fanLevel) })
            if (settings.rearWindowEnabled) add(openRearWindow(settings))
            add(runStep(PetModeStep.VOLUME) { volume.setMediaVolumePercent(settings.mediaVolumePercent) })
            if (settings.autoPlayCalmSound) {
                add(runStep(PetModeStep.CALM_SOUND) { player.play(CalmSituation.DEPARTURE) })
            }
        }
        return PetModeReport("펫 모드를 켰어요", steps)
    }

    /** 비가 오면 열지 않는다. 열었으면 반려동물이 스위치를 밟지 않게 뒷창문을 잠근다(미지원 차량은 조용히 건너뜀). */
    private suspend fun openRearWindow(settings: PetModeSettings): PetModeStepResult {
        if (withTimeoutOrNull(RAIN_CHECK_TIMEOUT_MS) { vehicle.isRaining() } == true) {
            return PetModeStepResult(PetModeStep.WINDOW, Result.failure(StepSkipped("비가 와서 뒷창문은 열지 않았어요")))
        }
        // 예전에 저장된 큰 값(최대 30%)도 상한으로 줄인다
        val percent = settings.rearWindowOpenPercent.coerceIn(0, PetModeSettings.REAR_WINDOW_MAX_PERCENT)
        val result = runStep(PetModeStep.WINDOW) { vehicle.setRearWindowVentilation(percent) }
        if (result.result.isSuccess) vehicle.setRearWindowLock(true)
        return result
    }

    private companion object {
        const val RAIN_CHECK_TIMEOUT_MS = 2_500L
    }
}

/**
 * 펫 모드 OFF: 안정 사운드 정지. 공조는 변경하지 않는다.
 * 창문은 자동으로 닫지 않는다. 반려동물이 창틈에 있으면 끼일 수 있어, 사용자가 확인하고 [RestoreRearWindowUseCase]로 닫는다.
 */
class StopPetModeUseCase @Inject constructor(
    private val player: CalmSoundPlayer,
) {
    suspend operator fun invoke(): PetModeReport {
        val steps = listOf(
            runStep(PetModeStep.CALM_SOUND) { player.stop(); Result.success(Unit) },
        )
        return PetModeReport("펫 모드를 껐어요", steps)
    }
}

/**
 * 펫 모드가 연 뒷창문을 진입 전 값으로 되돌린다. [previousWindowPercent]가 null(알 수 없음)이면 닫는다.
 * 잠금은 진입 전에 풀려 있던 게 확실할 때만 푼다(모르면 잠근 채로 둔다).
 */
class RestoreRearWindowUseCase @Inject constructor(
    private val vehicle: VehicleControl,
) {
    suspend operator fun invoke(previousWindowPercent: Int?, previousLocked: Boolean?): Result<Unit> = runCatching {
        vehicle.setRearWindowVentilation(previousWindowPercent ?: 0).getOrThrow()
        if (previousLocked == false) vehicle.setRearWindowLock(false)
    }
}

private suspend fun runStep(step: PetModeStep, block: suspend () -> Result<Unit>): PetModeStepResult =
    PetModeStepResult(step, runCatching { block().getOrThrow() })
