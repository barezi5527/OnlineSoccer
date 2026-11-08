package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/** Öffentlich sichtbare, nicht personenbezogene Werte der Startseite. */
@Singleton
class PublicStatsRepository @Inject constructor(
    private val client: OkHttpClient,
) {

    /** Lädt die Zahl der aktiven Manager ohne Anmeldung. */
    suspend fun aktiveManager(): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(OsApi.INDEX).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            parseAktiveManager(response.body?.string().orEmpty())
            }
        }.getOrNull()
    }

}

internal fun parseAktiveManager(html: String): Int? {
    val text = Jsoup.parse(html).text()
    return Regex("(?i)\\b([0-9][0-9.]*)\\s+Mitstreiter\\b")
        .find(text)
        ?.groupValues
        ?.get(1)
        ?.replace(".", "")
        ?.toIntOrNull()
}
