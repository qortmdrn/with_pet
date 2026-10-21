package com.withpet.car.domain.usecase

import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.model.PetModeSettings
import com.withpet.car.domain.model.UnsupportedFeature
import com.withpet.car.domain.repository.CalmSoundPlayer
import com.withpet.car.platform.audio.MediaVolumeControl
import com.withpet.car.platform.vehicle.VehicleControl
import javax.inject.Inject

enum class PetModeStep(val label: String) {
    TEMPERATURE("온도 조절"),
    FAN("풍량 조절"),
    WINDOW("창문 자동 조절"),
    VOLUME("볼륨 조절"),
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
            if (result.exceptionOrNull() is UnsupportedFeature) "이 차량에서는 ${step.label}을 지원하지 않아요"
            else "${step.label}에 실패했어요"
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
            add(runStep(PetModeStep.WINDOW) { vehicle.setRearWindowVentilation(settings.rearWindowOpenPercent) })
            add(runStep(PetModeStep.VOLUME) { volume.setMediaVolumePercent(settings.mediaVolumePercent) })
            if (settings.autoPlayCalmSound) {
                add(runStep(PetModeStep.CALM_SOUND) { player.play(CalmSituation.DEPARTURE) })
            }
        }
        return PetModeReport("펫 모드를 켰어요", steps)
    }
}

/** 펫 모드 OFF: 창문 원위치, 안정 사운드 정지. 공조는 변경하지 않는다. */
class StopPetModeUseCase @Inject constructor(
    private val vehicle: VehicleControl,
    private val player: CalmSoundPlayer,
) {
    /** [previousWindowPercent]가 null(진입 전 값을 알 수 없음)이면 안전하게 닫는다. */
    suspend operator fun invoke(previousWindowPercent: Int?): PetModeReport {
        val steps = listOf(
            runStep(PetModeStep.WINDOW) { vehicle.setRearWindowVentilation(previousWindowPercent ?: 0) },
            runStep(PetModeStep.CALM_SOUND) { player.stop(); Result.success(Unit) },
        )
        return PetModeReport("펫 모드를 껐어요", steps)
    }
}

private suspend fun runStep(step: PetModeStep, block: suspend () -> Result<Unit>): PetModeStepResult =
    PetModeStepResult(step, runCatching { block().getOrThrow() })
