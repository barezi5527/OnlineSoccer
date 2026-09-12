package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.AuswahlOption
import com.onlinesoccer.app.data.model.Taktik
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/** Herkunft einer zu ladenden Taktik (`raster1` = Standard, `raster2` = eigene). */
enum class TaktikQuelle { STANDARD, EIGENE }

/**
 * Taktik-Datenquelle (`taktiken.php`).
 *
 * Lese- und Schreibzugriff auf den Taktik-Editor:
 * - `ladeTaktik()` liest Anzeige + Auswahllisten.
 * - `ladeTaktik(id, quelle)` lädt eine gespeicherte Taktik ins Raster (form1).
 * - `loescheTaktik(id)` löscht eine eigene Taktik (form1).
 * - `speichereTaktik(...)` speichert das aktuelle Raster unter einem Namen (form2).
 * Schreibzugriffe erfolgen nur aus bestätigter Nutzerabsicht (eigene Formation → Server).
 */
@Singleton
class TaktikRepository @Inject constructor(
    private val client: OkHttpClient,
) {

    /** Lädt die aktuelle Taktik (angekreuzte Positionen) + Auswahllisten. */
    suspend fun ladeTaktik(): Taktik = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/taktiken.php") ?: return@withContext Taktik(emptySet())
        parse(html)
    }

    /** Lädt eine gespeicherte Taktik (Standard- oder eigene) ins Editor-Raster. */
    suspend fun ladeTaktik(id: String, quelle: TaktikQuelle): Taktik = withContext(Dispatchers.IO) {
        val form = FormBody.Builder().apply {
            this.add("load", "Laden")
            when (quelle) {
                TaktikQuelle.STANDARD -> this.add("raster1", id)
                TaktikQuelle.EIGENE -> this.add("raster2", id)
            }
        }.build()
        val html = post(form) ?: throw IOException("Sitzung abgelaufen – bitte neu anmelden.")
        parse(html)
    }

    /** Löscht eine eigene Taktik (`raster2`). */
    suspend fun loescheTaktik(id: String): Taktik = withContext(Dispatchers.IO) {
        val form = FormBody.Builder()
            .add("delete", "Löschen")
            .add("raster2", id)
            .build()
        val html = post(form) ?: throw IOException("Sitzung abgelaufen – bitte neu anmelden.")
        parse(html)
    }

    /** Speichert die Taktik unter einem Namen (form2 der Taktik-Seite). */
    suspend fun speichereTaktik(namen: String, codes: Set<String>): Taktik = withContext(Dispatchers.IO) {
        val form = FormBody.Builder().apply {
            add("speichername", namen)
            add("speichern", "Speichern unter...")
            codes.sorted().forEach { add("taktik[]", it) }
        }.build()
        val html = post(form) ?: throw IOException("Sitzung abgelaufen – bitte neu anmelden.")
        parse(html)
    }

    /** Liest die Taktik-Formulare der Seite. */
    internal fun parse(html: String): Taktik {
        val doc = Jsoup.parse(html)

        val codes = doc.select("input[type=checkbox][name=taktik[]][checked]")
            .mapNotNull { it.attr("value").takeIf { v -> v.isNotBlank() } }
            .toSet()

        fun optionen(selectName: String) = doc.select("select[name=$selectName] > option")
            .mapNotNull { opt ->
                val id = opt.attr("value")
                val label = opt.text().trim()
                if (id.isBlank() || label.isBlank()) null else AuswahlOption(id, label)
            }
            .filter { it.id != "0" }

        val name = doc.selectFirst("input[name=speichername]")?.attr("value")?.takeIf { it.isNotBlank() }

        return Taktik(
            codes = codes,
            speichername = name,
            standardTaktiken = optionen("raster1"),
            eigeneTaktiken = optionen("raster2"),
        )
    }

    private fun safeGet(url: String): String? {
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: return null
                if (SessionGuard.isLoginView(bytes)) return null
                bytes.toString(Charsets.UTF_8)
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun post(form: FormBody): String? {
        val request = Request.Builder()
            .url("${OsApi.BASE_URL}/taktiken.php")
            .post(form)
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: return null
                if (SessionGuard.isLoginView(bytes)) null else bytes.toString(Charsets.UTF_8)
            }
        } catch (e: IOException) {
            null
        }
    }
}