package com.withpet.car.platform.driving

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class CarUxRestrictionsSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : UxRestrictionsSource {

    override fun requiresDistractionOptimization(): Flow<Boolean> = callbackFlow {
        // 자동차가 아닌 기기(휴대폰 에뮬레이터 등)에는 Car 서비스가 없다
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            trySend(false)
            awaitClose()
            return@callbackFlow
        }
        val car = try {
            Car.createCar(context)
        } catch (t: Throwable) {
            Log.w(TAG, "Car 연결 실패", t)
            null
        }
        val manager = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
        if (manager == null) {
            trySend(false)
            awaitClose { car?.disconnect() }
            return@callbackFlow
        }
        trySend(manager.currentCarUxRestrictions?.isRequiresDistractionOptimization ?: false)
        manager.registerListener { restrictions ->
            trySend(restrictions.isRequiresDistractionOptimization)
        }
        awaitClose {
            manager.unregisterListener()
            car.disconnect()
        }
    }.distinctUntilChanged()

    private companion object {
        const val TAG = "CarUxRestrictions"
    }
}
