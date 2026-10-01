package com.onlinesoccer.app.data.model

/**
 * Ergebnis eines Teamwechsel-Versuchs.
 *
 * Wichtig: [Erfolgreich] heißt „der Server meldet jetzt ein anderes Team",
 * nicht „ein Request wurde gesendet". Bei `teamId == null` auf beiden Seiten
 * (Wappen nicht lesbar) entscheidet der Teamname über Erfolg oder Nichtstun;
 * ein blindes „Erfolg" wäre sonst eine Lüge gegenüber dem Nutzer.
 */
sealed interface TeamwechselErgebnis {
    /** Team wurde nachweislich gewechselt. */
    data class Erfolgreich(val teamId: Long?, val teamName: String?) : TeamwechselErgebnis

    /** Der Server meldet weiterhin dasselbe Team — es wurde nichts erreicht. */
    data object Unveraendert : TeamwechselErgebnis

    /** Technischer Fehler: kein/abgelaufener Serverkontakt oder Abbruch. */
    data class Fehler(val text: String) : TeamwechselErgebnis
}