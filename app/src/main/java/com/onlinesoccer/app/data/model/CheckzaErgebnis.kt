package com.onlinesoccer.app.data.model

/**
 * Strukturiertes Ergebnis des ZAT-Checks (checkza.php / osneu/checkza).
 * Die Website öffnet dies als Popup-Fenster mit drei Abschnitten:
 * Aufstellung, Aktionen, Einstellungen und einem Gesamtstatus.
 */
data class CheckzaErgebnis(
    val aufstellung: List<CheckzaEintrag>,
    val aktionen: List<CheckzaEintrag>,
    val einstellungen: List<CheckzaEintrag>,
    val gesamtStatus: String,
    val gueltig: Boolean,
)

/** Ein einzelner Eintrag innerhalb eines Checkza-Abschnitts. */
data class CheckzaEintrag(
    val text: String,
    val gueltig: Boolean,
)
