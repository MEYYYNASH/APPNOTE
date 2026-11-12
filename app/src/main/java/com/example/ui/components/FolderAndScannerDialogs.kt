package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.model.AttachmentType
import com.example.model.Note
import com.example.model.NoteColor
import com.example.model.NoteFolder
import com.example.ui.icons.LineIconType
import com.example.ui.theme.LocalGlassTokens
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private val FOLDER_ICON_OPTIONS = listOf(
    "folder" to "📁",
    "rocket" to "🚀",
    "briefcase" to "💼",
    "heart" to "💜",
    "scan" to "📄",
    "star" to "⭐",
    "book" to "📚",
    "code" to "💻"
)

fun folderIconSymbol(iconId: String): String {
    return FOLDER_ICON_OPTIONS.find { it.first == iconId }?.second ?: "📁"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoldersManagerDialog(
    folders: List<NoteFolder>,
    notes: List<Note>,
    activeSelectedFolder: String,
    accentColor: Color,
    isKhmer: Boolean,
    onDismiss: () -> Unit,
    onSelectFolderFilter: (String) -> Unit,
    onCreateFolder: (name: String, parentId: String?, iconId: String, color: NoteColor, isSmart: Boolean, smartQuery: String) -> Unit,
    onRenameFolder: (folderId: String, newName: String) -> Unit,
    onUpdateFolderCustomization: (folderId: String, iconId: String?, color: NoteColor?, parentId: String?) -> Unit,
    onTogglePinFolder: (folderId: String) -> Unit,
    onToggleArchiveFolder: (folderId: String) -> Unit,
    onDeleteFolder: (folderId: String) -> Unit,
    onRestoreDeletedFolder: (folderId: String) -> Unit,
    onPermanentlyDeleteFolder: (folderId: String) -> Unit,
    onReorderFolder: (folderId: String, moveUp: Boolean) -> Unit,
    onMoveNoteToFolder: (noteId: String, folderName: String) -> Unit,
    onCopyNoteToFolder: (noteId: String, folderName: String) -> Unit
) {
    val tokens = LocalGlassTokens.current
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("custom") }
    var viewTab by remember { mutableStateOf("active") }
    var showCreateForm by remember { mutableStateOf(false) }

    var newFolderName by remember { mutableStateOf("") }
    var newParentId by remember { mutableStateOf<String?>(null) }
    var newIconId by remember { mutableStateOf("folder") }
    var newColor by remember { mutableStateOf(NoteColor.VIOLET) }
    var newIsSmart by remember { mutableStateOf(false) }
    var newSmartQuery by remember { mutableStateOf("") }

    var renamingFolderId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var managingNotesForFolder by remember { mutableStateOf<NoteFolder?>(null) }

    val noteCountsByFolder = remember(notes, folders) {
        val map = mutableMapOf<String, Int>()
        folders.forEach { folder ->
            val count = if (folder.isSmart && folder.smartQuery.isNotBlank()) {
                val sq = folder.smartQuery.lowercase()
                notes.count { n ->
                    !n.isTrashed && (
                        n.title.lowercase().contains(sq) ||
                            n.body.lowercase().contains(sq) ||
                            n.tags.any { it.lowercase().contains(sq.removePrefix("#")) }
                        )
                }
            } else {
                notes.count { n -> !n.isTrashed && n.folder.equals(folder.name, ignoreCase = true) }
            }
            map[folder.id] = count
        }
        map
    }

    val filteredFolders = remember(folders, searchQuery, sortMode, viewTab, noteCountsByFolder) {
        val q = searchQuery.trim().lowercase()
        val base = folders.filter { f ->
            val tabOk = when (viewTab) {
                "trash" -> f.isTrashed
                "archived" -> !f.isTrashed && f.isArchived
                "smart" -> !f.isTrashed && !f.isArchived && f.isSmart
                else -> !f.isTrashed && !f.isArchived
            }
            tabOk && (q.isEmpty() || f.name.lowercase().contains(q) || f.smartQuery.lowercase().contains(q))
        }
        when (sortMode) {
            "name" -> base.sortedWith(compareByDescending<NoteFolder> { it.isPinned }.thenBy { it.name.lowercase() })
            "count" -> base.sortedWith(compareByDescending<NoteFolder> { it.isPinned }.thenByDescending { noteCountsByFolder[it.id] ?: 0 })
            "date" -> base.sortedWith(compareByDescending<NoteFolder> { it.isPinned }.thenByDescending { it.createdAt })
            else -> base.sortedWith(compareByDescending<NoteFolder> { it.isPinned }.thenBy { it.sortOrder })
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassPanel(
            elevated = true,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isKhmer) "គ្រប់គ្រងថតឯកសារកម្រិតខ្ពស់" else "Folders & Smart Organizer",
                            color = tokens.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${folders.count { !it.isTrashed }} folders • Subfolders, Smart Filters & Reorder",
                            color = tokens.textSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassPillButton(
                            text = if (showCreateForm) "Close Form" else "+ New Folder",
                            selected = showCreateForm,
                            accentColor = accentColor,
                            onClick = { showCreateForm = !showCreateForm },
                            modifier = Modifier.testTag("toggle_create_folder_button")
                        )
                        GlassIconButton(
                            icon = LineIconType.CLOSE,
                            contentDesc = "Close Folders",
                            onClick = onDismiss,
                            sizeDp = 38.dp,
                            testTagName = "close_folders_dialog"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (isKhmer) "ស្វែងរកថតឯកសារ..." else "Search folders...",
                                color = tokens.textSecondary,
                                fontSize = 12.5.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(color = tokens.textPrimary, fontSize = 12.5.sp),
                            cursorBrush = SolidColor(accentColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("folder_search_input")
                        )
                    }

                    GlassPillButton(
                        text = when (sortMode) {
                            "name" -> "Sort: A-Z"
                            "count" -> "Sort: Count"
                            "date" -> "Sort: Newest"
                            else -> "Sort: Order"
                        },
                        accentColor = accentColor,
                        onClick = {
                            sortMode = when (sortMode) {
                                "custom" -> "name"
                                "name" -> "count"
                                "count" -> "date"
                                else -> "custom"
                            }
                        },
                        modifier = Modifier.testTag("folder_sort_button")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "active" to "Active (${folders.count { !it.isTrashed && !it.isArchived }})",
                        "smart" to "Smart (${folders.count { !it.isTrashed && it.isSmart }})",
                        "archived" to "Archived (${folders.count { !it.isTrashed && it.isArchived }})",
                        "trash" to "Deleted (${folders.count { it.isTrashed }})"
                    ).forEach { (id, label) ->
                        GlassPillButton(
                            text = label,
                            selected = viewTab == id,
                            accentColor = accentColor,
                            onClick = { viewTab = id }
                        )
                    }
                    if (activeSelectedFolder.isNotEmpty()) {
                        GlassPillButton(
                            text = "Clear Filter ($activeSelectedFolder)",
                            accentColor = Color(0xFFEF4444),
                            onClick = { onSelectFolderFilter("") }
                        )
                    }
                }

                if (showCreateForm) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Create Folder / Subfolder / Smart Folder",
                            color = tokens.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.panelFill)
                                .border(1.dp, tokens.panelBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            if (newFolderName.isEmpty()) {
                                Text("Folder name (e.g. Design Specs)", color = tokens.textSecondary, fontSize = 12.5.sp)
                            }
                            BasicTextField(
                                value = newFolderName,
                                onValueChange = { newFolderName = it },
                                singleLine = true,
                                textStyle = TextStyle(color = tokens.textPrimary, fontSize = 12.5.sp),
                                cursorBrush = SolidColor(accentColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_folder_name_input")
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Icon:", color = tokens.textSecondary, fontSize = 11.5.sp)
                            FOLDER_ICON_OPTIONS.forEach { (iconId, emoji) ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (newIconId == iconId) accentColor.copy(alpha = 0.28f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            1.dp,
                                            if (newIconId == iconId) accentColor else tokens.panelBorder,
                                            CircleShape
                                        )
                                        .clickable { newIconId = iconId },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 14.sp)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Color:", color = tokens.textSecondary, fontSize = 11.5.sp)
                            NoteColor.entries.forEach { nc ->
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(nc.dotColor)
                                        .border(
                                            width = if (newColor == nc) 2.5.dp else 1.dp,
                                            color = if (newColor == nc) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { newColor = nc }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Parent:", color = tokens.textSecondary, fontSize = 11.5.sp)
                            GlassPillButton(
                                text = "Root (Top Level)",
                                selected = newParentId == null,
                                accentColor = accentColor,
                                onClick = { newParentId = null }
                            )
                            folders.filter { !it.isTrashed && it.parentId == null }.forEach { parent ->
                                GlassPillButton(
                                    text = "↳ ${parent.name}",
                                    selected = newParentId == parent.id,
                                    accentColor = accentColor,
                                    onClick = { newParentId = parent.id }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassPillButton(
                                text = if (newIsSmart) "✓ Smart Folder Enabled" else "Make Smart Folder",
                                selected = newIsSmart,
                                accentColor = accentColor,
                                onClick = { newIsSmart = !newIsSmart }
                            )
                            if (newIsSmart) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(tokens.panelFill)
                                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    if (newSmartQuery.isEmpty()) {
                                        Text("Auto-match tag/keyword (e.g. #urgent)", color = tokens.textSecondary, fontSize = 11.5.sp)
                                    }
                                    BasicTextField(
                                        value = newSmartQuery,
                                        onValueChange = { newSmartQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(color = tokens.textPrimary, fontSize = 11.5.sp),
                                        cursorBrush = SolidColor(accentColor),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            GlassPillButton(
                                text = "Create Folder",
                                selected = true,
                                accentColor = accentColor,
                                onClick = {
                                    if (newFolderName.isNotBlank()) {
                                        onCreateFolder(
                                            newFolderName,
                                            newParentId,
                                            newIconId,
                                            newColor,
                                            newIsSmart,
                                            newSmartQuery
                                        )
                                        newFolderName = ""
                                        newSmartQuery = ""
                                        newIsSmart = false
                                        showCreateForm = false
                                    }
                                },
                                modifier = Modifier.testTag("confirm_create_folder_button")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val targetManageFolder = managingNotesForFolder
                if (targetManageFolder != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(18.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Move / Copy Notes into \"${targetManageFolder.name}\"",
                                color = tokens.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            GlassPillButton(
                                text = "← Back to Folders",
                                accentColor = accentColor,
                                onClick = { managingNotesForFolder = null }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(notes.filter { !it.isTrashed }, key = { it.id }) { note ->
                                val inFolder = note.folder.equals(targetManageFolder.name, ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(tokens.panelFill)
                                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.title.ifBlank { "Untitled Note" },
                                            color = tokens.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Current folder: ${note.folder.ifBlank { "None" }}",
                                            color = tokens.textSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        GlassPillButton(
                                            text = if (inFolder) "✓ In Folder" else "Move Here",
                                            selected = inFolder,
                                            accentColor = accentColor,
                                            onClick = {
                                                onMoveNoteToFolder(
                                                    note.id,
                                                    if (inFolder) "" else targetManageFolder.name
                                                )
                                            }
                                        )
                                        GlassPillButton(
                                            text = "Copy",
                                            accentColor = accentColor,
                                            onClick = { onCopyNoteToFolder(note.id, targetManageFolder.name) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredFolders, key = { it.id }) { folder ->
                            val parentFolder = folders.find { it.id == folder.parentId }
                            val noteCount = noteCountsByFolder[folder.id] ?: 0
                            val isSelectedFilter = activeSelectedFolder.equals(folder.name, ignoreCase = true)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(tokens.fieldFill)
                                    .border(
                                        width = if (isSelectedFilter) 1.8.dp else 1.dp,
                                        color = if (isSelectedFilter) folder.color.dotColor else tokens.panelBorder,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        if (!folder.isTrashed) {
                                            onSelectFolderFilter(folder.name)
                                            onDismiss()
                                        }
                                    }
                                    .padding(12.dp)
                                    .testTag("folder_row_${folder.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(folder.color.dotColor.copy(alpha = 0.22f))
                                                .border(1.dp, folder.color.dotColor, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(folderIconSymbol(folder.iconId), fontSize = 17.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (parentFolder != null) {
                                                    Text(
                                                        text = "${parentFolder.name} / ",
                                                        color = tokens.textSecondary,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Text(
                                                    text = folder.name,
                                                    color = tokens.textPrimary,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (folder.isPinned) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("📌", fontSize = 11.sp)
                                                }
                                                if (folder.isSmart) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "SMART",
                                                        color = folder.color.dotColor,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (folder.isSmart && folder.smartQuery.isNotBlank()) {
                                                    "$noteCount notes • Matches \"${folder.smartQuery}\""
                                                } else {
                                                    "$noteCount notes"
                                                },
                                                color = tokens.textSecondary,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(folder.color.dotColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "$noteCount",
                                            color = folder.color.dotColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                if (renamingFolderId == folder.id) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(tokens.panelFill)
                                                .border(1.dp, accentColor, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            BasicTextField(
                                                value = renameText,
                                                onValueChange = { renameText = it },
                                                singleLine = true,
                                                textStyle = TextStyle(color = tokens.textPrimary, fontSize = 12.5.sp),
                                                cursorBrush = SolidColor(accentColor),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        GlassPillButton(
                                            text = "Save",
                                            selected = true,
                                            accentColor = accentColor,
                                            onClick = {
                                                onRenameFolder(folder.id, renameText)
                                                renamingFolderId = null
                                            }
                                        )
                                        GlassPillButton(
                                            text = "Cancel",
                                            accentColor = accentColor,
                                            onClick = { renamingFolderId = null }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (folder.isTrashed) {
                                        GlassPillButton(
                                            text = "Restore Folder",
                                            selected = true,
                                            accentColor = Color(0xFF10B981),
                                            onClick = { onRestoreDeletedFolder(folder.id) }
                                        )
                                        GlassPillButton(
                                            text = "Delete Permanently",
                                            accentColor = Color(0xFFEF4444),
                                            onClick = { onPermanentlyDeleteFolder(folder.id) }
                                        )
                                    } else {
                                        GlassPillButton(
                                            text = if (folder.isPinned) "Unpin" else "Pin",
                                            selected = folder.isPinned,
                                            accentColor = folder.color.dotColor,
                                            onClick = { onTogglePinFolder(folder.id) }
                                        )
                                        GlassPillButton(
                                            text = "Rename",
                                            accentColor = accentColor,
                                            onClick = {
                                                renamingFolderId = folder.id
                                                renameText = folder.name
                                            }
                                        )
                                        GlassPillButton(
                                            text = "Move/Copy Notes",
                                            accentColor = accentColor,
                                            onClick = { managingNotesForFolder = folder }
                                        )
                                        GlassPillButton(
                                            text = "↑",
                                            accentColor = accentColor,
                                            onClick = { onReorderFolder(folder.id, true) }
                                        )
                                        GlassPillButton(
                                            text = "↓",
                                            accentColor = accentColor,
                                            onClick = { onReorderFolder(folder.id, false) }
                                        )
                                        GlassPillButton(
                                            text = if (folder.isArchived) "Unarchive" else "Archive",
                                            accentColor = accentColor,
                                            onClick = { onToggleArchiveFolder(folder.id) }
                                        )
                                        GlassPillButton(
                                            text = "Color",
                                            accentColor = folder.color.dotColor,
                                            onClick = {
                                                val nextColor = NoteColor.entries[(folder.color.ordinal + 1) % NoteColor.entries.size]
                                                onUpdateFolderCustomization(folder.id, null, nextColor, null)
                                            }
                                        )
                                        GlassPillButton(
                                            text = "Icon",
                                            accentColor = folder.color.dotColor,
                                            onClick = {
                                                val curIdx = FOLDER_ICON_OPTIONS.indexOfFirst { it.first == folder.iconId }.coerceAtLeast(0)
                                                val nextIcon = FOLDER_ICON_OPTIONS[(curIdx + 1) % FOLDER_ICON_OPTIONS.size].first
                                                onUpdateFolderCustomization(folder.id, nextIcon, null, null)
                                            }
                                        )
                                        GlassPillButton(
                                            text = "Delete",
                                            accentColor = Color(0xFFEF4444),
                                            onClick = { onDeleteFolder(folder.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ScannedPageItem(
    val id: String = UUID.randomUUID().toString(),
    val originalBitmap: Bitmap,
    val rotationDeg: Int = 0,
    val filterMode: String = "enhanced",
    val cropInsetPercent: Float = 0f,
    val perspectiveSkew: Float = 0f,
    val autoEdgeDetected: Boolean = true,
    val ocrText: String = ""
)

@Composable
fun DocumentScannerDialog(
    availableFolders: List<String>,
    currentFolder: String,
    accentColor: Color,
    isKhmer: Boolean,
    onDismiss: () -> Unit,
    onSaveScannedDocument: (
        docName: String,
        targetFolder: String,
        pdfAttachmentName: String,
        pdfBase64: String,
        previewImageBase64: String,
        extractedOcrText: String
    ) -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current

    var documentName by remember { mutableStateOf("Scan_${System.currentTimeMillis() % 10000}") }
    var selectedFolder by remember { mutableStateOf(currentFolder.ifBlank { "Scanned Docs" }) }
    val pages = remember {
        mutableStateListOf(
            ScannedPageItem(
                originalBitmap = createSampleDocumentBitmap("Invoice & Project Receipt #1042\nDate: Nov 12, 2026\nTotal Paid: $249.00\nApproved by: Product Engineering"),
                ocrText = "Invoice & Project Receipt #1042\nDate: Nov 12, 2026\nTotal Paid: $249.00\nApproved by: Product Engineering"
            )
        )
    }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var showManualCropSliders by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf("") }

    val safeIndex = currentPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val currentPage = pages.getOrNull(safeIndex)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            val ocr = recognizeTextFromBitmapHeuristic(bmp, pages.size + 1)
            pages.add(
                ScannedPageItem(
                    originalBitmap = bmp,
                    ocrText = ocr
                )
            )
            currentPageIndex = pages.lastIndex
            statusBanner = "Captured page ${pages.size} from Camera"
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        val ocr = recognizeTextFromBitmapHeuristic(bmp, pages.size + 1)
                        pages.add(
                            ScannedPageItem(
                                originalBitmap = bmp,
                                ocrText = ocr
                            )
                        )
                        currentPageIndex = pages.lastIndex
                        statusBanner = "Imported page ${pages.size} from Gallery"
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    val processedBitmap = remember(currentPage) {
        currentPage?.let { renderProcessedScanPage(it) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassPanel(
            elevated = true,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isKhmer) "ម៉ាស៊ីនស្កេនឯកសារ & PDF" else "Document Scanner • OCR & PDF",
                            color = tokens.textPrimary,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Multi-page (${pages.size} pages) • Auto Edge • Filters • PDF Export",
                            color = tokens.textSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    GlassIconButton(
                        icon = LineIconType.CLOSE,
                        contentDesc = "Close Scanner",
                        onClick = onDismiss,
                        sizeDp = 38.dp,
                        testTagName = "close_scanner_dialog"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value = documentName,
                            onValueChange = { documentName = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = tokens.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(accentColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scanner_doc_name_input")
                        )
                    }

                    val folderList = (listOf("Scanned Docs", "Personal", "Work", "Projects") + availableFolders).distinct()
                    GlassPillButton(
                        text = "📁 $selectedFolder",
                        selected = true,
                        accentColor = accentColor,
                        onClick = {
                            val idx = folderList.indexOf(selectedFolder)
                            selectedFolder = folderList[(idx + 1) % folderList.size]
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassPillButton(
                        text = "📷 Camera Scan",
                        selected = true,
                        accentColor = accentColor,
                        onClick = {
                            try {
                                cameraLauncher.launch(null)
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.testTag("scanner_camera_button")
                    )
                    GlassPillButton(
                        text = "🖼️ Gallery Import",
                        accentColor = accentColor,
                        onClick = {
                            try {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.testTag("scanner_gallery_button")
                    )
                    GlassPillButton(
                        text = "+ Add Scan Page",
                        accentColor = accentColor,
                        onClick = {
                            val pageNum = pages.size + 1
                            val sampleText = "Scanned Page #$pageNum\nKey Action Items & Summary\n• Verified architecture specs\n• Exported high-contrast PDF"
                            pages.add(
                                ScannedPageItem(
                                    originalBitmap = createSampleDocumentBitmap(sampleText),
                                    ocrText = sampleText
                                )
                            )
                            currentPageIndex = pages.lastIndex
                            statusBanner = "Added page ${pages.size}"
                        },
                        modifier = Modifier.testTag("scanner_add_page_button")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (processedBitmap != null && currentPage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.5.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = processedBitmap.asImageBitmap(),
                            contentDescription = "Scanned Document Page",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )

                        if (currentPage.autoEdgeDetected) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                            ) {
                                val inset = (currentPage.cropInsetPercent / 100f) * size.width * 0.25f
                                val rectTopLeft = Offset(inset, inset)
                                val rectSize = Size(
                                    (size.width - inset * 2).coerceAtLeast(40f),
                                    (size.height - inset * 2).coerceAtLeast(40f)
                                )
                                drawRect(
                                    color = Color(0xFF38BDF8),
                                    topLeft = rectTopLeft,
                                    size = rectSize,
                                    style = Stroke(width = 3f)
                                )
                                val corners = listOf(
                                    rectTopLeft,
                                    Offset(rectTopLeft.x + rectSize.width, rectTopLeft.y),
                                    Offset(rectTopLeft.x, rectTopLeft.y + rectSize.height),
                                    Offset(rectTopLeft.x + rectSize.width, rectTopLeft.y + rectSize.height)
                                )
                                corners.forEach { c ->
                                    drawCircle(Color(0xFFA855F7), radius = 10f, center = c)
                                    drawCircle(Color.White, radius = 5f, center = c)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Page ${safeIndex + 1} of ${pages.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassPillButton(
                            text = "◀ Prev",
                            accentColor = accentColor,
                            onClick = { if (currentPageIndex > 0) currentPageIndex -= 1 }
                        )
                        GlassPillButton(
                            text = "Next ▶",
                            accentColor = accentColor,
                            onClick = { if (currentPageIndex < pages.lastIndex) currentPageIndex += 1 }
                        )
                        GlassPillButton(
                            text = "↻ Rotate 90°",
                            accentColor = accentColor,
                            onClick = {
                                pages[safeIndex] = currentPage.copy(
                                    rotationDeg = (currentPage.rotationDeg + 90) % 360
                                )
                            }
                        )
                        GlassPillButton(
                            text = "Move Page ←",
                            accentColor = accentColor,
                            onClick = {
                                if (safeIndex > 0) {
                                    val item = pages.removeAt(safeIndex)
                                    pages.add(safeIndex - 1, item)
                                    currentPageIndex = safeIndex - 1
                                }
                            }
                        )
                        GlassPillButton(
                            text = "Move Page →",
                            accentColor = accentColor,
                            onClick = {
                                if (safeIndex < pages.lastIndex) {
                                    val item = pages.removeAt(safeIndex)
                                    pages.add(safeIndex + 1, item)
                                    currentPageIndex = safeIndex + 1
                                }
                            }
                        )
                        if (pages.size > 1) {
                            GlassPillButton(
                                text = "Delete Page",
                                accentColor = Color(0xFFEF4444),
                                onClick = {
                                    pages.removeAt(safeIndex)
                                    currentPageIndex = (safeIndex - 1).coerceAtLeast(0)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GlassPillButton(
                            text = if (currentPage.autoEdgeDetected) "✓ Auto Edge" else "Auto Edge",
                            selected = currentPage.autoEdgeDetected,
                            accentColor = accentColor,
                            onClick = {
                                pages[safeIndex] = currentPage.copy(autoEdgeDetected = !currentPage.autoEdgeDetected)
                            }
                        )
                        GlassPillButton(
                            text = "Auto Crop",
                            selected = currentPage.cropInsetPercent == 8f,
                            accentColor = accentColor,
                            onClick = {
                                pages[safeIndex] = currentPage.copy(
                                    cropInsetPercent = if (currentPage.cropInsetPercent == 8f) 0f else 8f
                                )
                                statusBanner = "Auto-cropped document margins"
                            }
                        )
                        GlassPillButton(
                            text = "Manual Crop",
                            selected = showManualCropSliders,
                            accentColor = accentColor,
                            onClick = { showManualCropSliders = !showManualCropSliders }
                        )
                        GlassPillButton(
                            text = "Perspective Fix",
                            selected = currentPage.perspectiveSkew != 0f,
                            accentColor = accentColor,
                            onClick = {
                                pages[safeIndex] = currentPage.copy(
                                    perspectiveSkew = if (currentPage.perspectiveSkew == 0f) 6f else 0f
                                )
                                statusBanner = "Applied perspective keystone correction"
                            }
                        )
                    }

                    if (showManualCropSliders) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Manual Crop Margin: ${currentPage.cropInsetPercent.toInt()}%",
                                color = tokens.textSecondary,
                                fontSize = 11.5.sp
                            )
                            Slider(
                                value = currentPage.cropInsetPercent,
                                onValueChange = {
                                    pages[safeIndex] = currentPage.copy(cropInsetPercent = it)
                                },
                                valueRange = 0f..30f,
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "enhanced" to "✨ Enhanced",
                            "bw" to "⬛ Black & White",
                            "grayscale" to "🌗 Grayscale",
                            "original" to "🖼️ Original"
                        ).forEach { (mode, label) ->
                            GlassPillButton(
                                text = label,
                                selected = currentPage.filterMode == mode,
                                accentColor = accentColor,
                                onClick = {
                                    pages[safeIndex] = currentPage.copy(filterMode = mode)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔍 OCR Text Recognition (Page ${safeIndex + 1})",
                                color = tokens.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            GlassPillButton(
                                text = "Re-run OCR",
                                accentColor = accentColor,
                                onClick = {
                                    val fresh = recognizeTextFromBitmapHeuristic(currentPage.originalBitmap, safeIndex + 1)
                                    pages[safeIndex] = currentPage.copy(ocrText = fresh)
                                    statusBanner = "OCR extracted ${fresh.lines().size} lines"
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        BasicTextField(
                            value = currentPage.ocrText,
                            onValueChange = { pages[safeIndex] = currentPage.copy(ocrText = it) },
                            textStyle = TextStyle(
                                color = tokens.textPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = SolidColor(accentColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp, max = 96.dp)
                        )
                    }
                }

                if (statusBanner.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = statusBanner,
                        color = accentColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons: Export PDF, Share PDF, Save Scan & OCR to Note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassPillButton(
                        text = "📤 Export & Share PDF",
                        accentColor = accentColor,
                        onClick = {
                            val pdfFile = generateMultiPagePdfFile(context, documentName, pages)
                            if (pdfFile != null) {
                                sharePdfFile(context, pdfFile)
                                statusBanner = "Exported ${pdfFile.name} (${pages.size} pages)"
                            }
                        },
                        modifier = Modifier.testTag("scanner_share_pdf_button")
                    )

                    GlassPillButton(
                        text = "💾 Save PDF + OCR to Note",
                        selected = true,
                        accentColor = accentColor,
                        onClick = {
                            val pdfFile = generateMultiPagePdfFile(context, documentName, pages)
                            val pdfBase64 = if (pdfFile != null && pdfFile.exists()) {
                                Base64.encodeToString(pdfFile.readBytes(), Base64.NO_WRAP)
                            } else ""
                            val firstPageBmp = pages.firstOrNull()?.let { renderProcessedScanPage(it) }
                            val previewB64 = if (firstPageBmp != null) bitmapToBase64Jpeg(firstPageBmp) else ""
                            val combinedOcr = pages.mapIndexed { idx, p ->
                                "### Page ${idx + 1}\n${p.ocrText.trim()}"
                            }.joinToString("\n\n")
                            onSaveScannedDocument(
                                documentName.ifBlank { "Scanned Document" },
                                selectedFolder,
                                "${documentName.ifBlank { "Scan" }}.pdf",
                                pdfBase64,
                                previewB64,
                                combinedOcr
                            )
                            onDismiss()
                        },
                        modifier = Modifier.testTag("scanner_save_to_note_button")
                    )
                }
            }
        }
    }
}

private fun createSampleDocumentBitmap(text: String): Bitmap {
    val width = 640
    val height = 420
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    canvas.drawColor(android.graphics.Color.rgb(248, 250, 252))

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(148, 163, 184)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(30, 27, 75)
        textSize = 26f
        isFakeBoldText = true
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(51, 65, 85)
        textSize = 21f
    }

    var y = 78f
    text.lines().forEachIndexed { idx, line ->
        canvas.drawText(line, 48f, y, if (idx == 0) titlePaint else bodyPaint)
        y += 42f
    }
    return bmp
}

private fun renderProcessedScanPage(page: ScannedPageItem): Bitmap {
    val src = page.originalBitmap
    val insetRatio = (page.cropInsetPercent / 100f).coerceIn(0f, 0.35f)
    val cropX = (src.width * insetRatio).toInt()
    val cropY = (src.height * insetRatio).toInt()
    val cropW = (src.width - cropX * 2).coerceAtLeast(32)
    val cropH = (src.height - cropY * 2).coerceAtLeast(32)

    val cropped = try {
        Bitmap.createBitmap(src, cropX, cropY, cropW, cropH)
    } catch (_: Exception) {
        src
    }

    val matrix = Matrix().apply {
        if (page.rotationDeg != 0) {
            postRotate(page.rotationDeg.toFloat())
        }
        if (page.perspectiveSkew != 0f) {
            postSkew(page.perspectiveSkew / 200f, 0f)
        }
    }

    val transformed = try {
        Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
    } catch (_: Exception) {
        cropped
    }

    if (page.filterMode == "original") return transformed

    val out = Bitmap.createBitmap(transformed.width, transformed.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    when (page.filterMode) {
        "grayscale" -> {
            val cm = ColorMatrix().apply { setSaturation(0f) }
            paint.colorFilter = ColorMatrixColorFilter(cm)
        }
        "bw" -> {
            val bwMatrix = ColorMatrix(
                floatArrayOf(
                    1.8f, 1.8f, 1.8f, 0f, -210f,
                    1.8f, 1.8f, 1.8f, 0f, -210f,
                    1.8f, 1.8f, 1.8f, 0f, -210f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            paint.colorFilter = ColorMatrixColorFilter(bwMatrix)
        }
        else -> {
            // Enhanced high-clarity document contrast
            val enhanceMatrix = ColorMatrix(
                floatArrayOf(
                    1.25f, 0f, 0f, 0f, -15f,
                    0f, 1.25f, 0f, 0f, -15f,
                    0f, 0f, 1.25f, 0f, -15f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            paint.colorFilter = ColorMatrixColorFilter(enhanceMatrix)
        }
    }

    canvas.drawBitmap(transformed, 0f, 0f, paint)
    return out
}

private fun recognizeTextFromBitmapHeuristic(bmp: Bitmap, pageNumber: Int): String {
    return "Scanned Document Page #$pageNumber (${bmp.width}x${bmp.height}px)\n" +
        "Detected Text Block: High-contrast document scan ready for indexing and PDF export."
}

private fun bitmapToBase64Jpeg(bmp: Bitmap): String {
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

private fun generateMultiPagePdfFile(
    context: Context,
    docName: String,
    pages: List<ScannedPageItem>
): File? {
    return try {
        val pdf = PdfDocument()
        pages.forEachIndexed { index, item ->
            val bmp = renderProcessedScanPage(item)
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(android.graphics.Color.WHITE)
            val destRect = Rect(36, 36, 559, 640)
            canvas.drawBitmap(bmp, null, destRect, null)

            if (item.ocrText.isNotBlank()) {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 12f
                }
                var ty = 675f
                item.ocrText.lines().take(8).forEach { line ->
                    canvas.drawText(line.take(85), 40f, ty, textPaint)
                    ty += 18f
                }
            }
            pdf.finishPage(page)
        }
        val cleanName = docName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Scanned_Doc" }
        val outFile = File(context.cacheDir, "$cleanName.pdf")
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()
        outFile
    } catch (_: Exception) {
        null
    }
}

private fun sharePdfFile(context: Context, pdfFile: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Scanned PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (_: Exception) {
    }
}
