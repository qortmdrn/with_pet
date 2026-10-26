package com.withpet.car.domain.logic

import com.withpet.car.domain.model.VoiceCommand

/** 휴식 알림 질문에 대한 짧은 답("응, 추가해" / "아니, 나중에")을 Confirm·Reject로 바꾼다. */
object VoiceAnswerMatcher {

    // 거절을 먼저 본다: "아니 추가하지 마"처럼 둘 다 들어 있으면 거절
    private val reject = listOf("아니", "나중에", "다음에", "싫어", "됐어", "하지마", "취소", "괜찮아")
    private val confirm = listOf("추가", "좋아", "그래", "가자", "쉬어", "쉴래", "알았어", "오케이")
    // 한 글자 답은 답 전체이거나 맨 앞일 때만 ("응급"처럼 다른 말의 일부가 되지 않게)
    private val shortConfirm = listOf("응", "어", "네", "예", "웅")

    fun match(text: String): VoiceCommand? {
        val t = text.replace(Regex("[\\s.,!?~]"), "")
        if (t.isEmpty()) return null
        return when {
            reject.any { it in t } -> VoiceCommand.Reject
            confirm.any { it in t } -> VoiceCommand.Confirm
            shortConfirm.any { t == it || (t.startsWith(it) && t.length <= it.length + 2) } -> VoiceCommand.Confirm
            else -> null
        }
    }
}
