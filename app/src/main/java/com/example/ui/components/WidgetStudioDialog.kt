package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Note
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.theme.LocalGlassTokens
import com.example.util.AudioHelper
import com.example.util.ReminderScheduler
import com.example.widget.NotesWidgetManager
import com.example.widget.WidgetConfig
import com.example.widget.WidgetDataStore
import com.example.widget.WidgetGlassTheme
import com.example.widget.WidgetSizePreset
import com.example.widget.WidgetSyncWorker
import com.example.widget.WidgetType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val WIDGET_TEXT_COLORS = listOf(
    "#FFFFFF" to "White",
    "#E2E8F0" to "Frost",
    "#A5F3FC" to "Cyan",
    "#FDE68A" to "Amber",
    "#C4B5FD" to "Violet",
    "#0F172A" to "Slate"
)

private val REFRESH_PRESETS = listOf(
    0 to "Instant",
    15 to "15m",
    30 to "30m",
    60 to "60m"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetStudioDialog(
    notes: List<Note>,
    availableFolders: List<String>,
    isKhmer: Boolean,
    onDismiss: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf(WidgetConfig.load(context, 0)) }
    val activeNotes = remember(notes) {
        notes.filter { !it.isTrashed && !it.isArchived }
    }

    LaunchedEffect(Unit) {
        config = WidgetDataStore.getStudioConfig(context)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassPanel(
            elevated = true,
            cornerRadius = 26.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .widthIn(max = 540.dp)
                .heightIn(max = 720.dp)
                .testTag("widget_studio_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassIconButton(
                            icon = LineIconType.LAYERS,
                            contentDesc = "Widgets Studio",
                            active = true,
                            sizeDp = 38.dp,
                            onClick = {}
                        )
                        Column {
                            Text(
                                text = if (isKhmer) "ស្ទូឌីយោ Widget អេក្រង់ដើម" else "Home Screen Widgets",
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isKhmer) "បន្ថែម និងប្ដូររូបរាង Widget ផ្ទាល់" else "Live preview, customize & pin to Home Screen",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textSecondary
                            )
                        }
                    }

                    GlassIconButton(
                        icon = LineIconType.CLOSE,
                        contentDesc = "Close",
                        sizeDp = 36.dp,
                        onClick = onDismiss,
                        testTagName = "widget_studio_close_button"
                    )
                }

                // 1. Live Interactive Preview Box
                WidgetLivePreviewBox(
                    config = config,
                    notes = activeNotes
                )

                // 2. Size Presets (Small 2x2, Medium 4x2, Large 4x3)
                SectionLabel(if (isKhmer) "ទំហំ Widget" else "Widget Size")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidgetSizePreset.entries.forEach { preset ->
                        val selected = config.sizePreset == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("widget_size_${preset.id}")
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) tokens.accentBrush else SolidColor(tokens.fieldFill)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) tokens.accentStart else tokens.panelBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { config = config.copy(sizePreset = preset) }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.White else tokens.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = preset.cellsLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White.copy(alpha = 0.85f) else tokens.textSecondary
                                )
                            }
                        }
                    }
                }

                // 3. Widget Type
                SectionLabel(if (isKhmer) "ប្រភេទ Widget" else "Widget Type")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidgetType.entries.forEach { type ->
                        val selected = config.widgetType == type
                        StudioChip(
                            label = type.title,
                            selected = selected,
                            onClick = { config = config.copy(widgetType = type) },
                            testTag = "widget_type_${type.id}"
                        )
                    }
                }

                // 4. Glass Theme Swatches
                SectionLabel(if (isKhmer) "រចនាបថផ្ទៃកញ្ចក់" else "Background Glass Style")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidgetGlassTheme.entries.forEach { theme ->
                        val selected = config.glassTheme == theme
                        val swatchColor = Color(theme.baseColorInt or (0xFF shl 24))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) tokens.accentStart.copy(alpha = 0.18f) else tokens.fieldFill)
                                .border(
                                    width = if (selected) 1.6.dp else 1.dp,
                                    color = if (selected) tokens.accentStart else tokens.panelBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    val defaultText = if (theme.isLightSurface) "#0F172A" else "#FFFFFF"
                                    config = config.copy(
                                        glassTheme = theme,
                                        transparencyPercent = theme.defaultAlphaPercent,
                                        textColorHex = defaultText
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(1.dp, Color(theme.borderColorInt), CircleShape)
                            )
                            Text(
                                text = theme.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // 5. Transparency Slider (35% - 100%)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionLabel(if (isKhmer) "ភាពថ្លាផ្ទៃខាងក្រោយ" else "Background Opacity")
                        Text(
                            text = "${config.transparencyPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.accentStart,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = config.transparencyPercent.toFloat(),
                        onValueChange = {
                            config = config.copy(transparencyPercent = it.roundToInt().coerceIn(35, 100))
                        },
                        valueRange = 35f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = tokens.accentStart,
                            activeTrackColor = tokens.accentStart
                        )
                    )
                }

                // 6. Text Size & Text Color
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionLabel(if (isKhmer) "ទំហំអក្សរ & ពណ៌អក្សរ" else "Text Size & Color")
                        Text(
                            text = "${config.textSizeSp}sp",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.accentStart,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = config.textSizeSp.toFloat(),
                        onValueChange = {
                            config = config.copy(textSizeSp = it.roundToInt().coerceIn(10, 18))
                        },
                        valueRange = 10f..18f,
                        colors = SliderDefaults.colors(
                            thumbColor = tokens.accentStart,
                            activeTrackColor = tokens.accentStart
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WIDGET_TEXT_COLORS.forEach { (hex, name) ->
                            val selected = config.textColorHex.equals(hex, ignoreCase = true)
                            val parsed = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) tokens.accentStart else tokens.panelBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { config = config.copy(textColorHex = hex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(tokens.accentStart)
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. Notes Selection (Folder & Specific Note)
                SectionLabel(if (isKhmer) "ជ្រើសរើសកំណត់ត្រាបង្ហាញ" else "Displayed Notes Filter")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioChip(
                        label = if (isKhmer) "ថតទាំងអស់" else "All Folders",
                        selected = config.filterFolder.isBlank(),
                        onClick = { config = config.copy(filterFolder = "") }
                    )
                    availableFolders.forEach { folder ->
                        StudioChip(
                            label = folder,
                            selected = config.filterFolder.equals(folder, ignoreCase = true),
                            onClick = { config = config.copy(filterFolder = folder) }
                        )
                    }
                }

                if (activeNotes.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioChip(
                            label = if (isKhmer) "កំណត់ត្រាថ្មីបំផុត (ស្វ័យប្រវត្ត)" else "Auto Latest Note",
                            selected = config.selectedNoteId.isBlank(),
                            onClick = { config = config.copy(selectedNoteId = "") }
                        )
                        activeNotes.take(8).forEach { note ->
                            StudioChip(
                                label = note.title.ifBlank { "Untitled" }.take(22),
                                selected = config.selectedNoteId == note.id,
                                onClick = { config = config.copy(selectedNoteId = note.id) }
                            )
                        }
                    }
                }

                // 8. Upcoming Reminders & Background Refresh
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.fieldFill)
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isKhmer) "បង្ហាញការរំលឹកជិតមកដល់" else "Show Upcoming Reminders",
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isKhmer) "បង្ហាញម៉ោងរោទ៍លើ Widget" else "Display alarm countdown pill on widget",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textSecondary
                        )
                    }
                    Switch(
                        checked = config.showUpcomingReminder,
                        onCheckedChange = { config = config.copy(showUpcomingReminder = it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = tokens.accentStart)
                    )
                }

                SectionLabel(if (isKhmer) "ឥរិយាបថធ្វើបច្ចុប្បន្នភាព" else "Refresh Behavior")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    REFRESH_PRESETS.forEach { (minutes, label) ->
                        Box(modifier = Modifier.weight(1f)) {
                            StudioChip(
                                label = label,
                                selected = config.refreshIntervalMinutes == minutes,
                                onClick = { config = config.copy(refreshIntervalMinutes = minutes) }
                            )
                        }
                    }
                }

                // 9. Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassActionPill(
                        label = if (isKhmer) "អនុវត្ត" else "Apply Settings",
                        icon = LineIconType.CHECK,
                        primary = false,
                        onClick = {
                            scope.launch {
                                WidgetDataStore.saveStudioConfig(context, config)
                                WidgetSyncWorker.schedulePeriodicSync(context, config.refreshIntervalMinutes)
                                NotesWidgetManager.notifyDataChanged(context)
                                onShowToast("Widget settings applied & synced")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("widget_apply_button")
                    )

                    GlassActionPill(
                        label = if (isKhmer) "បន្ថែមលើអេក្រង់ដើម" else "Add to Home Screen",
                        icon = LineIconType.PLUS,
                        primary = true,
                        onClick = {
                            scope.launch {
                                WidgetDataStore.saveStudioConfig(context, config)
                                WidgetSyncWorker.schedulePeriodicSync(context, config.refreshIntervalMinutes)
                                val pinned = NotesWidgetManager.pinWidget(
                                    context = context,
                                    sizePreset = config.sizePreset,
                                    widgetType = config.widgetType,
                                    studioConfig = config
                                )
                                NotesWidgetManager.notifyDataChanged(context)
                                if (pinned) {
                                    onShowToast("Follow Android Home Screen prompt to place widget")
                                } else {
                                    onShowToast("Settings saved! Long-press your Home Screen → Widgets → APPNOTE")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("widget_pin_home_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetLivePreviewBox(
    config: WidgetConfig,
    notes: List<Note>
) {
    val bgColor = remember(config.glassTheme, config.transparencyPercent) {
        Color(config.resolveBackgroundArgb())
    }
    val borderColor = remember(config.glassTheme) {
        Color(config.glassTheme.borderColorInt)
    }
    val textColor = remember(config.textColorHex) {
        try {
            Color(android.graphics.Color.parseColor(config.textColorHex))
        } catch (_: Exception) {
            Color.White
        }
    }

    val filtered = remember(notes, config.filterFolder, config.selectedNoteId) {
        val byFolder = if (config.filterFolder.isBlank()) notes else {
            notes.filter { it.folder.equals(config.filterFolder, ignoreCase = true) }
        }
        if (config.selectedNoteId.isNotBlank()) {
            val picked = byFolder.find { it.id == config.selectedNoteId } ?: notes.find { it.id == config.selectedNoteId }
            if (picked != null) listOf(picked) + byFolder.filterNot { it.id == picked.id } else byFolder
        } else {
            byFolder
        }
    }

    val checklistNote = remember(filtered) {
        filtered.firstOrNull { it.isChecklist && it.checklist.isNotEmpty() } ?: filtered.firstOrNull()
    }
    val nextReminder = remember(notes) {
        val now = System.currentTimeMillis()
        notes.filter { it.reminderAt != null && it.reminderAt > now }.minByOrNull { it.reminderAt!! }
    }

    val boxWidthFraction = when (config.sizePreset) {
        WidgetSizePreset.SMALL -> 0.56f
        WidgetSizePreset.MEDIUM, WidgetSizePreset.LARGE -> 1f
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(boxWidthFraction)
                .clip(RoundedCornerShape(22.dp))
                .background(bgColor)
                .border(1.2.dp, borderColor, RoundedCornerShape(22.dp))
                .padding(14.dp)
                .testTag("widget_live_preview_box"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (config.widgetType) {
                        WidgetType.QUICK_NOTE -> "Quick Note"
                        WidgetType.CHECKLIST -> checklistNote?.title?.ifBlank { "Checklist" } ?: "Checklist"
                        WidgetType.UPCOMING_REMINDER -> "Reminders"
                        WidgetType.STICKY_NOTE -> filtered.firstOrNull()?.title?.ifBlank { "Pinned Note" } ?: "Pinned Note"
                        WidgetType.NOTES_LIST -> "APPNOTE • ${config.sizePreset.cellsLabel}"
                    },
                    fontSize = (config.textSizeSp + 1).sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "+ Note",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    if (config.sizePreset != WidgetSizePreset.SMALL) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "⏰",
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            if (config.showUpcomingReminder) {
                val remLabel = if (nextReminder?.reminderAt != null) {
                    "⏰ ${nextReminder.title.ifBlank { "Reminder" }} • ${ReminderScheduler.formatReminderTime(nextReminder.reminderAt)}"
                } else {
                    "⏰ Tap bell to create a quick reminder"
                }
                Text(
                    text = remLabel,
                    fontSize = (config.textSizeSp - 2).coerceAtLeast(9).sp,
                    color = Color(0xFFFBBF24),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            when (config.widgetType) {
                WidgetType.CHECKLIST -> {
                    val items = checklistNote?.checklist?.take(if (config.sizePreset == WidgetSizePreset.SMALL) 2 else 3).orEmpty()
                    if (items.isEmpty()) {
                        Text(
                            text = "No checklist tasks yet",
                            fontSize = config.textSizeSp.sp,
                            color = textColor.copy(alpha = 0.72f)
                        )
                    } else {
                        items.forEach { item ->
                            Text(
                                text = "${if (item.isDone) "☑" else "☐"} ${item.text.ifBlank { "Task" }}",
                                fontSize = config.textSizeSp.sp,
                                color = textColor.copy(alpha = if (item.isDone) 0.55f else 0.92f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                WidgetType.QUICK_NOTE, WidgetType.STICKY_NOTE -> {
                    val n = filtered.firstOrNull()
                    Text(
                        text = n?.body?.replace("\n", " ")?.ifBlank { "Tap to edit or create note" }
                            ?: "Tap + Note to capture directly from home screen",
                        fontSize = config.textSizeSp.sp,
                        color = textColor.copy(alpha = 0.85f),
                        maxLines = if (config.sizePreset == WidgetSizePreset.LARGE) 4 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                else -> {
                    val maxRows = when (config.sizePreset) {
                        WidgetSizePreset.SMALL -> 1
                        WidgetSizePreset.MEDIUM -> 2
                        WidgetSizePreset.LARGE -> 3
                    }
                    val shown = filtered.take(maxRows)
                    if (shown.isEmpty()) {
                        Text(
                            text = "No active notes in selection",
                            fontSize = config.textSizeSp.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    } else {
                        shown.forEach { n ->
                            val sub = if (n.isChecklist) {
                                "${n.checklist.count { it.isDone }}/${n.checklist.size} done"
                            } else {
                                n.body.replace("\n", " ").ifBlank { "Open note" }
                            }
                            Text(
                                text = "• ${n.title.ifBlank { "Untitled" }} — $sub",
                                fontSize = config.textSizeSp.sp,
                                color = textColor.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val tokens = LocalGlassTokens.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = tokens.textPrimary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun StudioChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = Modifier
            .let { if (testTag != null) it.testTag(testTag) else it }
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) tokens.accentBrush else SolidColor(tokens.fieldFill))
            .border(
                width = 1.dp,
                color = if (selected) tokens.accentStart else tokens.panelBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else tokens.textPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickReminderDialog(
    isKhmer: Boolean,
    defaultSoundStyle: String,
    customSoundBase64: String,
    onDismiss: () -> Unit,
    onSaveReminder: (title: String, body: String, delayMinutes: Int, soundStyle: String) -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current
    var titleInput by remember { mutableStateOf("") }
    var bodyInput by remember { mutableStateOf("") }
    var selectedDelayMinutes by remember { mutableStateOf(15) }
    var selectedSoundStyle by remember {
        mutableStateOf(defaultSoundStyle.ifBlank { AudioHelper.RINGTONE_PRESETS.first().id })
    }

    val delayPresets = listOf(
        5 to "5 min",
        15 to "15 min",
        60 to "1 hour",
        180 to "3 hours",
        1440 to "Tomorrow"
    )

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            elevated = true,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .testTag("quick_reminder_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LineIcon(type = LineIconType.BELL, tint = Color(0xFFF59E0B), size = 20.dp)
                        Text(
                            text = if (isKhmer) "បង្កើតការរំលឹករហ័ស" else "Quick Reminder",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    GlassIconButton(
                        icon = LineIconType.CLOSE,
                        contentDesc = "Close",
                        sizeDp = 34.dp,
                        onClick = onDismiss
                    )
                }

                // Title Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.fieldFill)
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (titleInput.isEmpty()) {
                        Text(
                            text = if (isKhmer) "ចំណងជើងការរំលឹក..." else "Reminder title...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textSecondary
                        )
                    }
                    BasicTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(tokens.accentStart),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_reminder_title_input")
                    )
                }

                // Body Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.fieldFill)
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (bodyInput.isEmpty()) {
                        Text(
                            text = if (isKhmer) "កំណត់ចំណាំបន្ថែម (ជម្រើស)..." else "Optional note details...",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textSecondary
                        )
                    }
                    BasicTextField(
                        value = bodyInput,
                        onValueChange = { bodyInput = it },
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = tokens.textPrimary),
                        cursorBrush = SolidColor(tokens.accentStart),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Time Delay Presets
                SectionLabel(if (isKhmer) "រំលឹកនៅពេល" else "Remind In")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    delayPresets.forEach { (mins, label) ->
                        StudioChip(
                            label = label,
                            selected = selectedDelayMinutes == mins,
                            onClick = { selectedDelayMinutes = mins }
                        )
                    }
                }

                // Alarm & Reminder Sounds + Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel(if (isKhmer) "សំឡេងរោទ៍" else "Alarm Sound")
                    Text(
                        text = if (isKhmer) "▶ សាកស្តាប់" else "▶ Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.accentStart,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                AudioHelper.previewRingtoneStyle(
                                    context = context,
                                    styleId = selectedSoundStyle,
                                    customSoundBase64 = customSoundBase64,
                                    volumePercent = 85
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioHelper.RINGTONE_PRESETS.take(6).forEach { preset ->
                        StudioChip(
                            label = if (isKhmer) preset.labelKm else preset.labelEn,
                            selected = selectedSoundStyle == preset.id,
                            onClick = { selectedSoundStyle = preset.id }
                        )
                    }
                }

                GlassActionPill(
                    label = if (isKhmer) "បង្កើតការរំលឹក" else "Create Reminder",
                    icon = LineIconType.BELL,
                    primary = true,
                    onClick = {
                        onSaveReminder(titleInput, bodyInput, selectedDelayMinutes, selectedSoundStyle)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_reminder_save_button")
                )
            }
        }
    }
}
