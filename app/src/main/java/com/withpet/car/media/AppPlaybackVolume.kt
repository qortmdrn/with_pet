package com.withpet.car.media

import com.withpet.car.platform.audio.MediaVolumeControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱이 재생하는 소리(안정 사운드)의 볼륨. [CalmPlaybackService]가 플레이어에 적용한다.
 * 차량 전체 미디어 볼륨은 서드파티 앱이 바꿀 수 없어(docs/pleos-sdk/phone-and-volume.md) 펫 모드는 앱 재생 볼륨만 낮춘다.
 */
@Singleton
class AppPlaybackVolume @Inject constructor() : MediaVolumeControl {
    private val _percent = MutableStateFlow(100)
    val percent: StateFlow<Int> = _percent.asStateFlow()

    override suspend fun setMediaVolumePercent(percent: Int): Result<Unit> {
        _percent.value = percent.coerceIn(0, 100)
        return Result.success(Unit)
    }
}
