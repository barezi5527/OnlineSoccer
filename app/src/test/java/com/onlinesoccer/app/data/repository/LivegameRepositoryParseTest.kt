package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.BerichtEreignisTyp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LivegameRepositoryParseTest {
    private val repo = LivegameRepository(okhttp3.OkHttpClient())

    @Test
    fun parse_klassifiziertEreignisseAusActions() {
        val json = """
            {
              "game": {"home": {"name": "Heim FC"}, "away": {"name": "Gast FC"}, "played": false},
              "actions": [
                {"minute": 1, "text": "Anpfiff"},
                {"minute": 23, "text": "Schuss, TOR Neuer Spielstand: 1:0 (Aaron Muller)"},
                {"minute": 45, "text": "Gelbe Karte für Schmidt"},
                {"minute": 60, "text": "Rote Karte für Berger"}
              ]
            }
        """.trimIndent()

        val live = repo.parse(json)

        assertEquals(4, live.ereignisse.size)
        assertEquals(BerichtEreignisTyp.SONSTIGES, live.ereignisse[0].typ)
        assertEquals(BerichtEreignisTyp.TOR, live.ereignisse[1].typ)
        assertTrue(live.ereignisse[1].text.contains("Neuer Spielstand"))
        assertEquals(BerichtEreignisTyp.GELBE_KARTE, live.ereignisse[2].typ)
        assertEquals(BerichtEreignisTyp.ROTE_KARTE, live.ereignisse[3].typ)
    }

    @Test
    fun parse_liestTeamsUndTore() {
        val json = """
            {
              "game": {"home": {"name": "Heim FC", "goals": 3}, "away": {"name": "Gast FC", "goals": 1}, "played": true},
              "actions": []
            }
        """.trimIndent()

        val live = repo.parse(json)

        assertEquals("Heim FC", live.heimName)
        assertEquals("Gast FC", live.gastName)
        assertEquals(3, live.heimTore)
        assertEquals(1, live.gastTore)
        assertTrue(live.played)
        assertTrue(live.ereignisse.isEmpty())
    }
}