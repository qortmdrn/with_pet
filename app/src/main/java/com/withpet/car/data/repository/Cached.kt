package com.withpet.car.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 한 번 읽은 뒤 메모리에 보관한다. 서버 연동 시 이것을 쓰는 저장소만 교체한다. */
internal class Cached<T>(private val io: CoroutineDispatcher, private val load: () -> T) {
    private val mutex = Mutex()
    private var value: T? = null

    suspend fun get(): T = mutex.withLock {
        value ?: withContext(io) { load() }.also { value = it }
    }
}
