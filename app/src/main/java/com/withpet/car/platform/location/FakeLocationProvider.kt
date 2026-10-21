package com.withpet.car.platform.location

import com.withpet.car.domain.model.GeoPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeLocationProvider @Inject constructor() : LocationProvider {

    private val location = MutableStateFlow<GeoPoint?>(SEOUL_CITY_HALL)
    /** 디버그 메뉴 표시용. null이면 위치 미수신 상태. */
    val current: StateFlow<GeoPoint?> = location.asStateFlow()

    override fun currentLocation(): Flow<GeoPoint> = location.filterNotNull()

    /** null을 넣으면 위치 미수신 상황을 흉내 낸다. */
    fun setLocation(point: GeoPoint?) {
        location.value = point
    }

    companion object {
        val SEOUL_CITY_HALL = GeoPoint(37.5663, 126.9779)
    }
}
