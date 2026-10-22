package com.withpet.car.ui.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.withpet.car.domain.model.HospitalCandidate
import com.withpet.car.domain.model.OpenStatus
import com.withpet.car.domain.usecase.CallAndNavigateResult
import com.withpet.car.domain.usecase.CallHospitalAndNavigateUseCase
import com.withpet.car.domain.usecase.EmergencyResult
import com.withpet.car.domain.usecase.FindEmergencyHospitalsUseCase
import com.withpet.car.platform.location.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

sealed interface EmergencyListState {
    data object Loading : EmergencyListState
    data object NoLocation : EmergencyListState
    data class Loaded(val result: EmergencyResult) : EmergencyListState
}

data class EmergencySelection(
    val candidate: HospitalCandidate,
    /** null이면 진행 중 */
    val result: CallAndNavigateResult?,
    /** 목록을 거치지 않고 가장 빨리 도착하는 진료 중 병원을 자동으로 고른 경우 */
    val auto: Boolean = false,
)

data class EmergencyUiState(
    val list: EmergencyListState = EmergencyListState.Loading,
    val selection: EmergencySelection? = null,
)

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val location: LocationProvider,
    private val findHospitals: FindEmergencyHospitalsUseCase,
    private val callAndNavigate: CallHospitalAndNavigateUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EmergencyUiState())
    val state: StateFlow<EmergencyUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = EmergencyUiState(EmergencyListState.Loading)
        viewModelScope.launch {
            val here = withTimeoutOrNull(LOCATION_TIMEOUT_MS) { location.currentLocation().firstOrNull() }
            val list = if (here == null) EmergencyListState.NoLocation else EmergencyListState.Loaded(findHospitals(here))
            _state.update { it.copy(list = list) }
            // 진료 중 병원이 있으면 응급 버튼 한 번으로 바로 전화·길 안내를 시작한다.
            // 후보는 진료 중 → ETA 순이라 첫 번째가 가장 빨리 도착하는 진료 중 병원이다. 미확인 병원은 목록에서 고르게 한다.
            (list as? EmergencyListState.Loaded)?.result?.candidates?.firstOrNull()
                ?.takeIf { it.status == OpenStatus.OPEN }
                ?.let { select(it, auto = true) }
        }
    }

    /** 터치 1회로 전화와 길 안내를 동시에 시작한다. */
    fun select(candidate: HospitalCandidate) = select(candidate, auto = false)

    private fun select(candidate: HospitalCandidate, auto: Boolean) {
        if (_state.value.selection?.let { it.candidate == candidate && it.result == null } == true) return
        _state.update { it.copy(selection = EmergencySelection(candidate, null, auto)) }
        viewModelScope.launch {
            val result = callAndNavigate(candidate.hospital)
            _state.update { it.copy(selection = EmergencySelection(candidate, result, auto)) }
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selection = null) }
    }

    private companion object {
        // 응급 목록 2초 이내 표시 (명세 7-2 수용 기준)
        const val LOCATION_TIMEOUT_MS = 1_500L
    }
}
