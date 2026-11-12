package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppStrings
import com.example.model.AttachmentType
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
import com.example.ui.components.InteractiveGlassWidgetsStrip
import com.example.ui.components.NeonBentoBottomBar
import com.example.ui.components.NeonBentoNoteCard
import com.example.ui.components.NeonFilterPillsRow
import com.example.ui.components.NoteContextBottomSheet
import com.example.ui.components.QuickCaptureDialog
import com.example.ui.components.formatNoteTime
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.theme.LocalGlassTokens
import com.example.util.ReminderScheduler
import java.util.Calendar
import kotlin.math.roundToInt

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
    onShowToast: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val isKhmer = language == LanguageOption.KHMER
    var showQuickCaptureDialog by remember { mutableStateOf(false) }
    var showFoldersDialog by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var contextSheetNote by remember { mutableStateOf<Note?>(null) }
    var searchFocused by remember { mutableStateOf(false) }

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
                // Full-Screen Header: Logo + Title on left, Folders + Scanner + Quick Capture + Theme Switch + Settings on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppGlassLogo(
                        sizeDp = 40.dp,
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

                    // Suite 1: Folders Manager Button
                    GlassIconButton(
                        icon = LineIconType.FOLDER,
                        contentDesc = "Folders Manager",
                        sizeDp = 38.dp,
                        onClick = { showFoldersDialog = true },
                        testTagName = "open_folders_manager_button"
                    )

                    // Suite 2: Document Scanner Button
                    GlassIconButton(
                        icon = LineIconType.CAMERA,
                        contentDesc = "Document Scanner",
                        sizeDp = 38.dp,
                        onClick = { showScannerDialog = true },
                        testTagName = "open_document_scanner_button"
                    )

                    // Quick Capture (Bolt)
                    GlassIconButton(
                        icon = LineIconType.QUICK_CAPTURE,
                        contentDesc = strings.quickCapture,
                        sizeDp = 38.dp,
                        onClick = { showQuickCaptureDialog = true },
                        testTagName = "quick_capture_fab"
                    )

                    // Quick Theme Switch (Sun / Moon)
                    GlassIconButton(
                        icon = if (tokens.isDark) LineIconType.SUN else LineIconType.MOON,
                        contentDesc = "Switch theme",
                        sizeDp = 38.dp,
                        onClick = onToggleTheme,
                        testTagName = "toggle_theme_button"
                    )

                    // Settings Button
                    GlassIconButton(
                        icon = LineIconType.SETTINGS,
                        contentDesc = strings.settingsTitle,
                        sizeDp = 38.dp,
                        onClick = onOpenSettings,
                        testTagName = "header_settings_button"
                    )
                }

                // Full-width Glass Search Bar + Integrated Filter Toggle Icon inside right end
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(tokens.fieldFill)
                        .border(
                            width = if (searchFocused) 2.dp else 1.dp,
                            color = if (searchFocused) tokens.accentStart else tokens.panelBorder,
                            shape = RoundedCornerShape(16.dp)
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

                        // Filter sliders toggle inside search bar right edge
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
                            .padding(horizontal = 16.dp, vertical = 4.dp),
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

                // Expandable Advanced Filter Selects Row (Type, Folder, Date, Modified/Created)
                AnimatedVisibility(visible = showFilterRow) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
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

                // Neon Filter Pills Row matching Screenshot 4: [All] [Pinned] [Favorites] [Folders] [More]
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    NeonFilterPillsRow(
                        activeTab = activeTab,
                        isKhmer = isKhmer,
                        onSelectTab = { tab ->
                            if (tab == FilterTab.FOLDERS) {
                                showFoldersDialog = true
                            }
                            onSelectTab(tab)
                        },
                        onOpenFoldersModal = { showFoldersDialog = true },
                        onOpenDocumentScanner = { showScannerDialog = true },
                        onToggleAdvancedFilters = onToggleFilterRow
                    )
                }

                // Active Folder Filter Chip if filtering by a specific folder
                if (searchFilters.folder.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "📁 Folder: ${searchFilters.folder}",
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.accentStart,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "(${notes.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(tokens.fieldFill)
                                .clickable { onUpdateFilters(null, "", null, null) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isKhmer) "បង្ហាញទាំងអស់" else "Clear Folder Filter",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textSecondary
                            )
                        }
                    }
                }

                // Scrollable Home Content: 5 Interactive Glass Widgets Strip + 2-Column Neon Bento Note Cards Grid
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notes_list_column"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 148.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Suite 5A: 5 Interactive Glass Widgets Strip at top of Home
                    item(key = "interactive_widgets_strip") {
                        InteractiveGlassWidgetsStrip(
                            notes = notes,
                            isKhmer = isKhmer,
                            onQuickNoteClick = { showQuickCaptureDialog = true },
                            onUpcomingReminderClick = { reminderNote ->
                                if (reminderNote != null) {
                                    onSelectNote(reminderNote.id)
                                } else {
                                    onCreateNote()
                                }
                            },
                            onStatisticsClick = {
                                onShowToast("Total active notes: ${notes.count { !it.isTrashed }}")
                            },
                            onPomodoroComplete = {
                                onShowToast("Pomodoro 25:00 Focus session completed!")
                            }
                        )
                    }

                    // Trash empty banner when viewing Trash tab
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
                                    .padding(horizontal = 16.dp, vertical = 36.dp)
                            )
                        }
                    } else {
                        // Suite 5B: 2-Column Neon Bento Note Cards Grid matching Screenshot 4
                        val chunkedPairs = notes.chunked(2)
                        itemsIndexed(
                            items = chunkedPairs,
                            key = { _, pair -> pair.joinToString("_") { it.id } }
                        ) { _, rowNotes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowNotes.forEach { note ->
                                    NeonBentoNoteCard(
                                        note = note,
                                        isSelected = note.id == selectedNoteId,
                                        hidePreview = hidePreviews,
                                        availableFolders = allFolders,
                                        onClick = { onSelectNote(note.id) },
                                        onTogglePin = { onTogglePin(note.id) },
                                        onToggleFavorite = { onToggleFavorite(note.id) },
                                        onToggleChecklistItem = { itemId ->
                                            onToggleChecklistItem(note.id, itemId)
                                        },
                                        onMoveToFolder = { targetFolder ->
                                            onMoveNoteToFolder(note.id, targetFolder)
                                            onShowToast("Moved to $targetFolder")
                                        },
                                        onCopyToFolder = { targetFolder ->
                                            onCopyNoteToFolder(note.id, targetFolder)
                                            onShowToast("Copied to $targetFolder")
                                        },
                                        onDuplicate = { onDuplicateNote(note.id) },
                                        onDelete = { onDeleteNote(note.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("note_card_${note.id}")
                                    )
                                }
                                if (rowNotes.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Dock Area: NeonBentoBottomBar ([📁 Personal ⌄ | 📊 N Notes] + [+ New Note])
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .alpha(if (contextSheetNote != null) 0f else 1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NeonBentoBottomBar(
                    activeFolder = searchFilters.folder.ifBlank { "Personal" },
                    totalNotesCount = notes.size,
                    availableFolders = allFolders,
                    isKhmer = isKhmer,
                    onSelectFolder = { chosenFolder ->
                        onUpdateFilters(null, chosenFolder, null, null)
                    },
                    onOpenFoldersManager = { showFoldersDialog = true },
                    onNewNoteClick = onCreateNote
                )

                // Keep compact navigation dock accessible for tests and quick section switching
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

    // Suite 1: Full Folders Manager Modal
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

    // Suite 2: Full Multi-Page Document Scanner Modal
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
