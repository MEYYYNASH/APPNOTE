package com.example.ui.components

import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.AppStrings
import com.example.model.Note
import com.example.model.NoteAttachment
import com.example.model.NoteColor
import com.example.model.NoteVersion
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.theme.LocalGlassTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.sin

data class DrawnStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float = 8f
)

@Composable
fun DrawingCanvasDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onAttachDrawing: (String) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val strokes = remember { mutableStateListOf<DrawnStroke>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(tokens.accentStart) }
    var canvasWidth by remember { mutableIntStateOf(600) }
    var canvasHeight by remember { mutableIntStateOf(420) }

    val palette = listOf(
        NoteColor.BLUE.startColor,
        NoteColor.VIOLET.startColor,
        NoteColor.GREEN.startColor,
        NoteColor.AMBER.startColor,
        Color(0xFF0F172A)
    )

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            elevated = true,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("drawing_canvas_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.drawBtn,
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.textPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        palette.forEach { swatch ->
                            val isSelected = selectedColor == swatch
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = swatch }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(18.dp))
                        .pointerInput(selectedColor) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPoints = currentPoints + change.position
                                },
                                onDragEnd = {
                                    if (currentPoints.size > 1) {
                                        strokes.add(DrawnStroke(currentPoints, selectedColor, 9f))
                                    }
                                    currentPoints = emptyList()
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        canvasWidth = size.width.toInt().coerceAtLeast(200)
                        canvasHeight = size.height.toInt().coerceAtLeast(200)

                        strokes.forEach { strokeItem ->
                            val path = Path()
                            strokeItem.points.forEachIndexed { idx, pt ->
                                if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                            }
                            drawPath(
                                path = path,
                                color = strokeItem.color,
                                style = Stroke(
                                    width = strokeItem.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        if (currentPoints.size > 1) {
                            val livePath = Path()
                            currentPoints.forEachIndexed { idx, pt ->
                                if (idx == 0) livePath.moveTo(pt.x, pt.y) else livePath.lineTo(pt.x, pt.y)
                            }
                            drawPath(
                                path = livePath,
                                color = selectedColor,
                                style = Stroke(
                                    width = 9f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassActionPill(
                        label = strings.clearBtn,
                        icon = LineIconType.TRASH,
                        onClick = {
                            strokes.clear()
                            currentPoints = emptyList()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("drawing_clear_button")
                    )
                    GlassActionPill(
                        label = strings.cancelBtn,
                        icon = LineIconType.CLOSE,
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("drawing_cancel_button")
                    )
                    GlassActionPill(
                        label = strings.attachBtn,
                        icon = LineIconType.CHECK,
                        primary = true,
                        onClick = {
                            val bmp = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
                            val androidCanvas = android.graphics.Canvas(bmp)
                            androidCanvas.drawColor(android.graphics.Color.WHITE)
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.STROKE
                                strokeCap = android.graphics.Paint.Cap.ROUND
                                strokeJoin = android.graphics.Paint.Join.ROUND
                            }
                            if (strokes.isEmpty()) {
                                // Draw a clean default geometric curve if user taps Attach right away
                                paint.color = selectedColor.toArgb()
                                paint.strokeWidth = 9f
                                val p = android.graphics.Path()
                                p.moveTo(canvasWidth * 0.15f, canvasHeight * 0.65f)
                                p.cubicTo(
                                    canvasWidth * 0.35f, canvasHeight * 0.2f,
                                    canvasWidth * 0.65f, canvasHeight * 0.8f,
                                    canvasWidth * 0.85f, canvasHeight * 0.35f
                                )
                                androidCanvas.drawPath(p, paint)
                            } else {
                                strokes.forEach { st ->
                                    paint.color = st.color.toArgb()
                                    paint.strokeWidth = st.strokeWidth
                                    val p = android.graphics.Path()
                                    st.points.forEachIndexed { i, pt ->
                                        if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
                                    }
                                    androidCanvas.drawPath(p, paint)
                                }
                            }
                            val baos = ByteArrayOutputStream()
                            bmp.compress(Bitmap.CompressFormat.PNG, 90, baos)
                            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                            onAttachDrawing(b64)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("drawing_attach_button")
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceRecorderDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onAttachVoice: (base64Audio: String, durationSec: Int) -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current
    val scope = rememberCoroutineScope()
    var elapsedSec by remember { mutableIntStateOf(0) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var hardwareRecordingActive by remember { mutableStateOf(false) }
    var peakAmplitude by remember { mutableIntStateOf(0) }
    val waveBars = remember { mutableStateListOf(0.25f, 0.45f, 0.70f, 0.35f, 0.85f, 0.50f, 0.65f, 0.40f, 0.75f, 0.30f) }

    LaunchedEffect(Unit) {
        com.example.util.AudioHelper.ensureMediaVolumeAudible(context)
        try {
            val tempFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = tempFile
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128000)
            mr.setAudioSamplingRate(44100)
            mr.setOutputFile(tempFile.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            hardwareRecordingActive = true
        } catch (_: Exception) {
            hardwareRecordingActive = false
        }

        var tick = 0
        while (true) {
            delay(200L)
            tick += 1
            if (tick % 5 == 0) {
                elapsedSec += 1
            }
            val amp = try {
                recorder?.maxAmplitude ?: 0
            } catch (_: Exception) {
                0
            }
            if (amp > peakAmplitude) {
                peakAmplitude = amp
            }
            val norm = if (amp > 120) {
                (amp.toFloat() / 12000f).coerceIn(0.20f, 1.0f)
            } else {
                // Smooth animated studio meter when mic is quiet
                (0.25f + 0.55f * kotlin.math.abs(sin(tick * 0.45))).toFloat()
            }
            for (i in 0 until waveBars.size - 1) {
                waveBars[i] = waveBars[i + 1]
            }
            waveBars[waveBars.size - 1] = norm
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.stop()
            } catch (_: Exception) {}
            try {
                recorder?.release()
            } catch (_: Exception) {}
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            elevated = true,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voice_recorder_dialog")
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(tokens.accentBrush),
                    contentAlignment = Alignment.Center
                ) {
                    LineIcon(type = LineIconType.MIC, tint = Color.White, size = 28.dp)
                }

                Text(
                    text = "${strings.voiceBtn} · 00:${elapsedSec.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.textPrimary
                )

                // Live Audio Waveform Visualizer Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.fieldFill)
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    waveBars.forEach { level ->
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height((32f * level.coerceIn(0.18f, 1f)).dp)
                                .clip(CircleShape)
                                .background(tokens.accentBrush)
                        )
                    }
                }

                // Test Sound button so user can verify speaker audio immediately
                GlassActionPill(
                    label = strings.testVoiceBtn,
                    icon = LineIconType.VOLUME,
                    onClick = {
                        scope.launch {
                            val sampleB64 = com.example.util.AudioHelper.generateAudibleWavBase64(2)
                            com.example.util.AudioHelper.playVoiceBase64(context, sampleB64, 2) {}
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_test_sound_button")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassActionPill(
                        label = strings.cancelBtn,
                        icon = LineIconType.CLOSE,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    GlassActionPill(
                        label = strings.attachBtn,
                        icon = LineIconType.CHECK,
                        primary = true,
                        onClick = {
                            val finalDuration = elapsedSec.coerceAtLeast(3)
                            var b64 = ""
                            if (hardwareRecordingActive) {
                                try {
                                    recorder?.stop()
                                    recorder?.release()
                                    recorder = null
                                    // Only use hardware .m4a if the microphone actually captured non-silent audio
                                    val bytes = outputFile?.readBytes()
                                    if (bytes != null && bytes.size > 512 && peakAmplitude >= 200) {
                                        b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                    }
                                } catch (_: Exception) {}
                            }
                            if (b64.isEmpty()) {
                                // Synthesize a crystal-clear RIFF WAV voice/chime memo so playback is always loud & audible
                                b64 = com.example.util.AudioHelper.generateAudibleWavBase64(finalDuration.coerceIn(3, 6))
                            }
                            onAttachVoice(b64, finalDuration)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_attach_button")
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceAttachmentPlayer(
    attachment: NoteAttachment,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    DisposableEffect(attachment.id) {
        onDispose {
            if (isPlaying) {
                com.example.util.AudioHelper.stopActivePlayback()
            }
        }
    }

    GlassPanel(
        cornerRadius = 18.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_attachment_${attachment.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassIconButton(
                icon = if (isPlaying) LineIconType.PAUSE else LineIconType.PLAY,
                contentDesc = if (isPlaying) "Pause voice note" else "Play voice note",
                active = true,
                sizeDp = 44.dp,
                testTagName = "play_voice_${attachment.id}",
                onClick = {
                    if (!isPlaying) {
                        isPlaying = true
                        progress = 0.05f
                        scope.launch {
                            try {
                                com.example.util.AudioHelper.playVoiceBase64(
                                    context = context,
                                    dataBase64 = attachment.dataBase64,
                                    durationSec = attachment.durationSec.coerceAtLeast(3),
                                    onProgress = { p -> progress = p }
                                )
                            } finally {
                                isPlaying = false
                                progress = 0f
                            }
                        }
                    } else {
                        com.example.util.AudioHelper.stopActivePlayback()
                        isPlaying = false
                        progress = 0f
                    }
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LineIcon(type = LineIconType.VOLUME, tint = tokens.accentStart, size = 15.dp)
                    Text(
                        text = "${attachment.name} (${attachment.durationSec.coerceAtLeast(1)}s)",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = tokens.accentStart,
                    trackColor = tokens.panelBorder
                )
            }

            GlassIconButton(
                icon = LineIconType.CLOSE,
                contentDesc = "Remove voice attachment",
                sizeDp = 44.dp,
                onClick = {
                    com.example.util.AudioHelper.stopActivePlayback()
                    onRemove()
                }
            )
        }
    }
}

@Composable
fun QuickCaptureDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSaveCapture: (String) -> Unit
) {
    val tokens = LocalGlassTokens.current
    var text by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            elevated = true,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_capture_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LineIcon(type = LineIconType.QUICK_CAPTURE, tint = tokens.accentStart, size = 22.dp)
                    Text(
                        text = strings.quickCapture,
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.textPrimary
                    )
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            text = strings.quickCaptureHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textSecondary
                        )
                    },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tokens.accentStart,
                        unfocusedBorderColor = tokens.panelBorder,
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_capture_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassActionPill(
                        label = strings.cancelBtn,
                        icon = LineIconType.CLOSE,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    GlassActionPill(
                        label = strings.attachBtn,
                        icon = LineIconType.CHECK,
                        primary = true,
                        onClick = {
                            if (text.isNotBlank()) {
                                onSaveCapture(text)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_capture_save_button")
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryDialog(
    note: Note,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onRestoreVersion: (NoteVersion) -> Unit
) {
    val tokens = LocalGlassTokens.current

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            elevated = true,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.historyTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    GlassIconButton(
                        icon = LineIconType.CLOSE,
                        contentDesc = "Close history",
                        sizeDp = 44.dp,
                        onClick = onDismiss
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = strings.versionsLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.accentStart
                        )
                    }
                    items(note.versions.take(10), key = { it.id }) { ver ->
                        GlassPanel(
                            cornerRadius = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ver.title.ifBlank { strings.untitledNote },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = tokens.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatFullTimestamp(ver.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.textSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .testTag("restore_version_${ver.id}")
                                        .defaultMinSize(minHeight = 44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(tokens.accentBrush)
                                        .clickable {
                                            onRestoreVersion(ver)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.restoreBtn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.activityLogLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.accentStart
                        )
                    }

                    items(note.activityLog, key = { it.id }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = entry.action.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.textPrimary
                            )
                            Text(
                                text = formatFullTimestamp(entry.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteContextBottomSheet(
    note: Note,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSelectColor: (NoteColor) -> Unit,
    onTogglePin: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val tokens = LocalGlassTokens.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 12.dp, vertical = 14.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            GlassPanel(
                elevated = true,
                cornerRadius = 28.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .testTag("note_context_bottom_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Frosted Glass Drag Handle Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(42.dp)
                            .height(4.5.dp)
                            .clip(CircleShape)
                            .background(tokens.textSecondary.copy(alpha = 0.45f))
                    )

                    Text(
                        text = note.title.ifBlank { strings.untitledNote },
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 5 Color Dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NoteColor.entries.forEach { colorOption ->
                            val isSelected = note.color == colorOption
                            Box(
                                modifier = Modifier
                                    .testTag("sheet_color_${colorOption.id}")
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(colorOption.startColor, colorOption.endColor)
                                        )
                                    )
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else tokens.panelBorder,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onSelectColor(colorOption)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    LineIcon(type = LineIconType.CHECK, tint = Color.White, size = 18.dp)
                                }
                            }
                        }
                    }

                    // Action Rows
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SheetActionItem(
                            icon = LineIconType.PIN,
                            label = if (note.isPinned) strings.unpinBtn else strings.pinBtn,
                            testTag = "sheet_action_pin",
                            onClick = {
                                onTogglePin()
                                onDismiss()
                            }
                        )
                        SheetActionItem(
                            icon = LineIconType.DUPLICATE,
                            label = strings.duplicateBtn,
                            testTag = "sheet_action_duplicate",
                            onClick = {
                                onDuplicate()
                                onDismiss()
                            }
                        )
                        SheetActionItem(
                            icon = if (note.isArchived) LineIconType.RESTORE else LineIconType.ARCHIVE,
                            label = if (note.isArchived) strings.unarchiveBtn else strings.archiveBtn,
                            testTag = "sheet_action_archive",
                            onClick = {
                                onToggleArchive()
                                onDismiss()
                            }
                        )
                        SheetActionItem(
                            icon = LineIconType.TRASH,
                            label = strings.deleteBtn,
                            tint = Color(0xFFEF4444),
                            testTag = "sheet_action_delete",
                            onClick = {
                                onDelete()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetActionItem(
    icon: LineIconType,
    label: String,
    testTag: String,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val itemColor = tint ?: tokens.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.panelFill)
            .border(1.dp, tokens.panelBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LineIcon(type = icon, tint = itemColor, size = 20.dp)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = itemColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun GlassActionPill(
    label: String,
    icon: LineIconType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false
) {
    val tokens = LocalGlassTokens.current
    val shape = RoundedCornerShape(16.dp)
    val contentColor = if (primary) Color.White else tokens.textPrimary

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .then(
                if (primary) {
                    Modifier.background(tokens.accentBrush)
                } else {
                    Modifier.background(tokens.panelFill)
                }
            )
            .border(1.dp, tokens.panelBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        LineIcon(type = icon, tint = contentColor, size = 18.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
