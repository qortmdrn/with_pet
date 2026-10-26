package com.withpet.car.data

import android.util.Log
import com.withpet.car.di.ApplicationScope
import com.withpet.car.domain.repository.HospitalRepository
import com.withpet.car.domain.repository.PetPlaceRepository
import com.withpet.car.domain.repository.WalkSpotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 공공데이터(병원 약 4,500곳·동반 장소 약 1,900곳·산책 스팟 약 560곳)를 앱 시작 때 미리 읽는다.
 * 응급 버튼을 처음 눌렀을 때 JSON 파싱을 기다리지 않게 한다 (응급 목록 2초 이내).
 */
@Singleton
class DataPreloader @Inject constructor(
    private val hospitals: HospitalRepository,
    private val places: PetPlaceRepository,
    private val walkSpots: WalkSpotRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            // 응급 데이터 먼저
            runCatching { hospitals.getAll() }.onFailure { Log.e(TAG, "병원 데이터 로드 실패", it) }
            runCatching { places.getAll() }.onFailure { Log.e(TAG, "동반 장소 데이터 로드 실패", it) }
            runCatching { walkSpots.getAll() }.onFailure { Log.e(TAG, "산책 스팟 데이터 로드 실패", it) }
        }
    }

    private companion object {
        const val TAG = "DataPreloader"
    }
}
