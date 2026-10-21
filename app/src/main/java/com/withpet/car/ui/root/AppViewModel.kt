package com.withpet.car.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.withpet.car.domain.model.VoiceCommand
import com.withpet.car.domain.usecase.DrivingRestrictionMonitor
import com.withpet.car.domain.usecase.PetModeManager
import com.withpet.car.domain.usecase.PetModeState
import com.withpet.car.domain.usecase.RestAlertCoordinator
import com.withpet.car.domain.usecase.RestSuggestion
import com.withpet.car.platform.voice.VoiceCommandSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 화면 공통 상태(주행 제한, 오버레이)와 음성 명령(8장)을 처리한다. */
@HiltViewModel
class AppViewModel @Inject constructor(
    voice: VoiceCommandSource,
    restriction: DrivingRestrictionMonitor,
    private val petModeManager: PetModeManager,
    private val restAlert: RestAlertCoordinator,
) : ViewModel() {

    val restricted: StateFlow<Boolean> = restriction.isRestricted
    val petMode: StateFlow<PetModeState> = petModeManager.state
    val restSuggestion: StateFlow<RestSuggestion?> = restAlert.suggestion

    private val emergencyRequests = Channel<Unit>(Channel.CONFLATED)
    val openEmergency: Flow<Unit> = emergencyRequests.receiveAsFlow()

    init {
        viewModelScope.launch {
            voice.commands().collect { command ->
                when (command) {
                    VoiceCommand.Emergency -> emergencyRequests.send(Unit)
                    VoiceCommand.PetModeOn -> petModeManager.turnOn()
                    VoiceCommand.PetModeOff -> petModeManager.turnOff()
                    VoiceCommand.Confirm -> restAlert.accept()
                    VoiceCommand.Reject -> restAlert.reject()
                }
            }
        }
    }

    fun acknowledgeSafetyCheck() = petModeManager.acknowledgeSafetyCheck()

    fun acceptRest() {
        viewModelScope.launch { restAlert.accept() }
    }

    fun rejectRest() {
        viewModelScope.launch { restAlert.reject() }
    }
}
