package com.withpet.car.platform

import com.withpet.car.domain.model.UnsupportedFeature
import com.withpet.car.domain.model.VoiceCommand
import com.withpet.car.platform.audio.MediaVolumeControl
import com.withpet.car.platform.phone.PhoneDialer
import com.withpet.car.platform.voice.SpeechOutput
import com.withpet.car.platform.voice.VoiceCommandSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

// Pleos 공개 SDK에 아직 대응 API가 없거나 연동 방식이 확인되지 않은 기능 (명세 13장).
// UI는 미지원 안내로 대체한다.

private fun <T> unsupported(feature: String): Result<T> =
    Result.failure(UnsupportedFeature("$feature: Pleos 연동 방식 확인 필요"))

/** 블루투스 휴대폰 발신 방식 확인 필요. 응급 화면은 번호를 크게 보여 준다. */
class PleosPhoneDialer @Inject constructor() : PhoneDialer {
    override fun isCallCapable(): Flow<Boolean> = flowOf(false)
    override suspend fun call(phoneNumber: String) = unsupported<Unit>("전화")
}

/** TODO: Gleo AI SpeechToText SDK로 정해진 명령어 인식 연동 */
class PleosVoiceCommandSource @Inject constructor() : VoiceCommandSource {
    override fun commands(): Flow<VoiceCommand> = emptyFlow()
}

/** TODO: Gleo AI TextToSpeech SDK 연동 */
class PleosSpeechOutput @Inject constructor() : SpeechOutput {
    override fun isAvailable() = false
    override suspend fun speak(text: String) = unsupported<Unit>("음성 안내")
}

/** AudioManager와 차량 API 중 볼륨 제어 방식 확인 필요 */
class PleosMediaVolumeControl @Inject constructor() : MediaVolumeControl {
    override suspend fun setMediaVolumePercent(percent: Int) = unsupported<Unit>("볼륨")
}
