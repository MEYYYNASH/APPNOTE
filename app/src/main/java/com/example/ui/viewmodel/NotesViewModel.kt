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
import com.example.model.Note
import com.example.model.NoteAttachment
import com.example.model.NoteColor
import com.example.model.NoteFolder
import com.example.model.NoteType
import com.example.model.NoteVersion
import com.example.model.RichTextSpan
import com.example.model.SearchFilters
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
    val folders: List<NoteFolder> = emptyList(),
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

class NotesViewModel(
    private val repository: NotesRepository,
    private val appContext: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private var savedPillJob: Job? = null
    private var toastJob: Job? = null
    private var pulseJob: Job? = null
    private val lastSnapshotTimeByNote = mutableMapOf<String, Long>()
    private var lastDuplicateWarnNoteId: String? = null

    init {
        val loadedNotes = repository.loadNotes()
        val loadedFolders = repository.loadFolders()
        val loadedSettings = repository.loadSettings()
        val loadedSearches = repository.loadRecentSearches()
        _uiState.value = NotesUiState(
            notes = loadedNotes,
            folders = loadedFolders,
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
        appContext?.let { ctx ->
            com.example.widget.NotesWidgetManager.notifyDataChanged(ctx)
        }
        if (showSaved) {
            triggerSavedPill()
        }
    }

    private fun persistFolders(updatedFolders: List<NoteFolder>, showSaved: Boolean = true) {
        val ok = repository.saveFolders(updatedFolders)
        _uiState.update {
            it.copy(
                folders = updatedFolders,
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

    fun createNewNote(initialFolder: String? = null) {
        val now = System.currentTimeMillis()
        val defaultIsChecklist = _uiState.value.settings.defaultNoteType == NoteType.CHECKLIST
        val accentColor = _uiState.value.settings.accent
        val initialChecklist = if (defaultIsChecklist) {
            listOf(ChecklistItem(UUID.randomUUID().toString(), "", false))
        } else {
            emptyList()
        }
        val activeFolder = initialFolder ?: _uiState.value.searchFilters.folder
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = "",
            body = "",
            isChecklist = defaultIsChecklist,
            checklist = initialChecklist,
            color = accentColor,
            isPinned = false,
            folder = activeFolder,
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

    fun createNoteFromTemplate(
        title: String,
        body: String,
        isChecklist: Boolean,
        checklist: List<ChecklistItem>,
        color: NoteColor,
        folder: String = "",
        tags: List<String> = emptyList()
    ) {
        val now = System.currentTimeMillis()
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = title,
            body = body,
            isChecklist = isChecklist,
            checklist = checklist,
            color = color,
            folder = folder,
            tags = tags,
            versions = listOf(
                NoteVersion(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    title = title,
                    body = body,
                    checklist = checklist,
                    isChecklist = isChecklist
                )
            ),
            activityLog = listOf(
                ActivityLogEntry(UUID.randomUUID().toString(), now, "created from template")
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
                activeTab = FilterTab.ALL,
                isMarkdownPreview = false
            )
        }
        showToast("Created \"$title\"")
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
            folder = _uiState.value.searchFilters.folder,
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

    fun createQuickReminderNote(
        context: Context,
        title: String,
        body: String,
        delayMinutes: Int,
        soundStyle: String
    ) {
        val cleanTitle = title.trim().ifBlank { "Quick Reminder" }
        val cleanBody = body.trim()
        val now = System.currentTimeMillis()
        val triggerAt = now + delayMinutes.coerceAtLeast(1) * 60_000L
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            body = cleanBody,
            isChecklist = false,
            color = NoteColor.AMBER,
            isPinned = false,
            folder = _uiState.value.searchFilters.folder,
            reminderAt = triggerAt,
            reminderFired = false,
            reminderSoundStyle = soundStyle,
            versions = listOf(
                NoteVersion(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    title = cleanTitle,
                    body = cleanBody,
                    checklist = emptyList(),
                    isChecklist = false
                )
            ),
            activityLog = listOf(
                ActivityLogEntry(UUID.randomUUID().toString(), now, "created with reminder")
            ),
            createdAt = now,
            updatedAt = now
        )
        val updated = listOf(newNote) + _uiState.value.notes
        lastSnapshotTimeByNote[newNote.id] = now
        persistNotes(updated)
        triggerEntrancePulse(newNote.id)

        com.example.util.ReminderScheduler.scheduleNoteReminder(
            context = context,
            noteId = newNote.id,
            title = cleanTitle,
            body = cleanBody,
            triggerAtMillis = triggerAt
        )
        val delayMs = (triggerAt - System.currentTimeMillis()).coerceAtLeast(1000L)
        if (delayMs <= 120_000L) {
            viewModelScope.launch {
                delay(delayMs)
                val current = _uiState.value.notes.find { it.id == newNote.id }
                if (current != null && current.reminderAt == triggerAt && !current.reminderFired) {
                    com.example.util.ReminderScheduler.showReminderNotification(
                        context = context,
                        noteId = current.id,
                        title = current.title.ifBlank { "Untitled Note" },
                        body = current.body
                    )
                    val firedList = _uiState.value.notes.map {
                        if (it.id == newNote.id) it.copy(reminderFired = true) else it
                    }
                    persistNotes(firedList, showSaved = false)
                }
            }
        }
        _uiState.update {
            it.copy(
                selectedNoteId = newNote.id,
                activeTab = FilterTab.ALL
            )
        }
        showToast("Reminder set for ${com.example.util.ReminderScheduler.formatReminderTime(triggerAt)}")
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
        isPinned: Boolean? = null,
        isFavorite: Boolean? = null,
        richSpans: List<RichTextSpan>? = null,
        bodyFontStyle: String? = null,
        bodyFontSizeSp: Float? = null,
        bodyTextColorArgb: Long? = null,
        clearBodyTextColor: Boolean = false,
        bodyTextAlign: String? = null
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
                val newFavorite = isFavorite ?: note.isFavorite

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

                val newRichSpans = richSpans ?: note.richSpans
                val newBodyFontStyle = bodyFontStyle ?: note.bodyFontStyle
                val newBodyFontSizeSp = bodyFontSizeSp ?: note.bodyFontSizeSp
                val newBodyTextColorArgb = if (clearBodyTextColor) null else (bodyTextColorArgb ?: note.bodyTextColorArgb)
                val newBodyTextAlign = bodyTextAlign ?: note.bodyTextAlign

                note.copy(
                    title = newTitle,
                    body = newBody,
                    isChecklist = newIsChecklist,
                    checklist = newChecklist,
                    tags = newTags,
                    folder = newFolder,
                    color = newColor,
                    isPinned = newPinned,
                    isFavorite = newFavorite,
                    versions = newVersions,
                    activityLog = newLogs,
                    richSpans = newRichSpans,
                    bodyFontStyle = newBodyFontStyle,
                    bodyFontSizeSp = newBodyFontSizeSp,
                    bodyTextColorArgb = newBodyTextColorArgb,
                    bodyTextAlign = newBodyTextAlign,
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

    fun toggleFavorite(noteId: String) {
        val target = _uiState.value.notes.find { it.id == noteId } ?: return
        updateNoteContent(noteId = noteId, isFavorite = !target.isFavorite)
    }

    fun toggleChecklistItemInNote(noteId: String, itemId: String) {
        val target = _uiState.value.notes.find { it.id == noteId } ?: return
        val updatedChecklist = target.checklist.map { item ->
            if (item.id == itemId) item.copy(isDone = !item.isDone) else item
        }
        updateNoteContent(noteId = noteId, checklist = updatedChecklist)
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

    // --- Suite 1: Folder Management Operations ---
    fun createFolder(
        name: String,
        parentId: String? = null,
        iconId: String = "folder",
        color: NoteColor = NoteColor.VIOLET,
        isSmart: Boolean = false,
        smartQuery: String = ""
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val current = _uiState.value.folders
        val newFolder = NoteFolder(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            parentId = parentId?.takeIf { it.isNotBlank() },
            iconId = iconId,
            color = color,
            isPinned = false,
            isSmart = isSmart,
            smartQuery = smartQuery.trim(),
            sortOrder = current.size,
            createdAt = System.currentTimeMillis()
        )
        persistFolders(current + newFolder)
        showToast("Created folder \"$cleanName\"")
    }

    fun renameFolder(folderId: String, newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) return
        val target = _uiState.value.folders.find { it.id == folderId } ?: return
        val oldName = target.name
        val updatedFolders = _uiState.value.folders.map {
            if (it.id == folderId) it.copy(name = cleanName) else it
        }
        persistFolders(updatedFolders)
        val updatedNotes = _uiState.value.notes.map { note ->
            if (note.folder.equals(oldName, ignoreCase = true)) {
                note.copy(folder = cleanName)
            } else note
        }
        persistNotes(updatedNotes, showSaved = false)
        showToast("Renamed folder to \"$cleanName\"")
    }

    fun updateFolderCustomization(
        folderId: String,
        iconId: String? = null,
        color: NoteColor? = null,
        parentId: String? = null
    ) {
        val updated = _uiState.value.folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(
                    iconId = iconId ?: folder.iconId,
                    color = color ?: folder.color,
                    parentId = if (parentId != null) parentId.ifBlank { null } else folder.parentId
                )
            } else folder
        }
        persistFolders(updated)
    }

    fun togglePinFolder(folderId: String) {
        val updated = _uiState.value.folders.map {
            if (it.id == folderId) it.copy(isPinned = !it.isPinned) else it
        }
        persistFolders(updated)
    }

    fun toggleArchiveFolder(folderId: String) {
        val updated = _uiState.value.folders.map {
            if (it.id == folderId) it.copy(isArchived = !it.isArchived) else it
        }
        persistFolders(updated)
    }

    fun deleteFolder(folderId: String) {
        val now = System.currentTimeMillis()
        val target = _uiState.value.folders.find { it.id == folderId } ?: return
        val updated = _uiState.value.folders.map {
            if (it.id == folderId) it.copy(isTrashed = true, isPinned = false, trashedAt = now) else it
        }
        persistFolders(updated)
        showToast("Moved folder \"${target.name}\" to Trash")
    }

    fun restoreDeletedFolder(folderId: String) {
        val target = _uiState.value.folders.find { it.id == folderId } ?: return
        val updated = _uiState.value.folders.map {
            if (it.id == folderId) it.copy(isTrashed = false, trashedAt = null) else it
        }
        persistFolders(updated)
        showToast("Restored folder \"${target.name}\"")
    }

    fun permanentlyDeleteFolder(folderId: String) {
        val target = _uiState.value.folders.find { it.id == folderId } ?: return
        val updatedFolders = _uiState.value.folders.filterNot { it.id == folderId }
        persistFolders(updatedFolders)
        val updatedNotes = _uiState.value.notes.map { note ->
            if (note.folder.equals(target.name, ignoreCase = true)) note.copy(folder = "") else note
        }
        persistNotes(updatedNotes, showSaved = false)
        showToast("Deleted folder \"${target.name}\"")
    }

    fun reorderFolder(folderId: String, moveUp: Boolean) {
        val active = _uiState.value.folders.filter { !it.isTrashed }.sortedBy { it.sortOrder }.toMutableList()
        val idx = active.indexOfFirst { it.id == folderId }
        if (idx == -1) return
        val targetIdx = if (moveUp) (idx - 1).coerceAtLeast(0) else (idx + 1).coerceAtMost(active.lastIndex)
        if (idx == targetIdx) return
        val item = active.removeAt(idx)
        active.add(targetIdx, item)
        val orderMap = active.mapIndexed { index, f -> f.id to index }.toMap()
        val updated = _uiState.value.folders.map { f ->
            orderMap[f.id]?.let { f.copy(sortOrder = it) } ?: f
        }
        persistFolders(updated)
    }

    fun moveNoteToFolder(noteId: String, targetFolderName: String) {
        updateNoteContent(noteId = noteId, folder = targetFolderName)
        showToast(
            if (targetFolderName.isBlank()) "Removed note from folder"
            else "Moved note to \"$targetFolderName\""
        )
    }

    fun copyNoteToFolder(noteId: String, targetFolderName: String) {
        val source = _uiState.value.notes.find { it.id == noteId } ?: return
        val now = System.currentTimeMillis()
        val copyNote = source.copy(
            id = UUID.randomUUID().toString(),
            folder = targetFolderName,
            isTrashed = false,
            trashedAt = null,
            createdAt = now,
            updatedAt = now,
            activityLog = listOf(ActivityLogEntry(UUID.randomUUID().toString(), now, "copied to $targetFolderName"))
        )
        persistNotes(listOf(copyNote) + _uiState.value.notes)
        showToast("Copied \"${source.title.ifBlank { "Untitled" }}\" to \"$targetFolderName\"")
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

    fun setNoteReminder(
        context: Context,
        noteId: String,
        reminderAtMillis: Long?,
        soundStyle: String? = null,
        alertMode: String? = null,
        vibrationPattern: String? = null,
        volumePercent: Int? = null
    ) {
        val now = System.currentTimeMillis()
        var targetNote: Note? = null
        val updated = _uiState.value.notes.map { note ->
            if (note.id == noteId) {
                val n = note.copy(
                    reminderAt = reminderAtMillis,
                    reminderFired = false,
                    reminderSoundStyle = soundStyle ?: note.reminderSoundStyle,
                    reminderAlertMode = alertMode ?: note.reminderAlertMode,
                    reminderVibrationPattern = vibrationPattern ?: note.reminderVibrationPattern,
                    reminderVolume = volumePercent ?: note.reminderVolume,
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
                folders = repository.loadFolders(),
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
            val matchesTab = when (state.activeTab) {
                FilterTab.ALL -> !note.isTrashed && !note.isArchived
                FilterTab.PINNED -> !note.isTrashed && !note.isArchived && note.isPinned
                FilterTab.FAVORITES -> !note.isTrashed && !note.isArchived && note.isFavorite
                FilterTab.FOLDERS -> !note.isTrashed && !note.isArchived && note.folder.isNotBlank()
                FilterTab.CHECKLISTS -> !note.isTrashed && !note.isArchived && note.isChecklist
                FilterTab.ARCHIVE -> !note.isTrashed && note.isArchived
                FilterTab.TRASH -> note.isTrashed
            }
            if (!matchesTab) return@filter false

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

            val matchesType = when (filters.type) {
                TypeFilter.ANY -> true
                TypeFilter.TEXT -> !note.isChecklist
                TypeFilter.CHECKLIST -> note.isChecklist
                TypeFilter.VOICE -> note.attachments.any { it.type == AttachmentType.VOICE }
                TypeFilter.IMAGE -> note.attachments.any { it.type == AttachmentType.IMAGE }
                TypeFilter.DRAWING -> note.attachments.any { it.type == AttachmentType.DRAWING }
                TypeFilter.SCAN -> note.attachments.any { it.type == AttachmentType.SCAN || it.type == AttachmentType.PDF }
            }
            if (!matchesType) return@filter false

            if (filters.folder.isNotEmpty()) {
                val smartFolder = state.folders.find { it.isSmart && it.name.equals(filters.folder, ignoreCase = true) }
                if (smartFolder != null && smartFolder.smartQuery.isNotBlank()) {
                    val sq = smartFolder.smartQuery.lowercase()
                    val matchesSmart = note.title.lowercase().contains(sq) ||
                        note.body.lowercase().contains(sq) ||
                        note.tags.any { it.lowercase().contains(sq.removePrefix("#")) }
                    if (!matchesSmart) return@filter false
                } else if (!note.folder.equals(filters.folder, ignoreCase = true)) {
                    return@filter false
                }
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
        val fromFolderModels = state.folders.filter { !it.isTrashed && !it.isArchived }.map { it.name.trim() }
        val fromNotes = state.notes.filter { !it.isTrashed }.map { it.folder.trim() }
        return (fromFolderModels + fromNotes)
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sorted()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotesViewModel(
                repository = NotesRepository(context),
                appContext = context.applicationContext
            ) as T
        }
    }
}
