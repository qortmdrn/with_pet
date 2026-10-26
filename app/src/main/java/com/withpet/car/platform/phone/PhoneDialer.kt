package com.withpet.car.platform.phone

import kotlinx.coroutines.flow.Flow

interface PhoneDialer {
    fun isCallCapable(): Flow<Boolean>      // 휴대폰 블루투스 연결 여부
    suspend fun call(phoneNumber: String): Result<Unit>

    /** true면 [call]은 다이얼러에 번호만 채운다. 사용자가 통화 버튼을 눌러야 실제로 걸린다. */
    val opensDialerOnly: Boolean get() = false
}
