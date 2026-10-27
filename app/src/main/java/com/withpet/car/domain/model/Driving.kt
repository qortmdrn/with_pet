package com.withpet.car.domain.model

enum class DrivingState { PARKED, DRIVING }

sealed interface VoiceCommand {
    data object Emergency : VoiceCommand
    data object PetModeOn : VoiceCommand
    data object PetModeOff : VoiceCommand
    data object Confirm : VoiceCommand
    data object Reject : VoiceCommand
}

class UnsupportedFeature(message: String) : Exception(message)

/** 안전을 위해 일부러 건너뛴 단계. [message]는 사용자에게 그대로 보여준다. 예: "비가 와서 뒷창문은 열지 않았어요" */
class StepSkipped(message: String) : Exception(message)
