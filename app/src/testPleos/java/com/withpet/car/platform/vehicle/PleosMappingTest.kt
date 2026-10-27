package com.withpet.car.platform.vehicle

import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.constant.control.WiperSwitch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PleosMappingTest {

    @Test
    fun `창문 개방률을 차량 위치 범위로 변환한다`() {
        assertEquals(0, WindowPositionMapping.toPosition(0, 0, 10))
        assertEquals(1, WindowPositionMapping.toPosition(10, 0, 10))
        assertEquals(10, WindowPositionMapping.toPosition(100, 0, 10))
        assertEquals(25, WindowPositionMapping.toPosition(150, 0, 25)) // 범위 밖은 자른다
        assertEquals(-5, WindowPositionMapping.toPosition(50, -10, 0))
    }

    @Test
    fun `차량 위치를 개방률로 되돌린다`() {
        assertEquals(10, WindowPositionMapping.toPercent(1, 0, 10))
        assertEquals(0, WindowPositionMapping.toPercent(5, 5, 5))
        assertEquals(100, WindowPositionMapping.toPercent(99, 0, 10))
    }

    @Test
    fun `앱 풍량 1~5단계를 차량 범위에 비례 배분한다`() {
        assertEquals(1, FanLevelMapping.toSpeed(1, 1, 8))
        assertEquals(8, FanLevelMapping.toSpeed(5, 1, 8))
        assertEquals(3, FanLevelMapping.toSpeed(2, 1, 8)) // 1 + 7 × 0.25 = 2.75 → 3
        assertEquals(1, FanLevelMapping.toSpeed(0, 1, 8))
    }

    @Test
    fun `간헐·연속 와이퍼는 작동 상태와 상관없이 비로 본다`() {
        assertTrue(WiperRain.isRaining(WiperSwitch.INTERMITTENT_LEVEL_1, WiperState.OFF)!!)
        assertTrue(WiperRain.isRaining(WiperSwitch.CONTINUOUS_LEVEL_5, null)!!)
    }

    @Test
    fun `AUTO는 실제 작동 여부로 판단한다`() {
        assertTrue(WiperRain.isRaining(WiperSwitch.AUTO, WiperState.ON)!!)
        assertFalse(WiperRain.isRaining(WiperSwitch.AUTO, WiperState.OFF)!!)
        assertNull(WiperRain.isRaining(WiperSwitch.AUTO, null))
    }

    @Test
    fun `와이퍼가 꺼져 있거나 한 번 닦기면 비가 아니다`() {
        assertFalse(WiperRain.isRaining(WiperSwitch.OFF, null)!!)
        assertFalse(WiperRain.isRaining(WiperSwitch.MIST, WiperState.OFF)!!)
        assertTrue(WiperRain.isRaining(WiperSwitch.MIST, WiperState.ON)!!) // 작동 중이면 안전하게 비로 본다
    }

    @Test
    fun `스위치를 못 읽으면 작동 상태로, 둘 다 모르면 null`() {
        assertTrue(WiperRain.isRaining(null, WiperState.ON)!!)
        assertFalse(WiperRain.isRaining(null, WiperState.OFF)!!)
        assertNull(WiperRain.isRaining(null, null))
        assertNull(WiperRain.isRaining(WiperSwitch.SERVICE, WiperState.SERVICE))
    }
}
