package com.charavault.app.data.release

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class ReleaseUpdateManifest(
    val channel: String = "stable",
    val versionName: String,
    val versionCode: Long,
    val apkUrl: String,
    val sha256: String? = null,
    val minSupportedVersionCode: Long = 1
)

data class AvailableUpdate(
    val versionName: String,
    val versionCode: Long,
    val apkUrl: String,
    val sha256: String?
)

class UpdateChecker(
    private val context: Context,
    private val manifestUrl: String = DEFAULT_MANIFEST_URL
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(): AvailableUpdate? = withContext(Dispatchers.IO) {
        val currentVersionCode = currentVersionCode()
        val manifest = fetchManifest()

        if (manifest.versionCode > currentVersionCode) {
            AvailableUpdate(
                versionName = manifest.versionName,
                versionCode = manifest.versionCode,
                apkUrl = manifest.apkUrl,
                sha256 = manifest.sha256
            )
        } else {
            null
        }
    }

    private fun fetchManifest(): ReleaseUpdateManifest {
        val connection = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }

        return connection.use {
            if (responseCode !in 200..299) {
                throw IllegalStateException("Update check failed with HTTP $responseCode")
            }
            json.decodeFromString<ReleaseUpdateManifest>(
                inputStream.bufferedReader().use { reader -> reader.readText() }
            )
        }
    }

    private fun currentVersionCode(): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    private inline fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T {
        return try {
            block()
        } finally {
            disconnect()
        }
    }

    companion object {
        const val DEFAULT_MANIFEST_URL =
            "https://github.com/Future-404/CharaVault/releases/latest/download/update.json"
    }
}
