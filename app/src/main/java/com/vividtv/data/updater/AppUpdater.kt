package com.vividtv.data.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val apkUrl: String,
    val releaseNotes: String = "",
)

@Serializable
data class GitHubRelease(
    val tag_name: String = "",
    val body: String = "",
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    val name: String = "",
    val browser_download_url: String = "",
)

/**
 * 自动更新管理器
 * 从 GitHub Releases 检测新版本并下载 APK
 */
@Singleton
class AppUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /** GitHub 仓库地址 */
    private val repoOwner = "zni56895-maker"
    private val repoName = "vivid-tv"

    /** 下载任务 ID */
    private var downloadId: Long = -1L
    private var downloadRegistered = false

    /** 检查是否有新版本 */
    suspend fun checkForUpdate(): UpdateInfo {
        return withContext(Dispatchers.IO) {
            try {
                val currentVersion = getCurrentVersion()
                val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
                val conn = url.openConnection()
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val body = conn.inputStream.bufferedReader().readText()

                val release = json.decodeFromString<GitHubRelease>(body)
                val latestTag = release.tag_name.removePrefix("v")
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }

                if (apkAsset == null || !isNewerVersion(latestTag, currentVersion)) {
                    return@withContext UpdateInfo(false, latestTag, "")
                }

                UpdateInfo(
                    hasUpdate = true,
                    latestVersion = latestTag,
                    apkUrl = apkAsset.browser_download_url,
                    releaseNotes = release.body.take(500),
                )
            } catch (_: Exception) {
                UpdateInfo(false, "", "")
            }
        }
    }

    /** 下载新版本 APK */
    fun downloadUpdate(apkUrl: String) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val uri = Uri.parse(apkUrl)
        val request = DownloadManager.Request(uri).apply {
            setTitle("Vivid TV 更新")
            setDescription("正在下载新版本…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "vivid-tv-update.apk")
            setMimeType("application/vnd.android.package-archive")
        }

        downloadId = downloadManager.enqueue(request)

        if (!downloadRegistered) {
            context.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED,
            )
            downloadRegistered = true
        }
    }

    /** 安装下载好的 APK */
    private fun installApk(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            }
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun getCurrentVersion(): String {
        return try {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            pkg.versionName ?: "0.0.0.0"
        } catch (_: Exception) {
            "0.0.0.0"
        }
    }

    /** 比较版本号 */
    private fun isNewerVersion(latestTag: String, current: String): Boolean {
        val latestParts = latestTag.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l != c) return l > c
        }
        return false
    }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id != downloadId) return

            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "vivid-tv-update.apk"
            )
            if (file.exists()) installApk(file)
        }
    }
}
