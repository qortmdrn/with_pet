package com.withpet.car.data.repository

import com.withpet.car.data.mock.MockJsonLoader
import com.withpet.car.domain.model.CalmPlaylist
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.model.Hospital
import com.withpet.car.domain.model.WalkSpot
import com.withpet.car.domain.repository.HospitalRepository
import com.withpet.car.domain.repository.PlaylistRepository
import com.withpet.car.domain.repository.WalkSpotRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 한 번 읽은 뒤 메모리에 보관한다. 서버 연동 시 이 클래스만 교체한다. */
private class Cached<T>(private val io: CoroutineDispatcher, private val load: () -> T) {
    private val mutex = Mutex()
    private var value: T? = null

    suspend fun get(): T = mutex.withLock {
        value ?: withContext(io) { load() }.also { value = it }
    }
}

class MockHospitalRepository(loader: MockJsonLoader, io: CoroutineDispatcher) : HospitalRepository {
    private val cache = Cached(io) { loader.hospitals().map { it.toDomain() } }
    override suspend fun getAll(): List<Hospital> = cache.get()
}

class MockWalkSpotRepository(loader: MockJsonLoader, io: CoroutineDispatcher) : WalkSpotRepository {
    private val cache = Cached(io) { loader.walkSpots().map { it.toDomain() } }
    override suspend fun getAll(): List<WalkSpot> = cache.get()
}

class MockPlaylistRepository(loader: MockJsonLoader, io: CoroutineDispatcher) : PlaylistRepository {
    private val cache = Cached(io) { loader.playlists().map { it.toDomain() } }
    override suspend fun getAll(): List<CalmPlaylist> = cache.get()
    override suspend fun get(situation: CalmSituation): CalmPlaylist? = cache.get().find { it.situation == situation }
}
