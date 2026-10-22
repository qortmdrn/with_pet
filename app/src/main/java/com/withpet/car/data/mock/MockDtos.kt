package com.withpet.car.data.mock

import com.withpet.car.domain.model.CalmPlaylist
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.model.CalmTrack
import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.WalkSpot
import com.withpet.car.domain.model.WalkSpotType
import kotlinx.serialization.Serializable

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
