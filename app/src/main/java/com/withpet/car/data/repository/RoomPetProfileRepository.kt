package com.withpet.car.data.repository

import com.withpet.car.data.local.DataStoreSettingsRepository
import com.withpet.car.data.local.PetProfileDao
import com.withpet.car.data.local.PetProfileEntity
import com.withpet.car.domain.model.PetProfile
import com.withpet.car.domain.repository.PetProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RoomPetProfileRepository(
    private val dao: PetProfileDao,
    private val settings: DataStoreSettingsRepository,
) : PetProfileRepository {

    override fun observeAll(): Flow<List<PetProfile>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeRepresentative(): Flow<PetProfile?> =
        combine(observeAll(), settings.representativePetId()) { pets, id ->
            pets.find { it.id == id } ?: pets.firstOrNull()
        }

    override fun observeRepresentativeId(): Flow<Long?> = observeRepresentative().map { it?.id }

    override suspend fun get(id: Long): PetProfile? = dao.get(id)?.toDomain()

    override suspend fun save(profile: PetProfile): Long {
        val rowId = dao.upsert(PetProfileEntity.from(profile))
        // @Upsert는 갱신 시 -1을 돌려준다
        return if (profile.id != 0L) profile.id else rowId
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
        if (settings.representativePetId().first() == id) settings.setRepresentativePetId(null)
    }

    override suspend fun setRepresentative(id: Long) = settings.setRepresentativePetId(id)
}
