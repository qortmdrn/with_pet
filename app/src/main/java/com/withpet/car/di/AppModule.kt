package com.withpet.car.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.withpet.car.data.local.DataStoreSettingsRepository
import com.withpet.car.data.local.PetDatabase
import com.withpet.car.data.mock.MockJsonLoader
import com.withpet.car.data.repository.MockHospitalRepository
import com.withpet.car.data.repository.AssetPetPlaceRepository
import com.withpet.car.data.repository.MockPlaylistRepository
import com.withpet.car.data.repository.MockWalkSpotRepository
import com.withpet.car.data.repository.RoomPetProfileRepository
import com.withpet.car.domain.repository.CalmSoundPlayer
import com.withpet.car.domain.repository.HospitalRepository
import com.withpet.car.domain.repository.PetPlaceRepository
import com.withpet.car.domain.repository.PetProfileRepository
import com.withpet.car.domain.repository.PlaylistRepository
import com.withpet.car.domain.repository.SettingsRepository
import com.withpet.car.domain.repository.WalkSpotRepository
import com.withpet.car.media.MediaControllerCalmSoundPlayer
import com.withpet.car.platform.driving.CarUxRestrictionsSource
import com.withpet.car.platform.driving.UxRestrictionsSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import java.time.Clock
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun mockJsonLoader(@ApplicationContext context: Context): MockJsonLoader =
        MockJsonLoader(Json { ignoreUnknownKeys = true }) { fileName ->
            context.assets.open("${MockJsonLoader.DIR}/$fileName").bufferedReader().use { it.readText() }
        }

    @Provides
    @Singleton
    fun hospitalRepository(loader: MockJsonLoader): HospitalRepository =
        MockHospitalRepository(loader, Dispatchers.IO)

    @Provides
    @Singleton
    fun walkSpotRepository(loader: MockJsonLoader): WalkSpotRepository =
        MockWalkSpotRepository(loader, Dispatchers.IO)

    @Provides
    @Singleton
    fun petPlaceRepository(@ApplicationContext context: Context): PetPlaceRepository =
        AssetPetPlaceRepository(Json { ignoreUnknownKeys = true }, {
            context.assets.open(AssetPetPlaceRepository.ASSET_PATH).bufferedReader().use { it.readText() }
        }, Dispatchers.IO)

    @Provides
    @Singleton
    fun playlistRepository(loader: MockJsonLoader): PlaylistRepository =
        MockPlaylistRepository(loader, Dispatchers.IO)

    @Provides
    @Singleton
    fun settingsDataStoreRepository(@ApplicationContext context: Context): DataStoreSettingsRepository =
        DataStoreSettingsRepository(context.settingsDataStore)

    @Provides
    fun settingsRepository(impl: DataStoreSettingsRepository): SettingsRepository = impl

    @Provides
    @Singleton
    fun petDatabase(@ApplicationContext context: Context): PetDatabase =
        Room.databaseBuilder(context, PetDatabase::class.java, "withpet.db").build()

    @Provides
    @Singleton
    fun petProfileRepository(db: PetDatabase, settings: DataStoreSettingsRepository): PetProfileRepository =
        RoomPetProfileRepository(db.petProfileDao(), settings)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindings {
    @Binds
    abstract fun calmSoundPlayer(impl: MediaControllerCalmSoundPlayer): CalmSoundPlayer

    @Binds
    abstract fun uxRestrictionsSource(impl: CarUxRestrictionsSource): UxRestrictionsSource
}
