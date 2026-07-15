package com.tankermanager.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Fresh water-fleet palette — teal lagoon + coral CTA (not purple / not cream-serif cliché)
val LagoonDeep = Color(0xFF0B5F5C)
val Lagoon = Color(0xFF128C87)
val LagoonSoft = Color(0xFF7FD4CF)
val Foam = Color(0xFFF3FBFB)
val Mist = Color(0xFFE4F4F3)
val Coral = Color(0xFFE35D4A)
val CoralSoft = Color(0xFFFFE8E4)
val Ink = Color(0xFF132726)
val InkMuted = Color(0xFF5A7270)
val Sun = Color(0xFFF0B429)
val Success = Color(0xFF2F9E7B)
val Warning = Color(0xFFE0A100)

val HeroBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF0B5F5C), Color(0xFF167C78), Color(0xFF2AA39C))
)

val CardBrush = Brush.linearGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF0FAF9))
)

private val LightColors = lightColorScheme(
    primary = LagoonDeep,
    onPrimary = Color.White,
    primaryContainer = Mist,
    onPrimaryContainer = LagoonDeep,
    secondary = Coral,
    onSecondary = Color.White,
    secondaryContainer = CoralSoft,
    onSecondaryContainer = Color(0xFF5C1C14),
    tertiary = Sun,
    background = Foam,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = InkMuted,
    outline = Color(0xFFB7D0CD),
    error = Color(0xFFC62828)
)

private val DarkColors = darkColorScheme(
    primary = LagoonSoft,
    onPrimary = Color(0xFF003734),
    primaryContainer = LagoonDeep,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFFFB4A8),
    onSecondary = Color(0xFF5C1C14),
    background = Color(0xFF0C1B1A),
    onBackground = Color(0xFFE6F5F3),
    surface = Color(0xFF122422),
    onSurface = Color(0xFFE6F5F3),
    surfaceVariant = Color(0xFF1C3331),
    onSurfaceVariant = Color(0xFFB7CDC9)
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )
)

fun <T> bubbly() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow
)

@Composable
fun TankerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
