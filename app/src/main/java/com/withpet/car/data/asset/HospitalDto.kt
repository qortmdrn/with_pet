package com.withpet.car.data.asset

import com.withpet.car.domain.logic.OpeningHoursParser
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.Hospital
import com.withpet.car.domain.model.OpeningHours
import kotlinx.serialization.Serializable
import java.time.LocalTime

/** assets/data/hospitals.json 한 항목. scripts/build_public_data.py가 만든다. */
@Serializable
data class HospitalDto(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val phone: String? = null,
    val hours: String? = null,
    val closedDays: String? = null,
    val updatedAt: String = "",
) {
    fun toDomain(): Hospital {
        // 응급 모드: 특정 주에만 쉬는 병원은 진료 중으로 단정하지 않는다
        val schedule = OpeningHoursParser.parse(hours, closedDays, strict = true)
        val weekly = schedule?.weekly.orEmpty()
        return Hospital(
            id = id,
            name = name,
            location = GeoPoint(lat, lng),
            address = address,
            phone = phone,
            is24Hours = weekly.size == 7 && weekly.all { it.isAllDay },
            weeklyHours = weekly,
            isPartner = false,
            closedOnHolidays = schedule?.closedOnHolidays.orEmpty(),
        )
    }

    private val OpeningHours.isAllDay get() = open == LocalTime.MIDNIGHT && close == LocalTime.MIDNIGHT
}
