package com.withpet.car.domain.usecase

import com.withpet.car.domain.logic.OpenStatusCalculator
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.OpenStatus
import com.withpet.car.domain.model.WalkSpot
import com.withpet.car.domain.model.WalkSpotType
import com.withpet.car.domain.repository.WalkSpotRepository
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

enum class WalkSpotFilter { ALL, DOG_PARK, PARK, TRAIL, LARGE_DOG }

data class NearbyWalkSpot(val spot: WalkSpot, val distanceKm: Double, val status: OpenStatus)

class GetNearbyWalkSpotsUseCase @Inject constructor(
    private val repository: WalkSpotRepository,
    private val clock: Clock,
) {
    /** 지금 닫혀 있는 곳은 빼고 거리순. 최대 [MAX_RESULTS]곳. */
    suspend operator fun invoke(origin: GeoPoint, filter: WalkSpotFilter = WalkSpotFilter.ALL): List<NearbyWalkSpot> {
        val now = LocalDateTime.now(clock)
        return repository.getAll()
            .filter { it.matches(filter) }
            .map { NearbyWalkSpot(it, origin.distanceKmTo(it.location), it.statusAt(now)) }
            .filter { it.distanceKm <= NEARBY_RADIUS_KM && it.status != OpenStatus.CLOSED }
            .sortedBy { it.distanceKm }
            .take(MAX_RESULTS)
    }

    private fun WalkSpot.matches(filter: WalkSpotFilter): Boolean = when (filter) {
        WalkSpotFilter.ALL -> true
        WalkSpotFilter.DOG_PARK -> type == WalkSpotType.DOG_PARK
        WalkSpotFilter.PARK -> type == WalkSpotType.PARK
        WalkSpotFilter.TRAIL -> type == WalkSpotType.TRAIL
        WalkSpotFilter.LARGE_DOG -> largeDogAllowed
    }

    companion object {
        const val NEARBY_RADIUS_KM = 20.0
        const val MAX_RESULTS = 20
    }
}

fun WalkSpot.statusAt(at: LocalDateTime): OpenStatus = OpenStatusCalculator.statusAt(weeklyHours, at, closedOnHolidays)
