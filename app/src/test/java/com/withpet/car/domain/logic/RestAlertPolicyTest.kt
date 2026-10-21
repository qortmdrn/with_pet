package com.withpet.car.domain.logic

import com.withpet.car.domain.model.RestAlertSettings
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RestAlertPolicyTest {

    private val settings = RestAlertSettings(enabled = true, intervalMinutes = 90)

    @Test
    fun `간격 미만이면 알리지 않는다`() {
        assertFalse(RestAlertPolicy().shouldAlert(89, petModeOn = true, settings))
    }

    @Test
    fun `간격 이상이고 펫 모드 ON이면 알린다`() {
        assertTrue(RestAlertPolicy().shouldAlert(90, petModeOn = true, settings))
    }

    @Test
    fun `펫 모드 OFF거나 알림이 꺼져 있으면 알리지 않는다`() {
        assertFalse(RestAlertPolicy().shouldAlert(120, petModeOn = false, settings))
        assertFalse(RestAlertPolicy().shouldAlert(120, petModeOn = true, settings.copy(enabled = false)))
    }

    @Test
    fun `표시 중에는 다시 알리지 않는다`() {
        val policy = RestAlertPolicy()
        assertTrue(policy.shouldAlert(90, true, settings))
        policy.onShown()
        assertFalse(policy.shouldAlert(91, true, settings))
    }

    @Test
    fun `나중에를 누르면 30분 뒤 다시 알린다`() {
        val policy = RestAlertPolicy()
        policy.onShown()
        policy.onRejected(90)
        assertFalse(policy.shouldAlert(119, true, settings))
        assertTrue(policy.shouldAlert(120, true, settings))
    }

    @Test
    fun `한 주행에서 2회 거절하면 더 알리지 않는다`() {
        val policy = RestAlertPolicy()
        policy.onShown(); policy.onRejected(90)
        assertTrue(policy.shouldAlert(120, true, settings))
        policy.onShown(); policy.onRejected(120)
        assertFalse(policy.shouldAlert(150, true, settings))
        assertFalse(policy.shouldAlert(300, true, settings))
    }

    @Test
    fun `정차로 연속 주행 시간이 초기화되면 새 주행으로 본다`() {
        val policy = RestAlertPolicy()
        policy.onShown(); policy.onRejected(90)
        policy.shouldAlert(120, true, settings)
        policy.onShown(); policy.onRejected(120)
        assertFalse(policy.shouldAlert(150, true, settings))

        policy.shouldAlert(0, true, settings) // 초기화
        assertFalse(policy.shouldAlert(89, true, settings))
        assertTrue(policy.shouldAlert(90, true, settings))
    }

    @Test
    fun `추가하면 다음 알림은 간격만큼 뒤`() {
        val policy = RestAlertPolicy()
        policy.onShown()
        policy.onAccepted(95, settings)
        assertFalse(policy.shouldAlert(184, true, settings))
        assertTrue(policy.shouldAlert(185, true, settings))
    }

    @Test
    fun `응답 없이 닫히면 거절로 세지 않는다`() {
        val policy = RestAlertPolicy()
        policy.onShown()
        policy.onDismissed()
        assertTrue(policy.shouldAlert(91, true, settings))
    }
}
