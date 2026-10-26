package com.withpet.car.platform.voice

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gleo AI가 보낸 Intent(Gleo Action)를 음성 명령으로 바꾸고, 7초 안에 결과를 broadcast로 돌려준다.
 * Pleos가 assets/schema.json을 검토·승인해야 Gleo가 실제로 호출한다. 승인 전에는 adb로 시험한다:
 *   adb shell am start -a SET_PET_MODE -n com.withpet.car/.MainActivity --es ACTION_PARAM '{"state":"ON"}'
 */
@Singleton
class GleoActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voice: PleosVoiceCommandSource,
) : ExternalActionHandler {

    override fun handle(intent: Intent): Boolean {
        val action = intent.action ?: return false
        if (action != GleoActions.FIND_EMERGENCY_VET && action != GleoActions.SET_PET_MODE) return false

        val command = GleoActions.parse(action, intent.getStringExtra(GleoActions.EXTRA_PARAM), intent.getStringExtra("state"))
        command?.let(voice::send)
        // 펫 모드 실행 결과(창문 미지원 등)는 앱 화면에서 따로 보여 준다. Gleo에는 요청을 받았다는 것만 알린다.
        respond(action, command != null, GleoActions.messageFor(command))
        return true
    }

    private fun respond(functionName: String, success: Boolean, message: String) {
        runCatching {
            context.sendBroadcast(
                Intent(GleoActions.RESPONSE_ACTION)
                    .putExtra("packageName", context.packageName)
                    .putExtra("response", GleoActions.response(functionName, success, message)),
            )
        }.onFailure { Log.w("GleoActionHandler", "Gleo 응답 실패", it) }
    }
}
