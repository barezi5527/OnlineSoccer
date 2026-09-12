package com.onlinesoccer.app.data.model

/**
 * Datenmodelle für die klassische Zugabgabe `zugabgabe.php` (p=1 Aktionen, p=2 Einstellungen).
 *
 * Quellen: echte Server-Dumps (za1/za2 + item-Seiten). Jede Aktion/Einstellung ist ein
 * selbständig gespeichertes Element (GET `?p=x&itemcreate=<id>&…&anlegen=…`).
 */

/** Kennung einer Aktion (p=1) oder Einstellung (p=2). Ids entsprechen dem `item`-Select. */
enum class ZugabgabeElementTyp(
    val gruppe: Int,
    val id: Int,
    val label: String,
) {
    // Aktionen (p=1)
    EINWECHSLUNG(1, 1, "Einwechslung"),
    EINSATZ(1, 2, "Einsatz festlegen"),
    HAERTE(1, 3, "Härte festlegen"),
    SPIELWEISE(1, 4, "Spielweise festlegen"),
    POSITIONSWECHSEL(1, 5, "Positionswechsel"),
    MANNDECKUNG(1, 6, "Manndeckung"),

    // Einstellungen (p=2)
    KAPITAEN(2, 8, "Kapitän"),
    SPIELMACHER(2, 9, "Spielmacher"),
    ELFMETERSCHUETZE(2, 10, "Elfmeterschütze"),
    FREISTOSS_DIREKT(2, 11, "Freistoss direkt"),
    FREISTOSS_INDIREKT(2, 12, "Freistoss indirekt"),
    ECKE(2, 13, "Ecke"),
    LIBERO(2, 14, "Libero"),
    TAKTIK_ABWEHR(2, 16, "Taktik - Abwehr"),
    TAKTIK_MITTELFELD(2, 17, "Taktik - Mittelfeld"),
    TAKTIK_STURM(2, 18, "Taktik - Sturm"),
    ;

    val istEinstellung: Boolean get() = gruppe == 2

    /** Reine Spieler-Auswahl (Einstellungen ohne Minute/Abhängigkeit). */
    val istSpielerAuswahl: Boolean
        get() = this in setOf(KAPITAEN, SPIELMACHER, ELFMETERSCHUETZE, FREISTOSS_DIREKT, FREISTOSS_INDIREKT, ECKE, LIBERO)

    /** Braucht Positionsraster (Zeile+Spalte ggf. Sonderplatz). */
    val brauchtPosition: Boolean get() = this == EINWECHSLUNG || this == POSITIONSWECHSEL

    companion object {
        fun vonId(gruppe: Int, id: Int): ZugabgabeElementTyp? =
            entries.firstOrNull { it.gruppe == gruppe && it.id == id }

        val aktionen: List<ZugabgabeElementTyp> get() = entries.filter { it.gruppe == 1 }
        val einstellungen: List<ZugabgabeElementTyp> get() = entries.filter { it.gruppe == 2 }
    }
}

/** Eine (Lese-)Option eines Auswahlfelds. */
data class AuswahlOption(
    val id: String,
    val label: String,
)

/** Kopfinformation der Zugabgabe-Seite (identisch auf p=0/1/2). */
data class ZugabgabeKopfinfo(
    val zat: Int?,
    val termin: String?,
    val spiel: String?,
    val status: String?,
) {
    val gueltig: Boolean get() = status.equals("Gültig", ignoreCase = true)
}

/** Ein bereits im Server liegendes Element (Aktion/Einstellung). */
data class ZugabgabeElementEintrag(
    val relaId: String,
    val text: String,
)

/** Geparste `zugabgabe.php?p=<gruppe>`-Seite: Kopfinfo + bestehende Elemente. */
data class ZugabgabeElementeSeite(
    val kopfinfo: ZugabgabeKopfinfo?,
    val elemente: List<ZugabgabeElementEintrag>,
    val loeschLabel: String?,
)

/** Eingelesene Formular-Optionen einer `?p=x&item=<id>`-Seite. */
data class ZugabgabeFormular(
    val typ: ZugabgabeElementTyp,
    val kopfinfo: ZugabgabeKopfinfo,
    /** Eigener Kader (zao_einspieler/zao_spieler/spieler_id). */
    val spieler: List<AuswahlOption> = emptyList(),
    /** Gegner-Kader (P1 bei Manndeckung). */
    val gegenspieler: List<AuswahlOption> = emptyList(),
    val minuten: List<AuswahlOption> = emptyList(),
    val abhaengigkeiten: List<AuswahlOption> = emptyList(),
    /** P1 Positionszeilen (A..O). */
    val positionsZeilen: List<AuswahlOption> = emptyList(),
    val positionsSpalten: List<AuswahlOption> = emptyList(),
    val positionsSonder: List<AuswahlOption> = emptyList(),
    /** P1-Werte (Einsatz/Härte/Spielweise/Taktik). */
    val werte: List<AuswahlOption> = emptyList(),
    /** Submit-Label des „Anlegen“-Buttons. */
    val saveButton: String = "   Neue Aktion anlegen   ",
)

/** „Zugabgabe Zusatz“ (`zuzu.php`): Eintrittspreise (Heimspiele) + Physio-Liste. */
data class ZuzuDaten(
    val preise: ZuzuPreise = ZuzuPreise(),
    val spieler: List<ZuzuSpieler> = emptyList(),
)

/** Eintrittspreise für Heimspiele (`zuzu.php`-Textfelder liga/pokal/int). */
data class ZuzuPreise(
    val liga: String? = null,
    val pokal: String? = null,
    val international: String? = null,
)

/** Kaderzeile der Physio-Liste (`zuzu.php`): Spieler mit Fitnesswert und Kosten. */
data class ZuzuSpieler(
    val nummer: Int,
    val pid: Long,
    val name: String,
    val fit: Int,
    val kosten: String,
)