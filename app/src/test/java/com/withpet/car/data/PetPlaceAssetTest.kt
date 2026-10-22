package com.withpet.car.data

import com.withpet.car.CITY_HALL
import com.withpet.car.data.repository.AssetPetPlaceRepository
import com.withpet.car.domain.model.PetPlaceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** scripts/build_pet_places.py가 만든 실제 asset을 검증한다. */
class PetPlaceAssetTest {

    private val repository = AssetPetPlaceRepository(
        Json { ignoreUnknownKeys = true },
        { File("src/main/assets/${AssetPetPlaceRepository.ASSET_PATH}").readText() },
        Dispatchers.Unconfined,
    )

    @Test
    fun `종류별로 충분히 있고, 좌표는 국내, id는 고유`() = runTest {
        val places = repository.getAll()
        PetPlaceCategory.entries.forEach { c -> assertTrue(places.count { it.category == c } >= 100, "$c 부족") }
        assertTrue(places.all { it.location.lat in 33.0..39.0 && it.location.lng in 124.0..132.0 })
        assertEquals(places.size, places.map { it.id }.toSet().size)
        assertTrue(places.all { it.name.isNotBlank() && it.address.isNotBlank() })
    }

    @Test
    fun `운영시간 해석률 85% 이상`() = runTest {
        val places = repository.getAll()
        val parsed = places.count { it.weeklyHours.isNotEmpty() }
        assertTrue(parsed >= places.size * 0.85, "해석 $parsed / ${places.size}")
    }

    @Test
    fun `서울 시청 20km 안에 카페가 있다 - 에뮬레이터 기본 위치`() = runTest {
        val near = repository.getAll().filter { it.category == PetPlaceCategory.CAFE && CITY_HALL.distanceKmTo(it.location) <= 20.0 }
        assertTrue(near.size >= 3)
    }
}
