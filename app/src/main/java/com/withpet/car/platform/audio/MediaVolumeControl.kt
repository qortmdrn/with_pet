package com.withpet.car.platform.audio

/** 펫 모드의 볼륨 낮추기. Pleos에서는 차량 볼륨을 바꿀 수 없어 앱 재생 볼륨([com.withpet.car.media.AppPlaybackVolume])만 바꾼다. */
interface MediaVolumeControl {
    suspend fun setMediaVolumePercent(percent: Int): Result<Unit>
}
