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

    @Test
    fun topscorer_parstFilterUndZeilen() {
        val daten = repo.parseTopscorer(dump("topscorer"))

        assertEquals("Alle", daten.laender.first().label)
        assertEquals("0", daten.laender.first().id)
        assertEquals("Deutschland", daten.laender.first { it.id == "6" }.label)
        assertEquals("1. Liga", daten.ligas.first { it.id == "1" }.label)

        assertEquals("10 Kategorien", 10, daten.statistiken.size)
        assertEquals("Tore", daten.statistiken.first { it.id == "1" }.label)
        assertEquals("Scorerpunkte", daten.statistiken.first { it.id == "3" }.label)
        assertEquals("Treter (Fairplay)", daten.statistiken.first { it.id == "8" }.label)

        assertEquals("7 Positionen (Alle + TOR–STU)", 7, daten.positionen.size)
        assertEquals("STU", daten.positionen.first { it.id == "6" }.label)

        assertEquals("Saison 24", daten.saisons.first { it.id == "24" }.label)

        assertEquals("5 Arten (Alle + 4)", 5, daten.arten.size)
        assertEquals("Liga", daten.arten.first { it.id == "1" }.label)
        assertEquals("Friendly", daten.arten.first { it.id == "4" }.label)

        assertTrue("Ergebniszeilen vorhanden", daten.zeilen.isNotEmpty())
        val erste = daten.zeilen.first()
        assertEquals(1, erste.nr)
        assertEquals(126744L, erste.spielerId)
        assertEquals("Aaron Mercer", erste.name)
        assertEquals("28", erste.alter)
        assertEquals("59.12", erste.skill)
        assertEquals("88.78", erste.opti)
        assertEquals("NIR", erste.land)
        assertEquals(477L, erste.teamId)
        assertEquals("Kickers Duisburg", erste.verein)
        assertEquals("2. Liga B Deutschland", erste.liga)
        assertEquals("8.00", erste.wert)
    }

    @Test
    fun topspieler_parstFilterUndZeilen() {
        val daten = repo.parseTopspieler(dump("statspieler"))

        assertEquals("Alle", daten.laender.first().label)
        assertEquals("0", daten.laender.first().id)
        assertEquals("Deutschland", daten.laender.first { it.id == "6" }.label)
        assertEquals("1. Liga", daten.ligas.first { it.id == "1" }.label)

        assertEquals("4 Statistiken", 4, daten.statistiken.size)
        assertEquals("Opt Skill", daten.statistiken.first { it.id == "16" }.label)

        assertEquals("7 Positionen (Alle + TOR–STU)", 7, daten.positionen.size)
        assertEquals("STU", daten.positionen.first { it.id == "6" }.label)

        assertEquals("8 Anzeigen", 8, daten.anzeigen.size)
        assertEquals("Top 10", daten.anzeigen.first { it.id == "1" }.label)
        assertEquals("Top 100", daten.anzeigen.first { it.id == "4" }.label)

        assertTrue("Ergebniszeilen vorhanden", daten.zeilen.isNotEmpty())
        val erste = daten.zeilen.first()
        assertEquals(1, erste.nr)
        assertEquals(104701L, erste.spielerId)
        assertEquals("Guido Knoche", erste.name)
        assertEquals(613L, erste.teamId)
        assertEquals("SGE Frankfurt", erste.team)
        assertEquals("31", erste.alter)
        assertEquals("STU", erste.position)
        assertEquals("GER", erste.nation)
        assertEquals("99,67", erste.wert)
    }
}