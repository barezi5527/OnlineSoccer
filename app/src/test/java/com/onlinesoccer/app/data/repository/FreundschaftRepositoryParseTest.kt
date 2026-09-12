package com.onlinesoccer.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreundschaftRepositoryParseTest {
    private val repository = FreundschaftRepository(okhttp3.OkHttpClient())

    @Test
    fun uebersichtLiestZatsUndGebuchtesSpiel() {
        val daten = repository.parse(
            """
            <select name="blindZat"><option value="12">ZAT 12</option></select>
            <select name="reserveZat[]"><option value="13">ZAT 13</option><option value="14" selected>ZAT 14</option></select>
            <table>
              <tr><th>ZAT</th><th>Gegner</th><th>Aktion</th></tr>
              <tr><td>ZAT 10</td><td>Team A</td><td><input type="hidden" name="FssId" value="42">Stornieren</td></tr>
            </table>
            """.trimIndent(),
        )

        assertEquals("12", daten.blindZats.single().value)
        assertEquals("14", daten.reservierteZats.single().value)
        assertTrue(daten.spiele.single().stornierbar)
    }
}
