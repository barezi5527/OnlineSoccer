package com.onlinesoccer.app.feature.statistik

import com.onlinesoccer.app.data.model.ScoutSpieler
import com.onlinesoccer.app.data.model.SpielerscoutDaten
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spielerscout – Kategorie-Metadaten und Zuordnung der Spielerlisten. */
class SpielerscoutTest {

    private val spieler = listOf(
        ScoutSpieler(nr = 1, pid = 1L, name = "A", teamId = null),
        ScoutSpieler(nr = 2, pid = 2L, name = "B", teamId = null),
    )

    @Test
    fun wertKopfProKategorie() {
        assertEquals("Opti Skill", ScoutKategorie.TALENTE.wertKopf)
        assertEquals("Skill", ScoutKategorie.ALLROUNDER.wertKopf)
        assertEquals("Opti Skill", ScoutKategorie.TOP_SPIELER.wertKopf)
        assertEquals("Opti Skill", ScoutKategorie.SUPERSTAR.wertKopf)
        assertEquals("Marktwert", ScoutKategorie.WERTANLAGE.wertKopf)
        assertEquals("Opti Skill", ScoutKategorie.VETERAN.wertKopf)
        assertTrue("Wertanlagen-Spalte breiter", ScoutKategorie.WERTANLAGE.wertSpalte > ScoutKategorie.TOP_SPIELER.wertSpalte)
    }

    @Test
    fun kategorieHatBeschreibung() {
        ScoutKategorie.entries.forEach { kategorie ->
            assertTrue("Titel für ${kategorie.name}", kategorie.titel.isNotBlank())
            assertTrue("Beschreibung für ${kategorie.name}", kategorie.beschreibung.isNotBlank())
        }
    }

    @Test
    fun spielerProKategorie() {
        val daten = SpielerscoutDaten(
            talente = spieler,
            allrounder = listOf(spieler[1]),
            topSpieler = listOf(spieler[0]),
            superstars = emptyList(),
            veteranen = listOf(spieler[1]),
            wertanlagen = listOf(spieler[1]),
        )

        assertEquals(spieler, kategorieSpieler(daten, ScoutKategorie.TALENTE))
        assertEquals(listOf(spieler[1]), kategorieSpieler(daten, ScoutKategorie.ALLROUNDER))
        assertEquals(listOf(spieler[0]), kategorieSpieler(daten, ScoutKategorie.TOP_SPIELER))
        assertTrue(kategorieSpieler(daten, ScoutKategorie.SUPERSTAR).isEmpty())
        assertEquals(listOf(spieler[1]), kategorieSpieler(daten, ScoutKategorie.VETERAN))
        assertEquals(listOf(spieler[1]), kategorieSpieler(daten, ScoutKategorie.WERTANLAGE))
    }
}