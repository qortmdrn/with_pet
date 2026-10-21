package com.withpet.car.di

import com.withpet.car.platform.audio.FakeMediaVolumeControl
import com.withpet.car.platform.audio.MediaVolumeControl
import com.withpet.car.platform.driving.DrivingStateProvider
import com.withpet.car.platform.driving.FakeDrivingStateProvider
import com.withpet.car.platform.location.FakeLocationProvider
import com.withpet.car.platform.location.LocationProvider
import com.withpet.car.platform.navigation.FakeNavigationController
import com.withpet.car.platform.navigation.NavigationController
import com.withpet.car.platform.phone.FakePhoneDialer
import com.withpet.car.platform.phone.PhoneDialer
import com.withpet.car.platform.vehicle.FakeVehicleControl
import com.withpet.car.platform.vehicle.VehicleControl
import com.withpet.car.platform.voice.FakeSpeechOutput
import com.withpet.car.platform.voice.FakeVoiceCommandSource
import com.withpet.car.platform.voice.SpeechOutput
import com.withpet.car.platform.voice.VoiceCommandSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** fake flavor: Pleos SDK 없이 실행한다. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds abstract fun vehicle(impl: FakeVehicleControl): VehicleControl
    @Binds abstract fun navigation(impl: FakeNavigationController): NavigationController
    @Binds abstract fun location(impl: FakeLocationProvider): LocationProvider
    @Binds abstract fun phone(impl: FakePhoneDialer): PhoneDialer
    @Binds abstract fun driving(impl: FakeDrivingStateProvider): DrivingStateProvider
    @Binds abstract fun voice(impl: FakeVoiceCommandSource): VoiceCommandSource
    @Binds abstract fun speech(impl: FakeSpeechOutput): SpeechOutput
    @Binds abstract fun volume(impl: FakeMediaVolumeControl): MediaVolumeControl
}
