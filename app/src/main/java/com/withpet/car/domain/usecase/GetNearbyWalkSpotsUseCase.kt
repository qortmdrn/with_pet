package com.withpet.car.domain.usecase

import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.WalkSpot
import com.withpet.car.domain.model.WalkSpotType
import com.withpet.car.domain.repository.WalkSpotRepository
import javax.inject.Inject

enum class WalkSpotFilter { ALL, DOG_PARK, PARK, TRAIL, LARGE_DOG }

data class NearbyWalkSpot(val spot: WalkSpot, val distanceKm: Double)

class GetNearbyWalkSpotsUseCase @Inject constructor(
    private val repository: WalkSpotRepository,
) {
    suspend operator fun invoke(origin: GeoPoint, filter: WalkSpotFilter = WalkSpotFilter.ALL): List<NearbyWalkSpot> =
        repository.getAll()
            .filter { it.matches(filter) }
            .map { NearbyWalkSpot(it, origin.distanceKmTo(it.location)) }
            .filter { it.distanceKm <= NEARBY_RADIUS_KM }
            .sortedBy { it.distanceKm }

    private fun WalkSpot.matches(filter: WalkSpotFilter): Boolean = when (filter) {
        WalkSpotFilter.ALL -> true
        WalkSpotFilter.DOG_PARK -> type == WalkSpotType.DOG_PARK
        WalkSpotFilter.PARK -> type == WalkSpotType.PARK
        WalkSpotFilter.TRAIL -> type == WalkSpotType.TRAIL
        WalkSpotFilter.LARGE_DOG -> largeDogAllowed
    }

    companion object {
        const val NEARBY_RADIUS_KM = 20.0
    }
}
