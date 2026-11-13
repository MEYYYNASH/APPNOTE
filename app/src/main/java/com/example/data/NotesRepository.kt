package com.example.data

import android.content.Context
import com.example.model.ActivityLogEntry
import com.example.model.AppSettings
import com.example.model.AttachmentType
import com.example.model.ChecklistItem
import com.example.model.EditorFontStyle
import com.example.model.FontOption
import com.example.model.LanguageOption
import com.example.model.Note
import com.example.model.NoteAttachment
import com.example.model.NoteColor
import com.example.model.NoteFolder
import com.example.model.NoteType
import com.example.model.NoteVersion
import com.example.model.RichTextSpan
import com.example.model.ThemeOption
import com.example.data.local.AppDatabase
import com.example.data.local.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
            val ok = writeRaw(KEY_NOTES, arr.toString())
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = AppDatabase.getInstance(appContext).noteDao()
                    dao.replaceAll(notes.map { it.toEntity() })
                } catch (_: Exception) {
                }
            }
            ok
        } catch (e: Exception) {
            usingFallback = true
            false
        }
    }

    fun loadFolders(): List<NoteFolder> {
        val raw = readRaw(KEY_FOLDERS)
        if (raw.isNullOrBlank()) {
            val seeded = createSeedFolders()
            saveFolders(seeded)
            return seeded
        }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<NoteFolder>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(parseFolder(obj))
            }
            if (list.isEmpty()) {
                val seeded = createSeedFolders()
                saveFolders(seeded)
                seeded
            } else {
                list
            }
        } catch (e: Exception) {
            val seeded = createSeedFolders()
            saveFolders(seeded)
            seeded
        }
    }

    fun saveFolders(folders: List<NoteFolder>): Boolean {
        return try {
            val arr = JSONArray()
            folders.forEach { arr.put(serializeFolder(it)) }
            writeRaw(KEY_FOLDERS, arr.toString())
        } catch (e: Exception) {
            usingFallback = true
            false
        }
    }

    private fun serializeFolder(folder: NoteFolder): JSONObject {
        return JSONObject().apply {
            put("id", folder.id)
            put("name", folder.name)
            put("parentId", folder.parentId ?: "")
            put("iconId", folder.iconId)
            put("color", folder.color.id)
            put("isPinned", folder.isPinned)
            put("isArchived", folder.isArchived)
            put("isTrashed", folder.isTrashed)
            if (folder.trashedAt != null) put("trashedAt", folder.trashedAt)
            put("isSmart", folder.isSmart)
            put("smartQuery", folder.smartQuery)
            put("sortOrder", folder.sortOrder)
            put("createdAt", folder.createdAt)
        }
    }

    private fun parseFolder(obj: JSONObject): NoteFolder {
        val parentRaw = obj.optString("parentId", "").trim()
        return NoteFolder(
            id = obj.optString("id", UUID.randomUUID().toString()),
            name = obj.optString("name", "Folder"),
            parentId = parentRaw.ifEmpty { null },
            iconId = obj.optString("iconId", "folder"),
            color = NoteColor.fromId(obj.optString("color", "violet")),
            isPinned = obj.optBoolean("isPinned", false),
            isArchived = obj.optBoolean("isArchived", false),
            isTrashed = obj.optBoolean("isTrashed", false),
            trashedAt = if (obj.has("trashedAt")) obj.optLong("trashedAt") else null,
            isSmart = obj.optBoolean("isSmart", false),
            smartQuery = obj.optString("smartQuery", ""),
            sortOrder = obj.optInt("sortOrder", 0),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }

    fun loadSettings(): AppSettings {
        val raw = readRaw(KEY_SETTINGS) ?: return AppSettings()
        return try {
            val obj = JSONObject(raw)
            AppSettings(
                theme = ThemeOption.fromId(obj.optString("theme", "system")),
                accent = NoteColor.fromId(obj.optString("accent", "violet")),
                font = FontOption.fromId(obj.optString("font", "figtree")),
                customFontPath = obj.optString("customFontPath", ""),
                customFontName = obj.optString("customFontName", ""),
                fontScale = obj.optDouble("fontScale", 1.0).toFloat().coerceIn(0.85f, 1.30f),
                language = LanguageOption.fromId(obj.optString("language", "en")),
                compactMode = obj.optBoolean("compactMode", false),
                animationsEnabled = obj.optBoolean("animationsEnabled", true),
                defaultNoteType = NoteType.fromId(obj.optString("defaultNoteType", "text")),
                snapshotIntervalSec = obj.optInt("snapshotIntervalSec", 30),
                notificationsEnabled = obj.optBoolean("notificationsEnabled", true),
                hidePreviews = obj.optBoolean("hidePreviews", false),
                webAppMode = false,
                githubOwnerRepo = obj.optString("githubOwnerRepo", "MEYYYNASH/APPNOTE").let {
                    if (it.equals("MEY-NASHH/APPNOTE", ignoreCase = true) || it.isBlank()) "MEYYYNASH/APPNOTE" else it
                },
                githubBranch = obj.optString("githubBranch", "main"),
                githubToken = obj.optString("githubToken", ""),
                appVersion = obj.optString("appVersion", "1.2.01").let {
                    if (it == "1.0" || it == "1.1.17" || it == "1.1.18" || it == "1.1.19" || it == "1.1.20" || it == "1.2.0" || it.isBlank()) "1.2.01" else it
                },
                latestAvailableVersion = obj.optString("latestAvailableVersion", ""),
                includePrereleases = obj.optBoolean("includePrereleases", false),
                reminderSoundEnabled = obj.optBoolean("reminderSoundEnabled", true),
                reminderAlertMode = obj.optString("reminderAlertMode", "sound_vibration"),
                reminderRingtoneStyle = obj.optString("reminderRingtoneStyle", "crystal_chime"),
                useDefaultPhoneRingtone = obj.optBoolean("useDefaultPhoneRingtone", false),
                customSoundUri = obj.optString("customSoundUri", ""),
                customSoundName = obj.optString("customSoundName", ""),
                customSoundSource = obj.optString("customSoundSource", ""),
                customSoundBase64 = obj.optString("customSoundBase64", ""),
                customNotificationSoundStyle = obj.optString("customNotificationSoundStyle", "glass_bell"),
                reminderVolumePercent = obj.optInt("reminderVolumePercent", 85).coerceIn(0, 100),
                separateSoundVolumeControl = obj.optBoolean("separateSoundVolumeControl", true),
                graduallyIncreaseVolume = obj.optBoolean("graduallyIncreaseVolume", false),
                repeatRingtoneCount = obj.optInt("repeatRingtoneCount", 2).coerceIn(1, 10),
                reminderSoundDurationSec = obj.optInt("reminderSoundDurationSec", 5).coerceIn(2, 30),
                customVibrationPattern = obj.optString("customVibrationPattern", "double_beat"),
                differentSoundsPerReminder = obj.optBoolean("differentSoundsPerReminder", true),
                dndEnabled = obj.optBoolean("dndEnabled", false),
                dndStartHour = obj.optInt("dndStartHour", 22).coerceIn(0, 23),
                dndEndHour = obj.optInt("dndEndHour", 7).coerceIn(0, 23),
                dndAllowPinnedBypass = obj.optBoolean("dndAllowPinnedBypass", true)
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
                put("webAppMode", false)
                put("githubOwnerRepo", settings.githubOwnerRepo)
                put("githubBranch", settings.githubBranch)
                put("githubToken", settings.githubToken)
                put("appVersion", settings.appVersion)
                put("latestAvailableVersion", settings.latestAvailableVersion)
                put("includePrereleases", settings.includePrereleases)
                put("reminderSoundEnabled", settings.reminderSoundEnabled)
                put("reminderAlertMode", settings.reminderAlertMode)
                put("reminderRingtoneStyle", settings.reminderRingtoneStyle)
                put("useDefaultPhoneRingtone", settings.useDefaultPhoneRingtone)
                put("customSoundUri", settings.customSoundUri)
                put("customSoundName", settings.customSoundName)
                put("customSoundSource", settings.customSoundSource)
                put("customSoundBase64", settings.customSoundBase64)
                put("customNotificationSoundStyle", settings.customNotificationSoundStyle)
                put("reminderVolumePercent", settings.reminderVolumePercent)
                put("separateSoundVolumeControl", settings.separateSoundVolumeControl)
                put("graduallyIncreaseVolume", settings.graduallyIncreaseVolume)
                put("repeatRingtoneCount", settings.repeatRingtoneCount)
                put("reminderSoundDurationSec", settings.reminderSoundDurationSec)
                put("customVibrationPattern", settings.customVibrationPattern)
                put("differentSoundsPerReminder", settings.differentSoundsPerReminder)
                put("dndEnabled", settings.dndEnabled)
                put("dndStartHour", settings.dndStartHour)
                put("dndEndHour", settings.dndEndHour)
                put("dndAllowPinnedBypass", settings.dndAllowPinnedBypass)
            }
            writeRaw(KEY_SETTINGS, obj.toString())
        } catch (e: Exception) {
            usingFallback = true
            false
        }
    }

    fun loadRecentSearches(): List<String> {
        val raw = readRaw(KEY_RECENT_SEARCHES) ?: return listOf("#roadmap", "Product", "Personal")
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
        val f = readRaw(KEY_FOLDERS)?.toByteArray(Charsets.UTF_8)?.size ?: 0
        val s = readRaw(KEY_SETTINGS)?.toByteArray(Charsets.UTF_8)?.size ?: 0
        val r = readRaw(KEY_RECENT_SEARCHES)?.toByteArray(Charsets.UTF_8)?.size ?: 0
        return n + f + s + r
    }

    fun exportFullBackupJson(notes: List<Note>, settings: AppSettings, recentSearches: List<String>): String {
        val root = JSONObject()
        root.put("version", settings.appVersion)
        root.put("exportedAt", System.currentTimeMillis())
        val notesArr = JSONArray()
        notes.forEach { notesArr.put(serializeNote(it)) }
        root.put("notes", notesArr)

        val foldersArr = JSONArray()
        loadFolders().forEach { foldersArr.put(serializeFolder(it)) }
        root.put("folders", foldersArr)

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
            put("reminderSoundEnabled", settings.reminderSoundEnabled)
            put("reminderAlertMode", settings.reminderAlertMode)
            put("reminderRingtoneStyle", settings.reminderRingtoneStyle)
            put("reminderVolumePercent", settings.reminderVolumePercent)
            put("customVibrationPattern", settings.customVibrationPattern)
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
            val foldersArr = root.optJSONArray("folders")
            if (foldersArr != null) {
                val folders = mutableListOf<NoteFolder>()
                for (i in 0 until foldersArr.length()) {
                    folders.add(parseFolder(foldersArr.getJSONObject(i)))
                }
                if (folders.isNotEmpty()) saveFolders(folders)
            }
            val currentSettings = loadSettings()
            val sObj = root.optJSONObject("settings")
            val settings = if (sObj != null) {
                currentSettings.copy(
                    theme = ThemeOption.fromId(sObj.optString("theme", "system")),
                    accent = NoteColor.fromId(sObj.optString("accent", "violet")),
                    font = FontOption.fromId(sObj.optString("font", "figtree")),
                    fontScale = sObj.optDouble("fontScale", 1.0).toFloat().coerceIn(0.85f, 1.30f),
                    language = LanguageOption.fromId(sObj.optString("language", "en")),
                    compactMode = sObj.optBoolean("compactMode", false),
                    animationsEnabled = sObj.optBoolean("animationsEnabled", true),
                    defaultNoteType = NoteType.fromId(sObj.optString("defaultNoteType", "text")),
                    snapshotIntervalSec = sObj.optInt("snapshotIntervalSec", 30),
                    notificationsEnabled = sObj.optBoolean("notificationsEnabled", true),
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
            put("isFavorite", note.isFavorite)
            put("isArchived", note.isArchived)
            put("isTrashed", note.isTrashed)
            if (note.trashedAt != null) put("trashedAt", note.trashedAt)
            if (note.reminderAt != null) put("reminderAt", note.reminderAt)
            put("reminderFired", note.reminderFired)
            put("reminderSoundStyle", note.reminderSoundStyle)
            put("reminderAlertMode", note.reminderAlertMode)
            put("reminderVibrationPattern", note.reminderVibrationPattern)
            put("reminderVolume", note.reminderVolume)
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

            val richSpansArr = JSONArray()
            note.richSpans.forEach { sp ->
                richSpansArr.put(JSONObject().apply {
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
            put("richSpans", richSpansArr)
            put("bodyFontStyle", note.bodyFontStyle)
            put("bodyFontSizeSp", note.bodyFontSizeSp.toDouble())
            if (note.bodyTextColorArgb != null) put("bodyTextColorArgb", note.bodyTextColorArgb)
            put("bodyTextAlign", note.bodyTextAlign)
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

        val richSpans = mutableListOf<RichTextSpan>()
        val richSpansArr = obj.optJSONArray("richSpans")
        if (richSpansArr != null) {
            for (i in 0 until richSpansArr.length()) {
                val s = richSpansArr.optJSONObject(i) ?: continue
                richSpans.add(
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

        return Note(
            id = obj.optString("id", UUID.randomUUID().toString()),
            title = obj.optString("title", ""),
            body = obj.optString("body", ""),
            isChecklist = obj.optBoolean("isChecklist", false),
            checklist = checklist,
            color = NoteColor.fromId(obj.optString("color", "blue")),
            isPinned = obj.optBoolean("isPinned", false),
            isFavorite = obj.optBoolean("isFavorite", false),
            isArchived = obj.optBoolean("isArchived", false),
            isTrashed = obj.optBoolean("isTrashed", false),
            trashedAt = if (obj.has("trashedAt")) obj.optLong("trashedAt") else null,
            reminderAt = if (obj.has("reminderAt")) obj.optLong("reminderAt") else null,
            reminderFired = obj.optBoolean("reminderFired", false),
            reminderSoundStyle = obj.optString("reminderSoundStyle", ""),
            reminderAlertMode = obj.optString("reminderAlertMode", ""),
            reminderVibrationPattern = obj.optString("reminderVibrationPattern", ""),
            reminderVolume = obj.optInt("reminderVolume", -1),
            tags = tags,
            folder = obj.optString("folder", ""),
            attachments = attachments,
            versions = versions,
            activityLog = logs,
            richSpans = richSpans,
            bodyFontStyle = obj.optString("bodyFontStyle", EditorFontStyle.SANS.id),
            bodyFontSizeSp = obj.optDouble("bodyFontSizeSp", 16.0).toFloat(),
            bodyTextColorArgb = if (obj.has("bodyTextColorArgb")) obj.optLong("bodyTextColorArgb") else null,
            bodyTextAlign = obj.optString("bodyTextAlign", "start"),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    private fun createSeedFolders(): List<NoteFolder> {
        val now = System.currentTimeMillis()
        return listOf(
            NoteFolder(
                id = "folder-personal",
                name = "Personal",
                iconId = "folder",
                color = NoteColor.VIOLET,
                isPinned = true,
                sortOrder = 0,
                createdAt = now - 3600_000L * 48
            ),
            NoteFolder(
                id = "folder-projects",
                name = "Projects",
                iconId = "rocket",
                color = NoteColor.BLUE,
                isPinned = true,
                sortOrder = 1,
                createdAt = now - 3600_000L * 36
            ),
            NoteFolder(
                id = "folder-work",
                name = "Work",
                iconId = "briefcase",
                color = NoteColor.GREEN,
                isPinned = false,
                sortOrder = 2,
                createdAt = now - 3600_000L * 24
            ),
            NoteFolder(
                id = "folder-journal",
                name = "Journal",
                iconId = "heart",
                color = NoteColor.PINK,
                isPinned = false,
                sortOrder = 3,
                createdAt = now - 3600_000L * 12
            ),
            NoteFolder(
                id = "folder-scans",
                name = "Scanned Docs",
                parentId = "folder-work",
                iconId = "scan",
                color = NoteColor.ORANGE,
                isPinned = false,
                sortOrder = 4,
                createdAt = now - 3600_000L * 6
            )
        )
    }

    private fun createSeedNotes(): List<Note> {
        val now = System.currentTimeMillis()
        val n1Created = now - 3600_000L * 1
        val n2Created = now - 3600_000L * 2
        val n3Created = now - 3600_000L * 20
        val n4Created = now - 3600_000L * 28

        return listOf(
            Note(
                id = "seed-bento-1",
                title = "Project Ideas",
                body = "Build a modern note app with AI features, neon glass widgets, document scanner with OCR & PDF export, and rich markdown editing.",
                isChecklist = false,
                color = NoteColor.BLUE,
                isPinned = true,
                isFavorite = true,
                tags = listOf("project", "design", "roadmap"),
                folder = "Projects",
                versions = listOf(
                    NoteVersion(
                        id = "v-bento-1",
                        timestamp = n1Created,
                        title = "Project Ideas",
                        body = "Build a modern note app with AI features...",
                        checklist = emptyList(),
                        isChecklist = false
                    )
                ),
                activityLog = listOf(
                    ActivityLogEntry("l-b1", n1Created, "created")
                ),
                createdAt = n1Created,
                updatedAt = n1Created
            ),
            Note(
                id = "seed-bento-2",
                title = "Shopping List",
                body = "",
                isChecklist = true,
                checklist = listOf(
                    ChecklistItem("cl-milk", "Milk", true),
                    ChecklistItem("cl-bread", "Bread", true),
                    ChecklistItem("cl-coffee", "Coffee", false)
                ),
                color = NoteColor.ORANGE,
                isPinned = true,
                isFavorite = true,
                tags = listOf("shopping", "personal"),
                folder = "Personal",
                versions = listOf(
                    NoteVersion(
                        id = "v-bento-2",
                        timestamp = n2Created,
                        title = "Shopping List",
                        body = "",
                        checklist = listOf(
                            ChecklistItem("cl-milk", "Milk", true),
                            ChecklistItem("cl-bread", "Bread", true),
                            ChecklistItem("cl-coffee", "Coffee", false)
                        ),
                        isChecklist = true
                    )
                ),
                activityLog = listOf(
                    ActivityLogEntry("l-b2", n2Created, "created")
                ),
                createdAt = n2Created,
                updatedAt = n2Created
            ),
            Note(
                id = "seed-bento-3",
                title = "Meeting Notes",
                body = "Discuss the new project and timeline with engineering & design leads. Review Q4 deliverables and release v1.2.01.",
                isChecklist = false,
                color = NoteColor.GREEN,
                isPinned = false,
                isFavorite = false,
                reminderAt = now + 3600_000L * 2,
                reminderSoundStyle = "crystal_chime",
                tags = listOf("meeting", "work"),
                folder = "Work",
                versions = listOf(
                    NoteVersion(
                        id = "v-bento-3",
                        timestamp = n3Created,
                        title = "Meeting Notes",
                        body = "Discuss the new project and timeline...",
                        checklist = emptyList(),
                        isChecklist = false
                    )
                ),
                activityLog = listOf(
                    ActivityLogEntry("l-b3", n3Created, "created")
                ),
                createdAt = n3Created,
                updatedAt = n3Created
            ),
            Note(
                id = "seed-bento-4",
                title = "Daily Journal",
                body = "Today is a great day! Completed the glassmorphic widgets strip, custom reminder ringtones, and folder organizer.",
                isChecklist = false,
                color = NoteColor.PINK,
                isPinned = false,
                isFavorite = true,
                tags = listOf("journal", "gratitude"),
                folder = "Journal",
                versions = listOf(
                    NoteVersion(
                        id = "v-bento-4",
                        timestamp = n4Created,
                        title = "Daily Journal",
                        body = "Today is a great day!",
                        checklist = emptyList(),
                        isChecklist = false
                    )
                ),
                activityLog = listOf(
                    ActivityLogEntry("l-b4", n4Created, "created")
                ),
                createdAt = n4Created,
                updatedAt = n4Created
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "glass_notes_local_storage"
        private const val KEY_NOTES = "glass_notes_data_v1"
        private const val KEY_FOLDERS = "glass_notes_folders_v1"
        private const val KEY_SETTINGS = "glass_notes_settings_v1"
        private const val KEY_RECENT_SEARCHES = "glass_notes_recent_searches_v1"
    }
}
