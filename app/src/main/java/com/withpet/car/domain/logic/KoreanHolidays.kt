package com.withpet.car.domain.logic

import com.withpet.car.domain.model.HolidayKind
import java.time.LocalDate

/**
 * 대한민국 공휴일 (대체공휴일·선거일 포함).
 *
 * 출처: Google 대한민국 공휴일 캘린더와 date.nager.at을 2026-10-26에 대조해 합쳤다.
 * 설·추석·대체공휴일은 해마다 바뀌므로 [COVERED_YEARS] 밖의 해는 공휴일을 알 수 없다(공휴일 아님으로 처리).
 * 매년 다음 해 날짜를 추가할 것. 임시공휴일은 지정되면 직접 추가해야 한다.
 */
object KoreanHolidays {

    val COVERED_YEARS = 2026..2027

    private val holidays: Map<LocalDate, HolidayKind> = buildMap {
        fun add(kind: HolidayKind, vararg dates: String) = dates.forEach { put(LocalDate.parse(it), kind) }

        add(HolidayKind.NEW_YEAR, "2026-01-01", "2027-01-01")
        add(HolidayKind.SEOLLAL, "2026-02-16", "2026-02-17", "2026-02-18")
        add(HolidayKind.SEOLLAL, "2027-02-06", "2027-02-07", "2027-02-08", "2027-02-09")
        add(HolidayKind.CHUSEOK, "2026-09-24", "2026-09-25", "2026-09-26")
        add(HolidayKind.CHUSEOK, "2027-09-14", "2027-09-15", "2027-09-16")
        add(
            HolidayKind.OTHER,
            // 2026: 삼일절(+대체), 노동절, 어린이날, 부처님오신날(+대체), 지방선거, 현충일, 제헌절, 광복절(+대체), 개천절(+대체), 한글날, 성탄절
            "2026-03-01", "2026-03-02", "2026-05-01", "2026-05-05", "2026-05-24", "2026-05-25", "2026-06-03",
            "2026-06-06", "2026-07-17", "2026-08-15", "2026-08-17", "2026-10-03", "2026-10-05", "2026-10-09", "2026-12-25",
            // 2027: 삼일절, 노동절(+대체), 어린이날, 부처님오신날, 현충일, 제헌절(+대체), 광복절(+대체), 개천절(+대체), 한글날(+대체), 성탄절(+대체)
            "2027-03-01", "2027-05-01", "2027-05-03", "2027-05-05", "2027-05-13", "2027-06-06", "2027-07-17", "2027-07-19",
            "2027-08-15", "2027-08-16", "2027-10-03", "2027-10-04", "2027-10-09", "2027-10-11", "2027-12-25", "2027-12-27",
        )
    }

    /** 공휴일이 아니면 null */
    fun kindOn(date: LocalDate): HolidayKind? = holidays[date]
}
