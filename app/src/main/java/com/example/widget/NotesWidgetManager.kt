package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotesWidgetManager {

    fun isPinningSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            return appWidgetManager.isRequestPinAppWidgetSupported
        }
        return false
    }

    fun pinWidget(
        context: Context,
        sizePreset: WidgetSizePreset = WidgetSizePreset.LARGE,
        widgetType: WidgetType = WidgetType.NOTES_LIST,
        studioConfig: WidgetConfig? = null
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val receiverClass = when {
                    sizePreset == WidgetSizePreset.SMALL || widgetType == WidgetType.QUICK_NOTE ->
                        QuickNoteGlanceReceiver::class.java
                    sizePreset == WidgetSizePreset.MEDIUM || widgetType == WidgetType.CHECKLIST ->
                        ChecklistGlanceReceiver::class.java
                    else -> NotesBoardGlanceReceiver::class.java
                }
                val provider = ComponentName(context, receiverClass)

                if (studioConfig != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        WidgetDataStore.saveStudioConfig(
                            context,
                            studioConfig.copy(sizePreset = sizePreset, widgetType = widgetType)
                        )
                    }
                }

                val callbackIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("widget_pinned_success", true)
                    putExtra("widget_type", widgetType.id)
                }
                val successCallback = PendingIntent.getActivity(
                    context,
                    sizePreset.ordinal + 990,
                    callbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val extras = Bundle().apply {
                    putString("widget_type", widgetType.id)
                    putString("widget_size", sizePreset.id)
                }

                return appWidgetManager.requestPinAppWidget(provider, extras, successCallback)
            }
        }
        return false
    }

    fun notifyDataChanged(context: Context) {
        NotesAppWidgetProvider.updateAllWidgets(context)
        CoroutineScope(Dispatchers.IO).launch {
            WidgetDataStore.bumpRevision(context)
            pushGlanceRevisionUpdate(context)
        }
    }
}
