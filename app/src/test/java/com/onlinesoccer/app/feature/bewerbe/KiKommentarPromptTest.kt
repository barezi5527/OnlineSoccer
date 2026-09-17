package com.onlinesoccer.app.feature.bewerbe

import com.onlinesoccer.app.data.model.BerichtEreignis
import com.onlinesoccer.app.data.model.SpielBericht
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KiKommentarPromptTest {
    @Test
    fun promptVerwendetDieGeparstenBerichtsdaten() {
        val prompt = KiKommentarPrompt.ausBericht(
            SpielBericht(
                saison = 24,
                zat = 2,
                heim = "SC Viktoria Ulm",
                gast = "SC Kaiserslautern",
                ergebnis = "2:2",
                ereignisse = listOf(BerichtEreignis(minute = "34", text = "TOR durch Camilo Baruco")),
                url = "test",
            ),
        )

        assertTrue(prompt.contains("SC Viktoria Ulm"))
        assertTrue(prompt.contains("SC Kaiserslautern"))
        assertTrue(prompt.contains("2:2"))
        assertTrue(prompt.contains("34. Minute - TOR durch Camilo Baruco"))
        assertTrue(prompt.contains("400 bis 500 Wörter"))
        assertTrue(prompt.contains("Lies den Spielbericht nicht einfach vor"))
        assertFalse(prompt.contains("Aktueller Spielstand: 2:2"))
        assertTrue(prompt.contains("AUFLÖSUNG - NUR AM ENDE VERWENDEN"))
        assertTrue(prompt.contains("ausschließlich in der abschließenden Auflösung"))
        assertTrue(prompt.contains("Nenne nach jedem Tor ausdrücklich den neuen Spielstand"))
        assertTrue(prompt.contains("Spielstand nach dem Tor"))
        assertFalse(prompt.contains("SPRACHAUSGABE - WICHTIG"))
        assertFalse(prompt.contains("starte die Sprachausgabe sofort"))
        assertFalse(prompt.contains("[automatisch einsetzen]"))
    }
}
