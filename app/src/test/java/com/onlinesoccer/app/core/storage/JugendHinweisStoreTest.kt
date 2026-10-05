package com.onlinesoccer.app.core.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JugendHinweisStoreTest {

    @Test
    fun identischerServerhinweisBleibtBisZurNeuanmeldungAusgeblendet() {
        assertTrue(jugendHinweisSollAusgeblendet("Warnung", "Warnung"))
    }

    @Test
    fun geaenderterServerhinweisWirdAngezeigt() {
        assertFalse(jugendHinweisSollAusgeblendet("Neue Warnung", "Alte Warnung"))
    }

    @Test
    fun fehlenderServerhinweisWirdNichtAusgeblendetBehandelt() {
        assertFalse(jugendHinweisSollAusgeblendet(null, "Warnung"))
    }
}
