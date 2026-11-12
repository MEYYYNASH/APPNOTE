package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import com.example.model.DateModeFilter
import com.example.model.DateRangeFilter
import com.example.model.FilterTab
import com.example.model.LanguageOption
import com.example.model.Note
import com.example.model.NoteColor
import com.example.model.SearchFilters
import com.example.model.Translations
import com.example.model.TypeFilter
import com.example.ui.components.AppGlassLogo
import com.example.ui.components.BottomDockSection
import com.example.ui.components.FloatingCapsuleBottomDock
import com.example.ui.components.GlassIconButton
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
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    var showQuickCaptureDialog by remember { mutableStateOf(false) }
    var contextSheetNote by remember { mutableStateOf<Note?>(null) }
    var searchFocused by remember { mutableStateOf(false) }

    val dayIndex = remember {
        val cal = Calendar.getInstance()
        (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
    }
    val dailyQuote = remember(language, dayIndex) {
        val q = if (language == LanguageOption.KHMER) {
            Translations.weekdayQuotesKm[dayIndex]
        } else {
            Translations.weekdayQuotesEn[dayIndex]
        }
        "“$q”"
    }

    val activeDockSection = when (activeTab) {
        FilterTab.ALL, FilterTab.PINNED -> BottomDockSection.HOME
        FilterTab.CHECKLISTS -> BottomDockSection.CHECKLISTS
        FilterTab.ARCHIVE, FilterTab.TRASH -> BottomDockSection.VAULT
    }

    val isOverlayOpen = showQuickCaptureDialog || contextSheetNote != null

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (isOverlayOpen) 26.dp else 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Full-Screen Header: Clean Vector Glass Logo + Title on left, Quick Capture (Bolt) + Theme Switch (Sun/Moon) on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppGlassLogo(
                        sizeDp = 42.dp,
                        cornerRadius = 14.dp,
                        modifier = Modifier.testTag("app_clean_logo_icon")
                    )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.appTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.6).sp
                        ),
                        color = tokens.textPrimary
                    )
                    Text(
                        text = dailyQuote,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp),
                        color = tokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .testTag("daily_weekday_quote")
                    )
                }

                // Quick Capture (Bolt) — unique icon & unique function
                GlassIconButton(
                    icon = LineIconType.QUICK_CAPTURE,
                    contentDesc = strings.quickCapture,
                    sizeDp = 42.dp,
                    onClick = { showQuickCaptureDialog = true },
                    testTagName = "quick_capture_fab"
                )

                // Quick Theme Switch (Sun / Moon) — unique icon & unique function
                GlassIconButton(
                    icon = if (tokens.isDark) LineIconType.SUN else LineIconType.MOON,
                    contentDesc = "Switch theme",
                    sizeDp = 42.dp,
                    onClick = onToggleTheme,
                    testTagName = "toggle_theme_button"
                )
            }

            // Full-width Glass Search Bar + Integrated Filter Toggle Icon inside right end
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .height(48.dp)
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
                        size = 18.dp
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = strings.searchPlaceholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = tokens.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
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
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable { onSearchQueryChange("") },
                            contentAlignment = Alignment.Center
                        ) {
                            LineIcon(
                                type = LineIconType.CLOSE,
                                tint = tokens.textSecondary,
                                size = 15.dp
                            )
                        }
                    }

                    // Filter sliders toggle inside search bar right edge
                    Box(
                        modifier = Modifier
                            .testTag("filter_chip_filters")
                            .size(34.dp)
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
                            size = 17.dp
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
                        .padding(horizontal = 20.dp, vertical = 4.dp),
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
                        .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterDropdownSelect(
                        currentLabel = if (language == LanguageOption.KHMER) searchFilters.type.labelKm else searchFilters.type.labelEn,
                        options = TypeFilter.entries.map {
                            (if (language == LanguageOption.KHMER) it.labelKm else it.labelEn) to {
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
                        currentLabel = if (language == LanguageOption.KHMER) searchFilters.dateRange.labelKm else searchFilters.dateRange.labelEn,
                        options = DateRangeFilter.entries.map {
                            (if (language == LanguageOption.KHMER) it.labelKm else it.labelEn) to {
                                onUpdateFilters(null, null, it, null)
                            }
                        },
                        testTag = "select_filter_date_range"
                    )

                    FilterDropdownSelect(
                        currentLabel = if (language == LanguageOption.KHMER) searchFilters.dateMode.labelKm else searchFilters.dateMode.labelEn,
                        options = DateModeFilter.entries.map {
                            (if (language == LanguageOption.KHMER) it.labelKm else it.labelEn) to {
                                onUpdateFilters(null, null, null, it)
                            }
                        },
                        testTag = "select_filter_date_mode"
                    )
                }
            }

            // Contextual Secondary Segment Strip (only non-duplicate sub-filters for the current dock section!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeDockSection == BottomDockSection.VAULT) {
                    FilterChipPill(
                        label = strings.tabArchive,
                        selected = activeTab == FilterTab.ARCHIVE,
                        testTag = "subchip_archive",
                        onClick = { onSelectTab(FilterTab.ARCHIVE) }
                    )
                    FilterChipPill(
                        label = strings.tabTrash,
                        selected = activeTab == FilterTab.TRASH,
                        testTag = "filter_chip_trash",
                        onClick = { onSelectTab(FilterTab.TRASH) }
                    )
                } else if (activeDockSection == BottomDockSection.HOME) {
                    FilterChipPill(
                        label = strings.tabAll,
                        selected = activeTab == FilterTab.ALL,
                        testTag = "subchip_all",
                        onClick = { onSelectTab(FilterTab.ALL) }
                    )
                    FilterChipPill(
                        label = strings.tabPinned,
                        selected = activeTab == FilterTab.PINNED,
                        testTag = "filter_chip_pinned",
                        onClick = { onSelectTab(FilterTab.PINNED) }
                    )

                    // Recent 3 notes compact strip inline on Home
                    if (activeTab == FilterTab.ALL && searchQuery.isBlank() && recentNotes.isNotEmpty()) {
                        recentNotes.forEach { recent ->
                            Box(
                                modifier = Modifier
                                    .testTag("recent_note_${recent.id}")
                                    .clip(CircleShape)
                                    .background(tokens.panelFill)
                                    .border(1.dp, tokens.panelBorder, CircleShape)
                                    .clickable { onSelectNote(recent.id) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = recent.title.ifBlank { strings.untitledNote }.take(16),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp),
                                    color = tokens.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Full-Screen Scrollable Notes List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("notes_list_column"),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 4.dp,
                    bottom = 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (notes.isEmpty()) {
                    item {
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
                }

                itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
                    NoteGlassCard(
                        note = note,
                        index = index,
                        isSelected = note.id == selectedNoteId,
                        isTrashTab = activeTab == FilterTab.TRASH,
                        hidePreviews = hidePreviews,
                        strings = strings,
                        onClick = { onSelectNote(note.id) },
                        onLongPress = { contextSheetNote = note }
                    )
                }
            }
        }

            // Sleek Floating Capsule Bottom Navigation Bar + Separate Circular "+" Action Button
            // Hidden when NoteContextBottomSheet is open so the dock never bleeds behind Delete!
            FloatingCapsuleBottomDock(
                activeSection = activeDockSection,
                homeLabel = if (language == LanguageOption.KHMER) "ទំព័រដើម" else "Home",
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
                onNewNote = onCreateNote,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .alpha(if (contextSheetNote != null) 0f else 1f)
            )
        }
    }

    if (showQuickCaptureDialog) {
        QuickCaptureDialog(
            strings = strings,
            onDismiss = { showQuickCaptureDialog = false },
            onSaveCapture = onQuickCapture
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteGlassCard(
    note: Note,
    index: Int,
    isSelected: Boolean,
    isTrashTab: Boolean,
    hidePreviews: Boolean,
    strings: AppStrings,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val entranceProgress = remember(note.id) {
        Animatable(if (tokens.animationsEnabled) 0f else 1f)
    }

    LaunchedEffect(note.id, tokens.animationsEnabled) {
        if (tokens.animationsEnabled && entranceProgress.value < 1f) {
            kotlinx.coroutines.delay((index.coerceAtMost(8) * 45L))
            entranceProgress.animateTo(1f, animationSpec = tween(350))
        } else {
            entranceProgress.snapTo(1f)
        }
    }

    val previewText = remember(note, strings) {
        if (note.isChecklist) {
            if (note.checklist.isNotEmpty()) {
                val doneCount = note.checklist.count { it.isDone }
                strings.doneOfLabel(doneCount, note.checklist.size)
            } else {
                "Empty checklist"
            }
        } else {
            note.body.replace(Regex("\\s+"), " ").trim().take(90).ifBlank { "No text yet" }
        }
    }

    val offsetY = ((1f - entranceProgress.value) * 14f).roundToInt()
    val cardScale = 0.98f + 0.02f * entranceProgress.value
    val shape = RoundedCornerShape(20.dp)
    val vertPad = if (tokens.compactMode) 10.dp else 14.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY) }
            .alpha(entranceProgress.value)
            .scale(cardScale)
            .clip(shape)
            .background(tokens.panelFill)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) tokens.accentStart else tokens.panelBorder,
                shape = shape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .testTag("note_card_${note.id}")
    ) {
        // Left edge 4dp colored indicator bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(vertical = 14.dp)
                .width(4.dp)
                .height(if (tokens.compactMode) 28.dp else 40.dp)
                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .background(note.color.dotColor)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = vertPad, bottom = vertPad),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = note.title.ifBlank { strings.untitledNote },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (note.reminderAt != null) {
                    LineIcon(
                        type = LineIconType.BELL,
                        tint = tokens.accentStart,
                        size = 14.dp
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

            Text(
                text = previewText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                color = tokens.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (hidePreviews) Modifier.blur(5.dp) else Modifier
            )

            val metaLine = if (isTrashTab) {
                strings.trashBanner
            } else {
                buildString {
                    append(formatNoteTime(note.updatedAt))
                    if (note.isChecklist) {
                        append(" · ")
                        append(strings.checklistTag)
                    }
                    if (note.reminderAt != null) {
                        append(" · ")
                        append(ReminderScheduler.formatReminderTime(note.reminderAt))
                    }
                    if (note.folder.isNotBlank()) {
                        append(" · ")
                        append(note.folder)
                    }
                }
            }
            Text(
                text = metaLine,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = tokens.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val contentColor = if (selected) Color.White else tokens.textSecondary

    Box(
        modifier = Modifier
            .testTag(testTag)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(tokens.accentBrush)
                } else {
                    Modifier.background(tokens.fieldFill)
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else tokens.panelBorder,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
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
