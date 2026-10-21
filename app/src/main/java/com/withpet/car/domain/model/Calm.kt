package com.withpet.car.domain.model

enum class CalmSituation { DEPARTURE, LONG_DRIVE, TO_HOSPITAL, RETURN_HOME }

data class CalmTrack(
    val id: String,
    val title: String,
    /** res/raw 리소스 이름 */
    val rawResName: String,
)

data class CalmPlaylist(
    val situation: CalmSituation,
    val title: String,
    val tracks: List<CalmTrack>,
)

data class CalmPlaybackState(
    val situation: CalmSituation? = null,
    val trackTitle: String? = null,
    val isPlaying: Boolean = false,
)
