package com.withpet.car.platform.voice

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.content.Context
import android.util.Log
import com.withpet.car.domain.logic.VoiceAnswerMatcher
import com.withpet.car.domain.model.VoiceCommand
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 음성 명령 두 갈래 (docs/pleos-sdk/stt.md, gleo-interface.md)
 * - 응급·펫 모드: Gleo AI가 Intent로 보낸다 → [GleoActionHandler]가 [send]로 넣는다.
 * - 휴식 알림 답(응/아니): 앱이 질문을 읽은 뒤 [listenForAnswer]로 SpeechToText를 한 번 연다.
 *   STT는 앱이 request()를 부를 때만 듣는다 (웨이크업 워드 없음). ON_DEVICE 모드라 앱 등록 불필요.
 */
@Singleton
class PleosVoiceCommandSource @Inject constructor(
    @ApplicationContext context: Context,
) : VoiceCommandSource, ResultListener {

    // 앱이 Gleo Intent로 막 켜졌을 때 화면이 아직 구독 전이어도 명령을 잃지 않도록 버퍼링한다
    private val commands = Channel<VoiceCommand>(Channel.BUFFERED)
    private val stt = SpeechToText(context.applicationContext, Mode.ON_DEVICE)
    private val listenMutex = Mutex()
    @Volatile private var ready = false
    @Volatile private var answer: CompletableDeferred<String?>? = null

    init {
        runCatching {
            stt.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    stt.addListener(this@PleosVoiceCommandSource)
                    stt.setLocale(Locale.KOREA)
                    ready = true
                }

                override fun onFailure(errorType: ErrorType) {
                    Log.w(TAG, "STT 초기화 실패: $errorType")
                }
            })
        }.onFailure { Log.w(TAG, "STT 초기화 실패", it) }
    }

    override fun commands(): Flow<VoiceCommand> = commands.receiveAsFlow()

    fun send(command: VoiceCommand) {
        commands.trySend(command)
    }

    override suspend fun listenForAnswer() {
        if (!ready) return
        listenMutex.withLock {
            val result = CompletableDeferred<String?>()
            answer = result
            try {
                stt.request()
                val text = withTimeoutOrNull(LISTEN_TIMEOUT_MS) { result.await() }
                text?.let(VoiceAnswerMatcher::match)?.let(::send)
            } catch (e: Exception) {
                Log.w(TAG, "음성 인식 실패", e)
            } finally {
                answer = null
                runCatching { stt.stop() }
            }
        }
    }

    // --- ResultListener ---
    override fun onUpdated(stt: String, completed: Boolean) {
        if (completed) answer?.complete(stt)
    }

    override fun onEndedRecognition() {
        // 최종 결과 없이 끝나면(무음 등) 답 없음
        answer?.complete(null)
    }

    override fun onError(errorType: ErrorType) {
        Log.w(TAG, "STT 오류: $errorType")
        answer?.complete(null)
    }

    override fun onReady() = Unit
    override fun onStartedRecognition() = Unit
    override fun onUpdatedEpdData(on: Long, off: Long) = Unit
    override fun onUpdatedRms(rms: Double) = Unit

    private companion object {
        const val TAG = "PleosVoiceCommand"
        const val LISTEN_TIMEOUT_MS = 8_000L
    }
}
