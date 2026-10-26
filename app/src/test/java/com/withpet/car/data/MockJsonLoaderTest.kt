package com.withpet.car.data

import com.withpet.car.data.mock.MockJsonLoader
import com.withpet.car.domain.model.CalmSituation
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
    fun `플레이리스트 - 상황별 2~3곡, 음원은 res raw에 있다`() {
        val playlists = loader.playlists().map { it.toDomain() }
        assertEquals(CalmSituation.entries.toSet(), playlists.map { it.situation }.toSet())
        assertTrue(playlists.all { it.tracks.size in 2..3 })
        val rawFiles = File("src/main/res/raw").list()!!.map { it.substringBeforeLast('.') }.toSet()
        assertTrue(playlists.flatMap { it.tracks }.all { it.rawResName in rawFiles })
    }
}
