package com.onlinesoccer.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests für den reinen Tausch-Helper und die Ersatzbank-Erkennung
 * (Regresssion: „zwei Positionen antippen muss Spieler tauschen“).
 */
class AufstellungLogikTest {

    private fun spieler(pid: Long, slot: AufstellungSlot?, raSlot: String?): AufstellungSpieler =
        AufstellungSpieler(
            pid = pid,
            name = "S$pid",
            nummer = "",
            alter = 25,
            skill = 80.0,
            opti = 90.0,
            fit = 99,
            mor = 88,
            position = SpielerPosition.STU,
            slot = slot,
            raSlot = raSlot,
        )

    @Test
    fun tauschePlaetze_tauschtSlotUndRaSlot() {
        val spieler = listOf(
            spieler(1, AufstellungSlot.Ersatz(0), "U"),
            spieler(2, AufstellungSlot.Ersatz(1), "V"),
        )

        val ergebnis = spieler.tauschePlaetze(1, 2)

        assertEquals(AufstellungSlot.Ersatz(1), ergebnis[0].slot)
        assertEquals("V", ergebnis[0].raSlot)
        assertEquals(AufstellungSlot.Ersatz(0), ergebnis[1].slot)
        assertEquals("U", ergebnis[1].raSlot)
    }

    @Test
    fun tauschePlaetze_tauschtFeldMitBank() {
        val spieler = listOf(
            spieler(1, AufstellungSlot.Feld(3, 2), "A"),
            spieler(2, AufstellungSlot.Ersatz(4), "Y"),
        )

        val ergebnis = spieler.tauschePlaetze(1, 2)

        assertEquals(AufstellungSlot.Ersatz(4), ergebnis[0].slot)
        assertEquals("Y", ergebnis[0].raSlot)
        assertEquals(AufstellungSlot.Feld(3, 2), ergebnis[1].slot)
        assertEquals("A", ergebnis[1].raSlot)
    }

    @Test
    fun tauschePlaetze_mitNullSlot() {
        val spieler = listOf(
            spieler(1, AufstellungSlot.Ersatz(0), "U"),
            spieler(2, null, null),
        )

        val ergebnis = spieler.tauschePlaetze(1, 2)

        assertNull(ergebnis[0].slot)
        assertNull(ergebnis[0].raSlot)
        assertEquals(AufstellungSlot.Ersatz(0), ergebnis[1].slot)
        assertEquals("U", ergebnis[1].raSlot)
    }

    @Test
    fun tauschePlaetze_unbekanntePidAendertNichts() {
        val spieler = listOf(spieler(1, AufstellungSlot.Ersatz(0), "U"))

        assertEquals(spieler, spieler.tauschePlaetze(1, 999L))
        assertEquals(spieler, spieler.tauschePlaetze(999L, 1L))
    }

    @Test
    fun spielerAufBankSlot_erkenntPerSlotUndRaSlot() {
        val aufstellung = Aufstellung(
            zat = null,
            spielart = null,
            gegner = null,
            status = null,
            spieler = listOf(
                spieler(1, AufstellungSlot.Ersatz(0), "U"),
                spieler(2, null, "W"),
            ),
        )

        assertEquals(1L, aufstellung.spielerAufBankSlot(0)?.pid)
        assertEquals(2L, aufstellung.spielerAufBankSlot(2)?.pid)
        assertNull(aufstellung.spielerAufBankSlot(1))
        assertNull(aufstellung.spielerAufBankSlot(5))
        assertNull(aufstellung.spielerAufBankSlot(9))
    }

    @Test
    fun toBetaJson_ersatztorwartLiegtAufServerIndexMinus5() {
        // Der Ersatztorwart sitzt im App-Modell auf Index 0 (Buchstabe U). Beim
        // Speichern im Beta-Format muss er auf die Server-Position -5 (ersatz_goal)
        // geschrieben werden, die übrigen Feld-Ersatzplätze gespiegelt folgen.
        val ersatz = listOf(
            spieler(41, AufstellungSlot.Ersatz(0), "U"),
            spieler(42, AufstellungSlot.Ersatz(1), "V"),
            spieler(43, AufstellungSlot.Ersatz(2), "W"),
            spieler(44, AufstellungSlot.Ersatz(3), "X"),
            spieler(45, AufstellungSlot.Ersatz(4), "Y"),
            spieler(46, AufstellungSlot.Ersatz(5), "Z"),
        )
        val feld = (1..10).map { spieler(10L + it, AufstellungSlot.Feld(it, 1), "") }
        val aufstellung = Aufstellung(
            zat = null,
            spielart = null,
            gegner = null,
            status = null,
            spieler = listOf(spieler(1, AufstellungSlot.Torwart, "T")) + feld + ersatz,
        )

        val json = AufstellungSerialisierung.toBetaJson(aufstellung)

        assertTrue("Ersatztorwart (U) => Server-Position -5", json.contains("[41,-5,-1]"))
        assertTrue("Feld-Ersatz V => Server-Position -4", json.contains("[42,-4,-1]"))
        assertTrue("Feld-Ersatz Z => Server-Position 0", json.contains("[46,0,-1]"))
    }
}