package com.withpet.car.platform.navigation

import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.RouteInfo
import kotlinx.coroutines.flow.Flow

interface NavigationController {
    suspend fun startGuidance(destination: GeoPoint, name: String): Result<Unit>
    suspend fun addWaypoint(point: GeoPoint, name: String): Result<Unit>
    suspend fun estimateEtaMinutes(destination: GeoPoint): Result<Int>
    fun activeRoute(): Flow<RouteInfo?>   // 경로 없으면 null
}
