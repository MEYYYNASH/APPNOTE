package com.example.widget

import android.content.Context
import android.content.SharedPreferences

enum class WidgetType(val id: String, val title: String) {
    QUICK_NOTE("quick_note", "Quick Note Launcher (Small)"),
    CHECKLIST("checklist", "Interactive Checklist (Medium)"),
    NOTES_LIST("notes_list", "Neon Bento Notes Grid (Large)"),
    UPCOMING_REMINDER("upcoming_reminder", "Upcoming Reminders (Medium)"),
    STICKY_NOTE("sticky_note", "Single Sticky Note (Small/Medium)")
}

enum class WidgetGlassTheme(val id: String, val label: String, val bgHex: String, val alphaPercent: Int) {
    FROSTED_VIOLET("frosted_violet", "Frosted Violet Glass", "#E61E1B4B", 85),
    DEEP_NEON("deep_neon", "Deep Neon Cyan", "#E60F172A", 90),
    DARK_AMOLED("dark_amoled", "Dark AMOLED Glass", "#F2000000", 95),
    LIGHT_TRANSLUCENT("light_translucent", "Light Translucent", "#CCFFFFFF", 80),
    MINIMAL_BORDER("minimal_border", "Minimal Specular", "#B318181B", 70)
}

data class WidgetConfig(
    val appWidgetId: Int = 0,
    val widgetType: WidgetType = WidgetType.NOTES_LIST,
    val glassTheme: WidgetGlassTheme = WidgetGlassTheme.FROSTED_VIOLET,
    val textSizeSp: Int = 12,
    val filterFolder: String = "",
    val selectedNoteId: String = "",
    val showUpcomingReminder: Boolean = true
) {
    companion object {
        private const val PREFS_NAME = "glassnotes_widget_prefs"

        fun load(context: Context, appWidgetId: Int): WidgetConfig {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val typeId = p.getString("type_$appWidgetId", WidgetType.NOTES_LIST.id) ?: WidgetType.NOTES_LIST.id
            val themeId = p.getString("theme_$appWidgetId", WidgetGlassTheme.FROSTED_VIOLET.id) ?: WidgetGlassTheme.FROSTED_VIOLET.id
            val textSize = p.getInt("text_size_$appWidgetId", 12)
            val folder = p.getString("folder_$appWidgetId", "") ?: ""
            val noteId = p.getString("note_id_$appWidgetId", "") ?: ""
            val showReminder = p.getBoolean("reminder_$appWidgetId", true)

            return WidgetConfig(
                appWidgetId = appWidgetId,
                widgetType = WidgetType.entries.find { it.id == typeId } ?: WidgetType.NOTES_LIST,
                glassTheme = WidgetGlassTheme.entries.find { it.id == themeId } ?: WidgetGlassTheme.FROSTED_VIOLET,
                textSizeSp = textSize,
                filterFolder = folder,
                selectedNoteId = noteId,
                showUpcomingReminder = showReminder
            )
        }

        fun save(context: Context, config: WidgetConfig) {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            p.edit()
                .putString("type_${config.appWidgetId}", config.widgetType.id)
                .putString("theme_${config.appWidgetId}", config.glassTheme.id)
                .putInt("text_size_${config.appWidgetId}", config.textSizeSp)
                .putString("folder_${config.appWidgetId}", config.filterFolder)
                .putString("note_id_${config.appWidgetId}", config.selectedNoteId)
                .putBoolean("reminder_${config.appWidgetId}", config.showUpcomingReminder)
                .apply()
        }
    }
}
