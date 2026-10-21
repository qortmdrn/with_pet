package com.withpet.car.domain.logic

import com.withpet.car.domain.model.OpenStatus
import com.withpet.car.domain.model.OpeningHours
import com.withpet.car.everyDay
import com.withpet.car.hospital
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

class OpenStatusCalculatorTest {

    // 2026-10-19 월요일
    private fun at(day: Int, time: String) = LocalDateTime.of(2026, 10, day, 0, 0).with(LocalTime.parse(time))

    @Test
    fun `24시 병원은 항상 진료 중`() {
        val h = hospital("a", is24Hours = true)
        assertEquals(OpenStatus.OPEN, OpenStatusCalculator.statusAt(h, at(19, "03:00")))
    }

    @Test
    fun `진료 시간이 비어 있으면 미확인`() {
        assertEquals(OpenStatus.UNKNOWN, OpenStatusCalculator.statusAt(hospital("a"), at(19, "12:00")))
    }

    @ParameterizedTest
    @CsvSource(
        "08:59, CLOSED",
        "09:00, OPEN",
        "20:59, OPEN",
        "21:00, CLOSED",
        "23:59, CLOSED",
    )
    fun `당일 영업 경계`(time: String, expected: OpenStatus) {
        val h = hospital("a", hours = everyDay("09:00", "21:00"))
        assertEquals(expected, OpenStatusCalculator.statusAt(h, at(19, time)))
    }

    @ParameterizedTest
    @CsvSource(
        "19, 23:59, OPEN",    // 월 자정 직전
        "20, 00:00, OPEN",    // 화 자정
        "20, 00:59, OPEN",    // 자정 직후, 전날 영업 이어짐
        "20, 01:00, CLOSED",
        "20, 09:59, CLOSED",
        "20, 10:00, OPEN",
    )
    fun `자정 넘김 영업`(day: Int, time: String, expected: OpenStatus) {
        val h = hospital("a", hours = everyDay("10:00", "01:00"))
        assertEquals(expected, OpenStatusCalculator.statusAt(h, at(day, time)))
    }

    @Test
    fun `자정 넘김 영업은 전날 영업일이 있어야 이어진다`() {
        // 일요일 휴진: 월요일 새벽 00:30은 일요일 밤 영업이 없으므로 닫힘
        val hours = everyDay("10:00", "01:00").filter { it.dayOfWeek != DayOfWeek.SUNDAY }
        val h = hospital("a", hours = hours)
        assertEquals(OpenStatus.CLOSED, OpenStatusCalculator.statusAt(h, at(19, "00:30")))
        // 화요일 새벽은 월요일 밤 영업이 이어짐
        assertEquals(OpenStatus.OPEN, OpenStatusCalculator.statusAt(h, at(20, "00:30")))
    }

    @Test
    fun `요일별로 다른 시간`() {
        val h = hospital(
            "a",
            hours = listOf(
                OpeningHours(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                OpeningHours(DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(14, 0)),
            ),
        )
        assertEquals(OpenStatus.OPEN, OpenStatusCalculator.statusAt(h, at(19, "17:00")))       // 월
        assertEquals(OpenStatus.CLOSED, OpenStatusCalculator.statusAt(h, at(20, "12:00")))     // 화 휴진
        assertEquals(OpenStatus.OPEN, OpenStatusCalculator.statusAt(h, at(24, "13:00")))       // 토
        assertEquals(OpenStatus.CLOSED, OpenStatusCalculator.statusAt(h, at(24, "15:00")))
    }
}
