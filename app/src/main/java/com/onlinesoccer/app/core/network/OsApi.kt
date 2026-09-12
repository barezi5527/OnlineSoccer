package com.onlinesoccer.app.core.network

/**
 * Öffentliche Endpunkt-Pfade und Konstanten.
 *
 * Hinweis: Alle Seiten mit persönlichen Daten liegen unter diesem Host;
 * Bildnachrichten können auf Subdomains verweisen. Es werden ausschließlich
 * HTTPS-Verbindungen verwendet.
 */
object OsApi {
    const val HOST = "os.ongapo.com"
    const val BASE_URL = "https://os.ongapo.com"

    const val USER_AGENT = "OnlineSoccerApp/0.1 (Android; okhttp 4.12)"

    const val LOGIN = "$BASE_URL/validate.php"
    const val MENU = "$BASE_URL/os_menu_haupt.html"
    const val MAIN = "$BASE_URL/haupt.php"
    const val INDEX = "$BASE_URL/index.php"
}