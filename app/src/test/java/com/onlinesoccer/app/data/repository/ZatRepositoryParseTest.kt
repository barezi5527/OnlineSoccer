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

    @Test
    fun zatReport_liestAbschnittNamenZatUndSaison() {
        val report = repo.parseZatReport(dump("zatreport_zat5"))
        assertEquals(5, report.zat)
        assertEquals(24, report.saison)
        assertTrue("Einnahmen absatz vorhanden", report.einnahmen.isNotEmpty())
        assertTrue("Trainingserfolge absatz vorhanden", report.trainingserfolge.isNotEmpty())
    }

    @Test
    fun zatReport_liestWaehlbareZatsUndSaisonsAusFormular() {
        val report = repo.parseZatReport(dump("zatreport_zat5"))
        assertTrue("ZAT-Optionen vorhanden", report.zats.containsAll(listOf(1, 5, 6)))
        assertTrue("Saison-Optionen vorhanden", report.saisons.containsAll(listOf(23, 24)))
        assertEquals("aktuelle Saison ist markiert", 24, report.saisons.lastOrNull())
    }

    @Test
    fun zatReport_liestEinnahmenUndGesamtsumme() {
        val report = repo.parseZatReport(dump("zatreport_zat5"))
        val labels = report.einnahmen.map { it.label }
        assertTrue("Zuschauereinnahmen", labels.contains("Zuschauereinnahmen"))
        assertTrue("Jugendförderung", labels.contains("Jugendförderung"))
        val gesamt = report.einnahmen.firstOrNull { it.label == "Gesamtsumme" }
        assertEquals("238.500 Euro", gesamt?.wert)
    }

    @Test
    fun zatReport_liestTrainingserfolgeMitPositionUndWertAenderung() {
        val report = repo.parseZatReport(dump("zatreport_zat5"))
        val bommel = report.trainingserfolge.firstOrNull { it.pid == 120513L }
        assertEquals("Kuldar Mitt", bommel?.name)
        assertEquals("TOR", bommel?.position)
        assertEquals("Zuverlässigkeit erfolglos", bommel?.beschreibung)
        assertEquals("(55 → 55)", bommel?.wert)

        val erfolgreich = report.trainingserfolge.first { it.name == "Dhuntiar Konechi" }
        assertEquals("ABW", erfolgreich.position)
        assertEquals("Geschwindigkeit erfolgreich", erfolgreich.beschreibung)
        assertEquals("(19 → 20)", erfolgreich.wert)
    }

    @Test
    fun zatReport_handhabtZeilenOhneWertAenderung() {
        val report = repo.parseZatReport(dump("zatreport_zat4"))
        val erfahrung = report.trainingserfolge.firstOrNull { it.beschreibung == "Erfahrung gestiegen" }
        assertTrue("Erfahrung-Eintrag vorhanden", erfahrung != null)
        assertEquals(null, erfahrung?.wert)
        assertTrue("ZAT 4 Report wertet ZAT", report.zat == 4)
    }
}