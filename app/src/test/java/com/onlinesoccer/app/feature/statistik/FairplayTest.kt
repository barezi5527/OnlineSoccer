package com.onlinesoccer.app.feature.statistik

import org.junit.Assert.assertEquals
import org.junit.Test

class FairplayTest {

    @Test
    fun punkteText_zeigtVierNachkommastellenMitKomma() {
        assertEquals("0,0000", fairplayPunkteText(0.0))
        assertEquals("0,0263", fairplayPunkteText(0.0263))
        assertEquals("45,6789", fairplayPunkteText(45.6789))
        assertEquals("–", fairplayPunkteText(null))
    }
}