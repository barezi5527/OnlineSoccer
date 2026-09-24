package com.onlinesoccer.app.feature.statistik

import com.onlinesoccer.app.data.model.LaenderOption
import org.junit.Assert.assertEquals
import org.junit.Test

class TopscorerTest {

    private val optionen = listOf(
        LaenderOption("0", "Alle"),
        LaenderOption("1", "1. Liga"),
        LaenderOption("24", "Saison 24"),
    )

    @Test
    fun gueltigeAuswahl_behaeltEnthaltenesWert() {
        assertEquals("1", gueltigeAuswahl(optionen, "1", "0"))
    }

    @Test
    fun gueltigeAuswahl_faelltAufStandardBeiUnbekanntemWert() {
        assertEquals("0", gueltigeAuswahl(optionen, "99", "0"))
    }

    @Test
    fun gueltigeAuswahl_laesstLeereOptionenUnangetastet() {
        assertEquals("7", gueltigeAuswahl(emptyList(), "7", "0"))
    }

    @Test
    fun gueltigeSaison_faelltAufAktuelleHoechsteSaison() {
        val saisons = listOf(
            LaenderOption("0", "Alle"),
            LaenderOption("23", "Saison 23"),
            LaenderOption("24", "Saison 24"),
        )
        assertEquals("24", gueltigeSaison(saisons, "25"))
    }

    @Test
    fun gueltigeSaison_behaeltGueltigesWert() {
        val saisons = listOf(
            LaenderOption("0", "Alle"),
            LaenderOption("24", "Saison 24"),
        )
        assertEquals("24", gueltigeSaison(saisons, "24"))
    }

    @Test
    fun gueltigeSaison_nurAlleLiefertNull() {
        assertEquals("0", gueltigeSaison(listOf(LaenderOption("0", "Alle")), "25"))
    }

    @Test
    fun wertKopf_zeigtKurzeStatistikNamen() {
        assertEquals("Tore", topscorerWertKopf("1"))
        assertEquals("Vorlagen", topscorerWertKopf("2"))
        assertEquals("Tore/Spiel", topscorerWertKopf("4"))
        assertEquals("Karten/Min", topscorerWertKopf("10"))
        assertEquals("Wert", topscorerWertKopf("42"))
    }
}