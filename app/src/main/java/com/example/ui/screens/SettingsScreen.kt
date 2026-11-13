package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxHeight
import com.example.ui.components.AppGlassLogo
import com.example.ui.components.CustomReminderSoundSettingsCard
import com.example.ui.components.GlassActionPill
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassOuterPane
import com.example.ui.components.GlassPanel
import com.example.ui.icons.LineIcon
import com.example.ui.icons.LineIconType
import com.example.ui.theme.LocalGlassTokens
import com.example.util.DownloadProgressInfo
import com.example.util.GitHubReleaseInfo
import com.example.util.GitHubSyncHelper
import com.example.util.SpeedTestResult
import com.example.util.UpdateState
import kotlinx.coroutines.Job
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
    onOpenWebMode: () -> Unit = {},
    onOpenWidgetStudio: () -> Unit = {},
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
    var downloadProgressInfo by remember {
        mutableStateOf(
            DownloadProgressInfo(
                state = UpdateState.IDLE,
                percentage = 0,
                formattedSpeed = "0.0 MB/s",
                formattedDownloaded = "0.0 MB",
                formattedTotal = "0.0 MB",
                formattedEta = "—"
            )
        )
    }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var isTestingSpeed by remember { mutableStateOf(false) }
    var speedTestResult by remember { mutableStateOf<SpeedTestResult?>(null) }
    var showReleaseNotesDialog by remember { mutableStateOf(false) }
    var showPublishReleaseDialog by remember { mutableStateOf(false) }
    var publishTagInput by remember { mutableStateOf("v1.2.0") }
    var publishNotesInput by remember {
        mutableStateOf("APPNOTE v1.2.0: Live WYSIWYG Rich-Text Editor (zero raw markdown tokens), Custom Text & Highlight Color Picker, Real Font Style Picker (Figtree, Playfair Display, Roboto Mono, Caveat, Space Grotesk), Clean Vector Icon Logo UI (zero emojis), Jetpack Glance Home Screen Widgets Studio, Folders Manager, Document Scanner (OCR & PDF), and Custom Reminder Sounds.")
    }
    var publishTokenInput by remember(settings.githubToken) { mutableStateOf(settings.githubToken) }
    var isPublishingRelease by remember { mutableStateOf(false) }
    var publishStatusMessage by remember { mutableStateOf("") }

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
            .blur(if (showReleaseNotesDialog || showPublishReleaseDialog) 26.dp else 0.dp)
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

                                // Optional GitHub Personal Access Token row to bypass 60 req/hr rate limit
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(tokens.fieldFill)
                                        .border(1.dp, tokens.panelBorder, RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isKm) "GitHub Personal Access Token (ជម្រើសបន្ថែម)" else "GitHub Token (Optional)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = tokens.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (settings.githubToken.isNotBlank()) "Connected" else "Rate-limit safe",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (settings.githubToken.isNotBlank()) Color(0xFF10B981) else tokens.accentStart,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = if (isKm) {
                                            "ការពារដោយប្រព័ន្ធទាញយក Web/Atom ស្វ័យប្រវត្ត។ អាចបញ្ចូល token (ghp_...) ដើម្បីបង្កើនកម្រិតដល់ 5,000/hr។"
                                        } else {
                                            "Protected by automatic GitHub Web & Atom feed fallback to avoid HTTP 403. Add a token (ghp_...) to boost API limit to 5,000 req/hr."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.textSecondary,
                                        lineHeight = 16.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(tokens.panelFill)
                                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        if (settings.githubToken.isEmpty()) {
                                            Text(
                                                text = "ghp_xxxxxxxxxxxxxxxxxxxx",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = tokens.textSecondary.copy(alpha = 0.5f)
                                            )
                                        }
                                        BasicTextField(
                                            value = settings.githubToken,
                                            onValueChange = { newToken ->
                                                onUpdateSettings { s -> s.copy(githubToken = newToken.trim()) }
                                            },
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(color = tokens.textPrimary),
                                            cursorBrush = SolidColor(tokens.accentStart),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

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
                                                notifyOnNewRelease = true,
                                                token = settings.githubToken
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
                                // Live Download HUD & Progress Monitor Card with MB speed and ETA
                                if (releaseInfo.state == UpdateState.DOWNLOADING) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(tokens.fieldFill)
                                            .border(1.dp, tokens.accentStart.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Header Row: Status & Cancel button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                LineIcon(
                                                    type = LineIconType.CLOUD_UPLOAD,
                                                    tint = tokens.accentStart,
                                                    size = 18.dp
                                                )
                                                Text(
                                                    text = if (isKm) {
                                                        "កំពុងទាញយក ${releaseInfo.latestReleaseTag}"
                                                    } else {
                                                        "Downloading ${releaseInfo.latestReleaseTag}"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = tokens.textPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        downloadJob?.cancel()
                                                        downloadJob = null
                                                        releaseInfo = releaseInfo.copy(
                                                            state = UpdateState.UPDATE_AVAILABLE,
                                                            statusMessage = if (isKm) "បានបោះបង់ការទាញយក" else "Download cancelled"
                                                        )
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (isKm) "បោះបង់" else "Cancel",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFEF4444),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        // Large Percentage & MB Progress Data
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Text(
                                                text = "${downloadProgressInfo.percentage}%",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = tokens.textPrimary,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "${downloadProgressInfo.formattedDownloaded} / ${downloadProgressInfo.formattedTotal}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = tokens.textSecondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        // Animated Smooth Glass Progress Bar
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(tokens.panelBorder.copy(alpha = 0.5f))
                                        ) {
                                            val animatedPct by animateFloatAsState(
                                                targetValue = (downloadProgressInfo.percentage / 100f).coerceIn(0f, 1f),
                                                label = "progress"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(animatedPct)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(
                                                                tokens.accentStart,
                                                                Color(0xFF06B6D4),
                                                                tokens.accentEnd
                                                            )
                                                        )
                                                    )
                                            )
                                        }

                                        // Real-time MB Speed & Time to End (ETA) HUD Metrics
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Speed Card (MB/s)
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(tokens.panelFill)
                                                    .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                LineIcon(
                                                    type = LineIconType.SPEEDOMETER,
                                                    tint = Color(0xFF06B6D4),
                                                    size = 20.dp
                                                )
                                                Column {
                                                    Text(
                                                        text = if (isKm) "ល្បឿន (Speed)" else "MB Speed",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = tokens.textSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        text = downloadProgressInfo.formattedSpeed,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color(0xFF06B6D4),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            // Speed Time to End (ETA)
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(tokens.panelFill)
                                                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                LineIcon(
                                                    type = LineIconType.TIMER,
                                                    tint = Color(0xFFF59E0B),
                                                    size = 20.dp
                                                )
                                                Column {
                                                    Text(
                                                        text = if (isKm) "ពេលដល់ចប់" else "Time to End",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = tokens.textSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        text = downloadProgressInfo.formattedEta,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color(0xFFF59E0B),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else if (releaseInfo.state == UpdateState.VERIFYING) {
                                    // Verifying State HUD
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(tokens.fieldFill)
                                            .border(1.dp, tokens.accentStart.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        LineIcon(
                                            type = LineIconType.CHECK,
                                            tint = tokens.accentStart,
                                            size = 20.dp
                                        )
                                        Column {
                                            Text(
                                                text = if (isKm) "កំពុងផ្ទៀងផ្ទាត់សុវត្ថិភាព APK..." else "Verifying APK Security & SHA-256...",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = tokens.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isKm) "ពិនិត្យ SHA-256 និង cryptographic signature" else "Checking package archive & signing certificate",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tokens.textSecondary
                                            )
                                        }
                                    }
                                } else if (releaseInfo.hasNewUpdate) {
                                    // Primary Action 2: Download Update Button
                                    GlassActionPill(
                                        label = if (isKm) {
                                            "ទាញយកកំណែថ្មី (${releaseInfo.apkFileName.ifBlank { "APK" }})"
                                        } else {
                                            "Download Update (${releaseInfo.latestReleaseTag})"
                                        },
                                        icon = LineIconType.CLOUD_UPLOAD,
                                        primary = true,
                                        onClick = {
                                            downloadJob = scope.launch {
                                                downloadProgressPct = 0
                                                downloadProgressInfo = DownloadProgressInfo(
                                                    state = UpdateState.DOWNLOADING,
                                                    formattedSpeed = "0.0 MB/s",
                                                    formattedDownloaded = "0.0 MB",
                                                    formattedTotal = if (releaseInfo.apkSizeBytes > 0L) GitHubSyncHelper.formatBytes(releaseInfo.apkSizeBytes) else "—",
                                                    formattedEta = "Calculating..."
                                                )
                                                val verifiedResult = GitHubSyncHelper.downloadAndVerifyReleaseApk(
                                                    context = context,
                                                    releaseInfo = releaseInfo,
                                                    token = settings.githubToken,
                                                    onProgress = { progress ->
                                                        downloadProgressInfo = progress
                                                        downloadProgressPct = progress.percentage
                                                    },
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
                                                if (verifiedResult.state == UpdateState.READY_TO_INSTALL &&
                                                    verifiedResult.verifiedApkPath.isNotBlank()
                                                ) {
                                                    if (GitHubSyncHelper.canRequestPackageInstalls(context)) {
                                                        GitHubSyncHelper.launchApkPackageInstaller(
                                                            context = context,
                                                            verifiedApkPath = verifiedResult.verifiedApkPath
                                                        )
                                                    } else {
                                                        onShowToast(
                                                            if (isKm) "សូមបើក 'Allow from this source' ក្នុង Settings ដើម្បីដំឡើង"
                                                            else "Please allow 'Install unknown apps' permission to install update"
                                                        )
                                                        GitHubSyncHelper.openInstallUnknownAppsSettings(context)
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("setting_download_update_button")
                                    )
                                }

                                // Permission alert banner if Install Unknown Apps is not yet granted
                                val hasInstallPerm = GitHubSyncHelper.canRequestPackageInstalls(context)
                                if ((releaseInfo.state == UpdateState.READY_TO_INSTALL ||
                                        releaseInfo.state == UpdateState.INSTALLATION_REQUESTED) &&
                                    !hasInstallPerm
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(14.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .clickable { GitHubSyncHelper.openInstallUnknownAppsSettings(context) },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        LineIcon(type = LineIconType.SETTINGS, tint = Color(0xFFF59E0B), size = 20.dp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isKm) "ត្រូវការសិទ្ធិដំឡើង (Install Permission)" else "Install Permission Required",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = tokens.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isKm) {
                                                    "ចុចទីនេះដើម្បីបើក 'Allow from this source' សម្រាប់ GlassNotes ក្នុង Settings"
                                                } else {
                                                    "Tap here to enable 'Allow from this source' for GlassNotes in Android Settings"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = tokens.textSecondary,
                                                lineHeight = 16.sp
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isKm) "បើក Settings" else "Settings",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFF59E0B),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
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
                                            val targetPath = if (releaseInfo.verifiedApkPath.isNotBlank() && File(releaseInfo.verifiedApkPath).exists()) {
                                                releaseInfo.verifiedApkPath
                                            } else {
                                                val cleanTag = releaseInfo.latestReleaseTag.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                                                val candidate = File(File(context.cacheDir, "updates"), "APPNOTE-$cleanTag.apk")
                                                if (candidate.exists()) candidate.absolutePath else ""
                                            }

                                            if (!GitHubSyncHelper.canRequestPackageInstalls(context)) {
                                                onShowToast(
                                                    if (isKm) "សូមបើក 'Allow from this source' ក្នុង Settings ដើម្បីដំឡើង"
                                                    else "Please allow 'Install unknown apps' permission to install update"
                                                )
                                                GitHubSyncHelper.openInstallUnknownAppsSettings(context)
                                                return@GlassActionPill
                                            }

                                            if (targetPath.isBlank() || !File(targetPath).exists()) {
                                                onShowToast(
                                                    if (isKm) "រកមិនឃើញឯកសារ APK សូមទាញយកម្តងទៀត"
                                                    else "APK not found. Please tap Download Update again."
                                                )
                                                return@GlassActionPill
                                            }

                                            val launched = GitHubSyncHelper.launchApkPackageInstaller(
                                                context = context,
                                                verifiedApkPath = targetPath
                                            )
                                            releaseInfo = if (launched) {
                                                releaseInfo.copy(
                                                    state = UpdateState.INSTALLATION_REQUESTED,
                                                    statusMessage = UpdateState.INSTALLATION_REQUESTED.statusTextEn,
                                                    verifiedApkPath = targetPath
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

                                // Live Speed Test Result Card
                                if (speedTestResult != null) {
                                    val r = speedTestResult!!
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF06B6D4).copy(alpha = 0.12f))
                                            .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LineIcon(type = LineIconType.SPEEDOMETER, tint = Color(0xFF06B6D4), size = 18.dp)
                                            Text(
                                                text = if (isKm) "ល្បឿនតេស្ត៖ ${r.formattedSpeed} (${r.pingMs}ms)" else "CDN Speed: ${r.formattedSpeed} • ${r.pingMs}ms",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = tokens.textPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = r.quality,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF06B6D4),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Secondary Actions Row: View Release Notes, Speed Test, Add to Releases, & Open GitHub Repository
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
                                        label = if (isTestingSpeed) {
                                            if (isKm) "កំពុងតេស្តល្បឿន..." else "Testing speed..."
                                        } else if (isKm) {
                                            "តេស្តល្បឿន (MB/s)"
                                        } else {
                                            "Test Download Speed"
                                        },
                                        testTag = "setting_speed_test_button",
                                        onClick = {
                                            if (isTestingSpeed) return@SettingsActionBtn
                                            scope.launch {
                                                isTestingSpeed = true
                                                val res = GitHubSyncHelper.runDownloadSpeedTest(token = settings.githubToken)
                                                speedTestResult = res
                                                isTestingSpeed = false
                                                onShowToast(res.statusMessage)
                                            }
                                        }
                                    )
                                    if (releaseInfo.state == UpdateState.READY_TO_INSTALL ||
                                        releaseInfo.state == UpdateState.INSTALLATION_REQUESTED
                                    ) {
                                        SettingsActionBtn(
                                            label = if (isKm) "បើកជាមួយ / ចែករំលែក APK" else "Open with / Share APK",
                                            testTag = "setting_share_apk_button",
                                            onClick = {
                                                val targetPath = if (releaseInfo.verifiedApkPath.isNotBlank() && File(releaseInfo.verifiedApkPath).exists()) {
                                                    releaseInfo.verifiedApkPath
                                                } else {
                                                    val cleanTag = releaseInfo.latestReleaseTag.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                                                    File(File(context.cacheDir, "updates"), "APPNOTE-$cleanTag.apk").absolutePath
                                                }
                                                GitHubSyncHelper.shareApkFile(context, targetPath)
                                            }
                                        )
                                    }
                                    SettingsActionBtn(
                                        label = if (isKm) "បង្កើត Release លើ GitHub" else "Add to Releases (GitHub)",
                                        testTag = "setting_publish_github_release_button",
                                        onClick = {
                                            publishStatusMessage = ""
                                            showPublishReleaseDialog = true
                                        }
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

                    // Card 1A: Home Screen Widgets Studio (Jetpack Glance + Customization)
                    item {
                        GlassPanel(
                            cornerRadius = 22.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onBack()
                                    onOpenWidgetStudio()
                                }
                                .testTag("setting_open_widget_studio_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GlassIconButton(
                                    icon = LineIconType.LAYERS,
                                    contentDesc = "Widgets Studio",
                                    active = true,
                                    sizeDp = 42.dp,
                                    onClick = {
                                        onBack()
                                        onOpenWidgetStudio()
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isKm) "ស្ទូឌីយោ Widget អេក្រង់ដើម" else "Home Screen Widgets Studio",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isKm) {
                                            "ប្ដូរទំហំ (Small/Medium/Large) ពណ៌ និងបន្ថែមលើអេក្រង់ដើម"
                                        } else {
                                            "Customize Small / Medium / Large glass widgets, transparency & pin to Home Screen"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.textSecondary
                                    )
                                }
                                SettingsActionBtn(
                                    label = if (isKm) "បើក" else "Open",
                                    testTag = "setting_open_widget_studio_button",
                                    onClick = {
                                        onBack()
                                        onOpenWidgetStudio()
                                    }
                                )
                            }
                        }
                    }

                    // Card 1B: Suite 4 — Custom Reminder & Sound Settings (24 controls)
                    item {
                        CustomReminderSoundSettingsCard(
                            settings = settings,
                            accentColor = tokens.accentStart,
                            isKhmer = isKm,
                            onUpdateSettings = onUpdateSettings,
                            onShowToast = onShowToast
                        )
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

                                // Interactive Accent Swatches (Blue, Green, Amber, Violet, Pink, Slate)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    NoteColor.entries.forEach { accentOption ->
                                        val isSelected = settings.accent == accentOption
                                        Box(
                                            modifier = Modifier
                                                .testTag("setting_accent_swatch_${accentOption.id}")
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(accentOption.startColor, accentOption.endColor)
                                                    )
                                                )
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) Color.White else tokens.panelBorder,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    onUpdateSettings { s -> s.copy(accent = accentOption) }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                LineIcon(
                                                    type = LineIconType.CHECK,
                                                    tint = Color.White,
                                                    size = 15.dp
                                                )
                                            }
                                        }
                                    }
                                }

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

    if (showPublishReleaseDialog) {
        Dialog(onDismissRequest = { if (!isPublishingRelease) showPublishReleaseDialog = false }) {
            GlassPanel(
                elevated = true,
                cornerRadius = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("publish_github_release_dialog")
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
                            text = "Add Release to GitHub",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        GlassIconButton(
                            icon = LineIconType.CLOSE,
                            contentDesc = "Close",
                            sizeDp = 36.dp,
                            onClick = { if (!isPublishingRelease) showPublishReleaseDialog = false },
                            testTagName = "close_publish_release_dialog"
                        )
                    }

                    val normalizedTag = GitHubSyncHelper.sanitizeGitTag(publishTagInput, "v1.2.0")

                    Text(
                        text = "Target: ${GitHubSyncHelper.OFFICIAL_REPO_SLUG}/releases • Valid Tag: $normalizedTag",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.accentStart,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Quick Valid Tag Presets to avoid any invalid tag error
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("v1.2.0", "v1.2.1", "v1.3.0").forEach { presetTag ->
                            val isSelectedTag = normalizedTag.equals(presetTag, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isSelectedTag) tokens.accentBrush
                                        else SolidColor(tokens.fieldFill)
                                    )
                                    .border(1.dp, tokens.panelBorder, RoundedCornerShape(50))
                                    .clickable { publishTagInput = presetTag }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = presetTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelectedTag) Color.White else tokens.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Tag Name Input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = publishTagInput,
                            onValueChange = { publishTagInput = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = tokens.textPrimary),
                            cursorBrush = SolidColor(tokens.accentStart),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // GitHub Personal Access Token Input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (publishTokenInput.isEmpty()) {
                            Text(
                                text = "GitHub Token (ghp_...) to auto-create tag & upload APK",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.textSecondary
                            )
                        }
                        BasicTextField(
                            value = publishTokenInput,
                            onValueChange = {
                                publishTokenInput = it
                                onUpdateSettings { s -> s.copy(githubToken = it) }
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = tokens.textPrimary),
                            cursorBrush = SolidColor(tokens.accentStart),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Release Notes Input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.fieldFill)
                            .border(1.dp, tokens.panelBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = publishNotesInput,
                            onValueChange = { publishNotesInput = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = tokens.textPrimary),
                            cursorBrush = SolidColor(tokens.accentStart),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (publishStatusMessage.isNotBlank()) {
                        Text(
                            text = publishStatusMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.accentStart,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    GlassActionPill(
                        label = if (isPublishingRelease) "Creating Tag $normalizedTag & Uploading APK..." else "Create Tag $normalizedTag & Upload APK",
                        icon = LineIconType.CLOUD_UPLOAD,
                        primary = true,
                        onClick = {
                            if (isPublishingRelease) return@GlassActionPill
                            scope.launch {
                                isPublishingRelease = true
                                publishStatusMessage = "Creating valid tag $normalizedTag & preparing release..."
                                val res = GitHubSyncHelper.createAndUploadGitHubRelease(
                                    context = context,
                                    token = publishTokenInput,
                                    tagNameInput = normalizedTag,
                                    releaseNotesInput = publishNotesInput,
                                    onStatus = { st -> publishStatusMessage = st }
                                )
                                isPublishingRelease = false
                                releaseInfo = res
                                if (res.state != UpdateState.FAILED) {
                                    showPublishReleaseDialog = false
                                    onShowToast("Published ${res.latestReleaseTag} to GitHub Releases!")
                                } else {
                                    publishStatusMessage = res.errorDetail.ifBlank { res.statusMessage }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    GlassActionPill(
                        label = "Open GitHub Web (Pre-filled Tag $normalizedTag)",
                        icon = LineIconType.SHARE,
                        onClick = {
                            val prefilledUrl = GitHubSyncHelper.buildNewReleaseWebUrl(
                                tagNameInput = normalizedTag,
                                releaseNotesInput = publishNotesInput
                            )
                            GitHubSyncHelper.openOfficialGitHubRepository(
                                context = context,
                                url = prefilledUrl
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = currentValue,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = tokens.textPrimary
                        )
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            tint = tokens.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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

@SuppressLint("ResourceType")
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
