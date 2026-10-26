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
import kotlinx.coroutines.withTimeoutOrNull
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
            val estimated = candidatesWithin(hospitals, origin, radius, now)
            if (estimated.isNotEmpty()) return EmergencyResult(rank(refineEta(estimated)), radius, null)
        }

        val nearest = hospitals.minByOrNull { origin.distanceKmTo(it.location) }?.let {
            HospitalCandidate(it, OpenStatusCalculator.statusAt(it, now), EtaEstimator.estimateMinutes(origin, it.location))
        }
        return EmergencyResult(emptyList(), EXPANDED_RADIUS_KM, nearest)
    }

    /** 반경 안의 닫히지 않은 병원. ETA는 직선거리 추정. */
    private fun candidatesWithin(
        hospitals: List<Hospital>,
        origin: GeoPoint,
        radiusKm: Double,
        now: LocalDateTime,
    ): List<HospitalCandidate> =
        hospitals
            .filter { origin.distanceKmTo(it.location) <= radiusKm }
            .map { HospitalCandidate(it, OpenStatusCalculator.statusAt(it, now), EtaEstimator.estimateMinutes(origin, it.location)) }
            .filter { it.status != OpenStatus.CLOSED }

    /**
     * 추정 순위 상위 [ETA_REFINE_COUNT]곳만 내비 ETA로 다시 계산한다.
     * 전부 [ETA_BUDGET_MS] 안에 와야 쓴다: 내비 ETA(교통 반영)와 직선거리 추정을 섞어 비교하면 순서가 틀어진다.
     * Pleos 내비는 ETA를 한 번에 하나씩 계산하므로 후보 수를 줄여 응급 목록 2초 이내를 지킨다.
     */
    private suspend fun refineEta(estimated: List<HospitalCandidate>): List<HospitalCandidate> = coroutineScope {
        val top = estimated.sortedWith(ORDER).take(ETA_REFINE_COUNT)
        val etas = withTimeoutOrNull(ETA_BUDGET_MS) {
            top.map { async { navigation.estimateEtaMinutes(it.hospital.location).getOrNull() } }.awaitAll()
        }
        if (etas == null || etas.any { it == null }) top
        else top.zip(etas) { candidate, eta -> candidate.copy(etaMinutes = eta!!) }
    }

    companion object {
        const val PRIMARY_RADIUS_KM = 20.0
        const val EXPANDED_RADIUS_KM = 50.0
        const val MAX_CANDIDATES = 3
        /** 내비 ETA로 다시 계산할 후보 수. 직선거리 순위와 실제 순위가 크게 어긋나지 않는다는 가정 */
        const val ETA_REFINE_COUNT = 5
        const val ETA_BUDGET_MS = 1_500L

        private val ORDER = compareBy<HospitalCandidate>({ it.status != OpenStatus.OPEN }, { it.etaMinutes })

        /** OPEN 먼저 → UNKNOWN, CLOSED 제외. 같은 그룹 안에서는 ETA 오름차순. 최대 3곳. */
        fun rank(candidates: List<HospitalCandidate>): List<HospitalCandidate> =
            candidates
                .filter { it.status != OpenStatus.CLOSED }
                .sortedWith(ORDER)
                .take(MAX_CANDIDATES)
    }
}
