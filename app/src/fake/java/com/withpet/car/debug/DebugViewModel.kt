package com.withpet.car.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.withpet.car.domain.model.DrivingState
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.VoiceCommand
import com.withpet.car.platform.audio.FakeMediaVolumeControl
import com.withpet.car.platform.driving.FakeDrivingStateProvider
import com.withpet.car.platform.location.FakeLocationProvider
import com.withpet.car.platform.navigation.FakeNavigationController
import com.withpet.car.platform.phone.FakePhoneDialer
import com.withpet.car.platform.vehicle.FakeVehicleControl
import com.withpet.car.platform.voice.FakeVoiceCommandSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val locationProvider: FakeLocationProvider,
    private val driving: FakeDrivingStateProvider,
    private val phone: FakePhoneDialer,
    private val vehicle: FakeVehicleControl,
    private val navigation: FakeNavigationController,
    private val voice: FakeVoiceCommandSource,
    volume: FakeMediaVolumeControl,
) : ViewModel() {

    val location: StateFlow<GeoPoint?> = locationProvider.current
    val drivingState: StateFlow<DrivingState> = driving.currentState
    val drivingMinutes: StateFlow<Int> = driving.currentMinutes
    val phoneConnected: StateFlow<Boolean> = phone.isConnected
    val windowSupported: StateFlow<Boolean> = vehicle.isWindowSupported
    val raining: StateFlow<Boolean> = vehicle.isRainingNow

    val route: StateFlow<String?> = navigation.currentRoute
        .map { r -> r?.let { (listOf("목적지: ${it.destination.name}") + it.waypoints.map { w -> "경유: ${w.name}" }).joinToString(" · ") } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val vehicleSummary: StateFlow<String> = combine(
        vehicle.cabinTemperature(),
        vehicle.fanLevel,
        vehicle.rearWindow,
        vehicle.rearWindowLock,
        volume.currentVolume,
    ) { temp, fan, window, lock, vol ->
        "온도 ${temp}℃ · 풍량 ${fan} · 뒷창문 ${window ?: "-"}%${if (lock) "(잠금)" else ""} · 볼륨 ${vol}%"
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun setLocation(point: GeoPoint?) = locationProvider.setLocation(point)
    fun setDrivingState(state: DrivingState) = driving.setState(state)
    fun setDrivingMinutes(minutes: Int) = driving.setContinuousDrivingMinutes(minutes)
    fun setPhoneConnected(value: Boolean) = phone.setConnected(value)
    fun setWindowSupported(value: Boolean) = vehicle.setWindowSupported(value)
    fun setRaining(value: Boolean) = vehicle.setRaining(value)
    fun sendVoice(command: VoiceCommand) = voice.send(command)
    fun clearRoute() = navigation.clearRoute()

    fun startSampleRoute() {
        viewModelScope.launch { navigation.startGuidance(GeoPoint(37.6584, 126.8320), "테스트 목적지 (일산 방향)") }
    }
}
