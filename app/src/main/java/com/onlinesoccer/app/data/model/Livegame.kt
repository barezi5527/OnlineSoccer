package com.onlinesoccer.app.data.model

/** Livegame-Daten aus `livegame/php/data.php?action=gamedata` (Phase-2 §15.1). */
data class LivegameData(
    val heimName: String?,
    val gastName: String?,
    val heimTore: Int,
    val gastTore: Int,
    val zat: Int?,
    val spieltyp: String?,
    val datum: String?,
    val stadion: String?,
    val played: Boolean,
    val ereignisse: List<LiveEreignis>,
    val heimTaktik: TaktikWerte?,
    val gastTaktik: TaktikWerte?,
    val heimStatistik: StatistikWerte?,
    val gastStatistik: StatistikWerte?,
)

data class LiveEreignis(val minute: Int, val text: String)

data class TaktikWerte(
    val commitment: Int,
    val hardness: Int,
    val playtype: Int,
    val defence: Int,
    val midfield: Int,
    val offence: Int,
)

data class StatistikWerte(
    val goals: Int,
    val offside: Int,
    val corners: Int,
    val fouls: Int,
    val penalties: Int,
    val possession: Int,
)