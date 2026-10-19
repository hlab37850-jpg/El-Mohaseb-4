package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MohasebDarkColorScheme = darkColorScheme(
  primary = MohasebLightBlue,
  onPrimary = MohasebDarkBlue,
  primaryContainer = MohasebDarkBlue,
  onPrimaryContainer = Color.White,
  secondary = MohasebCyanAccent,
  onSecondary = Color.White,
  background = Color(0xFF0F172A),
  surface = Color(0xFF1E293B),
  onBackground = Color(0xFFF8FAFC),
  onSurface = Color(0xFFF8FAFC)
)

private val MohasebLightColorScheme = lightColorScheme(
  primary = MohasebPrimaryBlue,
  onPrimary = Color.White,
  primaryContainer = MohasebLightBlue,
  onPrimaryContainer = MohasebDarkBlue,
  secondary = MohasebCyanAccent,
  onSecondary = Color.White,
  background = MohasebBackground,
  surface = MohasebSurface,
  onBackground = MohasebTextPrimary,
  onSurface = MohasebTextPrimary,
  surfaceVariant = Color(0xFFEBF2F8),
  onSurfaceVariant = MohasebTextSecondary,
  outline = MohasebBorder
)

@Composable
fun MohasebSoftTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) MohasebDarkColorScheme else MohasebLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
