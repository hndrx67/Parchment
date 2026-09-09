package org.hndrx.parchment.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import org.hndrx.parchment.settings.AppTheme
import org.hndrx.parchment.settings.AppPalette

fun paletteColors(palette: AppPalette, dark: Boolean): ColorScheme {
    val (lightAccent, darkAccent, lightPaper, darkPaper) = when (palette) {
        AppPalette.PARCHMENT -> listOf(0xFF944525, 0xFFFFB597, 0xFFFFFBF5, 0xFF191714)
        AppPalette.OLED_BLACK -> listOf(0xFF454545, 0xFFE3E3E3, 0xFFFAFAFA, 0xFF000000)
        AppPalette.MINT_GREEN -> listOf(0xFF006B4D, 0xFF83DDB8, 0xFFF3FCF6, 0xFF101B16)
        AppPalette.HAZE_PURPLE -> listOf(0xFF694397, 0xFFD5B5FA, 0xFFFDF7FF, 0xFF1B1523)
        AppPalette.OCEAN_BLUE -> listOf(0xFF005E8C, 0xFF92CCF4, 0xFFF4FAFF, 0xFF101A22)
        AppPalette.ROSE -> listOf(0xFF9A365D, 0xFFFFB1CC, 0xFFFFF7F9, 0xFF24151C)
    }
    val primary = Color(if (dark) darkAccent else lightAccent)
    val surface = Color(if (dark) darkPaper else lightPaper)
    val ink = if (dark) Color(0xFFF4EFF5) else Color(0xFF201E23)
    val container = lerp(surface, primary, if (dark) .20f else .13f)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary, onPrimary = if (dark) Color(0xFF17151A) else Color.White,
        primaryContainer = container, onPrimaryContainer = ink,
        secondary = primary, onSecondary = if (dark) Color(0xFF17151A) else Color.White,
        secondaryContainer = container, onSecondaryContainer = ink,
        tertiary = primary, onTertiary = if (dark) Color(0xFF17151A) else Color.White,
        tertiaryContainer = container, onTertiaryContainer = ink,
        background = surface, onBackground = ink, surface = surface, onSurface = ink,
        surfaceVariant = lerp(surface, primary, .10f), onSurfaceVariant = lerp(ink, surface, .20f),
        surfaceTint = primary, outline = lerp(ink, surface, .42f), outlineVariant = lerp(ink, surface, .74f),
        surfaceDim = surface, surfaceBright = lerp(surface, ink, .08f),
        surfaceContainerLowest = surface, surfaceContainerLow = lerp(surface, ink, .035f),
        surfaceContainer = lerp(surface, ink, .055f), surfaceContainerHigh = lerp(surface, ink, .08f),
        surfaceContainerHighest = lerp(surface, ink, .11f)
    )
}

@Composable
fun ParchmentTheme(appTheme: AppTheme = AppTheme.SYSTEM, palette: AppPalette = AppPalette.PARCHMENT, content: @Composable () -> Unit) {
    val dark = when (appTheme) { AppTheme.SYSTEM -> isSystemInDarkTheme(); AppTheme.LIGHT -> false; AppTheme.DARK -> true }
    val colors = paletteColors(palette, dark)
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(colors.background.toArgb()))
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = Typography(), shapes = Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    ), content = content)
}
