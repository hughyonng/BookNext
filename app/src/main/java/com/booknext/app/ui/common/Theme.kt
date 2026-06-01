package com.booknext.app.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

data class AppTheme(
    val id: String,
    val name: String,
    val primary: Color,
    val background: Color,
    val surface: Color,
)

val AppThemes = listOf(
    AppTheme("blue",   "经典蓝", Color(0xFF1A73E8), Color(0xFFF8F9FA), Color.White),
    AppTheme("green",  "护眼绿", Color(0xFF2E7D32), Color(0xFFF1F8E9), Color(0xFFFFFFFF)),
    AppTheme("sepia",  "纸张棕", Color(0xFF795548), Color(0xFFFFF8E1), Color(0xFFFFF8E1)),
    AppTheme("purple", "暮光紫", Color(0xFF6A1B9A), Color(0xFFF3E5F5), Color.White),
    AppTheme("dark",   "深夜黑", Color(0xFF4DA3FF), Color(0xFF121212), Color(0xFF1E1E1E)),
)

val LocalAppTheme = compositionLocalOf { AppThemes[0] }

@Composable
fun BookNextTheme(
    themeId: String = "blue",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appTheme = AppThemes.find { it.id == themeId } ?: AppThemes[0]
    val isDark = darkTheme || themeId == "dark"

    val colors = if (isDark) {
        darkColorScheme(
            primary = Color(0xFF90CAF9),
            surface = Color(0xFF121212),
            background = Color(0xFF121212),
            onSurface = Color(0xFFE0E0E0),
            onBackground = Color(0xFFE0E0E0),
        )
    } else {
        lightColorScheme(primary = appTheme.primary, background = appTheme.background, surface = appTheme.surface)
    }

    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}