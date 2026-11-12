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
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("photo_crop_dialog")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LineIcon(type = LineIconType.CROP, tint = tokens.accentStart, size = 20.dp)
                        Text(
                            text = strings.cropPhotoTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.textPrimary
                        )
                    }

                    // Rotate 90° button
                    Box(
                        modifier = Modifier
                            .testTag("crop_rotate_button")
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(12.dp))
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
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LineIcon(type = LineIconType.ROTATE, tint = tokens.textPrimary, size = 16.dp)
                            Text(
                                text = strings.rotateBtn,
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textPrimary
                            )
                        }
                    }
                }

                // Aspect ratio preset chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Free", "1:1", "4:3", "16:9").forEach { ratio ->
                        val selected = activePreset == ratio
                        Box(
                            modifier = Modifier
                                .testTag("crop_ratio_$ratio")
                                .clip(CircleShape)
                                .then(
                                    if (selected) Modifier.background(tokens.accentBrush)
                                    else Modifier.background(tokens.fieldFill)
                                )
                                .border(1.dp, tokens.panelBorder, CircleShape)
                                .clickable { applyAspectPreset(ratio) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
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

                // Interactive Crop Canvas Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0B101B))
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(18.dp))
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
                        val maskColor = Color.Black.copy(alpha = 0.52f)
                        drawRect(maskColor, Offset(0f, 0f), Size(w, t))
                        drawRect(maskColor, Offset(0f, b), Size(w, h - b))
                        drawRect(maskColor, Offset(0f, t), Size(l, ch))
                        drawRect(maskColor, Offset(r, t), Size(w - r, ch))

                        // Crop rectangle border
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(l, t),
                            size = Size(cw, ch),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Rule of thirds lines
                        val gridColor = Color.White.copy(alpha = 0.45f)
                        val thirdW = cw / 3f
                        val thirdH = ch / 3f
                        drawLine(gridColor, Offset(l + thirdW, t), Offset(l + thirdW, b), 1.dp.toPx())
                        drawLine(gridColor, Offset(l + thirdW * 2f, t), Offset(l + thirdW * 2f, b), 1.dp.toPx())
                        drawLine(gridColor, Offset(l, t + thirdH), Offset(r, t + thirdH), 1.dp.toPx())
                        drawLine(gridColor, Offset(l, t + thirdH * 2f), Offset(r, t + thirdH * 2f), 1.dp.toPx())

                        // 4 Corner handles
                        val handleRadius = 7.dp.toPx()
                        listOf(
                            Offset(l, t),
                            Offset(r, t),
                            Offset(l, b),
                            Offset(r, b)
                        ).forEach { corner ->
                            drawCircle(Color.White, radius = handleRadius, center = corner)
                            drawCircle(tokens.accentStart, radius = handleRadius * 0.6f, center = corner)
                        }
                    }
                }

                // Actions: Cancel & Crop & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
    onSetReminder: (Long?) -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            elevated = true,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reminder_picker_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
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
                        sizeDp = 40.dp,
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
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                    onSetReminder(null)
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
                            .defaultMinSize(minHeight = 46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                onSetReminder(targetMillis)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
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
                                        onSetReminder(picked.timeInMillis)
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
