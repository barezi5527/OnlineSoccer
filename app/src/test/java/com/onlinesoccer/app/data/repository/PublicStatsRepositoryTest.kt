package com.onlinesoccer.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PublicStatsRepositoryTest {

    @Test
    fun `parst die Managerzahl aus der Startseite`() {
        val html = "<p>Allerdings versuchen das die <b>1.513</b> Mitstreiter ebenfalls.</p>"

        assertEquals(1513, parseAktiveManager(html))
    }

    @Test
    fun `ignoriert Seiten ohne Managerzahl`() {
        assertNull(parseAktiveManager("<p>Willkommen bei Online Soccer</p>"))
    }
}
