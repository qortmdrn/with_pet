package com.withpet.car.platform.voice

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Pleos TextToSpeech SDK로 휴식 알림을 읽어 준다 (docs/pleos-sdk/tts.md).
 * ON_DEVICE 모드: 앱 등록(My Project 자격 증명) 없이 차량 온디바이스 엔진을 쓴다.
 */
@Singleton
class PleosSpeechOutput @Inject constructor(
    @ApplicationContext context: Context,
) : SpeechOutput, EventListener {

    private val tts = TextToSpeech(context.applicationContext, Mode.ON_DEVICE)
    private val mutex = Mutex()
    @Volatile private var ready = false
    @Volatile private var pending: CancellableContinuation<Result<Unit>>? = null

    init {
        runCatching {
            tts.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    tts.addEventListener(this@PleosSpeechOutput)
                    tts.setLocale(Locale.KOREA)
                    ready = true
                }

                override fun onFailure(errorType: ErrorType) {
                    Log.w(TAG, "TTS 초기화 실패: $errorType")
                }
            })
        }.onFailure { Log.w(TAG, "TTS 초기화 실패", it) }
    }

    override fun isAvailable() = ready

    /** 다 읽을 때까지 기다린다. 동시에 들어온 요청은 차례로 읽는다. */
    override suspend fun speak(text: String): Result<Unit> = mutex.withLock {
        if (!ready) return@withLock Result.failure(IllegalStateException("TTS 준비 안 됨"))
        runCatching {
            withTimeout(SPEAK_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    pending = cont
                    cont.invokeOnCancellation { runCatching { tts.stop() } }
                    tts.speak(text, true)
                }
            }.getOrThrow()
        }.also { pending = null }
    }

    private fun finish(result: Result<Unit>) {
        pending?.takeIf { it.isActive }?.resume(result)
        pending = null
    }

    // --- EventListener (콜백 스레드는 문서에 없음. 상태는 pending 하나만 건드린다) ---
    override fun onReady() = Unit
    override fun onStart() = Unit
    override fun onUpdatedRms(rms: Double) = Unit
    override fun onDone() = finish(Result.success(Unit))
    override fun onStop() = finish(Result.failure(IllegalStateException("TTS 중지됨")))
    override fun onError(errorType: ErrorType) {
        Log.w(TAG, "TTS 오류: $errorType")
        finish(Result.failure(IllegalStateException("TTS 오류: $errorType")))
    }

    private companion object {
        const val TAG = "PleosSpeechOutput"
        const val SPEAK_TIMEOUT_MS = 30_000L
    }
}
