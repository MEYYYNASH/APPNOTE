package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AppStrings
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.theme.LocalGlassTokens
import com.example.util.AudioHelper
import com.example.util.ReminderScheduler
import java.io.ByteArrayOutputStream
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PhotoCropDialog(
    initialBitmap: Bitmap,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onCropConfirmed: (String) -> Unit
) {
    val tokens = LocalGlassTokens.current
    var currentBitmap by remember(initialBitmap) { mutableStateOf(initialBitmap) }

    // Normalized crop bounds in [0f..1f]
    var cropLeft by remember { mutableFloatStateOf(0.08f) }
    var cropTop by remember { mutableFloatStateOf(0.08f) }
    var cropRight by remember { mutableFloatStateOf(0.92f) }
    var cropBottom by remember { mutableFloatStateOf(0.92f) }
    var activePreset by remember { mutableStateOf("Free") }

    fun applyAspectPreset(preset: String) {
        activePreset = preset
        when (preset) {
            "Free" -> {
                cropLeft = 0.08f
                cropTop = 0.08f
                cropRight = 0.92f
                cropBottom = 0.92f
            }
            "1:1" -> {
                cropLeft = 0.15f
                cropTop = 0.15f
                cropRight = 0.85f
                cropBottom = 0.85f
            }
            "4:3" -> {
                cropLeft = 0.10f
                cropTop = 0.16f
                cropRight = 0.90f
                cropBottom = 0.84f
            }
            "16:9" -> {
                cropLeft = 0.06f
                cropTop = 0.24f
                cropRight = 0.94f
                cropBottom = 0.76f
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            elevated = true,
            cornerRadius = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("photo_crop_dialog")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header (Sleek flat design)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LineIcon(type = LineIconType.CROP, tint = tokens.accentStart, size = 18.dp)
                        Text(
                            text = strings.cropPhotoTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = tokens.textPrimary
                        )
                    }

                    // Rotate 90° button
                    Box(
                        modifier = Modifier
                            .testTag("crop_rotate_button")
                            .clip(RoundedCornerShape(10.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                val matrix = Matrix().apply { postRotate(90f) }
                                currentBitmap = Bitmap.createBitmap(
                                    currentBitmap,
                                    0,
                                    0,
                                    currentBitmap.width,
                                    currentBitmap.height,
                                    matrix,
                                    true
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            LineIcon(type = LineIconType.ROTATE, tint = tokens.textPrimary, size = 14.dp)
                            Text(
                                text = strings.rotateBtn,
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textPrimary
                            )
                        }
                    }
                }

                // Aspect ratio preset chips (Flat & Clean)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Free", "1:1", "4:3", "16:9").forEach { ratio ->
                        val selected = activePreset == ratio
                        Box(
                            modifier = Modifier
                                .testTag("crop_ratio_$ratio")
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (selected) Modifier.background(tokens.accentBrush)
                                    else Modifier.background(tokens.fieldFill)
                                )
                                .border(1.dp, if (selected) tokens.accentStart else tokens.panelBorder, RoundedCornerShape(10.dp))
                                .clickable { applyAspectPreset(ratio) }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = ratio,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color.White else tokens.textPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Interactive Crop Canvas Container (Sleek flat preview)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF070B14))
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            var dragTarget = 0 // 0=center pan, 1=TL, 2=TR, 3=BL, 4=BR
                            detectDragGestures(
                                onDragStart = { pos ->
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val nx = pos.x / w
                                    val ny = pos.y / h
                                    val thresh = 0.14f
                                    dragTarget = when {
                                        abs(nx - cropLeft) < thresh && abs(ny - cropTop) < thresh -> 1
                                        abs(nx - cropRight) < thresh && abs(ny - cropTop) < thresh -> 2
                                        abs(nx - cropLeft) < thresh && abs(ny - cropBottom) < thresh -> 3
                                        abs(nx - cropRight) < thresh && abs(ny - cropBottom) < thresh -> 4
                                        else -> 0
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val dx = dragAmount.x / w
                                    val dy = dragAmount.y / h
                                    val minSpan = 0.18f

                                    when (dragTarget) {
                                        1 -> {
                                            cropLeft = (cropLeft + dx).coerceIn(0.02f, cropRight - minSpan)
                                            cropTop = (cropTop + dy).coerceIn(0.02f, cropBottom - minSpan)
                                        }
                                        2 -> {
                                            cropRight = (cropRight + dx).coerceIn(cropLeft + minSpan, 0.98f)
                                            cropTop = (cropTop + dy).coerceIn(0.02f, cropBottom - minSpan)
                                        }
                                        3 -> {
                                            cropLeft = (cropLeft + dx).coerceIn(0.02f, cropRight - minSpan)
                                            cropBottom = (cropBottom + dy).coerceIn(cropTop + minSpan, 0.98f)
                                        }
                                        4 -> {
                                            cropRight = (cropRight + dx).coerceIn(cropLeft + minSpan, 0.98f)
                                            cropBottom = (cropBottom + dy).coerceIn(cropTop + minSpan, 0.98f)
                                        }
                                        else -> {
                                            val bw = cropRight - cropLeft
                                            val bh = cropBottom - cropTop
                                            val nextL = (cropLeft + dx).coerceIn(0.02f, 0.98f - bw)
                                            val nextT = (cropTop + dy).coerceIn(0.02f, 0.98f - bh)
                                            cropLeft = nextL
                                            cropRight = nextL + bw
                                            cropTop = nextT
                                            cropBottom = nextT + bh
                                        }
                                    }
                                }
                            )
                        }
                        .testTag("photo_crop_canvas")
                ) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Photo to crop",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val l = cropLeft * w
                        val t = cropTop * h
                        val r = cropRight * w
                        val b = cropBottom * h
                        val cw = (r - l).coerceAtLeast(10f)
                        val ch = (b - t).coerceAtLeast(10f)

                        // Dimmed outside regions
                        val maskColor = Color.Black.copy(alpha = 0.65f)
                        drawRect(maskColor, Offset(0f, 0f), Size(w, t))
                        drawRect(maskColor, Offset(0f, b), Size(w, h - b))
                        drawRect(maskColor, Offset(0f, t), Size(l, ch))
                        drawRect(maskColor, Offset(r, t), Size(w - r, ch))

                        // Crop rectangle border (Sleek flat white)
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(l, t),
                            size = Size(cw, ch),
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Rule of thirds lines
                        val gridColor = Color.White.copy(alpha = 0.35f)
                        val thirdW = cw / 3f
                        val thirdH = ch / 3f
                        drawLine(gridColor, Offset(l + thirdW, t), Offset(l + thirdW, b), 0.75.dp.toPx())
                        drawLine(gridColor, Offset(l + thirdW * 2f, t), Offset(l + thirdW * 2f, b), 0.75.dp.toPx())
                        drawLine(gridColor, Offset(l, t + thirdH), Offset(r, t + thirdH), 0.75.dp.toPx())
                        drawLine(gridColor, Offset(l, t + thirdH * 2f), Offset(r, t + thirdH * 2f), 0.75.dp.toPx())

                        // 4 Corner handles (Modern glowing styling)
                        val handleRadius = 6.dp.toPx()
                        listOf(
                            Offset(l, t),
                            Offset(r, t),
                            Offset(l, b),
                            Offset(r, b)
                        ).forEach { corner ->
                            drawCircle(Color.White, radius = handleRadius, center = corner)
                            drawCircle(tokens.accentStart, radius = handleRadius * 0.55f, center = corner)
                        }
                    }
                }

                // Actions: Cancel & Crop & Save (Sleek flat action pills)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassActionPill(
                        label = strings.cancelBtn,
                        icon = LineIconType.CLOSE,
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("crop_cancel_button")
                    )
                    GlassActionPill(
                        label = strings.cropSaveBtn,
                        icon = LineIconType.CHECK,
                        primary = true,
                        onClick = {
                            val bmpW = currentBitmap.width
                            val bmpH = currentBitmap.height
                            val x = (cropLeft * bmpW).roundToInt().coerceIn(0, (bmpW - 10).coerceAtLeast(0))
                            val y = (cropTop * bmpH).roundToInt().coerceIn(0, (bmpH - 10).coerceAtLeast(0))
                            val w = ((cropRight - cropLeft) * bmpW).roundToInt().coerceIn(10, bmpW - x)
                            val h = ((cropBottom - cropTop) * bmpH).roundToInt().coerceIn(10, bmpH - y)

                            val croppedBmp = try {
                                Bitmap.createBitmap(currentBitmap, x, y, w, h)
                            } catch (_: Exception) {
                                currentBitmap
                            }
                            val baos = ByteArrayOutputStream()
                            croppedBmp.compress(Bitmap.CompressFormat.JPEG, 88, baos)
                            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                            onCropConfirmed(b64)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("crop_confirm_button")
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderPickerDialog(
    currentReminderAt: Long?,
    strings: AppStrings,
    isKhmer: Boolean,
    onDismiss: () -> Unit,
    initialSoundStyle: String = "",
    initialAlertMode: String = "",
    initialVibrationPattern: String = "",
    onSetReminderWithSound: ((Long?, String?, String?, String?) -> Unit)? = null,
    onSetReminder: (Long?) -> Unit = {}
) {
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current

    var selectedSoundStyle by remember(initialSoundStyle) {
        mutableStateOf(initialSoundStyle.ifBlank { "default" })
    }
    var selectedAlertMode by remember(initialAlertMode) {
        mutableStateOf(initialAlertMode.ifBlank { "default" })
    }
    var selectedVibrationPattern by remember(initialVibrationPattern) {
        mutableStateOf(initialVibrationPattern.ifBlank { "default" })
    }

    fun applyReminder(targetMillis: Long?) {
        val soundArg = if (selectedSoundStyle == "default") "" else selectedSoundStyle
        val modeArg = if (selectedAlertMode == "default") "" else selectedAlertMode
        val vibArg = if (selectedVibrationPattern == "default") "" else selectedVibrationPattern
        if (onSetReminderWithSound != null) {
            onSetReminderWithSound(targetMillis, soundArg, modeArg, vibArg)
        } else {
            onSetReminder(targetMillis)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            elevated = true,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reminder_picker_dialog")
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LineIcon(type = LineIconType.BELL, tint = tokens.accentStart, size = 20.dp)
                        Text(
                            text = strings.reminderTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.textPrimary
                        )
                    }
                    GlassIconButton(
                        icon = LineIconType.CLOSE,
                        contentDesc = strings.cancelBtn,
                        sizeDp = 38.dp,
                        onClick = onDismiss
                    )
                }

                if (currentReminderAt != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.accentStart, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ReminderScheduler.formatReminderTime(currentReminderAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .testTag("clear_reminder_button")
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.16f))
                                .clickable {
                                    applyReminder(null)
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = strings.clearReminderBtn,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Per-Reminder Sound Style Selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.fieldFill)
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isKhmer) "សំឡេងរំលឹកសម្រាប់កំណត់ត្រានេះ" else "Per-Reminder Sound & Alert Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.accentStart,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(tokens.accentStart.copy(alpha = 0.18f))
                                .border(1.dp, tokens.accentStart, RoundedCornerShape(8.dp))
                                .clickable {
                                    val styleToPreview = if (selectedSoundStyle == "default") "glass_chime" else selectedSoundStyle
                                    AudioHelper.previewRingtoneStyle(
                                        context = context,
                                        styleId = styleToPreview,
                                        customSoundBase64 = "",
                                        volumePercent = 85
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("per_reminder_preview_sound_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                LineIcon(
                                    type = LineIconType.PLAY,
                                    tint = tokens.textPrimary,
                                    size = 12.dp
                                )
                                Text(
                                    text = if (isKhmer) "ស្តាប់សាកល្បង" else "Preview Sound",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Ringtone style horizontal chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val soundOptions = listOf(
                            "default" to "App Default",
                            "glass_chime" to "Glass Chime",
                            "crystal_bell" to "Crystal Bell",
                            "soft_marimba" to "Soft Marimba",
                            "cosmic_pulse" to "Cosmic Pulse",
                            "sunrise_harp" to "Sunrise Harp",
                            "zen_bowl" to "Zen Bowl",
                            "digital_beep" to "Digital Beep",
                            "default_ringtone" to "Phone Ringtone"
                        )
                        soundOptions.forEach { (id, label) ->
                            val selected = selectedSoundStyle == id
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) tokens.accentStart else Color.Transparent)
                                    .border(1.dp, if (selected) tokens.accentStart else tokens.panelBorder, CircleShape)
                                    .clickable { selectedSoundStyle = id }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White else tokens.textSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Alert Mode & Vibration Pattern horizontal chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val alertModes = listOf(
                            "default" to "Default Mode",
                            "sound_vibration" to "Sound + Vib",
                            "sound_only" to "Sound Only",
                            "vibration_only" to "Vibration Only",
                            "silent" to "Silent"
                        )
                        alertModes.forEach { (id, label) ->
                            val selected = selectedAlertMode == id
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) tokens.accentEnd.copy(alpha = 0.8f) else Color.Transparent)
                                    .border(1.dp, if (selected) tokens.accentEnd else tokens.panelBorder, CircleShape)
                                    .clickable { selectedAlertMode = id }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White else tokens.textSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                val now = System.currentTimeMillis()
                val presets = listOf(
                    (if (isKhmer) "ក្នុង 10 វិនាទី (សាកល្បងសំឡេងរំលឹក)" else "In 10 seconds (Test Alarm)") to (now + 10_000L),
                    (if (isKhmer) "ក្នុង 15 នាទី" else "In 15 minutes") to (now + 15L * 60_000L),
                    (if (isKhmer) "ក្នុង 1 ម៉ោង" else "In 1 hour") to (now + 60L * 60_000L),
                    (if (isKhmer) "ស្អែកម៉ោង 9:00 ព្រឹក" else "Tomorrow 9:00 AM") to run {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        cal.timeInMillis
                    }
                )

                presets.forEachIndexed { idx, (label, targetMillis) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                applyReminder(targetMillis)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("reminder_preset_$idx"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = ReminderScheduler.formatReminderTime(targetMillis),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = tokens.textSecondary
                        )
                    }
                }

                // Custom Date & Time Picker button
                GlassActionPill(
                    label = if (isKhmer) "ជ្រើសរើសថ្ងៃ និងម៉ោងផ្ទាល់ខ្លួន..." else "Pick custom date & time...",
                    icon = LineIconType.BELL,
                    primary = true,
                    onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val picked = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            set(Calendar.MINUTE, minute)
                                            set(Calendar.SECOND, 0)
                                        }
                                        applyReminder(picked.timeInMillis)
                                        onDismiss()
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_custom_datetime_button")
                )
            }
        }
    }
}

