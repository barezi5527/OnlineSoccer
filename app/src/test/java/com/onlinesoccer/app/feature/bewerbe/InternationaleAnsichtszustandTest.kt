package com.onlinesoccer.app.feature.bewerbe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Bildschirm wird beim Zurück aus dem Mannschaftskader neu zusammengesetzt, während
 * das ViewModel weiterlebt. Ohne diese Prüfung würden Tabelle, ZAT und Saison verraten.
 */
class InternationaleAnsichtszustandTest {

    @Test
    fun gleicheAnsichtIstGeladenUndWirdNichtNeuGeladen() {
        val zustand = InternationaleUiState(ansichtKey = "OSC_GRUPPE")

        assertTrue(zustand.istGeladen("OSC_GRUPPE"))
    }

    @Test
    fun andereAnsichtWirdNeuGeladen() {
        val zustand = InternationaleUiState(ansichtKey = "OSC_GRUPPE")

        assertFalse(zustand.istGeladen("OSE_GRUPPE"))
    }

    @Test
    fun ohneVorherigeAnsichtWirdGeladen() {
        assertFalse(InternationaleUiState().istGeladen("OSC_GRUPPE"))
    }

    @Test
    fun zuruecksetzenZurUebersichtLaesstAnsichtNeuLaden() {
        val zustand = InternationaleUiState(ansichtKey = "OSC_GRUPPE")

        assertFalse(zustand.zurUebersicht().istGeladen("OSC_GRUPPE"))
        assertNull(zustand.zurUebersicht().ansichtKey)
    }
}