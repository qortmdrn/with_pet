package com.withpet.car.di

import com.withpet.car.media.AppPlaybackVolume
import com.withpet.car.platform.audio.MediaVolumeControl
import com.withpet.car.platform.driving.DrivingStateProvider
import com.withpet.car.platform.driving.PleosDrivingStateProvider
import com.withpet.car.platform.location.LocationProvider
import com.withpet.car.platform.location.PleosLocationProvider
import com.withpet.car.platform.navigation.NavigationController
import com.withpet.car.platform.navigation.PleosNavigationController
import com.withpet.car.platform.phone.PhoneDialer
import com.withpet.car.platform.phone.PleosPhoneDialer
import com.withpet.car.platform.vehicle.PleosVehicleControl
import com.withpet.car.platform.vehicle.VehicleControl
import com.withpet.car.platform.voice.ExternalActionHandler
import com.withpet.car.platform.voice.GleoActionHandler
import com.withpet.car.platform.voice.PleosSpeechOutput
import com.withpet.car.platform.voice.PleosVoiceCommandSource
import com.withpet.car.platform.voice.SpeechOutput
import com.withpet.car.platform.voice.VoiceCommandSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** pleos flavor: Pleos Connect SDK 어댑터 (docs/pleos-sdk). SDK에 API가 없는 전화는 ACTION_DIAL, 볼륨은 앱 재생 볼륨으로 대신한다. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds abstract fun vehicle(impl: PleosVehicleControl): VehicleControl
    @Binds abstract fun navigation(impl: PleosNavigationController): NavigationController
    @Binds abstract fun location(impl: PleosLocationProvider): LocationProvider
    @Binds abstract fun phone(impl: PleosPhoneDialer): PhoneDialer
    @Binds abstract fun driving(impl: PleosDrivingStateProvider): DrivingStateProvider
    @Binds abstract fun voice(impl: PleosVoiceCommandSource): VoiceCommandSource
    @Binds abstract fun speech(impl: PleosSpeechOutput): SpeechOutput
    @Binds abstract fun volume(impl: AppPlaybackVolume): MediaVolumeControl
    @Binds abstract fun externalActions(impl: GleoActionHandler): ExternalActionHandler
}
