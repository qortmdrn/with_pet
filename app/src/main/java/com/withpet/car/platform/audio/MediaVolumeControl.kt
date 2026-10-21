package com.withpet.car.platform.audio

/** 미디어 볼륨 제어. AudioManager와 차량 API 중 어느 쪽을 쓸지 확인 필요(13장). */
interface MediaVolumeControl {
    suspend fun setMediaVolumePercent(percent: Int): Result<Unit>
}
