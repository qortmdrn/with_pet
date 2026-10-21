package com.withpet.car.platform.driving

import com.withpet.car.di.ApplicationScope
import com.withpet.car.domain.logic.ContinuousDrivingTracker
import com.withpet.car.domain.model.DrivingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** 디버그 메뉴에서 주행 상태와 연속 주행 시간을 조작한다. 그 외에는 실제 시간에 따라 흐른다. */
@Singleton
class FakeDrivingStateProvider @Inject constructor(
    private val clock: Clock,
    @ApplicationScope scope: CoroutineScope,
) : DrivingStateProvider {

    private val tracker = ContinuousDrivingTracker()
    private val drivingState = MutableStateFlow(DrivingState.PARKED)
    private val minutes = MutableStateFlow(0)

    val currentState: StateFlow<DrivingState> = drivingState.asStateFlow()
    val currentMinutes: StateFlow<Int> = minutes.asStateFlow()

    init {
        scope.launch {
            while (true) {
                tick()
                delay(TICK_MS)
            }
        }
    }

    override fun state(): Flow<DrivingState> = drivingState.asStateFlow()
    override fun continuousDrivingMinutes(): Flow<Int> = minutes.asStateFlow()

    @Synchronized
    fun setState(state: DrivingState) {
        tracker.update(clock.instant(), drivingState.value)
        drivingState.value = state
        minutes.value = tracker.update(clock.instant(), state)
    }

    @Synchronized
    fun setContinuousDrivingMinutes(value: Int) {
        tracker.update(clock.instant(), drivingState.value)
        tracker.override(value)
        minutes.value = tracker.minutes
    }

    @Synchronized
    private fun tick() {
        minutes.value = tracker.update(clock.instant(), drivingState.value)
    }

    private companion object {
        const val TICK_MS = 10_000L
    }
}
