package com.onlinesoccer.app.data.model

enum class BerichtEreignisTyp {
    TOR, GELBE_KARTE, ROTE_KARTE, VERLETZUNG, ELFMETER, SONSTIGES,
}

data class BerichtSpieler(
    val name: String,
    val nummer: String? = null,
    val position: String? = null,
    val feldzeile: Int? = null,
    val feldspalte: Int? = null,
    /** Spieler-ID, falls der Bericht sie verlinkt (z. B. `spielerinfo(123)`). */
    val spielerId: Long? = null,
)

data class BerichtAufstellung(
    val formation: String? = null,
    val spieler: List<BerichtSpieler> = emptyList(),
    val einstellungen: BerichtEinstellungen = BerichtEinstellungen(),
) {
    val startspieler: List<BerichtSpieler>
        get() = spieler.filter { it.nummer == "T" || it.nummer.orEmpty() in "A".."L" }
}

data class BerichtEinstellungen(
    val einsatz: String? = null,
    val haerte: String? = null,
    val spielweise: String? = null,
    val taktikSturm: String? = null,
    val taktikMittelfeld: String? = null,
    val taktikAbwehr: String? = null,
)

/** Ein Ereignis im Spielbericht. */
data class BerichtEreignis(
    val minute: String? = null,
    val text: String,
    val typ: BerichtEreignisTyp = BerichtEreignisTyp.SONSTIGES,
) {
    val tor: Boolean get() = typ == BerichtEreignisTyp.TOR
}

/**
 * Einzelstatistik eines Spielers aus der Tabelle „Es folgen die Spielerstatistiken".
 * Alle Werte stehen exakt so im Bericht; fehlen sie, bleibt die Karte leer.
 */
data class BerichtSpielerStatistik(
    /** Spielnote, sofern der Bericht eine ausweist. */
    val note: String? = null,
    /** Gewonnene Zweikämpfe (ZK). */
    val zweikaempfe: Int = 0,
    /** Zweikampfquote in Prozent (ZK-%), 0…100. */
    val zweikampfQuote: Double = 0.0,
    /** Schüsse insgesamt. */
    val schuesse: Int = 0,
    /** Schüsse aufs Tor. */
    val aufsTor: Int = 0,
    /** Tore laut Spielerstatistik-Tabelle. */
    val tore: Int = 0,
    /** Vorlagen laut Spielerstatistik-Tabelle. */
    val vorlagen: Int = 0,
)

/** Ein Spieler samt seiner Statistik in der Reihenfolge des Berichts. */
data class BerichtSpielerStatistikEintrag(
    val name: String,
    val statistik: BerichtSpielerStatistik,
)
/** Statistik einer Mannschaft im Spielbericht. */
data class BerichtStatistik(
    val abseits: String? = null,
    val ecken: String? = null,
    val fouls: String? = null,
    val elfmeter: String? = null,
    val ballbesitz: String? = null,
    val schnittSkill: String? = null,
    val schnittOpti: String? = null,
    val fitness: String? = null,
    val moral: String? = null,
)

/** Nativer Spielbericht (`rep/saison/<saison>/<zat>/<heim>-<gast>.html`). */
data class SpielBericht(
    val saison: Int,
    val zat: Int,
    val heim: String? = null,
    val gast: String? = null,
    val heimId: Long? = null,
    val gastId: Long? = null,
    val ergebnis: String? = null,
    val datum: String? = null,
    val stadion: String? = null,
    val spielart: String? = null,
    val zuschauer: String? = null,
    val heimAufstellung: BerichtAufstellung? = null,
    val gastAufstellung: BerichtAufstellung? = null,
    val ereignisse: List<BerichtEreignis> = emptyList(),
    val statistik: BerichtStatistik? = null,
    /** Spielerstatistiken je Team, Schlüssel = Spielername (kleingeschrieben). */
    val heimSpielerStatistik: Map<String, BerichtSpielerStatistik> = emptyMap(),
    val gastSpielerStatistik: Map<String, BerichtSpielerStatistik> = emptyMap(),
    /** Geordnete Spielerstatistiken je Team für die Anzeige (Reihenfolge des Berichts). */
    val heimSpielerStatistikListe: List<BerichtSpielerStatistikEintrag> = emptyList(),
    val gastSpielerStatistikListe: List<BerichtSpielerStatistikEintrag> = emptyList(),
    val rohtext: String? = null,
    val url: String,
)
