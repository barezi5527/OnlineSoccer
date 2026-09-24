package com.onlinesoccer.app.data.repository

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressionstests für die „Topteams"-Statistik (`osneu/statteam`) gegen echte
 * Server-Dumps (`app/src/test/resources/dumps/statteam*.html`).
 */
class StatistikRepositoryParseTest {

    private val repo = StatistikRepository(okhttp3.OkHttpClient())

    private fun dump(name: String): String =
        File("src/test/resources/dumps/$name.html").readText()

    @Test
    fun topteams_mitLandUndLiga_parstFilterUndZeilen() {
        val daten = repo.parseTopteams(dump("statteam"))

        assertEquals("38 Länder (37 + Alle)", 38, daten.laender.size)
        assertEquals("8 Ligen (7 + Alle)", 8, daten.ligas.size)
        assertEquals("15 Statistiken", 15, daten.statistiken.size)
        assertEquals("8 Anzeigen", 8, daten.anzeigen.size)

        assertEquals("Alle", daten.laender.first().label)
        assertEquals("0", daten.laender.first().id)
        assertEquals("Deutschland", daten.laender.first { it.id == "6" }.label)
        assertEquals("1. Liga", daten.ligas.first { it.id == "1" }.label)
        assertEquals("Schnitt Opt. Skill", daten.statistiken.first { it.id == "2" }.label)
        assertEquals("Top 10", daten.anzeigen.first { it.id == "1" }.label)

        assertEquals("10 Zeilen", 10, daten.zeilen.size)
        val erste = daten.zeilen.first()
        assertEquals(1, erste.nr)
        assertEquals(164L, erste.teamId)
        assertEquals("Eintracht Wuppertal", erste.team)
        assertEquals("GER", erste.land)
        assertEquals("81,21", erste.wert)
    }

    @Test
    fun topteams_ohneFilter_parstAlleOptionenUndZeilen() {
        val daten = repo.parseTopteams(dump("statteam_alle"))

        assertEquals("38 Länder (37 + Alle)", 38, daten.laender.size)
        assertTrue("alle-Liga: Eintrag 0 als Alle", daten.ligas.first().id == "0")
        assertTrue(daten.zeilen.isNotEmpty())
        val erste = daten.zeilen.first()
        assertEquals("FC Sloboda Visoko", erste.team)
        assertEquals(1870L, erste.teamId)
        assertEquals("BIH", erste.land)
        assertEquals("85,06", erste.wert)
    }
}