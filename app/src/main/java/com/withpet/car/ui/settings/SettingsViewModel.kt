package com.withpet.car.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.withpet.car.domain.model.PetModeSettings
import com.withpet.car.domain.model.RestAlertSettings
import com.withpet.car.domain.repository.SettingsRepository
import com.withpet.car.domain.usecase.DrivingRestrictionMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val petMode: PetModeSettings = PetModeSettings(),
    val restAlert: RestAlertSettings = RestAlertSettings(),
    val restricted: Boolean = false,
    val loaded: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    restriction: DrivingRestrictionMonitor,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        repository.petModeSettings(),
        repository.restAlertSettings(),
        restriction.isRestricted,
    ) { petMode, rest, restricted -> SettingsUiState(petMode, rest, restricted, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun updatePetMode(transform: (PetModeSettings) -> PetModeSettings) {
        viewModelScope.launch { repository.updatePetModeSettings(transform(state.value.petMode)) }
    }

    fun updateRestAlert(transform: (RestAlertSettings) -> RestAlertSettings) {
        viewModelScope.launch { repository.updateRestAlertSettings(transform(state.value.restAlert)) }
    }
}
