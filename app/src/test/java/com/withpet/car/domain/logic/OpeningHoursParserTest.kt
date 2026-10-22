package com.withpet.car.domain.logic

import com.withpet.car.domain.model.OpeningHours
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalTime

class OpeningHoursParserTest {

    private fun days(hours: String, closed: String? = "연중무휴") = OpeningHoursParser.parse(hours, closed)?.map { it.dayOfWeek }

    private fun hoursOf(day: DayOfWeek, open: String, close: String) = OpeningHours(day, LocalTime.parse(open), LocalTime.parse(close))

    @Test
    fun `매일 영업`() {
        val result = OpeningHoursParser.parse("매일 10:00~22:00", "연중무휴")!!
        assertEquals(DayOfWeek.entries, result.map { it.dayOfWeek })
        assertEquals(hoursOf(MONDAY, "10:00", "22:00"), result.first())
    }

    @Test
    fun `요일 범위와 한 자리 시각`() {
        assertEquals(listOf(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY), days("화~일 9:00~18:00"))
        assertEquals(LocalTime.of(9, 0), OpeningHoursParser.parse("화~일 9:00~18:00", null)!!.first().open)
    }

    @Test
    fun `여러 구간 - 쉼표로 이어진 요일 목록`() {
        val result = OpeningHoursParser.parse("월, 수~금 15:00~21:30, 토~일 14:00~21:30", "매주 화요일")!!
        assertEquals(listOf(MONDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY), result.map { it.dayOfWeek })
        assertEquals(LocalTime.of(14, 0), result.first { it.dayOfWeek == SATURDAY }.open)
    }

    @Test
    fun `24시간과 자정 넘김 - 24시는 자정으로`() {
        assertEquals(hoursOf(MONDAY, "00:00", "00:00"), OpeningHoursParser.parse("매일 00:00~24:00", "연중무휴")!!.first())
        assertEquals(hoursOf(TUESDAY, "18:00", "02:00"), OpeningHoursParser.parse("화~일 18:00~02:00", "매주 월요일")!!.first())
    }

    @Test
    fun `휴무 요일을 뺀다 - 매주 뒤 요일 나열`() {
        assertEquals(listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), days("매일 09:00~18:00", "매주 토, 일, 법정공휴일"))
        assertEquals(listOf(SATURDAY, SUNDAY), days("매일 09:00~18:00", "매주 월~금"))
        assertEquals(listOf(WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY), days("매일 09:00~18:00", "매주 월, 화"))
    }

    @Test
    fun `명절·공휴일·N째주 휴무는 요일로 표현할 수 없어 무시한다`() {
        assertEquals(6, days("매일 09:00~18:00", "1월1일, 설, 추석 당일, 매주 월요일")!!.size)
        assertEquals(7, days("매일 09:00~18:00", "4째주 월요일")!!.size)
    }

    @Test
    fun `strict 모드 - 특정 주에만 쉬는 휴무가 있으면 미확인`() {
        assertNull(OpeningHoursParser.parse("매일 09:00~18:00", "4째주 월요일", strict = true))
        assertNull(OpeningHoursParser.parse("매일 09:00~18:00", "매주 일요일, 마지막주 수요일", strict = true))
        // 공휴일은 strict여도 무시 (요일로 표현 불가)
        assertEquals(6, OpeningHoursParser.parse("매일 09:00~18:00", "매주 일요일, 법정공휴일", strict = true)!!.size)
    }

    @Test
    fun `해석할 수 없으면 null`() {
        assertNull(OpeningHoursParser.parse("정보없음", null))
        assertNull(OpeningHoursParser.parse(null, null))
        assertNull(OpeningHoursParser.parse("전시공연에 따라 변동", "전시공연에 따라 변동"))
        assertNull(OpeningHoursParser.parse("화~일 하절기 09:00~18:00, 동절기 09:00~17:00", "매주 월요일"))
        assertNull(OpeningHoursParser.parse("화~일 3~10월 09:00~18:00, 11~2월 09:00~17:00", "매주 월요일"))
        assertNull(OpeningHoursParser.parse("매일 09:00~일몰 1시간 전", "연중무휴"))
        // 휴무일이 계절별이거나 알 수 없는 형식
        assertNull(OpeningHoursParser.parse("매일 09:00~18:00", "동절기 매주 월"))
        assertNull(OpeningHoursParser.parse("매일 09:00~18:00", "12~2월"))
        assertNull(OpeningHoursParser.parse("매일 09:00~18:00", "격주 일요일"))
    }

    @Test
    fun `물결표 변형을 허용한다`() {
        assertEquals(6, days("화~일 10:00 ∼ 18:00")!!.size)
    }

    @Test
    fun `휴무일 정보가 없으면 운영시간만 쓴다`() {
        assertEquals(7, days("매일 09:00~18:00", null)!!.size)
    }
}
