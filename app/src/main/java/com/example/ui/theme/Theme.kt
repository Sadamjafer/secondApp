package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = VibrantPrimaryContainer,
    onPrimary = VibrantOnPrimaryContainer,
    primaryContainer = VibrantPrimary,
    onPrimaryContainer = VibrantOnPrimary,
    secondary = VibrantSecondary,
    onSecondary = VibrantOnSecondary,
    secondaryContainer = VibrantSecondaryContainer,
    onSecondaryContainer = VibrantOnSecondaryContainer,
    background = Color(0xFF191C1C),
    onBackground = Color(0xFFE1E3E2),
    surface = Color(0xFF191C1C),
    onSurface = Color(0xFFE1E3E2),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFC1C9C9)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = VibrantPrimary,
    onPrimary = VibrantOnPrimary,
    primaryContainer = VibrantPrimaryContainer,
    onPrimaryContainer = VibrantOnPrimaryContainer,
    secondary = VibrantSecondary,
    onSecondary = VibrantOnSecondary,
    secondaryContainer = VibrantSecondaryContainer,
    onSecondaryContainer = VibrantOnSecondaryContainer,
    background = VibrantBackground,
    onBackground = VibrantOnBackground,
    surface = VibrantSurface,
    onSurface = VibrantOnSurface,
    surfaceVariant = VibrantSurfaceVariant,
    onSurfaceVariant = VibrantOnSurfaceVariant,
    outline = VibrantOutline,
    error = VibrantError
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors by default so the Vibrant design is fully rendered
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
