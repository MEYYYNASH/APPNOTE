package com.example.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.ui.theme.LocalGlassTokens
import com.example.util.AudioHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomReminderSoundSettingsCard(
    settings: AppSettings,
    accentColor: Color,
    isKhmer: Boolean,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current
    val scope = rememberCoroutineScope()
    var pendingImportSource by remember { mutableStateOf("Device") }
    var isPlayingPreview by remember { mutableStateOf(false) }

    val soundImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var displayName = "Custom_Audio_${System.currentTimeMillis() % 1000}.wav"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            displayName = cursor.getString(idx) ?: displayName
                        }
                    }
                }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    val limitedBytes = if (bytes.size > 450_000) bytes.copyOf(450_000) else bytes
                    val b64 = Base64.encodeToString(limitedBytes, Base64.NO_WRAP)
                    onUpdateSettings {
                        it.copy(
                            customSoundUri = uri.toString(),
                            customSoundName = displayName,
                            customSoundSource = pendingImportSource,
                            customSoundBase64 = b64,
                            reminderRingtoneStyle = "custom_imported",
                            useDefaultPhoneRingtone = false
                        )
                    }
                    onShowToast("Imported \"$displayName\" from $pendingImportSource")
                }
            } catch (_: Exception) {
                onShowToast("Failed to import audio file")
            }
        }
    }

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("custom_reminder_sound_settings_card"),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isKhmer) "ការកំណត់សំឡេងរំលឹកផ្ទាល់ខ្លួន" else "Custom Reminder & Sound Settings",
                        color = tokens.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Ringtones • Import Audio • Volume • Vibration • Do Not Disturb",
                        color = tokens.textSecondary,
                        fontSize = 11.5.sp
                    )
                }
                GlassPillButton(
                    text = if (isPlayingPreview) "■ Stop" else "▶ Test Sound",
                    selected = true,
                    accentColor = accentColor,
                    onClick = {
                        if (isPlayingPreview) {
                            AudioHelper.stopActivePlayback()
                            isPlayingPreview = false
                        } else {
                            isPlayingPreview = true
                            scope.launch {
                                AudioHelper.playReminderPreviewOrAlert(context, settings)
                                isPlayingPreview = false
                            }
                            onShowToast("Testing reminder sound (${settings.reminderVolumePercent}%)")
                        }
                    },
                    modifier = Modifier.testTag("test_reminder_sound_button")
                )
            }

            // 1. Enable / Disable Reminder Sound
            SettingSwitchRow(
                title = if (isKhmer) "បើកសំឡេងរំលឹក" else "Enable Reminder Sound",
                subtitle = "Master switch for all scheduled reminder alerts",
                checked = settings.reminderSoundEnabled,
                accentColor = accentColor,
                onCheckedChange = { enabled ->
                    onUpdateSettings { it.copy(reminderSoundEnabled = enabled) }
                },
                tag = "toggle_reminder_sound_enabled"
            )

            // 2. Alert Mode: Sound + Vibration | Sound Only | Vibration Only | Silent Reminder
            Text(
                text = if (isKhmer) "របៀបជូនដំណឹង (Alert Mode)" else "Reminder Alert Mode",
                color = tokens.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "sound_vibration" to "🔔 Sound + Vibration",
                    "sound_only" to "🔊 Sound Only",
                    "vibration_only" to "📳 Vibration Only",
                    "silent" to "🔕 Silent (No Sound)"
                ).forEach { (modeId, label) ->
                    GlassPillButton(
                        text = label,
                        selected = settings.reminderAlertMode == modeId,
                        accentColor = accentColor,
                        onClick = {
                            onUpdateSettings { it.copy(reminderAlertMode = modeId) }
                            if (modeId == "vibration_only") {
                                AudioHelper.triggerVibrationPattern(context, settings.customVibrationPattern)
                            }
                        },
                        modifier = Modifier.testTag("alert_mode_$modeId")
                    )
                }
            }

            // 3. Default Phone Ringtone Toggle
            SettingSwitchRow(
                title = if (isKhmer) "ប្រើសំឡេងរោទ៍ដើមរបស់ទូរស័ព្ទ" else "Default Phone Ringtone",
                subtitle = "Use Android system default notification/ringtone sound",
                checked = settings.useDefaultPhoneRingtone,
                accentColor = accentColor,
                onCheckedChange = { useDef ->
                    onUpdateSettings { it.copy(useDefaultPhoneRingtone = useDef) }
                },
                tag = "toggle_default_phone_ringtone"
            )

            // 4. Custom Ringtone Style & Preview Sound Before Selecting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isKhmer) "ស្ទីលសំឡេងរោទ៍ផ្ទាល់ខ្លួន" else "Custom Ringtone Style (Tap to Preview)",
                    color = tokens.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Preview on select",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioHelper.RINGTONE_PRESETS.forEach { preset ->
                    val selected = !settings.useDefaultPhoneRingtone && settings.reminderRingtoneStyle == preset.id
                    GlassPillButton(
                        text = "🎵 ${if (isKhmer) preset.labelKm else preset.labelEn}",
                        selected = selected,
                        accentColor = accentColor,
                        onClick = {
                            onUpdateSettings {
                                it.copy(
                                    reminderRingtoneStyle = preset.id,
                                    useDefaultPhoneRingtone = false
                                )
                            }
                            scope.launch {
                                AudioHelper.playReminderPreviewOrAlert(
                                    context = context,
                                    settings = settings.copy(useDefaultPhoneRingtone = false),
                                    overrideStyleId = preset.id
                                )
                            }
                        },
                        modifier = Modifier.testTag("ringtone_style_${preset.id}")
                    )
                }
                if (settings.customSoundName.isNotBlank()) {
                    GlassPillButton(
                        text = "📂 ${settings.customSoundName} (${settings.customSoundSource})",
                        selected = !settings.useDefaultPhoneRingtone && settings.reminderRingtoneStyle == "custom_imported",
                        accentColor = accentColor,
                        onClick = {
                            onUpdateSettings {
                                it.copy(
                                    reminderRingtoneStyle = "custom_imported",
                                    useDefaultPhoneRingtone = false
                                )
                            }
                            scope.launch {
                                AudioHelper.playReminderPreviewOrAlert(
                                    context = context,
                                    settings = settings.copy(useDefaultPhoneRingtone = false),
                                    overrideStyleId = "custom_imported"
                                )
                            }
                        }
                    )
                }
            }

            // 5. Import Custom Sound from Device / Google Drive / Files
            Text(
                text = if (isKhmer) "នាំចូលសំឡេងផ្ទាល់ខ្លួន" else "Import Custom Sound",
                color = tokens.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassPillButton(
                    text = "📱 Import from Device",
                    accentColor = accentColor,
                    onClick = {
                        pendingImportSource = "Device"
                        try {
                            soundImportLauncher.launch(arrayOf("audio/*"))
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.testTag("import_sound_device_button")
                )
                GlassPillButton(
                    text = "☁️ Import from Google Drive",
                    accentColor = accentColor,
                    onClick = {
                        pendingImportSource = "Google Drive"
                        try {
                            soundImportLauncher.launch(arrayOf("audio/*"))
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.testTag("import_sound_drive_button")
                )
                GlassPillButton(
                    text = "📁 Import from Files",
                    accentColor = accentColor,
                    onClick = {
                        pendingImportSource = "Files"
                        try {
                            soundImportLauncher.launch(arrayOf("audio/*", "application/ogg", "audio/wav", "audio/mpeg"))
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.testTag("import_sound_files_button")
                )
            }

            // 6. Custom Reminder Volume Slider (0-100%) + Separate Volume + Gradual Fade-In
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.fieldFill)
                    .border(1.dp, tokens.panelBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isKhmer) "កម្រិតសំឡេងរំលឹក (0–100%)" else "Custom Reminder Volume",
                        color = tokens.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${settings.reminderVolumePercent}%",
                        color = accentColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Slider(
                    value = settings.reminderVolumePercent.toFloat(),
                    onValueChange = { newVal ->
                        onUpdateSettings { it.copy(reminderVolumePercent = newVal.toInt().coerceIn(0, 100)) }
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor
                    ),
                    modifier = Modifier.testTag("reminder_volume_slider")
                )

                SettingSwitchRow(
                    title = if (isKhmer) "គ្រប់គ្រងកម្រិតសំឡេងដាច់ដោយឡែក" else "Separate Sound Volume Control",
                    subtitle = "Independent volume level from phone notification stream",
                    checked = settings.separateSoundVolumeControl,
                    accentColor = accentColor,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(separateSoundVolumeControl = v) } },
                    tag = "toggle_separate_volume"
                )

                SettingSwitchRow(
                    title = if (isKhmer) "បង្កើនសំឡេងបន្តិចម្តងៗ" else "Gradually Increase Volume",
                    subtitle = "Smooth acoustic fade-in from 15% to ${settings.reminderVolumePercent}%",
                    checked = settings.graduallyIncreaseVolume,
                    accentColor = accentColor,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(graduallyIncreaseVolume = v) } },
                    tag = "toggle_gradual_volume"
                )
            }

            // 7. Repeat Ringtone & Reminder Sound Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.fieldFill)
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Repeat Ringtone",
                        color = tokens.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3, 5).forEach { count ->
                            GlassPillButton(
                                text = "${count}x",
                                selected = settings.repeatRingtoneCount == count,
                                accentColor = accentColor,
                                onClick = { onUpdateSettings { it.copy(repeatRingtoneCount = count) } }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.fieldFill)
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Sound Duration",
                        color = tokens.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(3, 5, 10, 15).forEach { sec ->
                            GlassPillButton(
                                text = "${sec}s",
                                selected = settings.reminderSoundDurationSec == sec,
                                accentColor = accentColor,
                                onClick = { onUpdateSettings { it.copy(reminderSoundDurationSec = sec) } }
                            )
                        }
                    }
                }
            }

            // 8. Custom Vibration Pattern
            Text(
                text = if (isKhmer) "លំនាំញ័រផ្ទាល់ខ្លួន" else "Custom Vibration Pattern",
                color = tokens.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioHelper.VIBRATION_PRESETS.forEach { vib ->
                    GlassPillButton(
                        text = "📳 ${if (isKhmer) vib.labelKm else vib.labelEn}",
                        selected = settings.customVibrationPattern == vib.id,
                        accentColor = accentColor,
                        onClick = {
                            onUpdateSettings { it.copy(customVibrationPattern = vib.id) }
                            AudioHelper.triggerVibrationPattern(context, vib.id)
                        },
                        modifier = Modifier.testTag("vibration_pattern_${vib.id}")
                    )
                }
            }

            // 9. Custom Notification Sound + Different Sounds for Different Reminders
            Text(
                text = if (isKhmer) "សំឡេងជូនដំណឹងប្រព័ន្ធផ្ទាល់ខ្លួន" else "Custom Notification Sound",
                color = tokens.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioHelper.RINGTONE_PRESETS.take(5).forEach { preset ->
                    GlassPillButton(
                        text = preset.labelEn,
                        selected = settings.customNotificationSoundStyle == preset.id,
                        accentColor = accentColor,
                        onClick = {
                            onUpdateSettings { it.copy(customNotificationSoundStyle = preset.id) }
                            scope.launch {
                                AudioHelper.playReminderPreviewOrAlert(
                                    context = context,
                                    settings = settings,
                                    overrideStyleId = preset.id
                                )
                            }
                        }
                    )
                }
            }

            SettingSwitchRow(
                title = if (isKhmer) "សំឡេងខុសគ្នាសម្រាប់ការរំលឹកនីមួយៗ" else "Different Sounds for Different Reminders",
                subtitle = "Allow per-note custom ringtone, alert mode & vibration in Reminder Picker",
                checked = settings.differentSoundsPerReminder,
                accentColor = accentColor,
                onCheckedChange = { v -> onUpdateSettings { it.copy(differentSoundsPerReminder = v) } },
                tag = "toggle_per_reminder_sounds"
            )

            // 10. Do Not Disturb (DND) Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.fieldFill)
                    .border(1.dp, tokens.panelBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingSwitchRow(
                    title = if (isKhmer) "កុំរំខាន (Do Not Disturb)" else "Do Not Disturb Settings (Quiet Hours)",
                    subtitle = "Silence reminder sounds during scheduled quiet hours (${settings.dndStartHour}:00 – ${settings.dndEndHour}:00)",
                    checked = settings.dndEnabled,
                    accentColor = accentColor,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(dndEnabled = v) } },
                    tag = "toggle_dnd_enabled"
                )

                if (settings.dndEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start: ${settings.dndStartHour}:00",
                            color = tokens.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(20, 21, 22, 23).forEach { hr ->
                                GlassPillButton(
                                    text = "${hr}:00",
                                    selected = settings.dndStartHour == hr,
                                    accentColor = accentColor,
                                    onClick = { onUpdateSettings { it.copy(dndStartHour = hr) } }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "End: 0${settings.dndEndHour}:00",
                            color = tokens.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(6, 7, 8, 9).forEach { hr ->
                                GlassPillButton(
                                    text = "0${hr}:00",
                                    selected = settings.dndEndHour == hr,
                                    accentColor = accentColor,
                                    onClick = { onUpdateSettings { it.copy(dndEndHour = hr) } }
                                )
                            }
                        }
                    }

                    SettingSwitchRow(
                        title = "Allow Pinned Notes to Bypass DND",
                        subtitle = "Pinned priority notes will still chime during quiet hours",
                        checked = settings.dndAllowPinnedBypass,
                        accentColor = accentColor,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(dndAllowPinnedBypass = v) } },
                        tag = "toggle_dnd_bypass_pinned"
                    )
                }
            }

            // 11. Reset to Default Sound & Test Reminder Sound Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassPillButton(
                    text = if (isKhmer) "កំណត់សំឡេងដើមឡើងវិញ" else "Reset to Default Sound",
                    accentColor = Color(0xFFEF4444),
                    onClick = {
                        AudioHelper.stopActivePlayback()
                        onUpdateSettings {
                            it.copy(
                                reminderSoundEnabled = true,
                                reminderAlertMode = "sound_vibration",
                                reminderRingtoneStyle = "crystal_chime",
                                useDefaultPhoneRingtone = false,
                                customSoundUri = "",
                                customSoundName = "",
                                customSoundSource = "",
                                customSoundBase64 = "",
                                customNotificationSoundStyle = "glass_bell",
                                reminderVolumePercent = 85,
                                separateSoundVolumeControl = true,
                                graduallyIncreaseVolume = false,
                                repeatRingtoneCount = 2,
                                reminderSoundDurationSec = 5,
                                customVibrationPattern = "double_beat",
                                differentSoundsPerReminder = true,
                                dndEnabled = false
                            )
                        }
                        onShowToast("Reset reminder & sound settings to default")
                    },
                    modifier = Modifier.testTag("reset_default_sound_button")
                )

                GlassPillButton(
                    text = "🔔 Test Reminder Sound",
                    selected = true,
                    accentColor = accentColor,
                    onClick = {
                        scope.launch {
                            AudioHelper.playReminderPreviewOrAlert(context, settings)
                        }
                        onShowToast("Playing test reminder alert")
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    val tokens = LocalGlassTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.fieldFill)
            .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = tokens.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = tokens.textSecondary,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor
            )
        )
    }
}
