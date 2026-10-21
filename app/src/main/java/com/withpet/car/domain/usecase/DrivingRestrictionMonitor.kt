package com.withpet.car.domain.usecase

import com.withpet.car.di.ApplicationScope
import com.withpet.car.domain.model.DrivingState
import com.withpet.car.platform.driving.DrivingStateProvider
import com.withpet.car.platform.driving.UxRestrictionsSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 화면이 주행 중 규칙(명세 7장 공통 UI 규칙)을 적용해야 하는지.
 * 시스템 주행 제한(CarUxRestrictions)과 앱 자체 주행 상태 중 하나라도 해당하면 제한한다.
 */
@Singleton
class DrivingRestrictionMonitor @Inject constructor(
    driving: DrivingStateProvider,
    uxRestrictions: UxRestrictionsSource,
    @ApplicationScope scope: CoroutineScope,
) {
    val isRestricted: StateFlow<Boolean> = combine(
        driving.state().onStart { emit(DrivingState.PARKED) },
        uxRestrictions.requiresDistractionOptimization().onStart { emit(false) },
    ) { state, systemRequiresDo -> state == DrivingState.DRIVING || systemRequiresDo }
        .stateIn(scope, SharingStarted.Eagerly, false)
}
