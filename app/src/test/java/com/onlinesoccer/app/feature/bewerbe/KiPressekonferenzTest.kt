package com.onlinesoccer.app.feature.bewerbe

import com.onlinesoccer.app.data.model.BerichtEreignis
import com.onlinesoccer.app.data.model.BerichtEreignisTyp
import com.onlinesoccer.app.data.model.BerichtStatistik
import com.onlinesoccer.app.data.model.SpielBericht
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KiPressekonferenzTest {

    @Test
    fun siegEnthaeltTeamsUndErgebnis() {
        val aussage = KiPressekonferenz.aussage(bericht(ergebnis = "2:1"))

        assertTrue(aussage.ueberschrift.isNotBlank())
        assertTrue(aussage.text.contains("SC Viktoria Ulm"))
        assertTrue(aussage.text.contains("SC Kaiserslautern"))
        assertTrue(aussage.text.contains("stolz"))
        assertFalse(aussage.text.contains("weh"))
    }

    @Test
    fun niederlageEnthaeltEnttaeuschung() {
        val aussage = KiPressekonferenz.aussage(bericht(ergebnis = "0:3"))

        assertTrue(aussage.text.contains("weh"))
        assertTrue(aussage.text.contains("Cleverness"))
    }

    @Test
    fun unentschiedenErwaehntPunkt() {
        val aussage = KiPressekonferenz.aussage(bericht(ergebnis = "1:1"))

        assertTrue(aussage.text.contains("Ein Punkt"))
    }

    @Test
    fun ohneErgebnisBleibtDieAussageStandhaft() {
        val aussage = KiPressekonferenz.aussage(bericht(ergebnis = null))

        assertTrue(aussage.ueberschrift.isNotBlank())
        assertTrue(aussage.text.isNotBlank())
    }

    @Test
    fun roteKarteFliesstInDieStellungnahmeEin() {
        val aussage = KiPressekonferenz.aussage(
            bericht(
                ergebnis = "2:1",
                ereignisse = listOf(
                    BerichtEreignis(minute = "78", text = "Rote Karte", typ = BerichtEreignisTyp.ROTE_KARTE),
                ),
            ),
        )

        assertTrue(aussage.text.contains("Rote Karte"))
    }

    @Test
    fun elfmeterWirdErwaehnt() {
        val aussage = KiPressekonferenz.aussage(
            bericht(
                ergebnis = "1:0",
                ereignisse = listOf(
                    BerichtEreignis(minute = "55", text = "Elfmeter", typ = BerichtEreignisTyp.ELFMETER),
                ),
            ),
        )

        assertTrue(aussage.text.contains("Elfmeterpunkt"))
    }

    @Test
    fun hoherBallbesitzWirdErwaehnt() {
        val aussage = KiPressekonferenz.aussage(bericht(ergebnis = "2:1", ballbesitz = "68 : 32"))

        assertTrue(aussage.text.contains("Kontrolle"))
    }

    @Test
    fun ganzEnthaeltUeberschriftUndText() {
        val aussage = KiPressekonferenz.aussage(bericht(ergebnis = "2:1"))

        val ganz = aussage.ganz()
        assertTrue(ganz.contains(aussage.ueberschrift))
        assertTrue(ganz.contains("\n\n"))
        assertTrue(ganz.contains(aussage.text))
    }

    @Test
    fun gasttrainerSprechenAusGastPerspektive() {
        val heim = KiPressekonferenz.aussage(bericht(ergebnis = "1:3"), KiPressekonferenz.Trainer.HEIM)
        val gast = KiPressekonferenz.aussage(bericht(ergebnis = "1:3"), KiPressekonferenz.Trainer.GAST)

        assertTrue(heim.text.contains("weh"))
        assertTrue(gast.text.contains("stolz"))
        assertTrue(gast.text.contains("SC Kaiserslautern"))
        assertTrue(gast.text.contains("SC Viktoria Ulm"))
        assertEquals(KiPressekonferenz.Trainer.GAST, gast.trainer)
        assertEquals("SC Kaiserslautern", gast.teamName)
    }

    @Test
    fun heimUndGasttrainerErhaltenUnterschiedlicheAussagen() {
        val heim = KiPressekonferenz.aussage(bericht(ergebnis = "1:1"), KiPressekonferenz.Trainer.HEIM)
        val gast = KiPressekonferenz.aussage(bericht(ergebnis = "1:1"), KiPressekonferenz.Trainer.GAST)

        assertFalse(heim.ganz() == gast.ganz())
    }

    @Test
    fun ballbesitzWirdAusTrainersichtGewertet() {
        val heim = KiPressekonferenz.aussage(bericht(ergebnis = "2:1", ballbesitz = "30 : 70"), KiPressekonferenz.Trainer.HEIM)
        val gast = KiPressekonferenz.aussage(bericht(ergebnis = "2:1", ballbesitz = "30 : 70"), KiPressekonferenz.Trainer.GAST)

        assertTrue(heim.text.contains("zu viel Ballbesitz"))
        assertTrue(gast.text.contains("Kontrolle"))
    }

    private fun bericht(
        ergebnis: String?,
        ereignisse: List<BerichtEreignis> = emptyList(),
        ballbesitz: String? = null,
    ): SpielBericht = SpielBericht(
        saison = 25,
        zat = 12,
        heim = "SC Viktoria Ulm",
        gast = "SC Kaiserslautern",
        ergebnis = ergebnis,
        ereignisse = ereignisse,
        statistik = ballbesitz?.let { BerichtStatistik(ballbesitz = it) },
        url = "test",
    )
}
