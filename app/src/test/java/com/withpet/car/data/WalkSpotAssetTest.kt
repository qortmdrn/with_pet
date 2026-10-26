package com.withpet.car.data

import com.withpet.car.CITY_HALL
import com.withpet.car.data.repository.AssetWalkSpotRepository
import com.withpet.car.domain.model.OpenStatus
import com.withpet.car.domain.model.WalkSpotType
import com.withpet.car.domain.usecase.statusAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDateTime

/** scripts/build_public_data.py가 만든 실제 산책 스팟 asset을 검증한다. */
class WalkSpotAssetTest {

    private val repository = AssetWalkSpotRepository(
        Json { ignoreUnknownKeys = true },
        { File("src/main/assets/${AssetWalkSpotRepository.ASSET_PATH}").readText() },
        Dispatchers.Unconfined,
    )

    @Test
    fun `유형이 고루 있고, 좌표는 국내, id는 고유`() = runTest {
        val spots = repository.getAll()
        assertTrue(spots.size >= 500)
        listOf(WalkSpotType.DOG_PARK, WalkSpotType.PARK, WalkSpotType.TRAIL, WalkSpotType.WATERSIDE)
            .forEach { t -> assertTrue(spots.count { it.type == t } >= 20, "$t 부족") }
        assertTrue(spots.all { it.location.lat in 33.0..39.0 && it.location.lng in 124.0..132.0 })
        assertEquals(spots.size, spots.map { it.id }.toSet().size)
    }

    @Test
    fun `서울 시청 20km 안에 언제든 열린 곳이 4곳 이상 - 주행 제한 UI 테스트의 전제`() = runTest {
        val spots = repository.getAll().filter { CITY_HALL.distanceKmTo(it.location) <= 20.0 }
        // 새벽·공휴일에도
        listOf(LocalDateTime.of(2026, 10, 26, 3, 0), LocalDateTime.of(2026, 9, 25, 12, 0)).forEach { at ->
            assertTrue(spots.count { it.statusAt(at) == OpenStatus.OPEN } >= 4, "$at")
        }
    }
}
