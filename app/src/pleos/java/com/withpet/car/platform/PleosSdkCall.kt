package com.withpet.car.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Pleos SDK의 onComplete/onFailed 콜백 API를 suspend 함수로 감싼다. IPC 응답이 없으면 시간 초과로 실패한다. */
internal suspend fun <T> pleosCall(
    timeoutMs: Long = 3_000,
    block: (onComplete: (T) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
): T = withTimeout(timeoutMs) {
    suspendCancellableCoroutine { cont ->
        try {
            block(
                { value -> if (cont.isActive) cont.resume(value) },
                { error -> if (cont.isActive) cont.resumeWithException(error) },
            )
        } catch (e: Exception) {
            if (cont.isActive) cont.resumeWithException(e)
        }
    }
}
