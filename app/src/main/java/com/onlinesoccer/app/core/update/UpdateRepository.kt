package com.onlinesoccer.app.core.update

import com.onlinesoccer.app.BuildConfig
import com.onlinesoccer.app.core.network.UpdateClient
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private const val RELEASES_URL =
    "https://api.github.com/repos/barezi5527/OnlineSoccer-Download/releases/latest"
private const val DOWNLOAD_PAGE = "https://barezi5527.github.io/OnlineSoccer-Download/"

@Singleton
class UpdateRepository @Inject constructor(
    @UpdateClient private val client: OkHttpClient,
) {
    suspend fun verfuegbareVersion(): UpdateInfo? {
        val request = Request.Builder().url(RELEASES_URL).get().build()
        val response = client.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) throw IOException("GitHub-Release konnte nicht geladen werden.")
            val json = JSONObject(it.body?.string().orEmpty())
            if (json.optBoolean("draft") || json.optBoolean("prerelease")) return null

            val version = json.optString("tag_name").removePrefix("v")
            if (version.isBlank()) return null
            return UpdateInfo(version, DOWNLOAD_PAGE).takeIf {
                versionVergleich(it.version, BuildConfig.VERSION_NAME) > 0
            }
        }
    }
}

/** Vergleicht Punktversionen numerisch statt lexikografisch. */
internal fun versionVergleich(links: String, rechts: String): Int {
    val a = links.split('.').map { it.toIntOrNull() ?: 0 }
    val b = rechts.split('.').map { it.toIntOrNull() ?: 0 }
    for (index in 0 until maxOf(a.size, b.size)) {
        val vergleich = (a.getOrNull(index) ?: 0).compareTo(b.getOrNull(index) ?: 0)
        if (vergleich != 0) return vergleich
    }
    return 0
}
