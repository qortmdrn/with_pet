package com.withpet.car.platform.voice

import com.withpet.car.domain.model.VoiceCommand
import kotlinx.coroutines.flow.Flow

// MVP: 정해진 명령어만. LLM 사용 안 함
interface VoiceCommandSource {
    fun commands(): Flow<VoiceCommand>

    /**
     * 앱이 질문을 읽어 준 직후 "응/아니" 같은 짧은 답을 한 번 듣는다. 알아들으면 [commands]로 나온다.
     * 앱이 듣기를 시작하는 방식만 지원하는 플랫폼(Pleos STT)용. 기본은 아무것도 하지 않는다.
     */
    suspend fun listenForAnswer() {}
}
