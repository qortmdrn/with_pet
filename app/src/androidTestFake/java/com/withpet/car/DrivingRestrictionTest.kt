package com.withpet.car

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.withpet.car.domain.model.DrivingState
import com.withpet.car.platform.driving.FakeDrivingStateProvider
import com.withpet.car.ui.components.TestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class DrivingRestrictionTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var driving: FakeDrivingStateProvider

    @Before
    fun setUp() {
        hilt.inject()
    }

    @Test
    fun 주행_중_산책_스팟은_3곳만_보이고_필터가_숨겨진다() {
        driving.setState(DrivingState.DRIVING)
        compose.onNodeWithText("산책 스팟").performClick()

        compose.waitUntil(2_000) { compose.onAllNodesWithTag(TestTags.WALK_ITEM).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithTag(TestTags.WALK_ITEM).assertCountEquals(3)
        compose.onNodeWithTag(TestTags.MORE_AFTER_PARKING).assertExists()
        // 필터 칩 숨김
        compose.onNodeWithText("전체").assertDoesNotExist()
    }

    @Test
    fun 정차_중에는_전체_목록과_필터가_보인다() {
        driving.setState(DrivingState.PARKED)
        compose.onNodeWithText("산책 스팟").performClick()

        compose.waitUntil(2_000) { compose.onAllNodesWithTag(TestTags.WALK_ITEM).fetchSemanticsNodes().size > 3 }
        compose.onNodeWithTag(TestTags.MORE_AFTER_PARKING).assertDoesNotExist()
        compose.onNodeWithText("전체").assertExists()
    }
}
