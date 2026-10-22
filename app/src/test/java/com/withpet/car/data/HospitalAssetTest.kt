package com.withpet.car.data

import com.withpet.car.CITY_HALL
import com.withpet.car.data.repository.AssetHospitalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** scripts/build_public_data.py가 만든 실제 동물병원 asset을 검증한다. */
class HospitalAssetTest {

    private val repository = AssetHospitalRepository(
        Json { ignoreUnknownKeys = true },
        { File("src/main/assets/${AssetHospitalRepository.ASSET_PATH}").readText() },
        Dispatchers.Unconfined,
    )

    @Test
    fun `전국 병원, 좌표는 국내, id는 고유, 번호는 전화번호 형식`() = runTest {
        val hospitals = repository.getAll()
        assertTrue(hospitals.size >= 4_000)
        assertTrue(hospitals.all { it.location.lat in 33.0..39.0 && it.location.lng in 124.0..132.0 })
        assertEquals(hospitals.size, hospitals.map { it.id }.toSet().size)
        val phoneFormat = Regex("""0\d{1,3}-\d{3,4}-\d{4}|1\d{3}-\d{4}""")
        assertTrue(hospitals.mapNotNull { it.phone }.all { phoneFormat.matches(it) })
    }

    @Test
    fun `서울 시청 20km 안에 24시 병원이 있다 - 응급 원터치 UI 테스트의 전제`() = runTest {
        val near24 = repository.getAll().filter { it.is24Hours && CITY_HALL.distanceKmTo(it.location) <= 20.0 }
        assertTrue(near24.isNotEmpty())
    }
}
