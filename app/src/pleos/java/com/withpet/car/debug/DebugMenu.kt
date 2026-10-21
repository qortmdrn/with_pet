package com.withpet.car.debug

import androidx.compose.runtime.Composable

/** 디버그 메뉴는 fake flavor에서만 제공한다. */
object DebugMenu {
    const val AVAILABLE = false

    @Composable
    fun Screen() = Unit
}
