package com.onlinesoccer.app.data.model

/** Filteroption eines Transfer-Formulars (z. B. „Alter" → „16-18"). */
data class TransferOption(
    val wert: String,
    val label: String,
)

/** Eine Filterzeile (`select name=…`) inklusive aller Optionen (Platzhalter 0 = „Alle"). */
data class TransferFilter(
    val name: String,
    val optionen: List<TransferOption>,
) {
    val label: String get() = optionen.firstOrNull()?.label.orEmpty()

    fun option(wert: String): TransferOption? = optionen.firstOrNull { it.wert == wert }
}

/** Zeile der Transferliste (`osneu/transferliste`). */
data class TransferListeZeile(
    val spielerId: Long,
    val name: String,
    val status: String = "",
    val details: String = "",
    val alter: String = "",
    val position: String = "",
    val land: String = "",
    val skill: String = "",
    val optSkill: String = "",
    val teamId: Long? = null,
    val team: String = "",
    val abloese: String = "",
    val datum: String = "",
)

/** Ergebnis der Transferliste: Filteroptionen, Hinweis, Zeilen und Pagination. */
data class TransferListeErgebnis(
    val filter: List<TransferFilter> = emptyList(),
    val hinweis: String? = null,
    val zeilen: List<TransferListeZeile> = emptyList(),
    val seite: Int = 1,
    val gesamtSeiten: Int = 0,
    val treffer: Int = 0,
    val gesucht: Boolean = false,
)

/** Eintrag des Transfermarkts (`tm.php`). */
data class TransferMarktEintrag(
    val spielerId: Long,
    val name: String,
    val alter: String = "",
    val position: String = "",
    val land: String = "",
    val skill: String = "",
    val optSkill: String = "",
    val gebot: String = "",
    val bieter: String = "",
    val bieterTeamId: Long? = null,
    val gehalt: String = "",
    val dauer: String = "",
    val anzahl: String = "",
)

/** Ergebnis des Transfermarkts: Filteroptionen, Hinweis und Einträge. */
data class TransferMarktErgebnis(
    val filter: List<TransferFilter> = emptyList(),
    val hinweis: String? = null,
    val eintraege: List<TransferMarktEintrag> = emptyList(),
    val gesucht: Boolean = false,
)

/** Ein eigenes Transfermarkt-Gebot (`viewtm.php`). */
data class EigeneGeboteZeile(
    val spielerId: Long = 0L,
    val name: String = "",
    val alter: String = "",
    val land: String = "",
    val position: String = "",
    val skill: String = "",
    val optSkill: String = "",
    val laufzeit: String = "",
    val gehalt: String = "",
    val gebot: String = "",
    val transfertag: String = "",
)

/** „Eigene Gebote" (`viewtm.php`): Zeilen plus Summen-Zeile. */
data class EigeneGeboteErgebnis(
    val zeilen: List<EigeneGeboteZeile> = emptyList(),
    val summe: String = "",
)

/** Ein verliehener bzw. geliehener Spieler (`viewleih.php`). */
data class LeihUebersichtZeile(
    val spielerId: Long = 0L,
    val name: String = "",
    val alter: String = "",
    val land: String = "",
    val position: String = "",
    val skill: String = "",
    val optSkill: String = "",
    val dauer: String = "",
    val gehalt: String = "",
    val leihgebuehr: String = "",
    val leihclub: String = "",
    val leihclubId: Long? = null,
)

/** „Leihspieler Übersicht" (`viewleih.php`): verliehene und geliehene Spieler. */
data class LeihUebersichtErgebnis(
    val verliehen: List<LeihUebersichtZeile> = emptyList(),
    val geliehen: List<LeihUebersichtZeile> = emptyList(),
)

/** Eine Zeile der „Letzten …"-Listen (`osneu/lastxxx`). */
data class LetzteAktionenZeile(
    val spielerId: Long = 0L,
    val spieler: String = "",
    val datum: String = "",
    val position: String = "",
    val von: String = "",
    val vonId: Long? = null,
    val zu: String = "",
    val zuId: Long? = null,
    val team: String = "",
    val teamId: Long? = null,
    val ziel: String = "",
    val betrag: String = "",
    val dauer: String = "",
    val anmerkung: String = "",
)

/** Ergebnis der „Letzten …"-Seiten (`Letzte Transfers`, `Letzte Leihen`, …). */
data class LetzteAktionenErgebnis(
    val zeilen: List<LetzteAktionenZeile> = emptyList(),
)

/** Die fünf „Letzten …"-Unterseiten unter „Transfers". */
enum class LetzteAktionenArt(
    val pfad: String,
    val routeId: String,
    val titel: String,
) {
    TRANSFERS("osneu/lasttrans", "transfers", "Letzte Transfers"),
    LEIHEN("osneu/lastleih", "leihen", "Letzte Leihen"),
    VM("osneu/lastvm", "vm", "Letzte VM-Käufe"),
    TM("osneu/lasttm", "tm", "Letzte TM-Käufe"),
    BLITZ("osneu/lastblitz", "blitz", "Letzte Schnelltransfers");

    companion object {
        fun vonRouteId(id: String): LetzteAktionenArt? = entries.firstOrNull { it.routeId == id }
    }
}

/** Vorschau mitsamt Absende-Formular für ein Transfermarkt-Gebot (`gebot.php?s=<pid>`). */
data class GebotInformation(
    val spielerId: Long = 0L,
    val name: String = "",
    val alter: String = "",
    val nationalitaet: String = "",
    val position: String = "",
    val marktwert: String = "",
    val angeboteBis: String = "",
    val hoechstgebot: String = "",
    val gehalt: String = "",
    val bieter: String = "",
    val submitName: String = "",
    val submitValue: String = "",
)

/** Ergebnis eines abgesendeten Gebots (`POST gebot.php?s=<pid>`). */
data class GebotsErgebnis(
    val erfolg: Boolean = false,
    val meldung: String = "",
)

/**
 * Transferstatus-Option eines eigenen Spielers (`tstatus.php`, Radio-Gruppe U/N/A/T).
 * Reihenfolge entspricht der Website (Spalten „U | N | A | T"); `wert` ist der
 * Formularwert, `kuerzel` die in der Mannschafts-/Kaderansicht verwendete Kennung.
 */
enum class TransferStatus(
    val wert: String,
    val kuerzel: String,
    val label: String,
    val beschreibung: String,
) {
    U(
        "3", "U", "Unverkäuflich",
        "Spieler bleibt im Kader. Moralbonus, dafür 28 Tage Transfersperre, sobald der Status wieder geändert wird.",
    ),
    N("0", "N", "Normal", "Spieler steht ohne besondere Kennung im Kader."),
    A(
        "1", "A", "Auf Anfrage",
        "Kein Moral-Bonus/-Malus. Spieler ist separat auf der Transferliste auffindbar und kann mit Mindestablöse/Infotext versehen werden.",
    ),
    T(
        "2", "T", "Transfergelistet",
        "Spieler steht auf der Transferliste (Kürzel „T“ im Kader). Moral -20; muss mindestens 7 Tage gelistet bleiben, Mindestablöse/Infotext möglich.",
    );

    companion object {
        /** Status anhand des Formularwertes (3=U, 0=N, 1=A, 2=T). */
        fun vonWert(wert: String): TransferStatus? = entries.firstOrNull { it.wert == wert }
    }
}

/**
 * Transferdetail eines eigenen Spielers (`tstatus.php`, Checkbox-Gruppe V/K/T/L).
 * Ersetzt einen Transferinfotext; die zulässigen Status sind durch die Website
 * vorgegeben (V nur bei N/T, K/T/L nur bei A/T).
 */
enum class TransferDetail(
    val kuerzel: String,
    val label: String,
    val erlaubteStatus: Set<TransferStatus>,
) {
    V("V", "Verkauft / vergeben", setOf(TransferStatus.N, TransferStatus.T)),
    K("K", "Kann gekauft werden", setOf(TransferStatus.A, TransferStatus.T)),
    T("T", "Kann getauscht werden", setOf(TransferStatus.A, TransferStatus.T)),
    L("L", "Kann geliehen werden", setOf(TransferStatus.A, TransferStatus.T));

    /** Ist das Detail beim angegebenen Status erlaubt? */
    fun erlaubt(status: TransferStatus): Boolean = status in erlaubteStatus

    companion object {
        fun vonKuerzel(kuerzel: String): TransferDetail? = entries.firstOrNull { it.kuerzel == kuerzel }
    }
}

/** Zeile der „Transferstatus"-Übersicht (`tstatus.php`): ein Kaderspieler je Zeile. */
data class TransferStatusZeile(
    val spielerId: Long,
    val name: String,
    val mor: String = "",
    val fit: String = "",
    val skillSchnitt: String = "",
    val optSkill: String = "",
    val status: TransferStatus = TransferStatus.N,
    val mindestabloese: String = "",
    val transfertext: String = "",
    val details: Set<TransferDetail> = emptySet(),
) {
    /** Zeile mit geändertem Transferstatus (übrige Werte bleiben unverändert). */
    fun mitStatus(neuerStatus: TransferStatus): TransferStatusZeile = copy(status = neuerStatus)
}

/** „Transferstatus" (`tstatus.php`): alle eigenen Spieler mit ihren Statusangaben. */
data class TransferStatusErgebnis(
    val zeilen: List<TransferStatusZeile> = emptyList(),
)

/** Ergebnis eines abgesendeten Transferstatus (`POST tstatus.php`). */
data class TransferStatusAntwort(
    val erfolg: Boolean = false,
    val meldung: String = "",
)