package com.withpet.car.domain.logic

import com.withpet.car.domain.model.VoiceCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VoiceAnswerMatcherTest {

    @Test
    fun `수락`() {
        listOf("응", "응, 추가해", "네", "그래", "좋아요", "추가해 줘", "어 쉬어 가자").forEach {
            assertEquals(VoiceCommand.Confirm, VoiceAnswerMatcher.match(it), it)
        }
    }

    @Test
    fun `거절 - 수락 단어와 섞이면 거절이 우선`() {
        listOf("아니", "아니, 나중에", "나중에 할게", "됐어", "아니 추가하지 마").forEach {
            assertEquals(VoiceCommand.Reject, VoiceAnswerMatcher.match(it), it)
        }
    }

    @Test
    fun `모르는 말이나 다른 말의 일부인 한 글자는 무시`() {
        assertNull(VoiceAnswerMatcher.match(""))
        assertNull(VoiceAnswerMatcher.match("오늘 날씨 어때"))
        assertNull(VoiceAnswerMatcher.match("응급실이 어디야"))
    }
}
