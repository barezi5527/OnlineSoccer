package com.onlinesoccer.app.data.model

/** Auswahleintrag (Land/Liga) für die öffentlichen Listen der Website. */
data class LaenderOption(
    val id: String,
    val label: String,
    /** true, wenn die Website diese Option als Vorauswahl (`selected`) liefert. */
    val selected: Boolean = false,
)

/** Zeile der Liste „Freie Teams" (`osneu/freieteams`). */
data class FreieTeamZeile(
    val teamId: Long?,
    val verein: String,
    val land: String,
    val liga: String,
)

/** Ergebnis „Freie Teams": Anzahl-Hinweis + Länderauswahl + Zeilen. */
data class FreieTeamsDaten(
    val anzahlText: String? = null,
    val laender: List<LaenderOption> = emptyList(),
    val zeilen: List<FreieTeamZeile> = emptyList(),
)

/** Zeile der Team-/Managerliste (`osneu/managerliste`). */
data class ManagerZeile(
    val managerId: Long?,
    val manager: String,
    val teamId: Long?,
    val team: String,
    val nmr5: String = "",
    val nmr20: String = "",
    val nmrGesamt: String = "",
    val wechselsperre: String = "",
    val zugabgabe: String = "",
)

/** Ergebnis „Team-/Managerliste": Länder-/Ligaauswahl + Zeilen. */
data class ManagerlisteDaten(
    val laender: List<LaenderOption> = emptyList(),
    val ligas: List<LaenderOption> = emptyList(),
    val zeilen: List<ManagerZeile> = emptyList(),
)

/** Trefferzeile der Team-/Managersuche (`osneu/managersuche`). */
data class ManagerSucheZeile(
    val managerId: Long?,
    val manager: String,
    val teamId: Long?,
    val team: String,
)

/** Ergebnis „Team-/Managersuche": gefundene Manager-/Verein-Zeilen. */
data class ManagerSucheDaten(
    val zeilen: List<ManagerSucheZeile> = emptyList(),
)