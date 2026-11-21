package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.local.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimaryLight,
    onPrimary = OnCobaltPrimaryLight,
    primaryContainer = CobaltContainerLight,
    onPrimaryContainer = OnCobaltContainerLight,
    secondary = SignalTealSecondaryLight,
    onSecondary = OnSignalTealSecondaryLight,
    secondaryContainer = SignalTealContainerLight,
    onSecondaryContainer = OnSignalTealContainerLight,
    tertiary = CyanAccentTertiaryLight,
    onTertiary = OnCyanAccentTertiaryLight,
    tertiaryContainer = CyanAccentContainerLight,
    onTertiaryContainer = OnCyanAccentTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = CobaltPrimaryDark,
    onPrimary = OnCobaltPrimaryDark,
    primaryContainer = CobaltContainerDark,
    onPrimaryContainer = OnCobaltContainerDark,
    secondary = SignalTealSecondaryDark,
    onSecondary = OnSignalTealSecondaryDark,
    secondaryContainer = SignalTealContainerDark,
    onSecondaryContainer = OnSignalTealContainerDark,
    tertiary = CyanAccentTertiaryDark,
    onTertiary = OnCyanAccentTertiaryDark,
    tertiaryContainer = CyanAccentContainerDark,
    onTertiaryContainer = OnCyanAccentTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

val NetBillShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NetBillTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (useDarkTheme) DarkNetBillSemanticColors else LightNetBillSemanticColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDarkTheme
                controller.isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    CompositionLocalProvider(LocalNetBillColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = NetBillShapes,
            content = content
        )
    }
}
