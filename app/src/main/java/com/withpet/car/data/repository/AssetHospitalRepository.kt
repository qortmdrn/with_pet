package com.withpet.car.data.repository

import com.withpet.car.data.asset.HospitalDto
import com.withpet.car.domain.model.Hospital
import com.withpet.car.domain.repository.HospitalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json

/** 앱에 포함된 공공데이터(assets/data/hospitals.json)를 한 번 읽어 메모리에 둔다. */
class AssetHospitalRepository(json: Json, readText: () -> String, io: CoroutineDispatcher) : HospitalRepository {
    private val cache = Cached(io) { json.decodeFromString<List<HospitalDto>>(readText()).map { it.toDomain() } }
    override suspend fun getAll(): List<Hospital> = cache.get()

    companion object {
        const val ASSET_PATH = "data/hospitals.json"
    }
}
