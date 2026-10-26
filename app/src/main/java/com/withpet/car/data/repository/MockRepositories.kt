package com.withpet.car.data.repository

import com.withpet.car.data.mock.MockJsonLoader
import com.withpet.car.domain.model.CalmPlaylist
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.repository.PlaylistRepository
import kotlinx.coroutines.CoroutineDispatcher

class MockPlaylistRepository(loader: MockJsonLoader, io: CoroutineDispatcher) : PlaylistRepository {
    private val cache = Cached(io) { loader.playlists().map { it.toDomain() } }
    override suspend fun getAll(): List<CalmPlaylist> = cache.get()
    override suspend fun get(situation: CalmSituation): CalmPlaylist? = cache.get().find { it.situation == situation }
}
