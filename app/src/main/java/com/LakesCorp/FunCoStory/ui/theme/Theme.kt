package com.LakesCorp.FunCoStory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = InkBlue,
    onPrimary = OnInkBlue,
    secondary = SunsetOrange,
    onSecondary = OnSunsetOrange,
    tertiary = SageGreen,
    background = WarmCream,
    onBackground = OnWarmCream,
    surface = WarmCream,
    onSurface = OnWarmCream,
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = OnSurfaceVariantColor,
    outline = OutlineColor,
    outlineVariant = OutlineVariantColor,
    primaryContainer = PrimaryContainerColor,
    onPrimaryContainer = OnPrimaryContainerColor,
    secondaryContainer = SecondaryContainerColor,
    onSecondaryContainer = OnSecondaryContainerColor,
    tertiaryContainer = TertiaryContainerColor,
    onTertiaryContainer = OnTertiaryContainerColor
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkInkBlue,
    onPrimary = DarkOnInkBlue,
    secondary = DarkSunsetOrange,
    onSecondary = DarkOnSunsetOrange,
    tertiary = DarkSageGreen,
    background = DarkWarmCream,
    onBackground = DarkOnWarmCream,
    surface = DarkWarmCream,
    onSurface = DarkOnWarmCream,
    surfaceVariant = DarkSurfaceVariantColor,
    onSurfaceVariant = DarkOnSurfaceVariantColor,
    outline = DarkOutlineColor,
    outlineVariant = DarkOutlineVariantColor,
    primaryContainer = DarkPrimaryContainerColor,
    onPrimaryContainer = DarkOnPrimaryContainerColor,
    secondaryContainer = DarkSecondaryContainerColor,
    onSecondaryContainer = DarkOnSecondaryContainerColor,
    tertiaryContainer = DarkTertiaryContainerColor,
    onTertiaryContainer = DarkOnTertiaryContainerColor
)

// Standard custom Typography mimicking the Ink & Echo design system
val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    bodyLarge = TextStyle( // For story-body
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    labelLarge = TextStyle( // ui-label-lg
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.05.sp
    ),
    labelSmall = TextStyle( // ui-label-sm
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.sp
    ),
    bodySmall = TextStyle( // caption
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
)

@Composable
fun InkAndEchoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
