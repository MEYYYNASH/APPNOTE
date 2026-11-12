package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.model.LanguageOption
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
import com.example.ui.screens.SingleFileWebScreen
import com.example.ui.theme.GlassNotesTheme
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.viewmodel.NotesViewModel
import com.example.util.GitHubSyncHelper
import com.example.util.ReminderScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: NotesViewModel = viewModel(factory = NotesViewModel.Factory(context))
            GlassNotesApp(viewModel = viewModel)
        }
    }
}

@Composable
fun GlassNotesApp(viewModel: NotesViewModel) {
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
                when {
                    state.settings.webAppMode -> {
                        SingleFileWebScreen(
                            onBack = {
                                viewModel.updateSettings { it.copy(webAppMode = false) }
                            },
                            onShowToast = { msg -> viewModel.showToast(msg) }
                        )
                    }
                    state.isSettingsOpen -> {
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
                            onOpenWebMode = {
                                viewModel.closeSettings()
                                viewModel.updateSettings { it.copy(webAppMode = true) }
                            },
                            onShowToast = { msg -> viewModel.showToast(msg) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
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
                                        modifier = Modifier
                                            .width(380.dp)
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
                                // Mobile: True Full-Screen edge-to-edge layout (zero outer margin box!)
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
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
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
