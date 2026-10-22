package com.withpet.car.data.repository

import com.withpet.car.data.mock.MockJsonLoader
import com.withpet.car.domain.model.CalmPlaylist
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.model.WalkSpot
import com.withpet.car.domain.repository.PlaylistRepository
import com.withpet.car.domain.repository.WalkSpotRepository
import kotlinx.coroutines.CoroutineDispatcher

class MockWalkSpotRepository(loader: MockJsonLoader, io: CoroutineDispatcher) : WalkSpotRepository {
    private val cache = Cached(io) { loader.walkSpots().map { it.toDomain() } }
    override suspend fun getAll(): List<WalkSpot> = cache.get()
}

class MockPlaylistRepository(loader: MockJsonLoader, io: CoroutineDispatcher) : PlaylistRepository {
    private val cache = Cached(io) { loader.playlists().map { it.toDomain() } }
    override suspend fun getAll(): List<CalmPlaylist> = cache.get()
    override suspend fun get(situation: CalmSituation): CalmPlaylist? = cache.get().find { it.situation == situation }
}
