package com.withpet.car.data.mock

import com.withpet.car.domain.model.CalmPlaylist
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.model.CalmTrack
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.Hospital
import com.withpet.car.domain.model.OpeningHours
import com.withpet.car.domain.model.WalkSpot
import com.withpet.car.domain.model.WalkSpotType
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalTime

@Serializable
data class HospitalDto(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val phone: String,
    val is24Hours: Boolean,
    val weeklyHours: List<OpeningHoursDto> = emptyList(),
    val isPartner: Boolean = false,
) {
    fun toDomain() = Hospital(
        id = id,
        name = name,
        location = GeoPoint(lat, lng),
        address = address,
        phone = phone,
        is24Hours = is24Hours,
        weeklyHours = weeklyHours.map { it.toDomain() },
        isPartner = isPartner,
    )
}

@Serializable
data class OpeningHoursDto(val dayOfWeek: String, val open: String, val close: String) {
    fun toDomain() = OpeningHours(DayOfWeek.valueOf(dayOfWeek), LocalTime.parse(open), LocalTime.parse(close))
}

@Serializable
data class WalkSpotDto(
    val id: String,
    val name: String,
    val type: String,
    val lat: Double,
    val lng: Double,
    val leashRequired: Boolean,
    val hasWasteBin: Boolean,
    val hasWater: Boolean,
    val largeDogAllowed: Boolean,
) {
    fun toDomain() = WalkSpot(
        id = id,
        name = name,
        type = WalkSpotType.valueOf(type),
        location = GeoPoint(lat, lng),
        leashRequired = leashRequired,
        hasWasteBin = hasWasteBin,
        hasWater = hasWater,
        largeDogAllowed = largeDogAllowed,
    )
}

@Serializable
data class PlaylistDto(val situation: String, val title: String, val tracks: List<TrackDto>) {
    fun toDomain() = CalmPlaylist(CalmSituation.valueOf(situation), title, tracks.map { it.toDomain() })
}

@Serializable
data class TrackDto(val id: String, val title: String, val rawResName: String) {
    fun toDomain() = CalmTrack(id, title, rawResName)
}
