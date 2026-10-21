package com.withpet.car.platform.driving

import com.withpet.car.domain.model.DrivingState
import kotlinx.coroutines.flow.Flow

interface DrivingStateProvider {
    fun state(): Flow<DrivingState>          // PARKED, DRIVING
    fun continuousDrivingMinutes(): Flow<Int>
}
