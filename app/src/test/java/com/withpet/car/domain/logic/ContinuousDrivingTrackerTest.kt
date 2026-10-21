package com.withpet.car.domain.logic

import com.withpet.car.domain.model.DrivingState.DRIVING
import com.withpet.car.domain.model.DrivingState.PARKED
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class ContinuousDrivingTrackerTest {

    private val t0 = Instant.parse("2026-10-22T00:00:00Z")
    private fun min(m: Long) = t0.plusSeconds(m * 60)

    @Test
    fun `주행 시간만 누적한다`() {
        val tracker = ContinuousDrivingTracker()
        tracker.update(min(0), DRIVING)
        assertEquals(30, tracker.update(min(30), DRIVING))
        tracker.update(min(30), PARKED)
        assertEquals(30, tracker.update(min(35), PARKED))      // 5분 정차: 유지
        tracker.update(min(35), DRIVING)
        assertEquals(45, tracker.update(min(50), DRIVING))
    }

    @Test
    fun `정차 10분 이상이면 초기화`() {
        val tracker = ContinuousDrivingTracker()
        tracker.update(min(0), DRIVING)
        tracker.update(min(60), PARKED)
        assertEquals(60, tracker.update(min(69), PARKED))
        assertEquals(0, tracker.update(min(70), PARKED))
    }

    @Test
    fun `디버그 지정값에서 이어서 누적한다`() {
        val tracker = ContinuousDrivingTracker()
        tracker.update(min(0), DRIVING)
        tracker.override(88)
        assertEquals(90, tracker.update(min(2), DRIVING))
    }
}
