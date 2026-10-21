package com.withpet.car.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 다크 기본, 고대비. 플레오스 디자인 가이드를 받으면 이 값을 교체한다.
object WithPetColors {
    val Background = Color(0xFF0B0E13)
    val Surface = Color(0xFF181D25)
    val SurfaceHigh = Color(0xFF242B36)
    val Primary = Color(0xFFFFC145)
    val OnPrimary = Color(0xFF1A1300)
    val Emergency = Color(0xFFE5383B)
    val OnEmergency = Color.White
    val Open = Color(0xFF4ADE80)
    val Unknown = Color(0xFFFBBF24)
    val Closed = Color(0xFF9CA3AF)
    val TextPrimary = Color(0xFFF5F7FA)
    val TextSecondary = Color(0xFFB8C0CC)
}

private val colors = darkColorScheme(
    primary = WithPetColors.Primary,
    onPrimary = WithPetColors.OnPrimary,
    secondary = WithPetColors.Primary,
    onSecondary = WithPetColors.OnPrimary,
    background = WithPetColors.Background,
    onBackground = WithPetColors.TextPrimary,
    surface = WithPetColors.Surface,
    onSurface = WithPetColors.TextPrimary,
    surfaceVariant = WithPetColors.SurfaceHigh,
    onSurfaceVariant = WithPetColors.TextSecondary,
    surfaceContainerHigh = WithPetColors.SurfaceHigh,
    error = WithPetColors.Emergency,
    onError = WithPetColors.OnEmergency,
)

// 주행 중 화면 최소 24sp (명세 9장)
private val typography = Typography(
    headlineMedium = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp),
    bodyMedium = TextStyle(fontSize = 24.sp, lineHeight = 32.sp),
    labelLarge = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun WithPetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
