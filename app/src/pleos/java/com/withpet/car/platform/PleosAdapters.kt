package com.withpet.car.platform

import com.withpet.car.domain.model.DrivingState
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.RouteInfo
import com.withpet.car.domain.model.UnsupportedFeature
import com.withpet.car.domain.model.VoiceCommand
import com.withpet.car.platform.audio.MediaVolumeControl
import com.withpet.car.platform.driving.DrivingStateProvider
import com.withpet.car.platform.location.LocationProvider
import com.withpet.car.platform.navigation.NavigationController
import com.withpet.car.platform.phone.PhoneDialer
import com.withpet.car.platform.vehicle.VehicleControl
import com.withpet.car.platform.voice.SpeechOutput
import com.withpet.car.platform.voice.VoiceCommandSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

// TODO(M9): docs/pleos-sdk/ 의 SDK 문서를 확인한 뒤 Pleos Connect SDK로 구현한다.
//  SDK 클래스·메서드명이 확정되지 않았으므로 지금은 모두 미지원으로 응답한다.

private fun <T> notYet(feature: String): Result<T> =
    Result.failure(UnsupportedFeature("$feature: Pleos SDK 연동 전"))

class PleosVehicleControl @Inject constructor() : VehicleControl {
    override suspend fun setCabinTemperature(celsius: Float) = notYet<Unit>("공조 온도")
    override suspend fun setFanLevel(level: Int) = notYet<Unit>("풍량")
    override suspend fun setRearWindowVentilation(openPercent: Int) = notYet<Unit>("창문")
    override fun cabinTemperature(): Flow<Float?> = flowOf(null)
    override fun rearWindowOpenPercent(): Flow<Int?> = flowOf(null)
}

class PleosNavigationController @Inject constructor() : NavigationController {
    override suspend fun startGuidance(destination: GeoPoint, name: String) = notYet<Unit>("길 안내")
    override suspend fun addWaypoint(point: GeoPoint, name: String) = notYet<Unit>("경유지")
    override suspend fun estimateEtaMinutes(destination: GeoPoint) = notYet<Int>("ETA")
    override fun activeRoute(): Flow<RouteInfo?> = flowOf(null)
}

class PleosLocationProvider @Inject constructor() : LocationProvider {
    override fun currentLocation(): Flow<GeoPoint> = emptyFlow()
}

class PleosPhoneDialer @Inject constructor() : PhoneDialer {
    override fun isCallCapable(): Flow<Boolean> = flowOf(false)
    override suspend fun call(phoneNumber: String) = notYet<Unit>("전화")
}

class PleosDrivingStateProvider @Inject constructor() : DrivingStateProvider {
    override fun state(): Flow<DrivingState> = flowOf(DrivingState.PARKED)
    override fun continuousDrivingMinutes(): Flow<Int> = flowOf(0)
}

class PleosVoiceCommandSource @Inject constructor() : VoiceCommandSource {
    override fun commands(): Flow<VoiceCommand> = emptyFlow()
}

class PleosSpeechOutput @Inject constructor() : SpeechOutput {
    override fun isAvailable() = false
    override suspend fun speak(text: String) = notYet<Unit>("음성 안내")
}

class PleosMediaVolumeControl @Inject constructor() : MediaVolumeControl {
    override suspend fun setMediaVolumePercent(percent: Int) = notYet<Unit>("볼륨")
}
