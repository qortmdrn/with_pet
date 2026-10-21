package com.withpet.car.platform.driving

import com.withpet.car.di.ApplicationScope
import com.withpet.car.domain.logic.ContinuousDrivingTracker
import com.withpet.car.domain.model.DrivingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pleos 공개 Vehicle SDK에는 주행 여부를 바로 알려 주는 API가 없어,
 * 시스템 주행 제한(CarUxRestrictions의 DO 요구)을 주행 중으로 본다.
 */
@Singleton
class PleosDrivingStateProvider @Inject constructor(
    uxRestrictions: UxRestrictionsSource,
    private val clock: Clock,
    @ApplicationScope scope: CoroutineScope,
) : DrivingStateProvider {

    private val tracker = ContinuousDrivingTracker()
    private val minutes = MutableStateFlow(0)

    private val drivingState: StateFlow<DrivingState> = uxRestrictions.requiresDistractionOptimization()
        .map { if (it) DrivingState.DRIVING else DrivingState.PARKED }
        .stateIn(scope, SharingStarted.Eagerly, DrivingState.PARKED)

    init {
        scope.launch {
            while (true) {
                minutes.value = synchronized(tracker) { tracker.update(clock.instant(), drivingState.value) }
                delay(TICK_MS)
            }
        }
    }

    override fun state(): Flow<DrivingState> = drivingState
    override fun continuousDrivingMinutes(): Flow<Int> = minutes.asStateFlow()

    private companion object {
        const val TICK_MS = 10_000L
    }
}
