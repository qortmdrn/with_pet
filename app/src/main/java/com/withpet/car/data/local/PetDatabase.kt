package com.withpet.car.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import com.withpet.car.domain.model.PetProfile
import com.withpet.car.domain.model.Species
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pet_profile")
data class PetProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val species: String,
    val breed: String?,
    val birthYear: Int?,
    val weightKg: Float?,
    val photoUri: String?,
) {
    fun toDomain() = PetProfile(
        id = id,
        name = name,
        species = runCatching { Species.valueOf(species) }.getOrDefault(Species.OTHER),
        breed = breed,
        birthYear = birthYear,
        weightKg = weightKg,
        photoUri = photoUri,
    )

    companion object {
        fun from(profile: PetProfile) = PetProfileEntity(
            id = profile.id,
            name = profile.name,
            species = profile.species.name,
            breed = profile.breed,
            birthYear = profile.birthYear,
            weightKg = profile.weightKg,
            photoUri = profile.photoUri,
        )
    }
}

@Dao
interface PetProfileDao {
    @Query("SELECT * FROM pet_profile ORDER BY id")
    fun observeAll(): Flow<List<PetProfileEntity>>

    @Query("SELECT * FROM pet_profile WHERE id = :id")
    suspend fun get(id: Long): PetProfileEntity?

    @Upsert
    suspend fun upsert(entity: PetProfileEntity): Long

    @Query("DELETE FROM pet_profile WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [PetProfileEntity::class], version = 1, exportSchema = true)
abstract class PetDatabase : RoomDatabase() {
    abstract fun petProfileDao(): PetProfileDao
}
