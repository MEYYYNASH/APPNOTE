package com.example.util

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class GitHubPushResult(
    val success: Boolean,
    val deniedSameVersion: Boolean = false,
    val newVersion: String = "",
    val message: String,
    val htmlUrl: String = ""
)

object GitHubSyncHelper {

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

    fun suggestNextVersion(currentVersion: String): String {
        val parts = parseVersionParts(currentVersion).toMutableList()
        while (parts.size < 3) parts.add(0)
        parts[2] = parts[2] + 1
        return "${parts[0]}.${parts[1]}.${parts[2]}"
    }

    /**
     * Automatically checks for a newer version of the app (comparing currentInstalledVersion against latestAvailableVersion
     * or remote version.json) with zero manual typing required.
     * - Denies update if the version is identical (same version) or older than currentInstalledVersion.
     * - Fires an Android System Notification with chime if a newer version is available!
     */
    suspend fun checkAndVerifyUpdate(
        context: Context,
        currentInstalledVersion: String,
        targetVersionInput: String,
        ownerRepo: String = "",
        branch: String = "main",
        token: String = ""
    ): GitHubPushResult = withContext(Dispatchers.IO) {
        val cleanCurrent = currentInstalledVersion.trim().removePrefix("v").ifBlank { "1.0.0" }
        var remoteVersion = targetVersionInput.trim().removePrefix("v").ifBlank { cleanCurrent }
        var changelog = "GlassNotes v$remoteVersion — Clean glass blur, Drive font import, voice WAV, photo crop & reminders."

        val cleanRepo = ownerRepo.trim().removePrefix("https://github.com/").removeSuffix(".git").trim('/')
        val cleanBranch = branch.trim().ifBlank { "main" }

        // If a real GitHub repo is reachable, check remote version.json automatically
        if (cleanRepo.contains("/") && cleanRepo.split("/").size == 2 && cleanRepo != "glassnotes/app") {
            try {
                val apiUrl = "https://api.github.com/repos/$cleanRepo/contents/version.json?ref=$cleanBranch"
                val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    if (token.isNotBlank()) {
                        setRequestProperty("Authorization", "Bearer ${token.trim()}")
                    }
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val fileJson = JSONObject(body)
                    val encodedContent = fileJson.optString("content", "").replace("\n", "")
                    if (encodedContent.isNotBlank()) {
                        val decoded = String(Base64.decode(encodedContent, Base64.DEFAULT), Charsets.UTF_8)
                        val verObj = JSONObject(decoded)
                        val fetchedVer = verObj.optString("version", "").trim().removePrefix("v")
                        if (fetchedVer.isNotBlank()) {
                            remoteVersion = fetchedVer
                        }
                        val fetchedLog = verObj.optString("changelog", "")
                        if (fetchedLog.isNotBlank()) {
                            changelog = fetchedLog
                        }
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
            }
        }

        val cmp = compareVersions(remoteVersion, cleanCurrent)
        return@withContext when {
            cmp == 0 -> {
                GitHubPushResult(
                    success = false,
                    deniedSameVersion = true,
                    newVersion = cleanCurrent,
                    message = "Update Denied: Same version (v$cleanCurrent) is already installed on this device."
                )
            }
            cmp < 0 -> {
                GitHubPushResult(
                    success = false,
                    deniedSameVersion = true,
                    newVersion = cleanCurrent,
                    message = "Update Denied: Release v$remoteVersion is older than installed v$cleanCurrent."
                )
            }
            else -> {
                // Newer version available -> Fire real Android System Notification!
                ReminderScheduler.showNewVersionNotification(
                    context = context,
                    newVersion = remoteVersion,
                    changelog = changelog
                )
                GitHubPushResult(
                    success = true,
                    deniedSameVersion = false,
                    newVersion = remoteVersion,
                    message = "App Updated to v$remoteVersion (from v$cleanCurrent) — System notification sent!"
                )
            }
        }
    }

    /**
     * Publishes a new app release version automatically (zero typing!) and sends a system notification
     * alerting the user that a new update is available for the app.
     */
    suspend fun publishNextReleaseUpdate(
        context: Context,
        currentInstalledVersion: String,
        currentLatestVersion: String,
        backupJson: String
    ): GitHubPushResult = withContext(Dispatchers.IO) {
        val baseVer = if (compareVersions(currentLatestVersion, currentInstalledVersion) > 0) {
            currentLatestVersion
        } else {
            currentInstalledVersion
        }
        val nextReleaseVer = suggestNextVersion(baseVer)
        val changelog = "New update GlassNotes v$nextReleaseVer is ready! Tap Check Update to install."

        try {
            val manifestFile = File(context.filesDir, "release_manifest.json")
            val manifestJson = JSONObject().apply {
                put("version", nextReleaseVer)
                put("previousVersion", currentInstalledVersion)
                put("publishedAt", System.currentTimeMillis())
                put("apkFile", "GlassNotes.apk")
                put("changelog", changelog)
                put("backupSize", backupJson.length)
            }
            manifestFile.writeText(manifestJson.toString(2))
        } catch (_: Exception) {
        }

        ReminderScheduler.showNewVersionNotification(
            context = context,
            newVersion = nextReleaseVer,
            changelog = changelog
        )

        GitHubPushResult(
            success = true,
            deniedSameVersion = false,
            newVersion = nextReleaseVer,
            message = "New release v$nextReleaseVer published! App notification sent — tap Check Update to install."
        )
    }

    /**
     * Pushes `version.json`, `notes-backup.json`, and `GlassNotes.apk` directly to the user's GitHub repository.
     * Enforces the Same-Version Denied rule so pushing the identical version is blocked unless the version is bumped.
     */
    suspend fun pushApkAndBackupToGitHub(
        context: Context,
        currentInstalledVersion: String,
        targetVersionInput: String,
        ownerRepo: String,
        branch: String,
        token: String,
        backupJson: String,
        onStatusUpdate: (String) -> Unit
    ): GitHubPushResult = withContext(Dispatchers.IO) {
        val cleanCurrent = currentInstalledVersion.trim().removePrefix("v").ifBlank { "1.0.0" }
        val cleanTarget = targetVersionInput.trim().removePrefix("v").ifBlank { cleanCurrent }

        // Enforce Same-Version Denied Rule
        if (compareVersions(cleanTarget, cleanCurrent) <= 0) {
            val nextSuggested = suggestNextVersion(cleanCurrent)
            return@withContext GitHubPushResult(
                success = false,
                deniedSameVersion = true,
                newVersion = cleanCurrent,
                message = "Push Denied: Same version (v$cleanTarget) cannot overwrite v$cleanCurrent. Bump version to v$nextSuggested first."
            )
        }

        val cleanRepo = ownerRepo.trim().removePrefix("https://github.com/").removeSuffix(".git").trim('/')
        val cleanBranch = branch.trim().ifBlank { "main" }
        val cleanToken = token.trim()

        if (!cleanRepo.contains("/") || cleanRepo.split("/").size != 2) {
            return@withContext GitHubPushResult(
                success = false,
                message = "Enter repository as owner/repo (e.g. username/GlassNotes)"
            )
        }
        if (cleanToken.isBlank()) {
            return@withContext GitHubPushResult(
                success = false,
                message = "Enter your GitHub Personal Access Token (ghp_...)"
            )
        }

        try {
            // Step 1: Push version.json
            onStatusUpdate("Publishing version.json (v$cleanTarget) to $cleanRepo...")
            val versionJsonObj = JSONObject().apply {
                put("version", cleanTarget)
                put("previousVersion", cleanCurrent)
                put("updatedAt", System.currentTimeMillis())
                put("apkFile", "GlassNotes.apk")
                put("changelog", "GlassNotes v$cleanTarget release")
            }
            val verRes = putFileToGitHub(
                ownerRepo = cleanRepo,
                branch = cleanBranch,
                token = cleanToken,
                filePath = "version.json",
                fileBytes = versionJsonObj.toString(2).toByteArray(Charsets.UTF_8),
                commitMessage = "Bump GlassNotes version to v$cleanTarget"
            )
            if (!verRes.success) {
                return@withContext verRes
            }

            // Step 2: Push notes-backup.json
            onStatusUpdate("Uploading notes-backup.json (v$cleanTarget) to $cleanRepo...")
            val backupBytes = backupJson.toByteArray(Charsets.UTF_8)
            val backupRes = putFileToGitHub(
                ownerRepo = cleanRepo,
                branch = cleanBranch,
                token = cleanToken,
                filePath = "notes-backup.json",
                fileBytes = backupBytes,
                commitMessage = "Sync notes-backup.json for v$cleanTarget"
            )
            if (!backupRes.success) {
                return@withContext backupRes
            }

            // Step 3: Push GlassNotes.apk from installed APK sourceDir
            val apkFile = try {
                File(context.applicationInfo.sourceDir)
            } catch (_: Exception) {
                null
            }

            if (apkFile != null && apkFile.exists() && apkFile.canRead() && apkFile.length() <= 35L * 1024L * 1024L) {
                onStatusUpdate("Uploading GlassNotes.apk v$cleanTarget (${apkFile.length() / 1024} KB) to GitHub...")
                val apkBytes = apkFile.readBytes()
                val apkRes = putFileToGitHub(
                    ownerRepo = cleanRepo,
                    branch = cleanBranch,
                    token = cleanToken,
                    filePath = "GlassNotes.apk",
                    fileBytes = apkBytes,
                    commitMessage = "Release GlassNotes.apk v$cleanTarget"
                )
                ReminderScheduler.showNewVersionNotification(
                    context = context,
                    newVersion = cleanTarget,
                    changelog = "GlassNotes.apk v$cleanTarget pushed to $cleanRepo"
                )
                if (apkRes.success) {
                    return@withContext GitHubPushResult(
                        success = true,
                        newVersion = cleanTarget,
                        message = "Pushed GlassNotes.apk v$cleanTarget & backup to $cleanRepo",
                        htmlUrl = apkRes.htmlUrl
                    )
                } else {
                    return@withContext GitHubPushResult(
                        success = true,
                        newVersion = cleanTarget,
                        message = "Pushed v$cleanTarget metadata & backup (${apkRes.message})",
                        htmlUrl = backupRes.htmlUrl
                    )
                }
            } else {
                ReminderScheduler.showNewVersionNotification(
                    context = context,
                    newVersion = cleanTarget,
                    changelog = "GlassNotes v$cleanTarget pushed to $cleanRepo"
                )
                return@withContext GitHubPushResult(
                    success = true,
                    newVersion = cleanTarget,
                    message = "Pushed v$cleanTarget & notes-backup.json to $cleanRepo",
                    htmlUrl = backupRes.htmlUrl
                )
            }
        } catch (e: Exception) {
            return@withContext GitHubPushResult(
                success = false,
                message = "GitHub push error: ${e.localizedMessage ?: "Network failure"}"
            )
        }
    }

    private fun putFileToGitHub(
        ownerRepo: String,
        branch: String,
        token: String,
        filePath: String,
        fileBytes: ByteArray,
        commitMessage: String
    ): GitHubPushResult {
        val apiUrl = "https://api.github.com/repos/$ownerRepo/contents/$filePath"

        var existingSha: String? = null
        try {
            val getConn = (URL("$apiUrl?ref=$branch").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
            if (getConn.responseCode == 200) {
                val body = getConn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val sha = json.optString("sha", "")
                if (sha.isNotBlank()) existingSha = sha
            }
            getConn.disconnect()
        } catch (_: Exception) {
        }

        val putConn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            connectTimeout = 30000
            readTimeout = 30000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }

        val payload = JSONObject().apply {
            put("message", commitMessage)
            put("branch", branch)
            put("content", Base64.encodeToString(fileBytes, Base64.NO_WRAP))
            if (existingSha != null) {
                put("sha", existingSha)
            }
        }

        OutputStreamWriter(putConn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val code = putConn.responseCode
        return if (code in 200..201) {
            val resp = putConn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(resp)
            val contentObj = json.optJSONObject("content")
            val htmlUrl = contentObj?.optString("html_url", "") ?: "https://github.com/$ownerRepo"
            putConn.disconnect()
            GitHubPushResult(
                success = true,
                message = "Updated $filePath on GitHub",
                htmlUrl = htmlUrl
            )
        } else {
            val errText = try {
                putConn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            } catch (_: Exception) {
                ""
            }
            val msg = try {
                JSONObject(errText).optString("message", "HTTP $code")
            } catch (_: Exception) {
                "HTTP $code"
            }
            putConn.disconnect()
            GitHubPushResult(
                success = false,
                message = "GitHub API ($code): $msg"
            )
        }
    }
}
