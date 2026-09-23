package com.onlinesoccer.app.data.repository

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressionstests für die neuen Teaminformationen-Unterpunkte
 * (`showteam.php?s=6..10`) gegen echte Server-Dumps.
 */
class TeaminformationenRepositoryParseTest {

    private val repo = TeamRepository(okhttp3.OkHttpClient(), ZugabgabeRepository(okhttp3.OkHttpClient()))

    private fun dump(name: String): String =
        File("src/test/resources/dumps/$name.html").readText()

    @Test
    fun menu_listetAlleZwölfUnterpunkte() {
        val menu = repo.parseTeaminformationenMenu(dump("showteam_s0"))
        // Teamübersicht (s=0) ist eine „Mannschaft" in den Team-Bereich verschoben und
        // Tabellenplätze wurden entfernt – es bleiben 10 Teaminformations-Punkte.
        assertEquals("10 Unterpunkte im Team-Menü", 10, menu.size)
        assertTrue(
            "Teamübersicht (s=0) entfernt",
            menu.none { it.showteamS == "0" },
        )
        assertTrue(
            "Tabellenplätze entfernt",
            menu.none { it.label.contains("Tabellenplätze", true) },
        )
        assertTrue(menu.any { it.label.contains("Saisonplan", true) && it.showteamS == "6" })
        assertTrue(menu.any { it.label.contains("Vereinshistorie", true) && it.showteamS == "7" })
        assertTrue(menu.any { it.label.contains("Transferhistorie", true) && it.showteamS == "8" })
        assertTrue(menu.any { it.label.contains("Leihhistorie", true) && it.showteamS == "9" })
        assertTrue(menu.any { it.label.contains("Saisonhistorie", true) && it.showteamS == "10" })
    }

    @Test
    fun menu_parstAuchFremdenVerein_unterTeaminfo() {
        // Fremder Verein (im Ligatabellen-Link „javascript:teaminfo(3449)") öffnet st.php?c=3449.
        // Dessen Menü liefert st.php-Links mit c=<teamId>; s=0 und Tabellenplätze bleiben entfernt.
        val menu = repo.parseTeaminformationenMenu(dump("st_3449"))

        assertTrue("Fremdverein-Menü nicht leer", menu.isNotEmpty())
        assertTrue(
            "Kein Teamübersicht (s=0) im Fremdverein-Menü",
            menu.none { it.showteamS == "0" },
        )
        assertTrue(
            "Kein Tabellenplätze-Eintrag (fremd)",
            menu.all { it.tabellenplatzTeamId == null },
        )
        val teambasis = menu.first { it.label.contains("Vertragsdaten", true) }
        assertEquals("Vertragsdaten → st.php?s=1", "1", teambasis.showteamS)
        assertTrue(
            "link auf st.php?c=3449",
            teambasis.path.startsWith("st.php", ignoreCase = true) && teambasis.path.contains("c=3449"),
        )
    }

    @Test
    fun saisonplan_liestSaisonsUndSpiele() {
        val daten = repo.parseSaisonplan(dump("showteam_s6"))
        assertEquals("Saisons 1..24", (1..24).toList(), daten.saisons.sorted())
        assertEquals("Standard: gewählte Saison 24", 24, daten.gewaehlteSaison)
        assertTrue("Saisonplan enthält Spiele", daten.eintraege.isNotEmpty())

        val spiel = daten.eintraege.first { it.gegnerTeamId != null }
        assertTrue("ZAT numerisch", spiel.zat.toIntOrNull() != null)
        assertTrue("Spielart vorhanden", spiel.spielart.isNotBlank())
        assertTrue("Gegner vorhanden", spiel.gegner.isNotBlank())
        assertTrue("Ergebnis vorhanden", spiel.ergebnis.isNotBlank())
    }

    @Test
    fun saisonplan_mitSaisonParameter() {
        val daten = repo.parseSaisonplan(dump("showteam_s6_s1"))
        assertEquals("Saison 1 gewählt", 1, daten.gewaehlteSaison)
        assertEquals("Saisons 1..24", (1..24).toList(), daten.saisons.sorted())

        // Saison 1 besteht aus spielfreien Runden – echte Begegnungen dürfen leer sein,
        // die vereinzelten Spiele ohne Gegner werden gefiltert.
        daten.eintraege.forEach { assertTrue(it.gegner.isNotBlank()) }
    }

    @Test
    fun saisonplan_berichtLinkWirdExtrahierer() {
        val daten = repo.parseSaisonplan(dump("showteam_s6"))
        val mitBericht = daten.eintraege.firstOrNull { it.berichtUrl != null }
        assertNotNull("Bericht-Link vorhanden", mitBericht)
        assertTrue(
            "os_bericht-Parameter",
            mitBericht!!.berichtUrl!!.startsWith("3449,"),
        )
    }

    @Test
    fun vereinshistorie_liestAlleSpalten() {
        val eintraege = repo.parseVereinshistorie(dump("showteam_s7"))
        assertTrue("Vereinshistorie nicht leer", eintraege.isNotEmpty())

        val aktuell = eintraege.first { it.saison == "24" }
        assertEquals("0", aktuell.zat)
        assertEquals("36", aktuell.spielerAnzahl)
        assertEquals("50.18", aktuell.avgSkill)
        assertEquals("74.15", aktuell.avgOpti)
        assertEquals("24.58", aktuell.avgAlter)
        assertEquals("13.627.689", aktuell.avgMW)
        assertEquals("490.596.832", aktuell.sumMW)
        assertEquals("168.014", aktuell.avgGehalt)
        assertEquals("6.048.497", aktuell.sumGehalt)
        assertEquals("Bahri Er", aktuell.manager)
    }

    @Test
    fun transferhistorie_liestBloecke() {
        val bloecke = repo.parseTransferhistorie(dump("showteam_s8"))
        assertTrue("Transferblöcke vorhanden", bloecke.size >= 2)

        val erster = bloecke.first()
        assertTrue(erster.datum.startsWith("31.08.2026"))
        assertEquals("SC Viktoria Ulm", erster.team1)
        assertEquals(3449L, erster.team1Id)
        assertEquals("Dschibuti", erster.team2)
        assertEquals("Dschibuti hat kein Team, ID null", null, erster.team2Id)
        assertTrue("Details je Block vorhanden", erster.details.any { it.contains("Bargeld") })
    }

    @Test
    fun leihhistorie_liestZeilen() {
        val eintraege = repo.parseLeihhistorie(dump("showteam_s9"))
        assertEquals(1, eintraege.size)

        val leih = eintraege.first()
        assertEquals("14.07.2026 22:04", leih.datum)
        assertEquals("Lars Vincez", leih.spieler)
        assertEquals(97944L, leih.spielerPid)
        assertEquals("Adler Koblenz", leih.von)
        assertEquals("SC Viktoria Ulm", leih.zu)
        assertEquals("146.561 Euro", leih.zahlung)
        assertEquals("36 ZATs", leih.dauer)
    }

    @Test
    fun saisonhistorie_liestZeilen() {
        val eintraege = repo.parseSaisonhistorie(dump("showteam_s10"))
        assertTrue("Saisonhistorie nicht leer", eintraege.isNotEmpty())

        val aktuell = eintraege.first()
        assertEquals("23", aktuell.saison)
        assertEquals("2. Liga B", aktuell.liga)
        assertTrue(aktuell.tabelle.contains("Platz"))
        assertTrue(aktuell.pokal.isNotBlank())
        assertTrue(aktuell.ose.isNotBlank())
        assertTrue(aktuell.osc.isNotBlank())
    }
}