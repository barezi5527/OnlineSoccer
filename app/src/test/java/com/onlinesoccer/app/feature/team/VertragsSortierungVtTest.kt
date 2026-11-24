package com.onlinesoccer.app.feature.team

import com.onlinesoccer.app.data.model.SeitenAnsicht
import com.onlinesoccer.app.data.model.SpielerPosition
import com.onlinesoccer.app.data.model.UebersichtZeile
import com.onlinesoccer.app.data.repository.TeamRepository
import com.onlinesoccer.app.data.repository.ZugabgabeRepository
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VertragsSortierungVtTest {

    private val repo = TeamRepository(okhttp3.OkHttpClient(), ZugabgabeRepository(okhttp3.OkHttpClient()))

    private fun vtAnsicht() = repo.parseVertraegeVerlaengern(
        File("src/test/resources/dumps/vt.html").readText(),
    )

    private fun UebersichtZeile.laufzeit() =
        werte.firstOrNull { it.first.equals("Laufzeit", ignoreCase = true) }?.second

    @Test
    fun parser_liestPositionAusCssKlasse() {
        val zeile = vtAnsicht().abschnitte.first().zeilen.first { it.ueberschrift == "Heiko Kehrer" }

        assertEquals(SpielerPosition.TOR, zeile.position)
    }

    @Test
    fun sortiert_nachLaufzeitMitMonatsSuffix() {
        val sortiert = sortierteVerlaengerungen(vtAnsicht(), VertragsSortierung.LAUFZEIT)
        val zeilen = sortiert.abschnitte.first().zeilen
        val laufzeiten = zeilen.map { laufzeitZahl(it.laufzeit()) }

        val kuldar = zeilen.indexOfFirst { it.ueberschrift == "Kuldar Mitt" }
        val heiko = zeilen.indexOfFirst { it.ueberschrift == "Heiko Kehrer" }
        assertEquals(7, laufzeitZahl(zeilen[kuldar].laufzeit()))
        assertTrue(kuldar < heiko)
        assertEquals(laufzeiten.sorted(), laufzeiten)
    }

    @Test
    fun sortiert_nachName() {
        val sortiert = sortierteVerlaengerungen(vtAnsicht(), VertragsSortierung.NAME)
        val namen = sortiert.abschnitte.first().zeilen.map { it.ueberschrift.lowercase() }

        assertEquals(namen.sorted(), namen)
    }

    @Test
    fun sortiert_nachPosition() {
        val sortiert = sortierteVerlaengerungen(vtAnsicht(), VertragsSortierung.POSITION)
        val range = sortiert.abschnitte.first().zeilen.map { it.position?.rang ?: Int.MAX_VALUE }

        assertEquals(range.sorted(), range)
    }

    @Test
    fun andereSeiten_bleibenUnveraendert() {
        val ansicht = SeitenAnsicht(titel = "Training")

        assertSame(ansicht, sortierteVerlaengerungen(ansicht, VertragsSortierung.LAUFZEIT))
    }
}
