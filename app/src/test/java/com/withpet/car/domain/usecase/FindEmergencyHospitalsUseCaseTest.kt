package com.withpet.car.domain.usecase

import com.withpet.car.CITY_HALL
import com.withpet.car.domain.logic.EtaEstimator
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.Hospital
import com.withpet.car.domain.model.HospitalCandidate
import com.withpet.car.domain.model.OpenStatus
import com.withpet.car.domain.model.RouteInfo
import com.withpet.car.domain.repository.HospitalRepository
import com.withpet.car.everyDay
import com.withpet.car.hospital
import com.withpet.car.north
import com.withpet.car.platform.navigation.NavigationController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class FindEmergencyHospitalsUseCaseTest {

    private val zone = ZoneId.of("Asia/Seoul")
    // 월요일 23:30 — 야간 마감 병원은 대부분 닫힘
    private val clock = Clock.fixed(LocalDateTime.of(2026, 10, 19, 23, 30).atZone(zone).toInstant(), zone)

    private class Repo(private val list: List<Hospital>) : HospitalRepository {
        override suspend fun getAll() = list
    }

    /** ETA를 id 기준으로 지정. 없는 id는 실패시켜 직선거리 추정을 쓰게 한다. */
    private class Nav(private val etaByLocation: Map<GeoPoint, Int>) : NavigationController {
        override suspend fun startGuidance(destination: GeoPoint, name: String) = Result.success(Unit)
        override suspend fun addWaypoint(point: GeoPoint, name: String) = Result.success(Unit)
        override suspend fun estimateEtaMinutes(destination: GeoPoint): Result<Int> =
            etaByLocation[destination]?.let { Result.success(it) } ?: Result.failure(IllegalStateException())
        override fun activeRoute(): Flow<RouteInfo?> = flowOf(null)
    }

    private fun useCase(list: List<Hospital>, eta: Map<GeoPoint, Int> = emptyMap()) =
        FindEmergencyHospitalsUseCase(Repo(list), Nav(eta), clock)

    @Test
    fun `진료 중 먼저, 같은 그룹은 ETA 순, 닫힌 곳 제외, 최대 3곳`() = runTest {
        val open1 = hospital("open1", CITY_HALL.north(5.0), is24Hours = true)
        val open2 = hospital("open2", CITY_HALL.north(2.0), is24Hours = true)
        val unknown = hospital("unknown", CITY_HALL.north(1.0))
        val closed = hospital("closed", CITY_HALL.north(0.5), hours = everyDay("09:00", "21:00"))
        val open3 = hospital("open3", CITY_HALL.north(3.0), hours = everyDay("10:00", "01:00"))

        val result = useCase(listOf(open1, open2, unknown, closed, open3))(CITY_HALL)

        assertEquals(listOf("open2", "open3", "open1"), result.candidates.map { it.hospital.id })
        assertTrue(result.candidates.all { it.status == OpenStatus.OPEN })
        assertEquals(20.0, result.searchedRadiusKm)
    }

    @Test
    fun `미확인 병원은 진료 중 뒤에 온다`() = runTest {
        val unknownNear = hospital("unknown", CITY_HALL.north(1.0))
        val openFar = hospital("open", CITY_HALL.north(10.0), is24Hours = true)

        val result = useCase(listOf(unknownNear, openFar))(CITY_HALL)

        assertEquals(listOf("open", "unknown"), result.candidates.map { it.hospital.id })
        assertEquals(listOf(OpenStatus.OPEN, OpenStatus.UNKNOWN), result.candidates.map { it.status })
    }

    @Test
    fun `내비 ETA를 우선 쓰고, 실패하면 직선거리로 추정한다`() = runTest {
        val a = hospital("a", CITY_HALL.north(2.0), is24Hours = true)
        val b = hospital("b", CITY_HALL.north(8.0), is24Hours = true)
        // 내비 기준으로는 b가 더 빠르다
        val result = useCase(listOf(a, b), eta = mapOf(b.location to 2))(CITY_HALL)

        assertEquals(listOf("b", "a"), result.candidates.map { it.hospital.id })
        assertEquals(2, result.candidates[0].etaMinutes)
        // 내비 실패 → 직선거리 ÷ 40km/h
        assertEquals(EtaEstimator.estimateMinutes(CITY_HALL, a.location), result.candidates[1].etaMinutes)
    }

    @Test
    fun `20km 안에 없으면 50km로 넓힌다`() = runTest {
        val far = hospital("far", CITY_HALL.north(30.0), is24Hours = true)
        val result = useCase(listOf(far))(CITY_HALL)

        assertEquals(listOf("far"), result.candidates.map { it.hospital.id })
        assertEquals(50.0, result.searchedRadiusKm)
    }

    @Test
    fun `50km에도 진료 중인 곳이 없으면 가장 가까운 병원 1곳을 상태와 무관하게 준다`() = runTest {
        val closedNear = hospital("closedNear", CITY_HALL.north(1.0), hours = everyDay("09:00", "18:00"))
        val openTooFar = hospital("openFar", CITY_HALL.north(80.0), is24Hours = true)

        val result = useCase(listOf(closedNear, openTooFar))(CITY_HALL)

        assertTrue(result.candidates.isEmpty())
        assertEquals("closedNear", result.nearestFallback?.hospital?.id)
        assertEquals(OpenStatus.CLOSED, result.nearestFallback?.status)
    }

    @Test
    fun `병원이 0곳이면 후보와 대체 병원 모두 없다`() = runTest {
        val result = useCase(emptyList())(CITY_HALL)
        assertTrue(result.candidates.isEmpty())
        assertNull(result.nearestFallback)
    }

    @Test
    fun `rank는 CLOSED를 제외한다`() {
        val h = hospital("x")
        val ranked = FindEmergencyHospitalsUseCase.rank(
            listOf(
                HospitalCandidate(h, OpenStatus.CLOSED, 1),
                HospitalCandidate(h, OpenStatus.UNKNOWN, 2),
                HospitalCandidate(h, OpenStatus.OPEN, 9),
            ),
        )
        assertEquals(listOf(OpenStatus.OPEN, OpenStatus.UNKNOWN), ranked.map { it.status })
    }
}
