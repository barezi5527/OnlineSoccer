package com.onlinesoccer.app.feature.bewerbe

import com.onlinesoccer.app.data.model.WertTabelle
import org.junit.Assert.assertEquals
import org.junit.Test

class ClubRankingSucheTest {
    @Test
    fun vereinssucheIgnoriertGrossschreibungUndUmlaute() {
        val tabelle = tabelle()

        val treffer = clubRankingGefiltert(tabelle, "munchen")

        assertEquals(listOf(listOf("17.", "GER", "München SV", "12.345")), treffer.zeilen)
        assertEquals(listOf(170L), treffer.teamIds)
        assertEquals(listOf(null), treffer.zeilenKlasse)
    }

    @Test
    fun vereinssucheErhaeltDieUrspruenglicheRangposition() {
        val tabelle = tabelle()

        val treffer = clubRankingGefiltert(tabelle, "FC B")

        assertEquals("2.", treffer.zeilen.single().first())
        assertEquals(20L, treffer.teamIds.single())
    }

    @Test
    fun leereSucheZeigtDieUrspruenglicheTabelle() {
        val tabelle = tabelle()

        assertEquals(tabelle, clubRankingGefiltert(tabelle, "  "))
    }

    @Test
    fun unbekannterVereinLiefertLeereTabelle() {
        val treffer = clubRankingGefiltert(tabelle(), "gibt es nicht")

        assertEquals(emptyList<List<String>>(), treffer.zeilen)
        assertEquals(emptyList<Long?>(), treffer.teamIds)
    }

    private fun tabelle() = WertTabelle(
        header = listOf("#", "Lnd", "Club", "Sum"),
        zeilen = listOf(
            listOf("1.", "AUT", "Admira", "15.200"),
            listOf("2.", "BEL", "FC Brügge", "14.500"),
            listOf("17.", "GER", "München SV", "12.345"),
        ),
        teamIds = listOf(10L, 20L, 170L),
        zeilenKlasse = listOf(null, null, null),
    )
}
