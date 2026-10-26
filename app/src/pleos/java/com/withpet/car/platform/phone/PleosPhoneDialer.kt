package com.withpet.car.platform.phone

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pleos SDK에는 전화 API가 없다 (docs/pleos-sdk/phone-and-volume.md).
 * 표준 ACTION_DIAL로 차량 다이얼러에 병원 번호만 채워 띄운다. 자동 발신하지 않으므로 사용자가 통화 버튼을 눌러야 걸린다
 * (CALL_PHONE 권한 불필요, 테스트 중 실제 병원으로 전화가 걸리는 것도 막는다).
 * 블루투스 휴대폰 연결 여부는 알 수 없어, 다이얼러가 있으면 발신 가능으로 본다.
 */
@Singleton
class PleosPhoneDialer @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhoneDialer {

    override val opensDialerOnly = true

    override fun isCallCapable(): Flow<Boolean> =
        flowOf(dialIntent("0").resolveActivity(context.packageManager) != null)

    override suspend fun call(phoneNumber: String): Result<Unit> = runCatching {
        context.startActivity(dialIntent(phoneNumber).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun dialIntent(number: String) = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
}
