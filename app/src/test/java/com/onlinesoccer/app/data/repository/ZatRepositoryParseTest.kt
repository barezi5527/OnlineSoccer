package com.onlinesoccer.app.data.repository

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressionstests für den „Zugabgabe Zusatz“-Parser (`zuzu.php`)
 * gegen einen echten Server-Dump (app/src/test/resources/dumps/zuzu.html).
 */
class ZatRepositoryParseTest {

    private val repo = ZatRepository(okhttp3.OkHttpClient())

    private fun dump(name: String): String =
        File("src/test/resources/dumps/$name.html").readText()

    @Test
    fun zuzu_liestEintrittspreise() {
        val daten = repo.parseZuzu(dump("zuzu"))
        assertEquals("13", daten.preise.liga)
        assertEquals("15", daten.preise.pokal)
        assertEquals("15", daten.preise.international)
    }

    @Test
    fun zuzu_liestPhysioSpielerMitDaten() {
        val daten = repo.parseZuzu(dump("zuzu"))
        assertTrue("Kaderliste vorhanden", daten.spieler.size >= 30)
        val erster = daten.spieler.firstOrNull()
        assertEquals(1, erster?.nummer)
        assertEquals("Lars Vincez", erster?.name)
        assertEquals(74, erster?.fit)
        assertEquals("47.000", erster?.kosten)
        assertTrue("Spieler-ID wird aus spielerinfo-Link gelesen", daten.spieler.any { it.pid == 97944L })
    }
}