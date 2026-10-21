package com.withpet.car.domain.logic

import com.withpet.car.CITY_HALL
import com.withpet.car.domain.model.NamedPoint
import com.withpet.car.domain.model.RouteInfo
import com.withpet.car.north
import com.withpet.car.spot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RestSpotSelectorTest {

    private val behind = spot("behind", CITY_HALL.north(-2.0))
    private val aheadNear = spot("aheadNear", CITY_HALL.north(6.0))
    private val aheadFar = spot("aheadFar", CITY_HALL.north(14.0))
    private val tooFar = spot("tooFar", CITY_HALL.north(16.0))

    @Test
    fun `경로가 있으면 진행 방향 앞쪽 15km 이내 중 가장 가까운 곳`() {
        val route = RouteInfo(NamedPoint(CITY_HALL.north(40.0), "목적지"), emptyList())
        val selected = RestSpotSelector.select(listOf(behind, aheadFar, aheadNear, tooFar), CITY_HALL, route)
        assertEquals("aheadNear", selected?.id)
    }

    @Test
    fun `경로가 있는데 앞쪽에 없으면 고르지 않는다`() {
        val route = RouteInfo(NamedPoint(CITY_HALL.north(40.0), "목적지"), emptyList())
        assertNull(RestSpotSelector.select(listOf(behind, tooFar), CITY_HALL, route))
    }

    @Test
    fun `경로가 없으면 반경 5km 이내 중 가장 가까운 곳`() {
        assertEquals("behind", RestSpotSelector.select(listOf(behind, aheadNear), CITY_HALL, null)?.id)
        assertNull(RestSpotSelector.select(listOf(aheadNear, aheadFar), CITY_HALL, null))
    }
}
