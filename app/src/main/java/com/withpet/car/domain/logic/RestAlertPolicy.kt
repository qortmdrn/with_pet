package com.withpet.car.domain.logic

import com.withpet.car.domain.model.RestAlertSettings

/**
 * 휴식 알림 트리거 규칙 (명세 7-4).
 * - 연속 주행 시간이 간격 이상이고 펫 모드가 ON이면 알린다.
 * - [나중에]는 30분 뒤 재알림, 한 주행에서 거절 2회면 그 주행에는 더 알리지 않는다.
 * - 연속 주행 시간이 줄어들면(정차로 초기화) 새 주행으로 본다.
 */
class RestAlertPolicy {

    private var rejectCount = 0
    private var nextThresholdMinutes: Int? = null
    private var lastMinutes = 0
    var isShowing = false
        private set

    fun shouldAlert(drivingMinutes: Int, petModeOn: Boolean, settings: RestAlertSettings): Boolean {
        if (drivingMinutes < lastMinutes) startNewTrip()
        lastMinutes = drivingMinutes

        if (!settings.enabled || !petModeOn || isShowing) return false
        if (rejectCount >= MAX_REJECTS_PER_TRIP) return false
        val threshold = nextThresholdMinutes ?: settings.intervalMinutes
        return drivingMinutes >= threshold
    }

    fun onShown() {
        isShowing = true
    }

    fun onAccepted(drivingMinutes: Int, settings: RestAlertSettings) {
        isShowing = false
        nextThresholdMinutes = drivingMinutes + settings.intervalMinutes
    }

    fun onRejected(drivingMinutes: Int) {
        isShowing = false
        rejectCount++
        nextThresholdMinutes = drivingMinutes + SNOOZE_MINUTES
    }

    /** 펫 모드 OFF 등으로 사용자 응답 없이 카드를 닫는다. 거절로 세지 않는다. */
    fun onDismissed() {
        isShowing = false
    }

    private fun startNewTrip() {
        rejectCount = 0
        nextThresholdMinutes = null
        isShowing = false
    }

    companion object {
        const val SNOOZE_MINUTES = 30
        const val MAX_REJECTS_PER_TRIP = 2
    }
}
