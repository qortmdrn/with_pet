package com.withpet.car.domain.usecase

import com.withpet.car.CITY_HALL
import com.withpet.car.domain.model.WalkSpot
import com.withpet.car.domain.model.WalkSpotType
import com.withpet.car.domain.repository.WalkSpotRepository
import com.withpet.car.everyDay
import com.withpet.car.north
import com.withpet.car.spot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class GetNearbyWalkSpotsUseCaseTest {

    private val zone = ZoneId.of("Asia/Seoul")
    // 월요일 21:00
    private val clock = Clock.fixed(LocalDateTime.of(2026, 10, 26, 21, 0).atZone(zone).toInstant(), zone)

    private fun useCase(vararg spots: WalkSpot) = GetNearbyWalkSpotsUseCase(
        object : WalkSpotRepository { override suspend fun getAll() = spots.toList() },
        clock,
    )

    @Test
    fun `지금 닫힌 곳과 20km 밖은 빼고 거리순`() = runTest {
        val result = useCase(
            spot("far", CITY_HALL.north(10.0)),
            spot("closedNear", CITY_HALL.north(1.0), hours = everyDay("09:00", "18:00")),
            spot("unknown", CITY_HALL.north(5.0), hours = emptyList()),
            spot("near", CITY_HALL.north(2.0)),
            spot("tooFar", CITY_HALL.north(25.0)),
        )(CITY_HALL)
        assertEquals(listOf("near", "unknown", "far"), result.map { it.spot.id })
    }

    @Test
    fun `필터 - 반려견 놀이터, 대형견 가능`() = runTest {
        val spots = arrayOf(
            spot("dog", CITY_HALL.north(1.0), type = WalkSpotType.DOG_PARK),
            spot("large", CITY_HALL.north(2.0), largeDog = true),
            spot("park", CITY_HALL.north(3.0)),
        )
        assertEquals(listOf("dog"), useCase(*spots)(CITY_HALL, WalkSpotFilter.DOG_PARK).map { it.spot.id })
        assertEquals(listOf("large"), useCase(*spots)(CITY_HALL, WalkSpotFilter.LARGE_DOG).map { it.spot.id })
    }
}
