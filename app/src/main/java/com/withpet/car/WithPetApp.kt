package com.withpet.car

import android.app.Application
import com.withpet.car.domain.usecase.RestAlertCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WithPetApp : Application() {

    @Inject lateinit var restAlertCoordinator: RestAlertCoordinator

    override fun onCreate() {
        super.onCreate()
        restAlertCoordinator.start()
    }
}
