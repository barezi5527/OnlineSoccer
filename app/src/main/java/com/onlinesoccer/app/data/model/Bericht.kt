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
    val rohtext: String? = null,
    val url: String,
)
