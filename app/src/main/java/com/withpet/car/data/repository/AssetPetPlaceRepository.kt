package com.withpet.car.data.repository

import com.withpet.car.data.asset.PetPlaceDto
import com.withpet.car.domain.model.PetPlace
import com.withpet.car.domain.repository.PetPlaceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json

/** 앱에 포함된 공공데이터(assets/data/pet_places.json)를 한 번 읽어 메모리에 둔다. */
class AssetPetPlaceRepository(json: Json, readText: () -> String, io: CoroutineDispatcher) : PetPlaceRepository {
    private val cache = Cached(io) { json.decodeFromString<List<PetPlaceDto>>(readText()).map { it.toDomain() } }
    override suspend fun getAll(): List<PetPlace> = cache.get()

    companion object {
        const val ASSET_PATH = "data/pet_places.json"
    }
}
