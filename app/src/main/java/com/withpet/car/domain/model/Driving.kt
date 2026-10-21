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
