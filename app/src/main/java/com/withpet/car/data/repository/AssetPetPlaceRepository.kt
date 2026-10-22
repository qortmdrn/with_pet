package com.withpet.car.data.repository

import com.withpet.car.data.asset.PetPlaceDto
import com.withpet.car.domain.model.PetPlace
import com.withpet.car.domain.repository.PetPlaceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** 앱에 포함된 공공데이터(assets/data/pet_places.json)를 한 번 읽어 메모리에 둔다. */
class AssetPetPlaceRepository(
    private val json: Json,
    private val readText: () -> String,
    private val io: CoroutineDispatcher,
) : PetPlaceRepository {

    private val mutex = Mutex()
    private var cache: List<PetPlace>? = null

    override suspend fun getAll(): List<PetPlace> = mutex.withLock {
        cache ?: withContext(io) {
            json.decodeFromString<List<PetPlaceDto>>(readText()).map { it.toDomain() }
        }.also { cache = it }
    }

    companion object {
        const val ASSET_PATH = "data/pet_places.json"
    }
}
