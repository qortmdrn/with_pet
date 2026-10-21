package com.withpet.car.platform.driving

import kotlinx.coroutines.flow.Flow

/** 시스템 주행 제한 상태 (Android Automotive CarUxRestrictionsManager). */
interface UxRestrictionsSource {
    /** true면 시스템이 주행 중 주의 분산 최적화(DO)를 요구한다. */
    fun requiresDistractionOptimization(): Flow<Boolean>
}
