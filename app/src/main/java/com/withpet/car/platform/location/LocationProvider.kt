package com.withpet.car.platform.location

import com.withpet.car.domain.model.GeoPoint
import kotlinx.coroutines.flow.Flow

interface LocationProvider {
    fun currentLocation(): Flow<GeoPoint>
}
