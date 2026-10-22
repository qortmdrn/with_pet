package com.withpet.car.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class Hospital(
    val id: String,
    val name: String,
    val location: GeoPoint,
    val address: String,
    val phone: String?,                     // 없으면 전화 대신 길 안내만
    val is24Hours: Boolean,
    val weeklyHours: List<OpeningHours>,   // 비어 있으면 진료 시간 미확인
    val isPartner: Boolean,
)

/** close가 open보다 이르거나 같으면 자정을 넘겨 다음 날 close까지 영업한다. */
data class OpeningHours(val dayOfWeek: DayOfWeek, val open: LocalTime, val close: LocalTime)

enum class OpenStatus { OPEN, CLOSED, UNKNOWN }

data class HospitalCandidate(
    val hospital: Hospital,
    val status: OpenStatus,
    val etaMinutes: Int,
)
