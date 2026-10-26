package com.withpet.car.platform.voice

import com.withpet.car.domain.model.VoiceCommand
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GleoActionsTest {

    @Test
    fun `응급 - 파라미터 없음`() {
        assertEquals(VoiceCommand.Emergency, GleoActions.parse("FIND_EMERGENCY_VET", null))
    }

    @Test
    fun `펫 모드 - ACTION_PARAM JSON, 따옴표로 한 번 더 감싼 형태, 개별 extra`() {
        assertEquals(VoiceCommand.PetModeOn, GleoActions.parse("SET_PET_MODE", """{"state":"ON"}"""))
        assertEquals(VoiceCommand.PetModeOff, GleoActions.parse("SET_PET_MODE", """"{\"state\":\"off\"}""""))
        assertEquals(VoiceCommand.PetModeOn, GleoActions.parse("SET_PET_MODE", null, fallbackState = "ON"))
    }

    @Test
    fun `잘못된 값이나 모르는 action은 null`() {
        assertNull(GleoActions.parse("SET_PET_MODE", """{"state":"MAYBE"}"""))
        assertNull(GleoActions.parse("SET_PET_MODE", "not json"))
        assertNull(GleoActions.parse("PLAY_MUSIC", null))
    }

    @Test
    fun `callback 응답 형식`() {
        val response = Json.decodeFromString<JsonObject>(GleoActions.response("SET_PET_MODE", true, "Turning pet mode on."))
        assertEquals("SET_PET_MODE", response["functionName"]!!.jsonPrimitive.content)
        assertEquals("success", response["status"]!!.jsonPrimitive.content)
        assertEquals("voice", response["mode"]!!.jsonPrimitive.content)
    }
}
