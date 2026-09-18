package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.BerichtEreignis
import com.onlinesoccer.app.data.model.BerichtEreignisTyp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElfKartenTest {

    private val namen = setOf("Frank Bauer", "Max Müller", "Tim Weber")

    private fun ereignis(minute: String, text: String, typ: BerichtEreignisTyp) =
        BerichtEreignis(minute = minute, text = text, typ = typ)

    @Test
    fun kartenEreignisse_erkenntKarteFuerName() {
        val ereignisse = listOf(
            ereignis("65", "Gelbe Karte für Frank Bauer", BerichtEreignisTyp.GELBE_KARTE),
        )

        val ergebnis = ElfAuswertung.kartenEreignisse(ereignisse, namen)

        assertEquals(1, ergebnis.size)
        val (index, name, typ) = ergebnis.first()
        assertEquals(0, index)
        assertEquals("Frank Bauer", name)
        assertEquals(BerichtEreignisTyp.GELBE_KARTE, typ)
    }

    @Test
    fun kartenEreignisse_erkenntKassiertDafuer() {
        val ereignisse = listOf(
            ereignis("70", "FREISTOSS Frank Bauer kassiert dafür die gelbe Karte", BerichtEreignisTyp.GELBE_KARTE),
            ereignis("71", "Max Müller kassiert dafür die rote Karte", BerichtEreignisTyp.ROTE_KARTE),
        )

        val ergebnis = ElfAuswertung.kartenEreignisse(ereignisse, namen)

        assertEquals(2, ergebnis.size)
        assertEquals(0, ergebnis[0].first)
        assertEquals("Frank Bauer", ergebnis[0].second)
        assertEquals(BerichtEreignisTyp.GELBE_KARTE, ergebnis[0].third)
        assertEquals(1, ergebnis[1].first)
        assertEquals("Max Müller", ergebnis[1].second)
        assertEquals(BerichtEreignisTyp.ROTE_KARTE, ergebnis[1].third)
    }

    @Test
    fun kartenEreignisse_ignoriertNichtKartenUndMehrdeutiges() {
        val ereignisse = listOf(
            ereignis("5", "Anpfiff", BerichtEreignisTyp.SONSTIGES),
            ereignis("60", "Gelbe Karte", BerichtEreignisTyp.GELBE_KARTE),
        )

        assertTrue(ElfAuswertung.kartenEreignisse(ereignisse, namen).isEmpty())
    }

    @Test
    fun kartenJeSpieler_rotUebersteuertGelb() {
        val ereignisse = listOf(
            ereignis("65", "Gelbe Karte für Frank Bauer", BerichtEreignisTyp.GELBE_KARTE),
            ereignis("85", "Zweite gelbe Karte für Frank Bauer", BerichtEreignisTyp.ROTE_KARTE),
        )

        val ergebnis = ElfAuswertung.kartenJeSpieler(ereignisse, namen)

        assertEquals(BerichtEreignisTyp.ROTE_KARTE, ergebnis["frank bauer"])
    }

    @Test
    fun kartenJeSpieler_reineGelbeBleibtGelb() {
        val ereignisse = listOf(
            ereignis("65", "Gelbe Karte für Tim Weber", BerichtEreignisTyp.GELBE_KARTE),
        )

        val ergebnis = ElfAuswertung.kartenJeSpieler(ereignisse, namen)

        assertEquals(BerichtEreignisTyp.GELBE_KARTE, ergebnis["tim weber"])
    }

    @Test
    fun kartenEreignisse_zaehltGelbUndRotGetrennt() {
        val ereignisse = listOf(
            ereignis("30", "Gelbe Karte für Tim Weber", BerichtEreignisTyp.GELBE_KARTE),
            ereignis("75", "Rote Karte für Tim Weber", BerichtEreignisTyp.ROTE_KARTE),
        )

        assertEquals(2, ElfAuswertung.kartenEreignisse(ereignisse, namen).size)
    }

    @Test
    fun verletzteSpieler_erkenntVerletztenAmIndex() {
        val ereignisse = listOf(
            ereignis("5", "Anpfiff", BerichtEreignisTyp.SONSTIGES),
            ereignis("55", "Verletzung von Max Müller, muss vom Platz", BerichtEreignisTyp.VERLETZUNG),
            ereignis("60", "Weitermachen", BerichtEreignisTyp.SONSTIGES),
        )

        val ergebnis = ElfAuswertung.verletzteSpieler(ereignisse, namen)

        assertEquals(mapOf(1 to "Max Müller"), ergebnis)
    }

    @Test
    fun verletzteSpieler_ignoriertMehrdeutigeEreignisse() {
        val ereignisse = listOf(
            ereignis("55", "Nach dem Zusammenprall von Frank Bauer und Max Müller müssen beide vom Platz", BerichtEreignisTyp.VERLETZUNG),
        )

        assertTrue(ElfAuswertung.verletzteSpieler(ereignisse, namen).isEmpty())
    }

    @Test
    fun verletzteJeSpieler_fuerNamensEinfarbung() {
        val ereignisse = listOf(
            ereignis("40", "Verletzung von Tim Weber, muss vom Platz", BerichtEreignisTyp.VERLETZUNG),
        )

        val ergebnis = ElfAuswertung.verletzteJeSpieler(ereignisse, namen)

        assertEquals(1, ergebnis.size)
        assertEquals(BerichtEreignisTyp.VERLETZUNG, ergebnis["tim weber"])
    }
}