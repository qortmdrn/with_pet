package com.withpet.car.platform.navigation

import android.util.Log
import com.withpet.car.domain.logic.EtaEstimator
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.NamedPoint
import com.withpet.car.domain.model.RouteInfo
import com.withpet.car.platform.location.LocationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** 목적지·경유지를 메모리에 저장. ETA는 직선거리 ÷ 40km/h. */
@Singleton
class FakeNavigationController @Inject constructor(
    private val location: LocationProvider,
) : NavigationController {

    private val route = MutableStateFlow<RouteInfo?>(null)
    val currentRoute: StateFlow<RouteInfo?> = route.asStateFlow()

    override suspend fun startGuidance(destination: GeoPoint, name: String): Result<Unit> {
        route.value = RouteInfo(NamedPoint(destination, name), emptyList())
        Log.i(TAG, "가짜 길 안내 시작: $name")
        return Result.success(Unit)
    }

    override suspend fun addWaypoint(point: GeoPoint, name: String): Result<Unit> {
        val current = route.value ?: return Result.failure(IllegalStateException("활성 경로 없음"))
        route.value = current.copy(waypoints = current.waypoints + NamedPoint(point, name))
        Log.i(TAG, "가짜 경유지 추가: $name")
        return Result.success(Unit)
    }

    override suspend fun estimateEtaMinutes(destination: GeoPoint): Result<Int> {
        val here = withTimeoutOrNull(500) { location.currentLocation().firstOrNull() }
            ?: return Result.failure(IllegalStateException("위치 미수신"))
        return Result.success(EtaEstimator.estimateMinutes(here, destination))
    }

    override fun activeRoute(): Flow<RouteInfo?> = route.asStateFlow()

    fun clearRoute() {
        route.value = null
    }

    private companion object {
        const val TAG = "FakeNavigation"
    }
}
