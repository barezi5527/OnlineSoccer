package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.LigaFilter
import com.onlinesoccer.app.data.model.LigaOption
import com.onlinesoccer.app.data.model.LigaSaison
import com.onlinesoccer.app.data.model.LigaSpiel
import com.onlinesoccer.app.data.model.LigaSpieltag
import com.onlinesoccer.app.data.model.LigaTabellenKlasse
import com.onlinesoccer.app.data.model.LigaTabelle
import com.onlinesoccer.app.data.model.PokalAnsicht
import com.onlinesoccer.app.data.model.PokalRunde
import com.onlinesoccer.app.data.model.PokalSpiel
import com.onlinesoccer.app.data.model.PokalRundeOption
import com.onlinesoccer.app.data.model.WertTabelle
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/** Nationale Bewerbe: Ligatabelle, Spieltage, Landespokal. */
@Singleton
class BewerbeRepository @Inject constructor(
    private val client: OkHttpClient,
) {

    suspend fun ladeLigatabelle(
        filter: LigaFilter? = null,
        eigeneTeamId: Int? = null,
    ): LigaTabelle? = withContext(Dispatchers.IO) {
        val url = if (filter != null) {
            "${OsApi.BASE_URL}/lt.php?ligaauswahl=${filter.liga}&landauswahl=${filter.land}&tabauswahl=${filter.tab}&saauswahl=${filter.saison}&stataktion=Statistik+ausgeben"
        } else {
            "${OsApi.BASE_URL}/lt.php"
        }
        val html = safeGet(url) ?: return@withContext null
        parseLigatabelle(html, eigeneTeamId)
    }

    /**
     * Spieltage (`ls.php`). Ohne Parameter wird die Standard-/Eigenkonfiguration
     * des Server geladen (eigene Liga/Land/Saison); mit den Parametern wird die
     * gewünschte Liga/Land/Saison/Spieltag abgefragt.
     */
    suspend fun ladeSpieltag(
        zat: Int? = null,
        liga: Int? = null,
        land: Int? = null,
        saison: Int? = null,
    ): LigaSpieltag? = withContext(Dispatchers.IO) {
        val url = buildString {
            append("${OsApi.BASE_URL}/ls.php?erganzeigen=1&stataktion=Statistik+ausgeben")
            liga?.let { append("&ligaauswahl=$it") }
            land?.let { append("&landauswahl=$it") }
            saison?.let { append("&saauswahl=$it") }
            zat?.let { append("&stauswahl=$it") }
        }
        val html = safeGet(url) ?: return@withContext null
        parseSpieltag(html)
    }

    suspend fun ladePokal(saison: Int? = null, runde: Int? = null, land: Int? = null): PokalAnsicht = withContext(Dispatchers.IO) {
        // Land und Runde aus der leeren Form auslesen, dann mit Parametern laden
        val form = safeGet("${OsApi.BASE_URL}/lp.php")
            ?: throw IOException("Landespokal konnte nicht geladen werden.")
        val landWert = land ?: selectedInt(form, "landauswahl")
        val standardRunde = selectedInt(form, "stauswahl").coerceAtLeast(1)
        val standardSaison = selectedInt(form, "saauswahl").coerceAtLeast(1)
        val gewaehlteRunde = (runde ?: standardRunde).coerceAtLeast(1)
        val gewaehlteSaison = (saison ?: standardSaison).coerceAtLeast(1)
        val url = "${OsApi.BASE_URL}/lp.php?landauswahl=$landWert&stauswahl=$gewaehlteRunde&saauswahl=$gewaehlteSaison&erganzeigen=1&stataktion=Statistik+ausgeben"
        val html = safeGet(url)
            ?: throw IOException("Landespokal konnte nicht geladen werden.")
        parsePokal(html)
    }

    /** Ligatabelle: Haupttabelle + Saison-Optionen + aktueller Filter + eigener Club. */
    internal fun parseLigatabelle(html: String, eigeneTeamId: Int? = null): LigaTabelle? {
        val doc = Jsoup.parse(html)
        fun selectedInt(name: String): Int =
            doc.select("select[name=$name] option[selected]").firstOrNull()
                ?.attr("value")?.toIntOrNull() ?: 0

        val saisonen = doc.select("select[name=saauswahl] option")
            .mapNotNull { o ->
                o.attr("value").toIntOrNull()?.takeIf { it > 0 }?.let { LigaSaison(it, o.text().trim()) }
            }
        val saison = selectedInt("saauswahl")

        // Haupttabelle (meiste Zeilen) – der Header ist eine <td>-Zeile ohne <th>.
        val haupt = doc.select("table").filter { it.select("tr").isNotEmpty() }
            .maxByOrNull { it.select("tr").size } ?: return null
        val rows = haupt.select("tr").filter { it.select("td").isNotEmpty() }
        if (rows.isEmpty()) return null
        val kopfIndex = rows.indexOfFirst { it.select("th").isNotEmpty() }.let { if (it < 0) 0 else it }
        val header = rows[kopfIndex].select("th,td").map { it.text().trim() }
        val dataRows = rows.drop(kopfIndex + 1).toList()

        val teamIds = dataRows.map { tr ->
            tr.select("a[href*='teaminfo']").firstNotNullOfOrNull { a ->
                Regex("teaminfo\\((\\d+)\\)").find(a.attr("href"))?.groupValues?.get(1)?.toLongOrNull()
            }
        }
        val klassen = dataRows.map { tr -> LigaTabellenKlasse.vonCssKlasse(tr.className()) }
        val eigenZeile = eigeneTeamId?.let { own ->
            teamIds.indexOfFirst { it == own.toLong() }.takeIf { it >= 0 }
        }

        return LigaTabelle(
            header = header,
            zeilen = dataRows.map { tr -> tr.select("td").map { it.text().trim() } },
            zeilenTeamIds = teamIds,
            zeilenKlasse = klassen,
            eigenZeile = eigenZeile,
            saisonen = saisonen,
            saison = saison,
            ligaOptionen = optionen(doc, "ligaauswahl"),
            landOptionen = optionen(doc, "landauswahl"),
            tabOptionen = optionen(doc, "tabauswahl"),
            filter = LigaFilter(
                liga = selectedInt("ligaauswahl"),
                land = selectedInt("landauswahl"),
                tab = selectedInt("tabauswahl"),
                saison = saison,
            ),
        )
    }

    internal fun parsePokal(html: String): PokalAnsicht {
        val doc = Jsoup.parse(html)
        val runden = doc.select("table").mapNotNull { tabelle ->
            val spiele = mutableListOf<PokalSpiel>()
            tabelle.select("tr").forEach { tr ->
                val teams = tr.select("td a[href*='teaminfo']")
                if (teams.size < 2) return@forEach
                val heim = teams[0].text().trim()
                val gast = teams[1].text().trim()
                if (heim.isEmpty() || gast.isEmpty()) return@forEach
                val ergebnis = tr.select("td").map { it.text().trim() }
                    .firstOrNull { Regex("\\d+\\s*:\\s*\\d+").containsMatchIn(it) }
                val berichtUrl = if (ergebnis != null) {
                    tr.selectFirst("a[href*='os_bericht']")?.attr("href")?.let(::osBerichtUrl)
                } else {
                    null
                }
                spiele += PokalSpiel(heim = heim, gast = gast, ergebnis = ergebnis, berichtUrl = berichtUrl)
            }
            if (spiele.isEmpty()) return@mapNotNull null
            PokalRunde(spiele = spiele)
        }
        return PokalAnsicht(
            saison = selectedInt(html, "saauswahl"),
            land = selectedInt(html, "landauswahl"),
            runde = selectedInt(html, "stauswahl"),
            saisonen = doc.select("select[name=saauswahl] option").mapNotNull { option ->
                option.attr("value").toIntOrNull()?.takeIf { it > 0 }
                    ?.let { LigaSaison(it, option.text().trim()) }
            },
            rundenOptionen = doc.select("select[name=stauswahl] option").mapNotNull { option ->
                option.attr("value").toIntOrNull()?.takeIf { it > 0 }
                    ?.let { PokalRundeOption(it, option.text().trim()) }
            },
            landOptionen = optionen(doc, "landauswahl"),
            runden = runden,
        )
    }

    /**
     * Spieltage (`ls.php`): Die Tabelle hat keine normale Struktur, sondern pro
     * Spiel eine Zeile mit `teaminfo(H)`/`teaminfo(G)` sowie optional Zelltext
     * „0 : 2“ (gespielte Begegnung) und `os_bericht(H,G,typ,zat)` + `spielpreview(H,G,typ)`.
     */
    internal fun parseSpieltag(html: String): LigaSpieltag? {
        val doc = Jsoup.parse(html)
        val stauswahl = doc.select("select[name=stauswahl] option[selected]").attr("value")
        val liga = doc.select("select[name=ligaauswahl] option[selected]").attr("value")
        val land = doc.select("select[name=landauswahl] option[selected]").attr("value")
        val saison = doc.select("select[name=saauswahl] option[selected]").attr("value")
        val saisonVm = saison.toIntOrNull() ?: 0
        val zat = stauswahl.toIntOrNull() ?: 0
        val zatOptionen = doc.select("select[name=stauswahl] option")
            .mapNotNull { it.attr("value").toIntOrNull() }

        val spiele = mutableListOf<LigaSpiel>()
        doc.select("table tr").forEach { tr ->
            val teams = tr.select("td a[href*='teaminfo']")
            if (teams.size < 2) return@forEach
            val heimId = teamId(teams[0])
            val gastId = teamId(teams[1])
            // Server liefert ggf. Positionszusatz „Verein (8.)“ – fürs UI ablösen.
            val heim = ohnePositionszusatz(teams[0].text())
            val gast = ohnePositionszusatz(teams[1].text())
            if (heim.isEmpty() || gast.isEmpty()) return@forEach

            val ergebnis = tr.select("td").map { it.text().trim() }
                .firstOrNull { it.matches(Regex("\\d+\\s*:\\s*\\d+")) }
            val (toreH, toreG) = ergebnis?.let { splitErgebnis(it) } ?: (null to null)
            val gespielt = toreH != null

            // Bericht nur für gespielte Spiele (sonst leerer os_bericht-Anker → tote URL).
            val berichtUrl = if (gespielt) {
                tr.select("td a[href*='os_bericht']").firstOrNull()?.attr("href")?.let(::osBerichtUrl)
            } else {
                null
            }
            val vorschauId = tr.select("td a[href*='spielpreview']").firstOrNull()
                ?.attr("href")?.let(::spielpreviewId)

            spiele += LigaSpiel(
                zat = zat,
                heim = heim,
                gast = gast,
                heimId = heimId,
                gastId = gastId,
                toreHeim = toreH,
                toreGast = toreG,
                gespielt = gespielt,
                berichtUrl = berichtUrl,
                vorschauId = vorschauId,
            )
        }
        return LigaSpieltag(
            zat = zat,
            saison = saisonVm,
            liga = liga.toIntOrNull() ?: 0,
            land = land.toIntOrNull() ?: 0,
            zatOptionen = zatOptionen,
            ligaOptionen = optionen(doc, "ligaauswahl"),
            landOptionen = optionen(doc, "landauswahl"),
            saisonOptionen = optionen(doc, "saauswahl"),
            spiele = spiele.distinctBy { it.heim to it.gast },
        )
    }

    /** Entfernt Positionszusatz „SC Viktoria Ulm (8.)“ → „SC Viktoria Ulm“. */
private fun ohnePositionszusatz(name: String): String =
    name.trim().replace(Regex("\\s*\\(\\d+\\.\\)\\s*$"), "")

/** `spielpreview(H,G,typ)` -> Spielfyp-Kennung für die Vorschau. */
private fun spielpreviewId(jsCall: String): String? =
    Regex("spielpreview\\((\\d+),(\\d+),(\\d+)\\)").find(jsCall)
        ?.groupValues?.get(3)?.trim()?.takeIf { it.isNotBlank() }

private fun teamId(link: Element): Long? =
    Regex("teaminfo\\((\\d+)\\)").find(link.attr("href"))?.groupValues?.get(1)?.toLongOrNull()

    /** Gewählter Wert eines `<select>` aus der HTML-Seite. */
    internal fun selectedInt(html: String, name: String): Int =
        Jsoup.parse(html).select("select[name=$name] option[selected]").firstOrNull()
            ?.attr("value")?.toIntOrNull() ?: 0

    /** Wählbare Optionen eines `<select>` – Platzhalter-Optionen („--…--“) werden ignoriert. */
    private fun optionen(doc: org.jsoup.nodes.Document, name: String): List<LigaOption> =
        doc.select("select[name=$name] option").mapNotNull { o ->
            val wert = o.attr("value").toIntOrNull() ?: return@mapNotNull null
            val label = o.text().trim()
            if (label.isEmpty() || label.startsWith("--")) return@mapNotNull null
            LigaOption(wert, label)
        }

    /** `os_bericht(H,G,typ,saison)` -> `/rep/saison/{saison}/{zat}/{heim}-{gast}.html`. */
    internal fun osBerichtUrl(jsCall: String): String? {
        val m = Regex("os_bericht\\((\\d+),(\\d+),(\\d+),(\\d+)\\)").find(jsCall) ?: return null
        val (heim, gast, typ, saison) = m.destructured
        val zat = typ
        return "${OsApi.BASE_URL}/rep/saison/$saison/$zat/$heim-$gast.html"
    }

    internal fun splitErgebnis(text: String): Pair<Int?, Int?> {
        val norm = text.replace("–", "-").replace(" ", "")
        val match = Regex("(\\d+)[:-](\\d+)").find(norm) ?: return null to null
        return match.groupValues[1].toIntOrNull() to match.groupValues[2].toIntOrNull()
    }

    private fun safeGet(url: String): String? {
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                if (SessionGuard.isPureLoginView(bytes)) return null
                bytes.toString(Charsets.UTF_8)
            }
        } catch (e: IOException) {
            null
        }
    }
}
