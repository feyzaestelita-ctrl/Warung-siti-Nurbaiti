package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = PandanGreen,
    onPrimary = Color.White,
    primaryContainer = SageSurface,
    onPrimaryContainer = PandanGreenDark,
    secondary = TerracottaAmber,
    onSecondary = Color.White,
    secondaryContainer = TerracottaSoft,
    onSecondaryContainer = TerracottaDark,
    tertiary = TurmericGold,
    onTertiary = InkCharcoal,
    tertiaryContainer = TurmericSoft,
    onTertiaryContainer = InkCharcoal,
    background = WarmIvory,
    onBackground = InkCharcoal,
    surface = CardWhite,
    onSurface = InkCharcoal,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = OutlineWarm,
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorSoft,
    onErrorContainer = ErrorCrimson
)

private val DarkColorScheme = darkColorScheme(
    primary = PandanGreenLight,
    onPrimary = PandanGreenDark,
    primaryContainer = SageContainerDark,
    onPrimaryContainer = PandanGreenLight,
    secondary = TerracottaAmber,
    onSecondary = Color.White,
    secondaryContainer = TerracottaDark,
    onSecondaryContainer = TerracottaSoft,
    tertiary = TurmericGold,
    onTertiary = InkCharcoal,
    background = DarkMarketBg,
    onBackground = WarmIvory,
    surface = DarkMarketSurface,
    onSurface = WarmIvory,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB5C4BA),
    outline = OutlineDark
)

val WarungShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun WarungNurbaitiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WarungTypography,
        shapes = WarungShapes,
        content = content
    )
}
