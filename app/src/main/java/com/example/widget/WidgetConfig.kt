package com.example.widget

import android.content.Context

enum class WidgetSizePreset(
    val id: String,
    val label: String,
    val cellsLabel: String
) {
    SMALL("small", "Small", "2×2"),
    MEDIUM("medium", "Medium", "4×2"),
    LARGE("large", "Large", "4×3")
}

enum class WidgetType(val id: String, val title: String) {
    QUICK_NOTE("quick_note", "Quick Note & Reminders"),
    CHECKLIST("checklist", "Interactive Checklist"),
    NOTES_LIST("notes_list", "Notes & Upcoming Feed"),
    UPCOMING_REMINDER("upcoming_reminder", "Upcoming Reminders"),
    STICKY_NOTE("sticky_note", "Single Pinned / Selected Note")
}

enum class WidgetGlassTheme(
    val id: String,
    val label: String,
    val bgHex: String,
    val baseColorInt: Int,
    val borderColorInt: Int,
    val isLightSurface: Boolean,
    val defaultAlphaPercent: Int
) {
    FROSTED_VIOLET(
        id = "frosted_violet",
        label = "Frosted Violet Glass",
        bgHex = "#1E1B4B",
        baseColorInt = 0x1E1B4B,
        borderColorInt = 0xFF8B5CF6.toInt(),
        isLightSurface = false,
        defaultAlphaPercent = 88
    ),
    DEEP_NEON(
        id = "deep_neon",
        label = "Midnight Slate Glass",
        bgHex = "#0F172A",
        baseColorInt = 0x0F172A,
        borderColorInt = 0xFF38BDF8.toInt(),
        isLightSurface = false,
        defaultAlphaPercent = 90
    ),
    DARK_AMOLED(
        id = "dark_amoled",
        label = "Dark AMOLED",
        bgHex = "#07090F",
        baseColorInt = 0x07090F,
        borderColorInt = 0xFF6366F1.toInt(),
        isLightSurface = false,
        defaultAlphaPercent = 95
    ),
    LIGHT_TRANSLUCENT(
        id = "light_translucent",
        label = "Light Frosted",
        bgHex = "#F8FAFC",
        baseColorInt = 0xF8FAFC,
        borderColorInt = 0xFFCBD5E1.toInt(),
        isLightSurface = true,
        defaultAlphaPercent = 85
    ),
    MINIMAL_BORDER(
        id = "minimal_border",
        label = "Minimal Translucent",
        bgHex = "#18181B",
        baseColorInt = 0x18181B,
        borderColorInt = 0x66FFFFFF,
        isLightSurface = false,
        defaultAlphaPercent = 72
    )
}

data class WidgetConfig(
    val appWidgetId: Int = 0,
    val sizePreset: WidgetSizePreset = WidgetSizePreset.LARGE,
    val widgetType: WidgetType = WidgetType.NOTES_LIST,
    val glassTheme: WidgetGlassTheme = WidgetGlassTheme.FROSTED_VIOLET,
    val transparencyPercent: Int = 88,
    val textSizeSp: Int = 13,
    val textColorHex: String = "#FFFFFF",
    val filterFolder: String = "",
    val selectedNoteId: String = "",
    val showUpcomingReminder: Boolean = true,
    val refreshIntervalMinutes: Int = 0
) {
    fun resolveBackgroundArgb(): Int {
        val alphaByte = ((transparencyPercent.coerceIn(25, 100) / 100f) * 255f).toInt().coerceIn(64, 255)
        return (alphaByte shl 24) or (glassTheme.baseColorInt and 0x00FFFFFF)
    }

    companion object {
        private const val PREFS_NAME = "glassnotes_widget_prefs"

        fun load(context: Context, appWidgetId: Int): WidgetConfig {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val suffix = if (appWidgetId > 0 && p.contains("type_$appWidgetId")) "_$appWidgetId" else "_studio"
            val sizeId = p.getString("size$suffix", WidgetSizePreset.LARGE.id) ?: WidgetSizePreset.LARGE.id
            val typeId = p.getString("type$suffix", WidgetType.NOTES_LIST.id) ?: WidgetType.NOTES_LIST.id
            val themeId = p.getString("theme$suffix", WidgetGlassTheme.FROSTED_VIOLET.id) ?: WidgetGlassTheme.FROSTED_VIOLET.id
            val alphaPct = p.getInt("alpha$suffix", 88).coerceIn(25, 100)
            val textSize = p.getInt("text_size$suffix", 13).coerceIn(10, 20)
            val textColor = p.getString("text_color$suffix", "#FFFFFF") ?: "#FFFFFF"
            val folder = p.getString("folder$suffix", "") ?: ""
            val noteId = p.getString("note_id$suffix", "") ?: ""
            val showReminder = p.getBoolean("reminder$suffix", true)
            val refreshMin = p.getInt("refresh$suffix", 0)

            return WidgetConfig(
                appWidgetId = appWidgetId,
                sizePreset = WidgetSizePreset.entries.find { it.id == sizeId } ?: WidgetSizePreset.LARGE,
                widgetType = WidgetType.entries.find { it.id == typeId } ?: WidgetType.NOTES_LIST,
                glassTheme = WidgetGlassTheme.entries.find { it.id == themeId } ?: WidgetGlassTheme.FROSTED_VIOLET,
                transparencyPercent = alphaPct,
                textSizeSp = textSize,
                textColorHex = textColor,
                filterFolder = folder,
                selectedNoteId = noteId,
                showUpcomingReminder = showReminder,
                refreshIntervalMinutes = refreshMin
            )
        }

        fun save(context: Context, config: WidgetConfig) {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val suffix = if (config.appWidgetId > 0) "_${config.appWidgetId}" else "_studio"
            p.edit()
                .putString("size$suffix", config.sizePreset.id)
                .putString("type$suffix", config.widgetType.id)
                .putString("theme$suffix", config.glassTheme.id)
                .putInt("alpha$suffix", config.transparencyPercent.coerceIn(25, 100))
                .putInt("text_size$suffix", config.textSizeSp.coerceIn(10, 20))
                .putString("text_color$suffix", config.textColorHex)
                .putString("folder$suffix", config.filterFolder)
                .putString("note_id$suffix", config.selectedNoteId)
                .putBoolean("reminder$suffix", config.showUpcomingReminder)
                .putInt("refresh$suffix", config.refreshIntervalMinutes)
                .apply()
        }
    }
}
