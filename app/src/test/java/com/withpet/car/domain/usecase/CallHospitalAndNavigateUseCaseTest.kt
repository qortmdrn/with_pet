package com.withpet.car.domain.usecase

import com.withpet.car.domain.model.GeoPoint
import com.withpet.car.domain.model.RouteInfo
import com.withpet.car.hospital
import com.withpet.car.platform.navigation.NavigationController
import com.withpet.car.platform.phone.PhoneDialer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CallHospitalAndNavigateUseCaseTest {

    private val h = hospital("a")
    private val nav = mockk<NavigationController> {
        coEvery { startGuidance(any(), any()) } returns Result.success(Unit)
        every { activeRoute() } returns flowOf<RouteInfo?>(null)
    }

    @Test
    fun `휴대폰이 연결되어 있으면 전화와 길 안내를 함께 시작한다`() = runTest {
        val dialer = mockk<PhoneDialer> {
            every { isCallCapable() } returns flowOf(true)
        every { opensDialerOnly } returns false
            coEvery { call(any()) } returns Result.success(Unit)
        }
        val result = CallHospitalAndNavigateUseCase(dialer, nav)(h)

        assertEquals(CallAndNavigateResult(callStarted = true, guidanceStarted = true), result)
        coVerify { dialer.call(h.phone!!) }
        coVerify { nav.startGuidance(h.location, h.name) }
    }

    @Test
    fun `휴대폰 미연결이면 전화는 하지 않고 길 안내는 시작한다`() = runTest {
        val dialer = mockk<PhoneDialer> { every { isCallCapable() } returns flowOf(false) }
        val result = CallHospitalAndNavigateUseCase(dialer, nav)(h)

        assertEquals(CallAndNavigateResult(callStarted = false, guidanceStarted = true), result)
        coVerify(exactly = 0) { dialer.call(any()) }
        coVerify { nav.startGuidance(GeoPoint(h.location.lat, h.location.lng), h.name) }
    }

    @Test
    fun `다이얼러에 번호만 채우는 플랫폼이면 통화 버튼을 눌러야 한다고 알린다`() = runTest {
        val dialer = object : PhoneDialer {
            override fun isCallCapable() = flowOf(true)
            override suspend fun call(phoneNumber: String) = Result.success(Unit)
            override val opensDialerOnly = true
        }
        assertEquals(
            CallAndNavigateResult(callStarted = true, guidanceStarted = true, needsTapToCall = true),
            CallHospitalAndNavigateUseCase(dialer, nav)(h),
        )
    }

    @Test
    fun `전화번호가 없으면 전화는 하지 않고 길 안내는 시작한다`() = runTest {
        val dialer = mockk<PhoneDialer> { every { isCallCapable() } returns flowOf(true) }
        val result = CallHospitalAndNavigateUseCase(dialer, nav)(h.copy(phone = null))

        assertEquals(CallAndNavigateResult(callStarted = false, guidanceStarted = true), result)
        coVerify(exactly = 0) { dialer.call(any()) }
    }

    @Test
    fun `발신이 실패하면 번호 표시로 대체한다`() = runTest {
        val dialer = mockk<PhoneDialer> {
            every { isCallCapable() } returns flowOf(true)
        every { opensDialerOnly } returns false
            coEvery { call(any()) } returns Result.failure(IllegalStateException())
        }
        assertEquals(false, CallHospitalAndNavigateUseCase(dialer, nav)(h).callStarted)
    }
}
