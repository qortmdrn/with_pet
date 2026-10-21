package com.withpet.car.data

import com.withpet.car.CITY_HALL
import com.withpet.car.data.mock.MockJsonLoader
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.model.WalkSpotType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** 실제 assets/mock 파일이 명세 10장 조건을 지키는지 확인한다. */
class MockJsonLoaderTest {

    private val loader = MockJsonLoader(Json { ignoreUnknownKeys = true }) { name ->
        File("src/main/assets/${MockJsonLoader.DIR}/$name").readText()
    }

    @Test
    fun `병원 12곳 - 24시 3, 미확인 2, 야간 마감 7, 제휴 4, 반경 15km`() {
        val hospitals = loader.hospitals().map { it.toDomain() }
        assertEquals(12, hospitals.size)
        assertEquals(3, hospitals.count { it.is24Hours })
        assertEquals(2, hospitals.count { !it.is24Hours && it.weeklyHours.isEmpty() })
        assertEquals(7, hospitals.count { !it.is24Hours && it.weeklyHours.isNotEmpty() })
        assertEquals(4, hospitals.count { it.isPartner })
        assertTrue(hospitals.all { CITY_HALL.distanceKmTo(it.location) <= 15.0 })
        // 실제 병원명·번호 금지
        assertTrue(hospitals.all { it.name.startsWith("테스트 동물병원") })
        assertTrue(hospitals.all { it.phone.startsWith("010-0000-") })
    }

    @Test
    fun `산책 스팟 15곳 - 유형별 고르게, 대형견 가능 5곳, 반경 15km`() {
        val spots = loader.walkSpots().map { it.toDomain() }
        assertEquals(15, spots.size)
        assertEquals(5, spots.count { it.largeDogAllowed })
        WalkSpotType.entries.forEach { type -> assertEquals(3, spots.count { it.type == type }) }
        assertTrue(spots.all { CITY_HALL.distanceKmTo(it.location) <= 15.0 })
    }

    @Test
    fun `플레이리스트 - 상황별 2~3곡, 음원은 res raw에 있다`() {
        val playlists = loader.playlists().map { it.toDomain() }
        assertEquals(CalmSituation.entries.toSet(), playlists.map { it.situation }.toSet())
        assertTrue(playlists.all { it.tracks.size in 2..3 })
        val rawFiles = File("src/main/res/raw").list()!!.map { it.substringBeforeLast('.') }.toSet()
        assertTrue(playlists.flatMap { it.tracks }.all { it.rawResName in rawFiles })
    }
}
