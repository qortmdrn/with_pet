package com.withpet.car.platform.voice

/** 휴식 알림 문장을 읽어 주는 TTS. Gleo AI 연동 방식은 확인 필요(13장). */
interface SpeechOutput {
    fun isAvailable(): Boolean
    suspend fun speak(text: String): Result<Unit>
}
