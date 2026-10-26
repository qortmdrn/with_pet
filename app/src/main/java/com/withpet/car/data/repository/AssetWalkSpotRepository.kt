package com.withpet.car.data.repository

import com.withpet.car.data.asset.WalkSpotDto
import com.withpet.car.domain.model.WalkSpot
import com.withpet.car.domain.repository.WalkSpotRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json

/** 앱에 포함된 공공데이터(assets/data/walk_spots.json)를 한 번 읽어 메모리에 둔다. */
class AssetWalkSpotRepository(json: Json, readText: () -> String, io: CoroutineDispatcher) : WalkSpotRepository {
    private val cache = Cached(io) { json.decodeFromString<List<WalkSpotDto>>(readText()).map { it.toDomain() } }
    override suspend fun getAll(): List<WalkSpot> = cache.get()

    companion object {
        const val ASSET_PATH = "data/walk_spots.json"
    }
}
