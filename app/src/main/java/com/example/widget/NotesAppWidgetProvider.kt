package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.NotesRepository
import com.example.model.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_CHECKLIST_ITEM = "com.example.widget.ACTION_TOGGLE_CHECKLIST_ITEM"
        const val ACTION_ITEM_CLICK = "com.example.widget.ACTION_ITEM_CLICK"
        const val ACTION_REFRESH_WIDGETS = "com.example.widget.ACTION_REFRESH_WIDGETS"

        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_ACTION_CREATE_NOTE = "extra_action_create_note"
        const val EXTRA_ACTION_QUICK_CAPTURE = "extra_action_quick_capture"
        const val EXTRA_ACTION_QUICK_REMINDER = "extra_action_quick_reminder"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, NotesAppWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (allWidgetIds.isNotEmpty()) {
                val intent = Intent(context, NotesAppWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)
                }
                context.sendBroadcast(intent)
                appWidgetManager.notifyAppWidgetViewDataChanged(allWidgetIds, R.id.widget_notes_listview)
                appWidgetManager.notifyAppWidgetViewDataChanged(allWidgetIds, R.id.widget_checklist_listview)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        updateAppWidget(context, appWidgetManager, appWidgetId, newOptions)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_ITEM_CLICK -> {
                val actionType = intent.getStringExtra("action_type") ?: ""
                val noteId = intent.getStringExtra("note_id") ?: ""
                val itemId = intent.getStringExtra("item_id") ?: ""

                if (actionType == "toggle_item" && noteId.isNotBlank() && itemId.isNotBlank()) {
                    // Check off task directly without opening app!
                    toggleChecklistItem(context, noteId, itemId)
                } else if (noteId.isNotBlank()) {
                    // Open note in app
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(EXTRA_NOTE_ID, noteId)
                    }
                    context.startActivity(openIntent)
                }
            }
            ACTION_TOGGLE_CHECKLIST_ITEM -> {
                val noteId = intent.getStringExtra(EXTRA_NOTE_ID) ?: ""
                val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: ""
                if (noteId.isNotBlank() && itemId.isNotBlank()) {
                    toggleChecklistItem(context, noteId, itemId)
                }
            }
            ACTION_REFRESH_WIDGETS -> {
                updateAllWidgets(context)
            }
        }
    }

    private fun toggleChecklistItem(context: Context, noteId: String, itemId: String) {
        val repo = NotesRepository(context)
        val allNotes = repo.loadNotes()
        val updated = allNotes.map { note ->
            if (note.id == noteId) {
                val newChecklist = note.checklist.map { item ->
                    if (item.id == itemId) item.copy(isDone = !item.isDone) else item
                }
                note.copy(checklist = newChecklist, updatedAt = System.currentTimeMillis())
            } else note
        }
        repo.saveNotes(updated)
        updateAllWidgets(context)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        options: Bundle? = null
    ) {
        val opts = options ?: appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 120)
        val minHeight = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        val config = WidgetConfig.load(context, appWidgetId)

        // Select layout based on configured widget type or dimensions
        val isSmall = config.widgetType == WidgetType.QUICK_NOTE || (minWidth < 180 && minHeight < 140)
        val isChecklist = config.widgetType == WidgetType.CHECKLIST || (minWidth >= 180 && minHeight < 150)

        val views: RemoteViews = if (isSmall) {
            buildQuickNoteView(context, appWidgetId, config)
        } else if (isChecklist) {
            buildChecklistView(context, appWidgetId, config)
        } else {
            buildNotesListView(context, appWidgetId, config)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun buildQuickNoteView(context: Context, appWidgetId: Int, config: WidgetConfig): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_note)
        val repo = NotesRepository(context)
        val recentNote = repo.loadNotes().firstOrNull { !it.isTrashed && !it.isArchived }

        if (recentNote != null) {
            views.setTextViewText(R.id.widget_quick_title, recentNote.title.ifBlank { "Recent Note" })
            views.setTextViewText(R.id.widget_quick_preview, recentNote.body.take(65).ifBlank { "Tap to edit note..." })
        } else {
            views.setTextViewText(R.id.widget_quick_title, "Quick Note")
            views.setTextViewText(R.id.widget_quick_preview, "Tap to capture a new thought...")
        }

        // Tap root opens recent note or quick capture
        val rootIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (recentNote != null) {
                putExtra(EXTRA_NOTE_ID, recentNote.id)
            } else {
                putExtra(EXTRA_ACTION_QUICK_CAPTURE, true)
            }
        }
        val rootPending = PendingIntent.getActivity(
            context,
            appWidgetId * 10 + 1,
            rootIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_quick_note_root, rootPending)

        // Tap New Note button opens new note creation
        val newNoteIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION_CREATE_NOTE, true)
        }
        val newNotePending = PendingIntent.getActivity(
            context,
            appWidgetId * 10 + 2,
            newNoteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_new_note, newNotePending)

        return views
    }

    private fun buildChecklistView(context: Context, appWidgetId: Int, config: WidgetConfig): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_checklist)
        val repo = NotesRepository(context)
        val all = repo.loadNotes().filter { !it.isTrashed && !it.isArchived }
        val checklistNote = all.find { it.isChecklist } ?: all.firstOrNull()

        if (checklistNote != null) {
            views.setTextViewText(R.id.widget_checklist_title, checklistNote.title.ifBlank { "Checklist" })
            val done = checklistNote.checklist.count { it.isDone }
            views.setTextViewText(R.id.widget_checklist_progress, "$done/${checklistNote.checklist.size} done")
        }

        // Connect ListView adapter
        val serviceIntent = Intent(context, NotesWidgetService::class.java).apply {
            putExtra("appWidgetId", appWidgetId)
            putExtra("mode", "checklist")
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_checklist_listview, serviceIntent)

        // PendingIntent template for item clicks (toggle checkbox)
        val itemClickIntent = Intent(context, NotesAppWidgetProvider::class.java).apply {
            action = ACTION_ITEM_CLICK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val itemClickPending = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + 3,
            itemClickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_checklist_listview, itemClickPending)

        // + Button to add new note or task in app
        val addIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (checklistNote != null) {
                putExtra(EXTRA_NOTE_ID, checklistNote.id)
            } else {
                putExtra(EXTRA_ACTION_CREATE_NOTE, true)
            }
        }
        val addPending = PendingIntent.getActivity(
            context,
            appWidgetId * 10 + 4,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_checklist_add_btn, addPending)

        return views
    }

    private fun buildNotesListView(context: Context, appWidgetId: Int, config: WidgetConfig): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_notes_list)
        val repo = NotesRepository(context)
        val now = System.currentTimeMillis()
        val all = repo.loadNotes().filter { !it.isTrashed && !it.isArchived }

        // Upcoming reminder banner
        val upcoming = all.filter { it.reminderAt != null && it.reminderAt > now }.minByOrNull { it.reminderAt!! }
        if (upcoming != null && upcoming.reminderAt != null) {
            val fmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(upcoming.reminderAt))
            views.setTextViewText(R.id.widget_reminder_badge, "⏰ $fmt")
            views.setViewVisibility(R.id.widget_reminder_badge, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_reminder_badge, View.GONE)
        }

        // Connect ListView adapter
        val serviceIntent = Intent(context, NotesWidgetService::class.java).apply {
            putExtra("appWidgetId", appWidgetId)
            putExtra("mode", "notes")
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_notes_listview, serviceIntent)

        // PendingIntent template for note card clicks (opens that exact note)
        val itemClickIntent = Intent(context, NotesAppWidgetProvider::class.java).apply {
            action = ACTION_ITEM_CLICK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val itemClickPending = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + 5,
            itemClickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_notes_listview, itemClickPending)

        // + Note Button in header
        val addNoteIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION_CREATE_NOTE, true)
        }
        val addNotePending = PendingIntent.getActivity(
            context,
            appWidgetId * 10 + 6,
            addNoteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_add_note_large, addNotePending)

        return views
    }
}
