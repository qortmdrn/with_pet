package com.withpet.car.platform.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeMediaVolumeControl @Inject constructor() : MediaVolumeControl {
    private val volume = MutableStateFlow(60)
    val currentVolume: StateFlow<Int> = volume.asStateFlow()

    override suspend fun setMediaVolumePercent(percent: Int): Result<Unit> {
        volume.value = percent.coerceIn(0, 100)
        return Result.success(Unit)
    }
}
