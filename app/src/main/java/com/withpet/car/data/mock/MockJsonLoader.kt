package com.withpet.car.data.mock

import kotlinx.serialization.json.Json

/** assets/mock 아래 JSON 파일을 읽는다. 파일 접근은 [readText]로 주입해 JVM 테스트에서도 쓴다. */
class MockJsonLoader(
    private val json: Json,
    private val readText: (fileName: String) -> String,
) {
    fun hospitals(): List<HospitalDto> = json.decodeFromString(readText(HOSPITALS))
    fun walkSpots(): List<WalkSpotDto> = json.decodeFromString(readText(WALK_SPOTS))
    fun playlists(): List<PlaylistDto> = json.decodeFromString(readText(PLAYLISTS))

    companion object {
        const val DIR = "mock"
        const val HOSPITALS = "hospitals.json"
        const val WALK_SPOTS = "walk_spots.json"
        const val PLAYLISTS = "playlists.json"
    }
}
