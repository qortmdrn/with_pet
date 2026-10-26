package com.withpet.car.data.mock

import com.withpet.car.domain.model.CalmPlaylist
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.model.CalmTrack
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDto(val situation: String, val title: String, val tracks: List<TrackDto>) {
    fun toDomain() = CalmPlaylist(CalmSituation.valueOf(situation), title, tracks.map { it.toDomain() })
}

@Serializable
data class TrackDto(val id: String, val title: String, val rawResName: String) {
    fun toDomain() = CalmTrack(id, title, rawResName)
}
