package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.example.model.ChecklistItem
import com.example.model.EditorFontStyle
import com.example.model.Note
import com.example.model.NoteColor
import com.example.model.RichTextSpan
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val isChecklist: Boolean,
    val checklistJson: String,
    val colorId: String,
    val isPinned: Boolean,
    val isFavorite: Boolean,
    val isArchived: Boolean,
    val isTrashed: Boolean,
    val trashedAt: Long?,
    val reminderAt: Long?,
    val reminderFired: Boolean,
    val reminderSoundStyle: String,
    val reminderAlertMode: String,
    val reminderVibrationPattern: String,
    val reminderVolume: Int,
    val tagsCsv: String,
    val folder: String,
    val richMetaJson: String = "{}",
    val createdAt: Long,
    val updatedAt: Long
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getActiveNotesSync(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND reminderAt IS NOT NULL AND reminderAt >= :minTime ORDER BY reminderAt ASC")
    suspend fun getUpcomingReminders(minTime: Long): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(notes: List<NoteEntity>) {
        clearAll()
        upsertAll(notes)
    }
}

@Database(entities = [NoteEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glassnotes_room.db"
                ).fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

fun Note.toEntity(): NoteEntity {
    val clArr = JSONArray()
    checklist.forEach { item ->
        clArr.put(JSONObject().apply {
            put("id", item.id)
            put("text", item.text)
            put("isDone", item.isDone)
        })
    }
    val spansArr = JSONArray()
    richSpans.forEach { sp ->
        spansArr.put(JSONObject().apply {
            put("start", sp.start)
            put("end", sp.end)
            if (sp.isBold) put("isBold", true)
            if (sp.isItalic) put("isItalic", true)
            if (sp.isUnderline) put("isUnderline", true)
            if (sp.isStrikethrough) put("isStrikethrough", true)
            if (sp.highlightColorArgb != null) put("highlightColorArgb", sp.highlightColorArgb)
            if (sp.textColorArgb != null) put("textColorArgb", sp.textColorArgb)
            if (sp.fontSizeSp != null) put("fontSizeSp", sp.fontSizeSp.toDouble())
            if (!sp.fontStyleId.isNullOrBlank()) put("fontStyleId", sp.fontStyleId)
            if (sp.headingLevel > 0) put("headingLevel", sp.headingLevel)
            if (sp.isQuote) put("isQuote", true)
            if (sp.isCode) put("isCode", true)
            if (!sp.linkUrl.isNullOrBlank()) put("linkUrl", sp.linkUrl)
        })
    }
    val richMeta = JSONObject().apply {
        put("richSpans", spansArr)
        put("bodyFontStyle", bodyFontStyle)
        put("bodyFontSizeSp", bodyFontSizeSp.toDouble())
        if (bodyTextColorArgb != null) put("bodyTextColorArgb", bodyTextColorArgb)
        put("bodyTextAlign", bodyTextAlign)
    }
    return NoteEntity(
        id = id,
        title = title,
        body = body,
        isChecklist = isChecklist,
        checklistJson = clArr.toString(),
        colorId = color.id,
        isPinned = isPinned,
        isFavorite = isFavorite,
        isArchived = isArchived,
        isTrashed = isTrashed,
        trashedAt = trashedAt,
        reminderAt = reminderAt,
        reminderFired = reminderFired,
        reminderSoundStyle = reminderSoundStyle,
        reminderAlertMode = reminderAlertMode,
        reminderVibrationPattern = reminderVibrationPattern,
        reminderVolume = reminderVolume,
        tagsCsv = tags.joinToString(","),
        folder = folder,
        richMetaJson = richMeta.toString(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun NoteEntity.toNote(): Note {
    val parsedChecklist = mutableListOf<ChecklistItem>()
    try {
        val arr = JSONArray(checklistJson)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            parsedChecklist.add(
                ChecklistItem(
                    id = o.optString("id"),
                    text = o.optString("text"),
                    isDone = o.optBoolean("isDone", false)
                )
            )
        }
    } catch (_: Exception) {
    }
    val parsedSpans = mutableListOf<RichTextSpan>()
    var parsedFontStyle = EditorFontStyle.SANS.id
    var parsedFontSizeSp = 16f
    var parsedTextColor: Long? = null
    var parsedTextAlign = "start"
    try {
        if (richMetaJson.isNotBlank()) {
            val meta = JSONObject(richMetaJson)
            parsedFontStyle = meta.optString("bodyFontStyle", EditorFontStyle.SANS.id)
            parsedFontSizeSp = meta.optDouble("bodyFontSizeSp", 16.0).toFloat()
            parsedTextColor = if (meta.has("bodyTextColorArgb")) meta.optLong("bodyTextColorArgb") else null
            parsedTextAlign = meta.optString("bodyTextAlign", "start")
            val spArr = meta.optJSONArray("richSpans")
            if (spArr != null) {
                for (i in 0 until spArr.length()) {
                    val s = spArr.getJSONObject(i)
                    parsedSpans.add(
                        RichTextSpan(
                            start = s.optInt("start", 0),
                            end = s.optInt("end", 0),
                            isBold = s.optBoolean("isBold", false),
                            isItalic = s.optBoolean("isItalic", false),
                            isUnderline = s.optBoolean("isUnderline", false),
                            isStrikethrough = s.optBoolean("isStrikethrough", false),
                            highlightColorArgb = if (s.has("highlightColorArgb")) s.optLong("highlightColorArgb") else null,
                            textColorArgb = if (s.has("textColorArgb")) s.optLong("textColorArgb") else null,
                            fontSizeSp = if (s.has("fontSizeSp")) s.optDouble("fontSizeSp").toFloat() else null,
                            fontStyleId = s.optString("fontStyleId", "").ifBlank { null },
                            headingLevel = s.optInt("headingLevel", 0),
                            isQuote = s.optBoolean("isQuote", false),
                            isCode = s.optBoolean("isCode", false),
                            linkUrl = s.optString("linkUrl", "").ifBlank { null }
                        )
                    )
                }
            }
        }
    } catch (_: Exception) {
    }
    val parsedTags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return Note(
        id = id,
        title = title,
        body = body,
        isChecklist = isChecklist,
        checklist = parsedChecklist,
        color = NoteColor.fromId(colorId),
        isPinned = isPinned,
        isFavorite = isFavorite,
        isArchived = isArchived,
        isTrashed = isTrashed,
        trashedAt = trashedAt,
        reminderAt = reminderAt,
        reminderFired = reminderFired,
        reminderSoundStyle = reminderSoundStyle,
        reminderAlertMode = reminderAlertMode,
        reminderVibrationPattern = reminderVibrationPattern,
        reminderVolume = reminderVolume,
        tags = parsedTags,
        folder = folder,
        richSpans = parsedSpans,
        bodyFontStyle = parsedFontStyle,
        bodyFontSizeSp = parsedFontSizeSp,
        bodyTextColorArgb = parsedTextColor,
        bodyTextAlign = parsedTextAlign,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
