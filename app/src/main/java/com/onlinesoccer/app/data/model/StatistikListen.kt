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