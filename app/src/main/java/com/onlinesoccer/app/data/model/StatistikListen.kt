package com.onlinesoccer.app.data.model

/** Zeile der „Topteams"-Statistik (`osneu/statteam`). */
data class TopTeamZeile(
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
    val zeilen: List<TopTeamZeile> = emptyList(),
)

/** Zeile der „Topscorer"-Statistik (`topscorer.php`). */
data class TopscorerZeile(
    val nr: Int?,
    val spielerId: Long?,
    val name: String,
    val alter: String = "",
    val skill: String = "",
    val opti: String = "",
    val land: String = "",
    val teamId: Long?,
    val verein: String = "",
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

/** Zeile der „Topspieler"-Statistik (`osneu/statspieler`). */
data class TopspielerZeile(
    val nr: Int?,
    val spielerId: Long?,
    val name: String,
    val teamId: Long?,
    val team: String,
    val alter: String = "",
    val position: String = "",
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
)