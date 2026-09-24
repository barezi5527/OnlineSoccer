package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.HtmlTools
import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.LaenderOption
import com.onlinesoccer.app.data.model.TopTeamZeile
import com.onlinesoccer.app.data.model.TopteamsDaten
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * „Statistiken"-Bereich der Website. Derzeit umgesetzt: Topteams (`osneu/statteam`).
 * Alle Aufrufe sind rein lesend und funktionieren auch ohne Anmeldung.
 */
@Singleton
class StatistikRepository @Inject constructor(
    private val client: OkHttpClient,
) {

    /**
     * Topteams (`osneu/statteam`): Wertvollste Teams nach Land/Liga/Statistik/Anzeige.
     * Die Ergebnistabelle erscheint nur, wenn alle vier Parameter gesendet werden
     * (auch explizit mit 0 = „Alle").
     */
    suspend fun topteams(land: String, liga: String, statistik: String, anzeige: String): TopteamsDaten =
        withContext(Dispatchers.IO) {
            val query = listOf(
                "comboland=${land.ifBlank { "0" }}",
                "comboliga=${liga.ifBlank { "0" }}",
                "comboStatistik=${statistik.ifBlank { "2" }}",
                "comboAnzeige=${anzeige.ifBlank { "1" }}",
            ).joinToString("&")
            val html = safeGet("${OsApi.BASE_URL}/osneu/statteam?$query") ?: return@withContext TopteamsDaten()
            parseTopteams(html)
        }

    internal fun parseTopteams(html: String): TopteamsDaten {
        val doc = Jsoup.parse(html)
        val tabelle = resultTabelle(doc, listOf("Team", "Wert"))
        val zeilen = tabelle?.select("tr")?.mapNotNull { tr ->
            val zellen = tr.select("td")
            if (zellen.size < 4) return@mapNotNull null
            val teamZelle = zellen[1]
            val teamLink = teamZelle.selectFirst("a[href*='st.php']")
            val team = teamLink?.text()?.trim() ?: teamZelle.text().trim()
            if (team.isBlank()) return@mapNotNull null
            TopTeamZeile(
                nr = zellen[0].text().trim().toIntOrNull(),
                teamId = teamLink?.attr("href")
                    ?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                team = team,
                land = flagCode(zellen[2]),
                wert = zellen[3].text().trim(),
            )
        }.orEmpty()
        return TopteamsDaten(
            laender = optionenMitAlle(doc, "comboland"),
            ligas = optionenMitAlle(doc, "comboliga"),
            statistiken = optionen(doc, "comboStatistik"),
            anzeigen = optionen(doc, "comboAnzeige"),
            zeilen = zeilen,
        )
    }

    /** Ergebnistabelle einer Suchseite (Kopfzeile enthält alle gesuchten Spalten). */
    private fun resultTabelle(doc: Document, spalten: List<String>): Element? =
        doc.select("table").firstOrNull { t ->
            val kopf = t.select("tr").firstOrNull()?.select("th, td")?.map { it.text().trim() }.orEmpty()
            spalten.all { s -> kopf.any { it == s || it.contains(s, ignoreCase = true) } }
        }

    /** Optionen eines `select[name=…]`; der Platzhalter (Wert 0) wird als „Alle" gelistet. */
    private fun optionenMitAlle(doc: Document, name: String): List<LaenderOption> =
        doc.select("select[name=$name] option").mapNotNull { opt ->
            val id = opt.attr("value")
            val label = opt.text().trim()
            when {
                id.isBlank() || label.isBlank() -> null
                id == "0" -> LaenderOption("0", "Alle")
                else -> LaenderOption(id, label)
            }
        }

    /** Optionen eines `select[name=…]` ohne den „Alle"-Platzhalter (Statistik-/Anzeige-Auswahl). */
    private fun optionen(doc: Document, name: String): List<LaenderOption> =
        doc.select("select[name=$name] option").mapNotNull { opt ->
            val id = opt.attr("value")
            val label = opt.text().trim()
            if (id.isBlank() || id == "0" || label.isBlank()) null else LaenderOption(id, label)
        }

    /** Ländercode aus der Flaggen-Zelle (`images/flaggen/GER.gif` → „GER"). */
    private fun flagCode(zelle: Element): String =
        zelle.selectFirst("img[src*='flaggen/']")?.attr("src")
            ?.let { Regex("flaggen/(\\w+)\\.").find(it)?.groupValues?.get(1) }.orEmpty()

    private fun safeGet(url: String): String? = try {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            val bytes = response.body?.bytes() ?: return null
            if (SessionGuard.isLoginView(bytes)) null else HtmlTools.serverText(bytes)
        }
    } catch (e: IOException) {
        null
    }
}