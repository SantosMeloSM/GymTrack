package com.gymtrack.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtrack.app.data.repo.ThemeMode

/** Extra semantic colors not covered by Material roles. */
@Immutable
data class GtColors(
    val success: Color,
    val record: Color,
    val warmup: Color,
    val chartGrid: Color,
    val completedRow: Color,
    val isDark: Boolean,
    val seriesA: Color,
    val seriesB: Color,
)

val LocalGtColors = staticCompositionLocalOf { DarkExtra }

private val Lime = Color(0xFFC8F135)
private val LimeDeep = Color(0xFF4A6E00)

private val DarkScheme = darkColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF0D0E11),
    primaryContainer = Color(0xFF2A3410),
    onPrimaryContainer = Color(0xFFE3FA9B),
    secondary = Color(0xFF8FB4FF),
    onSecondary = Color(0xFF0D0E11),
    secondaryContainer = Color(0xFF1E2A40),
    onSecondaryContainer = Color(0xFFD6E2FF),
    tertiary = Color(0xFFFFC24B),
    onTertiary = Color(0xFF1A1200),
    background = Color(0xFF0D0E11),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF0D0E11),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF23272E),
    onSurfaceVariant = Color(0xFF9EA4AE),
    surfaceContainerLowest = Color(0xFF0A0B0D),
    surfaceContainerLow = Color(0xFF131519),
    surfaceContainer = Color(0xFF171A1F),
    surfaceContainerHigh = Color(0xFF1E2127),
    surfaceContainerHighest = Color(0xFF262A31),
    outline = Color(0xFF3A3F48),
    outlineVariant = Color(0xFF2A2E35),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF1A0000),
    inverseSurface = Color(0xFFF3F4F6),
    inverseOnSurface = Color(0xFF111317),
)

private val LightScheme = lightColorScheme(
    primary = LimeDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF59A),
    onPrimaryContainer = Color(0xFF1A2600),
    secondary = Color(0xFF2F5AAE),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE6FF),
    onSecondaryContainer = Color(0xFF0B1E45),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color.White,
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF111317),
    surface = Color(0xFFF4F5F7),
    onSurface = Color(0xFF111317),
    surfaceVariant = Color(0xFFE6E8EC),
    onSurfaceVariant = Color(0xFF575D67),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAFB),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEFF1F4),
    surfaceContainerHighest = Color(0xFFE6E8EC),
    outline = Color(0xFFC3C7CE),
    outlineVariant = Color(0xFFDDE0E5),
    error = Color(0xFFC62828),
)

private val DarkExtra = GtColors(Color(0xFF5BD88A), Color(0xFFFFC24B), Color(0xFFFF9F5A), Color(0x1FFFFFFF), Color(0x1AC8F135), true, Color(0xFF80A20C), Color(0xFF5E8EEB))
private val LightExtra = GtColors(Color(0xFF1E8E4E), Color(0xFF9A6400), Color(0xFFB85A12), Color(0x1A000000), Color(0x1F4A6E00), false, Color(0xFF4A6E00), Color(0xFF2F5AAE))

private val Base = FontFamily.Default

val GtTypography = Typography(
    displayLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Black, fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Black, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
)

val GtShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun GymTrackTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalGtColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = GtTypography, shapes = GtShapes, content = content)
    }
}

object Gt {
    val colors: GtColors @Composable get() = LocalGtColors.current
}
