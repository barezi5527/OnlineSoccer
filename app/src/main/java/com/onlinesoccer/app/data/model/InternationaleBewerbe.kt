package com.onlinesoccer.app.data.model

data class InternationaleOption(
    val wert: String,
    val label: String,
)

data class InternationaleFilter(
    val saison: String? = null,
    val runde: String? = null,
    val gruppe: String? = null,
    val ergebnisse: String? = null,
)

data class InternationaleAnsicht(
    val titel: String? = null,
    val saisonen: List<InternationaleOption> = emptyList(),
    val runden: List<InternationaleOption> = emptyList(),
    val gruppen: List<InternationaleOption> = emptyList(),
    val ergebnisOptionen: List<InternationaleOption> = emptyList(),
    val filter: InternationaleFilter = InternationaleFilter(),
    val abschnitte: List<String> = emptyList(),
    val tabellen: List<WertTabelle> = emptyList(),
    val spiele: List<InternationaleSpiel> = emptyList(),
    /** Teilnehmerliste (`intTeilnehmer`), nach Wettbewerb gruppiert. */
    val teilnehmer: List<InternationaleTeilnehmerGruppe> = emptyList(),
    /** Spieltage (ZAT) der Gruppenphase – die Website gliedert die Paarungen danach. */
    val zatOptionen: List<InternationaleOption> = emptyList(),
)

/** Eine Wettbewerbsgruppe der Teilnehmerliste (OSC, OSE, OSC-Qualifikation, OSE-Qualifikation). */
data class InternationaleTeilnehmerGruppe(
    val bezeichnung: String,
    val teams: List<InternationaleTeam> = emptyList(),
)

/** Ein Verein aus der Teilnehmerliste mit Trainer- und Detailzeile. */
data class InternationaleTeam(
    val name: String,
    val teamId: Long? = null,
    val zusatz: List<String> = emptyList(),
)

data class InternationaleSpiel(
    val heim: String,
    val gast: String,
    val ergebnis: String? = null,
    val berichtUrl: String? = null,
    val heimTeamId: Long? = null,
    val gastTeamId: Long? = null,
    /** Spieltag laut Website-Überschrift (`ZAT 17`), nur in der Gruppenphase vorhanden. */
    val zat: String? = null,
)

/**
 * Farbklasse einer Zeile in den internationalen Tabellen – wie die Website sie per
 * CSS-Klasse am `<tr>` der Gruppenphase setzt (`osneu/css/style.css`:
 * `intTabelle tr.top`, `tr.middle`, `tr.bottom`). Bedeutung und Farbe stammen
 * ausschließlich vom Server.
 */
enum class InternationaleTabelleKlasse(val cssKlasse: String, val label: String) {
    TOP("top", "Finalrunde"),
    MITTEL("middle", "Mittelfeld"),
    UNTEN("bottom", "Ausgeschieden"),
    ;

    companion object {
        fun vonCssKlasse(cssKlasse: String?): InternationaleTabelleKlasse? {
            if (cssKlasse.isNullOrBlank()) return null
            val einzelne = cssKlasse.trim().split(Regex("\\s+"))
            return entries.firstOrNull { it.cssKlasse in einzelne }
        }
    }
}
