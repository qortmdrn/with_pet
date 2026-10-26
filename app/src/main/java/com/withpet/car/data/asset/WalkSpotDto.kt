package com.withpet.car.data.asset

import com.withpet.car.domain.logic.OpeningHoursParser
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.WalkSpot
import com.withpet.car.domain.model.WalkSpotType
import kotlinx.serialization.Serializable

/** assets/data/walk_spots.json 한 항목. scripts/build_public_data.py가 만든다. */
@Serializable
data class WalkSpotDto(
    val id: String,
    val name: String,
    val type: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val leashRequired: Boolean = false,
    val sizeLimit: String? = null,
    val restriction: String? = null,
    val fee: String? = null,
    val hours: String? = null,
    val closedDays: String? = null,
) {
    fun toDomain(): WalkSpot {
        val schedule = OpeningHoursParser.parse(hours, closedDays)
        return WalkSpot(
            id = id,
            name = name,
            type = WalkSpotType.valueOf(type),
            location = GeoPoint(lat, lng),
            address = address,
            leashRequired = leashRequired,
            sizeLimit = sizeLimit,
            restriction = restriction,
            fee = fee,
            hoursText = hours,
            closedDaysText = closedDays,
            weeklyHours = schedule?.weekly.orEmpty(),
            closedOnHolidays = schedule?.closedOnHolidays.orEmpty(),
        )
    }
}
