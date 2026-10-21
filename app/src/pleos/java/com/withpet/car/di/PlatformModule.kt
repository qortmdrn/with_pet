package com.withpet.car.di

import com.withpet.car.platform.PleosDrivingStateProvider
import com.withpet.car.platform.PleosLocationProvider
import com.withpet.car.platform.PleosMediaVolumeControl
import com.withpet.car.platform.PleosNavigationController
import com.withpet.car.platform.PleosPhoneDialer
import com.withpet.car.platform.PleosSpeechOutput
import com.withpet.car.platform.PleosVehicleControl
import com.withpet.car.platform.PleosVoiceCommandSource
import com.withpet.car.platform.audio.MediaVolumeControl
import com.withpet.car.platform.driving.DrivingStateProvider
import com.withpet.car.platform.location.LocationProvider
import com.withpet.car.platform.navigation.NavigationController
import com.withpet.car.platform.phone.PhoneDialer
import com.withpet.car.platform.vehicle.VehicleControl
import com.withpet.car.platform.voice.SpeechOutput
import com.withpet.car.platform.voice.VoiceCommandSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** pleos flavor: 실제 Pleos Connect SDK 어댑터 (M9에서 구현). */
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
    @Binds abstract fun volume(impl: PleosMediaVolumeControl): MediaVolumeControl
}
