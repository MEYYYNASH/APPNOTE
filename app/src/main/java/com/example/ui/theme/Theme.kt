package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.model.AppSettings
import com.example.model.NoteColor
import com.example.model.ThemeOption

data class GlassTokens(
    val isDark: Boolean,
    val bgStart: Color,
    val bgEnd: Color,
    val paneFill: Color,          // --glass: rgba(255,255,255,.5) light / rgba(18,28,50,.5) dark
    val panelFill: Color,         // --card: rgba(255,255,255,.55) light / rgba(255,255,255,.06) dark
    val panelElevatedFill: Color, // --glass for modals/sheets
    val fieldFill: Color,         // --field: rgba(255,255,255,.6) light / rgba(255,255,255,.07) dark
    val panelBorder: Color,       // --edge: rgba(255,255,255,.75) light / rgba(255,255,255,.13) dark
    val textPrimary: Color,
    val textSecondary: Color,
    val accentStart: Color,
    val accentEnd: Color,
    val accentBrush: Brush,
    val animationsEnabled: Boolean,
    val compactMode: Boolean
)

val LocalGlassTokens = staticCompositionLocalOf {
    GlassTokens(
        isDark = false,
        bgStart = LightBgStart,
        bgEnd = LightBgEnd,
        paneFill = LightGlassPane,
        panelFill = LightGlassCard,
        panelElevatedFill = LightGlassPane,
        fieldFill = LightGlassField,
        panelBorder = LightGlassBorder,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        accentStart = NoteColor.BLUE.startColor,
        accentEnd = NoteColor.BLUE.endColor,
        accentBrush = Brush.linearGradient(listOf(NoteColor.BLUE.startColor, NoteColor.BLUE.endColor)),
        animationsEnabled = true,
        compactMode = false
    )
}

@Composable
fun GlassNotesTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.theme) {
        ThemeOption.SYSTEM -> systemDark
        ThemeOption.LIGHT -> false
        ThemeOption.DARK -> true
    }

    val accent = settings.accent
    val a1 = if (isDark && accent == NoteColor.BLUE) Color(0xFF4DA3FF) else accent.startColor
    val a2 = if (isDark && accent == NoteColor.BLUE) Color(0xFF9A7BFF) else accent.endColor

    val tokens = GlassTokens(
        isDark = isDark,
        bgStart = if (isDark) DarkBgStart else LightBgStart,
        bgEnd = if (isDark) DarkBgEnd else LightBgEnd,
        paneFill = if (isDark) DarkGlassPane else LightGlassPane,
        panelFill = if (isDark) DarkGlassCard else LightGlassCard,
        panelElevatedFill = if (isDark) DarkGlassPane else LightGlassPane,
        fieldFill = if (isDark) DarkGlassField else LightGlassField,
        panelBorder = if (isDark) DarkGlassBorder else LightGlassBorder,
        textPrimary = if (isDark) DarkTextPrimary else LightTextPrimary,
        textSecondary = if (isDark) DarkTextSecondary else LightTextSecondary,
        accentStart = a1,
        accentEnd = a2,
        accentBrush = Brush.linearGradient(listOf(a1, a2)),
        animationsEnabled = settings.animationsEnabled,
        compactMode = settings.compactMode
    )

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = a1,
            secondary = a2,
            background = DarkBgStart,
            surface = DarkGlassPane,
            onPrimary = Color.White,
            onBackground = DarkTextPrimary,
            onSurface = DarkTextPrimary,
            onSurfaceVariant = DarkTextSecondary
        )
    } else {
        lightColorScheme(
            primary = a1,
            secondary = a2,
            background = LightBgStart,
            surface = LightGlassPane,
            onPrimary = Color.White,
            onBackground = LightTextPrimary,
            onSurface = LightTextPrimary,
            onSurfaceVariant = LightTextSecondary
        )
    }

    val typography = buildAppTypography(
        fontOption = settings.font,
        scale = settings.fontScale,
        customFontPath = settings.customFontPath
    )

    CompositionLocalProvider(LocalGlassTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
