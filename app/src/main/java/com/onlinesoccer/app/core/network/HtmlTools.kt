package com.onlinesoccer.app.core.network

import com.onlinesoccer.app.core.ui.theme.positionVonText
import com.onlinesoccer.app.data.model.SeitenAnsicht
import com.onlinesoccer.app.data.model.SpielerPosition
import com.onlinesoccer.app.data.model.WertTabelle
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

/** Eine Tabellenzeile mit erkanntem Spieler-/Teamlink. */
data class TabellenZeile(
    val zellen: List<String>,
    val name: String?,
    val pid: Long? = null,
    val teamId: Long? = null,
    val sid: String? = null,
    val vorschau: String? = null,
    val berichtUrl: String? = null,
    /** Spielerposition aus den CSS-Klassen der Namenszelle (z. B. `TOR`, `STU`). */
    val position: SpielerPosition? = null,
)

data class GeparsteTabelle(
    val header: List<String>,
    val zeilen: List<TabellenZeile>,
) {
    fun spalte(label: String): Int =
        header.indexOfFirst { it.contains(label, ignoreCase = true) }

    fun wert(zeile: TabellenZeile, label: String): String? =
        spalte(label).takeIf { it >= 0 }?.let { zeile.zellen.getOrNull(it) }
            ?.takeIf { it.isNotBlank() }
}

/** Gemeinsame Jsoup-Hilfen für HTML-Tabellen (Website-Stil: klassische Tables). */
object HtmlTools {

    /** Erkennt die Tabelle(n) einer Seite und parst Header + Datenzeilen. */
    fun tabellen(doc: org.jsoup.nodes.Document): List<GeparsteTabelle> {
        return doc.select("table").mapNotNull { t ->
            val rows = t.select("tr").filter { it.select("td,th").isNotEmpty() }
            if (rows.isEmpty()) return@mapNotNull null
            val kopfZeile = rows.firstOrNull { it.select("th").isNotEmpty() } ?: rows.first()
            val header = kopfZeile.select("th,td").map { it.text().trim() }
            val start = rows.indexOf(kopfZeile) + 1
            val zeilen = rows.drop(start).mapNotNull { tr -> zeile(tr) }
            GeparsteTabelle(header = header, zeilen = zeilen)
        }.filter { it.zeilen.isNotEmpty() }
    }

    /** Einzelne Tabelle mit den meisten Datenzeilen (grob die Haupttabelle). */
    fun hauptTabelle(html: String): GeparsteTabelle? {
        val doc = Jsoup.parse(html)
        val alle = tabellen(doc)
        return alle.maxByOrNull { it.zeilen.size }
    }

    fun hauptTabelle(doc: org.jsoup.nodes.Document): GeparsteTabelle? =
        tabellen(doc).maxByOrNull { it.zeilen.size }

    private fun zeile(tr: Element): TabellenZeile? {
        val zellen = tr.select("td").map { it.text().trim() }
        if (zellen.size < 2) return null
        val link = tr.selectFirst("td a[href*='sp.php'], td a[href*='st.php']")
        val name = link?.text() ?: zellen.firstOrNull()
        val pid = link?.attr("href")?.let {
            Regex("[?&]s=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull()
        }
        val teamId = link?.attr("href")?.let {
            Regex("[?&]c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull()
        }
        val sid = tr.selectFirst("td a[href*='bericht.php']")?.attr("href")
            ?.let { Regex("[?&]s=([\\d,]+)").find(it)?.groupValues?.get(1) }
        val vorschau = tr.selectFirst("td a[href*='spielpreview']")?.attr("href")
            ?.let { Regex("[?&]t(?:1|2)=([\\d]+)").find(it)?.groupValues?.get(1) }
            ?: tr.selectFirst("td a[href*='vorschau']")?.attr("href")
        val berichtUrl = tr.selectFirst("td a[href*='rep/saison']")?.attr("href")
        return TabellenZeile(
            zellen = zellen,
            name = name,
            pid = pid,
            teamId = teamId,
            sid = sid,
            vorschau = vorschau,
            berichtUrl = berichtUrl,
            position = positionVonCssKlasse(link?.parent()?.className().orEmpty()),
        )
    }

    /** Ermittelt die Spielerposition aus den CSS-Klassen einer Namenszelle. */
    private fun positionVonCssKlasse(klassen: String): SpielerPosition? =
        klassen.split("\\s+".toRegex()).firstNotNullOfOrNull { positionVonText(it) }

    /** Extrahiert eine Titelzeile (h1/h2/h3) mit dem ZAT-/Spieltagbezug. */
    fun ueberschriften(doc: org.jsoup.nodes.Document): List<String> =
        doc.select("h1,h2,h3,font b").map { it.ownText().trim() }.filter { it.isNotEmpty() }.distinct()

    /**
     * Erkennt „gesperrte" Seiten (Login nötig, keine Rechte, 404-Stub) und liefert
     * einen freundlichen Hinweis – statt den Markierungstext als Inhalt zu rendern.
     */
    fun sperrHinweis(html: String): String? = when {
        html.contains("loginemail", ignoreCase = true) ->
            "Bitte zuerst in der App anmelden."
        html.contains("ohne team nicht verfügbar", ignoreCase = true) ||
            html.contains("ohne team nicht verf&uuml;gbar", ignoreCase = true) ||
            html.contains("ist ohne team nicht verfügbar", ignoreCase = true) ->
            "Diese Seite ist erst nach Anmeldung mit einem Team verfügbar."
        html.contains("404 - not found", ignoreCase = true) ->
            "Diese Seite ist ohne Anmeldung nicht verfügbar."
        html.contains("keine rechte hier zuzugreifen", ignoreCase = true) ||
            html.contains("du hat keine rechte", ignoreCase = true) ->
            "Keine Rechte zum Zugriff auf diese Seite."
        else -> null
    }

    /** Generische, rein lesende Seitenansicht (Absätze + Tabellen). */
    fun seitenAnsicht(html: String): SeitenAnsicht {
        val doc = Jsoup.parse(html)
        val absaetze = doc.select("p, li, td b").mapNotNull { it.text().trim() }
            .filter { it.isNotEmpty() && it.length < 300 }.toList()
        val tabellen = tabellen(doc).map {
            WertTabelle(header = it.header, zeilen = it.zeilen.map { z -> z.zellen })
        }
        return SeitenAnsicht(
            titel = ueberschriften(doc).firstOrNull(),
            absaetze = absaetze.distinct().toList().take(200),
            tabellen = tabellen,
        )
    }
}