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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.window.Dialog
import com.example.ui.components.AppGlassLogo
import com.example.ui.components.GlassActionPill
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassOuterPane
import com.example.ui.components.GlassPanel
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.theme.LocalGlassTokens
import com.example.util.GitHubReleaseInfo
import com.example.util.GitHubSyncHelper
import com.example.util.UpdateState
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

    val installedInfo = remember(context) { GitHubSyncHelper.getInstalledAppInfo(context) }
    var releaseInfo by remember {
        mutableStateOf(
            GitHubReleaseInfo(
                state = UpdateState.IDLE,
                statusMessage = UpdateState.IDLE.statusTextEn,
                hasNewUpdate = false,
                installedVersionName = installedInfo.versionName,
                installedVersionCode = installedInfo.versionCode,
                latestReleaseTag = settings.latestAvailableVersion.ifBlank { "Not checked yet" },
                releaseDate = "—"
            )
        )
    }
    var downloadProgressPct by remember { mutableStateOf(0) }
    var showReleaseNotesDialog by remember { mutableStateOf(false) }

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

    GlassOuterPane(
        cornerRadius = 24.dp,
        modifier = modifier
            .fillMaxSize()
            .blur(if (showReleaseNotesDialog) 26.dp else 0.dp)
            .testTag("settings_full_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with Back button, Clean Vector Glass Logo, and "Settings" title
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
                AppGlassLogo(
                    sizeDp = 34.dp,
                    cornerRadius = 10.dp
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
                    // Card 1: APPNOTE — GitHub Repository Connected Auto Update System (MEY-NASHH/APPNOTE)
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
                                // Header: Logo + Title + Official Repo Slug + New Version Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AppGlassLogo(
                                        sizeDp = 52.dp,
                                        cornerRadius = 16.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.githubSyncTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = tokens.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "GitHub: ${GitHubSyncHelper.OFFICIAL_REPO_SLUG}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tokens.accentStart,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // New Version Badge (shown only when a real newer GitHub release is published)
                                    if (releaseInfo.hasNewUpdate) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981).copy(alpha = 0.16f))
                                                .border(1.dp, Color(0xFF10B981), CircleShape)
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                                .testTag("version_comparison_badge")
                                        ) {
                                            Text(
                                                text = "NEW ${releaseInfo.latestReleaseTag}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF10B981),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(tokens.fieldFill)
                                                .border(1.dp, tokens.panelBorder, CircleShape)
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                                .testTag("version_comparison_badge")
                                        ) {
                                            Text(
                                                text = "v${installedInfo.versionName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tokens.textSecondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Metadata Grid: Installed Version, Latest GitHub Release, Release Date, Update Status
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(tokens.fieldFill)
                                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    UpdateMetaRow(
                                        label = if (isKm) "កំណែដំឡើងបច្ចុប្បន្ន" else "Installed Version",
                                        value = "v${installedInfo.versionName} (code ${installedInfo.versionCode})",
                                        valueTestTag = "installed_version_label"
                                    )
                                    UpdateMetaRow(
                                        label = if (isKm) "កំណែចុងក្រោយលើ GitHub" else "Latest GitHub Release",
                                        value = releaseInfo.latestReleaseTag,
                                        valueTestTag = "latest_github_release_label"
                                    )
                                    UpdateMetaRow(
                                        label = if (isKm) "កាលបរិច្ឆេទចេញផ្សាយ" else "Release Date",
                                        value = releaseInfo.releaseDate,
                                        valueTestTag = "github_release_date_label"
                                    )
                                    UpdateMetaRow(
                                        label = if (isKm) "ស្ថានភាពអាប់ដេត" else "Update Status",
                                        value = releaseInfo.statusMessage,
                                        valueColor = when (releaseInfo.state) {
                                            UpdateState.UPDATE_AVAILABLE,
                                            UpdateState.READY_TO_INSTALL,
                                            UpdateState.UP_TO_DATE -> Color(0xFF10B981)
                                            UpdateState.FAILED -> Color(0xFFEF4444)
                                            else -> tokens.accentStart
                                        },
                                        valueTestTag = "github_update_status_label"
                                    )
                                }

                                // Status / Error Detail Banner
                                if (releaseInfo.state != UpdateState.IDLE) {
                                    val bannerColor = when (releaseInfo.state) {
                                        UpdateState.FAILED -> Color(0xFFEF4444)
                                        UpdateState.UPDATE_AVAILABLE,
                                        UpdateState.READY_TO_INSTALL,
                                        UpdateState.UP_TO_DATE -> Color(0xFF10B981)
                                        else -> tokens.accentStart
                                    }
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
                                            type = if (releaseInfo.state == UpdateState.FAILED) LineIconType.CLOSE else LineIconType.CHECK,
                                            tint = bannerColor,
                                            size = 18.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = releaseInfo.statusMessage,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = tokens.textPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (releaseInfo.errorDetail.isNotBlank()) {
                                                Text(
                                                    text = releaseInfo.errorDetail,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = tokens.textSecondary
                                                )
                                            } else if (releaseInfo.verifiedSha256.isNotBlank()) {
                                                Text(
                                                    text = "Verified SHA-256: ${releaseInfo.verifiedSha256.take(16)}…",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = tokens.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                // Prerelease updates opt-in toggle (off by default per Section 3 & 7)
                                SettingCheckboxRow(
                                    label = if (isKm) "រួមបញ្ចូលកំណែសាកល្បង (Prerelease)" else "Include prerelease updates",
                                    checked = settings.includePrereleases,
                                    testTag = "setting_include_prerelease_checkbox",
                                    onCheckedChange = { enabled ->
                                        onUpdateSettings { it.copy(includePrereleases = enabled) }
                                    }
                                )

                                // Primary Action 1: Check Update (queries GitHub Releases)
                                GlassActionPill(
                                    label = if (releaseInfo.state == UpdateState.CHECKING) {
                                        UpdateState.CHECKING.statusTextEn
                                    } else if (isKm) {
                                        "ពិនិត្យមើលកំណែថ្មី (Check Update)"
                                    } else {
                                        "Check Update"
                                    },
                                    icon = LineIconType.BELL,
                                    primary = !releaseInfo.hasNewUpdate,
                                    onClick = {
                                        if (releaseInfo.state == UpdateState.CHECKING ||
                                            releaseInfo.state == UpdateState.DOWNLOADING
                                        ) return@GlassActionPill

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !settings.notificationsEnabled) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        scope.launch {
                                            releaseInfo = releaseInfo.copy(
                                                state = UpdateState.CHECKING,
                                                statusMessage = UpdateState.CHECKING.statusTextEn,
                                                errorDetail = ""
                                            )
                                            val result = GitHubSyncHelper.checkGitHubReleaseUpdate(
                                                context = context,
                                                includePrerelease = settings.includePrereleases,
                                                notifyOnNewRelease = true
                                            )
                                            releaseInfo = result
                                            onUpdateSettings {
                                                it.copy(
                                                    latestAvailableVersion = if (result.hasNewUpdate) result.latestReleaseTag else ""
                                                )
                                            }
                                            onShowToast(result.statusMessage)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("setting_check_update_button")
                                )

                                // Primary Action 2: Download Update (downloads actual signed APK & verifies SHA-256 + cert)
                                if (releaseInfo.hasNewUpdate ||
                                    releaseInfo.state == UpdateState.DOWNLOADING ||
                                    releaseInfo.state == UpdateState.VERIFYING
                                ) {
                                    GlassActionPill(
                                        label = when (releaseInfo.state) {
                                            UpdateState.DOWNLOADING -> "Downloading update... ($downloadProgressPct%)"
                                            UpdateState.VERIFYING -> UpdateState.VERIFYING.statusTextEn
                                            else -> if (isKm) {
                                                "ទាញយកកំណែថ្មី (${releaseInfo.apkFileName.ifBlank { "APK" }})"
                                            } else {
                                                "Download Update (${releaseInfo.latestReleaseTag})"
                                            }
                                        },
                                        icon = LineIconType.CLOUD_UPLOAD,
                                        primary = releaseInfo.state == UpdateState.UPDATE_AVAILABLE,
                                        onClick = {
                                            if (releaseInfo.state == UpdateState.DOWNLOADING ||
                                                releaseInfo.state == UpdateState.VERIFYING
                                            ) return@GlassActionPill

                                            scope.launch {
                                                downloadProgressPct = 0
                                                val verifiedResult = GitHubSyncHelper.downloadAndVerifyReleaseApk(
                                                    context = context,
                                                    releaseInfo = releaseInfo,
                                                    onStateChange = { st, pct, msg ->
                                                        downloadProgressPct = pct
                                                        releaseInfo = releaseInfo.copy(
                                                            state = st,
                                                            statusMessage = msg
                                                        )
                                                    }
                                                )
                                                releaseInfo = verifiedResult
                                                onShowToast(verifiedResult.statusMessage)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("setting_download_update_button")
                                    )
                                }

                                // Primary Action 3: Install Update (opens Android Package Installer for verified APK)
                                if (releaseInfo.state == UpdateState.READY_TO_INSTALL ||
                                    releaseInfo.state == UpdateState.INSTALLATION_REQUESTED
                                ) {
                                    GlassActionPill(
                                        label = if (isKm) "ដំឡើងកំណែថ្មី (Install Update)" else "Install Update",
                                        icon = LineIconType.CHECK,
                                        primary = true,
                                        onClick = {
                                            val launched = GitHubSyncHelper.launchApkPackageInstaller(
                                                context = context,
                                                verifiedApkPath = releaseInfo.verifiedApkPath
                                            )
                                            releaseInfo = if (launched) {
                                                releaseInfo.copy(
                                                    state = UpdateState.INSTALLATION_REQUESTED,
                                                    statusMessage = UpdateState.INSTALLATION_REQUESTED.statusTextEn
                                                )
                                            } else {
                                                releaseInfo.copy(
                                                    state = UpdateState.FAILED,
                                                    statusMessage = UpdateState.FAILED.statusTextEn,
                                                    errorDetail = "Could not launch Android Package Installer."
                                                )
                                            }
                                            onShowToast(releaseInfo.statusMessage)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("setting_install_update_button")
                                    )
                                }

                                // Secondary Actions Row: View Release Notes & Open GitHub Repository
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SettingsActionBtn(
                                        label = if (isKm) "មើលកំណត់ត្រាកំណែថ្មី" else "View Release Notes",
                                        testTag = "setting_view_release_notes_button",
                                        onClick = { showReleaseNotesDialog = true }
                                    )
                                    SettingsActionBtn(
                                        label = if (isKm) "បើក GitHub Repository" else "Open GitHub Repository",
                                        testTag = "setting_open_github_repo_button",
                                        onClick = {
                                            GitHubSyncHelper.openOfficialGitHubRepository(
                                                context = context,
                                                url = releaseInfo.htmlUrl
                                            )
                                        }
                                    )
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

    if (showReleaseNotesDialog) {
        Dialog(onDismissRequest = { showReleaseNotesDialog = false }) {
            GlassPanel(
                elevated = true,
                cornerRadius = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("release_notes_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "GitHub Release Notes (${releaseInfo.latestReleaseTag})",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        GlassIconButton(
                            icon = LineIconType.CLOSE,
                            contentDesc = "Close",
                            sizeDp = 36.dp,
                            onClick = { showReleaseNotesDialog = false },
                            testTagName = "close_release_notes_button"
                        )
                    }

                    Text(
                        text = "Repository: ${GitHubSyncHelper.OFFICIAL_REPO_SLUG} • Published: ${releaseInfo.releaseDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.accentStart,
                        fontWeight = FontWeight.SemiBold
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = releaseInfo.releaseNotes.ifBlank {
                                "No release notes loaded yet. Tap 'Check Update' to query published releases from ${GitHubSyncHelper.OFFICIAL_REPO_SLUG}.\n\n" +
                                    "Release Policy:\n" +
                                    "• Only published stable GitHub Releases trigger updates.\n" +
                                    "• Drafts and unpublished commits are ignored.\n" +
                                    "• Signed APK is verified via SHA-256 and signing certificate."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textPrimary
                        )
                    }

                    if (releaseInfo.expectedSha256.isNotBlank() || releaseInfo.verifiedSha256.isNotBlank()) {
                        Text(
                            text = "SHA-256: ${(releaseInfo.verifiedSha256.ifBlank { releaseInfo.expectedSha256 })}",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassActionPill(
                            label = "Open on GitHub",
                            icon = LineIconType.SHARE,
                            onClick = {
                                GitHubSyncHelper.openOfficialGitHubRepository(
                                    context = context,
                                    url = releaseInfo.htmlUrl
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        GlassActionPill(
                            label = "Done",
                            icon = LineIconType.CHECK,
                            primary = true,
                            onClick = { showReleaseNotesDialog = false },
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateMetaRow(
    label: String,
    value: String,
    valueTestTag: String,
    valueColor: Color? = null
) {
    val tokens = LocalGlassTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tokens.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor ?: tokens.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(valueTestTag)
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
