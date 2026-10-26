package com.withpet.car.data.asset

import com.withpet.car.domain.logic.OpeningHoursParser
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.PetPlace
import com.withpet.car.domain.model.PetPlaceCategory
import kotlinx.serialization.Serializable

/** assets/data/pet_places.json 한 항목. scripts/build_public_data.py가 만든다. */
@Serializable
data class PetPlaceDto(
    val id: String,
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val phone: String? = null,
    val parking: Boolean = false,
    val sizeLimit: String? = null,
    val restriction: String? = null,
    val extraFee: String? = null,
    val hours: String? = null,
    val closedDays: String? = null,
    val updatedAt: String = "",
) {
    fun toDomain(): PetPlace {
        val schedule = OpeningHoursParser.parse(hours, closedDays)
        return PetPlace(
            id = id,
            name = name,
            category = PetPlaceCategory.valueOf(category),
            location = GeoPoint(lat, lng),
            address = address,
            phone = phone,
            hasParking = parking,
            sizeLimit = sizeLimit,
            restriction = restriction,
            extraFee = extraFee,
            hoursText = hours,
            closedDaysText = closedDays,
            weeklyHours = schedule?.weekly.orEmpty(),
            closedOnHolidays = schedule?.closedOnHolidays.orEmpty(),
            updatedAt = updatedAt,
        )
    }
}
