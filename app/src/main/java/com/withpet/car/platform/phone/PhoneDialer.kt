package com.withpet.car.platform.phone

import kotlinx.coroutines.flow.Flow

interface PhoneDialer {
    fun isCallCapable(): Flow<Boolean>      // 휴대폰 블루투스 연결 여부
    suspend fun call(phoneNumber: String): Result<Unit>
}
