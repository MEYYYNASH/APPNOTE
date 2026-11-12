package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.example.MainActivity

object NotesWidgetManager {

    fun isPinningSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            return appWidgetManager.isRequestPinAppWidgetSupported
        }
        return false
    }

    /**
     * Pins a widget directly to the user's Android home screen.
     * Works on Android 8.0+ (Oreo, Pie, 10, 11, 12, 13, 14, 15, 16).
     */
    fun pinWidget(context: Context, widgetType: WidgetType): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val provider = ComponentName(context, NotesAppWidgetProvider::class.java)

                val callbackIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("widget_pinned_success", true)
                    putExtra("widget_type", widgetType.id)
                }
                val successCallback = PendingIntent.getActivity(
                    context,
                    991,
                    callbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val extras = Bundle().apply {
                    putString("widget_type", widgetType.id)
                }

                return appWidgetManager.requestPinAppWidget(provider, extras, successCallback)
            }
        }
        return false
    }

    fun notifyDataChanged(context: Context) {
        NotesAppWidgetProvider.updateAllWidgets(context)
    }
}
