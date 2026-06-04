package com.vividtv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 全暗色主题
 * 严格执行 Dark Mode 规范，仅暗色调
 */
private val VividDarkColorScheme = darkColorScheme(
    primary = VividColors.AccentBlue,
    onPrimary = Color.White,
    primaryContainer = VividColors.AccentBlueDark,
    secondary = VividColors.AccentRed,
    onSecondary = Color.White,
    tertiary = VividColors.AccentGreen,

    background = VividColors.BackgroundDarkest,
    onBackground = VividColors.TextPrimary,
    surface = VividColors.BackgroundDark,
    onSurface = VividColors.TextPrimary,
    surfaceVariant = VividColors.BackgroundMedium,
    onSurfaceVariant = VividColors.TextSecondary,

    outline = VividColors.Divider,
    outlineVariant = VividColors.DividerAccent,

    error = VividColors.Error,
    onError = Color.White,
)

@Composable
fun VividTvTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = VividDarkColorScheme,
        content = content,
    )
}
