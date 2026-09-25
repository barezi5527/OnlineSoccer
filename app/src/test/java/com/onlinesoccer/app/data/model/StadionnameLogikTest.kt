package com.onlinesoccer.app.data.model

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StadionnameLogikTest {
    private val zone = ZoneId.of("UTC")
    private val aktivSeit = LocalDate.of(2026, 9, 25).atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun normalisiertUndBegrenztNamen() {
        assertEquals("Arena am See", StadionnameLogik.normalisiere("  Arena am See  "))
        assertTrue(StadionnameLogik.istGueltig("Arena am See"))
        assertFalse(StadionnameLogik.istGueltig("   "))
        assertTrue(StadionnameLogik.fehler("x".repeat(61)) != null)
        assertTrue(StadionnameLogik.fehler("Arena\nam See") != null)
    }

    @Test
    fun eigenerHeimberichtNachSpeicherungVerwendetDenAnzeigenamen() {
        val bericht = bericht(datum = "26.09.2026")

        val name = StadionnameLogik.anzeigename(
            bericht = bericht,
            teamId = 3449L,
            gespeichert = StadionnameOverride(3449L, "Arena am See", aktivSeit),
            zoneId = zone,
        )

        assertEquals("Arena am See", name)
    }

    @Test
    fun gespielterBerichtAmSpeichertagBleibtBeimServernamen() {
        val bericht = bericht(datum = "25.09.2026")

        val name = StadionnameLogik.anzeigename(
            bericht = bericht,
            teamId = 3449L,
            gespeichert = StadionnameOverride(3449L, "Arena am See", aktivSeit),
            zoneId = zone,
        )

        assertNull(name)
        assertNull(
            StadionnameLogik.anzeigename(
                bericht = bericht(datum = "24.09.2026"),
                teamId = 3449L,
                gespeichert = StadionnameOverride(3449L, "Arena am See", aktivSeit),
                zoneId = zone,
            ),
        )
    }

    @Test
    fun fremderAuswaertsberichtOderUnvollstaendigerBerichtBleibtBeimServernamen() {
        val gespeichert = StadionnameOverride(3449L, "Arena am See", aktivSeit)

        assertNull(
            StadionnameLogik.anzeigename(
                bericht = bericht(datum = "26.09.2026", heimId = 709L),
                teamId = 3449L,
                gespeichert = gespeichert,
                zoneId = zone,
            ),
        )
        assertNull(
            StadionnameLogik.anzeigename(
                bericht = bericht(datum = null),
                teamId = 3449L,
                gespeichert = gespeichert,
                zoneId = zone,
            ),
        )
        assertNull(
            StadionnameLogik.anzeigename(
                bericht = bericht(datum = "26.09.2026", saison = 0),
                teamId = 3449L,
                gespeichert = gespeichert,
                zoneId = zone,
            ),
        )
    }

    private fun bericht(
        datum: String?,
        heimId: Long? = 3449L,
        saison: Int = 24,
    ) = SpielBericht(
        saison = saison,
        zat = 2,
        heimId = heimId,
        gastId = 709L,
        datum = datum,
        stadion = "Servername",
        url = "https://os.ongapo.com/rep/saison/24/2/3449-709.html",
    )
}
