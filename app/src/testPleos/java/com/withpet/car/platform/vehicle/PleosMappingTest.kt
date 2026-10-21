package com.withpet.car.platform.vehicle

import org.junit.jupiter.api.Assertions.assertEquals
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
}
