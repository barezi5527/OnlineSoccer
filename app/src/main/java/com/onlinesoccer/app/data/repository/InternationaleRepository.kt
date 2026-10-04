package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.InternationaleAnsicht
import com.onlinesoccer.app.data.model.InternationaleFilter
import com.onlinesoccer.app.data.model.InternationaleOption
import com.onlinesoccer.app.data.model.InternationaleSpiel
import com.onlinesoccer.app.data.model.InternationaleTabelleKlasse
import com.onlinesoccer.app.data.model.InternationaleTeam
import com.onlinesoccer.app.data.model.InternationaleTeilnehmerGruppe
import com.onlinesoccer.app.core.network.HtmlTools
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

@Singleton
class InternationaleRepository @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun lade(path: String, filter: InternationaleFilter = InternationaleFilter()): InternationaleAnsicht =
        withContext(Dispatchers.IO) {
            val url = "${OsApi.BASE_URL}$path"
            val ersteAntwort = safeGet(url)
                ?: throw IOException("Internationale Bewerbe konnten nicht geladen werden.")
            val ersteAnsicht = parse(ersteAntwort)
            if (!filter.istLeer()) {
                return@withContext safePost(url, filter)?.let(::parse) ?: ersteAnsicht
            }
            if (!ersteAnsicht.hatFilter) return@withContext ersteAnsicht

            // Die Wettbewerbsseiten liefern die Spiele erst nach dem Formular-POST.
            val standardFilter = ersteAnsicht.filter.copy(
                ergebnisse = ersteAnsicht.filter.ergebnisse
                    ?.takeUnless { it == "0" }
                    ?: ersteAnsicht.ergebnisOptionen.firstOrNull { it.wert == "1" }?.wert,
            )
            safePost(url, standardFilter)?.let(::parse) ?: ersteAnsicht
        }

    internal fun parse(html: String): InternationaleAnsicht {
        val doc = Jsoup.parse(html)
        val filter = InternationaleFilter(
            saison = selectedValue(doc, "season"),
            runde = selectedValue(doc, "runde"),
            gruppe = selectedValue(doc, "gruppe"),
            ergebnisse = selectedValue(doc, "ergebnisse"),
        )
        val international = doc.selectFirst("#international")
        val spiele = international?.let(::parseSpiele).orEmpty()
        val teilnehmer = parseTeilnehmer(doc)

        val tabellen = doc.selectFirst("table.osranking")?.let(::parseOsRanking)
            ?.let(::listOf)
            ?: HtmlTools.tabellen(doc).map { tabelle ->
                com.onlinesoccer.app.data.model.WertTabelle(
                    header = tabelle.header,
                    zeilen = tabelle.zeilen.map { it.zellen },
                    teamIds = tabelle.zeilen.map { it.teamId },
                    zeilenKlasse = tabelle.zeilen.map {
                        InternationaleTabelleKlasse.vonCssKlasse(it.cssKlasse)
                    },
                )
            }

        return InternationaleAnsicht(
            titel = doc.select("h1,h2,h3").firstOrNull()?.text()?.trim(),
            saisonen = options(doc, "season"),
            runden = options(doc, "runde").filterNot { it.wert == RUNDE_TURNIERBAUM },
            gruppen = options(doc, "gruppe"),
            ergebnisOptionen = options(doc, "ergebnisse"),
            filter = filter,
            abschnitte = if (teilnehmer.isNotEmpty()) {
                emptyList()
            } else {
                doc.select("p, li").map { it.text().trim() }
                    .filter { it.isNotEmpty() && it.length < 300 && it != "Konferenz ansehen" }
                    .distinct()
                    .take(120)
            },
            tabellen = tabellen,
            spiele = spiele,
            teilnehmer = teilnehmer,
            zatOptionen = spiele.mapNotNull { it.zat }.distinct()
                .map { zat -> InternationaleOption(zat, zat) },
        )
    }

    /**
     * Ranglisten (`osranking` und `clubranking`, beide `table.osranking`).
     *
     * Die Kopfzeile nennt Platz, Land und die drei Saisons, aber nicht den Verein
     * und nicht die Summenspalte an ihrer Stelle – beides steht nur in den
     * Datenzeilen. clubranking hat zusaetzlich je Saison eine zweite Zelle mit
     * dem Hover-Aufschluss (`div.info-box`), osranking nicht. Deshalb werden die
     * Werte ueber feste Indizes geholt, die Spaltennamen aber aus der Kopfzeile
     * der Seite abgeleitet: „Saison 23" wird zu „S23" und bleibt damit auch in
     * der naechsten Saison korrekt.
     *
     * Angezeigt wird in der Reihenfolge `#`, Land, Club, Summe, Saisons.
     */
    private fun parseOsRanking(table: org.jsoup.nodes.Element): com.onlinesoccer.app.data.model.WertTabelle {
        val rows = table.select("tr")
        val daten = rows.filter { it.select("td").isNotEmpty() }
        val istClubRanking = daten.any { it.select("a[href*='st.php?c=']").isNotEmpty() }
        val saisonen = rows.firstOrNull { it.select("th").isNotEmpty() }
            ?.select("th")
            ?.map { it.text().trim() }
            ?.filter { it.startsWith("Saison ") }
            .orEmpty()
        // clubranking: Platz, Flagge, Club, dann je Saison Punkte und Hover-Info.
        // osranking: Platz, Flagge, Land, dann je Saison ein Wert.
        val saisonIndex = { stelle: Int -> if (istClubRanking) 3 + 2 * stelle else 3 + stelle }
        val header = buildList {
            add("#")
            add("Lnd")
            if (istClubRanking) add("Club")
            add("Sum")
            saisonen.forEach { add("S" + it.removePrefix("Saison").trim()) }
        }
        val zeilen = daten.map { row ->
            val zellen = row.select("td").map { it.text().trim() }
            buildList {
                add(zellen.getOrElse(0) { "" })
                if (istClubRanking) {
                    // Der Clubname steht in derselben Zelle wie der Link; beim Land
                    // nimmt der Parser den Alt-Text der Flagge, weil es keinen
                    // Klartext in der Zeile gibt.
                    add(landBezeichnung(row))
                    add(zellen.getOrElse(2) { "" })
                } else {
                    add(landBezeichnung(row).ifEmpty { zellen.getOrElse(2) { "" } })
                }
                add(summe3(zellen.lastOrNull().orEmpty()))
                saisonen.indices.forEach { stelle -> add(normalizeZahl(zellen.getOrElse(saisonIndex(stelle)) { "" })) }
            }
        }
        return com.onlinesoccer.app.data.model.WertTabelle(
            header = header,
            zeilen = zeilen,
            teamIds = if (istClubRanking) daten.map(::teamIdAusZeile) else emptyList(),
            zeilenKlasse = daten.map {
                InternationaleTabelleKlasse.vonCssKlasse(it.attr("class"))
            },
        )
    }

    /** OSC/OSE-Paarungen sind auf der Website als fortlaufende Span-Gruppen gerendert. */
    private fun parseSpiele(container: org.jsoup.nodes.Element): List<InternationaleSpiel> {
        val gruppen = mutableListOf<Paarungsgruppe>()
        var aktuelleGruppe = mutableListOf<org.jsoup.nodes.Element>()
        var aktuellesZat: String? = null
        container.children().forEach { child ->
            // `<span class="first all"><h2>ZAT 17</h2></span>` leitet einen Spieltag ein.
            // Wichtig ist die Reihenfolge: erst die laufende Paarung abschließen, dann
            // den neuen Spieltag setzen, sonst trägt die vorige Paarung den neuen ZAT.
            val neueUeberschrift = zatUeberschrift(child)
            if (child.hasClass("first") && aktuelleGruppe.isNotEmpty()) {
                gruppen += Paarungsgruppe(aktuelleGruppe, aktuellesZat)
                aktuelleGruppe = mutableListOf()
            }
            if (neueUeberschrift != null) aktuellesZat = neueUeberschrift
            aktuelleGruppe += child
        }
        if (aktuelleGruppe.isNotEmpty()) gruppen += Paarungsgruppe(aktuelleGruppe, aktuellesZat)

        val spiele = gruppen.flatMap { (elemente, zat) ->
            val teams = elemente.flatMap { it.select("a[href*='st.php'], a[onclick*='teaminfo']") }
                .take(2)
                .map { link ->
                    link.text().trim() to teamId(link)
                }
            if (teams.size < 2) return@flatMap emptyList()

            val berichte = elemente.flatMap {
                it.select("a[href*='rep/saison'], a[href*='os_bericht'], a[onclick*='os_bericht']")
            }
            val ergebnisse = elemente.map { it.text().trim() }
                .filter { Regex("^\\d+\\s*:\\s*\\d+$").matches(it) }
            if (berichte.isEmpty()) {
                return@flatMap listOf(
                    InternationaleSpiel(
                        heim = teams[0].first,
                        gast = teams[1].first,
                        ergebnis = ergebnisse.firstOrNull(),
                        heimTeamId = teams[0].second,
                        gastTeamId = teams[1].second,
                        zat = zat,
                    ),
                )
            }
            berichte.mapIndexed { index, link ->
                val berichtTeams = reportTeams(link)
                val paarung = berichtTeams?.let { (heimId, gastId) ->
                    when {
                        heimId == teams[0].second && gastId == teams[1].second -> teams[0] to teams[1]
                        heimId == teams[1].second && gastId == teams[0].second -> teams[1] to teams[0]
                        else -> null
                    }
                } ?: if (index % 2 == 0) teams[0] to teams[1] else teams[1] to teams[0]
                InternationaleSpiel(
                    heim = paarung.first.first,
                    gast = paarung.second.first,
                    ergebnis = ergebnisse.getOrNull(index),
                    berichtUrl = berichtUrl(link),
                    heimTeamId = paarung.first.second,
                    gastTeamId = paarung.second.second,
                    zat = zat,
                )
            }
        }
        if (spiele.isNotEmpty()) return spiele.distinctBy { it.berichtUrl ?: (it.heim to it.gast) }

        // Fallback für ältere/abweichende Markups, bei denen die Paarungen nicht
        // mit den Website-Klassen gruppiert sind.
        return container.select("a[href*='st.php']")
            .mapNotNull { it.parent()?.parent() }
            .mapNotNull { parent ->
                val teams = parent.select("a[href*='st.php']").take(2)
                if (teams.size < 2) return@mapNotNull null
                val berichtLink = parent.selectFirst(
                    "a[href*='rep/saison'], a[href*='os_bericht'], a[onclick*='os_bericht']",
                )
                InternationaleSpiel(
                    heim = teams[0].text().trim(),
                    gast = teams[1].text().trim(),
                    ergebnis = parent.select("span").map { it.text().trim() }
                        .firstOrNull { Regex("\\d+\\s*:\\s*\\d+").containsMatchIn(it) },
                    berichtUrl = berichtLink?.let(::berichtUrl),
                    heimTeamId = teamId(teams[0]),
                    gastTeamId = teamId(teams[1]),
                    zat = zatUeberschrift(parent),
                )
            }
            .distinctBy { it.heim to it.gast }
    }

    /** Spieltag-Überschrift der Website (`ZAT 17`), sonst `null`. */
    private fun zatUeberschrift(element: org.jsoup.nodes.Element): String? {
        val kandidat = element.selectFirst("h1, h2, h3")?.text()?.trim()
        return kandidat?.takeIf { ZAT_REGEX.matches(it) }
    }

    private data class Paarungsgruppe(
        val elemente: List<org.jsoup.nodes.Element>,
        val zat: String?,
    )

    private companion object {
        /** Spieltag-Überschrift der Gruppenphase, z. B. `ZAT 17`. */
        val ZAT_REGEX = Regex("ZAT\\s*\\d+", RegexOption.IGNORE_CASE)

        /**
         * Wert der Rundenoption „Tableau" (`runde=99`) auf den Finalrundenseiten.
         * Die Option blendet den Turnierbaum an: ein CSS-Grid aus
         * `div.tableau-runde`/`div.tableau-paarung` ohne `#international` und ohne
         * Tabelle. Die App hätte dafür keinen Inhalt und zeigte einen leeren
         * Bildschirm, deshalb bleibt die Option im Filter weg. Alle übrigen Runden
         * (Zwischenrunde bis Finale) stehen normal zur Auswahl.
         */
        const val RUNDE_TURNIERBAUM = "99"
    }

    private fun teamId(link: org.jsoup.nodes.Element): Long? =
        Regex("(?:[?&]c=|teaminfo\\()([0-9]+)")
            .find(link.attr("href") + " " + link.attr("onclick"))
            ?.groupValues?.get(1)?.toLongOrNull()

    /** Team-ID der Tabellenzeile (erster Vereinslink der Zeile). */
    private fun teamIdAusZeile(row: org.jsoup.nodes.Element): Long? =
        row.selectFirst("a[href*='st.php'], a[onclick*='teaminfo']")?.let { link ->
            Regex("(?:[?&]c=|teaminfo\\()([0-9]+)")
                .find(link.attr("href") + " " + link.attr("onclick"))
                ?.groupValues?.get(1)?.toLongOrNull()
        }

    private fun reportTeams(link: org.jsoup.nodes.Element): Pair<Long, Long>? {
        val hrefMatch = Regex("/rep/saison/\\d+/\\d+/(\\d+)-(\\d+)\\.html")
            .find(link.attr("href"))
        if (hrefMatch != null) {
            val heim = hrefMatch.groupValues[1].toLongOrNull()
            val gast = hrefMatch.groupValues[2].toLongOrNull()
            return if (heim != null && gast != null) heim to gast else null
        }
        val onClickMatch = Regex("os_bericht\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)")
            .find(link.attr("onclick")) ?: return null
        return onClickMatch.groupValues[1].toLongOrNull()?.let { heim ->
            onClickMatch.groupValues[2].toLongOrNull()?.let { gast -> heim to gast }
        }
    }

    private fun berichtUrl(link: org.jsoup.nodes.Element): String? {
        val href = link.attr("href")
        return when {
            href.contains("os_bericht") -> osBerichtUrl(href)
            href.contains("rep/saison") -> absoluteUrl(href)
            else -> osBerichtUrl(link.attr("onclick"))
        }
    }

    /**
     * Summe auf höchstens drei Nachkommastellen kürzen. Die Website schreibt deutsch,
     * der Punkt ist also Tausender- und Nachkommaptrennzeichen zugleich: aus
     * `14.1363333` (Punkte 14.136,3333) wird `14.136`. Kürzere Werte bleiben
     * unangetastet (`30.08`), damit Saison- und Summenspalte einer Zeile gleich
     * aussehen.
     */
    private fun normalizeZahl(text: String): String {
        val t = text.replace('\u00A0', ' ').trim()
        if (t.isEmpty()) return t
        val dot = t.lastIndexOf('.')
        if (dot >= 0) {
            if (t.length - dot - 1 > 3) {
                return t.substring(0, dot + 4)
            }
            val frac = t.substring(dot + 1).trimEnd('0')
            val intp = t.substring(0, dot)
            return if (frac.isEmpty()) intp else "$intp.$frac"
        }
        val comma = t.lastIndexOf(',')
        if (comma >= 0) {
            val frac = t.substring(comma + 1).trimEnd('0')
            val intp = t.substring(0, comma)
            return if (frac.isEmpty()) intp else "$intp.$frac"
        }
        return t
    }

    private fun summe3(text: String): String = normalizeZahl(text)

    /**
     * Teilnehmerliste (`intTeilnehmer`): `<h2>`-Wettbewerbsüberschrift gefolgt von
     * `<ul class="int_teilnehmer">` mit je `<li>` und drei `<div>`-Zeilen
     * (Verein + Trainer, Liga/Runde, Skill-/Optischnitt).
     */
    internal fun parseTeilnehmer(doc: org.jsoup.nodes.Document): List<InternationaleTeilnehmerGruppe> {
        val gruppen = mutableListOf<InternationaleTeilnehmerGruppe>()
        var bezeichnung: String? = null
        var teams = mutableListOf<InternationaleTeam>()

        fun abschliessen() {
            val name = bezeichnung
            if (name != null && teams.isNotEmpty()) {
                gruppen += InternationaleTeilnehmerGruppe(name, teams.toList())
            }
        }

        doc.select("h1, h2, ul.int_teilnehmer").forEach { element ->
            when {
                element.tagName() == "h2" -> {
                    abschliessen()
                    bezeichnung = element.text().trim().ifEmpty { bezeichnung }
                    teams = mutableListOf()
                }
                element.tagName() == "ul" -> element.select("li").forEach { li ->
                    val link = li.selectFirst("a[href*='st.php'], a[onclick*='teaminfo']")
                    val name = link?.text()?.trim().orEmpty()
                    if (name.isEmpty()) return@forEach
                    val zusatz = li.select("div").drop(1)
                        .map { it.text().trim() }
                        .filter { it.isNotEmpty() }
                    teams += InternationaleTeam(
                        name = name,
                        teamId = link?.let(::teamId),
                        zusatz = zusatz,
                    )
                }
            }
        }
        abschliessen()
        return gruppen
    }

    /** Länderkürzel der Zeile aus dem Dateinamen der Flagge (`.../HUN.gif`). */
    private fun landBezeichnung(row: org.jsoup.nodes.Element): String {
        val flagge = row.selectFirst("td img[src*='flaggen/']") ?: return ""
        return flagge.attr("src")
            .substringAfterLast('/')
            .substringBeforeLast('.')
            .uppercase()
            .ifEmpty { flagge.attr("alt").trim() }
    }

    private fun options(doc: org.jsoup.nodes.Document, name: String): List<InternationaleOption> =
        doc.select("select[name=$name] option").map {
            InternationaleOption(it.attr("value"), it.text().trim())
        }

    private fun selectedValue(doc: org.jsoup.nodes.Document, name: String): String? =
        doc.select("select[name=$name] option[selected]").firstOrNull()?.attr("value")

    private fun safeGet(url: String): String? = request(Request.Builder().url(url).get().build())

    private fun safePost(url: String, filter: InternationaleFilter): String? {
        val form = FormBody.Builder()
        filter.saison?.let { form.add("season", it) }
        filter.runde?.let { form.add("runde", it) }
        filter.gruppe?.let { form.add("gruppe", it) }
        filter.ergebnisse?.let { form.add("ergebnisse", it) }
        return request(Request.Builder().url(url).post(form.build()).build())
    }

    private fun request(request: Request): String? = try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            if (SessionGuard.isPureLoginView(bytes)) return null
            HtmlTools.serverText(bytes)
        }
    } catch (_: IOException) {
        null
    }

    private fun absoluteUrl(href: String): String = when {
        href.startsWith("http") -> href
        href.startsWith("/") -> "${OsApi.BASE_URL}$href"
        else -> "${OsApi.BASE_URL}/$href"
    }

    private fun osBerichtUrl(onClick: String): String? {
        val match = Regex("os_bericht\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)")
            .find(onClick) ?: return null
        val (heim, gast, zat, saison) = match.destructured
        return "${OsApi.BASE_URL}/rep/saison/$saison/$zat/$heim-$gast.html"
    }

    private fun InternationaleFilter.istLeer(): Boolean =
        saison == null && runde == null && gruppe == null && ergebnisse == null

    private val InternationaleAnsicht.hatFilter: Boolean
        get() = saisonen.isNotEmpty() || runden.isNotEmpty() || gruppen.isNotEmpty() || ergebnisOptionen.isNotEmpty()
}
