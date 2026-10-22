package com.withpet.car.domain.usecase

import com.withpet.car.CITY_HALL
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.OpenStatus
import com.withpet.car.domain.model.OpeningHours
import com.withpet.car.domain.model.PetPlace
import com.withpet.car.domain.model.PetPlaceCategory
import com.withpet.car.domain.repository.PetPlaceRepository
import com.withpet.car.everyDay
import com.withpet.car.north
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class GetNearbyPetPlacesUseCaseTest {

    private val zone = ZoneId.of("Asia/Seoul")
    // 월요일 15:00
    private val clock = Clock.fixed(LocalDateTime.of(2026, 10, 19, 15, 0).atZone(zone).toInstant(), zone)

    private fun place(
        id: String,
        location: GeoPoint,
        category: PetPlaceCategory = PetPlaceCategory.CAFE,
        hours: List<OpeningHours> = everyDay("09:00", "18:00"),
    ) = PetPlace(
        id = id, name = "장소 $id", category = category, location = location, address = "주소", phone = null,
        hasParking = false, sizeLimit = null, restriction = null, extraFee = null,
        hoursText = null, closedDaysText = null, weeklyHours = hours, updatedAt = "2025-01-01",
    )

    private fun useCase(vararg places: PetPlace) = GetNearbyPetPlacesUseCase(
        object : PetPlaceRepository { override suspend fun getAll() = places.toList() },
        clock,
    )

    @Test
    fun `고른 종류만, 영업 중 → 미확인 → 종료 순, 같은 그룹은 거리순`() = runTest {
        val result = useCase(
            place("closedNear", CITY_HALL.north(0.5), hours = everyDay("18:00", "23:00")),
            place("unknownNear", CITY_HALL.north(1.0), hours = emptyList()),
            place("openFar", CITY_HALL.north(10.0)),
            place("openNear", CITY_HALL.north(3.0)),
            place("travel", CITY_HALL.north(0.1), category = PetPlaceCategory.TRAVEL),
        )(CITY_HALL, PetPlaceCategory.CAFE)

        assertEquals(listOf("openNear", "openFar", "unknownNear", "closedNear"), result.places.map { it.place.id })
        assertEquals(listOf(OpenStatus.OPEN, OpenStatus.OPEN, OpenStatus.UNKNOWN, OpenStatus.CLOSED), result.places.map { it.status })
        assertEquals(GetNearbyPetPlacesUseCase.PRIMARY_RADIUS_KM, result.searchedRadiusKm)
    }

    @Test
    fun `20km 안에 없으면 50km까지 넓힌다`() = runTest {
        val result = useCase(place("far", CITY_HALL.north(35.0)), place("tooFar", CITY_HALL.north(60.0)))(CITY_HALL, PetPlaceCategory.CAFE)
        assertEquals(listOf("far"), result.places.map { it.place.id })
        assertEquals(GetNearbyPetPlacesUseCase.EXPANDED_RADIUS_KM, result.searchedRadiusKm)
    }

    @Test
    fun `최대 20곳`() = runTest {
        val many = (1..30).map { place("p$it", CITY_HALL.north(it * 0.1)) }.toTypedArray()
        assertEquals(GetNearbyPetPlacesUseCase.MAX_RESULTS, useCase(*many)(CITY_HALL, PetPlaceCategory.CAFE).places.size)
    }
}
