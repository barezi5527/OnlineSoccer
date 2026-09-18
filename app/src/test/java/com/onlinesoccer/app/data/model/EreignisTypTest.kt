package com.onlinesoccer.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EreignisTypTest {

    @Test
    fun klassifikation_erkenntTorMitSpielstandZeile() {
        assertEquals(BerichtEreignisTyp.TOR, klassifiziereEreignis("Schuss, TOR Neuer Spielstand: 1:0 (Aaron Muller)"))
        assertEquals(BerichtEreignisTyp.TOR, klassifiziereEreignis("TOR Neuer Spielstand: 2:1 (Dieter Roth)"))
    }

    @Test
    fun klassifikation_erkenntTorAusSpieltext() {
        assertEquals(BerichtEreignisTyp.TOR, klassifiziereEreignis("Freistoß! TOR durch Aaron Muller"))
        assertEquals(BerichtEreignisTyp.TOR, klassifiziereEreignis("Elfmeter verwandelt – TOR Neuer Spielstand: 1:0 (Aaron Muller)"))
    }

    @Test
    fun klassifikation_erkenntTorNurAusNeuemSpielstand() {
        assertEquals(BerichtEreignisTyp.TOR, klassifiziereEreignis("Neuer Spielstand: 1:0 (Aaron Muller)"))
    }

    @Test
    fun klassifikation_keinTorBeiSchussAmTorVorbei() {
        assertEquals(BerichtEreignisTyp.SONSTIGES, klassifiziereEreignis("Schuss von Maxi knapp vorbei am Tor"))
        assertEquals(BerichtEreignisTyp.SONSTIGES, klassifiziereEreignis("Der Ball fliegt am Tor vorbei"))
        assertEquals(BerichtEreignisTyp.SONSTIGES, klassifiziereEreignis("Großchance, aber Maxi setzt den Ball am Tor vorbei"))
    }

    @Test
    fun klassifikation_unterscheidetRoteVorGelberKarte() {
        assertEquals(BerichtEreignisTyp.ROTE_KARTE, klassifiziereEreignis("Gelb-rote Karte für Müller"))
        assertEquals(BerichtEreignisTyp.ROTE_KARTE, klassifiziereEreignis("Zweite gelbe Karte für Schmidt"))
        assertEquals(BerichtEreignisTyp.ROTE_KARTE, klassifiziereEreignis("Rote Karte · Platzverweis für Berger"))
    }

    @Test
    fun klassifikation_erkenntGelbeKarte() {
        assertEquals(BerichtEreignisTyp.GELBE_KARTE, klassifiziereEreignis("Gelbe Karte für Winterhalter"))
    }

    @Test
    fun klassifikation_erkenntVerletzungUndElfmeter() {
        assertEquals(BerichtEreignisTyp.VERLETZUNG, klassifiziereEreignis("Verletzung von Rüter, muss vom Platz"))
        assertEquals(BerichtEreignisTyp.ELFMETER, klassifiziereEreignis("Elfmeter für die Heimmannschaft"))
        assertEquals(BerichtEreignisTyp.ELFMETER, klassifiziereEreignis("Strafstoß verwandelt"))
    }

    @Test
    fun klassifikation_sensorischesSonstiges() {
        assertEquals(BerichtEreignisTyp.SONSTIGES, klassifiziereEreignis("Anpfiff"))
        assertEquals(BerichtEreignisTyp.SONSTIGES, klassifiziereEreignis("Ecke für die Gastmannschaft"))
    }
}