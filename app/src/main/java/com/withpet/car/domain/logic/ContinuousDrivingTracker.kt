package com.withpet.car.domain.logic

import com.withpet.car.domain.model.DrivingState
import java.time.Duration
import java.time.Instant

/**
 * 연속 주행 시간 계산. 정차가 [resetAfterParked] 이상 이어지면 0으로 초기화한다.
 * 짧은 정차(신호 대기 등)는 주행 시간에 더하지 않지만 초기화하지도 않는다.
 */
class ContinuousDrivingTracker(
    private val resetAfterParked: Duration = Duration.ofMinutes(10),
) {
    private var accumulated: Duration = Duration.ZERO
    private var lastUpdate: Instant? = null
    private var lastState: DrivingState = DrivingState.PARKED
    private var parkedSince: Instant? = null

    val minutes: Int get() = accumulated.toMinutes().toInt()

    fun update(now: Instant, state: DrivingState): Int {
        val previous = lastUpdate
        if (previous != null && lastState == DrivingState.DRIVING && now.isAfter(previous)) {
            accumulated += Duration.between(previous, now)
        }
        if (state == DrivingState.PARKED) {
            val since = parkedSince ?: now.also { parkedSince = it }
            if (Duration.between(since, now) >= resetAfterParked) accumulated = Duration.ZERO
        } else {
            parkedSince = null
        }
        lastUpdate = now
        lastState = state
        return minutes
    }

    /** 디버그용: 연속 주행 시간을 직접 지정한다. */
    fun override(minutes: Int) {
        accumulated = Duration.ofMinutes(minutes.toLong().coerceAtLeast(0))
    }
}
