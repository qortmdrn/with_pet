package com.withpet.car.domain.logic

import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.RouteInfo
import com.withpet.car.domain.model.WalkSpot

object RestSpotSelector {
    const val AHEAD_ON_ROUTE_KM = 15.0
    const val AROUND_KM = 5.0

    /**
     * 활성 경로가 있으면 목적지 방향으로 앞쪽(목적지에 더 가까워지는) 15km 이내 중 가장 가까운 곳,
     * 없으면 현재 위치 반경 5km 이내 중 가장 가까운 곳.
     */
    fun select(spots: List<WalkSpot>, current: GeoPoint, route: RouteInfo?): WalkSpot? {
        if (route != null) {
            val destination = route.destination.point
            val remainingKm = current.distanceKmTo(destination)
            return spots
                .filter { current.distanceKmTo(it.location) <= AHEAD_ON_ROUTE_KM }
                .filter { it.location.distanceKmTo(destination) < remainingKm }
                .minByOrNull { current.distanceKmTo(it.location) }
        }
        return spots
            .filter { current.distanceKmTo(it.location) <= AROUND_KM }
            .minByOrNull { current.distanceKmTo(it.location) }
    }
}
