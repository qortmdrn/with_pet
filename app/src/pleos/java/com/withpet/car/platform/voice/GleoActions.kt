package com.withpet.car.platform.voice

import com.withpet.car.domain.model.VoiceCommand
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Gleo Interface 함수 이름과 파라미터 해석 (docs/pleos-sdk/gleo-interface.md).
 * 함수 이름이 곧 Intent action이고, assets/schema.json과 매니페스트 intent-filter에 같은 이름으로 선언한다.
 */
object GleoActions {
    const val FIND_EMERGENCY_VET = "FIND_EMERGENCY_VET"
    const val SET_PET_MODE = "SET_PET_MODE"

    const val EXTRA_PARAM = "ACTION_PARAM"
    const val RESPONSE_ACTION = "ai.pleos.playground.intent.action.APP_ACTION_RESPONSE"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @param param ACTION_PARAM extra 원문. 문서 예시처럼 따옴표로 한 겹 더 감싸 올 수 있다.
     * @param fallbackState ACTION_PARAM이 없을 때 개별 extra("state")
     * @return 모르는 action이거나 파라미터가 잘못되면 null
     */
    fun parse(action: String?, param: String?, fallbackState: String? = null): VoiceCommand? = when (action) {
        FIND_EMERGENCY_VET -> VoiceCommand.Emergency
        SET_PET_MODE -> when ((stateOf(param) ?: fallbackState)?.uppercase()) {
            "ON" -> VoiceCommand.PetModeOn
            "OFF" -> VoiceCommand.PetModeOff
            else -> null
        }
        else -> null
    }

    private fun stateOf(param: String?): String? = runCatching {
        var text = param?.trim() ?: return null
        // "\"{...}\"" 형태면 바깥 문자열을 한 번 벗긴다
        if (text.startsWith("\"")) text = json.decodeFromString<String>(text)
        json.decodeFromString<JsonObject>(text)["state"]?.jsonPrimitive?.content
    }.getOrNull()

    /** Gleo에 돌려줄 callback의 response JSON. message는 영어 문장 (Gleo가 사용자 언어로 다시 말한다). */
    fun response(functionName: String, success: Boolean, message: String): String = buildJsonObject {
        put("functionName", functionName)
        put("status", if (success) "success" else "fail")
        put("message", message)
        put("mode", "voice")
    }.toString()

    fun messageFor(command: VoiceCommand?): String = when (command) {
        VoiceCommand.Emergency -> "Showing nearby animal hospitals that are open now."
        VoiceCommand.PetModeOn -> "Turning pet mode on."
        VoiceCommand.PetModeOff -> "Turning pet mode off."
        else -> "Sorry, I could not handle that request."
    }
}
