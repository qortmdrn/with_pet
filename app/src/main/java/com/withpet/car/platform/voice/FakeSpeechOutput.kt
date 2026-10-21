package com.withpet.car.platform.voice

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeSpeechOutput @Inject constructor() : SpeechOutput {
    override fun isAvailable(): Boolean = true

    override suspend fun speak(text: String): Result<Unit> {
        Log.i("FakeSpeechOutput", "가짜 음성 안내: $text")
        return Result.success(Unit)
    }
}
