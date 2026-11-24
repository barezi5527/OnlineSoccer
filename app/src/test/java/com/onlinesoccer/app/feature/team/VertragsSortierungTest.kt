package com.onlinesoccer.app.feature.team

import com.onlinesoccer.app.data.model.SpielerPosition
import com.onlinesoccer.app.data.model.VertragZeile
import org.junit.Assert.assertEquals
import org.junit.Test

class VertragsSortierungTest {

    private fun vertrag(name: String, laufzeit: String? = null) = VertragZeile(
        pid = name.hashCode().toLong(),
        name = name,
        laufzeit = laufzeit,
    )

    @Test
    fun sortierteVertraege_nachLaufzeit_aufsteigend() {
        val state = TeamUiState(
            vertraege = listOf(
                vertrag("Lang", "24"),
                vertrag("Kurz", "6"),
                vertrag("Mittel", "12"),
            ),
            vertragsSortierung = VertragsSortierung.LAUFZEIT,
        )

        assertEquals(listOf("Kurz", "Mittel", "Lang"), state.sortierteVertraege.map { it.name })
    }

    @Test
    fun sortierteVertraege_ungueltigeLaufzeitZuletzt() {
        val state = TeamUiState(
            vertraege = listOf(
                vertrag("Unbekannt", null),
                vertrag("Zuletzt", "bald"),
                vertrag("Kurz", " 4 "),
                vertrag("Leer", ""),
            ),
            vertragsSortierung = VertragsSortierung.LAUFZEIT,
        )

        assertEquals(listOf("Kurz", "Leer", "Unbekannt", "Zuletzt"), state.sortierteVertraege.map { it.name })
    }

    @Test
    fun sortierteVertraege_gleichstandNachName() {
        val state = TeamUiState(
            vertraege = listOf(
                vertrag("Zebra", "8"),
                vertrag("Alpha", "8"),
                vertrag("Beta", "8"),
            ),
            vertragsSortierung = VertragsSortierung.LAUFZEIT,
        )

        assertEquals(listOf("Alpha", "Beta", "Zebra"), state.sortierteVertraege.map { it.name })
    }

    @Test
    fun sortierteVertraege_nachName() {
        val state = TeamUiState(
            vertraege = listOf(
                vertrag("zebra", "24"),
                vertrag("Alpha", "6"),
                vertrag("beta", "12"),
            ),
            vertragsSortierung = VertragsSortierung.NAME,
        )

        assertEquals(listOf("Alpha", "beta", "zebra"), state.sortierteVertraege.map { it.name })
    }

    @Test
    fun sortierteVertraege_nachPosition_dannLaufzeit() {
        val state = TeamUiState(
            vertraege = listOf(
                vertrag("Sturm", "4").copy(position = SpielerPosition.STU),
                vertrag("Tor2", "12").copy(position = SpielerPosition.TOR),
                vertrag("Tor1", "6").copy(position = SpielerPosition.TOR),
                vertrag("Abwehr", "2").copy(position = SpielerPosition.ABW),
            ),
            vertragsSortierung = VertragsSortierung.POSITION,
        )

        assertEquals(listOf("Tor1", "Tor2", "Abwehr", "Sturm"), state.sortierteVertraege.map { it.name })
    }
}
