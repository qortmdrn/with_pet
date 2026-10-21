package com.withpet.car.domain.usecase

import com.withpet.car.domain.logic.EtaEstimator
import com.withpet.car.domain.logic.OpenStatusCalculator
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.Hospital
import com.withpet.car.domain.model.HospitalCandidate
import com.withpet.car.domain.model.OpenStatus
import com.withpet.car.domain.repository.HospitalRepository
import com.withpet.car.platform.navigation.NavigationController
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

data class EmergencyResult(
    /** 진료 중 → 미확인 순, 같은 그룹은 ETA 순. 최대 3곳. */
    val candidates: List<HospitalCandidate>,
    val searchedRadiusKm: Double,
    /** 반경 50km에도 후보가 없을 때 진료 상태와 무관하게 가장 가까운 병원 */
    val nearestFallback: HospitalCandidate?,
)

class FindEmergencyHospitalsUseCase @Inject constructor(
    private val hospitalRepository: HospitalRepository,
    private val navigation: NavigationController,
    private val clock: Clock,
) {

    suspend operator fun invoke(origin: GeoPoint): EmergencyResult {
        val hospitals = hospitalRepository.getAll()
        val now = LocalDateTime.now(clock)

        for (radius in listOf(PRIMARY_RADIUS_KM, EXPANDED_RADIUS_KM)) {
            val candidates = rank(candidatesWithin(hospitals, origin, radius, now))
            if (candidates.isNotEmpty()) return EmergencyResult(candidates, radius, null)
        }

        val nearest = hospitals.minByOrNull { origin.distanceKmTo(it.location) }?.let {
            HospitalCandidate(it, OpenStatusCalculator.statusAt(it, now), eta(origin, it))
        }
        return EmergencyResult(emptyList(), EXPANDED_RADIUS_KM, nearest)
    }

    private suspend fun candidatesWithin(
        hospitals: List<Hospital>,
        origin: GeoPoint,
        radiusKm: Double,
        now: LocalDateTime,
    ): List<HospitalCandidate> = coroutineScope {
        hospitals
            .filter { origin.distanceKmTo(it.location) <= radiusKm }
            .map { it to OpenStatusCalculator.statusAt(it, now) }
            .filter { (_, status) -> status != OpenStatus.CLOSED }
            .map { (hospital, status) ->
                async { HospitalCandidate(hospital, status, eta(origin, hospital)) }
            }
            .awaitAll()
    }

    private suspend fun eta(origin: GeoPoint, hospital: Hospital): Int =
        runCatching { navigation.estimateEtaMinutes(hospital.location).getOrThrow() }
            .getOrElse { EtaEstimator.estimateMinutes(origin, hospital.location) }

    companion object {
        const val PRIMARY_RADIUS_KM = 20.0
        const val EXPANDED_RADIUS_KM = 50.0
        const val MAX_CANDIDATES = 3

        /** OPEN 먼저 → UNKNOWN, CLOSED 제외. 같은 그룹 안에서는 ETA 오름차순. 최대 3곳. */
        fun rank(candidates: List<HospitalCandidate>): List<HospitalCandidate> =
            candidates
                .filter { it.status != OpenStatus.CLOSED }
                .sortedWith(compareBy({ it.status != OpenStatus.OPEN }, { it.etaMinutes }))
                .take(MAX_CANDIDATES)
    }
}
