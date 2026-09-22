package com.onlinesoccer.app.feature.statistik

import com.onlinesoccer.app.data.model.LaenderOption
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class TopspielerTest {

    @Test
    fun werteKopf_zeigtStatistikNamen() {
        assertEquals("Opt Skill", topspielerWertKopf("16"))
        assertEquals("Skill", topspielerWertKopf("26"))
        assertEquals("Marktwert", topspielerWertKopf("33"))
        assertEquals("Gehalt", topspielerWertKopf("32"))
        assertEquals("Wert", topspielerWertKopf("99"))
    }

    @Test
    fun werteBreite_istFuerBetraegeBreiter() {
        assertEquals(96.dp, topspielerWertBreite("16"))
        assertEquals(96.dp, topspielerWertBreite("26"))
        assertEquals(120.dp, topspielerWertBreite("33"))
        assertEquals(120.dp, topspielerWertBreite("32"))
        assertEquals(96.dp, topspielerWertBreite("0"))
    }

    @Test
    fun optionLabel_liefertLabelDerAuswahl() {
        val optionen = listOf(
            LaenderOption("0", "Alle"),
            LaenderOption("33", "Marktwert"),
        )
        assertEquals("Marktwert", topspielerOptionLabel(optionen, "33"))
        assertEquals("Alle", topspielerOptionLabel(optionen, "0"))
        assertEquals(null, topspielerOptionLabel(optionen, "77"))
        assertEquals(null, topspielerOptionLabel(emptyList(), "33"))
    }
}