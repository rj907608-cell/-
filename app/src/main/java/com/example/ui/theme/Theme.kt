package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = PharmacyPrimaryDark,
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFFA7F0E3),
    secondary = PharmacySecondaryDark,
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFFCCE8E2),
    tertiary = PharmacyTertiaryDark,
    onTertiary = Color(0xFF00344F),
    tertiaryContainer = Color(0xFF004C70),
    onTertiaryContainer = Color(0xFFC3E8FF),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE2E2E2),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE2E2E2),
    surfaceVariant = Color(0xFF2B2B2B),
    onSurfaceVariant = Color(0xFFC4C7C5)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PharmacyPrimary,
    onPrimary = PharmacyOnPrimary,
    primaryContainer = PharmacyPrimaryContainer,
    onPrimaryContainer = PharmacyOnPrimaryContainer,
    secondary = PharmacySecondary,
    secondaryContainer = PharmacySecondaryContainer,
    onSecondaryContainer = PharmacyOnSecondaryContainer,
    tertiary = PharmacyTertiary,
    tertiaryContainer = PharmacyTertiaryContainer,
    onTertiaryContainer = PharmacyOnTertiaryContainer,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
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
