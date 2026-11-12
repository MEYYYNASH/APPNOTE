package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.NotesRepository
import com.example.model.ActivityLogEntry
import com.example.model.AppSettings
import com.example.model.AttachmentType
import com.example.model.ChecklistItem
import com.example.model.DateModeFilter
import com.example.model.DateRangeFilter
import com.example.model.FilterTab
import com.example.model.FontOption
import com.example.model.LanguageOption
import com.example.model.Note
import com.example.model.NoteAttachment
import com.example.model.NoteColor
import com.example.model.NoteType
import com.example.model.NoteVersion
import com.example.model.SearchFilters
import com.example.model.ThemeOption
import com.example.model.TypeFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ToastMessage(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val undoNoteId: String? = null
)

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val selectedNoteId: String? = null,
    val isEditorOpenOnMobile: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val searchQuery: String = "",
    val recentSearches: List<String> = emptyList(),
    val activeTab: FilterTab = FilterTab.ALL,
    val showFilterRow: Boolean = false,
    val searchFilters: SearchFilters = SearchFilters(),
    val settings: AppSettings = AppSettings(),
    val showSavedPill: Boolean = false,
    val savedPillTriggerCount: Int = 0,
    val activeToast: ToastMessage? = null,
    val pulseNoteId: String? = null,
    val storageBytes: Int = 0,
    val usingMemoryFallback: Boolean = false,
    val isMarkdownPreview: Boolean = false
)

class NotesViewModel(private val repository: NotesRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private var savedPillJob: Job? = null
    private var toastJob: Job? = null
    private var pulseJob: Job? = null
    private val lastSnapshotTimeByNote = mutableMapOf<String, Long>()
    private var lastDuplicateWarnNoteId: String? = null

    init {
        val loadedNotes = repository.loadNotes()
        val loadedSettings = repository.loadSettings()
        val loadedSearches = repository.loadRecentSearches()
        _uiState.value = NotesUiState(
            notes = loadedNotes,
            selectedNoteId = loadedNotes.firstOrNull { !it.isTrashed && !it.isArchived }?.id,
            recentSearches = loadedSearches,
            settings = loadedSettings,
            storageBytes = repository.calculateStorageBytes(),
            usingMemoryFallback = repository.isUsingMemoryFallback()
        )
    }

    private fun persistNotes(updatedNotes: List<Note>, showSaved: Boolean = true) {
        val ok = repository.saveNotes(updatedNotes)
        _uiState.update {
            it.copy(
                notes = updatedNotes,
                storageBytes = repository.calculateStorageBytes(),
                usingMemoryFallback = repository.isUsingMemoryFallback() || !ok
            )
        }
        if (showSaved) {
            triggerSavedPill()
        }
    }

    private fun triggerSavedPill() {
        savedPillJob?.cancel()
        _uiState.update {
            it.copy(
                showSavedPill = true,
                savedPillTriggerCount = it.savedPillTriggerCount + 1
            )
        }
        savedPillJob = viewModelScope.launch {
            delay(1150L)
            _uiState.update { it.copy(showSavedPill = false) }
        }
    }

    fun showToast(message: String, undoNoteId: String? = null) {
        toastJob?.cancel()
        val toast = ToastMessage(message = message, undoNoteId = undoNoteId)
        _uiState.update { it.copy(activeToast = toast) }
        toastJob = viewModelScope.launch {
            delay(4500L)
            _uiState.update { state ->
                if (state.activeToast?.id == toast.id) state.copy(activeToast = null) else state
            }
        }
    }

    fun dismissToast() {
        toastJob?.cancel()
        _uiState.update { it.copy(activeToast = null) }
    }

    private fun triggerEntrancePulse(noteId: String) {
        pulseJob?.cancel()
        _uiState.update { it.copy(pulseNoteId = noteId) }
        pulseJob = viewModelScope.launch {
            delay(1200L)
            _uiState.update { state ->
                if (state.pulseNoteId == noteId) state.copy(pulseNoteId = null) else state
            }
        }
    }

    fun selectNote(noteId: String) {
        val note = _uiState.value.notes.find { it.id == noteId } ?: return
        if (note.isTrashed) {
            // Tapping a trashed note restores it immediately
            restoreFromTrash(noteId)
            return
        }
        _uiState.update {
            it.copy(
                selectedNoteId = noteId,
                isEditorOpenOnMobile = true,
                isMarkdownPreview = false
            )
        }
    }

    fun closeMobileEditor() {
        _uiState.update { it.copy(isEditorOpenOnMobile = false) }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun toggleMarkdownPreview() {
        _uiState.update { it.copy(isMarkdownPreview = !it.isMarkdownPreview) }
    }

    fun createNewNote() {
        val now = System.currentTimeMillis()
        val defaultIsChecklist = _uiState.value.settings.defaultNoteType == NoteType.CHECKLIST
        val accentColor = _uiState.value.settings.accent
        val initialChecklist = if (defaultIsChecklist) {
            listOf(ChecklistItem(UUID.randomUUID().toString(), "", false))
        } else {
            emptyList()
        }
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = "",
            body = "",
            isChecklist = defaultIsChecklist,
            checklist = initialChecklist,
            color = accentColor,
            isPinned = false,
            versions = listOf(
                NoteVersion(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    title = "",
                    body = "",
                    checklist = initialChecklist,
                    isChecklist = defaultIsChecklist
                )
            ),
            activityLog = listOf(
                ActivityLogEntry(UUID.randomUUID().toString(), now, "created")
            ),
            createdAt = now,
            updatedAt = now
        )
        val updated = listOf(newNote) + _uiState.value.notes
        lastSnapshotTimeByNote[newNote.id] = now
        persistNotes(updated)
        triggerEntrancePulse(newNote.id)
        _uiState.update {
            it.copy(
                selectedNoteId = newNote.id,
                isEditorOpenOnMobile = true,
                activeTab = if (it.activeTab == FilterTab.TRASH || it.activeTab == FilterTab.ARCHIVE) FilterTab.ALL else it.activeTab,
                isMarkdownPreview = false
            )
        }
    }

    fun createQuickCaptureNote(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return
        val lines = trimmed.lines()
        val firstLine = lines.firstOrNull()?.trim().orEmpty()
        val restBody = if (lines.size > 1) lines.drop(1).joinToString("\n").trim() else ""
        val now = System.currentTimeMillis()
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = firstLine,
            body = restBody,
            isChecklist = false,
            color = _uiState.value.settings.accent,
            versions = listOf(
                NoteVersion(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    title = firstLine,
                    body = restBody,
                    checklist = emptyList(),
                    isChecklist = false
                )
            ),
            activityLog = listOf(
                ActivityLogEntry(UUID.randomUUID().toString(), now, "created")
            ),
            createdAt = now,
            updatedAt = now
        )
        val updated = listOf(newNote) + _uiState.value.notes
        lastSnapshotTimeByNote[newNote.id] = now
        persistNotes(updated)
        triggerEntrancePulse(newNote.id)
        checkDuplicateWarning(newNote, updated)
        _uiState.update {
            it.copy(
                selectedNoteId = newNote.id,
                activeTab = FilterTab.ALL
            )
        }
    }

    fun duplicateNote(noteId: String) {
        val source = _uiState.value.notes.find { it.id == noteId } ?: return
        val now = System.currentTimeMillis()
        val copyTitle = if (source.title.isBlank()) "Untitled (Copy)" else "${source.title} (Copy)"
        val duplicated = source.copy(
            id = UUID.randomUUID().toString(),
            title = copyTitle,
            isTrashed = false,
            trashedAt = null,
            createdAt = now,
            updatedAt = now,
            versions = listOf(
                NoteVersion(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    title = copyTitle,
                    body = source.body,
                    checklist = source.checklist,
                    isChecklist = source.isChecklist
                )
            ),
            activityLog = listOf(
                ActivityLogEntry(UUID.randomUUID().toString(), now, "created")
            )
        )
        val updated = listOf(duplicated) + _uiState.value.notes
        persistNotes(updated)
        triggerEntrancePulse(duplicated.id)
        showToast("Duplicated \"${source.title.ifBlank { "Untitled" }}\"")
    }

    fun updateNoteContent(
        noteId: String,
        title: String? = null,
        body: String? = null,
        isChecklist: Boolean? = null,
        checklist: List<ChecklistItem>? = null,
        tagsString: String? = null,
        folder: String? = null,
        color: NoteColor? = null,
        isPinned: Boolean? = null
    ) {
        val now = System.currentTimeMillis()
        val intervalMs = _uiState.value.settings.snapshotIntervalSec * 1000L
        val lastSnap = lastSnapshotTimeByNote[noteId] ?: 0L
        val shouldSnapshot = (now - lastSnap) >= intervalMs

        var modifiedNote: Note? = null
        val updatedList = _uiState.value.notes.map { note ->
            if (note.id != noteId) {
                note
            } else {
                val newTitle = title ?: note.title
                val newBody = body ?: note.body
                val newIsChecklist = isChecklist ?: note.isChecklist
                val newChecklist = checklist ?: if (isChecklist == true && note.checklist.isEmpty()) {
                    val lines = note.body.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    if (lines.isNotEmpty()) {
                        lines.map { ChecklistItem(UUID.randomUUID().toString(), it, false) }
                    } else {
                        listOf(ChecklistItem(UUID.randomUUID().toString(), "", false))
                    }
                } else {
                    note.checklist
                }
                val newTags = if (tagsString != null) {
                    tagsString.split(",")
                        .map { it.trim().removePrefix("#").trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                } else {
                    note.tags
                }
                val newFolder = folder ?: note.folder
                val newColor = color ?: note.color
                val newPinned = isPinned ?: note.isPinned

                val newVersions = if (shouldSnapshot && (newTitle != note.title || newBody != note.body || newChecklist != note.checklist)) {
                    lastSnapshotTimeByNote[noteId] = now
                    val snap = NoteVersion(
                        id = UUID.randomUUID().toString(),
                        timestamp = now,
                        title = newTitle,
                        body = newBody,
                        checklist = newChecklist,
                        isChecklist = newIsChecklist
                    )
                    (listOf(snap) + note.versions).take(10)
                } else {
                    note.versions
                }

                val newLogs = if (shouldSnapshot || note.activityLog.firstOrNull()?.action != "edited") {
                    (listOf(ActivityLogEntry(UUID.randomUUID().toString(), now, "edited")) + note.activityLog).take(30)
                } else {
                    note.activityLog
                }

                note.copy(
                    title = newTitle,
                    body = newBody,
                    isChecklist = newIsChecklist,
                    checklist = newChecklist,
                    tags = newTags,
                    folder = newFolder,
                    color = newColor,
                    isPinned = newPinned,
                    versions = newVersions,
                    activityLog = newLogs,
                    updatedAt = now
                ).also { modifiedNote = it }
            }
        }
        persistNotes(updatedList)
        modifiedNote?.let { checkDuplicateWarning(it, updatedList) }
    }

    private fun checkDuplicateWarning(edited: Note, allNotes: List<Note>) {
        val cleanTitle = edited.title.trim()
        val cleanBody = edited.body.trim()
        val match = allNotes.firstOrNull { other ->
            other.id != edited.id && !other.isTrashed && (
                (cleanTitle.length >= 4 && other.title.trim().equals(cleanTitle, ignoreCase = true)) ||
                (cleanBody.length >= 25 && other.body.trim().equals(cleanBody, ignoreCase = true))
            )
        }
        if (match != null && lastDuplicateWarnNoteId != "${edited.id}_${match.id}_${cleanTitle.hashCode()}") {
            lastDuplicateWarnNoteId = "${edited.id}_${match.id}_${cleanTitle.hashCode()}"
            val dupName = match.title.ifBlank { "Untitled note" }
            showToast("Possible duplicate of \"$dupName\"")
        }
    }

    fun togglePin(noteId: String) {
        val target = _uiState.value.notes.find { it.id == noteId } ?: return
        updateNoteContent(noteId = noteId, isPinned = !target.isPinned)
    }

    fun toggleArchive(noteId: String) {
        val now = System.currentTimeMillis()
        val updated = _uiState.value.notes.map { note ->
            if (note.id == noteId) {
                val nextArchived = !note.isArchived
                note.copy(
                    isArchived = nextArchived,
                    isPinned = if (nextArchived) false else note.isPinned,
                    updatedAt = now
                )
            } else note
        }
        persistNotes(updated)
    }

    fun moveToTrash(noteId: String) {
        val target = _uiState.value.notes.find { it.id == noteId } ?: return
        val now = System.currentTimeMillis()
        val updated = _uiState.value.notes.map { note ->
            if (note.id == noteId) {
                note.copy(isTrashed = true, isPinned = false, trashedAt = now, updatedAt = now)
            } else note
        }
        val nextSelected = if (_uiState.value.selectedNoteId == noteId) {
            updated.firstOrNull { !it.isTrashed && !it.isArchived }?.id
        } else {
            _uiState.value.selectedNoteId
        }
        persistNotes(updated)
        _uiState.update {
            it.copy(
                selectedNoteId = nextSelected,
                isEditorOpenOnMobile = false
            )
        }
        val titleLabel = target.title.ifBlank { "Untitled note" }
        showToast("Moved \"$titleLabel\" to Trash", undoNoteId = noteId)
    }

    fun restoreFromTrash(noteId: String) {
        val now = System.currentTimeMillis()
        var restoredTitle = "Note"
        val updated = _uiState.value.notes.map { note ->
            if (note.id == noteId) {
                restoredTitle = note.title.ifBlank { "Untitled note" }
                note.copy(
                    isTrashed = false,
                    trashedAt = null,
                    activityLog = (listOf(ActivityLogEntry(UUID.randomUUID().toString(), now, "restored")) + note.activityLog).take(30),
                    updatedAt = now
                )
            } else note
        }
        persistNotes(updated)
        _uiState.update { it.copy(selectedNoteId = noteId, activeToast = null) }
        showToast("Restored \"$restoredTitle\"")
    }

    fun emptyTrash() {
        val updated = _uiState.value.notes.filterNot { it.isTrashed }
        persistNotes(updated)
        showToast("Trash emptied")
    }

    fun addAttachment(
        noteId: String,
        type: AttachmentType,
        name: String,
        dataBase64: String,
        durationSec: Int = 0
    ) {
        val now = System.currentTimeMillis()
        val attachment = NoteAttachment(
            id = UUID.randomUUID().toString(),
            type = type,
            name = name,
            dataBase64 = dataBase64,
            durationSec = durationSec,
            createdAt = now
        )
        val updated = _uiState.value.notes.map { note ->
            if (note.id == noteId) {
                note.copy(
                    attachments = note.attachments + attachment,
                    activityLog = (listOf(ActivityLogEntry(UUID.randomUUID().toString(), now, "added attachment")) + note.activityLog).take(30),
                    updatedAt = now
                )
            } else note
        }
        persistNotes(updated)
    }

    fun updateAttachmentData(noteId: String, attachmentId: String, newBase64: String) {
        val now = System.currentTimeMillis()
        val updated = _uiState.value.notes.map { note ->
            if (note.id == noteId) {
                note.copy(
                    attachments = note.attachments.map { att ->
                        if (att.id == attachmentId) att.copy(dataBase64 = newBase64) else att
                    },
                    activityLog = (listOf(ActivityLogEntry(UUID.randomUUID().toString(), now, "cropped photo")) + note.activityLog).take(30),
                    updatedAt = now
                )
            } else note
        }
        persistNotes(updated)
        showToast("Photo cropped & saved")
    }

    fun setNoteReminder(context: Context, noteId: String, reminderAtMillis: Long?) {
        val now = System.currentTimeMillis()
        var targetNote: Note? = null
        val updated = _uiState.value.notes.map { note ->
            if (note.id == noteId) {
                val n = note.copy(
                    reminderAt = reminderAtMillis,
                    reminderFired = false,
                    activityLog = (listOf(
                        ActivityLogEntry(
                            UUID.randomUUID().toString(),
                            now,
                            if (reminderAtMillis != null) "set reminder" else "cleared reminder"
                        )
                    ) + note.activityLog).take(30),
                    updatedAt = now
                )
                targetNote = n
                n
            } else note
        }
        persistNotes(updated)

        val note = targetNote ?: return
        if (reminderAtMillis != null) {
            com.example.util.ReminderScheduler.scheduleNoteReminder(
                context = context,
                noteId = note.id,
                title = note.title.ifBlank { "Untitled Note" },
                body = note.body,
                triggerAtMillis = reminderAtMillis
            )
            val delayMs = (reminderAtMillis - System.currentTimeMillis()).coerceAtLeast(1000L)
            if (delayMs <= 120_000L) {
                viewModelScope.launch {
                    delay(delayMs)
                    val current = _uiState.value.notes.find { it.id == noteId }
                    if (current != null && current.reminderAt == reminderAtMillis && !current.reminderFired) {
                        com.example.util.ReminderScheduler.showReminderNotification(
                            context = context,
                            noteId = current.id,
                            title = current.title.ifBlank { "Untitled Note" },
                            body = current.body
                        )
                        val firedList = _uiState.value.notes.map {
                            if (it.id == noteId) it.copy(reminderFired = true) else it
                        }
                        persistNotes(firedList, showSaved = false)
                        showToast("Reminder: ${current.title.ifBlank { "Untitled Note" }}")
                    }
                }
            }
            showToast("Reminder set for ${com.example.util.ReminderScheduler.formatReminderTime(reminderAtMillis)}")
        } else {
            com.example.util.ReminderScheduler.cancelNoteReminder(context, note.id)
            showToast("Reminder cleared")
        }
    }

    fun removeAttachment(noteId: String, attachmentId: String) {
        val now = System.currentTimeMillis()
        val updated = _uiState.value.notes.map { note ->
            if (note.id == noteId) {
                note.copy(
                    attachments = note.attachments.filterNot { it.id == attachmentId },
                    updatedAt = now
                )
            } else note
        }
        persistNotes(updated)
    }

    fun restoreVersion(noteId: String, version: NoteVersion) {
        val now = System.currentTimeMillis()
        val updated = _uiState.value.notes.map { note ->
            if (note.id == noteId) {
                val snapOfCurrent = NoteVersion(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    title = note.title,
                    body = note.body,
                    checklist = note.checklist,
                    isChecklist = note.isChecklist
                )
                note.copy(
                    title = version.title,
                    body = version.body,
                    checklist = version.checklist,
                    isChecklist = version.isChecklist,
                    versions = (listOf(snapOfCurrent) + note.versions).take(10),
                    activityLog = (listOf(ActivityLogEntry(UUID.randomUUID().toString(), now, "restored")) + note.activityLog).take(30),
                    updatedAt = now
                )
            } else note
        }
        persistNotes(updated)
        showToast("Version restored")
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun commitRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val next = (listOf(trimmed) + _uiState.value.recentSearches.filterNot { it.equals(trimmed, ignoreCase = true) }).take(6)
        repository.saveRecentSearches(next)
        _uiState.update { it.copy(recentSearches = next) }
    }

    fun setFilterTab(tab: FilterTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun toggleFilterRow() {
        _uiState.update { it.copy(showFilterRow = !it.showFilterRow) }
    }

    fun updateSearchFilters(
        type: TypeFilter? = null,
        folder: String? = null,
        dateRange: DateRangeFilter? = null,
        dateMode: DateModeFilter? = null
    ) {
        _uiState.update { state ->
            val current = state.searchFilters
            state.copy(
                searchFilters = current.copy(
                    type = type ?: current.type,
                    folder = folder ?: current.folder,
                    dateRange = dateRange ?: current.dateRange,
                    dateMode = dateMode ?: current.dateMode
                )
            )
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val next = transform(_uiState.value.settings)
        val ok = repository.saveSettings(next)
        _uiState.update {
            it.copy(
                settings = next,
                storageBytes = repository.calculateStorageBytes(),
                usingMemoryFallback = repository.isUsingMemoryFallback() || !ok
            )
        }
        triggerSavedPill()
    }

    fun exportBackupJson(): String {
        val s = _uiState.value
        return repository.exportFullBackupJson(s.notes, s.settings, s.recentSearches)
    }

    fun importBackupJson(json: String): Boolean {
        val pair = repository.importFullBackupJson(json) ?: return false
        _uiState.update {
            it.copy(
                notes = pair.first,
                settings = pair.second,
                selectedNoteId = pair.first.firstOrNull { n -> !n.isTrashed }?.id,
                storageBytes = repository.calculateStorageBytes()
            )
        }
        triggerSavedPill()
        showToast("Backup restored (${pair.first.size} notes)")
        return true
    }

    fun getFilteredNotes(state: NotesUiState = _uiState.value): List<Note> {
        val now = System.currentTimeMillis()
        val q = state.searchQuery.trim().lowercase()
        val tagQuery = if (q.startsWith("#") && q.length > 1) q.removePrefix("#") else null
        val filters = state.searchFilters

        return state.notes.filter { note ->
            // 1. Tab filter
            val matchesTab = when (state.activeTab) {
                FilterTab.ALL -> !note.isTrashed && !note.isArchived
                FilterTab.PINNED -> !note.isTrashed && !note.isArchived && note.isPinned
                FilterTab.CHECKLISTS -> !note.isTrashed && !note.isArchived && note.isChecklist
                FilterTab.ARCHIVE -> !note.isTrashed && note.isArchived
                FilterTab.TRASH -> note.isTrashed
            }
            if (!matchesTab) return@filter false

            // 2. Search query filter
            if (q.isNotEmpty()) {
                val matchesSearch = if (tagQuery != null) {
                    note.tags.any { it.lowercase().contains(tagQuery) }
                } else {
                    note.title.lowercase().contains(q) ||
                        note.body.lowercase().contains(q) ||
                        note.checklist.any { it.text.lowercase().contains(q) } ||
                        note.tags.any { it.lowercase().contains(q) } ||
                        note.folder.lowercase().contains(q) ||
                        note.attachments.any { it.name.lowercase().contains(q) }
                }
                if (!matchesSearch) return@filter false
            }

            // 3. Advanced Filter Row
            val matchesType = when (filters.type) {
                TypeFilter.ANY -> true
                TypeFilter.TEXT -> !note.isChecklist
                TypeFilter.CHECKLIST -> note.isChecklist
                TypeFilter.VOICE -> note.attachments.any { it.type == AttachmentType.VOICE }
                TypeFilter.IMAGE -> note.attachments.any { it.type == AttachmentType.IMAGE }
                TypeFilter.DRAWING -> note.attachments.any { it.type == AttachmentType.DRAWING }
                TypeFilter.SCAN -> note.attachments.any { it.type == AttachmentType.SCAN }
            }
            if (!matchesType) return@filter false

            if (filters.folder.isNotEmpty() && !note.folder.equals(filters.folder, ignoreCase = true)) {
                return@filter false
            }

            val refTime = if (filters.dateMode == DateModeFilter.CREATED) note.createdAt else note.updatedAt
            val matchesDate = when (filters.dateRange) {
                DateRangeFilter.ANY -> true
                DateRangeFilter.LAST_DAY -> (now - refTime) <= 24L * 3600_000L
                DateRangeFilter.LAST_7_DAYS -> (now - refTime) <= 7L * 24L * 3600_000L
                DateRangeFilter.LAST_30_DAYS -> (now - refTime) <= 30L * 24L * 3600_000L
            }
            matchesDate
        }.sortedWith(
            compareByDescending<Note> { it.isPinned }
                .thenByDescending {
                    if (filters.dateMode == DateModeFilter.CREATED) it.createdAt else it.updatedAt
                }
        )
    }

    fun getRecentNotes(state: NotesUiState = _uiState.value): List<Note> {
        return state.notes
            .filter { !it.isTrashed && !it.isArchived }
            .sortedByDescending { it.updatedAt }
            .take(3)
    }

    fun getAllFolders(state: NotesUiState = _uiState.value): List<String> {
        return state.notes
            .map { it.folder.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotesViewModel(NotesRepository(context)) as T
        }
    }
}
