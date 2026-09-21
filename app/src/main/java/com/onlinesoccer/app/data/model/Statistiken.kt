package com.onlinesoccer.app.data.model

/** Zeile der „Topteams"-Statistik (`osneu/statteam`). */
data class TopteamZeile(
    val nr: Int?,
    val teamId: Long?,
    val team: String,
    val land: String = "",
    val wert: String = "",
)

/** Ergebnis „Topteams": Formularoptionen + Tabellenzeilen. */
data class TopteamsDaten(
    val laender: List<LaenderOption> = emptyList(),
    val ligas: List<LaenderOption> = emptyList(),
    val statistiken: List<LaenderOption> = emptyList(),
    val anzeigen: List<LaenderOption> = emptyList(),
    val zeilen: List<TopteamZeile> = emptyList(),
) {
    val hatErgebnis: Boolean get() = zeilen.isNotEmpty()
}

/** Zeile der „Topspieler"-Statistik (`osneu/statspieler`). */
data class TopspielerZeile(
    val nr: Int?,
    val pid: Long?,
    val name: String,
    val position: String? = null,
    val teamId: Long?,
    val team: String = "",
    val alter: String = "",
    val nation: String = "",
    val wert: String = "",
)

/** Ergebnis „Topspieler": Formularoptionen + Tabellenzeilen. */
data class TopspielerDaten(
    val laender: List<LaenderOption> = emptyList(),
    val ligas: List<LaenderOption> = emptyList(),
    val statistiken: List<LaenderOption> = emptyList(),
    val positionen: List<LaenderOption> = emptyList(),
    val anzeigen: List<LaenderOption> = emptyList(),
    val zeilen: List<TopspielerZeile> = emptyList(),
) {
    val hatErgebnis: Boolean get() = zeilen.isNotEmpty()
}

/** Zeile der „Fairplaytabelle" (`fpt.php`). */
data class FairplayZeile(
    val nr: Int?,
    val teamId: Long?,
    val team: String,
    val gelb: Int? = null,
    val gelbRot: Int? = null,
    val rot: Int? = null,
    val spiele: Int? = null,
    val punkte: Double? = null,
)

/** Zeile der „Topscorer"-Liste (`topscorer.php`). */
data class TopscorerZeile(
    val nr: Int?,
    val pid: Long?,
    val name: String,
    val position: String? = null,
    val alter: String = "",
    val skill: String = "",
    val opti: String = "",
    val land: String = "",
    val teamId: Long?,
    val team: String = "",
    val liga: String = "",
    val wert: String = "",
)

/** Ergebnis „Topscorer": Formularoptionen + Tabellenzeilen. */
data class TopscorerDaten(
    val laender: List<LaenderOption> = emptyList(),
    val ligas: List<LaenderOption> = emptyList(),
    val statistiken: List<LaenderOption> = emptyList(),
    val positionen: List<LaenderOption> = emptyList(),
    val saisons: List<LaenderOption> = emptyList(),
    val arten: List<LaenderOption> = emptyList(),
    val zeilen: List<TopscorerZeile> = emptyList(),
)

/** Basis-Filter der Spielersuche (`osneu/spielersuche`). */
data class SucheBasis(
    val landId: Int = 0,
    val ligaId: Int = 0,
    val anzeigeId: Int = 1,
    val nationId: Int = 0,
)

/** Eine Kriteriumszeile der Spielersuche (Attribut-ID, Operator, Wert, Sortierung). */
data class SucheKriterium(
    val attributId: Int,
    val opVon: String,
    val valVon: String,
    val opBis: String = "",
    val valBis: String = "",
    val sort: String = "",
)

/** Trefferzeile der Spielersuche. */
data class SucheSpielerZeile(
    val nr: Int?,
    val pid: Long?,
    val name: String,
    val position: String? = null,
    val alter: String = "",
    val nation: String = "",
    val teamId: Long?,
    val team: String = "",
)

/** Ergebnis der Spielersuche: Anzahl + Trefferzeilen. */
data class SpielersucheErgebnis(
    val anzahl: Int = 0,
    val zeilen: List<SucheSpielerZeile> = emptyList(),
)

/** Attribute der Spielersuche (Attribut-ID → Name), inkl. Meta für die Kriteriumszeilen. */
data class SucheAttribut(
    val id: Int,
    val name: String,
    val typ: String = "zahl",
    val werte: List<LaenderOption> = emptyList(),
)

/** Formularoptionen der Spielersuche. */
data class SpielersucheOptionen(
    val laender: List<LaenderOption> = emptyList(),
    val ligas: List<LaenderOption> = emptyList(),
    val anzeigen: List<LaenderOption> = emptyList(),
    val nationen: List<LaenderOption> = emptyList(),
    val attribute: List<SucheAttribut> = emptyList(),
)

/** Gespeicherte Suchabfrage (`action=abfragen`). */
data class GespeicherteAbfrage(
    val id: Int,
    val name: String,
)

/** Details einer gespeicherten Abfrage (`action=abfrage&id=…`). */
data class AbfrageDetails(
    val name: String,
    val basis: SucheBasis,
    val kriterien: List<SucheKriterium>,
)

/** Namensvorschlag der Spielervergleich-Suche (`osneu/ajax/findSpieler`). */
data class SpielerVorschlag(
    val id: Long,
    val name: String,
    val zusatz: String = "",
)

/** Eine Vergleichszeile des Spielervergleichs (`action=getSpieler`). */
data class VergleichZeile(
    val label: String,
    val wert1: String = "",
    val wert2: String = "",
    /** 1 = Spieler 1 besser, 2 = Spieler 2 besser, 0/kein = Gleichstand/neutral. */
    val besser: Int = 0,
)

/** Ergebnis des Spielervergleichs: Köpfe der beiden Spieler/Teams + Zeilen. */
data class Spielervergleich(
    val name1: String? = null,
    val pid1: Long? = null,
    val name2: String? = null,
    val pid2: Long? = null,
    val team1Id: Long? = null,
    val team1: String? = null,
    val team2Id: Long? = null,
    val team2: String? = null,
    val zeilen: List<VergleichZeile> = emptyList(),
)

/** Eine Schlüssel-/Wert-Zeile einer Statistik-Blockseite. */
data class KennzahlZeile(
    val label: String,
    val wert: String,
)

/** Ein Abschnitt der Spielstatistiken (`osneu/statistics`) mit optionalem Untertitel. */
data class StatistikAbschnitt(
    val titel: String,
    val untertitel: String? = null,
    val zeilen: List<KennzahlZeile> = emptyList(),
)

/** Ergebnis „Spielstatistiken". */
data class Spielstatistiken(
    val hinweise: List<String> = emptyList(),
    val abschnitte: List<StatistikAbschnitt> = emptyList(),
)

/** Eine Rekordzeile der Tabellenstatistiken (`osneu/userstatistics`). */
data class RekordZeile(
    val text: String,
    val teamId: Long? = null,
)

/** Abschnitt (Sektion/Untertitel) der Tabellenstatistiken mit Rekordzeilen. */
data class RekordAbschnitt(
    val titel: String,
    val untertitel: String = "",
    val zeilen: List<RekordZeile> = emptyList(),
)

/** Ergebnis „Tabellenstatistiken". */
data class Tabellenstatistiken(
    val abschnitte: List<RekordAbschnitt> = emptyList(),
)