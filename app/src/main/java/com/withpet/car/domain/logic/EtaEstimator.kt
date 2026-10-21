package com.withpet.car.domain.logic

import com.withpet.car.domain.model.GeoPoint
import kotlin.math.ceil

/** 내비게이션 ETA를 받을 수 없을 때 쓰는 직선거리 기반 추정. */
object EtaEstimator {
    const val AVERAGE_SPEED_KMH = 40.0

    fun estimateMinutes(from: GeoPoint, to: GeoPoint): Int =
        ceil(from.distanceKmTo(to) / AVERAGE_SPEED_KMH * 60).toInt().coerceAtLeast(1)
}
