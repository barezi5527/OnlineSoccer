package com.onlinesoccer.app.data.repository

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressionstests für die Statistik-Parser (`topscorer.php`) gegen einen
 * echten Server-Dump (app/src/test/resources/dumps/topscorer.html).
 */
class StatistikRepositoryParseTest {

    private val repo = StatistikRepository(okhttp3.OkHttpClient())

    private fun dump(name: String): String =
        File("src/test/resources/dumps/$name.html").readText()

    @Test
    fun topscorer_liestAlleDatenDerErstenZeile() {
        val daten = repo.parseTopscorer(dump("topscorer"))

        val erste = daten.zeilen.firstOrNull()
        assertNotNull("Zeilen vorhanden", erste)
        assertEquals(1, erste?.nr)
        assertEquals("Esben Frederiksen", erste?.name)
        assertEquals(166282L, erste?.pid)
        assertEquals("STU", erste?.position)
        assertEquals("21", erste?.alter)
        assertEquals("46.94", erste?.skill)
        assertEquals("77.56", erste?.opti)
        assertEquals("DEN", erste?.land)
        assertEquals(992L, erste?.teamId)
        assertEquals("SC St.Pauli", erste?.team)
        assertEquals("1. Liga Deutschland", erste?.liga)
        assertEquals("32.00", erste?.wert)
    }

    @Test
    fun topscorer_ueberspringtKopfzeile() {
        val daten = repo.parseTopscorer(dump("topscorer"))

        assertTrue("keine Kopfzeile als Datenzeile", daten.zeilen.none { it.name == "Name" || it.nr == null })
        assertTrue("alle Zeilen haben eine Nummer", daten.zeilen.all { it.nr != null })
    }

    @Test
    fun topscorer_entferntDoppelteSpieler() {
        val daten = repo.parseTopscorer(dump("topscorer"))

        val pids = daten.zeilen.mapNotNull { it.pid }
        assertEquals("kein Spieler doppelt", pids.size, pids.distinct().size)
        assertTrue("Doppelgänger wurden entfernt (91 eindeutige von 100 Einträgen)", daten.zeilen.size < 100)
    }

    // ---------------------------------------------------------------- Fairplay

    @Test
    fun fairplay_liestAlleDatenDerErstenZeile() {
        val zeilen = repo.parseFairplay(dump("fairplay"))

        val erste = zeilen.firstOrNull()
        assertNotNull("Zeilen vorhanden", erste)
        assertEquals(1, erste?.nr)
        assertEquals(1212L, erste?.teamId)
        assertEquals("Dinamo Istra-Pula", erste?.team)
        assertEquals(0, erste?.gelb)
        assertEquals(0, erste?.gelbRot)
        assertEquals(0, erste?.rot)
        assertEquals(38, erste?.spiele)
        assertEquals(0.0, erste?.punkte)
    }

    @Test
    fun fairplay_ueberspringtKopfzeile() {
        val zeilen = repo.parseFairplay(dump("fairplay"))

        assertTrue("keine Kopfzeile als Datenzeile", zeilen.none { it.team == "Name" || it.nr == null })
        assertTrue("alle Zeilen haben eine Nummer", zeilen.all { it.nr != null })
        assertTrue("alle Zeilen haben eine Team-ID", zeilen.all { it.teamId != null })
    }

    @Test
    fun fairplay_punkteMitNachkommastellen() {
        val zeilen = repo.parseFairplay(dump("fairplay"))

        assertEquals(0.0263, zeilen.firstOrNull { it.team == "Arsenal Donezk" }?.punkte)
        assertEquals(0.0526, zeilen.firstOrNull { it.team == "Newcastle Glory" }?.punkte)
        assertTrue("geteilte Platzierungen bleiben erhalten", zeilen.size >= 72)
    }

    @Test
    fun topscorer_liestFormularoptionen() {
        val daten = repo.parseTopscorer(dump("topscorer"))

        assertEquals(53, daten.laender.size)
        assertEquals(8, daten.ligas.size)
        assertEquals(10, daten.statistiken.size)
        assertEquals(7, daten.positionen.size)
        assertEquals(24, daten.saisons.size)
        assertEquals(5, daten.arten.size)
        assertEquals("Alle", daten.laender.first { it.id == "0" }.label)
        assertTrue("aktuelle Saison 24 vorhanden", daten.saisons.any { it.id == "24" })
        assertTrue("Statistik-Default Tore vorhanden", daten.statistiken.any { it.id == "1" && it.label == "Tore" })
    }

    // ---------------------------------------------------------------- Topspieler

    @Test
    fun topspieler_liestAlleDatenDerErstenZeile() {
        val daten = repo.parseTopspieler(dump("statspieler"))

        val erste = daten.zeilen.firstOrNull()
        assertNotNull("Zeilen vorhanden", erste)
        assertEquals(1, erste?.nr)
        assertEquals("Guido Knoche", erste?.name)
        assertEquals(104701L, erste?.pid)
        assertEquals("STU", erste?.position)
        assertEquals("31", erste?.alter)
        assertEquals("GER", erste?.nation)
        assertEquals(613L, erste?.teamId)
        assertEquals("SGE Frankfurt", erste?.team)
        assertEquals("99,67", erste?.wert)
    }

    @Test
    fun topspieler_ueberspringtKopfzeile() {
        val daten = repo.parseTopspieler(dump("statspieler"))

        assertTrue("keine Kopfzeile als Datenzeile", daten.zeilen.none { it.name == "Spieler" || it.nr == null })
        assertTrue("alle Zeilen haben eine Nummer", daten.zeilen.all { it.nr != null })
        assertEquals(10, daten.zeilen.size)
    }

    @Test
    fun topspieler_positionAusEigenerSpalte_liestAlleDumps() {
        val gueltig = setOf("TOR", "ABW", "DMI", "MIT", "OMI", "STU")
        for (name in listOf("statspieler", "statspieler_marktwert", "statspieler_tor")) {
            val daten = repo.parseTopspieler(dump(name))
            assertTrue("Dump $name hat Zeilen", daten.zeilen.isNotEmpty())
            assertTrue("Dump $name Positionen gültig", daten.zeilen.all { it.position in gueltig })
        }
    }

    @Test
    fun topspieler_marktwert_liestBetraege() {
        val daten = repo.parseTopspieler(dump("statspieler_marktwert"))

        assertEquals(100, daten.zeilen.size)
        assertTrue("Beträge mit Tausenderpunkten", daten.zeilen.all { it.wert.matches(Regex("\\d[\\d.]*")) })
    }

    @Test
    fun topspieler_liestFormularoptionen() {
        val daten = repo.parseTopspieler(dump("statspieler"))

        assertEquals(38, daten.laender.size)
        assertEquals(8, daten.ligas.size)
        assertEquals(4, daten.statistiken.size)
        assertEquals(7, daten.positionen.size)
        assertEquals(8, daten.anzeigen.size)
        assertEquals("Alle", daten.laender.first { it.id == "0" }.label)
        assertEquals("Opt Skill", daten.statistiken.first { it.id == "16" }.label)
        assertEquals("Marktwert", daten.statistiken.first { it.id == "33" }.label)
    }

    // ---------------------------------------------------------------- Spielersuche

    @Test
    fun suche_liestAlleDatenDerErstenZeile() {
        val zeilen = repo.parseSucheTreffer(dump("spielersuche_result"))

        val erste = zeilen.firstOrNull()
        assertNotNull("Zeilen vorhanden", erste)
        assertEquals(1, erste?.nr)
        assertEquals("Frano Tito", erste?.name)
        assertEquals(106419L, erste?.pid)
        assertEquals("DMI", erste?.position)
        assertEquals("32", erste?.alter)
        assertEquals("CRO", erste?.nation)
        assertEquals(63L, erste?.teamId)
        assertEquals("OSV Rostock", erste?.team)
    }

    @Test
    fun suche_ueberspringtKopfzeile() {
        val zeilen = repo.parseSucheTreffer(dump("spielersuche_result"))

        assertTrue("keine Kopfzeile als Datenzeile", zeilen.none { it.name == "Name" || it.nr == null })
        assertTrue("alle Zeilen haben einen Spieler und eine ID", zeilen.all { it.pid != null && it.name.isNotBlank() })
        assertTrue("Spieler vorhanden", zeilen.size >= 100)
    }

    @Test
    fun suche_positionAusEigenerSpalte() {
        val zeilen = repo.parseSucheTreffer(dump("spielersuche_result"))
        val gueltig = setOf("TOR", "ABW", "DMI", "MIT", "OMI", "STU")

        assertTrue("Positionen aus Positionsspalte", zeilen.all { it.position in gueltig })
        assertEquals("STU", zeilen.firstOrNull { it.name == "Davy Van Roy" }?.position)
        assertEquals("TOR", zeilen.firstOrNull { it.name == "Vedran Prednik" }?.position)
    }

    // ---------------------------------------------------------------- Spielerscout

    @Test
    fun scout_liestAlleDatenDerErstenZeile() {
        val zeilen = repo.parseScoutTreffer(dump("scout_superstar"))

        val erste = zeilen.firstOrNull()
        assertNotNull("Zeilen vorhanden", erste)
        assertEquals(1, erste?.nr)
        assertEquals("Matthias Prohaska", erste?.name)
        assertEquals(111822L, erste?.pid)
        assertEquals("MIT", erste?.position)
        assertEquals("31", erste?.alter)
        assertEquals(628L, erste?.teamId)
        assertEquals("Admira Pregarten", erste?.team)
        assertEquals("102,33", erste?.wert)
    }

    @Test
    fun scout_ueberspringtKopfzeileUndLiestKennzahl() {
        val zeilen = repo.parseScoutTreffer(dump("scout_superstar"))

        assertTrue("keine Kopfzeile als Datenzeile", zeilen.none { it.name == "Name" || it.nr == null })
        assertTrue("alle Zeilen haben Spieler und Wert", zeilen.all { it.pid != null && it.wert.matches(Regex("\\d[\\d,]*")) })
        assertTrue("Superstars vorhanden", zeilen.size in 20..30)
    }

    @Test
    fun scout_talenteSindJungUndAllrounderHabenSkill() {
        val talente = repo.parseScoutTreffer(dump("scout_talente"))
        val allrounder = repo.parseScoutTreffer(dump("scout_allrounder"))

        assertTrue("Talente vorhanden", talente.size >= 30)
        assertTrue("Talente jung (≤ 23)", talente.all { (it.alter.toIntOrNull() ?: 0) <= 23 })
        assertTrue("Allrounder vorhanden", allrounder.size >= 30)
        assertTrue("Allrounder führen Skill-Werte", allrounder.all { it.wert.isNotBlank() })
    }

    @Test
    fun scout_entferntDopplungenNachPrioritaet() {
        val s = { id: Long, name: String ->
            com.onlinesoccer.app.data.model.ScoutSpieler(nr = 1, pid = id, name = name, teamId = null)
        }
        val daten = repo.kategorisiereSpielerscout(
            talente = listOf(s(1, "Talent"), s(2, "Talent 2")),
            allrounder = listOf(s(1, "Talent")),
            topSpieler = listOf(s(3, "Top")),
            superstars = listOf(s(1, "Talent"), s(4, "Star")),
            veteranen = listOf(s(4, "Star")),
            wertanlagen = listOf(s(3, "Top")),
        )

        assertEquals("Superstars erhalten", listOf(1L, 4L), daten.superstars.map { it.pid })
        assertTrue("Veteranen ohne Dopplung", daten.veteranen.isEmpty())
        assertEquals("Talente ohne Dopplung", listOf(2L), daten.talente.map { it.pid })
        assertEquals("Top-Spieler", listOf(3L), daten.topSpieler.map { it.pid })
        assertTrue("Wertanlagen ohne Dopplung", daten.wertanlagen.isEmpty())
        assertTrue("Allrounder ohne Dopplung", daten.allrounder.isEmpty())
    }

    @Test
    fun scout_wertanlagenNachMarktwert() {
        val zeilen = repo.parseScoutTreffer(dump("scout_wertanlagen"))

        val erste = zeilen.firstOrNull()
        assertNotNull("Zeilen vorhanden", erste)
        assertEquals(1, erste?.nr)
        assertEquals("Tom Ullrich", erste?.name)
        assertEquals("TOR", erste?.position)
        assertEquals("35", erste?.alter)
        assertEquals("8.627.014", erste?.wert)
        assertTrue("Wertanlagen vorhanden", zeilen.size >= 30)
        assertTrue("Marktwert aufsteigend", zeilen.map { marktwert(it.wert) }.zipWithNext().all { (a, b) -> a <= b })
        assertTrue(
            "Marktwert als Kennzahl (Punkt-Tausender)",
            zeilen.take(30).all { it.wert.matches(Regex("\\d{1,3}(\\.\\d{3})+")) },
        )
    }

    private fun marktwert(roh: String): Long = roh.replace(".", "").toLongOrNull() ?: 0L

    @Test
    fun scout_begrenztJeKategorieAuf30() {
        val s = { id: Long ->
            com.onlinesoccer.app.data.model.ScoutSpieler(nr = 1, pid = id, name = "Spieler $id", teamId = null)
        }
        val daten = repo.kategorisiereSpielerscout(
            talente = (1L..40L).map { s(it) },
            allrounder = emptyList(),
            topSpieler = emptyList(),
            superstars = emptyList(),
            veteranen = emptyList(),
        )

        assertEquals(30, daten.talente.size)
        assertEquals((1L..30L).toList(), daten.talente.map { it.pid })
    }
}