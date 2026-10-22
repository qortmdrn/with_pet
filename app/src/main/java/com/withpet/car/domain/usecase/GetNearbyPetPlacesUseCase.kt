package com.withpet.car.domain.usecase

import com.withpet.car.domain.logic.OpenStatusCalculator
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.OpenStatus
import com.withpet.car.domain.model.PetPlace
import com.withpet.car.domain.model.PetPlaceCategory
import com.withpet.car.domain.repository.PetPlaceRepository
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

data class NearbyPetPlace(val place: PetPlace, val distanceKm: Double, val status: OpenStatus)

data class NearbyPetPlaces(
    /** 영업 중 → 미확인 → 영업 종료, 같은 그룹은 거리순. 최대 [GetNearbyPetPlacesUseCase.MAX_RESULTS]곳. */
    val places: List<NearbyPetPlace>,
    val searchedRadiusKm: Double,
)

class GetNearbyPetPlacesUseCase @Inject constructor(
    private val repository: PetPlaceRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(origin: GeoPoint, category: PetPlaceCategory): NearbyPetPlaces {
        val now = LocalDateTime.now(clock)
        val inCategory = repository.getAll()
            .filter { it.category == category }
            .map { it to origin.distanceKmTo(it.location) }

        // 시설이 드문 지역은 반경을 넓힌다
        val radius = if (inCategory.any { (_, d) -> d <= PRIMARY_RADIUS_KM }) PRIMARY_RADIUS_KM else EXPANDED_RADIUS_KM
        val places = inCategory
            .filter { (_, d) -> d <= radius }
            .map { (place, d) -> NearbyPetPlace(place, d, OpenStatusCalculator.statusAt(place.weeklyHours, now)) }
            .sortedWith(compareBy({ it.status.rank }, { it.distanceKm }))
            .take(MAX_RESULTS)
        return NearbyPetPlaces(places, radius)
    }

    private val OpenStatus.rank: Int
        get() = when (this) {
            OpenStatus.OPEN -> 0
            OpenStatus.UNKNOWN -> 1
            OpenStatus.CLOSED -> 2
        }

    companion object {
        const val PRIMARY_RADIUS_KM = 20.0
        const val EXPANDED_RADIUS_KM = 50.0
        const val MAX_RESULTS = 20

        /** 앱 목록 밖의 장소는 내장 내비 검색으로 찾는다 */
        fun naviKeyword(category: PetPlaceCategory): String = when (category) {
            PetPlaceCategory.CAFE -> "애견동반 카페"
            PetPlaceCategory.TRAVEL -> "애견동반 여행지"
            PetPlaceCategory.CULTURE -> "반려동물 동반 문화시설"
        }
    }
}
