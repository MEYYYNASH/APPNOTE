package com.example.model

import androidx.compose.ui.graphics.Color

enum class NoteColor(
    val id: String,
    val labelEn: String,
    val labelKm: String,
    val startColor: Color,
    val endColor: Color,
    val dotColor: Color
) {
    BLUE("blue", "Blue", "ខៀវ", Color(0xFF2F7CFF), Color(0xFF7A5CFF), Color(0xFF3B82F6)),
    GREEN("green", "Green", "បៃតង", Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF10B981)),
    AMBER("amber", "Amber", "លឿងទុំ", Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFF59E0B)),
    VIOLET("violet", "Violet", "ស្វាយ", Color(0xFFA855F7), Color(0xFF6366F1), Color(0xFFA855F7)),
    PINK("pink", "Pink", "ផ្កាឈូក", Color(0xFFEC4899), Color(0xFFF43F5E), Color(0xFFEC4899)),
    ORANGE("orange", "Orange", "ទឹកក្រូច", Color(0xFFF97316), Color(0xFFEA580C), Color(0xFFF97316)),
    SLATE("slate", "Slate", "ប្រផេះ", Color(0xFF64748B), Color(0xFF0F172A), Color(0xFF64748B));

    companion object {
        fun fromId(id: String): NoteColor = entries.find { it.id == id } ?: BLUE
    }
}

enum class ThemeOption(val id: String, val labelEn: String, val labelKm: String) {
    SYSTEM("system", "System", "ប្រព័ន្ធ"),
    LIGHT("light", "Light", "ភ្លឺ"),
    DARK("dark", "Dark", "ងងឹត");

    companion object {
        fun fromId(id: String): ThemeOption = entries.find { it.id == id } ?: SYSTEM
    }
}

enum class FontOption(val id: String, val labelEn: String, val labelKm: String) {
    FIGTREE("figtree", "Default (Figtree)", "លំនាំដើម (Figtree)"),
    SERIF("serif", "Serif", "Serif"),
    MONO("mono", "Mono", "Mono"),
    CUSTOM("custom", "Imported Font", "ពុម្ពអក្សរនាំចូល");

    companion object {
        fun fromId(id: String): FontOption = entries.find { it.id == id } ?: FIGTREE
    }
}

enum class LanguageOption(val id: String, val labelEn: String, val labelKm: String) {
    ENGLISH("en", "English", "English"),
    KHMER("km", "Khmer", "ខ្មែរ");

    companion object {
        fun fromId(id: String): LanguageOption = entries.find { it.id == id } ?: ENGLISH
    }
}

enum class NoteType(val id: String, val labelEn: String, val labelKm: String) {
    TEXT("text", "Text", "អត្ថបទ"),
    CHECKLIST("checklist", "Checklist", "បញ្ជី");

    companion object {
        fun fromId(id: String): NoteType = entries.find { it.id == id } ?: TEXT
    }
}

enum class AttachmentType(val id: String, val labelEn: String, val labelKm: String) {
    VOICE("voice", "Voice", "សំឡេង"),
    IMAGE("image", "Image", "រូបភាព"),
    SCAN("scan", "Scan", "ស្កេន"),
    DRAWING("drawing", "Drawing", "គំនូរ"),
    VIDEO("video", "Video", "វីដេអូ"),
    PDF("pdf", "PDF", "PDF"),
    FILE("file", "File", "ឯកសារ");

    companion object {
        fun fromId(id: String): AttachmentType = entries.find { it.id == id } ?: IMAGE
    }
}

data class NoteAttachment(
    val id: String,
    val type: AttachmentType,
    val name: String,
    val dataBase64: String,
    val durationSec: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChecklistItem(
    val id: String,
    val text: String,
    val isDone: Boolean = false
)

data class NoteVersion(
    val id: String,
    val timestamp: Long,
    val title: String,
    val body: String,
    val checklist: List<ChecklistItem>,
    val isChecklist: Boolean
)

data class ActivityLogEntry(
    val id: String,
    val timestamp: Long,
    val action: String
)

data class NoteFolder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val iconId: String = "folder",
    val iconName: String = "folder",
    val color: NoteColor = NoteColor.VIOLET,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val isSmart: Boolean = false,
    val smartQuery: String = "",
    val sortOrder: Int = 0,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class EditorFontStyle(
    val id: String,
    val labelEn: String,
    val labelKm: String
) {
    SANS("sans", "Modern Sans", "អក្សរទំនើប"),
    SERIF("serif", "Editorial Serif", "អក្សរកាសែត"),
    MONO("mono", "Code Mono", "អក្សរកូដ"),
    HANDWRITTEN("handwritten", "Handwritten", "អក្សរសរសេរដៃ"),
    GEOMETRIC("geometric", "Geometric", "អក្សរធរណីមាត្រ");

    companion object {
        fun fromId(id: String): EditorFontStyle =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: SANS
    }
}

data class RichTextSpan(
    val start: Int,
    val end: Int,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val highlightColorArgb: Long? = null,
    val textColorArgb: Long? = null,
    val fontSizeSp: Float? = null,
    val fontStyleId: String? = null,
    val headingLevel: Int = 0,
    val isQuote: Boolean = false,
    val isCode: Boolean = false,
    val linkUrl: String? = null
) {
    fun hasAnyFormatting(): Boolean {
        return isBold ||
            isItalic ||
            isUnderline ||
            isStrikethrough ||
            highlightColorArgb != null ||
            textColorArgb != null ||
            fontSizeSp != null ||
            !fontStyleId.isNullOrBlank() ||
            headingLevel > 0 ||
            isQuote ||
            isCode ||
            !linkUrl.isNullOrBlank()
    }
}

data class Note(
    val id: String,
    val title: String,
    val body: String,
    val isChecklist: Boolean = false,
    val checklist: List<ChecklistItem> = emptyList(),
    val color: NoteColor = NoteColor.BLUE,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val reminderAt: Long? = null,
    val reminderFired: Boolean = false,
    val reminderSoundStyle: String = "",
    val reminderAlertMode: String = "",
    val reminderVibrationPattern: String = "",
    val reminderVolume: Int = -1,
    val tags: List<String> = emptyList(),
    val folder: String = "",
    val attachments: List<NoteAttachment> = emptyList(),
    val versions: List<NoteVersion> = emptyList(),
    val activityLog: List<ActivityLogEntry> = emptyList(),
    val richSpans: List<RichTextSpan> = emptyList(),
    val bodyFontStyle: String = EditorFontStyle.SANS.id,
    val bodyFontSizeSp: Float = 16f,
    val bodyTextColorArgb: Long? = null,
    val bodyTextAlign: String = "start",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val theme: ThemeOption = ThemeOption.DARK,
    val accent: NoteColor = NoteColor.VIOLET,
    val font: FontOption = FontOption.FIGTREE,
    val customFontPath: String = "",
    val customFontName: String = "",
    val fontScale: Float = 1.0f,
    val language: LanguageOption = LanguageOption.ENGLISH,
    val compactMode: Boolean = false,
    val animationsEnabled: Boolean = true,
    val defaultNoteType: NoteType = NoteType.TEXT,
    val snapshotIntervalSec: Int = 30,
    val notificationsEnabled: Boolean = true,
    val hidePreviews: Boolean = false,
    val webAppMode: Boolean = false,
    val githubOwnerRepo: String = "MEYYYNASH/APPNOTE",
    val githubBranch: String = "main",
    val githubToken: String = "",
    val appVersion: String = "1.1.20",
    val latestAvailableVersion: String = "",
    val includePrereleases: Boolean = false,
    // Suite 4: Custom Reminder & Sound Settings
    val reminderSoundEnabled: Boolean = true,
    val useDefaultPhoneRingtone: Boolean = false,
    val reminderAlertMode: String = "sound_vibration",
    val reminderRingtoneStyle: String = "glass_chime",
    val customSoundUri: String = "",
    val customSoundPath: String = "",
    val customSoundName: String = "",
    val customSoundSource: String = "",
    val customSoundBase64: String = "",
    val customNotificationSoundStyle: String = "glass_chime",
    val customReminderVolumeEnabled: Boolean = true,
    val reminderVolumePercent: Int = 85,
    val reminderVolumeLevel: Int = 85,
    val separateSoundVolumeControl: Boolean = true,
    val notificationVolumeLevel: Int = 80,
    val graduallyIncreaseVolume: Boolean = false,
    val repeatRingtone: Boolean = false,
    val repeatRingtoneCount: Int = 1,
    val reminderSoundDurationSec: Int = 5,
    val customVibrationPattern: String = "default",
    val vibrationPattern: String = "default",
    val differentSoundsPerReminder: Boolean = true,
    val customNotificationSound: String = "glass_chime",
    val dndEnabled: Boolean = false,
    val dndStartHour: Int = 22,
    val dndEndHour: Int = 7,
    val dndAllowPinnedBypass: Boolean = true,
    val dndAllowPinnedReminders: Boolean = true
)

enum class FilterTab(val id: String, val labelEn: String, val labelKm: String) {
    ALL("all", "All", "ទាំងអស់"),
    PINNED("pinned", "Pinned", "បានខ្ទាស់"),
    FAVORITES("favorites", "Favorites", "ចូលចិត្ត"),
    FOLDERS("folders", "Folders", "ថតឯកសារ"),
    CHECKLISTS("checklists", "Checklists", "បញ្ជី"),
    ARCHIVE("archive", "Archive", "ប័ណ្ណសារ"),
    TRASH("trash", "Trash", "ធុងសំរាម")
}

enum class TypeFilter(val id: String, val labelEn: String, val labelKm: String) {
    ANY("any", "Type: any", "ប្រភេទ៖ ទាំងអស់"),
    TEXT("text", "Text", "អត្ថបទ"),
    CHECKLIST("checklist", "Checklist", "បញ្ជី"),
    VOICE("voice", "Voice", "សំឡេង"),
    IMAGE("image", "Image", "រូបភាព"),
    DRAWING("drawing", "Drawing", "គំនូរ"),
    SCAN("scan", "Scan", "ស្កេន")
}

enum class DateRangeFilter(val id: String, val labelEn: String, val labelKm: String) {
    ANY("any", "Date: any", "កាលបរិច្ឆេទ៖ ទាំងអស់"),
    LAST_DAY("1d", "Last day", "ថ្ងៃមុន"),
    LAST_7_DAYS("7d", "Last 7 days", "៧ ថ្ងៃមុន"),
    LAST_30_DAYS("30d", "Last 30 days", "៣០ ថ្ងៃមុន")
}

enum class DateModeFilter(val id: String, val labelEn: String, val labelKm: String) {
    MODIFIED("modified", "Modified", "កែប្រែ"),
    CREATED("created", "Created", "បង្កើត")
}

data class SearchFilters(
    val type: TypeFilter = TypeFilter.ANY,
    val folder: String = "",
    val dateRange: DateRangeFilter = DateRangeFilter.ANY,
    val dateMode: DateModeFilter = DateModeFilter.MODIFIED
)

data class AppStrings(
    val appTitle: String,
    val searchPlaceholder: String,
    val recentTitle: String,
    val tabAll: String,
    val tabPinned: String,
    val tabChecklists: String,
    val tabArchive: String,
    val tabTrash: String,
    val tabFilters: String,
    val quickCapture: String,
    val quickCaptureHint: String,
    val newNote: String,
    val untitledNote: String,
    val checklistTag: String,
    val wordsLabel: String,
    val editedLabel: String,
    val doneOfLabel: (Int, Int) -> String,
    val folderPlaceholder: String,
    val tagsPlaceholder: String,
    val bodyPlaceholder: String,
    val addChecklistItem: String,
    val voiceBtn: String,
    val imageBtn: String,
    val scanBtn: String,
    val drawBtn: String,
    val markdownPreview: String,
    val editMode: String,
    val historyTitle: String,
    val versionsLabel: String,
    val activityLogLabel: String,
    val restoreBtn: String,
    val shareBtn: String,
    val copyBtn: String,
    val deleteBtn: String,
    val pinBtn: String,
    val unpinBtn: String,
    val duplicateBtn: String,
    val archiveBtn: String,
    val unarchiveBtn: String,
    val savedPill: String,
    val settingsTitle: String,
    val themeLabel: String,
    val accentLabel: String,
    val fontLabel: String,
    val importFontBtn: String,
    val fontSizeLabel: String,
    val languageLabel: String,
    val compactModeLabel: String,
    val animationsLabel: String,
    val defaultTypeLabel: String,
    val snapshotIntervalLabel: String,
    val notificationsLabel: String,
    val hidePreviewsLabel: String,
    val storageUsageLabel: String,
    val emptyTrashBtn: String,
    val copyBackupBtn: String,
    val restoreBackupBtn: String,
    val webModeBtn: String,
    val aboutTitle: String,
    val backendNotice: String,
    val trashBanner: String,
    val emptyNotesHint: String,
    val undoLabel: String,
    val clearBtn: String,
    val attachBtn: String,
    val cancelBtn: String,
    val reminderTitle: String,
    val clearReminderBtn: String,
    val cropPhotoTitle: String,
    val cropSaveBtn: String,
    val rotateBtn: String,
    val githubSyncTitle: String,
    val githubPushApkBtn: String,
    val testVoiceBtn: String
)

object Translations {
    val english = AppStrings(
        appTitle = "Notes",
        searchPlaceholder = "Search notes, folders, #tags...",
        recentTitle = "Recent",
        tabAll = "All",
        tabPinned = "Pinned",
        tabChecklists = "Checklists",
        tabArchive = "Archive",
        tabTrash = "Trash",
        tabFilters = "Filters",
        quickCapture = "Quick Note",
        quickCaptureHint = "Tap to write...",
        newNote = "New Note",
        untitledNote = "Untitled",
        checklistTag = "Checklist",
        wordsLabel = "words",
        editedLabel = "Edited",
        doneOfLabel = { done, total -> "$done of $total done" },
        folderPlaceholder = "Folder",
        tagsPlaceholder = "Tags (comma separated)",
        bodyPlaceholder = "Start writing...",
        addChecklistItem = "Add item",
        voiceBtn = "Voice",
        imageBtn = "Image",
        scanBtn = "Scan",
        drawBtn = "Draw",
        markdownPreview = "Markdown",
        editMode = "Edit",
        historyTitle = "History",
        versionsLabel = "Version history",
        activityLogLabel = "Activity",
        restoreBtn = "Restore",
        shareBtn = "Share",
        copyBtn = "Copy",
        deleteBtn = "Delete",
        pinBtn = "Pin",
        unpinBtn = "Unpin",
        duplicateBtn = "Duplicate",
        archiveBtn = "Archive",
        unarchiveBtn = "Restore from archive",
        savedPill = "Saved",
        settingsTitle = "Settings",
        themeLabel = "Theme",
        accentLabel = "Accent",
        fontLabel = "Font",
        importFontBtn = "Import font from Drive (.ttf/.otf)",
        fontSizeLabel = "Font size",
        languageLabel = "Language",
        compactModeLabel = "Compact mode",
        animationsLabel = "Animations",
        defaultTypeLabel = "Default note type",
        snapshotIntervalLabel = "Auto-save snapshot (sec)",
        notificationsLabel = "Notifications",
        hidePreviewsLabel = "Hide previews (privacy)",
        storageUsageLabel = "Storage",
        emptyTrashBtn = "Empty trash",
        copyBackupBtn = "Copy backup",
        restoreBackupBtn = "Restore backup",
        webModeBtn = "Web view",
        aboutTitle = "GlassNotes v1.1.20",
        backendNotice = "GlassNotes v1.1.20 (code 10120). All notes, folders, scans, attachments, and custom reminder sounds stay locally on this device.",
        trashBanner = "Deleted · tap any note to restore",
        emptyNotesHint = "No notes yet. Tap + New Note to write your first one.",
        undoLabel = "Undo",
        clearBtn = "Clear",
        attachBtn = "Attach",
        cancelBtn = "Cancel",
        reminderTitle = "Reminder",
        clearReminderBtn = "Clear reminder",
        cropPhotoTitle = "Crop & Scan",
        cropSaveBtn = "Crop & Save",
        rotateBtn = "Rotate 90°",
        githubSyncTitle = "App Update & Version Notification",
        githubPushApkBtn = "Notify & Publish New Release",
        testVoiceBtn = "Test sound"
    )

    val khmer = AppStrings(
        appTitle = "កំណត់ត្រា",
        searchPlaceholder = "ស្វែងរកកំណត់ត្រា...",
        recentTitle = "ថ្មីៗ",
        tabAll = "ទាំងអស់",
        tabPinned = "បានខ្ទាស់",
        tabChecklists = "បញ្ជី",
        tabArchive = "ប័ណ្ណសារ",
        tabTrash = "ធុងសំរាម",
        tabFilters = "តម្រង",
        quickCapture = "កត់ត្រារហ័ស",
        quickCaptureHint = "សរសេរគំនិតរបស់អ្នក...",
        newNote = "កំណត់ត្រាថ្មី",
        untitledNote = "គ្មានចំណងជើង",
        checklistTag = "បញ្ជី",
        wordsLabel = "ពាក្យ",
        editedLabel = "បានកែប្រែ",
        doneOfLabel = { done, total -> "$done នៃ $total រួចរាល់" },
        folderPlaceholder = "ថតឯកសារ",
        tagsPlaceholder = "ស្លាក (ខណ្ឌដោយក្បៀស)",
        bodyPlaceholder = "ចាប់ផ្តើមសរសេរ",
        addChecklistItem = "បន្ថែមធាតុ",
        voiceBtn = "សំឡេង",
        imageBtn = "រូបភាព",
        scanBtn = "ស្កេន",
        drawBtn = "គំនូរ",
        markdownPreview = "Markdown",
        editMode = "កែសម្រួល",
        historyTitle = "ប្រវត្តិ",
        versionsLabel = "ប្រវត្តិកំណែ",
        activityLogLabel = "សកម្មភាព",
        restoreBtn = "ស្តារ",
        shareBtn = "ចែករំលែក",
        copyBtn = "ចម្លង",
        deleteBtn = "លុប",
        pinBtn = "ខ្ទាស់",
        unpinBtn = "ដោះខ្ទាស់",
        duplicateBtn = "ចម្លងទ្វេ",
        archiveBtn = "ទុកក្នុងប័ណ្ណសារ",
        unarchiveBtn = "ស្តារពីប័ណ្ណសារ",
        savedPill = "បានរក្សាទុក",
        settingsTitle = "ការកំណត់",
        themeLabel = "រូបរាង",
        accentLabel = "ពណ៌លម្អ",
        fontLabel = "ពុម្ពអក្សរ",
        importFontBtn = "នាំចូលពុម្ពអក្សរពី Drive (.ttf/.otf)",
        fontSizeLabel = "ទំហំអក្សរ",
        languageLabel = "ភាសា",
        compactModeLabel = "ទម្រង់បង្រួម",
        animationsLabel = "ចលនា",
        defaultTypeLabel = "ប្រភេទកំណត់ត្រាដើម",
        snapshotIntervalLabel = "រក្សាទុកកំណែស្វ័យប្រវត្តិ (វិនាទី)",
        notificationsLabel = "ការជូនដំណឹង",
        hidePreviewsLabel = "លាក់ខ្លឹមសារមើលជាមុន",
        storageUsageLabel = "ទំហំផ្ទុក",
        emptyTrashBtn = "សម្អាតធុងសំរាម",
        copyBackupBtn = "ចម្លងទិន្នន័យបម្រុង",
        restoreBackupBtn = "ស្តារទិន្នន័យបម្រុង",
        webModeBtn = "មើលទម្រង់វេប",
        aboutTitle = "GlassNotes កំណែ 1.1.20",
        backendNotice = "GlassNotes v1.1.20 (code 10120)។ ទិន្នន័យទាំងអស់រក្សាទុកក្នុងឧបករណ៍នេះ។",
        trashBanner = "បានលុប · ចុចដើម្បីស្តារឡើងវិញ",
        emptyNotesHint = "មិនទាន់មានកំណត់ត្រាទេ។ ចុច + ដើម្បីសរសេរ។",
        undoLabel = "មិនធ្វើវិញ",
        clearBtn = "សម្អាត",
        attachBtn = "ភ្ជាប់",
        cancelBtn = "បោះបង់",
        reminderTitle = "ការរំលឹក",
        clearReminderBtn = "លុបការរំលឹក",
        cropPhotoTitle = "កាត់រូបភាព",
        cropSaveBtn = "កាត់ និងរក្សាទុក",
        rotateBtn = "បង្វិល 90°",
        githubSyncTitle = "អាប់ដេតកម្មវិធី និងការជូនដំណឹងកំណែថ្មី",
        githubPushApkBtn = "បញ្ចេញកំណែថ្មី និងជូនដំណឹង",
        testVoiceBtn = "សាកល្បងសំឡេង"
    )

    fun forLanguage(language: LanguageOption): AppStrings =
        if (language == LanguageOption.KHMER) khmer else english

    val weekdayQuotesEn = listOf(
        "Dream big, write more.",
        "Small steps still move you forward.",
        "Write it down, let your mind rest.",
        "Ideas are fragile. Capture them.",
        "Clear notes, clear mind.",
        "Done is better than perfect.",
        "Start where you are."
    )

    val weekdayQuotesKm = listOf(
        "សុបិនឱ្យធំ សរសេរឱ្យច្រើន។",
        "ជំហានតូចៗក៏ជួយឱ្យអ្នកទៅមុខដែរ។",
        "សរសេរវាទុក ដើម្បីឱ្យចិត្តរបស់អ្នកស្ងប់។",
        "គំនិតងាយនឹងបាត់បង់។ ចូរកត់ត្រាវាទុក។",
        "កំណត់ត្រាច្បាស់លាស់ គំនិតភ្លឺស្វាង។",
        "ធ្វើឱ្យរួចរាល់ ប្រសើរជាងរង់ចាំភាពឥតខ្ចោះ។",
        "ចាប់ផ្តើមពីទីកន្លែងដែលអ្នកកំពុងឈរ។"
    )
}
