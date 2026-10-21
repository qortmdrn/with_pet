package com.withpet.car.platform.phone

import android.content.Context
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakePhoneDialer @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhoneDialer {

    private val connected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = connected.asStateFlow()

    /** 테스트 확인용: 지금까지 요청된 번호 */
    val calledNumbers = mutableListOf<String>()

    override fun isCallCapable(): Flow<Boolean> = connected.asStateFlow()

    override suspend fun call(phoneNumber: String): Result<Unit> {
        if (!connected.value) return Result.failure(IllegalStateException("휴대폰 미연결"))
        calledNumbers += phoneNumber
        Log.i(TAG, "가짜 전화 연결: $phoneNumber")
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "(가짜) 전화 연결: $phoneNumber", Toast.LENGTH_SHORT).show()
        }
        return Result.success(Unit)
    }

    fun setConnected(value: Boolean) {
        connected.value = value
    }

    private companion object {
        const val TAG = "FakePhoneDialer"
    }
}
