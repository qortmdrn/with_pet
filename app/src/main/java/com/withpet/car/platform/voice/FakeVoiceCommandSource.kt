package com.withpet.car.platform.voice

import com.withpet.car.domain.model.VoiceCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 음성 인식 대신 디버그 메뉴 버튼으로 명령을 보낸다. */
@Singleton
class FakeVoiceCommandSource @Inject constructor() : VoiceCommandSource {
    private val commands = MutableSharedFlow<VoiceCommand>(extraBufferCapacity = 8)

    override fun commands(): Flow<VoiceCommand> = commands.asSharedFlow()

    fun send(command: VoiceCommand) {
        commands.tryEmit(command)
    }
}
