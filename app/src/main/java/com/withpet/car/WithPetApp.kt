package com.withpet.car

import android.app.Application
import com.withpet.car.data.DataPreloader
import com.withpet.car.domain.usecase.RestAlertCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WithPetApp : Application() {

    @Inject lateinit var restAlertCoordinator: RestAlertCoordinator
    @Inject lateinit var dataPreloader: DataPreloader

    override fun onCreate() {
        super.onCreate()
        dataPreloader.start()
        restAlertCoordinator.start()
    }
}
