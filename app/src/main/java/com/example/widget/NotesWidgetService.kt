package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.NotesRepository
import com.example.model.ChecklistItem
import com.example.model.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra("appWidgetId", 0)
        val mode = intent.getStringExtra("mode") ?: "notes"
        return NotesWidgetRemoteViewsFactory(applicationContext, appWidgetId, mode)
    }
}

class NotesWidgetRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int,
    private val mode: String
) : RemoteViewsService.RemoteViewsFactory {

    private var notes: List<Note> = emptyList()
    private var checklistNote: Note? = null
    private var checklistItems: List<ChecklistItem> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault())

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        val repo = NotesRepository(context)
        val all = repo.loadNotes().filter { !it.isTrashed && !it.isArchived }
        val config = WidgetConfig.load(context, appWidgetId)

        if (mode == "checklist") {
            // Find specified checklist or first checklist note
            checklistNote = if (config.selectedNoteId.isNotBlank()) {
                all.find { it.id == config.selectedNoteId } ?: all.find { it.isChecklist }
            } else {
                all.find { it.isChecklist } ?: all.firstOrNull()
            }
            checklistItems = checklistNote?.checklist.orEmpty()
        } else {
            // Filter by folder if configured
            notes = if (config.filterFolder.isNotBlank()) {
                all.filter { it.folder.equals(config.filterFolder, ignoreCase = true) }
            } else {
                all
            }
        }
    }

    override fun onDestroy() {
        notes = emptyList()
        checklistItems = emptyList()
    }

    override fun getCount(): Int {
        return if (mode == "checklist") checklistItems.size else notes.size
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (mode == "checklist") {
            if (position < 0 || position >= checklistItems.size) return null
            val item = checklistItems[position]
            val views = RemoteViews(context.packageName, R.layout.widget_checklist_item)

            views.setTextViewText(R.id.widget_item_text, item.text)
            if (item.isDone) {
                views.setImageViewResource(R.id.widget_item_checkbox, R.drawable.ic_widget_check)
                views.setTextColor(R.id.widget_item_text, Color.parseColor("#88FFFFFF"))
            } else {
                views.setImageViewResource(R.id.widget_item_checkbox, R.drawable.ic_widget_circle)
                views.setTextColor(R.id.widget_item_text, Color.parseColor("#FFFFFFFF"))
            }

            // Fill-in Intent for toggle click
            val fillInIntent = Intent().apply {
                putExtra("action_type", "toggle_item")
                putExtra("note_id", checklistNote?.id.orEmpty())
                putExtra("item_id", item.id)
            }
            views.setOnClickFillInIntent(R.id.widget_checklist_item_root, fillInIntent)
            return views
        } else {
            if (position < 0 || position >= notes.size) return null
            val note = notes[position]
            val views = RemoteViews(context.packageName, R.layout.widget_note_item)

            views.setTextViewText(R.id.widget_item_title, note.title.ifBlank { "Untitled" })
            views.setTextViewText(R.id.widget_item_time, dateFormat.format(Date(note.updatedAt)))

            val previewText = if (note.isChecklist) {
                val done = note.checklist.count { it.isDone }
                "$done of ${note.checklist.size} completed"
            } else {
                note.body.replace("\n", " ").take(65).ifBlank { "No text yet" }
            }
            views.setTextViewText(R.id.widget_item_preview, previewText)

            // Fill-in Intent to open note in MainActivity
            val fillInIntent = Intent().apply {
                putExtra("action_type", "open_note")
                putExtra("note_id", note.id)
            }
            views.setOnClickFillInIntent(R.id.widget_note_item_root, fillInIntent)
            return views
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
