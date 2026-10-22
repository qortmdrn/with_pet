package com.withpet.car

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.withpet.car.platform.navigation.FakeNavigationController
import com.withpet.car.platform.phone.FakePhoneDialer
import com.withpet.car.ui.components.TestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class EmergencyFlowTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var dialer: FakePhoneDialer
    @Inject lateinit var navigation: FakeNavigationController

    @Before
    fun setUp() {
        hilt.inject()
    }

    @Test
    fun 응급_버튼_한_번으로_진료_중인_가장_가까운_병원에_전화와_길_안내가_시작된다() {
        compose.onNodeWithTag(TestTags.EMERGENCY_BUTTON).performClick()

        // 목록을 거치지 않고 바로 결과 화면 (응급 2초 이내, 명세 7-2 수용 기준)
        compose.waitUntil(2_000) { compose.onAllNodesWithText("전화를 연결하고 있어요").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("가장 빨리 도착하는 진료 중 병원").assertExists()
        compose.onAllNodesWithTag(TestTags.EMERGENCY_ITEM).assertCountEquals(0)

        assertEquals(1, dialer.calledNumbers.size)
        val route = navigation.currentRoute.value
        assertTrue(route != null && route.destination.name.startsWith("테스트 동물병원"))
    }

    @Test
    fun 다른_병원_보기로_목록에서_다른_병원을_고를_수_있다() {
        compose.onNodeWithTag(TestTags.EMERGENCY_BUTTON).performClick()
        compose.waitUntil(2_000) { compose.onAllNodesWithText("다른 병원 보기").fetchSemanticsNodes().isNotEmpty() }
        val first = navigation.currentRoute.value!!.destination.name

        compose.onNodeWithText("다른 병원 보기").performClick()
        compose.waitUntil(2_000) { compose.onAllNodesWithTag(TestTags.EMERGENCY_ITEM).fetchSemanticsNodes().size >= 2 }
        compose.onAllNodesWithTag(TestTags.EMERGENCY_ITEM)[1].performClick()

        compose.waitUntil(2_000) { navigation.currentRoute.value?.destination?.name != first }
        assertEquals(2, dialer.calledNumbers.size)
    }

    @Test
    fun 휴대폰_미연결이면_번호를_크게_보여주고_길_안내는_시작한다() {
        dialer.setConnected(false)
        compose.onNodeWithTag(TestTags.EMERGENCY_BUTTON).performClick()

        compose.waitUntil(2_000) { compose.onAllNodesWithText("휴대폰을 연결하면 바로 전화할 수 있어요").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("010-0000-", substring = true).assertCountEquals(1)
        assertTrue(dialer.calledNumbers.isEmpty())
        assertTrue(navigation.currentRoute.value != null)
    }
}
