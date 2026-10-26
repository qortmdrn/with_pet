package com.withpet.car.domain.logic

import com.withpet.car.domain.model.HolidayKind
import com.withpet.car.everyDay
import com.withpet.car.domain.model.OpenStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class KoreanHolidaysTest {

    @Test
    fun `설·추석·대체공휴일`() {
        assertEquals(HolidayKind.SEOLLAL, KoreanHolidays.kindOn(LocalDate.of(2026, 2, 17)))
        assertEquals(HolidayKind.CHUSEOK, KoreanHolidays.kindOn(LocalDate.of(2026, 9, 25)))
        assertEquals(HolidayKind.OTHER, KoreanHolidays.kindOn(LocalDate.of(2026, 3, 2)))   // 삼일절 대체
        assertEquals(HolidayKind.SEOLLAL, KoreanHolidays.kindOn(LocalDate.of(2027, 2, 9)))  // 설 대체
        assertEquals(HolidayKind.NEW_YEAR, KoreanHolidays.kindOn(LocalDate.of(2027, 1, 1)))
        assertNull(KoreanHolidays.kindOn(LocalDate.of(2026, 10, 26)))
    }

    @Test
    fun `공휴일 휴무인 곳은 공휴일에 닫힘, 아닌 곳은 그대로`() {
        val weekdays = everyDay("09:00", "18:00")
        val chuseokNoon = LocalDateTime.of(2026, 9, 25, 12, 0)
        assertEquals(OpenStatus.CLOSED, OpenStatusCalculator.statusAt(weekdays, chuseokNoon, HolidayKind.entries.toSet()))
        assertEquals(OpenStatus.CLOSED, OpenStatusCalculator.statusAt(weekdays, chuseokNoon, setOf(HolidayKind.CHUSEOK)))
        // 설에만 쉬는 곳은 추석에 연다
        assertEquals(OpenStatus.OPEN, OpenStatusCalculator.statusAt(weekdays, chuseokNoon, setOf(HolidayKind.SEOLLAL)))
        assertEquals(OpenStatus.OPEN, OpenStatusCalculator.statusAt(weekdays, chuseokNoon))
        // 평일에는 영향 없음
        assertEquals(OpenStatus.OPEN, OpenStatusCalculator.statusAt(weekdays, LocalDateTime.of(2026, 10, 26, 12, 0), HolidayKind.entries.toSet()))
    }
}
