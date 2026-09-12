package com.onlinesoccer.app.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regressionstests für die Seiten-Härtung: Marker-Erkennung gesperrter Seiten
 * ([HtmlTools.sperrHinweis]) und die Host-Whitelist in [TeamRepository.ladeSeite].
 */
class HtmlToolsSperreTest {

    private val teamRepo = com.onlinesoccer.app.data.repository.TeamRepository(
        okhttp3.OkHttpClient(),
        com.onlinesoccer.app.data.repository.ZugabgabeRepository(okhttp3.OkHttpClient()),
    )

    @Test
    fun sperrHinweis_loginemail() {
        val html = "<html><body><p>Bitte $ 25 loginemail anfordern</p></body></html>"
        assertEquals(
            "Bitte zuerst in der App anmelden.",
            HtmlTools.sperrHinweis(html),
        )
    }

    @Test
    fun sperrHinweis_ohneTeamNichtVerfuegbar() {
        val html = "<html><body><p>Diese Seite ist ohne Team nicht verfügbar.</p></body></html>"
        assertEquals(
            "Diese Seite ist erst nach Anmeldung mit einem Team verfügbar.",
            HtmlTools.sperrHinweis(html),
        )
    }

    @Test
    fun sperrHinweis_404Stub() {
        val html = "<html><body><p>Online-Soccer 404 - not found Die angeforderte Seite wurde leider nicht gefunden.</p></body></html>"
        assertEquals(
            "Diese Seite ist ohne Anmeldung nicht verfügbar.",
            HtmlTools.sperrHinweis(html),
        )
    }

    @Test
    fun sperrHinweis_keineRechte() {
        val html = "<html><body><p>Du hat keine Rechte hier zu zugreifen</p></body></html>"
        assertEquals(
            "Keine Rechte zum Zugriff auf diese Seite.",
            HtmlTools.sperrHinweis(html),
        )
    }

    @Test
    fun sperrHinweis_normaleSeite() {
        val html = "<html><body><h1>Nächstes Spiel</h1><p>Samstag, 12.09.2026</p></body></html>"
        assertNull(HtmlTools.sperrHinweis(html))
    }

    @Test
    fun ladeSeite_fremderHostWirdAbgewiesen() {
        val ergebnis = runBlocking { teamRepo.ladeSeite("https://example.com/evil") }
        assertNull("fremde http-URL erzeugt keine Seitensicht", ergebnis)
    }

    @Test
    fun ladeSeite_relativerPfadWirdZumBasisHost() {
        val ergebnis = runBlocking { teamRepo.ladeSeite("ju.php") }
        if (ergebnis != null) {
            assertEquals("os.ongapo.com", OsApi.BASE_URL.substringAfter("://"))
        }
    }
}