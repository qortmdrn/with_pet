package com.withpet.car.platform.navigation

import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.RouteInfo
import kotlinx.coroutines.flow.Flow

interface NavigationController {
    suspend fun startGuidance(destination: GeoPoint, name: String): Result<Unit>
    suspend fun addWaypoint(point: GeoPoint, name: String): Result<Unit>
    suspend fun estimateEtaMinutes(destination: GeoPoint): Result<Int>
    fun activeRoute(): Flow<RouteInfo?>   // 경로 없으면 null
    /** 내장 내비의 장소 검색 화면을 [keyword]로 연다 */
    suspend fun searchPlace(keyword: String): Result<Unit>
}
