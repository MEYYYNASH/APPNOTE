package com.example.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "glassnotes_widget_settings"
)

object WidgetDataStore {
    private val KEY_SIZE_PRESET = stringPreferencesKey("studio_size_preset")
    private val KEY_WIDGET_TYPE = stringPreferencesKey("studio_widget_type")
    private val KEY_GLASS_THEME = stringPreferencesKey("studio_glass_theme")
    private val KEY_TRANSPARENCY_PCT = intPreferencesKey("studio_transparency_pct")
    private val KEY_TEXT_SIZE_SP = intPreferencesKey("studio_text_size_sp")
    private val KEY_TEXT_COLOR_HEX = stringPreferencesKey("studio_text_color_hex")
    private val KEY_FILTER_FOLDER = stringPreferencesKey("studio_filter_folder")
    private val KEY_SELECTED_NOTE_ID = stringPreferencesKey("studio_selected_note_id")
    private val KEY_SHOW_REMINDER = booleanPreferencesKey("studio_show_reminder")
    private val KEY_REFRESH_MINUTES = intPreferencesKey("studio_refresh_minutes")
    private val KEY_REVISION = intPreferencesKey("widget_data_revision")

    fun observeStudioConfig(context: Context): Flow<WidgetConfig> {
        return context.applicationContext.widgetPrefsDataStore.data.map { prefs ->
            mapPrefsToConfig(prefs, appWidgetId = 0)
        }
    }

    suspend fun getStudioConfig(context: Context): WidgetConfig {
        val prefs = context.applicationContext.widgetPrefsDataStore.data.first()
        return mapPrefsToConfig(prefs, appWidgetId = 0)
    }

    suspend fun getConfigForWidget(context: Context, appWidgetId: Int): WidgetConfig {
        val prefs = context.applicationContext.widgetPrefsDataStore.data.first()
        val hasPerWidget = prefs[stringPreferencesKey("w_${appWidgetId}_type")] != null
        return if (hasPerWidget) {
            WidgetConfig(
                appWidgetId = appWidgetId,
                sizePreset = WidgetSizePreset.entries.find {
                    it.id == prefs[stringPreferencesKey("w_${appWidgetId}_size")]
                } ?: WidgetSizePreset.LARGE,
                widgetType = WidgetType.entries.find {
                    it.id == prefs[stringPreferencesKey("w_${appWidgetId}_type")]
                } ?: WidgetType.NOTES_LIST,
                glassTheme = WidgetGlassTheme.entries.find {
                    it.id == prefs[stringPreferencesKey("w_${appWidgetId}_theme")]
                } ?: WidgetGlassTheme.FROSTED_VIOLET,
                transparencyPercent = (prefs[intPreferencesKey("w_${appWidgetId}_alpha")] ?: 88).coerceIn(25, 100),
                textSizeSp = (prefs[intPreferencesKey("w_${appWidgetId}_text_size")] ?: 13).coerceIn(10, 20),
                textColorHex = prefs[stringPreferencesKey("w_${appWidgetId}_text_color")] ?: "#FFFFFF",
                filterFolder = prefs[stringPreferencesKey("w_${appWidgetId}_folder")] ?: "",
                selectedNoteId = prefs[stringPreferencesKey("w_${appWidgetId}_note_id")] ?: "",
                showUpcomingReminder = prefs[booleanPreferencesKey("w_${appWidgetId}_reminder")] ?: true,
                refreshIntervalMinutes = prefs[intPreferencesKey("w_${appWidgetId}_refresh")] ?: 0
            )
        } else {
            mapPrefsToConfig(prefs, appWidgetId).also {
                val legacy = WidgetConfig.load(context, appWidgetId)
                if (appWidgetId > 0 && legacy.filterFolder.isNotBlank()) return legacy
            }
        }
    }

    suspend fun saveStudioConfig(context: Context, config: WidgetConfig) {
        WidgetConfig.save(context, config.copy(appWidgetId = 0))
        context.applicationContext.widgetPrefsDataStore.edit { prefs ->
            prefs[KEY_SIZE_PRESET] = config.sizePreset.id
            prefs[KEY_WIDGET_TYPE] = config.widgetType.id
            prefs[KEY_GLASS_THEME] = config.glassTheme.id
            prefs[KEY_TRANSPARENCY_PCT] = config.transparencyPercent.coerceIn(25, 100)
            prefs[KEY_TEXT_SIZE_SP] = config.textSizeSp.coerceIn(10, 20)
            prefs[KEY_TEXT_COLOR_HEX] = config.textColorHex
            prefs[KEY_FILTER_FOLDER] = config.filterFolder
            prefs[KEY_SELECTED_NOTE_ID] = config.selectedNoteId
            prefs[KEY_SHOW_REMINDER] = config.showUpcomingReminder
            prefs[KEY_REFRESH_MINUTES] = config.refreshIntervalMinutes
            prefs[KEY_REVISION] = (prefs[KEY_REVISION] ?: 0) + 1
        }
    }

    suspend fun savePerWidgetConfig(context: Context, config: WidgetConfig) {
        WidgetConfig.save(context, config)
        if (config.appWidgetId <= 0) {
            saveStudioConfig(context, config)
            return
        }
        val id = config.appWidgetId
        context.applicationContext.widgetPrefsDataStore.edit { prefs ->
            prefs[stringPreferencesKey("w_${id}_size")] = config.sizePreset.id
            prefs[stringPreferencesKey("w_${id}_type")] = config.widgetType.id
            prefs[stringPreferencesKey("w_${id}_theme")] = config.glassTheme.id
            prefs[intPreferencesKey("w_${id}_alpha")] = config.transparencyPercent.coerceIn(25, 100)
            prefs[intPreferencesKey("w_${id}_text_size")] = config.textSizeSp.coerceIn(10, 20)
            prefs[stringPreferencesKey("w_${id}_text_color")] = config.textColorHex
            prefs[stringPreferencesKey("w_${id}_folder")] = config.filterFolder
            prefs[stringPreferencesKey("w_${id}_note_id")] = config.selectedNoteId
            prefs[booleanPreferencesKey("w_${id}_reminder")] = config.showUpcomingReminder
            prefs[intPreferencesKey("w_${id}_refresh")] = config.refreshIntervalMinutes
            prefs[KEY_REVISION] = (prefs[KEY_REVISION] ?: 0) + 1
        }
    }

    suspend fun bumpRevision(context: Context) {
        context.applicationContext.widgetPrefsDataStore.edit { prefs ->
            prefs[KEY_REVISION] = (prefs[KEY_REVISION] ?: 0) + 1
        }
    }

    private fun mapPrefsToConfig(prefs: Preferences, appWidgetId: Int): WidgetConfig {
        return WidgetConfig(
            appWidgetId = appWidgetId,
            sizePreset = WidgetSizePreset.entries.find { it.id == prefs[KEY_SIZE_PRESET] }
                ?: WidgetSizePreset.LARGE,
            widgetType = WidgetType.entries.find { it.id == prefs[KEY_WIDGET_TYPE] }
                ?: WidgetType.NOTES_LIST,
            glassTheme = WidgetGlassTheme.entries.find { it.id == prefs[KEY_GLASS_THEME] }
                ?: WidgetGlassTheme.FROSTED_VIOLET,
            transparencyPercent = (prefs[KEY_TRANSPARENCY_PCT] ?: 88).coerceIn(25, 100),
            textSizeSp = (prefs[KEY_TEXT_SIZE_SP] ?: 13).coerceIn(10, 20),
            textColorHex = prefs[KEY_TEXT_COLOR_HEX] ?: "#FFFFFF",
            filterFolder = prefs[KEY_FILTER_FOLDER] ?: "",
            selectedNoteId = prefs[KEY_SELECTED_NOTE_ID] ?: "",
            showUpcomingReminder = prefs[KEY_SHOW_REMINDER] ?: true,
            refreshIntervalMinutes = prefs[KEY_REFRESH_MINUTES] ?: 0
        )
    }
}
