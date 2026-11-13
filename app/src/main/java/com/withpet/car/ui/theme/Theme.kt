package com.withpet.car.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 밝은 톤 기본. 플레오스 디자인 가이드를 받으면 이 값을 교체한다.
object WithPetColors {
    val Background = Color(0xFFF4F5F7)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceHigh = Color(0xFFE9ECF1)
    val Outline = Color(0xFFD5DAE1)
    val Primary = Color(0xFFFFC145) // 버튼·토글 채움색 (위에 어두운 글자)
    val OnPrimary = Color(0xFF1A1300)
    val Accent = Color(0xFFA15C00) // 흰 바탕 위 글자·배지용 진한 호박색
    val Emergency = Color(0xFFD62F33)
    val OnEmergency = Color.White
    val Open = Color(0xFF15803D)
    val Unknown = Color(0xFFB45309)
    val Closed = Color(0xFF6B7280)
    val TextPrimary = Color(0xFF111827)
    val TextSecondary = Color(0xFF4B5563)
}

private val colors = lightColorScheme(
    primary = WithPetColors.Primary,
    onPrimary = WithPetColors.OnPrimary,
    primaryContainer = WithPetColors.Primary,
    onPrimaryContainer = WithPetColors.OnPrimary,
    secondary = WithPetColors.Accent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7B3),
    onSecondaryContainer = WithPetColors.OnPrimary,
    tertiary = WithPetColors.Accent,
    onTertiary = Color.White,
    background = WithPetColors.Background,
    onBackground = WithPetColors.TextPrimary,
    surface = WithPetColors.Surface,
    onSurface = WithPetColors.TextPrimary,
    surfaceVariant = WithPetColors.SurfaceHigh,
    onSurfaceVariant = WithPetColors.TextSecondary,
    surfaceTint = Color.Transparent,
    surfaceBright = WithPetColors.Surface,
    surfaceDim = WithPetColors.Background,
    surfaceContainerLowest = WithPetColors.Surface,
    surfaceContainerLow = WithPetColors.Surface,
    surfaceContainer = WithPetColors.Surface,
    surfaceContainerHigh = WithPetColors.SurfaceHigh,
    surfaceContainerHighest = WithPetColors.SurfaceHigh,
    outline = Color(0xFF9AA3AF),
    outlineVariant = WithPetColors.Outline,
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
