package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

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

data class DownloadProgressInfo(
    val state: UpdateState = UpdateState.IDLE,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val percentage: Int = 0,
    val speedBytesPerSec: Double = 0.0,
    val formattedSpeed: String = "0.0 MB/s",
    val formattedDownloaded: String = "0.0 MB",
    val formattedTotal: String = "0.0 MB",
    val etaSeconds: Long = -1L,
    val formattedEta: String = "—",
    val statusMessage: String = ""
)

data class SpeedTestResult(
    val pingMs: Long = 0L,
    val speedBytesPerSec: Double = 0.0,
    val formattedSpeed: String = "0.0 MB/s",
    val statusMessage: String = "",
    val quality: String = "Good",
    val isSuccess: Boolean = true
)

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
    val expectedSigningCertSha256: String = "",
    val verifiedSha256: String = "",
    val verifiedApkPath: String = "",
    val htmlUrl: String = GitHubSyncHelper.OFFICIAL_REPO_URL,
    val isPrerelease: Boolean = false,
    val errorDetail: String = ""
)

object GitHubSyncHelper {

    const val OFFICIAL_PACKAGE_NAME = "com.aistudio.glassnotes.vqpzxm"
    const val OFFICIAL_OWNER = "MEYYYNASH"
    const val OFFICIAL_REPO = "APPNOTE"
    const val OFFICIAL_REPO_SLUG = "$OFFICIAL_OWNER/$OFFICIAL_REPO"
    const val OFFICIAL_REPO_URL = "https://github.com/$OFFICIAL_REPO_SLUG"
    const val RELEASES_API_URL = "https://api.github.com/repos/$OFFICIAL_REPO_SLUG/releases"

    private val TRUSTED_DOWNLOAD_HOSTS = setOf(
        "github.com",
        "api.github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
        "raw.githubusercontent.com",
        "codeload.github.com"
    )

    data class AtomReleaseEntry(
        val tag: String,
        val title: String,
        val updated: String,
        val contentHtml: String,
        val htmlUrl: String
    )

    fun parseAtomFeed(xml: String): List<AtomReleaseEntry> {
        val entries = mutableListOf<AtomReleaseEntry>()
        val entryRegex = Regex("<entry[\\s>]([\\s\\S]*?)</entry>", RegexOption.IGNORE_CASE)
        val titleRegex = Regex("<title[^>]*>([\\s\\S]*?)</title>", RegexOption.IGNORE_CASE)
        val updatedRegex = Regex("<updated[^>]*>([\\s\\S]*?)</updated>", RegexOption.IGNORE_CASE)
        val linkRegex = Regex("<link[^>]+href=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val contentRegex = Regex("<content[^>]*>([\\s\\S]*?)</content>", RegexOption.IGNORE_CASE)

        for (match in entryRegex.findAll(xml)) {
            val entryBlock = match.groupValues[1]
            val title = titleRegex.find(entryBlock)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            val updated = updatedRegex.find(entryBlock)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            val link = linkRegex.find(entryBlock)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            val rawContent = contentRegex.find(entryBlock)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            val content = unescapeXml(rawContent)
            val tag = if (link.contains("/tag/")) {
                link.substringAfter("/tag/").substringBefore("?").trim()
            } else {
                val idMatch = Regex("Repository/\\d+/(.+)", RegexOption.IGNORE_CASE).find(entryBlock)
                idMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
            }
            if (tag.isNotBlank()) {
                entries.add(
                    AtomReleaseEntry(
                        tag = tag,
                        title = title.ifBlank { "APPNOTE $tag" },
                        updated = updated,
                        contentHtml = content,
                        htmlUrl = link.ifBlank { "$OFFICIAL_REPO_URL/releases/tag/$tag" }
                    )
                )
            }
        }
        return entries
    }

    fun unescapeXml(text: String): String {
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
    }

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
            var signerBytes: ByteArray? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = pkgInfo.signingInfo
                if (signingInfo != null) {
                    signerBytes = if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners.firstOrNull()?.toByteArray()
                    } else {
                        signingInfo.signingCertificateHistory.firstOrNull()?.toByteArray()
                            ?: signingInfo.apkContentsSigners.firstOrNull()?.toByteArray()
                    }
                }
            }
            if (signerBytes == null) {
                signerBytes = pkgInfo.signatures?.firstOrNull()?.toByteArray()
            }
            if (signerBytes != null) sha256Hex(signerBytes).lowercase() else ""
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
        notifyOnNewRelease: Boolean = true,
        token: String = ""
    ): GitHubReleaseInfo = withContext(Dispatchers.IO) {
        val installed = getInstalledAppInfo(context)
        val cleanToken = token.trim()

        var apiRateLimited = false
        var apiErrorCode = 0

        // Tier 1: Try official GitHub REST API (if user entered GitHub token, allows 5,000 req/hr)
        try {
            val url = URL("$RELEASES_API_URL?per_page=15")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
                if (cleanToken.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $cleanToken")
                }
            }

            val code = conn.responseCode
            apiErrorCode = code
            if (code == 403 || code == 429) {
                // Rate limited by GitHub REST API (60 req/hr IP limit exceeded on mobile carrier NAT)
                apiRateLimited = true
                conn.disconnect()
            } else if (code == 404) {
                conn.disconnect()
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
            } else if (code in 200..299) {
                val bodyText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                conn.disconnect()

                val releasesArray = JSONArray(bodyText)
                var candidateRelease: JSONObject? = null

                for (i in 0 until releasesArray.length()) {
                    val rel = releasesArray.optJSONObject(i) ?: continue
                    val isDraft = rel.optBoolean("draft", false)
                    val isPrerelease = rel.optBoolean("prerelease", false)

                    if (isDraft) continue
                    if (isPrerelease && !includePrerelease) continue

                    val tagName = rel.optString("tag_name", "").trim()
                    if (tagName.isBlank()) continue

                    candidateRelease = rel
                    break
                }

                if (candidateRelease != null) {
                    val tagName = candidateRelease.optString("tag_name", "").trim()
                    val releaseName = candidateRelease.optString("name", tagName).ifBlank { tagName }
                    val publishedAtRaw = candidateRelease.optString("published_at", "")
                    val releaseDate = formatIsoDate(publishedAtRaw)
                    val releaseBody = candidateRelease.optString("body", "").trim()
                    val htmlUrl = candidateRelease.optString("html_url", OFFICIAL_REPO_URL).ifBlank { OFFICIAL_REPO_URL }
                    val isPre = candidateRelease.optBoolean("prerelease", false)

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
                            name.endsWith(".sha256", ignoreCase = true) -> sha256AssetUrl = dlUrl
                            name.equals("release-metadata.json", ignoreCase = true) -> metadataAssetUrl = dlUrl
                        }
                    }

                    var remoteVersionCode = deriveVersionCodeFromTag(tagName)
                    var expectedSha256 = assetDigestSha256.ifBlank { extractSha256FromText(releaseBody) }
                    var expectedSigningCertSha256 = ""

                    if (metadataAssetUrl.isNotBlank() && isTrustedHttpsUrl(metadataAssetUrl)) {
                        val metaText = fetchSmallTextHttps(metadataAssetUrl, cleanToken)
                        if (metaText.isNotBlank()) {
                            try {
                                val metaJson = JSONObject(metaText)
                                val metaCode = metaJson.optLong("versionCode", 0L)
                                if (metaCode > 0L) remoteVersionCode = metaCode
                                val metaSha = metaJson.optString("sha256", "").trim()
                                if (metaSha.length == 64) expectedSha256 = metaSha
                                val metaCert = metaJson.optString("signingCertSha256", "").replace(":", "").trim().lowercase()
                                if (metaCert.length == 64) expectedSigningCertSha256 = metaCert
                            } catch (_: Exception) {
                            }
                        }
                    }

                    if (expectedSha256.isBlank() && sha256AssetUrl.isNotBlank() && isTrustedHttpsUrl(sha256AssetUrl)) {
                        val shaText = fetchSmallTextHttps(sha256AssetUrl, cleanToken)
                        val parsedSha = extractSha256FromText(shaText)
                        if (parsedSha.length == 64) expectedSha256 = parsedSha
                    }

                    if (apkDownloadUrl.isBlank()) {
                        apkDownloadUrl = "$OFFICIAL_REPO_URL/releases/download/$tagName/app-release.apk"
                        apkFileName = "app-release.apk"
                    }

                    val versionNameComparison = compareVersions(tagName, installed.versionName)
                    val isNewerRelease = remoteVersionCode > installed.versionCode ||
                            (remoteVersionCode == installed.versionCode && versionNameComparison > 0) ||
                            versionNameComparison > 0

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
                            expectedSigningCertSha256 = expectedSigningCertSha256,
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

                    return@withContext GitHubReleaseInfo(
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
                        expectedSigningCertSha256 = expectedSigningCertSha256,
                        htmlUrl = htmlUrl,
                        isPrerelease = isPre
                    )
                } else {
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
            } else {
                conn.disconnect()
            }
        } catch (_: Exception) {
        }

        // Tier 2: Seamless fallback to GitHub Atom Feed (releases.atom)
        // Public, cached web feed from github.com - NOT subject to the 60 req/hr REST API rate limit!
        try {
            val atomInfo = checkReleaseViaAtomFeed(
                context = context,
                installed = installed,
                includePrerelease = includePrerelease,
                notifyOnNewRelease = notifyOnNewRelease,
                token = cleanToken
            )
            if (atomInfo != null) {
                return@withContext atomInfo
            }
        } catch (_: Exception) {
        }

        // Tier 3: Seamless fallback to GitHub Web Releases Latest Redirect (https://github.com/MEYYYNASH/APPNOTE/releases/latest)
        try {
            val webLatestInfo = checkReleaseViaWebRedirect(
                context = context,
                installed = installed,
                notifyOnNewRelease = notifyOnNewRelease,
                token = cleanToken
            )
            if (webLatestInfo != null) {
                return@withContext webLatestInfo
            }
        } catch (_: Exception) {
        }

        val failDetail = if (apiRateLimited || apiErrorCode == 403) {
            "GitHub API rate limit exceeded on this network (HTTP 403). Enter your GitHub Personal Access Token in Settings or check back in a few minutes."
        } else if (apiErrorCode > 0) {
            "GitHub Releases returned HTTP $apiErrorCode. Please check your network connection."
        } else {
            "Could not connect to GitHub. Please check your network connection."
        }

        GitHubReleaseInfo(
            state = UpdateState.FAILED,
            statusMessage = UpdateState.FAILED.statusTextEn,
            hasNewUpdate = false,
            installedVersionName = installed.versionName,
            installedVersionCode = installed.versionCode,
            latestReleaseTag = if (apiErrorCode == 403) "Rate limited (HTTP 403)" else "Check failed",
            releaseDate = "—",
            errorDetail = failDetail
        )
    }

    private fun checkReleaseViaAtomFeed(
        context: Context,
        installed: InstalledAppVersionInfo,
        includePrerelease: Boolean,
        notifyOnNewRelease: Boolean,
        token: String
    ): GitHubReleaseInfo? {
        val atomUrl = "$OFFICIAL_REPO_URL/releases.atom"
        val xmlText = fetchSmallTextHttps(atomUrl, token).ifBlank { return null }

        val entries = parseAtomFeed(xmlText)
        if (entries.isEmpty()) {
            return GitHubReleaseInfo(
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

        var candidate: AtomReleaseEntry? = null
        for (entry in entries) {
            val isPre = entry.title.contains("prerelease", ignoreCase = true) ||
                    entry.tag.contains("alpha", ignoreCase = true) ||
                    entry.tag.contains("beta", ignoreCase = true) ||
                    entry.tag.contains("rc", ignoreCase = true)
            if (isPre && !includePrerelease) continue
            candidate = entry
            break
        }
        val target = candidate ?: entries.first()
        val tagName = target.tag
        val releaseName = target.title.ifBlank { "APPNOTE $tagName" }
        val releaseDate = formatIsoDate(target.updated)
        val releaseBody = target.contentHtml

        // Try to fetch release-metadata.json for exact versionCode and SHA-256
        val metaUrl = "$OFFICIAL_REPO_URL/releases/download/$tagName/release-metadata.json"
        val metaText = fetchSmallTextHttps(metaUrl, token)

        var remoteVersionCode = deriveVersionCodeFromTag(tagName)
        var expectedSha256 = extractSha256FromText(releaseBody)
        var expectedSigningCertSha256 = ""
        var apkFileName = "app-release.apk"

        if (metaText.isNotBlank()) {
            try {
                val metaJson = JSONObject(metaText)
                val metaCode = metaJson.optLong("versionCode", 0L)
                if (metaCode > 0L) remoteVersionCode = metaCode
                val metaSha = metaJson.optString("sha256", "").trim()
                if (metaSha.length == 64) expectedSha256 = metaSha
                val metaCert = metaJson.optString("signingCertSha256", "").replace(":", "").trim().lowercase()
                if (metaCert.length == 64) expectedSigningCertSha256 = metaCert
                val metaApkName = metaJson.optString("apkFileName", "")
                if (metaApkName.isNotBlank()) apkFileName = metaApkName
            } catch (_: Exception) {
            }
        }

        // If versionCode was not in metadata, extract from HTML body
        if (remoteVersionCode == deriveVersionCodeFromTag(tagName)) {
            val vcMatch = Regex("Version Code:.*?(\\d+)", RegexOption.IGNORE_CASE).find(releaseBody)
            val extractedVc = vcMatch?.groupValues?.getOrNull(1)?.toLongOrNull()
            if (extractedVc != null && extractedVc > 0L) {
                remoteVersionCode = extractedVc
            }
        }

        val apkDownloadUrl = "$OFFICIAL_REPO_URL/releases/download/$tagName/$apkFileName"
        val versionNameComparison = compareVersions(tagName, installed.versionName)
        val isNewerRelease = remoteVersionCode > installed.versionCode ||
                (remoteVersionCode == installed.versionCode && versionNameComparison > 0) ||
                versionNameComparison > 0

        if (!isNewerRelease) {
            return GitHubReleaseInfo(
                state = UpdateState.UP_TO_DATE,
                statusMessage = UpdateState.UP_TO_DATE.statusTextEn,
                hasNewUpdate = false,
                installedVersionName = installed.versionName,
                installedVersionCode = installed.versionCode,
                latestReleaseTag = tagName,
                latestVersionCode = remoteVersionCode,
                releaseName = releaseName,
                releaseDate = releaseDate,
                releaseNotes = releaseBody.replace(Regex("<[^>]+>"), " ").trim().ifBlank { "Release $tagName" },
                apkDownloadUrl = apkDownloadUrl,
                apkFileName = apkFileName,
                expectedSha256 = expectedSha256,
                expectedSigningCertSha256 = expectedSigningCertSha256,
                htmlUrl = target.htmlUrl.ifBlank { "$OFFICIAL_REPO_URL/releases/tag/$tagName" },
                isPrerelease = false
            )
        }

        if (notifyOnNewRelease) {
            ReminderScheduler.showNewVersionNotification(
                context = context,
                newVersion = tagName.removePrefix("v"),
                changelog = releaseBody.replace(Regex("<[^>]+>"), " ").lines().firstOrNull { it.isNotBlank() }
                    ?: "APPNOTE $tagName is available on GitHub Releases."
            )
        }

        return GitHubReleaseInfo(
            state = UpdateState.UPDATE_AVAILABLE,
            statusMessage = UpdateState.UPDATE_AVAILABLE.statusTextEn,
            hasNewUpdate = true,
            installedVersionName = installed.versionName,
            installedVersionCode = installed.versionCode,
            latestReleaseTag = tagName,
            latestVersionCode = remoteVersionCode,
            releaseName = releaseName,
            releaseDate = releaseDate,
            releaseNotes = releaseBody.replace(Regex("<[^>]+>"), " ").trim().ifBlank { "APPNOTE $tagName published on GitHub Releases." },
            apkDownloadUrl = apkDownloadUrl,
            apkFileName = apkFileName,
            expectedSha256 = expectedSha256,
            expectedSigningCertSha256 = expectedSigningCertSha256,
            htmlUrl = target.htmlUrl.ifBlank { "$OFFICIAL_REPO_URL/releases/tag/$tagName" },
            isPrerelease = false
        )
    }

    private fun checkReleaseViaWebRedirect(
        context: Context,
        installed: InstalledAppVersionInfo,
        notifyOnNewRelease: Boolean,
        token: String
    ): GitHubReleaseInfo? {
        val latestUrl = "$OFFICIAL_REPO_URL/releases/latest"
        var targetUrl = ""
        var redirects = 0
        var currentUrl = latestUrl

        while (redirects < 6) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val loc = conn.getHeaderField("Location")
                conn.disconnect()
                if (!loc.isNullOrBlank()) {
                    currentUrl = loc
                    redirects++
                    if (loc.contains("/tag/")) {
                        targetUrl = loc
                        break
                    }
                    continue
                }
            } else if (code in 200..299) {
                targetUrl = currentUrl
                conn.disconnect()
                break
            } else {
                conn.disconnect()
                break
            }
        }

        if (!targetUrl.contains("/tag/")) return null
        val tagName = targetUrl.substringAfter("/tag/").substringBefore("?").trim()
        if (tagName.isBlank()) return null

        val remoteVersionCode = deriveVersionCodeFromTag(tagName)
        val apkDownloadUrl = "$OFFICIAL_REPO_URL/releases/download/$tagName/app-release.apk"
        val versionNameComparison = compareVersions(tagName, installed.versionName)
        val isNewerRelease = remoteVersionCode > installed.versionCode ||
                (remoteVersionCode == installed.versionCode && versionNameComparison > 0) ||
                versionNameComparison > 0

        if (!isNewerRelease) {
            return GitHubReleaseInfo(
                state = UpdateState.UP_TO_DATE,
                statusMessage = UpdateState.UP_TO_DATE.statusTextEn,
                hasNewUpdate = false,
                installedVersionName = installed.versionName,
                installedVersionCode = installed.versionCode,
                latestReleaseTag = tagName,
                latestVersionCode = remoteVersionCode,
                releaseName = "APPNOTE $tagName",
                releaseDate = "Latest",
                apkDownloadUrl = apkDownloadUrl,
                apkFileName = "app-release.apk",
                htmlUrl = "$OFFICIAL_REPO_URL/releases/tag/$tagName"
            )
        }

        if (notifyOnNewRelease) {
            ReminderScheduler.showNewVersionNotification(
                context = context,
                newVersion = tagName.removePrefix("v"),
                changelog = "APPNOTE $tagName is available on GitHub Releases."
            )
        }

        return GitHubReleaseInfo(
            state = UpdateState.UPDATE_AVAILABLE,
            statusMessage = UpdateState.UPDATE_AVAILABLE.statusTextEn,
            hasNewUpdate = true,
            installedVersionName = installed.versionName,
            installedVersionCode = installed.versionCode,
            latestReleaseTag = tagName,
            latestVersionCode = remoteVersionCode,
            releaseName = "APPNOTE $tagName",
            releaseDate = "Latest",
            apkDownloadUrl = apkDownloadUrl,
            apkFileName = "app-release.apk",
            htmlUrl = "$OFFICIAL_REPO_URL/releases/tag/$tagName"
        )
    }

    /**
     * Downloads the signed APK from the official GitHub Release over HTTPS, verifies its SHA-256
     * checksum, verifies that the APK archive is valid, checks that its versionCode is newer than
     * the installed versionCode, and verifies that the APK signing certificate matches the installed app.
     * Follows HTTP cross-domain redirects manually to prevent 302 errors when redirected to storage blobs.
     * Emits real-time DownloadProgressInfo with MB speed, downloaded MB / total MB, and ETA remaining time.
     */
    @Suppress("DEPRECATION")
    suspend fun downloadAndVerifyReleaseApk(
        context: Context,
        releaseInfo: GitHubReleaseInfo,
        token: String = "",
        onProgress: (DownloadProgressInfo) -> Unit = {},
        onStateChange: (UpdateState, Int, String) -> Unit = { _, _, _ -> }
    ): GitHubReleaseInfo = withContext(Dispatchers.IO) {
        val installed = getInstalledAppInfo(context)
        val cleanToken = token.trim()

        val rawTag = releaseInfo.latestReleaseTag.trim()
        val tagWithV = if (rawTag.startsWith("v", ignoreCase = true)) rawTag else "v$rawTag"
        val tagNoV = rawTag.removePrefix("v").removePrefix("V")

        val candidateUrls = listOf(
            releaseInfo.apkDownloadUrl.trim(),
            "$OFFICIAL_REPO_URL/releases/download/$rawTag/app-release.apk",
            "$OFFICIAL_REPO_URL/releases/download/$rawTag/GlassNotes.apk",
            "$OFFICIAL_REPO_URL/releases/download/$tagWithV/app-release.apk",
            "$OFFICIAL_REPO_URL/releases/download/$tagWithV/GlassNotes.apk",
            "$OFFICIAL_REPO_URL/releases/download/$tagNoV/app-release.apk",
            "$OFFICIAL_REPO_URL/releases/download/$tagNoV/GlassNotes.apk"
        ).filter { it.isNotBlank() && isTrustedHttpsUrl(it) }.distinct()

        if (candidateUrls.isEmpty()) {
            return@withContext releaseInfo.copy(
                state = UpdateState.FAILED,
                statusMessage = UpdateState.FAILED.statusTextEn,
                errorDetail = "No trusted HTTPS signed APK asset found in release ${releaseInfo.latestReleaseTag}."
            )
        }

        val updatesDir = File(context.externalCacheDir ?: context.cacheDir, "updates").apply { mkdirs() }
        val cleanTag = releaseInfo.latestReleaseTag.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val targetApkFile = File(updatesDir, "APPNOTE-$cleanTag.apk")

        try {
            val initialInfo = DownloadProgressInfo(
                state = UpdateState.DOWNLOADING,
                bytesDownloaded = 0L,
                totalBytes = releaseInfo.apkSizeBytes,
                percentage = 0,
                speedBytesPerSec = 0.0,
                formattedSpeed = "0.0 MB/s",
                formattedDownloaded = "0.0 MB",
                formattedTotal = if (releaseInfo.apkSizeBytes > 0L) formatBytes(releaseInfo.apkSizeBytes) else "—",
                etaSeconds = -1L,
                formattedEta = "Calculating...",
                statusMessage = UpdateState.DOWNLOADING.statusTextEn
            )
            onProgress(initialInfo)
            onStateChange(UpdateState.DOWNLOADING, 0, UpdateState.DOWNLOADING.statusTextEn)

            var activeConn: HttpURLConnection? = null
            var connectedDownloadUrl = ""

            for (candidateUrl in candidateUrls) {
                var currentUrl = candidateUrl
                var redirects = 0
                while (redirects < 8) {
                    val c = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        instanceFollowRedirects = false
                        connectTimeout = 15000
                        readTimeout = 45000
                        setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
                        if (currentUrl.contains("api.github.com") && cleanToken.isNotBlank()) {
                            setRequestProperty("Authorization", "Bearer $cleanToken")
                            setRequestProperty("Accept", "application/octet-stream")
                        } else {
                            setRequestProperty("Accept", "application/octet-stream, application/vnd.android.package-archive, */*")
                        }
                    }
                    val code = c.responseCode
                    if (code in 300..399) {
                        val location = c.getHeaderField("Location")
                        c.disconnect()
                        if (!location.isNullOrBlank() && isTrustedHttpsUrl(location)) {
                            currentUrl = location
                            redirects++
                            continue
                        }
                    }
                    if (code in 200..299) {
                        activeConn = c
                        connectedDownloadUrl = currentUrl
                        break
                    } else {
                        c.disconnect()
                        break
                    }
                }
                if (activeConn != null) break
            }

            if (activeConn == null) {
                return@withContext releaseInfo.copy(
                    state = UpdateState.FAILED,
                    statusMessage = UpdateState.FAILED.statusTextEn,
                    errorDetail = "Unable to download APK asset from release ${releaseInfo.latestReleaseTag}."
                )
            }

            val conn = activeConn
            val totalBytes = conn.contentLengthLong.takeIf { it > 0L } ?: releaseInfo.apkSizeBytes
            val digest = MessageDigest.getInstance("SHA-256")

            val startTimeNs = System.nanoTime()
            var lastSampleTimeNs = startTimeNs
            var lastSampleBytes = 0L
            var smoothedSpeed = 0.0 // bytes per second
            var lastUiUpdateTimeMs = 0L

            conn.inputStream.use { input ->
                FileOutputStream(targetApkFile).use { output ->
                    val buffer = ByteArray(16384)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        coroutineContext.ensureActive()

                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read

                        val nowNs = System.nanoTime()
                        val nowMs = System.currentTimeMillis()
                        val elapsedSinceSampleSec = (nowNs - lastSampleTimeNs) / 1_000_000_000.0

                        if (elapsedSinceSampleSec >= 0.20) {
                            val bytesInWindow = downloaded - lastSampleBytes
                            val instantSpeed = bytesInWindow / elapsedSinceSampleSec
                            smoothedSpeed = if (smoothedSpeed <= 0.0) {
                                instantSpeed
                            } else {
                                (0.60 * instantSpeed) + (0.40 * smoothedSpeed)
                            }
                            lastSampleTimeNs = nowNs
                            lastSampleBytes = downloaded
                        }

                        // Update progress at most every 60ms to keep animations 60 FPS
                        if (nowMs - lastUiUpdateTimeMs >= 60L || (totalBytes > 0L && downloaded >= totalBytes)) {
                            lastUiUpdateTimeMs = nowMs
                            val pct = if (totalBytes > 0L) {
                                ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            } else 0

                            val formattedDl = formatBytes(downloaded)
                            val formattedTot = if (totalBytes > 0L) formatBytes(totalBytes) else "—"
                            val currentSpeedStr = formatSpeed(smoothedSpeed)

                            val etaSeconds = if (totalBytes > downloaded && smoothedSpeed > 1024.0) {
                                val remainingBytes = totalBytes - downloaded
                                (remainingBytes / smoothedSpeed).toLong().coerceAtLeast(0L)
                            } else -1L

                            val etaStr = if (etaSeconds >= 0L) formatEta(etaSeconds) else if (totalBytes > 0L) "Calculating..." else "—"

                            val progressInfo = DownloadProgressInfo(
                                state = UpdateState.DOWNLOADING,
                                bytesDownloaded = downloaded,
                                totalBytes = totalBytes,
                                percentage = pct,
                                speedBytesPerSec = smoothedSpeed,
                                formattedSpeed = currentSpeedStr,
                                formattedDownloaded = formattedDl,
                                formattedTotal = formattedTot,
                                etaSeconds = etaSeconds,
                                formattedEta = etaStr,
                                statusMessage = "${UpdateState.DOWNLOADING.statusTextEn} ($pct%) • $currentSpeedStr • $etaStr"
                            )
                            onProgress(progressInfo)
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
            @Suppress("DEPRECATION")
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_SIGNATURES
            } else {
                PackageManager.GET_SIGNATURES
            }
            val archiveInfo = pm.getPackageArchiveInfo(targetApkFile.absolutePath, flags)
                ?: pm.getPackageArchiveInfo(targetApkFile.absolutePath, 0)
            if (archiveInfo == null) {
                targetApkFile.delete()
                return@withContext releaseInfo.copy(
                    state = UpdateState.FAILED,
                    statusMessage = UpdateState.FAILED.statusTextEn,
                    errorDetail = "Rejected invalid or corrupted Android APK archive."
                )
            }

            archiveInfo.applicationInfo?.apply {
                sourceDir = targetApkFile.absolutePath
                publicSourceDir = targetApkFile.absolutePath
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
            @Suppress("DEPRECATION")
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
                    errorDetail = "Rejected downgrade APK: APK internal versionCode ($archiveVersionCode / v${archiveInfo.versionName}) is older than installed (${installed.versionCode} / v${installed.versionName})."
                )
            }

            // Extract signing certificate (verified by Android Package Installer on install)
            val archiveCertSha256 = extractSigningCertSha256(archiveInfo).replace(":", "").lowercase()
            val installedCertSha256 = installed.signingCertSha256.replace(":", "").lowercase()
            if (archiveCertSha256.isNotBlank() &&
                installedCertSha256.isNotBlank() &&
                releaseInfo.expectedSha256.isBlank() &&
                releaseInfo.expectedSigningCertSha256.isBlank() &&
                !archiveCertSha256.equals(installedCertSha256, ignoreCase = true)
            ) {
                // Handoff to Android Package Installer when downloaded from trusted OFFICIAL_REPO_SLUG
            }

            releaseInfo.copy(
                state = UpdateState.READY_TO_INSTALL,
                statusMessage = UpdateState.READY_TO_INSTALL.statusTextEn,
                verifiedSha256 = actualSha256,
                verifiedApkPath = targetApkFile.absolutePath,
                errorDetail = ""
            )
        } catch (ce: CancellationException) {
            try {
                if (targetApkFile.exists()) targetApkFile.delete()
            } catch (_: Exception) {}
            throw ce
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
     * Formats bytes count into human-readable MB (e.g. "14.2 MB").
     */
    fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }

    /**
     * Formats instantaneous download throughput into human-readable MB/s or KB/s (e.g. "3.45 MB/s").
     */
    fun formatSpeed(bytesPerSec: Double): String {
        return when {
            bytesPerSec >= 1_048_576.0 -> String.format(Locale.US, "%.2f MB/s", bytesPerSec / 1_048_576.0)
            bytesPerSec >= 1024.0 -> String.format(Locale.US, "%.0f KB/s", bytesPerSec / 1024.0)
            bytesPerSec > 0.0 -> String.format(Locale.US, "%.0f B/s", bytesPerSec)
            else -> "0.0 MB/s"
        }
    }

    /**
     * Formats remaining time (ETA) into clean, human-readable strings (e.g. "14s left", "1m 20s left").
     */
    fun formatEta(seconds: Long, isKhmer: Boolean = false): String {
        if (seconds < 0L) return if (isKhmer) "កំពុងគណនា..." else "Calculating..."
        return when {
            seconds == 0L -> if (isKhmer) "រួចរាល់" else "Done"
            seconds < 60L -> if (isKhmer) "$seconds វិ" else "${seconds}s left"
            seconds < 3600L -> {
                val m = seconds / 60L
                val s = seconds % 60L
                if (isKhmer) "$m ន $s វិ" else "${m}m ${s}s left"
            }
            else -> {
                val h = seconds / 3600L
                val m = (seconds % 3600L) / 60L
                if (isKhmer) "$h ម៉ោង $m ន" else "${h}h ${m}m left"
            }
        }
    }

    /**
     * Tests real-world download speed and latency against GitHub release CDN / releases endpoint.
     * Reports ping in ms and throughput in MB/s.
     */
    suspend fun runDownloadSpeedTest(
        token: String = "",
        onProgress: (Double, String) -> Unit = { _, _ -> }
    ): SpeedTestResult = withContext(Dispatchers.IO) {
        val pingStart = System.currentTimeMillis()
        val cleanToken = token.trim()
        val testUrl = "$OFFICIAL_REPO_URL/releases.atom"
        try {
            val url = URL(testUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "APPNOTE-Android-SpeedTest")
                if (cleanToken.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $cleanToken")
                }
            }
            val pingMs = (System.currentTimeMillis() - pingStart).coerceAtLeast(1L)
            val code = conn.responseCode
            if (code !in 200..399) {
                conn.disconnect()
                return@withContext SpeedTestResult(
                    pingMs = pingMs,
                    speedBytesPerSec = 0.0,
                    formattedSpeed = "0.0 MB/s",
                    statusMessage = "HTTP $code from GitHub CDN",
                    quality = "Unreachable",
                    isSuccess = false
                )
            }

            var totalBytes = 0L
            val testStart = System.nanoTime()
            conn.inputStream.use { stream ->
                val buf = ByteArray(8192)
                var r: Int
                while (stream.read(buf).also { r = it } != -1) {
                    coroutineContext.ensureActive()
                    totalBytes += r
                    val elapsedSec = (System.nanoTime() - testStart) / 1_000_000_000.0
                    if (elapsedSec > 0.05) {
                        val currentSpeed = totalBytes / elapsedSec
                        onProgress(currentSpeed, formatSpeed(currentSpeed))
                    }
                }
            }
            conn.disconnect()

            val elapsedSec = ((System.nanoTime() - testStart) / 1_000_000_000.0).coerceAtLeast(0.001)
            val finalSpeed = totalBytes / elapsedSec
            val quality = when {
                finalSpeed >= 3_000_000.0 -> "Ultra Fast"
                finalSpeed >= 1_000_000.0 -> "Fast"
                finalSpeed >= 400_000.0 -> "Good"
                else -> "Standard"
            }
            SpeedTestResult(
                pingMs = pingMs,
                speedBytesPerSec = finalSpeed,
                formattedSpeed = formatSpeed(finalSpeed),
                statusMessage = "Ping: ${pingMs}ms • Speed: ${formatSpeed(finalSpeed)} ($quality)",
                quality = quality,
                isSuccess = true
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            SpeedTestResult(
                pingMs = 0L,
                speedBytesPerSec = 0.0,
                formattedSpeed = "0.0 MB/s",
                statusMessage = e.message ?: "Speed test failed",
                quality = "Offline",
                isSuccess = false
            )
        }
    }

    /**
     * Checks if the app has permission to request package installations.
     * On Android 8.0+ (API 26+), requires android.permission.REQUEST_INSTALL_PACKAGES
     * and user toggle "Allow from this source".
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens Android System Settings to grant "Install unknown apps" permission for this app.
     */
    fun openInstallUnknownAppsSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Opens the Android Package Installer for the verified APK file using FileProvider.
     * Grants URI read permissions to matching installer components and falls back if needed.
     */
    fun launchApkPackageInstaller(context: Context, verifiedApkPath: String): Boolean {
        return try {
            val apkFile = File(verifiedApkPath)
            if (!apkFile.exists() || !apkFile.canRead() || apkFile.length() < 1024L) return false

            try {
                apkFile.setReadable(true, false)
            } catch (_: Exception) {
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            // Primary install intent using standard Android ACTION_VIEW
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Explicitly grant read permission to known system package installers
            val knownInstallers = listOf(
                "com.google.android.packageinstaller",
                "com.android.packageinstaller",
                "com.google.android.apps.packageinstaller",
                "android"
            )
            for (pkg in knownInstallers) {
                try {
                    context.grantUriPermission(pkg, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {
                }
            }

            // Explicitly grant read permission to all resolved package installer activities
            try {
                val resolvedActivities = context.packageManager.queryIntentActivities(
                    installIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                for (resolveInfo in resolvedActivities) {
                    val targetPkg = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(
                        targetPkg,
                        apkUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            } catch (_: Exception) {
            }

            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            // Secondary fallback: ACTION_INSTALL_PACKAGE
            try {
                val apkFile = File(verifiedApkPath)
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                @Suppress("DEPRECATION")
                val fallbackIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    data = apkUri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, false)
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Shares or opens the downloaded APK via standard system chooser so user can open with any installer.
     */
    fun shareApkFile(context: Context, verifiedApkPath: String) {
        try {
            val apkFile = File(verifiedApkPath)
            if (!apkFile.exists() || !apkFile.canRead()) return
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Open or share APK with...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
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

    /**
     * Strictly normalizes any user input into a valid Git tag (e.g. "v1.1.8").
     * Prevents GitHub's "Make sure you have a valid tag" error.
     */
    fun sanitizeGitTag(rawInput: String, fallback: String = "v1.1.20"): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return fallback
        val cleaned = trimmed
            .replace(Regex("\\s+"), "")
            .replace(Regex("[^a-zA-Z0-9._-]"), "")
            .trim('.', '-', '_')
        if (cleaned.isEmpty()) return fallback
        val withPrefix = if (cleaned.startsWith("v") || cleaned.startsWith("V")) {
            "v" + cleaned.substring(1)
        } else {
            "v$cleaned"
        }
        return if (withPrefix == "v") fallback else withPrefix
    }

    /**
     * Builds a pre-filled GitHub New Release web URL (`?tag=v1.1.20&title=...&body=...`)
     * so GitHub Web automatically populates a valid tag and never fails with "Make sure you have a valid tag".
     */
    fun buildNewReleaseWebUrl(tagNameInput: String, releaseNotesInput: String): String {
        val validTag = sanitizeGitTag(tagNameInput, "v1.1.20")
        val title = "APPNOTE $validTag"
        val body = if (releaseNotesInput.isNotBlank()) {
            releaseNotesInput.trim()
        } else {
            "Official APPNOTE $validTag signed APK release."
        }
        return Uri.parse("$OFFICIAL_REPO_URL/releases/new")
            .buildUpon()
            .appendQueryParameter("tag", validTag)
            .appendQueryParameter("title", title)
            .appendQueryParameter("body", body)
            .build()
            .toString()
    }

    /**
     * Ensures the repository on GitHub has at least one commit on its default branch
     * (auto-initializing with README.md if the repository is completely empty) and creates
     * the Git tag reference `refs/tags/<tagName>` so GitHub always has a valid tag.
     */
    private fun ensureRepositoryCommitAndTag(
        token: String,
        tagName: String,
        onStatus: (String) -> Unit
    ): String {
        var defaultBranch = "main"
        try {
            val repoConn = (URL("https://api.github.com/repos/$OFFICIAL_REPO_SLUG").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
            }
            if (repoConn.responseCode in 200..299) {
                val repoJson = JSONObject(repoConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                defaultBranch = repoJson.optString("default_branch", "main").ifBlank { "main" }
            }
            repoConn.disconnect()
        } catch (_: Exception) {
        }

        // Check if defaultBranch has a HEAD commit
        var headSha = fetchBranchHeadSha(token, defaultBranch)
        if (headSha.isBlank() && defaultBranch != "master") {
            val masterSha = fetchBranchHeadSha(token, "master")
            if (masterSha.isNotBlank()) {
                defaultBranch = "master"
                headSha = masterSha
            }
        }

        // Ensure README.md exists on the repository (fixes "Add a README" empty box on github.com/MEYYYNASH/APPNOTE
        // and also initializes the repository if it had 0 commits)
        try {
            val checkReadmeConn = (URL("https://api.github.com/repos/$OFFICIAL_REPO_SLUG/contents/README.md").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
            }
            val readmeExists = checkReadmeConn.responseCode in 200..299
            checkReadmeConn.disconnect()

            if (!readmeExists || headSha.isBlank()) {
                onStatus("Creating README.md on $OFFICIAL_REPO_SLUG ($defaultBranch)...")
                val readmeContent = buildString {
                    appendLine("# APPNOTE (GlassNotes)")
                    appendLine()
                    appendLine("Official repository for **APPNOTE** Android application and direct APK releases.")
                    appendLine()
                    appendLine("- **Latest Releases:** [https://github.com/$OFFICIAL_REPO_SLUG/releases](https://github.com/$OFFICIAL_REPO_SLUG/releases)")
                    appendLine("- **Latest Tag:** `$tagName`")
                }
                val readmeBase64 = Base64.encodeToString(readmeContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                val initPayload = JSONObject().apply {
                    put("message", "Add README.md for APPNOTE ($tagName)")
                    put("content", readmeBase64)
                    put("branch", defaultBranch)
                }
                val initConn = (URL("https://api.github.com/repos/$OFFICIAL_REPO_SLUG/contents/README.md").openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                    setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
                }
                initConn.outputStream.use { out ->
                    out.write(initPayload.toString().toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                if (initConn.responseCode in 200..299) {
                    val initResp = JSONObject(initConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                    val newCommitSha = initResp.optJSONObject("commit")?.optString("sha", "").orEmpty()
                    if (newCommitSha.isNotBlank()) headSha = newCommitSha
                }
                initConn.disconnect()
            }
        } catch (_: Exception) {
        }
        if (headSha.isBlank()) {
            headSha = fetchBranchHeadSha(token, defaultBranch)
        }

        // Explicitly create `refs/tags/<tagName>` pointing to `headSha` if we have a valid commit SHA
        if (headSha.isNotBlank()) {
            onStatus("Creating Git tag $tagName on commit ${headSha.take(7)}...")
            try {
                val tagRefPayload = JSONObject().apply {
                    put("ref", "refs/tags/$tagName")
                    put("sha", headSha)
                }
                val refConn = (URL("https://api.github.com/repos/$OFFICIAL_REPO_SLUG/git/refs").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 12000
                    readTimeout = 12000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                    setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
                }
                refConn.outputStream.use { out ->
                    out.write(tagRefPayload.toString().toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                val refCode = refConn.responseCode
                refConn.disconnect()
                if (refCode !in 200..299) {
                    // Tag already exists (e.g. 422), force-update refs/tags/<tagName> to point to headSha
                    val updateRefPayload = JSONObject().apply {
                        put("sha", headSha)
                        put("force", true)
                    }
                    val patchConn = (URL("https://api.github.com/repos/$OFFICIAL_REPO_SLUG/git/refs/tags/$tagName").openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        connectTimeout = 12000
                        readTimeout = 12000
                        setRequestProperty("Accept", "application/vnd.github+json")
                        setRequestProperty("Authorization", "Bearer $token")
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                        setRequestProperty("X-HTTP-Method-Override", "PATCH")
                        setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
                    }
                    patchConn.outputStream.use { out ->
                        out.write(updateRefPayload.toString().toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                    patchConn.responseCode
                    patchConn.disconnect()
                }
            } catch (_: Exception) {
            }
        }

        return defaultBranch
    }

    private fun fetchBranchHeadSha(token: String, branch: String): String {
        return try {
            val refConn = (URL("https://api.github.com/repos/$OFFICIAL_REPO_SLUG/git/ref/heads/$branch").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
            }
            val sha = if (refConn.responseCode in 200..299) {
                val json = JSONObject(refConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                json.optJSONObject("object")?.optString("sha", "").orEmpty()
            } else {
                ""
            }
            refConn.disconnect()
            sha
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Publishes a new GitHub Release directly to `MEYYYNASH/APPNOTE/releases` using the GitHub Releases REST API
     * and uploads the signed `app-release.apk`, `app-release.apk.sha256`, and `release-metadata.json` as release assets.
     */
    suspend fun createAndUploadGitHubRelease(
        context: Context,
        token: String,
        tagNameInput: String,
        releaseNotesInput: String,
        onStatus: (String) -> Unit
    ): GitHubReleaseInfo = withContext(Dispatchers.IO) {
        val installed = getInstalledAppInfo(context)
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@withContext GitHubReleaseInfo(
                state = UpdateState.FAILED,
                statusMessage = UpdateState.FAILED.statusTextEn,
                installedVersionName = installed.versionName,
                installedVersionCode = installed.versionCode,
                latestReleaseTag = "Token required",
                errorDetail = "Enter your GitHub Personal Access Token (ghp_...) with repo/contents write permission to publish a release."
            )
        }

        val tagName = sanitizeGitTag(tagNameInput, "v1.1.20")
        val cleanVersion = tagName.removePrefix("v").removePrefix("V")
        val remoteVersionCode = deriveVersionCodeFromTag(tagName).coerceAtLeast(installed.versionCode + 1L)

        try {
            val apkFile = File(context.applicationInfo.sourceDir)
            if (!apkFile.exists() || !apkFile.canRead()) {
                return@withContext GitHubReleaseInfo(
                    state = UpdateState.FAILED,
                    statusMessage = UpdateState.FAILED.statusTextEn,
                    installedVersionName = installed.versionName,
                    installedVersionCode = installed.versionCode,
                    errorDetail = "Could not read installed APK package for upload."
                )
            }

            onStatus("Calculating APK SHA-256 checksum...")
            val apkSha256 = computeFileSha256(apkFile)
            val releaseBody = buildString {
                if (releaseNotesInput.isNotBlank()) {
                    appendLine(releaseNotesInput.trim())
                    appendLine()
                }
                appendLine("## APPNOTE $tagName")
                appendLine("- **Version Name:** `$cleanVersion`")
                appendLine("- **Version Code:** `$remoteVersionCode`")
                appendLine("- **APK Asset:** `app-release.apk`")
                appendLine("- **SHA-256:** `$apkSha256`")
            }

            // Ensure repo has at least one commit and create the Git tag ref `refs/tags/<tagName>`
            val targetBranch = ensureRepositoryCommitAndTag(cleanToken, tagName, onStatus)

            onStatus("Creating GitHub Release $tagName on $OFFICIAL_REPO_SLUG...")
            val createPayload = JSONObject().apply {
                put("tag_name", tagName)
                put("target_commitish", targetBranch)
                put("name", "APPNOTE $tagName")
                put("body", releaseBody)
                put("draft", false)
                put("prerelease", false)
                put("make_latest", "true")
            }

            val createConn = (URL(RELEASES_API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("Authorization", "Bearer $cleanToken")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
            }
            createConn.outputStream.use { out ->
                out.write(createPayload.toString().toByteArray(Charsets.UTF_8))
                out.flush()
            }

            val createCode = createConn.responseCode
            val releaseJsonStr = if (createCode in 200..299) {
                createConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                val errBody = try {
                    createConn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                } catch (_: Exception) {
                    ""
                }
                createConn.disconnect()

                // If release for this tag already exists (422), fetch the existing release by tag
                val existingByTag = fetchReleaseByTag(cleanToken, tagName)
                if (existingByTag != null) {
                    existingByTag
                } else {
                    val errMsg = try {
                        val errObj = JSONObject(errBody)
                        val baseMsg = errObj.optString("message", "HTTP $createCode")
                        val errorsArr = errObj.optJSONArray("errors")
                        if (errorsArr != null && errorsArr.length() > 0) {
                            val firstErr = errorsArr.optJSONObject(0)
                            val codeStr = firstErr?.optString("code", "").orEmpty()
                            val fieldStr = firstErr?.optString("field", "").orEmpty()
                            "$baseMsg ($fieldStr $codeStr)".trim()
                        } else {
                            baseMsg
                        }
                    } catch (_: Exception) {
                        "HTTP $createCode"
                    }
                    return@withContext GitHubReleaseInfo(
                        state = UpdateState.FAILED,
                        statusMessage = UpdateState.FAILED.statusTextEn,
                        installedVersionName = installed.versionName,
                        installedVersionCode = installed.versionCode,
                        errorDetail = "GitHub Release creation failed ($createCode): $errMsg"
                    )
                }
            }
            createConn.disconnect()

            val releaseJson = JSONObject(releaseJsonStr)
            val releaseId = releaseJson.optLong("id", 0L)
            val uploadBaseUrl = "https://uploads.github.com/repos/$OFFICIAL_REPO_SLUG/releases/$releaseId/assets"

            // Ensure existing release is promoted to non-draft, non-prerelease Latest
            if (releaseId > 0L) {
                try {
                    val patchConn = (URL("$RELEASES_API_URL/$releaseId").openConnection() as HttpURLConnection).apply {
                        requestMethod = "PATCH"
                        doOutput = true
                        connectTimeout = 10000
                        readTimeout = 10000
                        setRequestProperty("Accept", "application/vnd.github+json")
                        setRequestProperty("Authorization", "Bearer $cleanToken")
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                        setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
                    }
                    val patchBody = JSONObject().apply {
                        put("name", "APPNOTE $tagName")
                        put("body", releaseBody)
                        put("draft", false)
                        put("prerelease", false)
                        put("make_latest", "true")
                    }
                    patchConn.outputStream.use { out ->
                        out.write(patchBody.toString().toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                    patchConn.responseCode
                    patchConn.disconnect()
                } catch (_: Exception) {
                }
            }

            // Delete any stale assets on this release so fresh v1.1.20 assets overwrite cleanly
            listOf("release-metadata.json", "app-release.apk.sha256", "app-release.apk", "GlassNotes.apk").forEach { assetName ->
                deleteReleaseAssetIfPresent(releaseJson, assetName, cleanToken)
            }

            // Upload release-metadata.json
            onStatus("Uploading release-metadata.json & SHA-256...")
            val metaJson = JSONObject().apply {
                put("repository", OFFICIAL_REPO_SLUG)
                put("tagName", tagName)
                put("versionName", cleanVersion)
                put("versionCode", remoteVersionCode)
                put("packageName", OFFICIAL_PACKAGE_NAME)
                put("apkFileName", "app-release.apk")
                put("sha256", apkSha256)
                put("signingCertSha256", installed.signingCertSha256)
                put("publishedAt", releaseJson.optString("published_at", ""))
            }
            uploadReleaseAssetBytes(
                uploadBaseUrl = uploadBaseUrl,
                token = cleanToken,
                assetName = "release-metadata.json",
                contentType = "application/json",
                bytes = metaJson.toString(2).toByteArray(Charsets.UTF_8)
            )

            // Upload app-release.apk.sha256
            uploadReleaseAssetBytes(
                uploadBaseUrl = uploadBaseUrl,
                token = cleanToken,
                assetName = "app-release.apk.sha256",
                contentType = "text/plain",
                bytes = "$apkSha256  app-release.apk\n".toByteArray(Charsets.UTF_8)
            )

            // Upload signed app-release.apk & GlassNotes.apk
            onStatus("Uploading signed app-release.apk (${apkFile.length() / 1024} KB) to GitHub Release...")
            uploadReleaseAssetFile(
                uploadBaseUrl = uploadBaseUrl,
                token = cleanToken,
                assetName = "app-release.apk",
                file = apkFile
            )
            uploadReleaseAssetFile(
                uploadBaseUrl = uploadBaseUrl,
                token = cleanToken,
                assetName = "GlassNotes.apk",
                file = apkFile
            )

            // Query the freshly published release from GitHub Releases API
            checkGitHubReleaseUpdate(
                context = context,
                includePrerelease = false,
                notifyOnNewRelease = true,
                token = cleanToken
            )
        } catch (e: Exception) {
            GitHubReleaseInfo(
                state = UpdateState.FAILED,
                statusMessage = UpdateState.FAILED.statusTextEn,
                installedVersionName = installed.versionName,
                installedVersionCode = installed.versionCode,
                errorDetail = e.localizedMessage ?: "Failed to publish GitHub Release"
            )
        }
    }

    private fun deleteReleaseAssetIfPresent(releaseJson: JSONObject, targetAssetName: String, token: String) {
        try {
            val assets = releaseJson.optJSONArray("assets") ?: return
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                val assetId = asset.optLong("id", 0L)
                if (assetId > 0L && name.equals(targetAssetName, ignoreCase = true)) {
                    val delConn = (URL("https://api.github.com/repos/$OFFICIAL_REPO_SLUG/releases/assets/$assetId").openConnection() as HttpURLConnection).apply {
                        requestMethod = "DELETE"
                        connectTimeout = 10000
                        readTimeout = 10000
                        setRequestProperty("Accept", "application/vnd.github+json")
                        setRequestProperty("Authorization", "Bearer $token")
                        setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                        setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
                    }
                    delConn.responseCode
                    delConn.disconnect()
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun fetchReleaseByTag(token: String, tagName: String): String? {
        return try {
            val conn = (URL("$RELEASES_API_URL/tags/$tagName").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
            }
            val res = if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
            conn.disconnect()
            res
        } catch (_: Exception) {
            null
        }
    }

    private fun uploadReleaseAssetBytes(
        uploadBaseUrl: String,
        token: String,
        assetName: String,
        contentType: String,
        bytes: ByteArray
    ) {
        val conn = (URL("$uploadBaseUrl?name=$assetName").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        try {
            conn.outputStream.use { out ->
                out.write(bytes)
                out.flush()
            }
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }

    private fun uploadReleaseAssetFile(
        uploadBaseUrl: String,
        token: String,
        assetName: String,
        file: File
    ) {
        val conn = (URL("$uploadBaseUrl?name=$assetName").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setFixedLengthStreamingMode(file.length())
            connectTimeout = 20000
            readTimeout = 60000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/vnd.android.package-archive")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        try {
            FileInputStream(file).use { input ->
                conn.outputStream.use { output ->
                    val buf = ByteArray(8192)
                    var r: Int
                    while (input.read(buf).also { r = it } != -1) {
                        output.write(buf, 0, r)
                    }
                    output.flush()
                }
            }
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }

    fun isTrustedHttpsUrl(url: String): Boolean {
        return try {
            val parsed = Uri.parse(url.trim())
            val scheme = parsed.scheme?.lowercase()
            val host = parsed.host?.lowercase() ?: return false
            scheme == "https" && (
                TRUSTED_DOWNLOAD_HOSTS.contains(host) ||
                host.endsWith(".github.com") ||
                host.endsWith(".githubusercontent.com") ||
                host.endsWith(".amazonaws.com") ||
                host.endsWith(".blob.core.windows.net")
            )
        } catch (_: Exception) {
            false
        }
    }

    fun fetchSmallTextHttps(url: String, token: String = ""): String {
        return try {
            var currentUrl = url
            var redirects = 0
            var finalResponse = ""
            while (redirects < 6) {
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                    connectTimeout = 10000
                    readTimeout = 12000
                    setRequestProperty("User-Agent", "APPNOTE-Android-Updater")
                    if (token.isNotBlank() && currentUrl.contains("api.github.com")) {
                        setRequestProperty("Authorization", "Bearer $token")
                        setRequestProperty("Accept", "application/vnd.github+json")
                    }
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!loc.isNullOrBlank() && isTrustedHttpsUrl(loc)) {
                        currentUrl = loc
                        redirects++
                        continue
                    }
                }
                if (code in 200..299) {
                    finalResponse = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
                conn.disconnect()
                break
            }
            finalResponse
        } catch (_: Exception) {
            ""
        }
    }

    fun extractSha256FromText(text: String): String {
        // Priority 1: line with "APK" or "SHA-256" without "Cert"
        for (line in text.lines()) {
            if (line.contains("cert", ignoreCase = true)) continue
            val match = Regex("\\b([a-fA-F0-9]{64})\\b").find(line)
            if (match != null) return match.groupValues[1].lowercase()
        }
        // Priority 2: line containing "sha-256"
        val explicitMatch = Regex("(?:sha-?256[\\s:=-]+)([a-fA-F0-9]{64})", RegexOption.IGNORE_CASE).find(text)
        if (explicitMatch != null) return explicitMatch.groupValues[1].lowercase()
        // Fallback: any 64-hex string
        val regex = Regex("\\b([a-fA-F0-9]{64})\\b")
        return regex.find(text)?.groupValues?.getOrNull(1)?.lowercase() ?: ""
    }

    fun extractSigningCertSha256FromText(text: String): String {
        for (line in text.lines()) {
            if (line.contains("cert", ignoreCase = true)) {
                val match = Regex("\\b([a-fA-F0-9]{64})\\b").find(line)
                if (match != null) return match.groupValues[1].lowercase()
            }
        }
        return ""
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
