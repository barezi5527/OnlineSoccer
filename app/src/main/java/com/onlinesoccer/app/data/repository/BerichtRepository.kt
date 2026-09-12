package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.BerichtEreignis
import com.onlinesoccer.app.data.model.BerichtEreignisTyp
import com.onlinesoccer.app.data.model.BerichtAufstellung
import com.onlinesoccer.app.data.model.BerichtEinstellungen
import com.onlinesoccer.app.data.model.BerichtSpieler
import com.onlinesoccer.app.data.model.BerichtStatistik
import com.onlinesoccer.app.data.model.SpielBericht
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * Spielberichte. Bevorzugte Quelle ist der statische Bericht
 * `rep/saison/<s>/<zat>/<heim>-<gast>.html`; alternativ die
 * session-geschützte Berichtsseite `bericht.php?s=<sid>`.
 */
@Singleton
class BerichtRepository @Inject constructor(
    private val client: OkHttpClient,
) {
    private val torRegex = Regex("TOR\\s+Neuer Spielstand:\\s*(\\d+):(\\d+)\\s*\\(([^)]+)\\)")

    suspend fun ladeBericht(sid: String?, url: String? = null): SpielBericht? = withContext(Dispatchers.IO) {
        val basis = OsApi.BASE_URL
        val staticUrl = url?.takeIf { it.startsWith("http") }
        val wrapper = sid?.takeIf { it.isNotBlank() }?.let { safeGet("$basis/bericht.php?s=$it") }

        val repUrl = wrapper?.let { html ->
            val (heim, gast, zat, saison) = Regex("os_bericht\\((\\d+),(\\d+),(\\d+),(\\d+)\\)")
                .find(html)?.destructured ?: return@let null
            "$basis/rep/saison/$saison/$zat/$heim-$gast.html"
        }

        val kandidaten = buildList {
            staticUrl?.let { add(it) }
            repUrl?.let { add(it) }
            wrapper?.let { add("${basis}/bericht.php?s=$sid") }
        }.distinct()

        var gewaehlt: Pair<String, String>? = null
        for (c in kandidaten) {
            val html = safeGet(c) ?: continue
            if (istVollbericht(html)) {
                gewaehlt = c to html
                break
            }
        }
        val ausgewaehlt = gewaehlt ?: return@withContext null
        parseBericht(ausgewaehlt.second, url = ausgewaehlt.first, sid = sid)
    }

    private fun istVollbericht(html: String): Boolean =
        html.length > 1500 &&
            html.contains("<h2", ignoreCase = true) &&
            html.contains("Es folgt der Spielbericht", ignoreCase = true) &&
            html.contains("Es folgen die Spielstatistiken", ignoreCase = true)

    internal fun parseBericht(html: String, url: String, sid: String?): SpielBericht {
        val doc = Jsoup.parse(html)
        val bodyText = doc.body()?.text().orEmpty()

        var saison = 0
        var zat = 0
        var heimId: Long? = null
        var gastId: Long? = null
        Regex("os_bericht\\((\\d+),(\\d+),(\\d+),(\\d+)\\)").find(html)?.destructured?.let { (h, g, z, s) ->
            heimId = h.toLongOrNull()
            gastId = g.toLongOrNull()
            zat = z.toIntOrNull() ?: 0
            saison = s.toIntOrNull() ?: 0
        }

        // Der statische Bericht führt die Vereinsnamen in der h2-Überschrift.
        var heim: String? = null
        var gast: String? = null
        doc.selectFirst("h2")?.text()?.split(" - ", limit = 2)?.let { teams ->
            if (teams.size == 2) {
                heim = teams[0].trim().takeIf { it.isNotBlank() }
                gast = teams[1].trim().takeIf { it.isNotBlank() }
            }
        }

        val kopftext = doc.selectFirst("h2")?.nextElementSibling()?.text().orEmpty()
        val kopfwerte = Regex(
            "Datum\\s*:\\s*(.*?)\\s+Stadion\\s*:\\s*(.*?)\\s+Spielart\\s*:\\s*(.*?)\\s+Zuschaueranzahl\\s*:\\s*(.*)",
            RegexOption.IGNORE_CASE,
        ).find(kopftext)

        // Ereignisse zwischen "Es folgt der Spielbericht" und "Es folgen die Spielstatistiken"
        val tickerStart = bodyText.indexOf("Es folgt der Spielbericht")
        val tickerEnd = bodyText.indexOf("Es folgen die Spielstatistiken")
        val ticker = if (tickerStart >= 0 && tickerEnd > tickerStart) {
            bodyText.substring(tickerStart, tickerEnd)
        } else {
            bodyText
        }

        val segmente = regexSplit(ticker)
        val ereignisse = mutableListOf<BerichtEreignis>()
        segmente.forEach { segment ->
            val minuteMatch = Regex("^(\\d{1,3})\\s*\\.\\s*").find(segment)
            val minute = minuteMatch?.groupValues?.get(1)
            val text = (minuteMatch?.let { segment.removeRange(it.range) } ?: segment).trim()
            if (minute != null && text.isNotBlank() && text.length < 700) {
                ereignisse += BerichtEreignis(minute = minute, text = text, typ = ereignisTyp(segment))
            }
        }

        // Endstand aus den Statistiken, sonst letzter Spielstand aus dem Ticker
        val endstand = Regex("Endstand\\s*(\\d+)\\s+(\\d+)").find(bodyText)?.let {
            it.groupValues[1] + ":" + it.groupValues[2]
        } ?: torRegex.findAll(ticker).lastOrNull()?.let {
            it.groupValues[1] + ":" + it.groupValues[2]
        }

        return SpielBericht(
            saison = saison,
            zat = zat,
            heim = heim,
            gast = gast,
            heimId = heimId,
            gastId = gastId,
            ergebnis = endstand,
            datum = kopfwerte?.groupValues?.getOrNull(1)?.trim(),
            stadion = kopfwerte?.groupValues?.getOrNull(2)?.trim(),
            spielart = kopfwerte?.groupValues?.getOrNull(3)?.trim(),
            zuschauer = kopfwerte?.groupValues?.getOrNull(4)?.trim(),
            heimAufstellung = parseAufstellung(doc, "H", parseEinstellungen(doc, 0)),
            gastAufstellung = parseAufstellung(doc, "G", parseEinstellungen(doc, 1)),
            ereignisse = ereignisse.distinctBy { it.minute to it.text }.take(200),
            statistik = parseStatistik(doc),
            rohtext = bodyText,
            url = url,
        )
    }

    private fun ereignisTyp(text: String): BerichtEreignisTyp = when {
        torRegex.containsMatchIn(text) || Regex("\\btor\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) -> BerichtEreignisTyp.TOR
        Regex("gelb[- ]rote?\\s+karte|zweite gelbe|rote?\\s+karte|platzverweis|des feldes verwiesen", RegexOption.IGNORE_CASE).containsMatchIn(text) -> BerichtEreignisTyp.ROTE_KARTE
        Regex("gelbe?\\s+karte", RegexOption.IGNORE_CASE).containsMatchIn(text) -> BerichtEreignisTyp.GELBE_KARTE
        Regex("verletz", RegexOption.IGNORE_CASE).containsMatchIn(text) -> BerichtEreignisTyp.VERLETZUNG
        Regex("elfmeter|11[- ]?meter|strafstoß|penalty", RegexOption.IGNORE_CASE).containsMatchIn(text) -> BerichtEreignisTyp.ELFMETER
        else -> BerichtEreignisTyp.SONSTIGES
    }

    private fun parseAufstellung(doc: org.jsoup.nodes.Document, teamClass: String, einstellungen: BerichtEinstellungen): BerichtAufstellung? {
        val raster = doc.select("table").firstOrNull { table ->
            table.select("td[bgcolor]").any { cell ->
                cell.attr("bgcolor").lowercase() in setOf("#cc0033", "#3377ff", "#009933", "#ffff00")
            }
        }
        val slotPositionen = mutableMapOf<String, String>()
        val slotKoordinaten = mutableMapOf<String, Pair<Int, Int>>()
        raster?.select("tr")?.forEach { row ->
            val cells = row.select("td")
            val zeilenlabel = row.selectFirst("td")?.text()?.trim().orEmpty()
            val feldzeile = "ONMLKJIHGFEDCBA".indexOf(zeilenlabel)
            if (feldzeile < 0) {
                val torwartZellen = cells.filter { it.attr("bgcolor").equals("#ffff00", ignoreCase = true) }
                val torwartZelle = if (teamClass == "H") torwartZellen.firstOrNull() else torwartZellen.lastOrNull()
                if (torwartZelle != null) {
                    slotKoordinaten["T"] = 15 to 5
                    slotPositionen["T"] = "Torwart"
                }
                return@forEach
            }
            val rasterlabels = cells.mapIndexedNotNull { index, cell ->
                index.takeIf {
                    cell.text().trim() == zeilenlabel && cell.attr("bgcolor").isBlank()
                }
            }
            // The same table contains the player list below the grid. Those
            // rows start with A-L/T as well, but are not full 15x11 grid rows.
            // A real matrix row has the two panels plus their border labels.
            if (cells.size < 20 || rasterlabels.size < 2) return@forEach
            val rasterstart = rasterlabels.getOrNull(if (teamClass == "H") 0 else 1)?.plus(1)
                ?: return@forEach
            cells.drop(rasterstart).take(11).forEachIndexed { spalte, cell ->
                val slot = cell.text().trim().takeIf { it.matches(Regex("[A-Z]")) }
                if (slot != null) {
                    slotKoordinaten[slot] = feldzeile to spalte
                    val rolle = when (cell.attr("bgcolor").lowercase()) {
                        "#cc0033" -> "Sturm"
                        "#3377ff" -> "Mittelfeld"
                        "#009933" -> "Abwehr"
                        "#ffff00" -> "Torwart"
                        else -> null
                    }
                    rolle?.let { slotPositionen[slot] = it }
                }
            }
        }
        if (raster?.select("td[bgcolor]")?.any { it.attr("bgcolor").equals("#ffff00", ignoreCase = true) } == true) {
            // The report renders the goalkeeper in a colspan row below the A row.
            slotKoordinaten["T"] = 15 to 5
            slotPositionen["T"] = "Torwart"
        }
        val spieler = doc.select("tr").mapNotNull { row ->
            val markers = row.select("b.$teamClass")
            if (markers.size < 2) return@mapNotNull null
            val slot = markers.firstOrNull()?.text()?.trim()?.takeIf { it.matches(Regex("[A-Z]")) }
                ?: return@mapNotNull null
            val name = markers.drop(1).joinToString(" ") { it.text().trim() }.trim()
                .takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val koordinaten = slotKoordinaten[slot]
            BerichtSpieler(
                name = name,
                nummer = slot,
                position = slotPositionen[slot],
                feldzeile = koordinaten?.first,
                feldspalte = koordinaten?.second,
            )
        }
        if (spieler.isEmpty()) return null

        // The website exposes positions through the matrix, not as a separate
        // formation label. Do not derive or invent a formation string here.
        return BerichtAufstellung(formation = null, spieler = spieler, einstellungen = einstellungen)
    }

    private fun parseEinstellungen(doc: org.jsoup.nodes.Document, valueIndex: Int): BerichtEinstellungen {
        val table = doc.select("table").firstOrNull { table ->
            table.select("tr").any { it.select("td").firstOrNull()?.text()?.trim() == "Einsatz" }
        } ?: return BerichtEinstellungen()

        fun wert(label: String): String? {
            val row = table.select("tr").firstOrNull { it.select("td").firstOrNull()?.text()?.trim() == label }
                ?: return null
            return row.select("td").drop(1).map { it.text().trim() }.filter { it.isNotBlank() }
                .getOrNull(valueIndex)
        }
        return BerichtEinstellungen(
            einsatz = wert("Einsatz"),
            haerte = wert("Härte"),
            spielweise = wert("Spielweise"),
            taktikSturm = wert("Taktik - Sturm"),
            taktikMittelfeld = wert("Taktik - Mittelfeld"),
            taktikAbwehr = wert("Taktik - Abwehr"),
        )
    }

    private fun regexSplit(ticker: String): List<String> {
        val bereiche = Regex("\\d{1,3}\\s*\\.").findAll(ticker).map { it.range }.toList()
        if (bereiche.isEmpty()) return listOf(ticker)
        val ergebnis = mutableListOf<String>()
        bereiche.forEachIndexed { i, range ->
            val ab = range.first
            val bis = if (i + 1 < bereiche.size) bereiche[i + 1].first else ticker.length
            ergebnis += ticker.substring(ab, bis)
        }
        return ergebnis
    }

    private fun parseStatistik(doc: org.jsoup.nodes.Document): BerichtStatistik? {
        val tabelle = doc.select("table").firstOrNull { table ->
            table.select("tr").any { it.select("td").firstOrNull()?.text()?.trim() == "Endstand" }
        } ?: return null

        fun wert(label: String): String? {
            val row = tabelle.select("tr").firstOrNull {
                it.select("td").firstOrNull()?.text()?.trim() == label
            } ?: return null
            val values = row.select("td").drop(1).map { it.text().trim() }.filter { it.isNotEmpty() }
            return values.takeIf { it.isNotEmpty() }?.joinToString(" : ")
        }

        return BerichtStatistik(
            abseits = wert("Abseits"),
            ecken = wert("Eckenverhältnis"),
            fouls = wert("Fouls"),
            elfmeter = wert("Elfmeter"),
            ballbesitz = wert("Ballbesitz"),
            schnittSkill = wert("Schnitt Skill"),
            schnittOpti = wert("Schnitt Opt.Skill"),
            fitness = wert("Fitness"),
            moral = wert("Moral"),
        )
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
