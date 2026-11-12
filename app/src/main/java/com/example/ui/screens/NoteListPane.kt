package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.AppStrings
import com.example.model.DateModeFilter
import com.example.model.DateRangeFilter
import com.example.model.FilterTab
import com.example.model.LanguageOption
import com.example.model.Note
import com.example.model.NoteColor
import com.example.model.NoteFolder
import com.example.model.SearchFilters
import com.example.model.Translations
import com.example.model.TypeFilter
import com.example.ui.components.AppGlassLogo
import com.example.ui.components.BottomDockSection
import com.example.ui.components.DocumentScannerDialog
import com.example.ui.components.FloatingCapsuleBottomDock
import com.example.ui.components.FoldersManagerDialog
import com.example.ui.components.GlassIconButton
import com.example.ui.components.NoteContextBottomSheet
import com.example.ui.components.QuickCaptureDialog
import com.example.ui.components.QuickReminderDialog
import com.example.ui.components.WidgetStudioDialog
import com.example.ui.components.formatNoteTime
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.theme.LocalGlassTokens
import com.example.util.ReminderScheduler
import java.util.Calendar

@Composable
fun NoteListPane(
    notes: List<Note>,
    recentNotes: List<Note>,
    allFolders: List<String>,
    selectedNoteId: String?,
    searchQuery: String,
    recentSearches: List<String>,
    activeTab: FilterTab,
    showFilterRow: Boolean,
    searchFilters: SearchFilters,
    hidePreviews: Boolean,
    language: LanguageOption,
    pulseNoteId: String?,
    strings: AppStrings,
    onSearchQueryChange: (String) -> Unit,
    onCommitSearch: (String) -> Unit,
    onSelectTab: (FilterTab) -> Unit,
    onToggleFilterRow: () -> Unit,
    onUpdateFilters: (TypeFilter?, String?, DateRangeFilter?, DateModeFilter?) -> Unit,
    onSelectNote: (String) -> Unit,
    onCreateNote: () -> Unit,
    onQuickCapture: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangeNoteColor: (String, NoteColor) -> Unit,
    onTogglePin: (String) -> Unit,
    onDuplicateNote: (String) -> Unit,
    onToggleArchive: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onEmptyTrash: () -> Unit,
    folders: List<NoteFolder> = emptyList(),
    settings: AppSettings = AppSettings(),
    onToggleFavorite: (String) -> Unit = {},
    onToggleChecklistItem: (String, String) -> Unit = { _, _ -> },
    onMoveNoteToFolder: (String, String) -> Unit = { _, _ -> },
    onCopyNoteToFolder: (String, String) -> Unit = { _, _ -> },
    onCreateFolder: (String, String?, String, NoteColor, Boolean, String) -> Unit = { _, _, _, _, _, _ -> },
    onRenameFolder: (String, String) -> Unit = { _, _ -> },
    onUpdateFolderCustomization: (String, String?, NoteColor?, String?) -> Unit = { _, _, _, _ -> },
    onTogglePinFolder: (String) -> Unit = {},
    onToggleArchiveFolder: (String) -> Unit = {},
    onDeleteFolder: (String) -> Unit = {},
    onRestoreDeletedFolder: (String) -> Unit = {},
    onPermanentlyDeleteFolder: (String) -> Unit = {},
    onReorderFolder: (String, Boolean) -> Unit = { _, _ -> },
    onSaveScannedDocumentAsNote: (String, String, String, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onCreateQuickReminder: (String, String, Int, String) -> Unit = { _, _, _, _ -> },
    externalQuickCaptureTrigger: Int = 0,
    externalQuickReminderTrigger: Int = 0,
    externalWidgetStudioTrigger: Int = 0,
    onShowToast: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val isKhmer = language == LanguageOption.KHMER

    var showQuickCaptureDialog by remember { mutableStateOf(false) }
    var showQuickReminderDialog by remember { mutableStateOf(false) }
    var showWidgetStudioDialog by remember { mutableStateOf(false) }
    var showFoldersDialog by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var contextSheetNote by remember { mutableStateOf<Note?>(null) }
    var searchFocused by remember { mutableStateOf(false) }

    LaunchedEffect(externalQuickCaptureTrigger) {
        if (externalQuickCaptureTrigger > 0) {
            showQuickCaptureDialog = true
        }
    }
    LaunchedEffect(externalQuickReminderTrigger) {
        if (externalQuickReminderTrigger > 0) {
            showQuickReminderDialog = true
        }
    }
    LaunchedEffect(externalWidgetStudioTrigger) {
        if (externalWidgetStudioTrigger > 0) {
            showWidgetStudioDialog = true
        }
    }

    val dayIndex = remember {
        val cal = Calendar.getInstance()
        (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
    }
    val dailyQuote = remember(language, dayIndex) {
        val q = if (isKhmer) {
            Translations.weekdayQuotesKm[dayIndex]
        } else {
            Translations.weekdayQuotesEn[dayIndex]
        }
        "“$q”"
    }

    val activeDockSection = when (activeTab) {
        FilterTab.ALL, FilterTab.PINNED, FilterTab.FAVORITES, FilterTab.FOLDERS -> BottomDockSection.HOME
        FilterTab.CHECKLISTS -> BottomDockSection.CHECKLISTS
        FilterTab.ARCHIVE, FilterTab.TRASH -> BottomDockSection.VAULT
    }

    val isOverlayOpen = showQuickCaptureDialog ||
        showQuickReminderDialog ||
        showWidgetStudioDialog ||
        showFoldersDialog ||
        showScannerDialog ||
        contextSheetNote != null

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (isOverlayOpen) 26.dp else 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Clean Minimal Header: Logo + "Notes" + Quote on left; ONLY Quick Capture (⚡) + Theme (☀️/🌙) on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppGlassLogo(
                        sizeDp = 42.dp,
                        cornerRadius = 13.dp,
                        modifier = Modifier.testTag("app_clean_logo_icon")
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.appTitle,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.6).sp
                            ),
                            color = tokens.textPrimary
                        )
                        Text(
                            text = dailyQuote,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = tokens.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .testTag("daily_weekday_quote")
                        )
                    }

                    GlassIconButton(
                        icon = LineIconType.QUICK_CAPTURE,
                        contentDesc = strings.quickCapture,
                        sizeDp = 40.dp,
                        onClick = { showQuickCaptureDialog = true },
                        testTagName = "quick_capture_fab"
                    )

                    GlassIconButton(
                        icon = if (tokens.isDark) LineIconType.SUN else LineIconType.MOON,
                        contentDesc = "Switch theme",
                        sizeDp = 40.dp,
                        onClick = onToggleTheme,
                        testTagName = "toggle_theme_button"
                    )
                }

                // Clean Rounded Search Pill with Filter Toggle inside right edge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .height(46.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(tokens.fieldFill)
                        .border(
                            width = if (searchFocused) 1.6.dp else 1.dp,
                            color = if (searchFocused) tokens.accentStart else tokens.panelBorder,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LineIcon(
                            type = LineIconType.SEARCH,
                            tint = tokens.textSecondary,
                            size = 17.dp
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = strings.searchPlaceholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = tokens.textPrimary
                                ),
                                cursorBrush = SolidColor(tokens.accentStart),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { onCommitSearch(searchQuery) }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { searchFocused = it.isFocused }
                                    .testTag("search_input")
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { onSearchQueryChange("") },
                                contentAlignment = Alignment.Center
                            ) {
                                LineIcon(
                                    type = LineIconType.CLOSE,
                                    tint = tokens.textSecondary,
                                    size = 14.dp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .testTag("filter_chip_filters")
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (showFilterRow) tokens.accentBrush else SolidColor(Color.Transparent)
                                )
                                .clickable(onClick = onToggleFilterRow),
                            contentAlignment = Alignment.Center
                        ) {
                            LineIcon(
                                type = LineIconType.FILTER,
                                tint = if (showFilterRow) Color.White else tokens.textSecondary,
                                size = 16.dp
                            )
                        }
                    }
                }

                // Recent search suggestions (when focused)
                AnimatedVisibility(visible = searchFocused && recentSearches.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        recentSearches.take(6).forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tokens.panelFill)
                                    .border(1.dp, tokens.panelBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSearchQueryChange(suggestion)
                                        onCommitSearch(suggestion)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.textPrimary
                                )
                            }
                        }
                    }
                }

                // Expandable Advanced Filter Row
                AnimatedVisibility(visible = showFilterRow) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterDropdownSelect(
                            currentLabel = if (isKhmer) searchFilters.type.labelKm else searchFilters.type.labelEn,
                            options = TypeFilter.entries.map {
                                (if (isKhmer) it.labelKm else it.labelEn) to {
                                    onUpdateFilters(it, null, null, null)
                                }
                            },
                            testTag = "select_filter_type"
                        )

                        val folderOptions = buildList {
                            add("Folder: any" to { onUpdateFilters(null, "", null, null) })
                            allFolders.forEach { f ->
                                add(f to { onUpdateFilters(null, f, null, null) })
                            }
                        }
                        FilterDropdownSelect(
                            currentLabel = searchFilters.folder.ifEmpty { "Folder: any" },
                            options = folderOptions,
                            testTag = "select_filter_folder"
                        )

                        FilterDropdownSelect(
                            currentLabel = if (isKhmer) searchFilters.dateRange.labelKm else searchFilters.dateRange.labelEn,
                            options = DateRangeFilter.entries.map {
                                (if (isKhmer) it.labelKm else it.labelEn) to {
                                    onUpdateFilters(null, null, it, null)
                                }
                            },
                            testTag = "select_filter_date_range"
                        )

                        FilterDropdownSelect(
                            currentLabel = if (isKhmer) searchFilters.dateMode.labelKm else searchFilters.dateMode.labelEn,
                            options = DateModeFilter.entries.map {
                                (if (isKhmer) it.labelKm else it.labelEn) to {
                                    onUpdateFilters(null, null, null, it)
                                }
                            },
                            testTag = "select_filter_date_mode"
                        )
                    }
                }

                // Clean Translucent Horizontal Pills Row matching screenshot: [All] [Pinned] [Folders] [Widgets] [Remind] [Scanner] [Trash]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CleanFilterPill(
                        label = strings.tabAll,
                        selected = activeTab == FilterTab.ALL && searchFilters.folder.isBlank(),
                        onClick = {
                            onUpdateFilters(null, "", null, null)
                            onSelectTab(FilterTab.ALL)
                        },
                        testTag = "pill_filter_all"
                    )
                    CleanFilterPill(
                        label = strings.tabPinned,
                        selected = activeTab == FilterTab.PINNED,
                        onClick = { onSelectTab(FilterTab.PINNED) },
                        testTag = "pill_filter_pinned"
                    )
                    CleanFilterPill(
                        label = if (isKhmer) "ថតឯកសារ" else "Folders",
                        selected = activeTab == FilterTab.FOLDERS || searchFilters.folder.isNotBlank(),
                        onClick = { showFoldersDialog = true },
                        testTag = "open_folders_manager_button"
                    )
                    CleanFilterPill(
                        label = if (isKhmer) "Widget" else "Widgets",
                        selected = showWidgetStudioDialog,
                        onClick = { showWidgetStudioDialog = true },
                        testTag = "open_widget_studio_button"
                    )
                    CleanFilterPill(
                        label = if (isKhmer) "រំលឹក" else "Remind",
                        selected = showQuickReminderDialog,
                        onClick = { showQuickReminderDialog = true },
                        testTag = "open_quick_reminder_button"
                    )
                    CleanFilterPill(
                        label = if (isKhmer) "ស្កេន" else "Scanner",
                        selected = showScannerDialog,
                        onClick = { showScannerDialog = true },
                        testTag = "open_document_scanner_button"
                    )
                    CleanFilterPill(
                        label = strings.tabTrash,
                        selected = activeTab == FilterTab.TRASH,
                        onClick = { onSelectTab(FilterTab.TRASH) },
                        testTag = "pill_filter_trash"
                    )
                }

                // Active Folder Indicator (if a folder filter is currently applied)
                if (searchFilters.folder.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${searchFilters.folder} (${notes.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.accentStart,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isKhmer) "សម្អាត" else "Clear",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUpdateFilters(null, "", null, null) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Clean Single-Column Frosted Glass Note List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notes_list_column"),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        top = 4.dp,
                        bottom = 110.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeTab == FilterTab.TRASH && notes.isNotEmpty()) {
                        item(key = "trash_header_bar") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.14f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.trashBanner,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tokens.textPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEF4444))
                                        .clickable(onClick = onEmptyTrash)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .testTag("empty_trash_banner_button")
                                ) {
                                    Text(
                                        text = strings.emptyTrashBtn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (notes.isEmpty()) {
                        item(key = "empty_notes_placeholder") {
                            Text(
                                text = if (searchQuery.isNotBlank() || activeTab != FilterTab.ALL) {
                                    "Nothing matches yet."
                                } else {
                                    strings.emptyNotesHint
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = tokens.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 48.dp)
                            )
                        }
                    } else {
                        items(
                            items = notes,
                            key = { it.id }
                        ) { note ->
                            CleanGlassNoteRowCard(
                                note = note,
                                isSelected = note.id == selectedNoteId,
                                hidePreview = hidePreviews,
                                strings = strings,
                                onClick = { onSelectNote(note.id) },
                                onLongClick = { contextSheetNote = note },
                                modifier = Modifier.testTag("note_card_${note.id}")
                            )
                        }
                    }
                }
            }

            // Single Clean Bottom Capsule Navigation Dock matching Screenshot
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .alpha(if (contextSheetNote != null) 0f else 1f)
            ) {
                FloatingCapsuleBottomDock(
                    activeSection = activeDockSection,
                    homeLabel = if (isKhmer) "ទំព័រដើម" else "Home",
                    checklistsLabel = strings.tabChecklists,
                    vaultLabel = strings.tabArchive,
                    settingsLabel = strings.settingsTitle,
                    newNoteLabel = strings.newNote,
                    onSelectSection = { section ->
                        when (section) {
                            BottomDockSection.HOME -> onSelectTab(FilterTab.ALL)
                            BottomDockSection.CHECKLISTS -> onSelectTab(FilterTab.CHECKLISTS)
                            BottomDockSection.VAULT -> onSelectTab(FilterTab.ARCHIVE)
                            BottomDockSection.SETTINGS -> onOpenSettings()
                        }
                    },
                    onNewNote = onCreateNote
                )
            }
        }
    }

    if (showQuickCaptureDialog) {
        QuickCaptureDialog(
            strings = strings,
            onDismiss = { showQuickCaptureDialog = false },
            onSaveCapture = onQuickCapture
        )
    }

    if (showQuickReminderDialog) {
        QuickReminderDialog(
            isKhmer = isKhmer,
            defaultSoundStyle = settings.reminderRingtoneStyle,
            customSoundBase64 = settings.customSoundBase64,
            onDismiss = { showQuickReminderDialog = false },
            onSaveReminder = onCreateQuickReminder
        )
    }

    if (showWidgetStudioDialog) {
        WidgetStudioDialog(
            notes = notes,
            availableFolders = allFolders,
            isKhmer = isKhmer,
            onDismiss = { showWidgetStudioDialog = false },
            onShowToast = onShowToast
        )
    }

    if (showFoldersDialog) {
        FoldersManagerDialog(
            folders = folders,
            notes = notes,
            activeSelectedFolder = searchFilters.folder,
            accentColor = tokens.accentStart,
            isKhmer = isKhmer,
            onDismiss = { showFoldersDialog = false },
            onSelectFolderFilter = { folderName ->
                onUpdateFilters(null, folderName, null, null)
                showFoldersDialog = false
            },
            onCreateFolder = onCreateFolder,
            onRenameFolder = onRenameFolder,
            onUpdateFolderCustomization = onUpdateFolderCustomization,
            onTogglePinFolder = onTogglePinFolder,
            onToggleArchiveFolder = onToggleArchiveFolder,
            onDeleteFolder = onDeleteFolder,
            onRestoreDeletedFolder = onRestoreDeletedFolder,
            onPermanentlyDeleteFolder = onPermanentlyDeleteFolder,
            onReorderFolder = onReorderFolder,
            onMoveNoteToFolder = onMoveNoteToFolder,
            onCopyNoteToFolder = onCopyNoteToFolder
        )
    }

    if (showScannerDialog) {
        DocumentScannerDialog(
            availableFolders = allFolders,
            currentFolder = searchFilters.folder.ifBlank { "Scanned Docs" },
            accentColor = tokens.accentStart,
            isKhmer = isKhmer,
            onDismiss = { showScannerDialog = false },
            onSaveScannedDocument = { docName, targetFolder, pdfName, pdfB64, previewImgB64, ocrText ->
                onSaveScannedDocumentAsNote(
                    docName,
                    targetFolder,
                    pdfName,
                    pdfB64,
                    previewImgB64,
                    ocrText
                )
                showScannerDialog = false
            }
        )
    }

    contextSheetNote?.let { targetNote ->
        NoteContextBottomSheet(
            note = targetNote,
            strings = strings,
            onDismiss = { contextSheetNote = null },
            onSelectColor = { color -> onChangeNoteColor(targetNote.id, color) },
            onTogglePin = { onTogglePin(targetNote.id) },
            onDuplicate = { onDuplicateNote(targetNote.id) },
            onToggleArchive = { onToggleArchive(targetNote.id) },
            onDelete = { onDeleteNote(targetNote.id) }
        )
    }
}

@Composable
private fun CleanFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = Modifier
            .testTag(testTag)
            .clip(CircleShape)
            .background(
                if (selected) tokens.accentBrush else SolidColor(tokens.fieldFill)
            )
            .border(
                width = 1.dp,
                color = if (selected) tokens.accentStart else tokens.panelBorder,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) Color.White else tokens.textPrimary,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CleanGlassNoteRowCard(
    note: Note,
    isSelected: Boolean,
    hidePreview: Boolean,
    strings: AppStrings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val shape = RoundedCornerShape(18.dp)
    val accentBarColor = note.color.startColor
    val now = System.currentTimeMillis()
    val hasActiveReminder = note.reminderAt != null && !note.reminderFired && note.reminderAt > now

    val previewLine = remember(note.body, note.isChecklist, note.checklist, hidePreview) {
        if (hidePreview) {
            "••••••••••••"
        } else if (note.isChecklist && note.checklist.isNotEmpty()) {
            val done = note.checklist.count { it.isDone }
            "$done of ${note.checklist.size} done"
        } else {
            note.body.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
                .ifBlank { "No additional text" }
        }
    }

    val metaLine = remember(note.updatedAt, note.isChecklist, note.folder, note.reminderAt) {
        buildList {
            add(formatNoteTime(note.updatedAt))
            if (note.isChecklist) add("Checklist")
            if (note.folder.isNotBlank()) add(note.folder)
            if (hasActiveReminder && note.reminderAt != null) {
                add("Reminder ${ReminderScheduler.formatReminderTime(note.reminderAt)}")
            }
        }.joinToString(" · ")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.panelFill)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) tokens.accentStart else tokens.panelBorder,
                shape = shape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left vertical colored accent bar (matches screenshot)
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(48.dp)
                .clip(CircleShape)
                .background(accentBarColor)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = note.title.ifBlank { strings.untitledNote },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hasActiveReminder) {
                        LineIcon(
                            type = LineIconType.BELL,
                            tint = Color(0xFFF59E0B),
                            size = 13.dp
                        )
                    }
                    if (note.isPinned) {
                        LineIcon(
                            type = LineIconType.PIN,
                            tint = tokens.accentStart,
                            size = 14.dp
                        )
                    }
                }
            }

            Text(
                text = previewLine,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = tokens.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = metaLine,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                color = tokens.textSecondary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterDropdownSelect(
    currentLabel: String,
    options: List<Pair<String, () -> Unit>>,
    testTag: String
) {
    val tokens = LocalGlassTokens.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .testTag(testTag)
                .clip(RoundedCornerShape(10.dp))
                .background(tokens.fieldFill)
                .border(1.dp, tokens.panelBorder, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp),
                color = tokens.textPrimary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (optionLabel, action) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        action()
                        expanded = false
                    }
                )
            }
        }
    }
}
