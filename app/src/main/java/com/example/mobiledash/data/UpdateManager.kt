package com.example.mobiledash.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.mobiledash.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionName: String,
    val releaseName: String,
    val apkUrl: String,
)

class UpdateManager(private val context: Context) {
    suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val connection = (URL(AppConfig.GITHUB_RELEASES_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MobileDash Android")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val release = JSONObject(body)
            val tag = release.optString("tag_name").trim().removePrefix("v")
            if (tag.isBlank() || !isRemoteVersionNewer(tag, currentVersionName())) return@withContext null
            val asset = release.optJSONArray("assets")
                ?.let { assets ->
                    buildList {
                        for (index in 0 until assets.length()) {
                            assets.optJSONObject(index)?.let(::add)
                        }
                    }
                }
                ?.firstOrNull { asset ->
                    asset.optString("name").endsWith(".apk", ignoreCase = true)
                } ?: return@withContext null
            AppUpdateInfo(
                versionName = tag,
                releaseName = release.optString("name").ifBlank { "Версия $tag" },
                apkUrl = asset.optString("browser_download_url"),
            ).takeIf { it.apkUrl.isNotBlank() }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadAndInstall(update: AppUpdateInfo): Boolean = withContext(Dispatchers.IO) {
        val apkFile = File(context.getExternalFilesDir("updates"), "mobiledash-${update.versionName}.apk")
        apkFile.parentFile?.mkdirs()
        val connection = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "MobileDash Android")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext false
            BufferedInputStream(connection.inputStream).use { input ->
                apkFile.outputStream().use { output -> input.copyTo(output) }
            }
            installApk(apkFile)
            true
        } catch (_: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }

    private fun installApk(apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }
        }
        context.startActivity(intent)
    }

    private fun currentVersionName(): String {
        return runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName.orEmpty()
        }.getOrDefault("")
    }
}

private fun isRemoteVersionNewer(remote: String, current: String): Boolean {
    val remoteParts = remote.versionParts()
    val currentParts = current.versionParts()
    val maxSize = maxOf(remoteParts.size, currentParts.size)
    for (index in 0 until maxSize) {
        val remotePart = remoteParts.getOrNull(index) ?: 0
        val currentPart = currentParts.getOrNull(index) ?: 0
        if (remotePart != currentPart) return remotePart > currentPart
    }
    return false
}

private fun String.versionParts(): List<Int> {
    return trim()
        .removePrefix("v")
        .split(".", "-", "_")
        .mapNotNull { part -> part.takeWhile(Char::isDigit).toIntOrNull() }
}
