package com.onlinesoccer.app.core.network

/**
 * Erkennt den Anmeldezustand anhand des Seiteninhalts statt des HTTP-Statuscodes.
 *
 * Phase-2-Erkenntnis: Der Server liefert auch ohne gültige Session HTTP 200.
 * Persönliche Ansicht enthält „Kontostand", die abgemeldete Ansicht den
 * Login-Feldnamen `loginemail` bzw. Demo-Marker „DemoTeam".
 *
 * Demo-/Gastzustand: Ohne Anmeldung (sogar ganz ohne Session-Cookie) liefert der
 * Server eine Demo-Ansicht („DemoTeam", „Demo-Managerbüro"). Diese darf nicht als
 * persönliche Sitzung gewertet werden – sonst würde eine abgelaufene Session als
 * „eingeloggt" durchgehen und fremde Demo-Daten anzeigen.
 */
object SessionGuard {

    /** Persönliche, eingeloggte Ansicht (echtes Managerbüro ohne Demo-Marker). */
    fun isPersonalView(bytes: ByteArray): Boolean {
        val html = html(bytes)
        return html.contains(MARKER_PERSONAL) &&
            !html.contains(MARKER_LOGIN_FORM) &&
            !isDemoOffice(html)
    }

    /** Demo-/Gast-Ansicht (abgemeldeter Zustand). */
    fun isDemoView(bytes: ByteArray): Boolean {
        return html(bytes).contains(MARKER_DEMO)
    }

    /** Klare Login-/Demo-Ansicht (abgemeldet). */
    fun isLoginView(bytes: ByteArray): Boolean {
        return isPureLoginView(bytes) || isDemoView(bytes)
    }

    /**
     * Nur Login-Formular-Marker (ohne Demo-Team-Heuristik).
     * Für Seiten, die „DemoTeam" als echten Teamnamen enthalten können (z. B. PM).
     */
    fun isPureLoginView(bytes: ByteArray): Boolean {
        return html(bytes).contains(MARKER_LOGIN_FORM)
    }

    /** Positiv-Erkennung des Demo-Managerbüros (nur auf `haupt.php` relevant). */
    private fun isDemoOffice(html: String): Boolean = html.contains(MARKER_DEMO_OFFICE)

    private fun html(bytes: ByteArray): String = HtmlTools.serverText(bytes)

    private const val MARKER_PERSONAL = "Kontostand"
    private const val MARKER_LOGIN_FORM = "loginemail"
    private const val MARKER_DEMO = "DemoTeam"
    private const val MARKER_DEMO_OFFICE = "Demo-Managerb&uuml;ro"
}
