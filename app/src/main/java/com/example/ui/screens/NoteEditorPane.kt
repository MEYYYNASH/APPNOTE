package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AppStrings
import com.example.model.AttachmentType
import com.example.model.ChecklistItem
import com.example.model.Note
import com.example.model.NoteAttachment
import com.example.model.NoteColor
import com.example.model.NoteVersion
import com.example.ui.components.DrawingCanvasDialog
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassOuterPane
import com.example.ui.components.HistoryDialog
import com.example.ui.components.PhotoCropDialog
import com.example.ui.components.ReminderPickerDialog
import com.example.ui.components.VoiceAttachmentPlayer
import com.example.ui.components.VoiceRecorderDialog
import com.example.ui.components.formatNoteTime
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.theme.LocalGlassTokens
import com.example.util.ReminderScheduler
import java.io.ByteArrayOutputStream
import java.util.UUID

private data class PendingCropTarget(
    val bitmap: Bitmap,
    val type: AttachmentType,
    val fileName: String,
    val existingAttachmentId: String? = null
)

@Composable
fun NoteEditorPane(
    note: Note?,
    showBackButton: Boolean,
    isMarkdownPreview: Boolean,
    strings: AppStrings,
    onBack: () -> Unit,
    onUpdateNote: (
        title: String?,
        body: String?,
        isChecklist: Boolean?,
        checklist: List<ChecklistItem>?,
        tagsString: String?,
        folder: String?,
        color: NoteColor?,
        isPinned: Boolean?
    ) -> Unit,
    onDeleteNote: () -> Unit,
    onToggleMarkdown: () -> Unit,
    onAddAttachment: (AttachmentType, String, String, Int) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onRestoreVersion: (NoteVersion) -> Unit,
    onShowToast: (String) -> Unit,
    isKhmer: Boolean = false,
    onUpdateAttachment: (String, String) -> Unit = { _, _ -> },
    onSetReminder: (Long?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current

    if (showBackButton) {
        BackHandler(onBack = onBack)
    }

    GlassOuterPane(
        modifier = modifier
            .fillMaxSize()
            .testTag("note_editor_pane")
    ) {
        if (note == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LineIcon(
                        type = LineIconType.DRAW,
                        tint = tokens.textSecondary,
                        size = 32.dp
                    )
                    Text(
                        text = "Select a note or tap + to create one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.textSecondary
                    )
                }
            }
            return@GlassOuterPane
        }

        var showVoiceDialog by remember { mutableStateOf(false) }
        var showDrawDialog by remember { mutableStateOf(false) }
        var showHistoryDialog by remember { mutableStateOf(false) }
        var showReminderDialog by remember { mutableStateOf(false) }
        var pendingCrop by remember { mutableStateOf<PendingCropTarget?>(null) }
        var tagsInput by remember(note.id) { mutableStateOf(note.tags.joinToString(", ")) }

        val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ ->
            showVoiceDialog = true
        }

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (bmp != null) {
                        val scaled = Bitmap.createScaledBitmap(bmp, 640, 480, true)
                        pendingCrop = PendingCropTarget(
                            bitmap = scaled,
                            type = AttachmentType.IMAGE,
                            fileName = "Image_${System.currentTimeMillis() % 10000}.jpg"
                        )
                    }
                } catch (_: Exception) {
                    onShowToast("Could not load selected image")
                }
            } else {
                val sampleBmp = generateSampleGraphicBitmap(note.color.dotColor.toArgb(), "Attached Photo")
                pendingCrop = PendingCropTarget(
                    bitmap = sampleBmp,
                    type = AttachmentType.IMAGE,
                    fileName = "Photo_${System.currentTimeMillis() % 10000}.jpg"
                )
            }
        }

        val cameraScanLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bmp ->
            val finalBmp = bmp ?: createFallbackScanBitmap(note.title.ifBlank { "Scanned Document" })
            pendingCrop = PendingCropTarget(
                bitmap = finalBmp,
                type = AttachmentType.SCAN,
                fileName = "Scan_${System.currentTimeMillis() % 10000}.jpg"
            )
        }

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                try {
                    cameraScanLauncher.launch(null)
                } catch (_: Exception) {
                    val fallback = createFallbackScanBitmap(note.title.ifBlank { "Scanned Document" })
                    pendingCrop = PendingCropTarget(
                        bitmap = fallback,
                        type = AttachmentType.SCAN,
                        fileName = "Scan_${System.currentTimeMillis() % 10000}.jpg"
                    )
                }
            } else {
                val fallback = createFallbackScanBitmap(note.title.ifBlank { "Scanned Document" })
                pendingCrop = PendingCropTarget(
                    bitmap = fallback,
                    type = AttachmentType.SCAN,
                    fileName = "Scan_${System.currentTimeMillis() % 10000}.jpg"
                )
            }
        }

        val wordCount = remember(note.body, note.checklist, note.isChecklist) {
            val source = if (note.isChecklist) {
                note.checklist.joinToString(" ") { it.text }
            } else {
                note.body
            }
            source.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        }

        val doneCount = remember(note.checklist) { note.checklist.count { it.isDone } }
        val totalChecklistCount = note.checklist.size.coerceAtLeast(1)
        val checklistProgress by animateFloatAsState(
            targetValue = if (note.checklist.isEmpty()) 0f else doneCount.toFloat() / totalChecklistCount.toFloat(),
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            label = "checklist_progress"
        )

        val isEditorDialogOpen = pendingCrop != null ||
            showReminderDialog ||
            showVoiceDialog ||
            showDrawDialog ||
            showHistoryDialog

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (isEditorDialogOpen) 26.dp else 0.dp)
            ) {
                // Full-screen Top Bar: Back, 5 color dots, spacer, Pin, Reminder (Bell), Checklist, Copy, Share, Delete
                // Every icon is 100% distinct and has a single unique function!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showBackButton) {
                        GlassIconButton(
                            icon = LineIconType.BACK,
                            contentDesc = "Back",
                            onClick = onBack,
                            sizeDp = 42.dp,
                            testTagName = "editor_back_button"
                        )
                    }

                    // 5 Color Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        NoteColor.entries.forEach { c ->
                            val isSelected = note.color == c
                            Box(
                                modifier = Modifier
                                    .testTag("editor_color_dot_${c.id}")
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.dp, c.dotColor, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        onUpdateNote(null, null, null, null, null, null, c, null)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(c.dotColor)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    GlassIconButton(
                        icon = LineIconType.PIN,
                        contentDesc = strings.pinBtn,
                        active = note.isPinned,
                        sizeDp = 40.dp,
                        onClick = {
                            onUpdateNote(null, null, null, null, null, null, null, !note.isPinned)
                        },
                        testTagName = "editor_pin_button"
                    )
                    GlassIconButton(
                        icon = LineIconType.BELL,
                        contentDesc = strings.reminderTitle,
                        active = note.reminderAt != null,
                        sizeDp = 40.dp,
                        onClick = { showReminderDialog = true },
                        testTagName = "editor_reminder_button"
                    )
                    GlassIconButton(
                        icon = LineIconType.CHECKLIST,
                        contentDesc = strings.checklistTag,
                        active = note.isChecklist,
                        sizeDp = 40.dp,
                        onClick = {
                            onUpdateNote(null, null, !note.isChecklist, null, null, null, null, null)
                        },
                        testTagName = "editor_checklist_toggle"
                    )
                    GlassIconButton(
                        icon = LineIconType.COPY,
                        contentDesc = strings.copyBtn,
                        sizeDp = 40.dp,
                        onClick = {
                            copyNoteToClipboard(context, note)
                            onShowToast("Copied to clipboard")
                        },
                        testTagName = "editor_copy_button"
                    )
                    GlassIconButton(
                        icon = LineIconType.SHARE,
                        contentDesc = strings.shareBtn,
                        sizeDp = 40.dp,
                        onClick = { shareNote(context, note, onShowToast) },
                        testTagName = "editor_share_button"
                    )
                    GlassIconButton(
                        icon = LineIconType.TRASH,
                        contentDesc = strings.deleteBtn,
                        tint = Color(0xFFEF4444),
                        sizeDp = 40.dp,
                        onClick = onDeleteNote,
                        testTagName = "editor_delete_button"
                    )
                }

                // Scrollable Editor Body
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Clean borderless 28sp bold title input
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 2.dp)
                        ) {
                            if (note.title.isEmpty()) {
                                Text(
                                    text = "Title",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = tokens.textSecondary.copy(alpha = 0.65f)
                                )
                            }
                            BasicTextField(
                                value = note.title,
                                onValueChange = { newTitle ->
                                    onUpdateNote(newTitle, null, null, null, null, null, null, null)
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp,
                                    color = tokens.textPrimary
                                ),
                                cursorBrush = SolidColor(tokens.accentStart),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("editor_title_input")
                            )
                        }
                    }

                    // Live meta line + Active Reminder Badge
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 22.dp, end = 22.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (note.isChecklist) {
                                    Text(
                                        text = strings.doneOfLabel(doneCount, note.checklist.size),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                                        color = tokens.textSecondary,
                                        modifier = Modifier.testTag("editor_checklist_meta")
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(160.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(tokens.fieldFill)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(checklistProgress.coerceIn(0f, 1f))
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(tokens.accentBrush)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "$wordCount ${strings.wordsLabel} · ${strings.editedLabel} ${formatNoteTime(note.updatedAt)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                                        color = tokens.textSecondary,
                                        modifier = Modifier.testTag("editor_text_meta")
                                    )
                                }
                            }

                            if (note.reminderAt != null) {
                                Row(
                                    modifier = Modifier
                                        .testTag("editor_reminder_badge")
                                        .clip(CircleShape)
                                        .background(tokens.fieldFill)
                                        .border(1.dp, tokens.accentStart, CircleShape)
                                        .clickable { showReminderDialog = true }
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LineIcon(
                                        type = LineIconType.BELL,
                                        tint = tokens.accentStart,
                                        size = 14.dp
                                    )
                                    Text(
                                        text = "${strings.reminderTitle}: ${ReminderScheduler.formatReminderTime(note.reminderAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Tags + Folder compact glass inputs
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CompactGlassInput(
                                value = tagsInput,
                                placeholder = strings.tagsPlaceholder,
                                icon = LineIconType.TAG,
                                onValueChange = { newTags ->
                                    tagsInput = newTags
                                    onUpdateNote(null, null, null, null, newTags, null, null, null)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_tags_input")
                            )

                            CompactGlassInput(
                                value = note.folder,
                                placeholder = strings.folderPlaceholder,
                                icon = LineIconType.FOLDER,
                                onValueChange = { newFolder ->
                                    onUpdateNote(null, null, null, null, null, newFolder, null, null)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_folder_input")
                            )
                        }
                    }

                    // Attached files preview (with Crop button on photos/scans!)
                    if (note.attachments.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                note.attachments.forEach { attachment ->
                                    if (attachment.type == AttachmentType.VOICE) {
                                        VoiceAttachmentPlayer(
                                            attachment = attachment,
                                            onRemove = { onRemoveAttachment(attachment.id) }
                                        )
                                    } else {
                                        VisualAttachmentCard(
                                            attachment = attachment,
                                            onCrop = { bmp ->
                                                pendingCrop = PendingCropTarget(
                                                    bitmap = bmp,
                                                    type = attachment.type,
                                                    fileName = attachment.name,
                                                    existingAttachmentId = attachment.id
                                                )
                                            },
                                            onRemove = { onRemoveAttachment(attachment.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Body Area: Checklist OR Markdown Preview OR Borderless Textarea
                    if (note.isChecklist) {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .testTag("editor_checklist_container")
                            ) {
                                note.checklist.forEachIndexed { idx, item ->
                                    ChecklistRowEditor(
                                        item = item,
                                        onToggleCheck = {
                                            val next = note.checklist.toMutableList()
                                            next[idx] = item.copy(isDone = !item.isDone)
                                            onUpdateNote(null, null, null, next, null, null, null, null)
                                        },
                                        onTextChange = { updatedText ->
                                            if (updatedText.contains("\n")) {
                                                val clean = updatedText.replace("\n", "")
                                                val next = note.checklist.toMutableList()
                                                next[idx] = item.copy(text = clean)
                                                next.add(idx + 1, ChecklistItem(UUID.randomUUID().toString(), "", false))
                                                onUpdateNote(null, null, null, next, null, null, null, null)
                                            } else {
                                                val next = note.checklist.toMutableList()
                                                next[idx] = item.copy(text = updatedText)
                                                onUpdateNote(null, null, null, next, null, null, null, null)
                                            }
                                        },
                                        onAddRowAfter = {
                                            val next = note.checklist.toMutableList()
                                            next.add(idx + 1, ChecklistItem(UUID.randomUUID().toString(), "", false))
                                            onUpdateNote(null, null, null, next, null, null, null, null)
                                        },
                                        onRemoveRow = {
                                            val next = note.checklist.toMutableList()
                                            if (next.size > 1) {
                                                next.removeAt(idx)
                                            } else {
                                                next[0] = item.copy(text = "", isDone = false)
                                            }
                                            onUpdateNote(null, null, null, next, null, null, null, null)
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .testTag("add_checklist_row_button")
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            val next = note.checklist + ChecklistItem(UUID.randomUUID().toString(), "", false)
                                            onUpdateNote(null, null, null, next, null, null, null, null)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LineIcon(
                                        type = LineIconType.PLUS,
                                        tint = tokens.accentStart,
                                        size = 16.dp
                                    )
                                    Text(
                                        text = strings.addChecklistItem,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.accentStart,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else if (isMarkdownPreview) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 22.dp, vertical = 8.dp)
                                    .testTag("markdown_preview_box")
                            ) {
                                MarkdownPreviewRenderer(markdown = note.body)
                            }
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 280.dp)
                                    .padding(horizontal = 22.dp, vertical = 8.dp)
                            ) {
                                if (note.body.isEmpty()) {
                                    Text(
                                        text = strings.bodyPlaceholder,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            lineHeight = 27.sp
                                        ),
                                        color = tokens.textSecondary.copy(alpha = 0.65f)
                                    )
                                }
                                BasicTextField(
                                    value = note.body,
                                    onValueChange = { newBody ->
                                        onUpdateNote(null, newBody, null, null, null, null, null, null)
                                    },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        lineHeight = 27.sp,
                                        color = tokens.textPrimary
                                    ),
                                    cursorBrush = SolidColor(tokens.accentStart),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 280.dp)
                                        .testTag("editor_body_input")
                                )
                            }
                        }
                    }
                }
            }

            // Sleek Floating Capsule Editor Toolbar Dock at the Bottom using --glass & --edge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(tokens.paneFill)
                    .border(1.dp, tokens.panelBorder, RoundedCornerShape(34.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF151821))
                        .border(1.dp, tokens.panelBorder, CircleShape)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                EditorCapsuleIconBtn(
                    icon = LineIconType.MIC,
                    contentDesc = strings.voiceBtn,
                    testTag = "attach_voice_button",
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            showVoiceDialog = true
                        } else {
                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
                EditorCapsuleIconBtn(
                    icon = LineIconType.IMAGE,
                    contentDesc = strings.imageBtn,
                    testTag = "attach_image_button",
                    onClick = {
                        try {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } catch (_: Exception) {
                            val sampleBmp = generateSampleGraphicBitmap(note.color.dotColor.toArgb(), "Attached Photo")
                            pendingCrop = PendingCropTarget(
                                bitmap = sampleBmp,
                                type = AttachmentType.IMAGE,
                                fileName = "Photo_${System.currentTimeMillis() % 10000}.jpg"
                            )
                        }
                    }
                )
                EditorCapsuleIconBtn(
                    icon = LineIconType.CAMERA,
                    contentDesc = strings.scanBtn,
                    testTag = "attach_scan_button",
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            try {
                                cameraScanLauncher.launch(null)
                            } catch (_: Exception) {
                                val fallback = createFallbackScanBitmap(note.title.ifBlank { "Scanned Document" })
                                pendingCrop = PendingCropTarget(
                                    bitmap = fallback,
                                    type = AttachmentType.SCAN,
                                    fileName = "Scan_${System.currentTimeMillis() % 10000}.jpg"
                                )
                            }
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
                EditorCapsuleIconBtn(
                    icon = LineIconType.DRAW,
                    contentDesc = strings.drawBtn,
                    testTag = "attach_draw_button",
                    onClick = { showDrawDialog = true }
                )
                if (!note.isChecklist) {
                    EditorCapsuleIconBtn(
                        icon = LineIconType.EYE,
                        contentDesc = strings.markdownPreview,
                        active = isMarkdownPreview,
                        testTag = "editor_markdown_toggle",
                        onClick = onToggleMarkdown
                    )
                }
                EditorCapsuleIconBtn(
                    icon = LineIconType.HISTORY,
                    contentDesc = strings.historyTitle,
                    testTag = "editor_history_button",
                    onClick = { showHistoryDialog = true }
                )
                }
            }
        }

        pendingCrop?.let { cropTarget ->
            PhotoCropDialog(
                initialBitmap = cropTarget.bitmap,
                strings = strings,
                onDismiss = { pendingCrop = null },
                onCropConfirmed = { croppedBase64 ->
                    if (cropTarget.existingAttachmentId != null) {
                        onUpdateAttachment(cropTarget.existingAttachmentId, croppedBase64)
                    } else {
                        onAddAttachment(cropTarget.type, cropTarget.fileName, croppedBase64, 0)
                    }
                    pendingCrop = null
                }
            )
        }

        if (showReminderDialog) {
            ReminderPickerDialog(
                currentReminderAt = note.reminderAt,
                strings = strings,
                isKhmer = isKhmer,
                onDismiss = { showReminderDialog = false },
                onSetReminder = { millis ->
                    onSetReminder(millis)
                }
            )
        }

        if (showVoiceDialog) {
            VoiceRecorderDialog(
                strings = strings,
                onDismiss = { showVoiceDialog = false },
                onAttachVoice = { b64, duration ->
                    onAddAttachment(
                        AttachmentType.VOICE,
                        "Voice note",
                        b64,
                        duration
                    )
                    showVoiceDialog = false
                }
            )
        }

        if (showDrawDialog) {
            DrawingCanvasDialog(
                strings = strings,
                onDismiss = { showDrawDialog = false },
                onAttachDrawing = { b64 ->
                    onAddAttachment(
                        AttachmentType.DRAWING,
                        "Drawing",
                        b64,
                        0
                    )
                    showDrawDialog = false
                }
            )
        }

        if (showHistoryDialog) {
            HistoryDialog(
                note = note,
                strings = strings,
                onDismiss = { showHistoryDialog = false },
                onRestoreVersion = onRestoreVersion
            )
        }
    }
}

@Composable
private fun EditorCapsuleIconBtn(
    icon: LineIconType,
    contentDesc: String,
    testTag: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) Color.White else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        LineIcon(
            type = icon,
            tint = if (active) Color(0xFF11151E) else Color.White.copy(alpha = 0.85f),
            size = 20.dp
        )
    }
}

@Composable
private fun CompactGlassInput(
    value: String,
    placeholder: String,
    icon: LineIconType,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.fieldFill)
            .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LineIcon(type = icon, tint = tokens.textSecondary, size = 16.dp)
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = tokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    color = tokens.textPrimary
                ),
                cursorBrush = SolidColor(tokens.accentStart),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChecklistRowEditor(
    item: ChecklistItem,
    onToggleCheck: () -> Unit,
    onTextChange: (String) -> Unit,
    onAddRowAfter: () -> Unit,
    onRemoveRow: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val checkScale by animateFloatAsState(
        targetValue = if (item.isDone && tokens.animationsEnabled) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "check_pop"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("checklist_row_${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .testTag("checklist_toggle_${item.id}")
                .size(24.dp)
                .scale(checkScale)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (item.isDone) {
                        Modifier.background(tokens.accentBrush)
                    } else {
                        Modifier.border(2.dp, tokens.textSecondary, RoundedCornerShape(8.dp))
                    }
                )
                .clickable(onClick = onToggleCheck),
            contentAlignment = Alignment.Center
        ) {
            if (item.isDone) {
                LineIcon(
                    type = LineIconType.CHECK,
                    tint = Color.White,
                    size = 15.dp,
                    strokeWidthDp = 2.dp
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (item.text.isEmpty()) {
                Text(
                    text = "List item",
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.textSecondary.copy(alpha = 0.6f)
                )
            }
            BasicTextField(
                value = item.text,
                onValueChange = onTextChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = if (item.isDone) tokens.textSecondary else tokens.textPrimary,
                    textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                ),
                singleLine = true,
                cursorBrush = SolidColor(tokens.accentStart),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { onAddRowAfter() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace && item.text.isEmpty()) {
                            onRemoveRow()
                            true
                        } else if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            onAddRowAfter()
                            true
                        } else {
                            false
                        }
                    }
            )
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemoveRow),
            contentAlignment = Alignment.Center
        ) {
            LineIcon(
                type = LineIconType.CLOSE,
                tint = tokens.textSecondary.copy(alpha = 0.65f),
                size = 16.dp
            )
        }
    }
}

@Composable
private fun VisualAttachmentCard(
    attachment: NoteAttachment,
    onCrop: (Bitmap) -> Unit,
    onRemove: () -> Unit
) {
    val decodedBitmap = remember(attachment.dataBase64) {
        try {
            val bytes = Base64.decode(attachment.dataBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
    val imageBitmap = remember(decodedBitmap) { decodedBitmap?.asImageBitmap() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .testTag("attachment_card_${attachment.id}")
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = attachment.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (decodedBitmap != null) {
                Box(
                    modifier = Modifier
                        .testTag("crop_attachment_${attachment.id}")
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { onCrop(decodedBitmap) },
                    contentAlignment = Alignment.Center
                ) {
                    LineIcon(
                        type = LineIconType.CROP,
                        tint = Color.White,
                        size = 15.dp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .testTag("remove_attachment_${attachment.id}")
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                LineIcon(
                    type = LineIconType.CLOSE,
                    tint = Color.White,
                    size = 14.dp
                )
            }
        }
    }
}

@Composable
fun MarkdownPreviewRenderer(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val lines = remember(markdown) { markdown.lines() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lines.forEach { rawLine ->
            val trimmed = rawLine.trim()
            when {
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("### "), tokens.accentStart),
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("## "), tokens.accentStart),
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("# "), tokens.accentStart),
                        style = MaterialTheme.typography.headlineMedium,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.accentStart,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = parseInlineMarkdown(trimmed.substring(2), tokens.accentStart),
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.textPrimary
                        )
                    }
                }
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(rawLine, tokens.accentStart),
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.textPrimary
                    )
                }
            }
        }
    }
}

private fun parseInlineMarkdown(text: String, codeAccent: Color) = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text.startsWith("*", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = codeAccent,
                            background = codeAccent.copy(alpha = 0.14f),
                            fontSize = 13.sp
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}

private fun copyNoteToClipboard(context: Context, note: Note) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val content = buildNotePlainText(note)
        clipboard.setPrimaryClip(ClipData.newPlainText(note.title.ifBlank { "Note" }, content))
    } catch (_: Exception) {}
}

private fun shareNote(context: Context, note: Note, onToast: (String) -> Unit) {
    val content = buildNotePlainText(note)
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title.ifBlank { "Notes" })
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Note").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        copyNoteToClipboard(context, note)
        onToast("Copied to share")
    }
}

private fun buildNotePlainText(note: Note): String {
    val bodyPart = if (note.isChecklist) {
        note.checklist.joinToString("\n") { "${if (it.isDone) "[x]" else "[ ]"} ${it.text}" }
    } else {
        note.body
    }
    return buildString {
        if (note.title.isNotBlank()) {
            appendLine(note.title)
            appendLine()
        }
        append(bodyPart)
    }
}

private fun generateSampleGraphicBitmap(colorArgb: Int, label: String): Bitmap {
    val bmp = Bitmap.createBitmap(480, 320, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    canvas.drawColor(android.graphics.Color.rgb(240, 246, 255))
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.color = colorArgb
    canvas.drawRoundRect(24f, 24f, 456f, 296f, 28f, 28f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 30f
    canvas.drawText(label, 64f, 168f, paint)
    return bmp
}

private fun createFallbackScanBitmap(title: String): Bitmap {
    val bmp = Bitmap.createBitmap(440, 280, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 4f
    paint.color = android.graphics.Color.rgb(37, 99, 235)
    canvas.drawRect(20f, 20f, 420f, 260f, paint)
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.rgb(15, 23, 42)
    paint.textSize = 24f
    canvas.drawText("SCANNED DOCUMENT", 44f, 74f, paint)
    paint.textSize = 18f
    paint.color = android.graphics.Color.rgb(71, 85, 105)
    canvas.drawText(title.take(28), 44f, 118f, paint)
    canvas.drawLine(44f, 148f, 380f, 148f, paint)
    canvas.drawLine(44f, 180f, 340f, 180f, paint)
    canvas.drawLine(44f, 212f, 300f, 212f, paint)
    return bmp
}
