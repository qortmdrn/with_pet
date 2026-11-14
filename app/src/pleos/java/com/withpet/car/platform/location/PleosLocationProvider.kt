package com.withpet.car.platform.location

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.withpet.car.di.ApplicationScope
import com.withpet.car.domain.model.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

/** Pleos FusedLocation SDK. 위치는 기기 안에서만 쓰고 로그에 좌표를 남기지 않는다. */
@Singleton
class PleosLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope scope: CoroutineScope,
) : LocationProvider {

    private val locations: Flow<GeoPoint> = callbackFlow {
        // 위치 권한은 런타임 권한이다. 사용자가 허용할 때까지 기다렸다가 구독한다.
        while (context.checkSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            delay(PERMISSION_POLL_MS)
        }
        val fused = FusedLocation(context.applicationContext)
        val started = runCatching {
            fused.initialize()
            fused.isSupported()
        }.onFailure { Log.w(TAG, "FusedLocation 초기화 실패", it) }.getOrDefault(false)

        val listener = object : FusedLocationListener {
            override fun onChanged(location: LocationData?) {
                location?.let { trySend(GeoPoint(it.latitude, it.longitude)) }
            }

            override fun onError(e: Exception) {
                Log.w(TAG, "위치 수신 오류", e)
            }
        }
        if (started) {
            runCatching {
                fused.getLastKnownLocation(listener)
                fused.registerFusedLocationCallback(listener)
            }.onFailure { Log.w(TAG, "위치 구독 실패", it) }
        }
        // 실패해도 흐름을 닫지 않는다: 화면은 "위치 미수신"으로 처리한다
        awaitClose {
            runCatching {
                if (started) fused.unregisterFusedLocationCallback(listener)
                fused.release()
            }
        }
    }.shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    override fun currentLocation(): Flow<GeoPoint> = locations

    private companion object {
        const val TAG = "PleosLocation"
        const val STOP_TIMEOUT_MS = 5_000L
        const val PERMISSION = "pleos.car.permission.FUSED_LOCATION"
        const val PERMISSION_POLL_MS = 1_000L
    }
}
