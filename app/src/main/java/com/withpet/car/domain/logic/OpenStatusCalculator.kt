package com.withpet.car.domain.logic

import com.withpet.car.domain.model.HolidayKind
import com.withpet.car.domain.model.Hospital
import com.withpet.car.domain.model.OpenStatus
import com.withpet.car.domain.model.OpeningHours
import java.time.LocalDateTime

object OpenStatusCalculator {

    fun statusAt(hospital: Hospital, at: LocalDateTime): OpenStatus {
        if (hospital.is24Hours) return OpenStatus.OPEN
        return statusAt(hospital.weeklyHours, at, hospital.closedOnHolidays)
    }

    /**
     * @param weeklyHours 비어 있으면 진료·영업 시간 미확인
     * @param closedOnHolidays 오늘이 이 종류의 공휴일이면 닫힘 ([KoreanHolidays])
     */
    fun statusAt(weeklyHours: List<OpeningHours>, at: LocalDateTime, closedOnHolidays: Set<HolidayKind> = emptySet()): OpenStatus {
        if (weeklyHours.isEmpty()) return OpenStatus.UNKNOWN
        if (KoreanHolidays.kindOn(at.toLocalDate()) in closedOnHolidays) return OpenStatus.CLOSED

        val today = at.dayOfWeek
        val yesterday = today.minus(1)
        val time = at.toLocalTime()

        val isOpen = weeklyHours.any { hours ->
            val overnight = !hours.close.isAfter(hours.open)
            when {
                hours.dayOfWeek == today && overnight -> !time.isBefore(hours.open)
                hours.dayOfWeek == today -> !time.isBefore(hours.open) && time.isBefore(hours.close)
                // 전날 시작해 자정을 넘긴 영업
                hours.dayOfWeek == yesterday && overnight -> time.isBefore(hours.close)
                else -> false
            }
        }
        return if (isOpen) OpenStatus.OPEN else OpenStatus.CLOSED
    }
}
