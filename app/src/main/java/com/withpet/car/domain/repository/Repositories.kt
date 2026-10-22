package com.withpet.car.domain.repository

import com.withpet.car.domain.model.CalmPlaybackState
import com.withpet.car.domain.model.CalmPlaylist
import com.withpet.car.domain.model.CalmSituation
import com.withpet.car.domain.model.Hospital
import com.withpet.car.domain.model.PetModeSettings
import com.withpet.car.domain.model.PetProfile
import com.withpet.car.domain.model.RestAlertSettings
import com.withpet.car.domain.model.WalkSpot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface HospitalRepository {
    suspend fun getAll(): List<Hospital>
}

interface WalkSpotRepository {
    suspend fun getAll(): List<WalkSpot>
}

interface PlaylistRepository {
    suspend fun getAll(): List<CalmPlaylist>
    suspend fun get(situation: CalmSituation): CalmPlaylist?
}

interface PetProfileRepository {
    fun observeAll(): Flow<List<PetProfile>>
    /** 대표로 지정된 반려동물. 지정이 없으면 처음 등록한 반려동물. */
    fun observeRepresentative(): Flow<PetProfile?>
    fun observeRepresentativeId(): Flow<Long?>
    suspend fun get(id: Long): PetProfile?
    /** id가 0이면 새로 만든다. 저장된 id를 돌려준다. */
    suspend fun save(profile: PetProfile): Long
    suspend fun delete(id: Long)
    suspend fun setRepresentative(id: Long)
}

interface SettingsRepository {
    fun petModeSettings(): Flow<PetModeSettings>
    fun restAlertSettings(): Flow<RestAlertSettings>
    suspend fun updatePetModeSettings(settings: PetModeSettings)
    suspend fun updateRestAlertSettings(settings: RestAlertSettings)

    /** 안정 사운드 화면에서 마지막으로 고른 상황. 다음 실행 때 기본 선택으로 쓴다 (명세 7-5). */
    fun lastCalmSituation(): Flow<CalmSituation?>
    suspend fun setLastCalmSituation(situation: CalmSituation)
}

/** 안정 사운드 재생기. 구현은 MediaSessionService와 연결된다. */
interface CalmSoundPlayer {
    val state: StateFlow<CalmPlaybackState>
    suspend fun play(situation: CalmSituation): Result<Unit>
    suspend fun stop()
}
