package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.HtmlTools
import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.AbfrageDetails
import com.onlinesoccer.app.data.model.FairplayZeile
import com.onlinesoccer.app.data.model.GespeicherteAbfrage
import com.onlinesoccer.app.data.model.LaenderOption
import com.onlinesoccer.app.data.model.RekordAbschnitt
import com.onlinesoccer.app.data.model.RekordZeile
import com.onlinesoccer.app.data.model.SpielerVorschlag
import com.onlinesoccer.app.data.model.SpielersucheErgebnis
import com.onlinesoccer.app.data.model.SpielersucheOptionen
import com.onlinesoccer.app.data.model.Spielervergleich
import com.onlinesoccer.app.data.model.Spielstatistiken
import com.onlinesoccer.app.data.model.KennzahlZeile
import com.onlinesoccer.app.data.model.StatistikAbschnitt
import com.onlinesoccer.app.data.model.SucheAttribut
import com.onlinesoccer.app.data.model.SucheBasis
import com.onlinesoccer.app.data.model.SucheKriterium
import com.onlinesoccer.app.data.model.SucheSpielerZeile
import com.onlinesoccer.app.data.model.Tabellenstatistiken
import com.onlinesoccer.app.data.model.TopscorerDaten
import com.onlinesoccer.app.data.model.TopscorerZeile
import com.onlinesoccer.app.data.model.TopspielerDaten
import com.onlinesoccer.app.data.model.TopspielerZeile
import com.onlinesoccer.app.data.model.TopteamsDaten
import com.onlinesoccer.app.data.model.TopteamZeile
import com.onlinesoccer.app.data.model.VergleichZeile
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.json.JSONArray
import org.json.JSONObject

/**
 * „Statistiken"-Bereich der Website: Topteams (`osneu/statteam`), Topspieler
 * (`osneu/statspieler`), Fairplaytabelle (`fpt.php`), Topscorer (`topscorer.php`),
 * Spielersuche (`osneu/spielersuche`), Spielervergleich (`osneu/spielervergleich`),
 * Spielstatistiken (`osneu/statistics`) und Tabellenstatistiken (`osneu/userstatistics`).
 *
 * Alle Aufrufe sind rein lesend und funktionieren auch ohne Anmeldung.
 */
@Singleton
class StatistikRepository @Inject constructor(
    private val client: OkHttpClient,
) {

    // ---------------------------------------------------------------- Topteams

    suspend fun topteams(land: String, liga: String, statistik: String, anzeige: String): TopteamsDaten =
        withContext(Dispatchers.IO) {
            val query = buildList {
                add("comboland=${land.ifBlank { "0" }}")
                add("comboliga=${liga.ifBlank { "0" }}")
                add("comboStatistik=${statistik.ifBlank { "2" }}")
                add("comboAnzeige=${anzeige.ifBlank { "1" }}")
            }.joinToString("&")
            val html = safeGet("${OsApi.BASE_URL}/osneu/statteam?$query") ?: return@withContext TopteamsDaten()
            parseTopteams(html)
        }

    internal fun parseTopteams(html: String): TopteamsDaten {
        val doc = Jsoup.parse(html)
        val tabelle = resultTabelle(doc, listOf("Team", "Wert"))
        val zeilen = tabelle?.select("tr")?.mapNotNull { tr ->
            val zellen = tr.select("td")
            if (zellen.size < 4) return@mapNotNull null
            val teamLink = zellen[1].selectFirst("a[href*='st.php']")
            val team = teamLink?.text()?.trim() ?: zellen[1].text().trim()
            if (team.isBlank()) return@mapNotNull null
            TopteamZeile(
                nr = zellen[0].text().trim().toIntOrNull(),
                teamId = teamLink?.attr("href")?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                team = team,
                land = flagCode(zellen[2]),
                wert = zellen[3].text().trim(),
            )
        }.orEmpty()
        return TopteamsDaten(
            laender = optionen(doc, "comboland"),
            ligas = optionen(doc, "comboliga"),
            statistiken = optionen(doc, "comboStatistik"),
            anzeigen = optionen(doc, "comboAnzeige"),
            zeilen = zeilen,
        )
    }

    // -------------------------------------------------------------- Topspieler

    suspend fun topspieler(land: String, liga: String, statistik: String, position: String, anzeige: String): TopspielerDaten =
        withContext(Dispatchers.IO) {
            val query = buildList {
                add("comboLand=${land.ifBlank { "0" }}")
                add("comboLiga=${liga.ifBlank { "0" }}")
                add("comboStatistik=${statistik.ifBlank { "16" }}")
                add("comboPosition=${position.ifBlank { "0" }}")
                add("comboAnzeige=${anzeige.ifBlank { "1" }}")
            }.joinToString("&")
            val html = safeGet("${OsApi.BASE_URL}/osneu/statspieler?$query") ?: return@withContext TopspielerDaten()
            parseTopspieler(html)
        }

    internal fun parseTopspieler(html: String): TopspielerDaten {
        val doc = Jsoup.parse(html)
        val tabelle = resultTabelle(doc, listOf("Spieler", "Wert"))
        val zeilen = tabelle?.select("tr")?.mapNotNull { tr ->
            val zellen = tr.select("td")
            if (zellen.size < 7) return@mapNotNull null
            val nameZelle = zellen[1]
            val spielerLink = nameZelle.selectFirst("a[href*='sp.php']")
            val teamLink = zellen[2].selectFirst("a[href*='st.php']")
            val name = spielerLink?.text()?.trim() ?: nameZelle.text().trim()
            if (name.isBlank()) return@mapNotNull null
            TopspielerZeile(
                nr = zellen[0].text().trim().toIntOrNull(),
                pid = spielerLink?.attr("href")?.let { Regex("s=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                name = name,
                position = nameZelle.selectFirst("a")?.parent()?.className()?.trim()?.ifEmpty { null },
                teamId = teamLink?.attr("href")?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                team = teamLink?.text()?.trim().orEmpty(),
                alter = zellen[3].text().trim(),
                nation = flagCode(zellen[5]),
                wert = zellen[6].text().trim(),
            )
        }.orEmpty()
        return TopspielerDaten(
            laender = optionen(doc, "comboLand"),
            ligas = optionen(doc, "comboLiga"),
            statistiken = optionen(doc, "comboStatistik"),
            positionen = optionen(doc, "comboPosition"),
            anzeigen = optionen(doc, "comboAnzeige"),
            zeilen = zeilen,
        )
    }

    // ---------------------------------------------------------------- Fairplay

    suspend fun fairplay(): List<FairplayZeile> = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/fpt.php") ?: return@withContext emptyList()
        parseFairplay(html)
    }

    internal fun parseFairplay(html: String): List<FairplayZeile> {
        val doc = Jsoup.parse(html)
        return doc.select("table#fairplay tr").drop(1).mapNotNull { tr ->
            val zellen = tr.select("td")
            if (zellen.size < 7) return@mapNotNull null
            val team = zellen[1].text().trim()
            if (team.isBlank()) return@mapNotNull null
            FairplayZeile(
                nr = zellen[0].text().trim().toIntOrNull(),
                teamId = zellen[1].selectFirst("a")?.attr("onClick")
                    ?.let { Regex("teaminfo\\((\\d+)\\)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                team = team,
                gelb = zellen[2].text().trim().toIntOrNull(),
                gelbRot = zellen[3].text().trim().toIntOrNull(),
                rot = zellen[4].text().trim().toIntOrNull(),
                spiele = zellen[5].text().trim().toIntOrNull(),
                punkte = zellen[6].text().trim().replace(',', '.').toDoubleOrNull(),
            )
        }
    }

    // ---------------------------------------------------------------- Topscorer

    suspend fun topscorer(land: String, liga: String, statistik: String, position: String, saison: String, art: String): TopscorerDaten =
        withContext(Dispatchers.IO) {
            val query = buildList {
                add("landauswahl=${land.ifBlank { "0" }}")
                add("ligaauswahl=${liga.ifBlank { "0" }}")
                add("statistik=${statistik.ifBlank { "1" }}")
                add("pos=${position.ifBlank { "0" }}")
                add("saison=${saison.ifBlank { "0" }}")
                add("art=${art.ifBlank { "0" }}")
            }.joinToString("&")
            val html = safeGet("${OsApi.BASE_URL}/topscorer.php?$query") ?: return@withContext TopscorerDaten()
            parseTopscorer(html)
        }

    internal fun parseTopscorer(html: String): TopscorerDaten {
        val doc = Jsoup.parse(html)
        val tabelle = resultTabelle(doc, listOf("Verein", "Wert"))
        val zeilen = tabelle?.select("tr")?.mapNotNull { tr ->
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
                pid = spielerLink?.attr("onClick")
                    ?.let { Regex("spielerinfo\\((\\d+)\\)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                name = name,
                position = spielerLink?.parent()?.className()?.trim()?.ifEmpty { null },
                alter = zellen[2].text().trim(),
                skill = zellen[3].text().trim(),
                opti = zellen[4].text().trim(),
                land = zellen[5].selectFirst("img[src*='flaggen/']")?.attr("src")
                    ?.let { Regex("flaggen/(\\w+)\\.").find(it)?.groupValues?.get(1) }.orEmpty(),
                teamId = teamLink?.attr("onClick")
                    ?.let { Regex("teaminfo\\((\\d+)\\)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                team = teamLink?.text()?.trim() ?: teamZelle.text().trim(),
                liga = zellen[7].text().trim(),
                wert = zellen[8].text().trim(),
            )
        }.orEmpty()
        return TopscorerDaten(
            laender = optionen(doc, "landauswahl"),
            ligas = optionen(doc, "ligaauswahl"),
            statistiken = optionen(doc, "statistik"),
            positionen = optionen(doc, "pos"),
            saisons = optionen(doc, "saison"),
            arten = optionen(doc, "art"),
            zeilen = zeilen,
        )
    }

    // -------------------------------------------------------------- Spielersuche

    /** Lädt die Formularoptionen der Spielersuche (Länder/Ligen/Attribute). */
    suspend fun spielersucheOptionen(): SpielersucheOptionen = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/osneu/spielersuche") ?: return@withContext SpielersucheOptionen()
        parseSpielersucheOptionen(html)
    }

    internal fun parseSpielersucheOptionen(html: String): SpielersucheOptionen {
        val doc = Jsoup.parse(html)
        val meta = attributMeta(html)
        val attribute = attributOptionen(html).mapNotNull { (id, name) ->
            val m = meta[id]
            SucheAttribut(
                id = id,
                name = name,
                typ = m?.getString("typ") ?: "zahl",
                werte = m?.optJSONObject("werte")?.let { w ->
                    val keys = w.keys()
                    val list = mutableListOf<LaenderOption>()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        list += LaenderOption(k, w.getString(k))
                    }
                    list
                } ?: emptyList(),
            )
        }
        return SpielersucheOptionen(
            laender = optionenById(doc, "land"),
            ligas = optionenById(doc, "liga"),
            anzeigen = optionenById(doc, "anzeige"),
            nationen = optionenById(doc, "nation"),
            attribute = attribute,
        )
    }

    /** Führt die Spielersuche aus (`action=suchen`, JSON-POST). */
    suspend fun spielerSuchen(basis: SucheBasis, kriterien: List<SucheKriterium>): SpielersucheErgebnis =
        withContext(Dispatchers.IO) {
            val json = JSONObject()
                .put("landId", basis.landId)
                .put("ligaId", basis.ligaId)
                .put("anzeigeId", basis.anzeigeId)
                .put("nationId", basis.nationId)
                .put("kriterien", kriterienJson(kriterien))
            val antwort = postJson("${OsApi.BASE_URL}/osneu/spielersuche?action=suchen", json.toString())
                ?: return@withContext SpielersucheErgebnis()
            runCatching {
                val obj = JSONObject(antwort)
                val anzahl = obj.optInt("anzahl", 0)
                val htmlSnippet = obj.optString("html", "")
                SpielersucheErgebnis(anzahl = anzahl, zeilen = parseSucheTreffer(htmlSnippet))
            }.getOrElse { SpielersucheErgebnis() }
        }

    internal fun parseSucheTreffer(html: String): List<SucheSpielerZeile> {
        val doc = Jsoup.parse(html)
        return doc.select("tr").mapNotNull { tr ->
            val zellen = tr.select("td")
            if (zellen.size < 6) return@mapNotNull null
            val nameZelle = zellen[1]
            val spielerLink = nameZelle.selectFirst("a[href*='sp.php']")
            val teamLink = zellen[5].selectFirst("a[href*='st.php']")
            val name = spielerLink?.text()?.trim() ?: nameZelle.text().trim()
            if (name.isBlank()) return@mapNotNull null
            SucheSpielerZeile(
                nr = zellen[0].text().trim().toIntOrNull(),
                pid = spielerLink?.attr("href")?.let { Regex("s=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                name = name,
                position = nameZelle.selectFirst("a")?.parent()?.className()?.trim()?.ifEmpty { null },
                alter = zellen[2].text().trim(),
                nation = flagCode(zellen[4]),
                teamId = teamLink?.attr("href")?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                team = teamLink?.text()?.trim().orEmpty(),
            )
        }
    }

    /** Lädt die Liste der gespeicherten Abfragen (`action=abfragen`). */
    suspend fun abfragen(): List<GespeicherteAbfrage> = withContext(Dispatchers.IO) {
        val json = safeGet("${OsApi.BASE_URL}/osneu/spielersuche?action=abfragen") ?: return@withContext emptyList()
        runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(GespeicherteAbfrage(o.optInt("sps_id", 0), o.optString("sps_name", "")))
                }
            }
        }.getOrElse { emptyList() }
    }

    /** Lädt die Details einer gespeicherten Abfrage (`action=abfrage&id=…`). */
    suspend fun abfrage(id: Int): AbfrageDetails? = withContext(Dispatchers.IO) {
        val json = safeGet("${OsApi.BASE_URL}/osneu/spielersuche?action=abfrage&id=$id") ?: return@withContext null
        runCatching {
            val o = JSONObject(json)
            val kriterien = mutableListOf<SucheKriterium>()
            o.optJSONArray("kriterien")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val k = arr.getJSONObject(i)
                    kriterien += SucheKriterium(
                        attributId = k.optInt("attributId", 0),
                        opVon = k.optString("opVon", ""),
                        valVon = k.optString("valVon", ""),
                        opBis = k.optString("opBis", ""),
                        valBis = k.optString("valBis", ""),
                        sort = k.optString("sort", ""),
                    )
                }
            }
            AbfrageDetails(
                name = o.optString("name", ""),
                basis = SucheBasis(
                    landId = o.optInt("landId", 0),
                    ligaId = o.optInt("ligaId", 0),
                    anzeigeId = o.optInt("anzeigeId", 1),
                    nationId = o.optInt("nationId", 0),
                ),
                kriterien = kriterien,
            )
        }.getOrNull()
    }

    /**
     * Speichert (oder überschreibt) eine Suchabfrage (`action=speichern`).
     * Liefert die neue/existierende Abfrage-ID oder `null` bei einem Fehler.
     */
    suspend fun abfrageSpeichern(basis: SucheBasis, kriterien: List<SucheKriterium>, name: String, id: Int): Int? =
        withContext(Dispatchers.IO) {
            val json = JSONObject()
                .put("landId", basis.landId)
                .put("ligaId", basis.ligaId)
                .put("anzeigeId", basis.anzeigeId)
                .put("nationId", basis.nationId)
                .put("name", name)
                .put("id", id)
                .put("kriterien", kriterienJson(kriterien))
            val antwort = postJson("${OsApi.BASE_URL}/osneu/spielersuche?action=speichern", json.toString())
                ?: return@withContext null
            runCatching {
                val o = JSONObject(antwort)
                if (o.optString("status") == "fail") null else o.optInt("id", 0).takeIf { it > 0 }
            }.getOrNull()
        }

    /** Löscht eine gespeicherte Abfrage (`action=loeschen`). */
    suspend fun abfrageLoeschen(id: Int): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().put("id", id)
        postJson("${OsApi.BASE_URL}/osneu/spielersuche?action=loeschen", json.toString()) != null
    }

    // ----------------------------------------------------------- Spielervergleich

    /** Namenssuche des Spielervergleichs (`osneu/ajax/findSpieler`). */
    suspend fun findSpieler(term: String): List<SpielerVorschlag> = withContext(Dispatchers.IO) {
        if (term.isBlank()) return@withContext emptyList()
        val json = safeGet(
            "${OsApi.BASE_URL}/osneu/ajax/findSpieler?term=${java.net.URLEncoder.encode(term, "UTF-8")}",
        ) ?: return@withContext emptyList()
        runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        SpielerVorschlag(
                            id = o.optLong("id", 0L),
                            name = o.optString("name", ""),
                            zusatz = o.optString("zusatz", ""),
                        ),
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    /** Vergleicht zwei Spieler (`action=getSpieler`, `ansicht` 0 = normal, 1 = Differenz). */
    suspend fun vergleiche(id1: Long, id2: Long, ansicht: Int = 0): Spielervergleich? =
        withContext(Dispatchers.IO) {
            val url = "${OsApi.BASE_URL}/osneu/spielervergleich?action=getSpieler&ansicht=$ansicht&id1=$id1&id2=$id2"
            val html = safeGet(url) ?: return@withContext null
            parseVergleich(html)
        }

    internal fun parseVergleich(html: String): Spielervergleich {
        val doc = Jsoup.parse(html)
        var name1: String? = null
        var pid1: Long? = null
        var name2: String? = null
        var pid2: Long? = null
        var team1Id: Long? = null
        var team1: String? = null
        var team2Id: Long? = null
        var team2: String? = null
        val zeilen = mutableListOf<VergleichZeile>()

        doc.select("tr").forEach { tr ->
            val zellen = tr.select("td")

            // Team-Spalten (Wappen + Link) kommen nur in den ersten Zeilen mit rowspan vor.
            tr.select("td img[src*='wappen/']").eachAttr("src").firstOrNull()?.let { src ->
                val id = Regex("wappen/(\\d+)\\.").find(src)?.groupValues?.get(1)?.toLongOrNull()
                if (id != null) {
                    val link = tr.selectFirst("a[href*='st.php']")
                    val teamName = link?.text()?.trim()
                    if (team1Id == null && team1 == null) {
                        team1Id = id; team1 = teamName
                    } else if (team2Id == null && team2 == null) {
                        team2Id = id; team2 = teamName
                    }
                }
            }

            // Kopfzeile mit den beiden Spieler-Namen.
            val namenImKopf = tr.selectFirst("td a[href*='sp.php']")
            if (namenImKopf != null && zellen.size >= 4) {
                val links = tr.select("td a[href*='sp.php']")
                links.getOrNull(0)?.let {
                    pid1 = Regex("s=(\\d+)").find(it.attr("href"))?.groupValues?.get(1)?.toLongOrNull()
                    name1 = it.text().trim()
                }
                links.getOrNull(1)?.let {
                    pid2 = Regex("s=(\\d+)").find(it.attr("href"))?.groupValues?.get(1)?.toLongOrNull()
                    name2 = it.text().trim()
                }
                return@forEach
            }

            val label = zellen.firstOrNull()?.text()?.trim()?.replace("\u00a0", " ")?.trim().orEmpty()
            val labelRechts = zellen.lastOrNull()?.text()?.trim()?.replace("\u00a0", " ")?.trim().orEmpty()
            if (zellen.size >= 4 && label.isNotEmpty() && label == labelRechts && zellen[2].text().isNotBlank()) {
                val w1 = zellen[2].text().trim()
                val w2 = zellen[3].text().trim()
                zeilen += VergleichZeile(
                    label = label,
                    wert1 = w1,
                    wert2 = w2,
                    besser = vergleichsGewinner(w1, w2),
                )
            } else if (zellen.size == 1 && label.isNotEmpty()) {
                // Abschnitts-Überschrift („Saison", „Gesamt").
                zeilen += VergleichZeile(label = label)
            }
        }
        return Spielervergleich(
            name1 = name1, pid1 = pid1, name2 = name2, pid2 = pid2,
            team1Id = team1Id, team1 = team1, team2Id = team2Id, team2 = team2,
            zeilen = zeilen,
        )
    }

    // --------------------------------------------------------- Spielstatistiken

    suspend fun spielstatistiken(): Spielstatistiken = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/osneu/statistics") ?: return@withContext Spielstatistiken()
        parseSpielstatistiken(html)
    }

    internal fun parseSpielstatistiken(html: String): Spielstatistiken {
        val doc = Jsoup.parse(html)
        val abschnitte = mutableListOf<StatistikAbschnitt>()
        var titel: String? = null
        var untertitel: String? = null
        val zeilen = mutableListOf<KennzahlZeile>()

        fun flush() {
            if (titel != null && zeilen.isNotEmpty()) {
                abschnitte += StatistikAbschnitt(titel = titel!!, untertitel = untertitel, zeilen = zeilen.toList())
            }
            zeilen.clear()
        }

        doc.select("table tr").forEach { tr ->
            val h2 = tr.selectFirst("td h2")?.text()?.trim()
            val h3 = tr.selectFirst("td h3")?.text()?.trim()
            when {
                h2 != null -> {
                    flush(); titel = h2; untertitel = null
                }
                h3 != null -> {
                    flush(); untertitel = h3
                }
                else -> {
                    val zellen = tr.select("td").map { it.text().trim() }
                    if (zellen.size >= 2 && zellen[0].replace("\u00a0", "").isNotEmpty()) {
                        zeilen += KennzahlZeile(zellen[0], zellen.getOrElse(1) { "" })
                    }
                }
            }
        }
        flush()

        val hinweise = doc.select("p, li").mapNotNull { it.text().trim().takeIf { t -> t.isNotEmpty() } }
            .filter {
                it.startsWith("Diese Statistiken") || it.startsWith("Die Statistiken") ||
                    it.startsWith("Spielergebnisse") || it.startsWith("Wir behalten")
            }
        return Spielstatistiken(hinweise = hinweise, abschnitte = abschnitte)
    }

    // ------------------------------------------------------- Tabellenstatistiken

    suspend fun tabellenstatistiken(): Tabellenstatistiken = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/osneu/userstatistics") ?: return@withContext Tabellenstatistiken()
        parseTabellenstatistiken(html)
    }

    internal fun parseTabellenstatistiken(html: String): Tabellenstatistiken {
        val doc = Jsoup.parse(html)
        val abschnitte = mutableListOf<RekordAbschnitt>()
        var titel = ""
        var untertitel = ""
        var line = StringBuilder()
        var teamId: Long? = null

        fun abschnitt() = abschnitte.lastOrNull()?.takeIf { it.titel == titel && it.untertitel == untertitel }

        fun flushLine() {
            val text = line.toString().trim()
            if (text.isNotEmpty()) {
                val letzte = abschnitt()
                if (letzte != null) {
                    abschnitte[abschnitte.lastIndex] = letzte.copy(zeilen = letzte.zeilen + RekordZeile(text, teamId))
                } else {
                    abschnitte += RekordAbschnitt(titel, untertitel, mutableListOf(RekordZeile(text, teamId)))
                }
            }
            line = StringBuilder()
            teamId = null
        }

        fun walk(node: Node) {
            when (node) {
                is TextNode -> line.append(node.text())
                is Element -> when (node.tagName()) {
                    "h1", "h2", "h3" -> {
                        flushLine()
                        if (node.tagName() == "h3") untertitel = node.text().trim()
                        else { titel = node.text().trim(); untertitel = "" }
                    }
                    "br" -> flushLine()
                    "a" -> {
                        if (teamId == null && node.attr("href").contains("st.php")) {
                            teamId = Regex("c=(\\d+)").find(node.attr("href"))
                                ?.groupValues?.get(1)?.toLongOrNull()
                        }
                        line.append(node.text())
                    }
                    else -> node.childNodes().forEach { walk(it) }
                }
                else -> node.childNodes().forEach { walk(it) }
            }
        }

        doc.body().childNodes().forEach { walk(it) }
        flushLine()
        return Tabellenstatistiken(abschnitte = abschnitte)
    }

    // ------------------------------------------------------------------ Helfer

    /** Die Ergebnistabelle einer Suchseite (Kopfzeile enthält alle gesuchten Spalten). */
    private fun resultTabelle(doc: Document, spalten: List<String>): Element? =
        doc.select("table").firstOrNull { t ->
            val kopf = t.select("tr").firstOrNull()?.select("th,td")?.map { it.text().trim() }.orEmpty()
            spalten.all { s -> kopf.any { it.contains(s, ignoreCase = true) } }
        }

    /** Optionen eines `select[name=…]`; der Platzhalter (Wert 0/leer) wird als „Alle" gelistet. */
    private fun optionen(doc: Document, name: String): List<LaenderOption> =
        doc.select("select[name=$name] option").mapNotNull { opt ->
            val id = opt.attr("value")
            val label = opt.text().trim()
            if (id.isBlank() || label.isBlank()) null
            else if (id == "0") LaenderOption("0", "Alle")
            else LaenderOption(id, label)
        }

    /** Optionen eines `select#id=…` (die Spielersuche nutzt `id` statt `name`). */
    private fun optionenById(doc: Document, id: String): List<LaenderOption> =
        doc.select("select#$id option").mapNotNull { opt ->
            val wert = opt.attr("value")
            val label = opt.text().trim()
            if (wert.isBlank() || label.isBlank()) null
            else if (wert == "0") LaenderOption("0", "Alle")
            else LaenderOption(wert, label)
        }

    /** Attributliste der Spielersuche aus dem `<template id="kriteriumVorlage">`. */
    private fun attributOptionen(html: String): List<Pair<Int, String>> {
        val vorlage = Regex("<template\\s+id=\"kriteriumVorlage\">(.*?)</template>", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1) ?: return emptyList()
        return Regex("<option\\s+value=\"(\\d+)\">(.*?)</option>", RegexOption.DOT_MATCHES_ALL)
            .findAll(vorlage)
            .mapNotNull { m ->
                val id = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                id to m.groupValues[2].trim()
            }.toList()
    }

    /** Ländercode aus der Flaggen-Zelle (`images/flaggen/GER.gif` → „GER"). */
    private fun flagCode(zelle: Element): String =
        zelle.selectFirst("img[src*='flaggen/']")?.attr("src")
            ?.let { Regex("flaggen/(\\w+)\\.").find(it)?.groupValues?.get(1) }.orEmpty()

    /** `attributMeta`-JSON der Spielersuche aus dem eingebetteten Script (Attribut-ID → Meta). */
    private fun attributMeta(html: String): Map<Int, JSONObject> {
        val start = html.indexOf("attributMeta") ?: return emptyMap()
        val eq = html.indexOf('{', start) ?: return emptyMap()
        var depth = 0
        var end = -1
        for (i in eq until html.length) {
            when (html[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) { end = i; break }
                }
            }
        }
        if (end <= eq) return emptyMap()
        return runCatching {
            val obj = JSONObject(html.substring(eq, end + 1))
            obj.keys().asSequence().mapNotNull { k ->
                val id = k.toIntOrNull() ?: return@mapNotNull null
                id to obj.getJSONObject(k)
            }.toMap()
        }.getOrElse { emptyMap() }
    }

    private fun kriterienJson(kriterien: List<SucheKriterium>): JSONArray {
        val arr = JSONArray()
        kriterien.forEach { k ->
            arr.put(
                JSONObject()
                    .put("attributId", k.attributId)
                    .put("opVon", k.opVon)
                    .put("valVon", k.valVon)
                    .put("opBis", k.opBis)
                    .put("valBis", k.valBis)
                    .put("sort", k.sort),
            )
        }
        return arr
    }

    /** Bestimmt den besser platzierten Spieler zweier Zahlen-/Werte-Spalten. */
    private fun vergleichsGewinner(links: String, rechts: String): Int {
        val l = links.replace('.', ',').replace(',', '.').toDoubleOrNull() ?: return 0
        val r = rechts.replace('.', ',').replace(',', '.').toDoubleOrNull() ?: return 0
        return when {
            l > r -> 1
            r > l -> 2
            else -> 0
        }
    }

    private fun safeGet(url: String): String? = try {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            val bytes = response.body?.bytes() ?: return null
            if (SessionGuard.isLoginView(bytes)) null else HtmlTools.serverText(bytes)
        }
    } catch (e: IOException) {
        null
    }

    private fun postJson(url: String, json: String): String? = try {
        val body: RequestBody = json.toRequestBody(JSON_MEDIA_TYPE)
        client.newCall(Request.Builder().url(url).post(body).build()).execute().use { response ->
            val bytes = response.body?.bytes() ?: return null
            if (SessionGuard.isLoginView(bytes)) null else HtmlTools.serverText(bytes)
        }
    } catch (e: IOException) {
        null
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}