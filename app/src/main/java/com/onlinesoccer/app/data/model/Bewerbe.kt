package com.onlinesoccer.app.data.model

/** Einzelspiel innerhalb eines Spieltags (`ls.php`). */
data class LigaSpiel(
    val zat: Int,
    val heim: String,
    val gast: String,
    val heimId: Long? = null,
    val gastId: Long? = null,
    val toreHeim: Int? = null,
    val toreGast: Int? = null,
    val gespielt: Boolean = false,
    val eigenerVerein: Boolean = false,
    /** Spielbericht-Kennung (Link `bericht.php?s=…`), falls vorhanden. */
    val berichtSid: String? = null,
    /** Statischer Bericht `rep/saison/<saison>/<zat>/<heim>-<gast>.html`. */
    val berichtUrl: String? = null,
    /** Vorschau-Kennung (nicht gespielte Spiele), falls vorhanden. */
    val vorschauId: String? = null,
)

/** Spieltag `ls.php`: ZAT + Liste der Spiele. */
data class LigaSpieltag(
    val zat: Int,
    val datum: String? = null,
    val saison: Int = 0,
    val liga: Int = 0,
    val land: Int = 0,
    /** Wählbare Spieltage (aus `stauswahl`-Optionen) für die Auswahl. */
    val zatOptionen: List<Int> = emptyList(),
    /** Wählbare Ligen (`ligaauswahl`) für die Filter-Leiste. */
    val ligaOptionen: List<LigaOption> = emptyList(),
    /** Wählbare Länder (`landauswahl`) für die Filter-Leiste. */
    val landOptionen: List<LigaOption> = emptyList(),
    /** Wählbare Saisons (`saauswahl`) für die Filter-Leiste. */
    val saisonOptionen: List<LigaOption> = emptyList(),
    val spiele: List<LigaSpiel> = emptyList(),
)

/** Farbklasse einer Ligatabellen-Zeile – wie die Website (`lt.php`) sie per CSS-Klasse markiert. */
enum class LigaTabellenKlasse(val cssKlasse: String, val label: String) {
    OSC("osc", "Aufstiegsplatz / OSC"),
    OSCQ("oscq", "Relegationsplatz (Auf) / OSCQ"),
    OSE("ose", "OSE"),
    OSEQ("oseq", "OSEQ"),
    RELE("rele", "Relegationsplatz (Ab)"),
    AB("ab", "Abstiegsplatz"),
    ;

    companion object {
        /**
         * Liest die CSS-Klassen eines `<tr>` der Ligatabelle in die Farbklasse um.
         * Der Server markiert jede Zeile server-seitig mit der für die jeweilige Liga
         * geltenden Platzbedeutung – die Zeile kann dabei zusätzlich Formatierungs-
         * klassen (z. B. `lineover`) tragen, daher wird jede Einzelklasse geprüft.
         * Unbekannte Klassen (z. B. das neutrale `tabelle`) ergeben `null`.
         */
        fun vonCssKlasse(cssKlasse: String?): LigaTabellenKlasse? {
            if (cssKlasse.isNullOrBlank()) return null
            val einzelne = cssKlasse.trim().split(Regex("\\s+"))
            return entries.firstOrNull { it.cssKlasse in einzelne }
        }
    }
}

/** Ligatabelle (`lt.php`): generische Zeile. */
data class LigaTabelle(
    val header: List<String> = emptyList(),
    val zeilen: List<List<String>> = emptyList(),
    /** Team-ID je Tabellenzeile (aus `teaminfo(H)`-Links), parallel zu `zeilen`. */
    val zeilenTeamIds: List<Long?> = emptyList(),
    /** Farbklasse (Aufstieg/Abstieg u. a.) je Tabellenzeile, parallel zu `zeilen`. */
    val zeilenKlasse: List<LigaTabellenKlasse?> = emptyList(),
    val eigenZeile: Int? = null,
    val saisonen: List<LigaSaison> = emptyList(),
    val saison: Int = 0,
    /** Wählbare Ligen (`ligaauswahl`) für die Filter-Leiste. */
    val ligaOptionen: List<LigaOption> = emptyList(),
    /** Wählbare Länder (`landauswahl`) für die Filter-Leiste. */
    val landOptionen: List<LigaOption> = emptyList(),
    /** Wählbare Tabellenarten (`tabauswahl`) für die Filter-Leiste. */
    val tabOptionen: List<LigaOption> = emptyList(),
    val filter: LigaFilter? = null,
)

/** Wählbare Filter-Option einer Liga-Ansicht (`ligaauswahl`/`landauswahl`/`tabauswahl`). */
data class LigaOption(
    val wert: Int,
    val label: String,
)

/** Eine wählbare Saison der Ligatabelle (`saauswahl`-Option). */
data class LigaSaison(
    val wert: Int,
    val label: String,
)

/** Aktuelle Filter der Ligatabelle (`lt.php`), damit bei Saisonwechsel die Liga beibehalten wird. */
data class LigaFilter(
    val liga: Int,
    val land: Int,
    val tab: Int,
    val saison: Int,
)

/** Landes-Pokal (`lp.php`): Runden als generische Tabellen. */
data class PokalAnsicht(
    val saison: Int = 0,
    val land: Int = 0,
    val runde: Int = 0,
    val saisonen: List<LigaSaison> = emptyList(),
    val rundenOptionen: List<PokalRundeOption> = emptyList(),
    /** Wählbare Länder (`landauswahl`) für die Filter-Leiste. */
    val landOptionen: List<LigaOption> = emptyList(),
    val runden: List<PokalRunde> = emptyList(),
)

data class PokalRundeOption(
    val wert: Int,
    val label: String,
)

data class PokalRunde(
    val spiele: List<PokalSpiel> = emptyList(),
)

data class PokalSpiel(
    val heim: String,
    val gast: String,
    val ergebnis: String? = null,
    val berichtUrl: String? = null,
)

/** ZAT-Ergebnisse (`zer.php`): je Liga die Ergebnisse des ZAT. */
data class ZatErgebnisse(
    val zat: Int? = null,
    val zusammengefasst: List<ZatLiga> = emptyList(),
)

data class ZatLiga(
    val name: String,
    val spiele: List<LigaSpiel>,
)

/** Allgemeine, rein lesende Seitenansicht (Absätze + Tabellen oder gegliederte Abschnitte). */
data class SeitenAnsicht(
    val titel: String? = null,
    val absaetze: List<String> = emptyList(),
    val tabellen: List<WertTabelle> = emptyList(),
    val abschnitte: List<UebersichtAbschnitt> = emptyList(),
)
