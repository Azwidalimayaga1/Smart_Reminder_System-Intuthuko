package com.intuthuko.smartreminder.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

var isDarkTheme by mutableStateOf(false)

private fun appColors() = if (isDarkTheme) darkColorScheme(
    primary = CobaltBlue, onPrimary = MidnightNavy, primaryContainer = LightBlue, onPrimaryContainer = CobaltBlue,
    secondary = PurpleAccent, onSecondary = MidnightNavy, secondaryContainer = PurpleLight, onSecondaryContainer = PurpleAccent,
    tertiary = SuccessGreen, onTertiary = MidnightNavy, background = WarmBackground, onBackground = MainText,
    surface = SurfaceWhite, onSurface = MainText, surfaceVariant = Gray100, onSurfaceVariant = SecondaryText,
    error = Danger, onError = MidnightNavy, errorContainer = HighPriorityBackground, onErrorContainer = Danger, outline = BorderColor
) else lightColorScheme(
    primary = CobaltBlue, onPrimary = Color.White, primaryContainer = LightBlue, onPrimaryContainer = CobaltBlue,
    secondary = PurpleAccent, onSecondary = Color.White, secondaryContainer = PurpleLight, onSecondaryContainer = PurpleAccent,
    tertiary = SuccessGreen, onTertiary = Color.White, background = WarmBackground, onBackground = MainText,
    surface = SurfaceWhite, onSurface = MainText, surfaceVariant = Gray100, onSurfaceVariant = SecondaryText,
    error = Danger, onError = Color.White, errorContainer = HighPriorityBackground, onErrorContainer = Danger, outline = BorderColor
)

@Composable
fun CampusSyncTheme(content: @Composable () -> Unit) {
    val revision = com.intuthuko.smartreminder.data.LocalStore.revision
    val theme = remember(revision) { com.intuthuko.smartreminder.data.LocalStore.prefs.getString("theme", "device") }
    isDarkTheme = when (theme) { "dark" -> true; "light" -> false; else -> androidx.compose.foundation.isSystemInDarkTheme() }
    val colorScheme = appColors()
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = WarmBackground.toArgb()
            window.navigationBarColor = SurfaceWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
        content = content
    )
}
