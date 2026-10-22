package com.withpet.car.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.model.PetModeSettings
import com.withpet.car.domain.model.RestAlertSettings
import com.withpet.car.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun petModeSettings(): Flow<PetModeSettings> = dataStore.data.map { p ->
        val d = PetModeSettings()
        PetModeSettings(
            targetTempC = p[TARGET_TEMP] ?: d.targetTempC,
            fanLevel = p[FAN_LEVEL] ?: d.fanLevel,
            rearWindowOpenPercent = p[WINDOW_PERCENT] ?: d.rearWindowOpenPercent,
            mediaVolumePercent = p[VOLUME_PERCENT] ?: d.mediaVolumePercent,
            autoPlayCalmSound = p[AUTO_PLAY] ?: d.autoPlayCalmSound,
        )
    }.distinctUntilChanged()

    override fun restAlertSettings(): Flow<RestAlertSettings> = dataStore.data.map { p ->
        val d = RestAlertSettings()
        RestAlertSettings(
            enabled = p[REST_ENABLED] ?: d.enabled,
            intervalMinutes = p[REST_INTERVAL] ?: d.intervalMinutes,
        )
    }.distinctUntilChanged()

    override suspend fun updatePetModeSettings(settings: PetModeSettings) {
        dataStore.edit { p ->
            p[TARGET_TEMP] = settings.targetTempC
            p[FAN_LEVEL] = settings.fanLevel
            p[WINDOW_PERCENT] = settings.rearWindowOpenPercent
            p[VOLUME_PERCENT] = settings.mediaVolumePercent
            p[AUTO_PLAY] = settings.autoPlayCalmSound
        }
    }

    override suspend fun updateRestAlertSettings(settings: RestAlertSettings) {
        dataStore.edit { p ->
            p[REST_ENABLED] = settings.enabled
            p[REST_INTERVAL] = settings.intervalMinutes
        }
    }

    override fun lastCalmSituation(): Flow<CalmSituation?> = dataStore.data.map { p ->
        p[LAST_CALM_SITUATION]?.let { name -> CalmSituation.entries.find { it.name == name } }
    }.distinctUntilChanged()

    override suspend fun setLastCalmSituation(situation: CalmSituation) {
        dataStore.edit { p -> p[LAST_CALM_SITUATION] = situation.name }
    }

    fun representativePetId(): Flow<Long?> = dataStore.data.map { it[REPRESENTATIVE_PET] }.distinctUntilChanged()

    suspend fun setRepresentativePetId(id: Long?) {
        dataStore.edit { p -> if (id == null) p.remove(REPRESENTATIVE_PET) else p[REPRESENTATIVE_PET] = id }
    }

    private companion object {
        val TARGET_TEMP = floatPreferencesKey("pet_mode_target_temp_c")
        val FAN_LEVEL = intPreferencesKey("pet_mode_fan_level")
        val WINDOW_PERCENT = intPreferencesKey("pet_mode_rear_window_percent")
        val VOLUME_PERCENT = intPreferencesKey("pet_mode_media_volume_percent")
        val AUTO_PLAY = booleanPreferencesKey("pet_mode_auto_play_calm")
        val REST_ENABLED = booleanPreferencesKey("rest_alert_enabled")
        val REST_INTERVAL = intPreferencesKey("rest_alert_interval_minutes")
        val REPRESENTATIVE_PET = longPreferencesKey("representative_pet_id")
        val LAST_CALM_SITUATION = stringPreferencesKey("last_calm_situation")
    }
}
