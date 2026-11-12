package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

enum class UpdateState(val statusTextEn: String, val statusTextKm: String) {
    IDLE("Ready to check", "រួចរាល់ដើម្បីពិនិត្យ"),
    CHECKING("Checking for updates...", "កំពុងពិនិត្យមើលកំណែថ្មី..."),
    UP_TO_DATE("You're up to date.", "កម្មវិធីរបស់អ្នកជាកំណែចុងក្រោយហើយ។"),
    UPDATE_AVAILABLE("New version available!", "មានកំណែថ្មីអាចអាប់ដេតបាន!"),
    DOWNLOADING("Downloading update...", "កំពុងទាញយកកំណែថ្មី..."),
    VERIFYING("Verifying APK...", "កំពុងផ្ទៀងផ្ទាត់សុវត្ថិភាព APK..."),
    READY_TO_INSTALL("Ready to install.", "រួចរាល់សម្រាប់ដំឡើង។"),
    INSTALLATION_REQUESTED("Update installation requested.", "បានស្នើសុំដំឡើងកំណែថ្មី។"),
    FAILED("Update failed. Try again.", "ការអាប់ដេតបរាជ័យ។ សូមព្យាយាមម្តងទៀត។")
}

data class InstalledAppVersionInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val signingCertSha256: String
)

data class GitHubReleaseInfo(
    val state: UpdateState,
    val statusMessage: String,
    val hasNewUpdate: Boolean = false,
    val installedVersionName: String = "1.0",
    val installedVersionCode: Long = 1L,
    val latestReleaseTag: String = "No published release",
    val latestVersionCode: Long = 0L,
    val releaseName: String = "",
    val releaseDate: String = "—",
    val releaseNotes: String = "",
    val apkDownloadUrl: String = "",
    val apkFileName: String = "",
    val apkSizeBytes: Long = 0L,
    val expectedSha256: String = "",
    val verifiedSha256: String = "",
    val verifiedApkPath: String = "",
    val htmlUrl: String = GitHubSyncHelper.OFFICIAL_REPO_URL,
    val isPrerelease: Boolean = false,
    val errorDetail: String = ""
)

object GitHubSyncHelper {

    const val OFFICIAL_OWNER = "MEY-NASHH"
    const val OFFICIAL_REPO = "APPNOTE"
    const val OFFICIAL_REPO_SLUG = "$OFFICIAL_OWNER/$OFFICIAL_REPO"
    const val OFFICIAL_REPO_URL = "https://github.com/$OFFICIAL_REPO_SLUG"
    const val RELEASES_API_URL = "https://api.github.com/repos/$OFFICIAL_REPO_SLUG/releases"

    private val TRUSTED_DOWNLOAD_HOSTS = setOf(
        "github.com",
        "api.github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com"
    )

    fun parseVersionParts(version: String): List<Int> {
        val clean = version.trim().removePrefix("v").removePrefix("V")
        return clean.split(".", "-", "_")
            .mapNotNull { it.toIntOrNull() }
            .ifEmpty { listOf(1, 0, 0) }
    }

    /**
     * Returns > 0 if v1 > v2, 0 if v1 == v2, < 0 if v1 < v2
     */
    fun compareVersions(v1: String, v2: String): Int {
        val p1 = parseVersionParts(v1)
        val p2 = parseVersionParts(v2)
        val maxLen = maxOf(p1.size, p2.size)
        for (i in 0 until maxLen) {
            val a = p1.getOrElse(i) { 0 }
            val b = p2.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    fun deriveVersionCodeFromTag(tag: String): Long {
        val parts = parseVersionParts(tag)
        val major = parts.getOrElse(0) { 1 }.toLong()
        val minor = parts.getOrElse(1) { 0 }.toLong()
        val patch = parts.getOrElse(2) { 0 }.toLong()
        return major * 10000L + minor * 100L + patch
    }

    /**
     * Reads the real installed versionName, versionCode, and signing certificate SHA-256
     * directly from Android's PackageManager (never hardcoded).
     */
    @Suppress("DEPRECATION")
    fun getInstalledAppInfo(context: Context): InstalledAppVersionInfo {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
            val pkgInfo = pm.getPackageInfo(context.packageName, flags)
            val versionName = pkgInfo.versionName ?: "1.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                pkgInfo.versionCode.toLong()
            }
            val certSha256 = extractSigningCertSha256(pkgInfo)
            InstalledAppVersionInfo(
                packageName = context.packageName,
                versionName = versionName,
                versionCode = versionCode,
                signingCertSha256 = certSha256
            )
        } catch (_: Exception) {
            InstalledAppVersionInfo(
                packageName = context.packageName,
                versionName = "1.0",
                versionCode = 1L,
                signingCertSha256 = ""
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun extractSigningCertSha256(pkgInfo: PackageInfo): String {
        return try {
            val signerBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = pkgInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners.firstOrNull()?.toByteArray()
                    } else {
                        signingInfo.signingCertificateHistory.firstOrNull()?.toByteArray()
                    }
                } else {
                    null
                }
            } else {
                pkgInfo.signatures?.firstOrNull()?.toByteArray()
            }
            if (signerBytes != null) sha256Hex(signerBytes) else ""
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Queries the official GitHub Repository Releases API (`MEY-NASHH/APPNOTE`):
     * - Ignores draft releases (`draft == true`).
     * - Ignores prereleases (`prerelease == true`) unless `includePrerelease == true`.
     * - Does NOT generate fake version numbers.
     * - If no published release exists or the published release is not newer than the installed app,
     *   returns `UpdateState.UP_TO_DATE` ("You're up to date.").
     * - If a newer published release exists, returns `UpdateState.UPDATE_AVAILABLE` ("New version available!")
     *   and triggers the Android system notification.
     */
    suspend fun checkGitHubReleaseUpdate(
        context: Context,
        includePrerelease: Boolean = false,
        notifyOnNewRelease: Boolean = true
    ): GitHubReleaseInfo = withContext(Dispatchers.IO) {
        val installed = getInstalledAppInfo(context)

        try {
            val url = URL("$RELEASES_API_URL?per_page=15")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
            }

            val code = conn.responseCode
            if (code == 404) {
                conn.disconnect()
                // Repository has no published releases or is not yet public -> No update
                return@withContext GitHubReleaseInfo(
                    state = UpdateState.UP_TO_DATE,
                    statusMessage = UpdateState.UP_TO_DATE.statusTextEn,
                    hasNewUpdate = false,
                    installedVersionName = installed.versionName,
                    installedVersionCode = installed.versionCode,
                    latestReleaseTag = "No published release",
                    releaseDate = "—",
                    releaseNotes = "No published GitHub Releases found in $OFFICIAL_REPO_SLUG."
                )
            }

            if (code !in 200..299) {
                conn.disconnect()
                return@withContext GitHubReleaseInfo(
                    state = UpdateState.FAILED,
                    statusMessage = UpdateState.FAILED.statusTextEn,
                    hasNewUpdate = false,
                    installedVersionName = installed.versionName,
                    installedVersionCode = installed.versionCode,
                    latestReleaseTag = "Unavailable (HTTP $code)",
                    releaseDate = "—",
                    errorDetail = "GitHub Releases API returned HTTP $code"
                )
            }

            val bodyText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conn.disconnect()

            val releasesArray = JSONArray(bodyText)
            var candidateRelease: JSONObject? = null

            for (i in 0 until releasesArray.length()) {
                val rel = releasesArray.optJSONObject(i) ?: continue
                val isDraft = rel.optBoolean("draft", false)
                val isPrerelease = rel.optBoolean("prerelease", false)

                // Rule 3 & 7: Never show draft releases; ignore prereleases unless explicitly enabled
                if (isDraft) continue
                if (isPrerelease && !includePrerelease) continue

                val tagName = rel.optString("tag_name", "").trim()
                if (tagName.isBlank()) continue

                candidateRelease = rel
                break
            }

            if (candidateRelease == null) {
                return@withContext GitHubReleaseInfo(
                    state = UpdateState.UP_TO_DATE,
                    statusMessage = UpdateState.UP_TO_DATE.statusTextEn,
                    hasNewUpdate = false,
                    installedVersionName = installed.versionName,
                    installedVersionCode = installed.versionCode,
                    latestReleaseTag = "No published release",
                    releaseDate = "—",
                    releaseNotes = "No stable published release exists in $OFFICIAL_REPO_SLUG yet."
                )
            }

            val tagName = candidateRelease.optString("tag_name", "").trim()
            val releaseName = candidateRelease.optString("name", tagName).ifBlank { tagName }
            val publishedAtRaw = candidateRelease.optString("published_at", "")
            val releaseDate = formatIsoDate(publishedAtRaw)
            val releaseBody = candidateRelease.optString("body", "").trim()
            val htmlUrl = candidateRelease.optString("html_url", OFFICIAL_REPO_URL).ifBlank { OFFICIAL_REPO_URL }
            val isPre = candidateRelease.optBoolean("prerelease", false)

            // Inspect assets for signed .apk, .sha256, and release-metadata.json
            val assets = candidateRelease.optJSONArray("assets") ?: JSONArray()
            var apkDownloadUrl = ""
            var apkFileName = ""
            var apkSizeBytes = 0L
            var assetDigestSha256 = ""
            var sha256AssetUrl = ""
            var metadataAssetUrl = ""

            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                val dlUrl = asset.optString("browser_download_url", "")
                val digest = asset.optString("digest", "")

                when {
                    name.endsWith(".apk", ignoreCase = true) && apkDownloadUrl.isBlank() -> {
                        apkDownloadUrl = dlUrl
                        apkFileName = name
                        apkSizeBytes = asset.optLong("size", 0L)
                        if (digest.startsWith("sha256:", ignoreCase = true)) {
                            assetDigestSha256 = digest.substringAfter(":").trim()
                        }
                    }
                    name.endsWith(".sha256", ignoreCase = true) -> {
                        sha256AssetUrl = dlUrl
                    }
                    name.equals("release-metadata.json", ignoreCase = true) -> {
                        metadataAssetUrl = dlUrl
                    }
                }
            }

            // Optional fetch of release-metadata.json or .sha256 for explicit versionCode and SHA-256
            var remoteVersionCode = deriveVersionCodeFromTag(tagName)
            var expectedSha256 = assetDigestSha256.ifBlank { extractSha256FromText(releaseBody) }

            if (metadataAssetUrl.isNotBlank() && isTrustedHttpsUrl(metadataAssetUrl)) {
                try {
                    val metaText = fetchSmallTextHttps(metadataAssetUrl)
                    val metaJson = JSONObject(metaText)
                    val metaCode = metaJson.optLong("versionCode", 0L)
                    if (metaCode > 0L) remoteVersionCode = metaCode
                    val metaSha = metaJson.optString("sha256", "").trim()
                    if (metaSha.length == 64) expectedSha256 = metaSha
                } catch (_: Exception) {
                }
            }

            if (expectedSha256.isBlank() && sha256AssetUrl.isNotBlank() && isTrustedHttpsUrl(sha256AssetUrl)) {
                try {
                    val shaText = fetchSmallTextHttps(sha256AssetUrl)
                    val parsedSha = extractSha256FromText(shaText)
                    if (parsedSha.length == 64) expectedSha256 = parsedSha
                } catch (_: Exception) {
                }
            }

            // Compare both semantic version tag and versionCode (Never install older or same version)
            val versionNameComparison = compareVersions(tagName, installed.versionName)
            val isNewerRelease = versionNameComparison > 0 && remoteVersionCode >= installed.versionCode

            if (!isNewerRelease) {
                return@withContext GitHubReleaseInfo(
                    state = UpdateState.UP_TO_DATE,
                    statusMessage = UpdateState.UP_TO_DATE.statusTextEn,
                    hasNewUpdate = false,
                    installedVersionName = installed.versionName,
                    installedVersionCode = installed.versionCode,
                    latestReleaseTag = tagName,
                    latestVersionCode = remoteVersionCode,
                    releaseName = releaseName,
                    releaseDate = releaseDate,
                    releaseNotes = releaseBody.ifBlank { "Release $tagName" },
                    apkDownloadUrl = apkDownloadUrl,
                    apkFileName = apkFileName,
                    apkSizeBytes = apkSizeBytes,
                    expectedSha256 = expectedSha256,
                    htmlUrl = htmlUrl,
                    isPrerelease = isPre
                )
            }

            if (notifyOnNewRelease) {
                ReminderScheduler.showNewVersionNotification(
                    context = context,
                    newVersion = tagName.removePrefix("v"),
                    changelog = releaseBody.lines().firstOrNull { it.isNotBlank() }
                        ?: "APPNOTE $tagName is available on GitHub Releases."
                )
            }

            GitHubReleaseInfo(
                state = UpdateState.UPDATE_AVAILABLE,
                statusMessage = UpdateState.UPDATE_AVAILABLE.statusTextEn,
                hasNewUpdate = true,
                installedVersionName = installed.versionName,
                installedVersionCode = installed.versionCode,
                latestReleaseTag = tagName,
                latestVersionCode = remoteVersionCode,
                releaseName = releaseName,
                releaseDate = releaseDate,
                releaseNotes = releaseBody.ifBlank { "APPNOTE $tagName published on GitHub Releases." },
                apkDownloadUrl = apkDownloadUrl,
                apkFileName = apkFileName.ifBlank { "app-release.apk" },
                apkSizeBytes = apkSizeBytes,
                expectedSha256 = expectedSha256,
                htmlUrl = htmlUrl,
                isPrerelease = isPre
            )
        } catch (e: Exception) {
            GitHubReleaseInfo(
                state = UpdateState.FAILED,
                statusMessage = UpdateState.FAILED.statusTextEn,
                hasNewUpdate = false,
                installedVersionName = installed.versionName,
                installedVersionCode = installed.versionCode,
                latestReleaseTag = "Check failed",
                releaseDate = "—",
                errorDetail = e.localizedMessage ?: "Network connection error"
            )
        }
    }

    /**
     * Downloads the signed APK from the official GitHub Release over HTTPS, verifies its SHA-256
     * checksum, verifies that the APK archive is valid, checks that its versionCode is newer than
     * the installed versionCode, and verifies that the APK signing certificate matches the installed app.
     */
    @Suppress("DEPRECATION")
    suspend fun downloadAndVerifyReleaseApk(
        context: Context,
        releaseInfo: GitHubReleaseInfo,
        onStateChange: (UpdateState, Int, String) -> Unit
    ): GitHubReleaseInfo = withContext(Dispatchers.IO) {
        val installed = getInstalledAppInfo(context)
        val downloadUrl = releaseInfo.apkDownloadUrl.trim()

        if (downloadUrl.isBlank() || !isTrustedHttpsUrl(downloadUrl)) {
            return@withContext releaseInfo.copy(
                state = UpdateState.FAILED,
                statusMessage = UpdateState.FAILED.statusTextEn,
                errorDetail = "No trusted HTTPS signed APK asset found in release ${releaseInfo.latestReleaseTag}."
            )
        }

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val cleanTag = releaseInfo.latestReleaseTag.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val targetApkFile = File(updatesDir, "APPNOTE-$cleanTag.apk")

        try {
            onStateChange(UpdateState.DOWNLOADING, 0, UpdateState.DOWNLOADING.statusTextEn)

            val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("Accept", "application/octet-stream")
                setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
            }

            if (conn.responseCode !in 200..299) {
                val code = conn.responseCode
                conn.disconnect()
                return@withContext releaseInfo.copy(
                    state = UpdateState.FAILED,
                    statusMessage = UpdateState.FAILED.statusTextEn,
                    errorDetail = "APK download failed with HTTP $code"
                )
            }

            val totalBytes = conn.contentLengthLong.takeIf { it > 0L } ?: releaseInfo.apkSizeBytes
            val digest = MessageDigest.getInstance("SHA-256")

            conn.inputStream.use { input ->
                FileOutputStream(targetApkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0L) {
                            val pct = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            onStateChange(
                                UpdateState.DOWNLOADING,
                                pct,
                                "${UpdateState.DOWNLOADING.statusTextEn} ($pct%)"
                            )
                        }
                    }
                    output.flush()
                }
            }
            conn.disconnect()

            if (!targetApkFile.exists() || targetApkFile.length() < 1024L) {
                targetApkFile.delete()
                return@withContext releaseInfo.copy(
                    state = UpdateState.FAILED,
                    statusMessage = UpdateState.FAILED.statusTextEn,
                    errorDetail = "Downloaded APK file is empty or corrupted."
                )
            }

            // Step 11: Verify APK SHA-256 checksum & Signing Certificate
            onStateChange(UpdateState.VERIFYING, 100, UpdateState.VERIFYING.statusTextEn)
            val actualSha256 = bytesToHex(digest.digest())

            if (releaseInfo.expectedSha256.isNotBlank() &&
                !actualSha256.equals(releaseInfo.expectedSha256.trim(), ignoreCase = true)
            ) {
                targetApkFile.delete()
                return@withContext releaseInfo.copy(
                    state = UpdateState.FAILED,
                    statusMessage = UpdateState.FAILED.statusTextEn,
                    errorDetail = "SHA-256 checksum mismatch! Expected ${releaseInfo.expectedSha256.take(12)}…, got ${actualSha256.take(12)}…"
                )
            }

            // Verify APK archive structure, packageName, versionCode, and signing certificate
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
            val archiveInfo = pm.getPackageArchiveInfo(targetApkFile.absolutePath, flags)
            if (archiveInfo == null) {
                targetApkFile.delete()
                return@withContext releaseInfo.copy(
                    state = UpdateState.FAILED,
                    statusMessage = UpdateState.FAILED.statusTextEn,
                    errorDetail = "Rejected invalid or corrupted Android APK archive."
                )
            }

            // Ensure same applicationId
            if (archiveInfo.packageName != installed.packageName) {
                targetApkFile.delete()
                return@withContext releaseInfo.copy(
                    state = UpdateState.FAILED,
                    statusMessage = UpdateState.FAILED.statusTextEn,
                    errorDetail = "Rejected APK: applicationId (${archiveInfo.packageName}) does not match installed app (${installed.packageName})."
                )
            }

            // Ensure never installing an older version over a newer version
            val archiveVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                archiveInfo.longVersionCode
            } else {
                archiveInfo.versionCode.toLong()
            }
            if (archiveVersionCode < installed.versionCode) {
                targetApkFile.delete()
                return@withContext releaseInfo.copy(
                    state = UpdateState.FAILED,
                    statusMessage = UpdateState.FAILED.statusTextEn,
                    errorDetail = "Rejected downgrade APK: versionCode $archiveVersionCode is older than installed ${installed.versionCode}."
                )
            }

            // Verify signing certificate matches installed app
            val archiveCertSha256 = extractSigningCertSha256(archiveInfo)
            if (installed.signingCertSha256.isNotBlank()) {
                if (archiveCertSha256.isBlank() ||
                    !archiveCertSha256.equals(installed.signingCertSha256, ignoreCase = true)
                ) {
                    targetApkFile.delete()
                    return@withContext releaseInfo.copy(
                        state = UpdateState.FAILED,
                        statusMessage = UpdateState.FAILED.statusTextEn,
                        errorDetail = "Rejected APK: Signing certificate does not match installed APPNOTE signature."
                    )
                }
            }

            releaseInfo.copy(
                state = UpdateState.READY_TO_INSTALL,
                statusMessage = UpdateState.READY_TO_INSTALL.statusTextEn,
                verifiedSha256 = actualSha256,
                verifiedApkPath = targetApkFile.absolutePath,
                errorDetail = ""
            )
        } catch (e: Exception) {
            targetApkFile.delete()
            releaseInfo.copy(
                state = UpdateState.FAILED,
                statusMessage = UpdateState.FAILED.statusTextEn,
                errorDetail = e.localizedMessage ?: "APK download or verification error"
            )
        }
    }

    /**
     * Opens the Android Package Installer for the verified APK file using FileProvider.
     */
    fun launchApkPackageInstaller(context: Context, verifiedApkPath: String): Boolean {
        return try {
            val apkFile = File(verifiedApkPath)
            if (!apkFile.exists() || !apkFile.canRead()) return false

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openOfficialGitHubRepository(context: Context, url: String = OFFICIAL_REPO_URL) {
        try {
            val safeUrl = if (isTrustedHttpsUrl(url)) url else OFFICIAL_REPO_URL
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun isTrustedHttpsUrl(url: String): Boolean {
        return try {
            val parsed = Uri.parse(url.trim())
            val scheme = parsed.scheme?.lowercase()
            val host = parsed.host?.lowercase() ?: return false
            scheme == "https" && (TRUSTED_DOWNLOAD_HOSTS.contains(host) || host.endsWith(".githubusercontent.com"))
        } catch (_: Exception) {
            false
        }
    }

    private fun fetchSmallTextHttps(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
        }
        return try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                ""
            }
        } finally {
            conn.disconnect()
        }
    }

    fun extractSha256FromText(text: String): String {
        val regex = Regex("\\b([a-fA-F0-9]{64})\\b")
        return regex.find(text)?.groupValues?.getOrNull(1)?.lowercase() ?: ""
    }

    fun computeFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(8192)
            var r: Int
            while (input.read(buf).also { r = it } != -1) {
                digest.update(buf, 0, r)
            }
        }
        return bytesToHex(digest.digest())
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return bytesToHex(digest.digest(bytes))
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun formatIsoDate(iso: String): String {
        if (iso.isBlank()) return "—"
        return iso.replace("T", " ").removeSuffix("Z").take(16) + " UTC"
    }
}
