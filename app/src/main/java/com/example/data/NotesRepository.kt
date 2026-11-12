package com.example.data

import android.content.Context
import com.example.model.ActivityLogEntry
import com.example.model.AppSettings
import com.example.model.AttachmentType
import com.example.model.ChecklistItem
import com.example.model.FontOption
import com.example.model.LanguageOption
import com.example.model.Note
import com.example.model.NoteAttachment
import com.example.model.NoteColor
import com.example.model.NoteType
import com.example.model.NoteVersion
import com.example.model.ThemeOption
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class NotesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val inMemoryFallback = mutableMapOf<String, String>()
    private var usingFallback = false

    private val prefs by lazy {
        try {
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            usingFallback = true
            null
        }
    }

    private fun readRaw(key: String): String? {
        return try {
            val p = prefs
            if (p != null && !usingFallback) {
                p.getString(key, null) ?: inMemoryFallback[key]
            } else {
                inMemoryFallback[key]
            }
        } catch (e: Exception) {
            usingFallback = true
            inMemoryFallback[key]
        }
    }

    private fun writeRaw(key: String, value: String): Boolean {
        inMemoryFallback[key] = value
        return try {
            val p = prefs
            if (p != null && !usingFallback) {
                p.edit().putString(key, value).apply()
            }
            true
        } catch (e: Exception) {
            usingFallback = true
            false
        }
    }

    fun isUsingMemoryFallback(): Boolean = usingFallback

    fun loadNotes(): List<Note> {
        val raw = readRaw(KEY_NOTES)
        if (raw.isNullOrBlank()) {
            val seeded = createSeedNotes()
            saveNotes(seeded)
            return seeded
        }
        return try {
            val arr = JSONArray(raw)
            val now = System.currentTimeMillis()
            val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
            val parsed = mutableListOf<Note>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val note = parseNote(obj)
                // Purge trashed notes older than 30 days
                val trashedAt = note.trashedAt
                if (note.isTrashed && trashedAt != null && (now - trashedAt) > thirtyDaysMs) {
                    continue
                }
                parsed.add(note)
            }
            parsed
        } catch (e: Exception) {
            val seeded = createSeedNotes()
            saveNotes(seeded)
            seeded
        }
    }

    fun saveNotes(notes: List<Note>): Boolean {
        return try {
            val arr = JSONArray()
            notes.forEach { arr.put(serializeNote(it)) }
            writeRaw(KEY_NOTES, arr.toString())
        } catch (e: Exception) {
            usingFallback = true
            false
        }
    }

    fun loadSettings(): AppSettings {
        val raw = readRaw(KEY_SETTINGS) ?: return AppSettings()
        return try {
            val obj = JSONObject(raw)
            AppSettings(
                theme = ThemeOption.fromId(obj.optString("theme", "system")),
                accent = NoteColor.fromId(obj.optString("accent", "blue")),
                font = FontOption.fromId(obj.optString("font", "figtree")),
                customFontPath = obj.optString("customFontPath", ""),
                customFontName = obj.optString("customFontName", ""),
                fontScale = obj.optDouble("fontScale", 1.0).toFloat().coerceIn(0.85f, 1.30f),
                language = LanguageOption.fromId(obj.optString("language", "en")),
                compactMode = obj.optBoolean("compactMode", false),
                animationsEnabled = obj.optBoolean("animationsEnabled", true),
                defaultNoteType = NoteType.fromId(obj.optString("defaultNoteType", "text")),
                snapshotIntervalSec = obj.optInt("snapshotIntervalSec", 30),
                notificationsEnabled = obj.optBoolean("notificationsEnabled", false),
                hidePreviews = obj.optBoolean("hidePreviews", false),
                webAppMode = obj.optBoolean("webAppMode", false),
                githubOwnerRepo = obj.optString("githubOwnerRepo", "MEY-NASHH/APPNOTE"),
                githubBranch = obj.optString("githubBranch", "main"),
                githubToken = obj.optString("githubToken", ""),
                appVersion = obj.optString("appVersion", "1.0"),
                latestAvailableVersion = obj.optString("latestAvailableVersion", ""),
                includePrereleases = obj.optBoolean("includePrereleases", false)
            )
        } catch (e: Exception) {
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings): Boolean {
        return try {
            val obj = JSONObject().apply {
                put("theme", settings.theme.id)
                put("accent", settings.accent.id)
                put("font", settings.font.id)
                put("customFontPath", settings.customFontPath)
                put("customFontName", settings.customFontName)
                put("fontScale", settings.fontScale.toDouble())
                put("language", settings.language.id)
                put("compactMode", settings.compactMode)
                put("animationsEnabled", settings.animationsEnabled)
                put("defaultNoteType", settings.defaultNoteType.id)
                put("snapshotIntervalSec", settings.snapshotIntervalSec)
                put("notificationsEnabled", settings.notificationsEnabled)
                put("hidePreviews", settings.hidePreviews)
                put("webAppMode", settings.webAppMode)
                put("githubOwnerRepo", settings.githubOwnerRepo)
                put("githubBranch", settings.githubBranch)
                put("githubToken", settings.githubToken)
                put("appVersion", settings.appVersion)
                put("latestAvailableVersion", settings.latestAvailableVersion)
                put("includePrereleases", settings.includePrereleases)
            }
            writeRaw(KEY_SETTINGS, obj.toString())
        } catch (e: Exception) {
            usingFallback = true
            false
        }
    }

    fun loadRecentSearches(): List<String> {
        val raw = readRaw(KEY_RECENT_SEARCHES) ?: return listOf("#roadmap", "Product", "Architecture")
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until minOf(6, arr.length())) {
                val item = arr.optString(i).trim()
                if (item.isNotEmpty()) list.add(item)
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRecentSearches(searches: List<String>) {
        try {
            val arr = JSONArray()
            searches.take(6).forEach { arr.put(it) }
            writeRaw(KEY_RECENT_SEARCHES, arr.toString())
        } catch (_: Exception) {
        }
    }

    fun calculateStorageBytes(): Int {
        val n = readRaw(KEY_NOTES)?.toByteArray(Charsets.UTF_8)?.size ?: 0
        val s = readRaw(KEY_SETTINGS)?.toByteArray(Charsets.UTF_8)?.size ?: 0
        val r = readRaw(KEY_RECENT_SEARCHES)?.toByteArray(Charsets.UTF_8)?.size ?: 0
        return n + s + r
    }

    fun exportFullBackupJson(notes: List<Note>, settings: AppSettings, recentSearches: List<String>): String {
        val root = JSONObject()
        root.put("version", settings.appVersion)
        root.put("exportedAt", System.currentTimeMillis())
        val notesArr = JSONArray()
        notes.forEach { notesArr.put(serializeNote(it)) }
        root.put("notes", notesArr)

        val settingsObj = JSONObject().apply {
            put("theme", settings.theme.id)
            put("accent", settings.accent.id)
            put("font", settings.font.id)
            put("customFontName", settings.customFontName)
            put("fontScale", settings.fontScale.toDouble())
            put("language", settings.language.id)
            put("compactMode", settings.compactMode)
            put("animationsEnabled", settings.animationsEnabled)
            put("defaultNoteType", settings.defaultNoteType.id)
            put("snapshotIntervalSec", settings.snapshotIntervalSec)
            put("notificationsEnabled", settings.notificationsEnabled)
            put("hidePreviews", settings.hidePreviews)
            put("appVersion", settings.appVersion)
        }
        root.put("settings", settingsObj)
        val searchesArr = JSONArray()
        recentSearches.take(6).forEach { searchesArr.put(it) }
        root.put("recentSearches", searchesArr)
        return root.toString(2)
    }

    fun importFullBackupJson(json: String): Pair<List<Note>, AppSettings>? {
        return try {
            val root = JSONObject(json)
            val notesArr = root.optJSONArray("notes") ?: return null
            val notes = mutableListOf<Note>()
            for (i in 0 until notesArr.length()) {
                notes.add(parseNote(notesArr.getJSONObject(i)))
            }
            val currentSettings = loadSettings()
            val sObj = root.optJSONObject("settings")
            val settings = if (sObj != null) {
                currentSettings.copy(
                    theme = ThemeOption.fromId(sObj.optString("theme", "system")),
                    accent = NoteColor.fromId(sObj.optString("accent", "blue")),
                    font = FontOption.fromId(sObj.optString("font", "figtree")),
                    fontScale = sObj.optDouble("fontScale", 1.0).toFloat().coerceIn(0.85f, 1.30f),
                    language = LanguageOption.fromId(sObj.optString("language", "en")),
                    compactMode = sObj.optBoolean("compactMode", false),
                    animationsEnabled = sObj.optBoolean("animationsEnabled", true),
                    defaultNoteType = NoteType.fromId(sObj.optString("defaultNoteType", "text")),
                    snapshotIntervalSec = sObj.optInt("snapshotIntervalSec", 30),
                    notificationsEnabled = sObj.optBoolean("notificationsEnabled", false),
                    hidePreviews = sObj.optBoolean("hidePreviews", false),
                    webAppMode = false
                )
            } else {
                currentSettings
            }
            saveNotes(notes)
            saveSettings(settings)
            Pair(notes, settings)
        } catch (e: Exception) {
            null
        }
    }

    private fun serializeNote(note: Note): JSONObject {
        return JSONObject().apply {
            put("id", note.id)
            put("title", note.title)
            put("body", note.body)
            put("isChecklist", note.isChecklist)
            put("color", note.color.id)
            put("isPinned", note.isPinned)
            put("isArchived", note.isArchived)
            put("isTrashed", note.isTrashed)
            if (note.trashedAt != null) put("trashedAt", note.trashedAt)
            if (note.reminderAt != null) put("reminderAt", note.reminderAt)
            put("reminderFired", note.reminderFired)
            put("folder", note.folder)
            put("createdAt", note.createdAt)
            put("updatedAt", note.updatedAt)

            val tagsArr = JSONArray()
            note.tags.forEach { tagsArr.put(it) }
            put("tags", tagsArr)

            val clArr = JSONArray()
            note.checklist.forEach { item ->
                clArr.put(JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("isDone", item.isDone)
                })
            }
            put("checklist", clArr)

            val attArr = JSONArray()
            note.attachments.forEach { att ->
                attArr.put(JSONObject().apply {
                    put("id", att.id)
                    put("type", att.type.id)
                    put("name", att.name)
                    put("dataBase64", att.dataBase64)
                    put("durationSec", att.durationSec)
                    put("createdAt", att.createdAt)
                })
            }
            put("attachments", attArr)

            val verArr = JSONArray()
            note.versions.take(10).forEach { ver ->
                val vClArr = JSONArray()
                ver.checklist.forEach { item ->
                    vClArr.put(JSONObject().apply {
                        put("id", item.id)
                        put("text", item.text)
                        put("isDone", item.isDone)
                    })
                }
                verArr.put(JSONObject().apply {
                    put("id", ver.id)
                    put("timestamp", ver.timestamp)
                    put("title", ver.title)
                    put("body", ver.body)
                    put("isChecklist", ver.isChecklist)
                    put("checklist", vClArr)
                })
            }
            put("versions", verArr)

            val logArr = JSONArray()
            note.activityLog.take(30).forEach { entry ->
                logArr.put(JSONObject().apply {
                    put("id", entry.id)
                    put("timestamp", entry.timestamp)
                    put("action", entry.action)
                })
            }
            put("activityLog", logArr)
        }
    }

    private fun parseNote(obj: JSONObject): Note {
        val tags = mutableListOf<String>()
        val tagsArr = obj.optJSONArray("tags")
        if (tagsArr != null) {
            for (i in 0 until tagsArr.length()) {
                val t = tagsArr.optString(i).trim()
                if (t.isNotEmpty()) tags.add(t)
            }
        }

        val checklist = mutableListOf<ChecklistItem>()
        val clArr = obj.optJSONArray("checklist")
        if (clArr != null) {
            for (i in 0 until clArr.length()) {
                val c = clArr.getJSONObject(i)
                checklist.add(
                    ChecklistItem(
                        id = c.optString("id", UUID.randomUUID().toString()),
                        text = c.optString("text", ""),
                        isDone = c.optBoolean("isDone", false)
                    )
                )
            }
        }

        val attachments = mutableListOf<NoteAttachment>()
        val attArr = obj.optJSONArray("attachments")
        if (attArr != null) {
            for (i in 0 until attArr.length()) {
                val a = attArr.getJSONObject(i)
                attachments.add(
                    NoteAttachment(
                        id = a.optString("id", UUID.randomUUID().toString()),
                        type = AttachmentType.fromId(a.optString("type", "image")),
                        name = a.optString("name", "Attachment"),
                        dataBase64 = a.optString("dataBase64", ""),
                        durationSec = a.optInt("durationSec", 0),
                        createdAt = a.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val versions = mutableListOf<NoteVersion>()
        val verArr = obj.optJSONArray("versions")
        if (verArr != null) {
            for (i in 0 until verArr.length()) {
                val v = verArr.getJSONObject(i)
                val vCl = mutableListOf<ChecklistItem>()
                val vClArr = v.optJSONArray("checklist")
                if (vClArr != null) {
                    for (j in 0 until vClArr.length()) {
                        val vc = vClArr.getJSONObject(j)
                        vCl.add(
                            ChecklistItem(
                                id = vc.optString("id", UUID.randomUUID().toString()),
                                text = vc.optString("text", ""),
                                isDone = vc.optBoolean("isDone", false)
                            )
                        )
                    }
                }
                versions.add(
                    NoteVersion(
                        id = v.optString("id", UUID.randomUUID().toString()),
                        timestamp = v.optLong("timestamp", System.currentTimeMillis()),
                        title = v.optString("title", ""),
                        body = v.optString("body", ""),
                        checklist = vCl,
                        isChecklist = v.optBoolean("isChecklist", false)
                    )
                )
            }
        }

        val logs = mutableListOf<ActivityLogEntry>()
        val logArr = obj.optJSONArray("activityLog")
        if (logArr != null) {
            for (i in 0 until logArr.length()) {
                val l = logArr.getJSONObject(i)
                logs.add(
                    ActivityLogEntry(
                        id = l.optString("id", UUID.randomUUID().toString()),
                        timestamp = l.optLong("timestamp", System.currentTimeMillis()),
                        action = l.optString("action", "edited")
                    )
                )
            }
        }

        return Note(
            id = obj.optString("id", UUID.randomUUID().toString()),
            title = obj.optString("title", ""),
            body = obj.optString("body", ""),
            isChecklist = obj.optBoolean("isChecklist", false),
            checklist = checklist,
            color = NoteColor.fromId(obj.optString("color", "blue")),
            isPinned = obj.optBoolean("isPinned", false),
            isArchived = obj.optBoolean("isArchived", false),
            isTrashed = obj.optBoolean("isTrashed", false),
            trashedAt = if (obj.has("trashedAt")) obj.optLong("trashedAt") else null,
            reminderAt = if (obj.has("reminderAt")) obj.optLong("reminderAt") else null,
            reminderFired = obj.optBoolean("reminderFired", false),
            tags = tags,
            folder = obj.optString("folder", ""),
            attachments = attachments,
            versions = versions,
            activityLog = logs,
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    private fun createSeedNotes(): List<Note> {
        val now = System.currentTimeMillis()
        val n1Created = now - 3600_000L * 5
        val n2Created = now - 3600_000L * 18
        val n3Created = now - 3600_000L * 42

        return listOf(
            Note(
                id = "seed-1",
                title = "Glassmorphism Design System",
                body = "# Frosted Glass Principles\n\nOur interface uses **translucent panels** with `26px backdrop blur`, saturation boost, and a crisp `1px` specular edge.\n\n- Soft drifting color blobs in blue, violet, and cyan\n- Two-color gradient accents across interactive pills\n- Zero emojis and clean 2px rounded line icons only",
                isChecklist = false,
                color = NoteColor.BLUE,
                isPinned = true,
                tags = listOf("design", "architecture", "ui"),
                folder = "Product",
                versions = listOf(
                    NoteVersion(
                        id = "v-seed-1",
                        timestamp = n1Created,
                        title = "Glassmorphism Design System",
                        body = "# Frosted Glass Principles\nInitial specification draft for translucent cards.",
                        checklist = emptyList(),
                        isChecklist = false
                    )
                ),
                activityLog = listOf(
                    ActivityLogEntry("l-1", now - 1800_000L, "edited"),
                    ActivityLogEntry("l-0", n1Created, "created")
                ),
                createdAt = n1Created,
                updatedAt = now - 1800_000L
            ),
            Note(
                id = "seed-2",
                title = "Release Sprint Checklist",
                body = "",
                isChecklist = true,
                checklist = listOf(
                    ChecklistItem("c-1", "Configure Figtree, Serif, and Mono typography", true),
                    ChecklistItem("c-2", "Verify two-pane layout on wide screens and phone slide", true),
                    ChecklistItem("c-3", "Test Voice, Image, Scan, and Drawing canvas attachments", true),
                    ChecklistItem("c-4", "Add English and Khmer language translations", false),
                    ChecklistItem("c-5", "Prepare JSON backup and restore verification", false)
                ),
                color = NoteColor.VIOLET,
                isPinned = true,
                tags = listOf("roadmap", "sprint"),
                folder = "Engineering",
                versions = listOf(
                    NoteVersion(
                        id = "v-seed-2",
                        timestamp = n2Created,
                        title = "Release Sprint Checklist",
                        body = "",
                        checklist = listOf(
                            ChecklistItem("c-1", "Configure Figtree, Serif, and Mono typography", true),
                            ChecklistItem("c-2", "Verify two-pane layout on wide screens and phone slide", false)
                        ),
                        isChecklist = true
                    )
                ),
                activityLog = listOf(
                    ActivityLogEntry("l-3", now - 3600_000L * 2, "edited"),
                    ActivityLogEntry("l-2", n2Created, "created")
                ),
                createdAt = n2Created,
                updatedAt = now - 3600_000L * 2
            ),
            Note(
                id = "seed-3",
                title = "Offline & LocalStorage Architecture",
                body = "## Privacy First Storage\n\nAll notes, attachments, and version snapshots are saved in **local storage** wrapped in `try/catch` with an automatic in-memory fallback.\n\n- Collaboration, shared folders, and public links require a backend server and are intentionally excluded\n- Export full JSON backups anytime from Settings",
                isChecklist = false,
                color = NoteColor.GREEN,
                isPinned = false,
                tags = listOf("privacy", "storage"),
                folder = "Architecture",
                versions = listOf(
                    NoteVersion(
                        id = "v-seed-3",
                        timestamp = n3Created,
                        title = "Offline & LocalStorage Architecture",
                        body = "All notes are stored locally on device.",
                        checklist = emptyList(),
                        isChecklist = false
                    )
                ),
                activityLog = listOf(
                    ActivityLogEntry("l-4", n3Created, "created")
                ),
                createdAt = n3Created,
                updatedAt = n3Created
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "glass_notes_local_storage"
        private const val KEY_NOTES = "glass_notes_data_v1"
        private const val KEY_SETTINGS = "glass_notes_settings_v1"
        private const val KEY_RECENT_SEARCHES = "glass_notes_recent_searches_v1"
    }
}
