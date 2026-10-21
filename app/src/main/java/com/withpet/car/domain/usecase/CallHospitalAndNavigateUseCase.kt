package com.withpet.car.domain.usecase

import com.withpet.car.domain.model.Hospital
import com.withpet.car.platform.navigation.NavigationController
import com.withpet.car.platform.phone.PhoneDialer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class CallAndNavigateResult(
    /** 휴대폰 미연결 또는 발신 실패. UI는 번호를 크게 보여 준다. */
    val callStarted: Boolean,
    val guidanceStarted: Boolean,
)

/** 병원 선택 시 전화 연결과 길 안내를 동시에 시작한다 (명세 7-2 6~7). */
class CallHospitalAndNavigateUseCase @Inject constructor(
    private val dialer: PhoneDialer,
    private val navigation: NavigationController,
) {
    suspend operator fun invoke(hospital: Hospital): CallAndNavigateResult = coroutineScope {
        val guidance = async {
            runCatching { navigation.startGuidance(hospital.location, hospital.name).getOrThrow() }.isSuccess
        }
        val call = async {
            val capable = runCatching { dialer.isCallCapable().first() }.getOrDefault(false)
            capable && runCatching { dialer.call(hospital.phone).getOrThrow() }.isSuccess
        }
        CallAndNavigateResult(callStarted = call.await(), guidanceStarted = guidance.await())
    }
}
