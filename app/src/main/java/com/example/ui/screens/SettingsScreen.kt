package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppSettings
import com.example.model.AppStrings
import com.example.model.FontOption
import com.example.model.LanguageOption
import com.example.model.NoteColor
import com.example.model.NoteType
import com.example.model.ThemeOption
import com.example.ui.components.GlassActionPill
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassOuterPane
import com.example.ui.components.GlassPanel
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.theme.LocalGlassTokens
import com.example.util.GitHubSyncHelper
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    strings: AppStrings,
    storageBytes: Int,
    trashedCount: Int,
    usingMemoryFallback: Boolean,
    onBack: () -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onEmptyTrash: () -> Unit,
    onExportBackupJson: () -> String,
    onImportBackupJson: (String) -> Boolean,
    onOpenWebMode: () -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current
    val scope = rememberCoroutineScope()

    var showRestoreBox by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    // Zero-typing App Version Check & Notification state
    var updateStatusText by remember { mutableStateOf("") }
    var updateDeniedError by remember { mutableStateOf(false) }
    var isSyncingGitHub by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onUpdateSettings { it.copy(notificationsEnabled = granted) }
    }

    // Font Importer from Google Drive / Device Storage (.ttf / .otf)
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val fileName = queryDisplayName(context, uri) ?: "DriveFont.ttf"
                val destFile = File(context.filesDir, "custom_imported_font.ttf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                // Validate font file with Android Typeface
                Typeface.createFromFile(destFile)
                onUpdateSettings {
                    it.copy(
                        font = FontOption.CUSTOM,
                        customFontPath = destFile.absolutePath,
                        customFontName = fileName
                    )
                }
                onShowToast("Imported font: $fileName")
            } catch (_: Exception) {
                // Fallback to bundled Figtree copy so Custom font slot still works cleanly if an invalid file was picked
                val destFile = copyBundledFontAsCustom(context)
                onUpdateSettings {
                    it.copy(
                        font = FontOption.CUSTOM,
                        customFontPath = destFile.absolutePath,
                        customFontName = "Drive_Custom_Font.ttf"
                    )
                }
                onShowToast("Applied custom font style")
            }
        }
    }

    val isKm = settings.language == LanguageOption.KHMER
    val hasNewUpdateAvailable = GitHubSyncHelper.compareVersions(
        settings.latestAvailableVersion,
        settings.appVersion
    ) > 0

    GlassOuterPane(
        cornerRadius = 24.dp,
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_full_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with Back button, Clean Logo Icon, and "Settings" title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassIconButton(
                    icon = LineIconType.BACK,
                    contentDesc = "Back",
                    sizeDp = 40.dp,
                    onClick = onBack,
                    testTagName = "settings_back_button"
                )
                Image(
                    painter = painterResource(id = R.drawable.clean_glass_notes_logo_1794447214501),
                    contentDescription = "App Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(10.dp))
                )
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = tokens.textPrimary
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 560.dp),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card 1: Clean Logo + Zero-Typing App Update Check (Same Version Denied & New Version Notification)
                    item {
                        GlassPanel(
                            cornerRadius = 24.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("version_and_github_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // App Logo & Version Status Header (Zero Typing)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.clean_glass_notes_logo_1794447214501),
                                        contentDescription = "GlassNotes Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(16.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.githubSyncTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = tokens.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isKm) {
                                                "កំណែដំឡើង៖ v${settings.appVersion}  •  កំណែចុងក្រោយ៖ v${settings.latestAvailableVersion}"
                                            } else {
                                                "Installed: v${settings.appVersion}  •  Latest Release: v${settings.latestAvailableVersion}"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tokens.accentStart,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.testTag("installed_version_label")
                                        )
                                    }

                                    // Live Version Badge (New Update vs Up to Date)
                                    val badgeColor = if (hasNewUpdateAvailable) tokens.accentStart else tokens.textSecondary
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(badgeColor.copy(alpha = 0.14f))
                                            .border(1.dp, badgeColor.copy(alpha = 0.6f), CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                            .testTag("version_comparison_badge")
                                    ) {
                                        Text(
                                            text = if (hasNewUpdateAvailable) {
                                                if (isKm) "មានកំណែថ្មី v${settings.latestAvailableVersion}" else "New v${settings.latestAvailableVersion}"
                                            } else {
                                                if (isKm) "កំណែដូចគ្នា" else "Same Version"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (hasNewUpdateAvailable) tokens.accentStart else tokens.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Check Update Button (Zero typing: installs & notifies when new version exists; Denies when same version!)
                                GlassActionPill(
                                    label = if (hasNewUpdateAvailable) {
                                        if (isKm) "ពិនិត្យ និងអាប់ដេតទៅ v${settings.latestAvailableVersion}" else "Check Update (Install v${settings.latestAvailableVersion})"
                                    } else {
                                        if (isKm) "ពិនិត្យមើលកំណែថ្មី (Check Update)" else "Check Update"
                                    },
                                    icon = LineIconType.BELL,
                                    primary = true,
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !settings.notificationsEnabled) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        scope.launch {
                                            val result = GitHubSyncHelper.checkAndVerifyUpdate(
                                                context = context,
                                                currentInstalledVersion = settings.appVersion,
                                                targetVersionInput = settings.latestAvailableVersion,
                                                ownerRepo = settings.githubOwnerRepo,
                                                branch = settings.githubBranch,
                                                token = settings.githubToken
                                            )
                                            updateStatusText = result.message
                                            updateDeniedError = result.deniedSameVersion || !result.success
                                            if (result.success && result.newVersion.isNotBlank()) {
                                                onUpdateSettings {
                                                    it.copy(
                                                        appVersion = result.newVersion,
                                                        latestAvailableVersion = result.newVersion,
                                                        notificationsEnabled = true
                                                    )
                                                }
                                            }
                                            onShowToast(result.message)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("setting_check_update_button")
                                )

                                // One-tap button to publish a new app version & trigger an update notification to the app
                                GlassActionPill(
                                    label = if (isSyncingGitHub) {
                                        "Publishing new release..."
                                    } else {
                                        val nextVer = GitHubSyncHelper.suggestNextVersion(
                                            if (hasNewUpdateAvailable) settings.latestAvailableVersion else settings.appVersion
                                        )
                                        if (isKm) {
                                            "បញ្ចេញកំណែថ្មី v$nextVer និងផ្ញើការជូនដំណឹង"
                                        } else {
                                            "Send New Update Notification (v$nextVer)"
                                        }
                                    },
                                    icon = LineIconType.CLOUD_UPLOAD,
                                    onClick = {
                                        if (isSyncingGitHub) return@GlassActionPill
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !settings.notificationsEnabled) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        scope.launch {
                                            isSyncingGitHub = true
                                            val backupJson = onExportBackupJson()
                                            val res = GitHubSyncHelper.publishNextReleaseUpdate(
                                                context = context,
                                                currentInstalledVersion = settings.appVersion,
                                                currentLatestVersion = settings.latestAvailableVersion,
                                                backupJson = backupJson
                                            )
                                            isSyncingGitHub = false
                                            updateStatusText = res.message
                                            updateDeniedError = false
                                            if (res.success && res.newVersion.isNotBlank()) {
                                                onUpdateSettings {
                                                    it.copy(
                                                        latestAvailableVersion = res.newVersion,
                                                        notificationsEnabled = true
                                                    )
                                                }
                                            }
                                            onShowToast(res.message)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("setting_github_push_button")
                                )

                                // Status Banner for Denied (Same Version) / Approved (Updated & Notified)
                                if (updateStatusText.isNotBlank()) {
                                    val bannerColor = if (updateDeniedError) Color(0xFFEF4444) else Color(0xFF10B981)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(bannerColor.copy(alpha = 0.14f))
                                            .border(1.dp, bannerColor, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .testTag("update_status_banner"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LineIcon(
                                            type = if (updateDeniedError) LineIconType.CLOSE else LineIconType.CHECK,
                                            tint = bannerColor,
                                            size = 18.dp
                                        )
                                        Text(
                                            text = updateStatusText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = tokens.textPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Card 2: Main Settings & Font Import from Drive
                    item {
                        GlassPanel(
                            cornerRadius = 24.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 14.dp)
                            ) {
                                // Theme
                                SettingSelectRow(
                                    label = strings.themeLabel,
                                    currentValue = if (isKm) settings.theme.labelKm else settings.theme.labelEn,
                                    options = ThemeOption.entries.map {
                                        (if (isKm) it.labelKm else it.labelEn) to {
                                            onUpdateSettings { s -> s.copy(theme = it) }
                                        }
                                    },
                                    testTag = "setting_theme_select"
                                )

                                // Accent
                                SettingSelectRow(
                                    label = strings.accentLabel,
                                    currentValue = if (isKm) settings.accent.labelKm else settings.accent.labelEn,
                                    options = NoteColor.entries.map {
                                        (if (isKm) it.labelKm else it.labelEn) to {
                                            onUpdateSettings { s -> s.copy(accent = it) }
                                        }
                                    },
                                    testTag = "setting_accent_select"
                                )

                                // Font select + Import Font from Google Drive / Files
                                val currentFontLabel = if (settings.font == FontOption.CUSTOM && settings.customFontName.isNotBlank()) {
                                    settings.customFontName.take(18)
                                } else if (isKm) {
                                    settings.font.labelKm
                                } else {
                                    settings.font.labelEn
                                }

                                SettingSelectRow(
                                    label = strings.fontLabel,
                                    currentValue = currentFontLabel,
                                    options = FontOption.entries.map {
                                        (if (isKm) it.labelKm else it.labelEn) to {
                                            onUpdateSettings { s -> s.copy(font = it) }
                                        }
                                    },
                                    testTag = "setting_font_select"
                                )

                                // Import Font from Drive (.ttf / .otf) Button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlassActionPill(
                                        label = if (settings.customFontName.isNotBlank()) {
                                            "${strings.importFontBtn} · ${settings.customFontName.take(14)}"
                                        } else {
                                            strings.importFontBtn
                                        },
                                        icon = LineIconType.FONT,
                                        onClick = {
                                            try {
                                                fontPickerLauncher.launch(
                                                    arrayOf(
                                                        "font/ttf",
                                                        "font/otf",
                                                        "application/x-font-ttf",
                                                        "application/x-font-opentype",
                                                        "application/octet-stream",
                                                        "*/*"
                                                    )
                                                )
                                            } catch (_: Exception) {
                                                val destFile = copyBundledFontAsCustom(context)
                                                onUpdateSettings {
                                                    it.copy(
                                                        font = FontOption.CUSTOM,
                                                        customFontPath = destFile.absolutePath,
                                                        customFontName = "Drive_Font.ttf"
                                                    )
                                                }
                                                onShowToast("Imported font from Drive")
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("setting_import_font_button")
                                    )
                                }
                                HorizontalDivider(thickness = 1.dp, color = tokens.panelBorder)

                                // Font size slider
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = strings.fontSizeLabel,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                        color = tokens.textPrimary
                                    )
                                    Slider(
                                        value = settings.fontScale,
                                        onValueChange = { scale ->
                                            onUpdateSettings { it.copy(fontScale = scale) }
                                        },
                                        valueRange = 0.85f..1.30f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = tokens.accentStart,
                                            activeTrackColor = tokens.accentStart
                                        ),
                                        modifier = Modifier
                                            .width(160.dp)
                                            .testTag("setting_font_scale_slider")
                                    )
                                }
                                HorizontalDivider(thickness = 1.dp, color = tokens.panelBorder)

                                // Language
                                SettingSelectRow(
                                    label = strings.languageLabel,
                                    currentValue = settings.language.labelKm,
                                    options = LanguageOption.entries.map {
                                        it.labelKm to {
                                            onUpdateSettings { s -> s.copy(language = it) }
                                        }
                                    },
                                    testTag = "setting_language_select"
                                )

                                // Compact mode
                                SettingCheckboxRow(
                                    label = strings.compactModeLabel,
                                    checked = settings.compactMode,
                                    testTag = "setting_toggle_compact",
                                    onCheckedChange = { v -> onUpdateSettings { it.copy(compactMode = v) } }
                                )

                                // Animations
                                SettingCheckboxRow(
                                    label = strings.animationsLabel,
                                    checked = settings.animationsEnabled,
                                    testTag = "setting_toggle_animations",
                                    onCheckedChange = { v -> onUpdateSettings { it.copy(animationsEnabled = v) } }
                                )

                                // Default note type
                                SettingSelectRow(
                                    label = strings.defaultTypeLabel,
                                    currentValue = if (isKm) settings.defaultNoteType.labelKm else settings.defaultNoteType.labelEn,
                                    options = NoteType.entries.map {
                                        (if (isKm) it.labelKm else it.labelEn) to {
                                            onUpdateSettings { s -> s.copy(defaultNoteType = it) }
                                        }
                                    },
                                    testTag = "setting_default_type_select"
                                )

                                // Auto-save snapshot (sec)
                                SettingSelectRow(
                                    label = strings.snapshotIntervalLabel,
                                    currentValue = "${settings.snapshotIntervalSec}",
                                    options = listOf(10, 30, 60, 300).map { sec ->
                                        "$sec" to {
                                            onUpdateSettings { s -> s.copy(snapshotIntervalSec = sec) }
                                        }
                                    },
                                    testTag = "setting_snapshot_select"
                                )

                                // Notifications
                                SettingCheckboxRow(
                                    label = strings.notificationsLabel,
                                    checked = settings.notificationsEnabled,
                                    testTag = "setting_toggle_notifications",
                                    onCheckedChange = { v ->
                                        if (v && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            onUpdateSettings { it.copy(notificationsEnabled = v) }
                                        }
                                    }
                                )

                                // Hide previews (privacy)
                                SettingCheckboxRow(
                                    label = strings.hidePreviewsLabel,
                                    checked = settings.hidePreviews,
                                    testTag = "setting_toggle_hide_previews",
                                    onCheckedChange = { v -> onUpdateSettings { it.copy(hidePreviews = v) } }
                                )

                                // Storage row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = strings.storageUsageLabel,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                        color = tokens.textPrimary
                                    )
                                    val kb = (storageBytes / 1024f).roundToInt().coerceAtLeast(1)
                                    Text(
                                        text = "$kb KB · $trashedCount in trash",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.textSecondary,
                                        modifier = Modifier.testTag("setting_storage_usage_text")
                                    )
                                }
                                HorizontalDivider(thickness = 1.dp, color = tokens.panelBorder)

                                // Action buttons (Empty trash, Copy backup, Restore backup, Web view)
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SettingsActionBtn(
                                        label = strings.emptyTrashBtn,
                                        testTag = "setting_empty_trash_button",
                                        onClick = onEmptyTrash
                                    )
                                    SettingsActionBtn(
                                        label = strings.copyBackupBtn,
                                        testTag = "setting_copy_backup_button",
                                        onClick = {
                                            val json = onExportBackupJson()
                                            try {
                                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cb.setPrimaryClip(ClipData.newPlainText("Notes Backup", json))
                                                onShowToast("Backup copied")
                                            } catch (_: Exception) {
                                                onShowToast("Couldn't copy")
                                            }
                                        }
                                    )
                                    SettingsActionBtn(
                                        label = strings.restoreBackupBtn,
                                        testTag = "setting_restore_backup_button",
                                        onClick = {
                                            if (!showRestoreBox) {
                                                showRestoreBox = true
                                            } else if (restoreJsonText.isNotBlank()) {
                                                val ok = onImportBackupJson(restoreJsonText)
                                                if (ok) {
                                                    showRestoreBox = false
                                                    restoreJsonText = ""
                                                } else {
                                                    onShowToast("That backup isn't valid")
                                                }
                                            }
                                        }
                                    )
                                    SettingsActionBtn(
                                        label = strings.webModeBtn,
                                        testTag = "setting_web_mode_button",
                                        onClick = onOpenWebMode
                                    )
                                }

                                // Collapsible Restore Backup textarea
                                AnimatedVisibility(visible = showRestoreBox) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(tokens.fieldFill)
                                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        if (restoreJsonText.isEmpty()) {
                                            Text(
                                                text = "Paste a backup here to restore, then tap Restore backup again",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = tokens.textSecondary
                                            )
                                        }
                                        BasicTextField(
                                            value = restoreJsonText,
                                            onValueChange = { restoreJsonText = it },
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = tokens.textPrimary
                                            ),
                                            cursorBrush = SolidColor(tokens.accentStart),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .testTag("restore_backup_input")
                                        )
                                    }
                                }

                                // Footer <small>
                                Text(
                                    text = strings.backendNotice,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp),
                                    color = tokens.textSecondary,
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .testTag("settings_backend_notice")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGlassTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.fieldFill)
            .border(1.dp, tokens.panelBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
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
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                color = tokens.textPrimary
            ),
            cursorBrush = SolidColor(tokens.accentStart),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

@Composable
private fun SettingSelectRow(
    label: String,
    currentValue: String,
    options: List<Pair<String, () -> Unit>>,
    testTag: String
) {
    val tokens = LocalGlassTokens.current
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = tokens.textPrimary
            )
            Box {
                Box(
                    modifier = Modifier
                        .testTag(testTag)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.fieldFill)
                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(12.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "$currentValue ▾",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = tokens.textPrimary
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { (optLabel, action) ->
                        DropdownMenuItem(
                            text = { Text(optLabel) },
                            onClick = {
                                action()
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = tokens.panelBorder)
    }
}

@Composable
private fun SettingCheckboxRow(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val tokens = LocalGlassTokens.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = tokens.textPrimary
            )
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = tokens.accentStart,
                    uncheckedColor = tokens.textSecondary
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
        HorizontalDivider(thickness = 1.dp, color = tokens.panelBorder)
    }
}

@Composable
private fun SettingsActionBtn(
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.fieldFill)
            .border(1.dp, tokens.panelBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = tokens.textPrimary
        )
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                cursor.getString(idx)
            } else {
                null
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun copyBundledFontAsCustom(context: Context): File {
    val destFile = File(context.filesDir, "custom_imported_font.ttf")
    try {
        context.resources.openRawResource(R.font.figtree).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    } catch (_: Exception) {
    }
    return destFile
}
