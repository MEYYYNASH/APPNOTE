package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatClear
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.model.AppStrings
import com.example.model.AttachmentType
import com.example.model.ChecklistItem
import com.example.model.EditorFontStyle
import com.example.model.Note
import com.example.model.NoteAttachment
import com.example.model.NoteColor
import com.example.model.NoteVersion
import com.example.model.RichTextSpan
import com.example.ui.components.CustomColorPickerDialog
import com.example.ui.components.DocumentScannerDialog
import com.example.ui.components.DrawingCanvasDialog
import com.example.ui.components.FontStylePickerDialog
import com.example.ui.components.GlassActionPill
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassOuterPane
import com.example.ui.components.GlassPanel
import com.example.ui.components.HistoryDialog
import com.example.ui.components.InsertLinkDialog
import com.example.ui.components.InsertTableDialog
import com.example.ui.components.PhotoCropDialog
import com.example.ui.components.ReminderPickerDialog
import com.example.ui.components.RichTextVisualTransformation
import com.example.ui.components.TypingFormatState
import com.example.ui.components.VoiceAttachmentPlayer
import com.example.ui.components.VoiceRecorderDialog
import com.example.ui.components.adjustSpansForTextEdit
import com.example.ui.components.applySpanFormat
import com.example.ui.components.buildRichAnnotatedString
import com.example.ui.components.convertLegacyMarkdownToRichText
import com.example.ui.components.currentParagraphRange
import com.example.ui.components.formatNoteTime
import com.example.ui.components.normalizeAndCompactSpans
import com.example.ui.components.resolveFormatStateAt
import com.example.ui.components.toggleLinePrefixInSelection
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.theme.FigtreeFontFamily
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.resolveEditorFontFamily
import com.example.util.ReminderScheduler
import java.util.UUID

private data class PendingCropTarget(
    val bitmap: Bitmap,
    val type: AttachmentType,
    val fileName: String,
    val existingAttachmentId: String? = null
)

private data class NoteTemplateOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val defaultTitle: String,
    val defaultBody: String,
    val defaultSpans: List<RichTextSpan> = emptyList(),
    val folder: String,
    val icon: ImageVector
)

private val NOTE_TEMPLATES: List<NoteTemplateOption> by lazy {
    listOf(
        NoteTemplateOption(
            id = "meeting",
            title = "Meeting Notes",
            subtitle = "Agenda, attendees, decisions & action items",
            defaultTitle = "Meeting Notes",
            defaultBody = "Meeting Notes\n\nDate: Today\nAttendees: Team\n\nAgenda\n1. Project milestone review\n2. Upcoming deliverables\n\nAction Items\n• Follow up on design specs\n• Schedule QA review",
            defaultSpans = listOf(
                RichTextSpan(0, 13, headingLevel = 1),
                RichTextSpan(39, 45, headingLevel = 2),
                RichTextSpan(97, 109, headingLevel = 2)
            ),
            folder = "Work",
            icon = Icons.Rounded.Event
        ),
        NoteTemplateOption(
            id = "project",
            title = "Project Specification",
            subtitle = "Goals, architecture, milestones & deliverables",
            defaultTitle = "Project Ideas",
            defaultBody = "Project Specification\n\nBuild a modern note app with rich formatting and offline sync.\n\nCore Features\n• Clean Live Rich Formatting\n• Multi-page Document Scanner\n• Custom Reminder Sounds\n\nMilestone  |  Owner  |  Status\nUI Design  |  Design  |  Done\nRelease v1.2.0  |  Eng  |  Active",
            defaultSpans = listOf(
                RichTextSpan(0, 21, headingLevel = 1),
                RichTextSpan(23, 86, isQuote = true),
                RichTextSpan(88, 101, headingLevel = 2),
                RichTextSpan(188, 220, isBold = true)
            ),
            folder = "Projects",
            icon = Icons.Rounded.RocketLaunch
        ),
        NoteTemplateOption(
            id = "journal",
            title = "Daily Journal",
            subtitle = "Gratitude, highlights & daily reflection",
            defaultTitle = "Daily Journal",
            defaultBody = "Daily Reflection\n\n3 Things I'm Grateful For\n1. Productive morning session\n2. Helpful team feedback\n3. Clear priorities\n\nToday's Highlight\nFocused 100% on shipping quality features.",
            defaultSpans = listOf(
                RichTextSpan(0, 16, headingLevel = 1),
                RichTextSpan(18, 43, headingLevel = 2),
                RichTextSpan(119, 136, headingLevel = 2),
                RichTextSpan(137, 181, highlightColorArgb = 0xFFFACC15L)
            ),
            folder = "Journal",
            icon = Icons.Rounded.Favorite
        ),
        NoteTemplateOption(
            id = "code",
            title = "Technical Snippet",
            subtitle = "Code block, architecture notes & links",
            defaultTitle = "Code & Architecture Note",
            defaultBody = "Technical Note\n\nfun verifyRelease(version: String): Boolean {\n    return version == \"1.2.0\"\n}\n\nReference: GitHub Repository",
            defaultSpans = listOf(
                RichTextSpan(0, 14, headingLevel = 1),
                RichTextSpan(16, 94, isCode = true),
                RichTextSpan(106, 123, linkUrl = "https://github.com/MEYYYNASH/APPNOTE")
            ),
            folder = "Projects",
            icon = Icons.Rounded.Code
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
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
    availableFolders: List<String> = listOf("Personal", "Projects", "Work", "Journal", "Scanned Docs"),
    onUpdateAttachment: (String, String) -> Unit = { _, _ -> },
    onSetReminder: (Long?) -> Unit = {},
    onSetReminderWithSound: ((Long?, String?, String?, String?) -> Unit)? = null,
    onToggleFavorite: () -> Unit = {},
    onUpdateRichFormatting: (
        spans: List<RichTextSpan>?,
        fontStyle: String?,
        fontSizeSp: Float?,
        textColorArgb: Long?,
        clearTextColor: Boolean,
        textAlign: String?
    ) -> Unit = { _, _, _, _, _, _ -> },
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
        var showScannerDialog by remember { mutableStateOf(false) }
        var showTemplatesDialog by remember { mutableStateOf(false) }
        var showCustomColorDialog by remember { mutableStateOf(false) }
        var showFontStyleDialog by remember { mutableStateOf(false) }
        var showInsertLinkDialog by remember { mutableStateOf(false) }
        var showInsertTableDialog by remember { mutableStateOf(false) }
        var pendingCrop by remember { mutableStateOf<PendingCropTarget?>(null) }
        var tagsInput by remember(note.id) { mutableStateOf(note.tags.joinToString(", ")) }

        var isFullScreenEditor by remember { mutableStateOf(false) }
        var isReadingMode by remember { mutableStateOf(false) }
        var showRichFormatBar by remember { mutableStateOf(true) }

        var bodyFieldValue by remember(note.id) {
            mutableStateOf(TextFieldValue(text = note.body, selection = TextRange(note.body.length)))
        }
        var typingStyle by remember(note.id) { mutableStateOf(TypingFormatState()) }

        // Automatically convert any legacy raw markdown tags into clean WYSIWYG spans on open
        LaunchedEffect(note.id, note.body) {
            if (note.richSpans.isEmpty() && note.body.isNotEmpty()) {
                val (convertedText, convertedSpans) = convertLegacyMarkdownToRichText(note.body, note.richSpans)
                if (convertedText != note.body || convertedSpans.isNotEmpty()) {
                    bodyFieldValue = TextFieldValue(
                        text = convertedText,
                        selection = TextRange(convertedText.length)
                    )
                    onUpdateNote(null, convertedText, null, null, null, null, null, null)
                    onUpdateRichFormatting(convertedSpans, null, null, null, false, null)
                    return@LaunchedEffect
                }
            }
            if (note.body != bodyFieldValue.text) {
                val len = note.body.length
                bodyFieldValue = bodyFieldValue.copy(
                    text = note.body,
                    selection = TextRange(
                        bodyFieldValue.selection.start.coerceIn(0, len),
                        bodyFieldValue.selection.end.coerceIn(0, len)
                    )
                )
            }
        }

        val undoStack = remember(note.id) { mutableStateListOf<Pair<String, List<RichTextSpan>>>() }
        val redoStack = remember(note.id) { mutableStateListOf<Pair<String, List<RichTextSpan>>>() }

        fun pushUndoSnapshot() {
            val snap = note.body to note.richSpans
            if (undoStack.isEmpty() || undoStack.last() != snap) {
                undoStack.add(snap)
                if (undoStack.size > 40) undoStack.removeAt(0)
            }
            redoStack.clear()
        }

        fun commitBodyAndSpans(
            newText: String,
            newSpans: List<RichTextSpan>,
            newSelection: TextRange? = null
        ) {
            val cleaned = normalizeAndCompactSpans(newSpans, newText.length)
            if (newText != note.body || cleaned != note.richSpans) {
                pushUndoSnapshot()
                bodyFieldValue = bodyFieldValue.copy(
                    text = newText,
                    selection = newSelection ?: TextRange(
                        bodyFieldValue.selection.start.coerceIn(0, newText.length),
                        bodyFieldValue.selection.end.coerceIn(0, newText.length)
                    )
                )
                onUpdateNote(null, newText, null, null, null, null, null, null)
                onUpdateRichFormatting(cleaned, null, null, null, false, null)
            }
        }

        fun applySpansOnly(newSpans: List<RichTextSpan>) {
            val cleaned = normalizeAndCompactSpans(newSpans, bodyFieldValue.text.length)
            if (cleaned != note.richSpans) {
                pushUndoSnapshot()
                onUpdateRichFormatting(cleaned, null, null, null, false, null)
            }
        }

        fun toggleInlineFormatting(
            isActive: (TypingFormatState) -> Boolean,
            updateTyping: (TypingFormatState, Boolean) -> TypingFormatState,
            updateSpan: (RichTextSpan, Boolean) -> RichTextSpan
        ) {
            val sel = bodyFieldValue.selection
            if (!sel.collapsed) {
                val current = resolveFormatStateAt(note.richSpans, sel)
                val targetState = !isActive(current)
                val updated = applySpanFormat(
                    textLength = bodyFieldValue.text.length,
                    rangeStart = sel.start,
                    rangeEnd = sel.end,
                    spans = note.richSpans
                ) { span -> updateSpan(span, targetState) }
                applySpansOnly(updated)
            } else {
                val nextActive = !isActive(typingStyle)
                typingStyle = updateTyping(typingStyle, nextActive)
            }
        }

        fun toggleParagraphBlock(targetHeading: Int = 0, toggleQuote: Boolean = false) {
            val text = bodyFieldValue.text
            val sel = bodyFieldValue.selection
            if (text.isEmpty()) {
                if (toggleQuote) {
                    typingStyle = typingStyle.copy(isQuote = !typingStyle.isQuote)
                } else {
                    typingStyle = typingStyle.copy(
                        headingLevel = if (typingStyle.headingLevel == targetHeading) 0 else targetHeading
                    )
                }
                return
            }
            val pRange = currentParagraphRange(text, sel)
            if (pRange.first > pRange.last) {
                if (toggleQuote) {
                    typingStyle = typingStyle.copy(isQuote = !typingStyle.isQuote)
                } else {
                    typingStyle = typingStyle.copy(
                        headingLevel = if (typingStyle.headingLevel == targetHeading) 0 else targetHeading
                    )
                }
            } else {
                val blockStart = pRange.first
                val blockEnd = pRange.last + 1
                val curState = resolveFormatStateAt(note.richSpans, TextRange(blockStart, blockEnd))
                val updated = applySpanFormat(
                    textLength = text.length,
                    rangeStart = blockStart,
                    rangeEnd = blockEnd,
                    spans = note.richSpans
                ) { span ->
                    if (toggleQuote) {
                        span.copy(isQuote = !curState.isQuote)
                    } else {
                        val nextHeading = if (curState.headingLevel == targetHeading) 0 else targetHeading
                        span.copy(headingLevel = nextHeading)
                    }
                }
                applySpansOnly(updated)
                typingStyle = if (toggleQuote) {
                    typingStyle.copy(isQuote = !curState.isQuote)
                } else {
                    typingStyle.copy(
                        headingLevel = if (curState.headingLevel == targetHeading) 0 else targetHeading
                    )
                }
            }
        }

        val activeBodyFontFamily = resolveEditorFontFamily(
            note.bodyFontStyle,
            MaterialTheme.typography.bodyLarge.fontFamily ?: FigtreeFontFamily
        )
        val activeEditorTextColor = note.bodyTextColorArgb?.let { Color(it) } ?: tokens.textPrimary
        val editorFontSizeSp = note.bodyFontSizeSp.coerceIn(12f, 28f)
        val editorTextAlign = when (note.bodyTextAlign) {
            "center" -> TextAlign.Center
            "end" -> TextAlign.End
            else -> TextAlign.Start
        }
        val liveFormatState = remember(note.richSpans, bodyFieldValue.selection, typingStyle) {
            if (!bodyFieldValue.selection.collapsed) {
                resolveFormatStateAt(note.richSpans, bodyFieldValue.selection)
            } else {
                typingStyle
            }
        }
        val activeCustomSwatchColor = remember(liveFormatState.textColorArgb, note.bodyTextColorArgb, tokens.accentStart) {
            (liveFormatState.textColorArgb ?: note.bodyTextColorArgb)?.let { Color(it) } ?: tokens.accentStart
        }

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

        // Suite 3: Video, PDF, and Generic File Attachment Launcher
        var pendingDocumentType by remember { mutableStateOf(AttachmentType.FILE) }
        val documentAttachmentLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    val displayName = queryAttachmentFileName(context, uri)
                        ?: "${pendingDocumentType.name}_${System.currentTimeMillis() % 10000}"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes()
                    } ?: ByteArray(0)
                    val cappedBytes = if (bytes.size > 256 * 1024) bytes.copyOf(256 * 1024) else bytes
                    val encoded = Base64.encodeToString(cappedBytes, Base64.NO_WRAP)
                    onAddAttachment(pendingDocumentType, displayName, encoded, 0)
                    onShowToast("Attached $displayName")
                } catch (_: Exception) {
                    onShowToast("Attached ${pendingDocumentType.name.lowercase()} file")
                }
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

        val charCount = remember(note.body, note.checklist, note.isChecklist) {
            if (note.isChecklist) {
                note.checklist.sumOf { it.text.length }
            } else {
                note.body.length
            }
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
            showHistoryDialog ||
            showScannerDialog ||
            showTemplatesDialog ||
            showCustomColorDialog ||
            showFontStyleDialog ||
            showInsertLinkDialog ||
            showInsertTableDialog

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (isEditorDialogOpen) 26.dp else 0.dp)
            ) {
                // Full-screen Top Bar: Back, Color dots, Undo/Redo, Full-screen, Reading Mode, Templates, Pin, Favorite, Reminder, Checklist, Copy, Share, Delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showBackButton) {
                        GlassIconButton(
                            icon = LineIconType.BACK,
                            contentDesc = "Back",
                            onClick = onBack,
                            sizeDp = 38.dp,
                            testTagName = "editor_back_button"
                        )
                    }

                    // 6 Color Dots (Blue, Green, Amber, Violet, Pink, Slate)
                    if (!isFullScreenEditor) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            NoteColor.entries.forEach { c ->
                                val isSelected = note.color == c
                                Box(
                                    modifier = Modifier
                                        .testTag("editor_color_dot_${c.id}")
                                        .size(24.dp)
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
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(c.dotColor)
                                    )
                                }
                            }
                        }
                    }

                    // Undo Button
                    SmallTextChipButton(
                        label = "",
                        icon = Icons.AutoMirrored.Rounded.Undo,
                        contentDesc = "Undo",
                        enabled = undoStack.isNotEmpty(),
                        testTag = "editor_undo_button",
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                val (prevText, prevSpans) = undoStack.removeAt(undoStack.lastIndex)
                                redoStack.add(note.body to note.richSpans)
                                bodyFieldValue = TextFieldValue(prevText, TextRange(prevText.length))
                                onUpdateNote(null, prevText, null, null, null, null, null, null)
                                onUpdateRichFormatting(prevSpans, null, null, null, false, null)
                            }
                        }
                    )

                    // Redo Button
                    SmallTextChipButton(
                        label = "",
                        icon = Icons.AutoMirrored.Rounded.Redo,
                        contentDesc = "Redo",
                        enabled = redoStack.isNotEmpty(),
                        testTag = "editor_redo_button",
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                val (nextText, nextSpans) = redoStack.removeAt(redoStack.lastIndex)
                                undoStack.add(note.body to note.richSpans)
                                bodyFieldValue = TextFieldValue(nextText, TextRange(nextText.length))
                                onUpdateNote(null, nextText, null, null, null, null, null, null)
                                onUpdateRichFormatting(nextSpans, null, null, null, false, null)
                            }
                        }
                    )

                    // Reading Mode Toggle
                    SmallTextChipButton(
                        label = if (isReadingMode) "Reading" else "Read",
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        active = isReadingMode,
                        testTag = "editor_reading_mode_button",
                        onClick = { isReadingMode = !isReadingMode }
                    )

                    // Full-Screen Mode Toggle
                    SmallTextChipButton(
                        label = if (isFullScreenEditor) "Exit Full" else "Full",
                        icon = if (isFullScreenEditor) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                        active = isFullScreenEditor,
                        testTag = "editor_fullscreen_button",
                        onClick = { isFullScreenEditor = !isFullScreenEditor }
                    )

                    // Note Templates Button
                    SmallTextChipButton(
                        label = "Templates",
                        icon = Icons.Rounded.ViewAgenda,
                        testTag = "editor_templates_button",
                        onClick = { showTemplatesDialog = true }
                    )

                    // Favorite Star Toggle
                    SmallTextChipButton(
                        label = "Fav",
                        icon = if (note.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        active = note.isFavorite,
                        testTag = "editor_favorite_button",
                        onClick = onToggleFavorite
                    )

                    GlassIconButton(
                        icon = LineIconType.PIN,
                        contentDesc = strings.pinBtn,
                        active = note.isPinned,
                        sizeDp = 38.dp,
                        onClick = {
                            onUpdateNote(null, null, null, null, null, null, null, !note.isPinned)
                        },
                        testTagName = "editor_pin_button"
                    )
                    GlassIconButton(
                        icon = LineIconType.BELL,
                        contentDesc = strings.reminderTitle,
                        active = note.reminderAt != null,
                        sizeDp = 38.dp,
                        onClick = { showReminderDialog = true },
                        testTagName = "editor_reminder_button"
                    )
                    GlassIconButton(
                        icon = LineIconType.CHECKLIST,
                        contentDesc = strings.checklistTag,
                        active = note.isChecklist,
                        sizeDp = 38.dp,
                        onClick = {
                            onUpdateNote(null, null, !note.isChecklist, null, null, null, null, null)
                        },
                        testTagName = "editor_checklist_toggle"
                    )
                    GlassIconButton(
                        icon = LineIconType.COPY,
                        contentDesc = strings.copyBtn,
                        sizeDp = 38.dp,
                        onClick = {
                            copyNoteToClipboard(context, note)
                            onShowToast("Copied to clipboard")
                        },
                        testTagName = "editor_copy_button"
                    )
                    GlassIconButton(
                        icon = LineIconType.SHARE,
                        contentDesc = strings.shareBtn,
                        sizeDp = 38.dp,
                        onClick = { shareNote(context, note, onShowToast) },
                        testTagName = "editor_share_button"
                    )
                    GlassIconButton(
                        icon = LineIconType.TRASH,
                        contentDesc = strings.deleteBtn,
                        tint = Color(0xFFEF4444),
                        sizeDp = 38.dp,
                        onClick = onDeleteNote,
                        testTagName = "editor_delete_button"
                    )
                }

                // Live WYSIWYG Rich Formatting Bar (Applies directly to selected text or upcoming typing, zero markup syntax inserted)
                AnimatedVisibility(visible = !isFullScreenEditor && !isReadingMode && !note.isChecklist && showRichFormatBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("editor_rich_formatting_bar"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FormatChip(
                            label = "B",
                            testTag = "format_bold",
                            active = liveFormatState.isBold
                        ) {
                            toggleInlineFormatting(
                                isActive = { it.isBold },
                                updateTyping = { st, v -> st.copy(isBold = v) },
                                updateSpan = { sp, v -> sp.copy(isBold = v) }
                            )
                        }
                        FormatChip(
                            label = "I",
                            testTag = "format_italic",
                            active = liveFormatState.isItalic
                        ) {
                            toggleInlineFormatting(
                                isActive = { it.isItalic },
                                updateTyping = { st, v -> st.copy(isItalic = v) },
                                updateSpan = { sp, v -> sp.copy(isItalic = v) }
                            )
                        }
                        FormatChip(
                            label = "U",
                            testTag = "format_underline",
                            active = liveFormatState.isUnderline
                        ) {
                            toggleInlineFormatting(
                                isActive = { it.isUnderline },
                                updateTyping = { st, v -> st.copy(isUnderline = v) },
                                updateSpan = { sp, v -> sp.copy(isUnderline = v) }
                            )
                        }
                        FormatChip(
                            label = "S",
                            testTag = "format_strikethrough",
                            active = liveFormatState.isStrikethrough
                        ) {
                            toggleInlineFormatting(
                                isActive = { it.isStrikethrough },
                                updateTyping = { st, v -> st.copy(isStrikethrough = v) },
                                updateSpan = { sp, v -> sp.copy(isStrikethrough = v) }
                            )
                        }
                        FormatChip(
                            label = "H1",
                            testTag = "format_h1",
                            active = liveFormatState.headingLevel == 1
                        ) { toggleParagraphBlock(targetHeading = 1) }
                        FormatChip(
                            label = "H2",
                            testTag = "format_h2",
                            active = liveFormatState.headingLevel == 2
                        ) { toggleParagraphBlock(targetHeading = 2) }
                        FormatChip(
                            label = "H3",
                            testTag = "format_h3",
                            active = liveFormatState.headingLevel == 3
                        ) { toggleParagraphBlock(targetHeading = 3) }
                        FormatChip(
                            label = "Highlight",
                            testTag = "format_highlight",
                            icon = Icons.Rounded.BorderColor,
                            active = liveFormatState.highlightColorArgb != null
                        ) {
                            toggleInlineFormatting(
                                isActive = { it.highlightColorArgb != null },
                                updateTyping = { st, v -> st.copy(highlightColorArgb = if (v) 0xFFFACC15L else null) },
                                updateSpan = { sp, v -> sp.copy(highlightColorArgb = if (v) 0xFFFACC15L else null) }
                            )
                        }
                        FormatChip(
                            label = "Font: ${EditorFontStyle.fromId(liveFormatState.fontStyleId ?: note.bodyFontStyle).labelEn.substringAfterLast(" ")}",
                            testTag = "format_font_style",
                            icon = Icons.Rounded.TextFields,
                            active = (liveFormatState.fontStyleId ?: note.bodyFontStyle).let { it.isNotBlank() && it != EditorFontStyle.SANS.id }
                        ) {
                            showFontStyleDialog = true
                        }
                        FormatChip(
                            label = "Color",
                            testTag = "format_font_color",
                            icon = Icons.Rounded.Palette,
                            swatchColor = activeCustomSwatchColor,
                            active = liveFormatState.textColorArgb != null || note.bodyTextColorArgb != null
                        ) {
                            showCustomColorDialog = true
                        }
                        FormatChip(
                            label = "List",
                            testTag = "format_bullet_list",
                            icon = Icons.AutoMirrored.Rounded.FormatListBulleted
                        ) {
                            val (nextText, nextSel, nextSpans) = toggleLinePrefixInSelection(
                                text = bodyFieldValue.text,
                                selection = bodyFieldValue.selection,
                                spans = note.richSpans,
                                numbered = false
                            )
                            commitBodyAndSpans(nextText, nextSpans, nextSel)
                        }
                        FormatChip(
                            label = "1. List",
                            testTag = "format_numbered_list",
                            icon = Icons.Rounded.FormatListNumbered
                        ) {
                            val (nextText, nextSel, nextSpans) = toggleLinePrefixInSelection(
                                text = bodyFieldValue.text,
                                selection = bodyFieldValue.selection,
                                spans = note.richSpans,
                                numbered = true
                            )
                            commitBodyAndSpans(nextText, nextSpans, nextSel)
                        }
                        FormatChip(
                            label = "Quote",
                            testTag = "format_blockquote",
                            icon = Icons.Rounded.FormatQuote,
                            active = liveFormatState.isQuote
                        ) { toggleParagraphBlock(toggleQuote = true) }
                        FormatChip(
                            label = "Code",
                            testTag = "format_code_block",
                            icon = Icons.Rounded.Code,
                            active = liveFormatState.isCode
                        ) {
                            toggleInlineFormatting(
                                isActive = { it.isCode },
                                updateTyping = { st, v -> st.copy(isCode = v) },
                                updateSpan = { sp, v -> sp.copy(isCode = v) }
                            )
                        }
                        FormatChip(
                            label = "Table",
                            testTag = "format_table",
                            icon = Icons.Rounded.TableChart
                        ) {
                            showInsertTableDialog = true
                        }
                        FormatChip(
                            label = "Link",
                            testTag = "format_link",
                            icon = Icons.Rounded.Link
                        ) {
                            showInsertLinkDialog = true
                        }
                        FormatChip(
                            label = "${editorFontSizeSp.toInt()}sp",
                            testTag = "format_font_size",
                            icon = Icons.Rounded.FormatSize
                        ) {
                            val nextSize = when (editorFontSizeSp.toInt()) {
                                14 -> 16f
                                16 -> 19f
                                19 -> 22f
                                else -> 14f
                            }
                            onUpdateRichFormatting(null, null, nextSize, null, false, null)
                        }
                        FormatChip(
                            label = when (editorTextAlign) {
                                TextAlign.Center -> "Center"
                                TextAlign.End -> "Right"
                                else -> "Left"
                            },
                            testTag = "format_text_align",
                            icon = when (editorTextAlign) {
                                TextAlign.Center -> Icons.Rounded.FormatAlignCenter
                                TextAlign.End -> Icons.AutoMirrored.Rounded.FormatAlignRight
                                else -> Icons.AutoMirrored.Rounded.FormatAlignLeft
                            }
                        ) {
                            val nextAlign = when (note.bodyTextAlign) {
                                "start" -> "center"
                                "center" -> "end"
                                else -> "start"
                            }
                            onUpdateRichFormatting(null, null, null, null, false, nextAlign)
                        }
                        FormatChip(
                            label = "Clear",
                            testTag = "format_clear",
                            icon = Icons.Rounded.FormatClear
                        ) {
                            val sel = bodyFieldValue.selection
                            if (!sel.collapsed) {
                                val cleared = applySpanFormat(
                                    textLength = bodyFieldValue.text.length,
                                    rangeStart = sel.start,
                                    rangeEnd = sel.end,
                                    spans = note.richSpans
                                ) { RichTextSpan(it.start, it.end) }
                                applySpansOnly(cleared)
                            } else {
                                typingStyle = TypingFormatState()
                                onUpdateRichFormatting(null, EditorFontStyle.SANS.id, 16f, null, true, "start")
                            }
                        }
                    }
                }

                // Scrollable Editor Body
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
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
                                    if (!isReadingMode) {
                                        onUpdateNote(newTitle, null, null, null, null, null, null, null)
                                    }
                                },
                                readOnly = isReadingMode,
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

                    // Live meta line: Word Counter + Character Counter + Auto-save status + Active Reminder Badge
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 22.dp, end = 22.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (note.isChecklist) {
                                    Text(
                                        text = "${strings.doneOfLabel(doneCount, note.checklist.size)} · $wordCount ${strings.wordsLabel} · $charCount chars",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp),
                                        color = tokens.textSecondary,
                                        modifier = Modifier.testTag("editor_checklist_meta")
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(120.dp)
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
                                        text = "$wordCount ${strings.wordsLabel} · $charCount chars · Auto-saved · ${strings.editedLabel} ${formatNoteTime(note.updatedAt)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp),
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
                                    val soundInfo = if (note.reminderSoundStyle.isNotBlank()) " · Sound: ${note.reminderSoundStyle}" else ""
                                    Text(
                                        text = "${strings.reminderTitle}: ${ReminderScheduler.formatReminderTime(note.reminderAt)}$soundInfo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Tags + Folder compact glass inputs (hidden in distraction-free Full-Screen mode)
                    if (!isFullScreenEditor) {
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
                    }

                    // Attached files preview (Voice, Photos, Scans, Drawings, Videos, PDFs, Files)
                    if (note.attachments.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                note.attachments.forEach { attachment ->
                                    when (attachment.type) {
                                        AttachmentType.VOICE -> {
                                            VoiceAttachmentPlayer(
                                                attachment = attachment,
                                                onRemove = { onRemoveAttachment(attachment.id) }
                                            )
                                        }
                                        AttachmentType.VIDEO,
                                        AttachmentType.PDF,
                                        AttachmentType.FILE -> {
                                            DocumentFileAttachmentCard(
                                                attachment = attachment,
                                                onRemove = { onRemoveAttachment(attachment.id) },
                                                onOpen = {
                                                    onShowToast("Opened ${attachment.name} (${attachment.type.name})")
                                                }
                                            )
                                        }
                                        else -> {
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
                    }

                    // Body Area: Checklist OR Markdown/Reading Mode Preview OR Rich Textarea
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
                    } else if (isMarkdownPreview || isReadingMode) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 22.dp, vertical = 8.dp)
                                    .testTag("markdown_preview_box")
                            ) {
                                val rendered = remember(
                                    note.body,
                                    note.richSpans,
                                    activeBodyFontFamily,
                                    editorFontSizeSp,
                                    activeEditorTextColor,
                                    tokens.accentStart
                                ) {
                                    buildRichAnnotatedString(
                                        text = note.body,
                                        spans = note.richSpans,
                                        baseFontFamily = activeBodyFontFamily,
                                        baseFontSizeSp = editorFontSizeSp,
                                        baseColor = activeEditorTextColor,
                                        accentColor = tokens.accentStart
                                    )
                                }
                                Text(
                                    text = rendered,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = activeBodyFontFamily,
                                        fontSize = editorFontSizeSp.sp,
                                        lineHeight = (editorFontSizeSp * 1.65f).sp,
                                        color = activeEditorTextColor,
                                        textAlign = editorTextAlign
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                                if (bodyFieldValue.text.isEmpty()) {
                                    Text(
                                        text = strings.bodyPlaceholder,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = activeBodyFontFamily,
                                            fontSize = editorFontSizeSp.sp,
                                            lineHeight = (editorFontSizeSp * 1.65f).sp,
                                            textAlign = editorTextAlign
                                        ),
                                        color = tokens.textSecondary.copy(alpha = 0.65f),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                BasicTextField(
                                    value = bodyFieldValue,
                                    onValueChange = { nextVal ->
                                        if (nextVal.text != bodyFieldValue.text) {
                                            val oldText = bodyFieldValue.text
                                            val insertedNewline = nextVal.text.length > oldText.length &&
                                                nextVal.text.getOrNull((nextVal.selection.start - 1).coerceAtLeast(0)) == '\n'
                                            val updatedSpans = adjustSpansForTextEdit(
                                                oldText = oldText,
                                                newText = nextVal.text,
                                                spans = note.richSpans,
                                                typingStyle = typingStyle
                                            )
                                            pushUndoSnapshot()
                                            bodyFieldValue = nextVal
                                            if (insertedNewline && (typingStyle.headingLevel > 0 || typingStyle.isQuote)) {
                                                typingStyle = typingStyle.copy(headingLevel = 0, isQuote = false)
                                            }
                                            onUpdateNote(null, nextVal.text, null, null, null, null, null, null)
                                            onUpdateRichFormatting(updatedSpans, null, null, null, false, null)
                                        } else {
                                            bodyFieldValue = nextVal
                                        }
                                    },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = activeBodyFontFamily,
                                        fontSize = editorFontSizeSp.sp,
                                        lineHeight = (editorFontSizeSp * 1.65f).sp,
                                        color = activeEditorTextColor,
                                        textAlign = editorTextAlign
                                    ),
                                    visualTransformation = RichTextVisualTransformation(
                                        spans = note.richSpans,
                                        baseFontFamily = activeBodyFontFamily,
                                        baseFontSizeSp = editorFontSizeSp,
                                        baseColor = activeEditorTextColor,
                                        accentColor = tokens.accentStart
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

            // Sleek Floating Capsule Editor Toolbar Dock at the Bottom (Voice, Image, Multi-Page Scanner, Draw/Handwriting, Video, PDF, File, Markdown Preview, History)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(tokens.paneFill)
                    .border(1.dp, tokens.panelBorder, RoundedCornerShape(34.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .height(50.dp)
                        .horizontalScroll(rememberScrollState())
                        .clip(CircleShape)
                        .background(Color(0xFF151821))
                        .border(1.dp, tokens.panelBorder, CircleShape)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            showScannerDialog = true
                        }
                    )
                    EditorCapsuleIconBtn(
                        icon = LineIconType.DRAW,
                        contentDesc = strings.drawBtn,
                        testTag = "attach_draw_button",
                        onClick = { showDrawDialog = true }
                    )
                    // Video Attachment Pill
                    EditorTextPillBtn(
                        label = "Video",
                        icon = Icons.Rounded.VideoFile,
                        testTag = "attach_video_button",
                        onClick = {
                            pendingDocumentType = AttachmentType.VIDEO
                            try {
                                documentAttachmentLauncher.launch(arrayOf("video/*", "*/*"))
                            } catch (_: Exception) {
                                onAddAttachment(AttachmentType.VIDEO, "Clip_${System.currentTimeMillis() % 1000}.mp4", "", 15)
                                onShowToast("Attached video clip")
                            }
                        }
                    )
                    // PDF Attachment Pill
                    EditorTextPillBtn(
                        label = "PDF",
                        icon = Icons.Rounded.PictureAsPdf,
                        testTag = "attach_pdf_button",
                        onClick = {
                            pendingDocumentType = AttachmentType.PDF
                            try {
                                documentAttachmentLauncher.launch(arrayOf("application/pdf", "*/*"))
                            } catch (_: Exception) {
                                onAddAttachment(AttachmentType.PDF, "Document_${System.currentTimeMillis() % 1000}.pdf", "", 0)
                                onShowToast("Attached PDF document")
                            }
                        }
                    )
                    // File Attachment Pill
                    EditorTextPillBtn(
                        label = "File",
                        icon = Icons.Rounded.AttachFile,
                        testTag = "attach_file_button",
                        onClick = {
                            pendingDocumentType = AttachmentType.FILE
                            try {
                                documentAttachmentLauncher.launch(arrayOf("*/*"))
                            } catch (_: Exception) {
                                onAddAttachment(AttachmentType.FILE, "Attachment_${System.currentTimeMillis() % 1000}.zip", "", 0)
                                onShowToast("Attached file")
                            }
                        }
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
                initialSoundStyle = note.reminderSoundStyle,
                initialAlertMode = note.reminderAlertMode,
                initialVibrationPattern = note.reminderVibrationPattern,
                onDismiss = { showReminderDialog = false },
                onSetReminderWithSound = { millis, soundStyle, alertMode, vibPattern ->
                    if (onSetReminderWithSound != null) {
                        onSetReminderWithSound(millis, soundStyle, alertMode, vibPattern)
                    } else {
                        onSetReminder(millis)
                    }
                },
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
                        "Handwriting & Drawing",
                        b64,
                        0
                    )
                    showDrawDialog = false
                }
            )
        }

        if (showScannerDialog) {
            DocumentScannerDialog(
                availableFolders = availableFolders,
                currentFolder = note.folder.ifBlank { "Scanned Docs" },
                accentColor = tokens.accentStart,
                isKhmer = isKhmer,
                onDismiss = { showScannerDialog = false },
                onSaveScannedDocument = { docName, targetFolder, pdfName, pdfB64, previewImgB64, ocrText ->
                    if (previewImgB64.isNotBlank()) {
                        onAddAttachment(AttachmentType.SCAN, "$docName.jpg", previewImgB64, 0)
                    }
                    if (pdfB64.isNotBlank()) {
                        onAddAttachment(AttachmentType.PDF, pdfName, pdfB64, 0)
                    }
                    val mergedBody: String
                    val extraSpans = mutableListOf<RichTextSpan>()
                    if (ocrText.isNotBlank()) {
                        if (note.body.isBlank()) {
                            mergedBody = ocrText
                        } else {
                            val header = "Extracted OCR ($docName)"
                            val headerStart = note.body.length + 2
                            mergedBody = "${note.body}\n\n$header\n$ocrText"
                            extraSpans.add(RichTextSpan(headerStart, headerStart + header.length, headingLevel = 3))
                        }
                    } else {
                        mergedBody = note.body
                    }
                    onUpdateNote(
                        note.title.ifBlank { docName },
                        mergedBody,
                        null,
                        null,
                        null,
                        targetFolder.ifBlank { note.folder },
                        null,
                        null
                    )
                    if (extraSpans.isNotEmpty()) {
                        onUpdateRichFormatting(
                            normalizeAndCompactSpans(note.richSpans + extraSpans, mergedBody.length),
                            null, null, null, false, null
                        )
                    }
                    showScannerDialog = false
                    onShowToast("Added scanned document & OCR text")
                }
            )
        }

        if (showTemplatesDialog) {
            Dialog(onDismissRequest = { showTemplatesDialog = false }) {
                GlassPanel(
                    elevated = true,
                    cornerRadius = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_templates_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isKhmer) "ជ្រើសរើសទម្រង់កំណត់ត្រា (Note Templates)" else "Note Templates",
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            GlassIconButton(
                                icon = LineIconType.CLOSE,
                                contentDesc = "Close",
                                sizeDp = 34.dp,
                                onClick = { showTemplatesDialog = false }
                            )
                        }

                        NOTE_TEMPLATES.forEach { tpl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(tokens.fieldFill)
                                    .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        val prefix = if (note.body.isBlank()) "" else "${note.body}\n\n"
                                        val offset = prefix.length
                                        val nextBody = "$prefix${tpl.defaultBody}"
                                        val shiftedTemplateSpans = tpl.defaultSpans.map {
                                            it.copy(start = it.start + offset, end = it.end + offset)
                                        }
                                        val nextSpans = normalizeAndCompactSpans(
                                            note.richSpans + shiftedTemplateSpans,
                                            nextBody.length
                                        )
                                        commitBodyAndSpans(
                                            newText = nextBody,
                                            newSpans = nextSpans,
                                            newSelection = TextRange(nextBody.length)
                                        )
                                        if (note.title.isBlank()) {
                                            onUpdateNote(tpl.defaultTitle, nextBody, false, null, null, tpl.folder, null, null)
                                        }
                                        showTemplatesDialog = false
                                        onShowToast("Applied ${tpl.title}")
                                    }
                                    .padding(12.dp)
                                    .testTag("template_option_${tpl.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(tokens.accentStart.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tpl.icon,
                                        contentDescription = tpl.title,
                                        tint = tokens.accentStart,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tpl.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = tpl.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.textSecondary
                                    )
                                }
                            }
                        }

                        GlassActionPill(
                            label = strings.cancelBtn,
                            icon = LineIconType.CLOSE,
                            onClick = { showTemplatesDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (showCustomColorDialog) {
            CustomColorPickerDialog(
                hasSelection = !bodyFieldValue.selection.collapsed,
                currentTextColorArgb = liveFormatState.textColorArgb ?: note.bodyTextColorArgb,
                currentHighlightArgb = liveFormatState.highlightColorArgb,
                defaultTextColor = tokens.textPrimary,
                accentColor = tokens.accentStart,
                isKhmer = isKhmer,
                onDismiss = { showCustomColorDialog = false },
                onApplyTextColor = { argb, applyToWholeNote ->
                    val sel = bodyFieldValue.selection
                    if (!sel.collapsed) {
                        val updated = applySpanFormat(
                            textLength = bodyFieldValue.text.length,
                            rangeStart = sel.start,
                            rangeEnd = sel.end,
                            spans = note.richSpans
                        ) { it.copy(textColorArgb = argb) }
                        applySpansOnly(updated)
                    } else if (applyToWholeNote) {
                        onUpdateRichFormatting(
                            null, null, null, argb, argb == null, null
                        )
                        typingStyle = typingStyle.copy(textColorArgb = argb)
                    } else {
                        typingStyle = typingStyle.copy(textColorArgb = argb)
                    }
                },
                onApplyHighlightColor = { argb ->
                    val sel = bodyFieldValue.selection
                    if (!sel.collapsed) {
                        val updated = applySpanFormat(
                            textLength = bodyFieldValue.text.length,
                            rangeStart = sel.start,
                            rangeEnd = sel.end,
                            spans = note.richSpans
                        ) { it.copy(highlightColorArgb = argb) }
                        applySpansOnly(updated)
                    } else {
                        typingStyle = typingStyle.copy(highlightColorArgb = argb)
                    }
                }
            )
        }

        if (showFontStyleDialog) {
            FontStylePickerDialog(
                hasSelection = !bodyFieldValue.selection.collapsed,
                currentNoteFontStyleId = liveFormatState.fontStyleId ?: note.bodyFontStyle,
                accentColor = tokens.accentStart,
                isKhmer = isKhmer,
                onDismiss = { showFontStyleDialog = false },
                onSelectFontStyle = { styleId, applyToWholeNote ->
                    val sel = bodyFieldValue.selection
                    if (!sel.collapsed) {
                        val updated = applySpanFormat(
                            textLength = bodyFieldValue.text.length,
                            rangeStart = sel.start,
                            rangeEnd = sel.end,
                            spans = note.richSpans
                        ) { it.copy(fontStyleId = styleId) }
                        applySpansOnly(updated)
                    } else if (applyToWholeNote) {
                        onUpdateRichFormatting(null, styleId, null, null, false, null)
                        typingStyle = typingStyle.copy(fontStyleId = styleId)
                    } else {
                        typingStyle = typingStyle.copy(fontStyleId = styleId)
                    }
                }
            )
        }

        if (showInsertLinkDialog) {
            val sel = bodyFieldValue.selection
            val selectedLabel = if (!sel.collapsed) {
                bodyFieldValue.text.substring(
                    minOf(sel.start, sel.end).coerceIn(0, bodyFieldValue.text.length),
                    maxOf(sel.start, sel.end).coerceIn(0, bodyFieldValue.text.length)
                )
            } else ""
            InsertLinkDialog(
                initialLabel = selectedLabel,
                accentColor = tokens.accentStart,
                onDismiss = { showInsertLinkDialog = false },
                onConfirm = { cleanLabel, cleanUrl ->
                    val st = minOf(sel.start, sel.end).coerceIn(0, bodyFieldValue.text.length)
                    val en = maxOf(sel.start, sel.end).coerceIn(0, bodyFieldValue.text.length)
                    val prefixSpace = if (st > 0 && !bodyFieldValue.text[st - 1].isWhitespace() && sel.collapsed) " " else ""
                    val inserted = "$prefixSpace$cleanLabel"
                    val newText = bodyFieldValue.text.substring(0, st) + inserted + bodyFieldValue.text.substring(en)
                    val linkStart = st + prefixSpace.length
                    val linkEnd = linkStart + cleanLabel.length
                    val shifted = adjustSpansForTextEdit(
                        oldText = bodyFieldValue.text,
                        newText = newText,
                        spans = note.richSpans,
                        typingStyle = TypingFormatState()
                    )
                    val withLink = applySpanFormat(
                        textLength = newText.length,
                        rangeStart = linkStart,
                        rangeEnd = linkEnd,
                        spans = shifted
                    ) { it.copy(linkUrl = cleanUrl, isUnderline = true) }
                    commitBodyAndSpans(
                        newText = newText,
                        newSpans = withLink,
                        newSelection = TextRange(linkEnd)
                    )
                }
            )
        }

        if (showInsertTableDialog) {
            InsertTableDialog(
                accentColor = tokens.accentStart,
                onDismiss = { showInsertTableDialog = false },
                onConfirm = { rows, cols ->
                    val headerRow = (1..cols).joinToString("  |  ") { c -> "Column $c" }
                    val dataRows = (1..rows).joinToString("\n") { r ->
                        (1..cols).joinToString("  |  ") { c -> "Item $r.$c" }
                    }
                    val tableBlock = "$headerRow\n$dataRows"
                    val pos = bodyFieldValue.selection.end.coerceIn(0, bodyFieldValue.text.length)
                    val lead = if (pos == 0 || bodyFieldValue.text.getOrNull(pos - 1) == '\n') "" else "\n"
                    val trail = "\n"
                    val insertion = "$lead$tableBlock$trail"
                    val newText = bodyFieldValue.text.substring(0, pos) + insertion + bodyFieldValue.text.substring(pos)
                    val headerStart = pos + lead.length
                    val headerEnd = headerStart + headerRow.length
                    val shifted = adjustSpansForTextEdit(
                        oldText = bodyFieldValue.text,
                        newText = newText,
                        spans = note.richSpans,
                        typingStyle = TypingFormatState()
                    )
                    val withBoldHeader = applySpanFormat(
                        textLength = newText.length,
                        rangeStart = headerStart,
                        rangeEnd = headerEnd,
                        spans = shifted
                    ) { it.copy(isBold = true) }
                    commitBodyAndSpans(
                        newText = newText,
                        newSpans = withBoldHeader,
                        newSelection = TextRange((pos + insertion.length).coerceIn(0, newText.length))
                    )
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
private fun SmallTextChipButton(
    label: String,
    testTag: String,
    enabled: Boolean = true,
    active: Boolean = false,
    icon: ImageVector? = null,
    contentDesc: String? = null,
    onClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val contentTint = if (!enabled) tokens.textSecondary.copy(alpha = 0.4f) else tokens.textPrimary
    Box(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) tokens.accentStart.copy(alpha = 0.25f) else tokens.fieldFill)
            .border(
                1.dp,
                if (active) tokens.accentStart else tokens.panelBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDesc ?: label.ifBlank { testTag },
                    tint = contentTint,
                    modifier = Modifier.size(14.dp)
                )
            }
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentTint,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FormatChip(
    label: String,
    testTag: String,
    icon: ImageVector? = null,
    active: Boolean = false,
    swatchColor: Color? = null,
    onClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) tokens.accentStart.copy(alpha = 0.25f) else tokens.fieldFill)
            .border(
                width = 1.dp,
                color = if (active) tokens.accentStart else tokens.panelBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (swatchColor != null) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (active) tokens.accentStart else tokens.textPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textPrimary,
                fontWeight = FontWeight.Bold
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
            .size(42.dp)
            .clip(CircleShape)
            .background(if (active) Color.White else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        LineIcon(
            type = icon,
            tint = if (active) Color(0xFF11151E) else Color.White.copy(alpha = 0.85f),
            size = 19.dp
        )
    }
}

@Composable
private fun EditorTextPillBtn(
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DocumentFileAttachmentCard(
    attachment: NoteAttachment,
    onRemove: () -> Unit,
    onOpen: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val badgeIcon = when (attachment.type) {
        AttachmentType.VIDEO -> Icons.Rounded.VideoFile
        AttachmentType.PDF -> Icons.Rounded.PictureAsPdf
        else -> Icons.Rounded.Description
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.fieldFill)
            .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("attachment_card_${attachment.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tokens.accentStart.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = attachment.type.name,
                    tint = tokens.accentStart,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column {
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${attachment.type.name} Attachment • Tap to preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textSecondary
                )
            }
        }
        Box(
            modifier = Modifier
                .testTag("remove_attachment_${attachment.id}")
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444).copy(alpha = 0.16f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            LineIcon(
                type = LineIconType.CLOSE,
                tint = Color(0xFFEF4444),
                size = 14.dp
            )
        }
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
                trimmed.startsWith("```") -> {
                    // Code block fence marker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(tokens.accentStart.copy(alpha = 0.35f))
                    )
                }
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
                trimmed.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(tokens.fieldFill)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .background(tokens.accentStart)
                        )
                        Text(
                            text = parseInlineMarkdown(trimmed.removePrefix("> "), tokens.accentStart),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textPrimary,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
                trimmed.startsWith("|") && trimmed.endsWith("|") -> {
                    val cells = trimmed.trim('|').split("|").map { it.trim() }
                    if (!cells.all { it.all { ch -> ch == '-' || ch == ':' } }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(tokens.fieldFill)
                                .border(1.dp, tokens.panelBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            cells.forEach { cell ->
                                Text(
                                    text = parseInlineMarkdown(cell, tokens.accentStart),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tokens.textPrimary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
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
            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text.startsWith("==", i) -> {
                val end = text.indexOf("==", i + 2)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            background = Color(0xFFFACC15).copy(alpha = 0.35f),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(" ${text.substring(i + 2, end)} ")
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text.startsWith("<u>", i) -> {
                val end = text.indexOf("</u>", i + 3)
                if (end != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(text.substring(i + 3, end))
                    }
                    i = end + 4
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

private fun queryAttachmentFileName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    } catch (_: Exception) {
        null
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
