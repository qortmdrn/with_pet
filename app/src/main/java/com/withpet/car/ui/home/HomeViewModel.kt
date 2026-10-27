package com.withpet.car.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.withpet.car.domain.model.CalmPlaybackState
import com.withpet.car.domain.model.PetProfile
import com.withpet.car.domain.repository.CalmSoundPlayer
import com.withpet.car.domain.repository.PetProfileRepository
import com.withpet.car.domain.usecase.DrivingRestrictionMonitor
import com.withpet.car.domain.usecase.PetModeManager
import com.withpet.car.domain.usecase.PetModeState
import com.withpet.car.platform.vehicle.VehicleControl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val pet: PetProfile? = null,
    val petMode: PetModeState = PetModeState(),
    val cabinTempC: Float? = null,
    val playback: CalmPlaybackState = CalmPlaybackState(),
    val restricted: Boolean = false,
) {
    /** 펫 모드 ON일 때 진행 중 상태 한 줄. 예: "실내 22℃ · 안정 사운드 재생 중" */
    val statusLine: String?
        get() {
            if (!petMode.isOn) return null
            val parts = buildList {
                cabinTempC?.let { add("실내 ${formatTemp(it)}℃") }
                if (playback.isPlaying) add("안정 사운드 재생 중")
            }
            return parts.joinToString(" · ").ifEmpty { "펫 모드 켜짐" }
        }

    private fun formatTemp(value: Float) = if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    petProfiles: PetProfileRepository,
    vehicle: VehicleControl,
    player: CalmSoundPlayer,
    restriction: DrivingRestrictionMonitor,
    private val petModeManager: PetModeManager,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        petProfiles.observeRepresentative().onStart { emit(null) },
        petModeManager.state,
        vehicle.cabinTemperature().onStart { emit(null) },
        player.state,
        restriction.isRestricted,
    ) { pet, petMode, temp, playback, restricted ->
        HomeUiState(pet, petMode, temp, playback, restricted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setPetMode(on: Boolean) {
        viewModelScope.launch { if (on) petModeManager.turnOn() else petModeManager.turnOff() }
    }

    fun closeRearWindow() {
        viewModelScope.launch { petModeManager.closeRearWindow() }
    }
}
