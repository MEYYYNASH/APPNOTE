package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AttachmentType
import com.example.model.LanguageOption
import com.example.model.NoteAttachment
import com.example.model.NoteColor
import com.example.model.ThemeOption
import com.example.model.Translations
import com.example.ui.components.AppGlassLogo
import com.example.ui.components.DynamicIslandSavePill
import com.example.ui.components.GlassBackground
import com.example.ui.components.UndoToastBar
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.screens.NoteEditorPane
import com.example.ui.screens.NoteListPane
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.GlassNotesTheme
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.viewmodel.NotesViewModel
import com.example.util.GitHubSyncHelper
import com.example.widget.NotesAppWidgetProvider
import com.example.widget.WidgetSyncWorker
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val latestIntentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        latestIntentState.value = intent
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: NotesViewModel = viewModel(factory = NotesViewModel.Factory(context))
            val pendingIntent by latestIntentState
            GlassNotesApp(
                viewModel = viewModel,
                pendingIntent = pendingIntent,
                onIntentConsumed = { latestIntentState.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestIntentState.value = intent
    }
}

@Composable
fun GlassNotesApp(
    viewModel: NotesViewModel,
    pendingIntent: Intent? = null,
    onIntentConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val isCurrentlyDark = when (state.settings.theme) {
        ThemeOption.SYSTEM -> systemDark
        ThemeOption.LIGHT -> false
        ThemeOption.DARK -> true
    }

    val strings = Translations.forLanguage(state.settings.language)
    val isKhmer = state.settings.language == LanguageOption.KHMER
    val filteredNotes = viewModel.getFilteredNotes(state)
    val recentNotes = viewModel.getRecentNotes(state)
    val allFolders = viewModel.getAllFolders(state)
    val selectedNote = state.notes.find { it.id == state.selectedNoteId }
    val trashedCount = state.notes.count { it.isTrashed }

    val toggleQuickTheme = {
        val nextTheme = if (isCurrentlyDark) ThemeOption.LIGHT else ThemeOption.DARK
        viewModel.updateSettings { it.copy(theme = nextTheme) }
    }

    val handleSaveScannedDocAsNote: (String, String, String, String, String, String) -> Unit =
        { docName, targetFolder, pdfName, pdfB64, previewImgB64, ocrText ->
            val attachments = buildList {
                if (previewImgB64.isNotBlank()) {
                    add(
                        NoteAttachment(
                            id = UUID.randomUUID().toString(),
                            type = AttachmentType.SCAN,
                            name = "$docName.jpg",
                            dataBase64 = previewImgB64
                        )
                    )
                }
                if (pdfB64.isNotBlank()) {
                    add(
                        NoteAttachment(
                            id = UUID.randomUUID().toString(),
                            type = AttachmentType.PDF,
                            name = pdfName,
                            dataBase64 = pdfB64
                        )
                    )
                }
            }
            viewModel.createNoteFromTemplate(
                title = docName,
                body = ocrText.ifBlank { "Scanned document saved to $targetFolder." },
                isChecklist = false,
                checklist = emptyList(),
                folder = targetFolder.ifBlank { "Scanned Docs" },
                color = NoteColor.BLUE,
                tags = listOf("scan", "pdf")
            )
            val newestId = viewModel.uiState.value.selectedNoteId
            if (newestId != null) {
                attachments.forEach { att ->
                    viewModel.addAttachment(newestId, att.type, att.name, att.dataBase64, att.durationSec)
                }
            }
        }

    var quickCaptureTrigger by remember { mutableIntStateOf(0) }
    var quickReminderTrigger by remember { mutableIntStateOf(0) }
    var widgetStudioTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        WidgetSyncWorker.enqueueImmediateSync(context)
    }

    LaunchedEffect(pendingIntent) {
        val intent = pendingIntent ?: return@LaunchedEffect
        val targetNoteId = intent.getStringExtra(NotesAppWidgetProvider.EXTRA_NOTE_ID)
        val actionCreateNote = intent.getBooleanExtra(NotesAppWidgetProvider.EXTRA_ACTION_CREATE_NOTE, false)
        val actionQuickCapture = intent.getBooleanExtra(NotesAppWidgetProvider.EXTRA_ACTION_QUICK_CAPTURE, false)
        val actionQuickReminder = intent.getBooleanExtra(NotesAppWidgetProvider.EXTRA_ACTION_QUICK_REMINDER, false)
        val widgetPinned = intent.getBooleanExtra("widget_pinned_success", false)

        if (!targetNoteId.isNullOrBlank()) {
            viewModel.closeSettings()
            viewModel.selectNote(targetNoteId)
        } else if (actionCreateNote) {
            viewModel.closeSettings()
            viewModel.createNewNote()
        } else if (actionQuickCapture) {
            viewModel.closeSettings()
            viewModel.closeMobileEditor()
            quickCaptureTrigger++
        } else if (actionQuickReminder) {
            viewModel.closeSettings()
            viewModel.closeMobileEditor()
            quickReminderTrigger++
        } else if (widgetPinned) {
            viewModel.showToast("Widget added to Home Screen!")
        }
        onIntentConsumed()
    }

    var detectedGitHubReleaseTag by remember { mutableStateOf("") }
    var dismissedUpdateBannerForVersion by remember { mutableStateOf("") }
    val hasNewAppUpdate = detectedGitHubReleaseTag.isNotBlank()

    // Query official GitHub Releases (MEYYYNASH/APPNOTE) in background; never show fake updates!
    LaunchedEffect(state.settings.includePrereleases, state.settings.githubToken) {
        val result = GitHubSyncHelper.checkGitHubReleaseUpdate(
            context = context,
            includePrerelease = state.settings.includePrereleases,
            notifyOnNewRelease = true,
            token = state.settings.githubToken
        )
        if (result.hasNewUpdate) {
            detectedGitHubReleaseTag = result.latestReleaseTag
            viewModel.updateSettings { it.copy(latestAvailableVersion = result.latestReleaseTag) }
        } else {
            detectedGitHubReleaseTag = ""
            if (state.settings.latestAvailableVersion.isNotBlank()) {
                viewModel.updateSettings { it.copy(latestAvailableVersion = "") }
            }
        }
    }

    GlassNotesTheme(settings = state.settings) {
        val tokens = LocalGlassTokens.current
        GlassBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                if (state.isSettingsOpen) {
                    SettingsScreen(
                        settings = state.settings,
                        strings = strings,
                        storageBytes = state.storageBytes,
                        trashedCount = trashedCount,
                        usingMemoryFallback = state.usingMemoryFallback,
                        onBack = { viewModel.closeSettings() },
                        onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                        onEmptyTrash = { viewModel.emptyTrash() },
                        onExportBackupJson = { viewModel.exportBackupJson() },
                        onImportBackupJson = { json -> viewModel.importBackupJson(json) },
                        onOpenWidgetStudio = { widgetStudioTrigger++ },
                        onShowToast = { msg -> viewModel.showToast(msg) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isWideScreen = maxWidth >= 720.dp

                        if (isWideScreen) {
                            // Full-screen two-pane split with a crisp 1dp glass divider
                            Row(modifier = Modifier.fillMaxSize()) {
                                NoteListPane(
                                    notes = filteredNotes,
                                    recentNotes = recentNotes,
                                    allFolders = allFolders,
                                    selectedNoteId = state.selectedNoteId,
                                    searchQuery = state.searchQuery,
                                    recentSearches = state.recentSearches,
                                    activeTab = state.activeTab,
                                    showFilterRow = state.showFilterRow,
                                    searchFilters = state.searchFilters,
                                    hidePreviews = state.settings.hidePreviews,
                                    language = state.settings.language,
                                    pulseNoteId = state.pulseNoteId,
                                    strings = strings,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onCommitSearch = viewModel::commitRecentSearch,
                                    onSelectTab = viewModel::setFilterTab,
                                    onToggleFilterRow = viewModel::toggleFilterRow,
                                    onUpdateFilters = viewModel::updateSearchFilters,
                                    onSelectNote = viewModel::selectNote,
                                    onCreateNote = viewModel::createNewNote,
                                    onQuickCapture = viewModel::createQuickCaptureNote,
                                    onToggleTheme = toggleQuickTheme,
                                    onOpenSettings = viewModel::openSettings,
                                    onChangeNoteColor = { id, c ->
                                        viewModel.updateNoteContent(noteId = id, color = c)
                                    },
                                    onTogglePin = viewModel::togglePin,
                                    onDuplicateNote = viewModel::duplicateNote,
                                    onToggleArchive = viewModel::toggleArchive,
                                    onDeleteNote = viewModel::moveToTrash,
                                    onEmptyTrash = viewModel::emptyTrash,
                                    folders = state.folders,
                                    settings = state.settings,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onToggleChecklistItem = viewModel::toggleChecklistItemInNote,
                                    onMoveNoteToFolder = viewModel::moveNoteToFolder,
                                    onCopyNoteToFolder = viewModel::copyNoteToFolder,
                                    onCreateFolder = viewModel::createFolder,
                                    onRenameFolder = viewModel::renameFolder,
                                    onUpdateFolderCustomization = viewModel::updateFolderCustomization,
                                    onTogglePinFolder = viewModel::togglePinFolder,
                                    onToggleArchiveFolder = viewModel::toggleArchiveFolder,
                                    onDeleteFolder = viewModel::deleteFolder,
                                    onRestoreDeletedFolder = viewModel::restoreDeletedFolder,
                                    onPermanentlyDeleteFolder = viewModel::permanentlyDeleteFolder,
                                    onReorderFolder = viewModel::reorderFolder,
                                    onSaveScannedDocumentAsNote = handleSaveScannedDocAsNote,
                                    onCreateQuickReminder = { title, body, delayMins, soundStyle ->
                                        viewModel.createQuickReminderNote(
                                            context = context,
                                            title = title,
                                            body = body,
                                            delayMinutes = delayMins,
                                            soundStyle = soundStyle
                                        )
                                    },
                                    externalQuickCaptureTrigger = quickCaptureTrigger,
                                    externalQuickReminderTrigger = quickReminderTrigger,
                                    externalWidgetStudioTrigger = widgetStudioTrigger,
                                    onShowToast = viewModel::showToast,
                                    modifier = Modifier
                                        .width(420.dp)
                                        .fillMaxHeight()
                                )

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(tokens.panelBorder)
                                )

                                NoteEditorPane(
                                    note = selectedNote,
                                    showBackButton = false,
                                    isMarkdownPreview = state.isMarkdownPreview,
                                    strings = strings,
                                    isKhmer = isKhmer,
                                    availableFolders = allFolders,
                                    onBack = {},
                                    onUpdateNote = { title, body, isCl, cl, tags, folder, color, pin ->
                                        selectedNote?.let { n ->
                                            viewModel.updateNoteContent(
                                                noteId = n.id,
                                                title = title,
                                                body = body,
                                                isChecklist = isCl,
                                                checklist = cl,
                                                tagsString = tags,
                                                folder = folder,
                                                color = color,
                                                isPinned = pin
                                            )
                                        }
                                    },
                                    onUpdateRichFormatting = { spans, fontStyle, fontSizeSp, textColorArgb, clearTextColor, textAlign ->
                                        selectedNote?.let { n ->
                                            viewModel.updateNoteContent(
                                                noteId = n.id,
                                                richSpans = spans,
                                                bodyFontStyle = fontStyle,
                                                bodyFontSizeSp = fontSizeSp,
                                                bodyTextColorArgb = textColorArgb,
                                                clearBodyTextColor = clearTextColor,
                                                bodyTextAlign = textAlign
                                            )
                                        }
                                    },
                                    onDeleteNote = {
                                        selectedNote?.let { viewModel.moveToTrash(it.id) }
                                    },
                                    onToggleMarkdown = viewModel::toggleMarkdownPreview,
                                    onAddAttachment = { type, name, b64, dur ->
                                        selectedNote?.let {
                                            viewModel.addAttachment(it.id, type, name, b64, dur)
                                        }
                                    },
                                    onUpdateAttachment = { attId, b64 ->
                                        selectedNote?.let {
                                            viewModel.updateAttachmentData(it.id, attId, b64)
                                        }
                                    },
                                    onRemoveAttachment = { attId ->
                                        selectedNote?.let {
                                            viewModel.removeAttachment(it.id, attId)
                                        }
                                    },
                                    onSetReminder = { millis ->
                                        selectedNote?.let {
                                            viewModel.setNoteReminder(context, it.id, millis)
                                        }
                                    },
                                    onSetReminderWithSound = { millis, soundStyle, alertMode, vibPattern ->
                                        selectedNote?.let {
                                            viewModel.setNoteReminder(
                                                context = context,
                                                noteId = it.id,
                                                reminderAtMillis = millis,
                                                soundStyle = soundStyle,
                                                alertMode = alertMode,
                                                vibrationPattern = vibPattern
                                            )
                                        }
                                    },
                                    onToggleFavorite = {
                                        selectedNote?.let { viewModel.toggleFavorite(it.id) }
                                    },
                                    onRestoreVersion = { ver ->
                                        selectedNote?.let {
                                            viewModel.restoreVersion(it.id, ver)
                                        }
                                    },
                                    onShowToast = { msg -> viewModel.showToast(msg) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                        } else {
                            // Mobile: True Full-Screen edge-to-edge layout
                            AnimatedContent(
                                targetState = state.isEditorOpenOnMobile && selectedNote != null,
                                transitionSpec = {
                                    if (!state.settings.animationsEnabled) {
                                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                    } else if (targetState) {
                                        (slideInHorizontally { it } + fadeIn(tween(260))) togetherWith
                                            (slideOutHorizontally { -it / 5 } + fadeOut(tween(220)))
                                    } else {
                                        (slideInHorizontally { -it / 5 } + fadeIn(tween(260))) togetherWith
                                            (slideOutHorizontally { it } + fadeOut(tween(220)))
                                    }
                                },
                                label = "mobile_pane_slide",
                                modifier = Modifier.fillMaxSize()
                            ) { showMobileEditor ->
                                if (showMobileEditor && selectedNote != null) {
                                    NoteEditorPane(
                                        note = selectedNote,
                                        showBackButton = true,
                                        isMarkdownPreview = state.isMarkdownPreview,
                                        strings = strings,
                                        isKhmer = isKhmer,
                                        availableFolders = allFolders,
                                        onBack = viewModel::closeMobileEditor,
                                        onUpdateNote = { title, body, isCl, cl, tags, folder, color, pin ->
                                            viewModel.updateNoteContent(
                                                noteId = selectedNote.id,
                                                title = title,
                                                body = body,
                                                isChecklist = isCl,
                                                checklist = cl,
                                                tagsString = tags,
                                                folder = folder,
                                                color = color,
                                                isPinned = pin
                                            )
                                        },
                                        onUpdateRichFormatting = { spans, fontStyle, fontSizeSp, textColorArgb, clearTextColor, textAlign ->
                                            viewModel.updateNoteContent(
                                                noteId = selectedNote.id,
                                                richSpans = spans,
                                                bodyFontStyle = fontStyle,
                                                bodyFontSizeSp = fontSizeSp,
                                                bodyTextColorArgb = textColorArgb,
                                                clearBodyTextColor = clearTextColor,
                                                bodyTextAlign = textAlign
                                            )
                                        },
                                        onDeleteNote = {
                                            viewModel.moveToTrash(selectedNote.id)
                                        },
                                        onToggleMarkdown = viewModel::toggleMarkdownPreview,
                                        onAddAttachment = { type, name, b64, dur ->
                                            viewModel.addAttachment(selectedNote.id, type, name, b64, dur)
                                        },
                                        onUpdateAttachment = { attId, b64 ->
                                            viewModel.updateAttachmentData(selectedNote.id, attId, b64)
                                        },
                                        onRemoveAttachment = { attId ->
                                            viewModel.removeAttachment(selectedNote.id, attId)
                                        },
                                        onSetReminder = { millis ->
                                            viewModel.setNoteReminder(context, selectedNote.id, millis)
                                        },
                                        onSetReminderWithSound = { millis, soundStyle, alertMode, vibPattern ->
                                            viewModel.setNoteReminder(
                                                context = context,
                                                noteId = selectedNote.id,
                                                reminderAtMillis = millis,
                                                soundStyle = soundStyle,
                                                alertMode = alertMode,
                                                vibrationPattern = vibPattern
                                            )
                                        },
                                        onToggleFavorite = {
                                            viewModel.toggleFavorite(selectedNote.id)
                                        },
                                        onRestoreVersion = { ver ->
                                            viewModel.restoreVersion(selectedNote.id, ver)
                                        },
                                        onShowToast = { msg -> viewModel.showToast(msg) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    NoteListPane(
                                        notes = filteredNotes,
                                        recentNotes = recentNotes,
                                        allFolders = allFolders,
                                        selectedNoteId = state.selectedNoteId,
                                        searchQuery = state.searchQuery,
                                        recentSearches = state.recentSearches,
                                        activeTab = state.activeTab,
                                        showFilterRow = state.showFilterRow,
                                        searchFilters = state.searchFilters,
                                        hidePreviews = state.settings.hidePreviews,
                                        language = state.settings.language,
                                        pulseNoteId = state.pulseNoteId,
                                        strings = strings,
                                        onSearchQueryChange = viewModel::setSearchQuery,
                                        onCommitSearch = viewModel::commitRecentSearch,
                                        onSelectTab = viewModel::setFilterTab,
                                        onToggleFilterRow = viewModel::toggleFilterRow,
                                        onUpdateFilters = viewModel::updateSearchFilters,
                                        onSelectNote = viewModel::selectNote,
                                        onCreateNote = viewModel::createNewNote,
                                        onQuickCapture = viewModel::createQuickCaptureNote,
                                        onToggleTheme = toggleQuickTheme,
                                        onOpenSettings = viewModel::openSettings,
                                        onChangeNoteColor = { id, c ->
                                            viewModel.updateNoteContent(noteId = id, color = c)
                                        },
                                        onTogglePin = viewModel::togglePin,
                                        onDuplicateNote = viewModel::duplicateNote,
                                        onToggleArchive = viewModel::toggleArchive,
                                        onDeleteNote = viewModel::moveToTrash,
                                        onEmptyTrash = viewModel::emptyTrash,
                                        folders = state.folders,
                                        settings = state.settings,
                                        onToggleFavorite = viewModel::toggleFavorite,
                                        onToggleChecklistItem = viewModel::toggleChecklistItemInNote,
                                        onMoveNoteToFolder = viewModel::moveNoteToFolder,
                                        onCopyNoteToFolder = viewModel::copyNoteToFolder,
                                        onCreateFolder = viewModel::createFolder,
                                        onRenameFolder = viewModel::renameFolder,
                                        onUpdateFolderCustomization = viewModel::updateFolderCustomization,
                                        onTogglePinFolder = viewModel::togglePinFolder,
                                        onToggleArchiveFolder = viewModel::toggleArchiveFolder,
                                        onDeleteFolder = viewModel::deleteFolder,
                                        onRestoreDeletedFolder = viewModel::restoreDeletedFolder,
                                        onPermanentlyDeleteFolder = viewModel::permanentlyDeleteFolder,
                                        onReorderFolder = viewModel::reorderFolder,
                                        onSaveScannedDocumentAsNote = handleSaveScannedDocAsNote,
                                        onCreateQuickReminder = { title, body, delayMins, soundStyle ->
                                            viewModel.createQuickReminderNote(
                                                context = context,
                                                title = title,
                                                body = body,
                                                delayMinutes = delayMins,
                                                soundStyle = soundStyle
                                            )
                                        },
                                        externalQuickCaptureTrigger = quickCaptureTrigger,
                                        externalQuickReminderTrigger = quickReminderTrigger,
                                        externalWidgetStudioTrigger = widgetStudioTrigger,
                                        onShowToast = viewModel::showToast,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                // In-App Glass Update Notification Banner (shown ONLY when a real newer GitHub release is published)
                AnimatedVisibility(
                    visible = hasNewAppUpdate &&
                        dismissedUpdateBannerForVersion != detectedGitHubReleaseTag &&
                        !state.isSettingsOpen,
                    enter = fadeIn(tween(240)),
                    exit = fadeOut(tween(180)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp, start = 14.dp, end = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(tokens.panelFill)
                            .border(1.dp, tokens.accentStart.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("in_app_update_notification_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppGlassLogo(
                            sizeDp = 28.dp,
                            cornerRadius = 8.dp
                        )
                        Column {
                            Text(
                                text = if (isKhmer) {
                                    "មានកំណែថ្មី $detectedGitHubReleaseTag លើ GitHub"
                                } else {
                                    "New GitHub Release $detectedGitHubReleaseTag"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isKhmer) {
                                    "ចុចដើម្បីទាញយក និងដំឡើង APK"
                                } else {
                                    "${GitHubSyncHelper.OFFICIAL_REPO_SLUG} • Tap to Download"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .testTag("banner_install_update_button")
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(tokens.accentStart, tokens.accentEnd)
                                    )
                                )
                                .clickable {
                                    viewModel.openSettings()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isKhmer) "អាប់ដេត" else "Update",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable {
                                    dismissedUpdateBannerForVersion = detectedGitHubReleaseTag
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LineIcon(
                                type = LineIconType.CLOSE,
                                tint = tokens.textSecondary,
                                size = 14.dp
                            )
                        }
                    }
                }

                // Dynamic Island Save Pill at Top Center
                DynamicIslandSavePill(
                    visible = state.showSavedPill,
                    label = strings.savedPill,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )

                // Toast with Undo above the Bottom Capsule Dock
                UndoToastBar(
                    toast = state.activeToast,
                    undoLabel = strings.undoLabel,
                    onUndo = { noteId -> viewModel.restoreFromTrash(noteId) },
                    onDismiss = viewModel::dismissToast,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 86.dp)
                )
            }
        }
    }
}
