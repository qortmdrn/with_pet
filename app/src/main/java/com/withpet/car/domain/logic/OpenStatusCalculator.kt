package com.withpet.car.domain.logic

import com.withpet.car.domain.model.Hospital
import com.withpet.car.domain.model.OpenStatus
import java.time.LocalDateTime

object OpenStatusCalculator {

    fun statusAt(hospital: Hospital, at: LocalDateTime): OpenStatus {
        if (hospital.is24Hours) return OpenStatus.OPEN
        if (hospital.weeklyHours.isEmpty()) return OpenStatus.UNKNOWN

        val today = at.dayOfWeek
        val yesterday = today.minus(1)
        val time = at.toLocalTime()

        val isOpen = hospital.weeklyHours.any { hours ->
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
