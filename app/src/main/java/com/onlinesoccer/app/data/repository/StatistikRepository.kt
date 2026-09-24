package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.HtmlTools
import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.LaenderOption
import com.onlinesoccer.app.data.model.TopscorerDaten
import com.onlinesoccer.app.data.model.TopscorerZeile
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
 * „Statistiken"-Bereich der Website. Derzeit umgesetzt: Topteams (`osneu/statteam`)
 * und Topscorer (`topscorer.php`). Alle Aufrufe sind rein lesend und funktionieren
 * auch ohne Anmeldung.
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

    // ---------------------------------------------------------------- Topscorer

    /**
     * Topscorer (`topscorer.php`): immer nur die Top 100 nach Land/Liga/Statistik/
     * Position/Saison/Art. Wie bei Topteams erscheint die Ergebnistabelle nur,
     * wenn alle Parameter gesendet werden (auch mit 0 = „Alle").
     */
    suspend fun topscorer(
        land: String,
        liga: String,
        statistik: String,
        position: String,
        saison: String,
        art: String,
    ): TopscorerDaten = withContext(Dispatchers.IO) {
        val query = listOf(
            "landauswahl=${land.ifBlank { "0" }}",
            "ligaauswahl=${liga.ifBlank { "0" }}",
            "statistik=${statistik.ifBlank { "1" }}",
            "pos=${position.ifBlank { "0" }}",
            "saison=${saison.ifBlank { "0" }}",
            "art=${art.ifBlank { "0" }}",
        ).joinToString("&")
        val html = safeGet("${OsApi.BASE_URL}/topscorer.php?$query") ?: return@withContext TopscorerDaten()
        parseTopscorer(html)
    }

    internal fun parseTopscorer(html: String): TopscorerDaten {
        val doc = Jsoup.parse(html)
        val tabelle = resultTabelle(doc, listOf("Verein", "Wert"))
        // Erste Zeile ist die Kopfzeile – auf dieser Seite mit <td>, nicht <th>.
        val zeilen = tabelle?.select("tr")?.drop(1)?.mapNotNull { tr ->
            val zellen = tr.select("td")
            if (zellen.size < 9) return@mapNotNull null
            val nameZelle = zellen[1]
            val spielerLink = nameZelle.selectFirst("a")
            val name = spielerLink?.text()?.trim() ?: nameZelle.text().trim()
            if (name.isBlank()) return@mapNotNull null
            val teamZelle = zellen[6]
            val teamLink = teamZelle.selectFirst("a")
            TopscorerZeile(
                nr = zellen[0].text().trim().toIntOrNull(),
                pid = jsId(spielerLink, "spielerinfo"),
                name = name,
                position = spielerLink?.parent()?.className()?.trim()?.ifEmpty { null },
                alter = zellen[2].text().trim(),
                skill = zellen[3].text().trim(),
                opti = zellen[4].text().trim(),
                land = flagCode(zellen[5]),
                teamId = jsId(teamLink, "teaminfo"),
                team = teamLink?.text()?.trim() ?: teamZelle.text().trim(),
                liga = zellen[7].text().trim(),
                wert = zellen[8].text().trim(),
            )
        }.orEmpty()
        return TopscorerDaten(
            laender = optionenMitAlle(doc, "landauswahl"),
            ligas = optionenMitAlle(doc, "ligaauswahl"),
            statistiken = optionen(doc, "statistik"),
            positionen = optionenMitAlle(doc, "pos"),
            saisons = optionenMitAlle(doc, "saison"),
            arten = optionenMitAlle(doc, "art"),
            // Die Website listet Spieler je Filter teils doppelt (z.B. Vereinswechsel)
            // – pro Spieler bleibt nur der erste Eintrag.
            zeilen = zeilen.distinctBy { it.pid ?: it.name },
        )
    }

    /** Ergebnistabelle einer Suchseite (Kopfzeile enthält alle gesuchten Spalten). */
    private fun resultTabelle(doc: Document, spalten: List<String>): Element? =
        doc.select("table").firstOrNull { t ->
            // Kopfzeile je Seite als <th> (statteam) oder <td> (topscorer).
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

    /** ID aus einem JS-Link `javascript:funktion(123)` → 123. */
    private fun jsId(link: Element?, funktion: String): Long? =
        link?.attr("href")?.let { Regex("$funktion\\((\\d+)\\)").find(it)?.groupValues?.get(1)?.toLongOrNull() }

    private fun safeGet(url: String): String? = try {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            val bytes = response.body?.bytes() ?: return null
            if (SessionGuard.isLoginView(bytes)) null else HtmlTools.serverText(bytes)
        }
    } catch (e: IOException) {
        null
    }
}