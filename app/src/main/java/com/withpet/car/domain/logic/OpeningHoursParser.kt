package com.withpet.car.domain.logic

import com.withpet.car.domain.model.OpeningHours
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * 공공데이터 운영시간·휴무일 원문을 요일별 영업시간으로 바꾼다.
 *
 * 해석할 수 있는 형식
 * - 운영시간: "매일 09:00~18:00", "화~일 10:00~17:00", "월, 수~금 15:00~21:30, 토~일 14:00~21:30"
 *   ("24:00" 마감은 자정, "00:00~24:00"은 종일)
 * - 휴무일: "연중무휴", "매주 월요일", "매주 토, 일", "매주 월~금"
 *
 * 계절별 시간(하절기·3~10월), "일몰", "변동" 등은 null(미확인)을 돌려준다.
 * 공휴일·명절·"N째주" 휴무는 요일 단위로 표현할 수 없어 무시한다 (화면에서 원문을 함께 보여 준다).
 */
object OpeningHoursParser {

    private const val DAY = "[월화수목금토일]"
    private const val DAY_ITEM = "$DAY(?:요일)?(?:\\s*~\\s*$DAY(?:요일)?)?"
    private const val DAY_LIST = "$DAY_ITEM(?:\\s*,\\s*$DAY_ITEM)*"
    private const val TIME = "\\d{1,2}:\\d{2}"
    private const val SEGMENT = "(매일|$DAY_LIST)\\s+($TIME)\\s*~\\s*($TIME)"

    private val segment = Regex(SEGMENT)
    private val hoursLine = Regex("^$SEGMENT(?:\\s*,\\s*$SEGMENT)*$")
    private val dayItem = Regex("($DAY)(?:요일)?(?:\\s*~\\s*($DAY)(?:요일)?)?")
    private val weeklyClosed = Regex("^매주\\s+($DAY_LIST)(?:\\s*휴무)?$")
    private val bareDays = Regex("^$DAY_LIST$")
    private val seasonal = Regex("하절기|동절기|\\d+\\s*~\\s*\\d+\\s*월")

    // 요일 단위가 아닌 휴무 (무시)
    private val ignoredClosed = listOf("설", "추석", "1월1일", "공휴일", "연휴", "째주", "마지막주", "개교기념일")

    fun parse(hours: String?, closedDays: String?): List<OpeningHours>? {
        val line = hours?.normalize() ?: return null
        if (!hoursLine.matches(line)) return null

        val byDay = linkedMapOf<DayOfWeek, OpeningHours>()
        for (match in segment.findAll(line)) {
            val (days, open, close) = match.destructured
            val openTime = parseTime(open) ?: return null
            val closeTime = parseTime(close) ?: return null
            val targetDays = if (days == "매일") DayOfWeek.entries else parseDays(days)
            // 뒤에 나온 구간이 앞 구간을 덮어쓴다 ("매일 …, 토~일 …")
            targetDays.forEach { byDay[it] = OpeningHours(it, openTime, closeTime) }
        }

        val closed = parseClosedDays(closedDays) ?: return null
        return byDay.values.filter { it.dayOfWeek !in closed }.sortedBy { it.dayOfWeek }
    }

    /** 쉬는 요일. 해석할 수 없는 내용이 있으면 null. */
    private fun parseClosedDays(text: String?): Set<DayOfWeek>? {
        val line = text?.normalize() ?: return emptySet()
        if (line == "연중무휴") return emptySet()

        val closed = mutableSetOf<DayOfWeek>()
        // "매주 토, 일, 법정공휴일"처럼 '매주' 뒤의 요일 나열이 쉼표로 이어진다
        var weekly = false
        for (token in line.split(',').map { it.trim() }.filter { it.isNotEmpty() }) {
            when {
                weeklyClosed.matches(token) -> {
                    weekly = true
                    closed += parseDays(weeklyClosed.find(token)!!.groupValues[1])
                }
                weekly && bareDays.matches(token) -> closed += parseDays(token)
                // "동절기 매주 월", "12~2월"처럼 기간이 붙은 휴무나 형식이 어긋난 '매주'는 해석하지 않는다
                seasonal.containsMatchIn(token) || "매주" in token -> return null
                ignoredClosed.any { it in token } -> weekly = false
                else -> return null
            }
        }
        return closed
    }

    private fun parseDays(text: String): List<DayOfWeek> =
        dayItem.findAll(text).flatMap { m ->
            val from = dayOf(m.groupValues[1])
            val to = m.groupValues[2].takeIf { it.isNotEmpty() }?.let(::dayOf) ?: from
            generateSequence(from) { if (it == to) null else it.plus(1) }
        }.toList()

    private fun dayOf(korean: String): DayOfWeek = when (korean) {
        "월" -> DayOfWeek.MONDAY
        "화" -> DayOfWeek.TUESDAY
        "수" -> DayOfWeek.WEDNESDAY
        "목" -> DayOfWeek.THURSDAY
        "금" -> DayOfWeek.FRIDAY
        "토" -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }

    /** "24:00"은 자정(00:00). close ≤ open이면 다음 날 마감으로 해석된다. */
    private fun parseTime(text: String): LocalTime? {
        val (h, m) = text.split(':').map { it.toInt() }
        return when {
            h == 24 && m == 0 -> LocalTime.MIDNIGHT
            h in 0..23 && m in 0..59 -> LocalTime.of(h, m)
            else -> null
        }
    }

    private fun String.normalize(): String? =
        replace('∼', '~').replace('～', '~').trim().takeIf { it.isNotEmpty() }
}
