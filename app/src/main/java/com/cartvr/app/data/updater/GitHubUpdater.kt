package com.cartvr.app.data.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import com.cartvr.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GitHubAsset> = emptyList()
)

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long
)

class GitHubUpdater(private val context: Context) {

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val noRedirectClient = HttpClient(OkHttp) {
        followRedirects = false
    }

    suspend fun checkForUpdate(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val owner = BuildConfig.GITHUB_OWNER
            val repo = BuildConfig.GITHUB_REPO

            // 1. Primary rate-limit-free check via GitHub web /releases/latest redirect
            val resolvedTag = runCatching {
                val resp = noRedirectClient.get("https://github.com/$owner/$repo/releases/latest") {
                    header("User-Agent", "CartVR/${BuildConfig.VERSION_NAME}")
                }
                val location = resp.headers["Location"] ?: return@runCatching null
                val marker = "/releases/tag/"
                val idx = location.indexOf(marker)
                if (idx >= 0) location.substring(idx + marker.length).substringBefore("?").trim() else null
            }.getOrNull()

            if (!resolvedTag.isNullOrBlank()) {
                val remoteVersion = resolvedTag.removePrefix("v")
                if (!isNewerVersion(remoteVersion, BuildConfig.VERSION_NAME)) {
                    return@runCatching null
                }

                // Best-effort metadata from GitHub API (falls back gracefully if 403 rate limited)
                val optionalRelease = runCatching {
                    httpClient.get("https://api.github.com/repos/$owner/$repo/releases/tags/$resolvedTag") {
                        header("Accept", "application/vnd.github+json")
                        header("User-Agent", "CartVR/${BuildConfig.VERSION_NAME}")
                    }.body<GitHubRelease>()
                }.getOrNull()

                val apkAsset = optionalRelease?.assets?.firstOrNull {
                    it.name.equals("CartVR.apk", ignoreCase = true)
                } ?: optionalRelease?.assets?.firstOrNull {
                    it.name.endsWith(".apk", ignoreCase = true)
                }

                return@runCatching UpdateInfo(
                    versionName = remoteVersion,
                    releaseNotes = optionalRelease?.body?.ifBlank { "Bug fixes and improvements." }
                        ?: "Version $remoteVersion is ready to install.",
                    apkDownloadUrl = apkAsset?.browserDownloadUrl
                        ?: "https://github.com/$owner/$repo/releases/download/$resolvedTag/CartVR.apk",
                    apkSizeBytes = apkAsset?.size ?: 0L
                )
            }

            // 2. Fallback to GitHub API if redirect header wasn't available
            val release: GitHubRelease = httpClient.get("https://api.github.com/repos/$owner/$repo/releases/latest") {
                header("Accept", "application/vnd.github+json")
                header("User-Agent", "CartVR/${BuildConfig.VERSION_NAME}")
            }.body()

            val remoteVersion = release.tagName.trim().removePrefix("v")
            if (!isNewerVersion(remoteVersion, BuildConfig.VERSION_NAME)) {
                return@runCatching null
            }

            val apkAsset = release.assets.firstOrNull {
                it.name.equals("CartVR.apk", ignoreCase = true)
            } ?: release.assets.firstOrNull {
                it.name.endsWith(".apk", ignoreCase = true)
            } ?: return@runCatching null

            UpdateInfo(
                versionName = remoteVersion,
                releaseNotes = release.body?.ifBlank { "Bug fixes and improvements." }
                    ?: "Bug fixes and improvements.",
                apkDownloadUrl = apkAsset.browserDownloadUrl,
                apkSizeBytes = apkAsset.size
            )
        }
    }

    fun canInstallUnknownApps(): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    fun openUnknownAppSourcesSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun downloadAndInstall(
        updateInfo: UpdateInfo,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val apkFile = File(context.cacheDir, "CartVR-update.apk")
            if (apkFile.exists()) apkFile.delete()

            val response = httpClient.get(updateInfo.apkDownloadUrl) {
                header("User-Agent", "CartVR/${BuildConfig.VERSION_NAME}")
            }
            val totalBytes = response.contentLength() ?: updateInfo.apkSizeBytes
            val channel = response.bodyAsChannel()

            FileOutputStream(apkFile).use { output ->
                var downloaded = 0L
                val readBuffer = ByteArray(16 * 1024)
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(readBuffer, 0, readBuffer.size)
                    if (bytesRead <= 0) break
                    output.write(readBuffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (totalBytes > 0) {
                        onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }

            onProgress(1f)
            installApkWithPackageInstaller(apkFile)
        }
    }

    private fun installApkWithPackageInstaller(apkFile: File) {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        session.use { s ->
            FileInputStream(apkFile).use { input ->
                s.openWrite("CartVR.apk", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    s.fsync(output)
                }
            }
            s.commit(InstallStatusReceiver.createPendingIntent(context).intentSender)
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
