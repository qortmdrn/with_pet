package com.withpet.car.platform.voice

import com.withpet.car.domain.model.VoiceCommand
import kotlinx.coroutines.flow.Flow

// MVP: 정해진 명령어만. LLM 사용 안 함
interface VoiceCommandSource {
    fun commands(): Flow<VoiceCommand>
}
