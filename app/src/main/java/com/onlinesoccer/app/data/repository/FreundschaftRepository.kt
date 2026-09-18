package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.HtmlTools
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.FreundschaftOption
import com.onlinesoccer.app.data.model.FreundschaftSpiel
import com.onlinesoccer.app.data.model.Freundschaftsdaten
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Element
import org.jsoup.Jsoup

@Singleton
class FreundschaftRepository @Inject constructor(
    private val client: OkHttpClient,
) {
    private var endpoint = "/osneu/friendlies"

    suspend fun lade(): Freundschaftsdaten = withContext(Dispatchers.IO) {
        val html = get("/osneu/friendlies")?.also { endpoint = "/osneu/friendlies" }
            ?: get("/friendly.php")?.also { endpoint = "/friendly.php" }
            ?: throw IOException("Freundschaftsspiele konnten nicht geladen werden.")
        parse(html)
    }

    suspend fun blindeEinladung(zat: String, doppelt: Boolean): Freundschaftsdaten = aktion(
        mapOf("blindZat" to zat, "insertBlind" to "Blinde Einladung absenden") +
            if (doppelt) mapOf("blindDouble" to "1") else emptyMap(),
    )

    suspend fun reserviere(zats: List<String>): Freundschaftsdaten = aktionListe(
        zats.map { "reserveZat[]" to it } + ("insertResZat" to "ZAT reservieren"),
    )

    suspend fun loescheReservierungen(): Freundschaftsdaten = aktion(mapOf("delResAll" to "1"))

    suspend fun storniere(id: String): Freundschaftsdaten = aktion(
        mapOf("FssId" to id, "requestStornoFssFixed" to "Stornieren"),
    )

    suspend fun zeigeTeams(land: String, liga: String): Freundschaftsdaten = aktion(
        mapOf("FssLand" to land, "FssLiga" to liga, "showFssTeams" to "Teams anzeigen"),
    )

    internal fun parse(html: String): Freundschaftsdaten {
        val doc = Jsoup.parse(html)
        fun optionen(vararg names: String): List<FreundschaftOption> = doc.select("select").firstOrNull {
            it.attr("name") in names
        }?.select("option")?.mapNotNull { option ->
            val label = option.text().trim()
            val value = option.attr("value").trim()
            if (!option.hasAttr("disabled") && label.isNotEmpty() && value.isNotEmpty()) FreundschaftOption(value, label) else null
        }.orEmpty()

        val meldungen = doc.select(".success, .error, .warning, .info, .message, .meldung")
            .map { it.text().trim() }.filter { it.isNotEmpty() }.distinct()
        val spiele = doc.select("table tr").mapNotNull { row -> parseSpiel(row) }
        val reservierbare = optionen("reserveZat[]", "reserve[]")
        val reservierte = doc.select("select").filter { it.attr("name") in setOf("reserveZat[]", "reserve[]") }
            .flatMap { it.select("option[selected]") }
            .map { FreundschaftOption(it.attr("value"), it.text().trim()) }

        return Freundschaftsdaten(
            spiele = spiele,
            reservierteZats = reservierte,
            reservierbareZats = reservierbare.filterNot { option -> reservierte.any { it.value == option.value } },
            blindZats = optionen("blindZat", "blindzat"),
            laender = optionen("FssLand", "land"),
            ligen = optionen("FssLiga", "lliga"),
            teams = doc.select("a[href*='st.php?c=']").mapNotNull { link ->
                val value = Regex("[?&]c=(\\d+)").find(link.attr("href"))?.groupValues?.get(1) ?: return@mapNotNull null
                FreundschaftOption(value, link.text().trim())
            }.distinctBy { it.value },
            meldungen = meldungen,
        )
    }

    private fun parseSpiel(row: Element): FreundschaftSpiel? {
        val text = row.text().trim()
        if (text.isEmpty() || text.length > 400 || !Regex("ZAT|Freund|FSS|Spiel", RegexOption.IGNORE_CASE).containsMatchIn(text)) return null
        val cells = row.select("td").map { it.text().trim() }.filter { it.isNotEmpty() }
        if (cells.size < 2) return null
        val id = row.selectFirst("input[name=FssId], input[name=fssid]")?.attr("value")?.takeIf { it.isNotBlank() }
        val zat = Regex("ZAT\\s*([0-9]+)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
        val datenZellen = if (id != null && cells.size > 2) cells.dropLast(1) else cells
        return FreundschaftSpiel(id, zat, datenZellen.last(), datenZellen.drop(1).dropLast(1).joinToString(" · "), id != null)
    }

    private suspend fun aktion(params: Map<String, String>): Freundschaftsdaten = withContext(Dispatchers.IO) {
        postAndReload(params.toList())
    }

    private suspend fun aktionListe(params: List<Pair<String, String>>): Freundschaftsdaten = withContext(Dispatchers.IO) {
        postAndReload(params)
    }

    private suspend fun postAndReload(params: List<Pair<String, String>>): Freundschaftsdaten {
        val html = post(params) ?: throw IOException("Freundschaftsspiel-Aktion konnte nicht ausgeführt werden.")
        Jsoup.parse(html).select(".error, .errorbox, .errortext").firstOrNull()?.text()
            ?.takeIf { it.isNotBlank() }
            ?.let { throw IOException(it) }
        return lade()
    }

    private fun get(path: String): String? = request(Request.Builder().url("${OsApi.BASE_URL}$path").get().build())

    private fun post(params: List<Pair<String, String>>): String? {
        val umgeschrieben = if (endpoint == "/friendly.php") params.map { (key, value) ->
            when (key) {
                "blindZat" -> "blindzat" to value
                "blindDouble" -> "doppelt" to value
                "insertBlind" -> "blind" to value
                "reserveZat[]" -> "reserve[]" to value
                "insertResZat" -> "sreserve" to value
                "delResAll" -> "deleteall" to value
                "requestStornoFssFixed" -> "confirmdelete" to value
                "showFssTeams" -> "sland" to value
                else -> key to value
            }
        } else params
        val body = FormBody.Builder().apply { umgeschrieben.forEach { (key, value) -> add(key, value) } }.build()
        return request(Request.Builder().url("${OsApi.BASE_URL}$endpoint").post(body).build())
    }

    private fun request(request: Request): String? = try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            if (SessionGuard.isLoginView(bytes)) return null
            HtmlTools.serverText(bytes)
        }
    } catch (_: IOException) {
        null
    }
}
