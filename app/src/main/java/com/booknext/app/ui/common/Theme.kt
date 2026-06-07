package com.booknext.app.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

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
    uiFontScale: Float = 1.0f,
    uiFontFamily: String = "sans-serif",
    uiLineSpacing: Float = 1.5f,
    content: @Composable () -> Unit
) {
    val appTheme = AppThemes.find { it.id == themeId } ?: AppThemes[0]
    val isDark = darkTheme || themeId == "dark"

    val colors = if (isDark) {
        darkColorScheme(
            primary = Color(0xFF90CAF9),
            surface = Color(0xFF1E2428),
            background = Color(0xFF1E2428),
            onSurface = Color(0xFFE0E0E0),
            onBackground = Color(0xFFE0E0E0),
        )
    } else {
        lightColorScheme(
            primary = appTheme.primary,
            background = appTheme.background,
            surface = appTheme.surface,
            primaryContainer = appTheme.primary.copy(alpha = 0.12f),
            onPrimaryContainer = appTheme.primary,
            secondaryContainer = appTheme.primary.copy(alpha = 0.08f),
            onSecondaryContainer = appTheme.primary,
        )
    }

    val fontFamily = when (uiFontFamily) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }

    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colors,
            typography = uiTypography(uiFontScale, fontFamily, uiLineSpacing),
            content = content,
        )
    }
}

private fun uiTypography(scale: Float, fontFamily: FontFamily, lineSpacing: Float): Typography {
    val base = Typography()
    val ls = lineSpacing / 1.5f
    fun Float.scaled() = (this * scale).sp
    fun Float.lined() = (this * scale * ls).sp
    return Typography(
        displayLarge   = base.displayLarge.copy(fontSize = base.displayLarge.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.displayLarge.lineHeight.value.lined()),
        displayMedium  = base.displayMedium.copy(fontSize = base.displayMedium.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.displayMedium.lineHeight.value.lined()),
        displaySmall   = base.displaySmall.copy(fontSize = base.displaySmall.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.displaySmall.lineHeight.value.lined()),
        headlineLarge  = base.headlineLarge.copy(fontSize = base.headlineLarge.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.headlineLarge.lineHeight.value.lined()),
        headlineMedium = base.headlineMedium.copy(fontSize = base.headlineMedium.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.headlineMedium.lineHeight.value.lined()),
        headlineSmall  = base.headlineSmall.copy(fontSize = base.headlineSmall.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.headlineSmall.lineHeight.value.lined()),
        titleLarge     = base.titleLarge.copy(fontSize = base.titleLarge.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.titleLarge.lineHeight.value.lined()),
        titleMedium    = base.titleMedium.copy(fontSize = base.titleMedium.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.titleMedium.lineHeight.value.lined()),
        titleSmall     = base.titleSmall.copy(fontSize = base.titleSmall.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.titleSmall.lineHeight.value.lined()),
        bodyLarge      = base.bodyLarge.copy(fontSize = base.bodyLarge.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.bodyLarge.lineHeight.value.lined()),
        bodyMedium     = base.bodyMedium.copy(fontSize = base.bodyMedium.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.bodyMedium.lineHeight.value.lined()),
        bodySmall      = base.bodySmall.copy(fontSize = base.bodySmall.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.bodySmall.lineHeight.value.lined()),
        labelLarge     = base.labelLarge.copy(fontSize = base.labelLarge.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.labelLarge.lineHeight.value.lined()),
        labelMedium    = base.labelMedium.copy(fontSize = base.labelMedium.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.labelMedium.lineHeight.value.lined()),
        labelSmall     = base.labelSmall.copy(fontSize = base.labelSmall.fontSize.value.scaled(), fontFamily = fontFamily, lineHeight = base.labelSmall.lineHeight.value.lined()),
    )
}
