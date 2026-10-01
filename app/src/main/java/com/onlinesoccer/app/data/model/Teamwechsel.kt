package com.onlinesoccer.app.data.model

/**
 * Befund zum Zweitteam aus `haupt.php`.
 *
 * Die Website kennt nur einen reinen Session-Toggle: jeder Aufruf von
 * `haupt.php?changetosecond=true` schaltet zwischen Haupt- und Zweitteam um —
 * es gibt kein Ziel und kein `false`. Ob ein Wechsel überhaupt möglich ist,
 * verrät deshalb allein das Vorhandensein des Ankers `changetosecond`.
 */
data class TeamwechselInfo(
    /** Anker `changetosecond` vorhanden, d. h. der Account besitzt ein Zweitteam. */
    val wechselMoeglich: Boolean,
    /** Anzeigename aus „Zu X wechseln" — nur Anzeige, nie Erkennungsmerkmal. */
    val zweitTeamName: String? = null,
)