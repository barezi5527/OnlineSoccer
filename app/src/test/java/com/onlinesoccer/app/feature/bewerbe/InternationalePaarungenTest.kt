package com.onlinesoccer.app.feature.bewerbe

import com.onlinesoccer.app.data.model.InternationaleSpiel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class InternationalePaarungenTest {
    @Test
    fun heimUndRueckspielWerdenNachTeamIdsZuEinerPaarungGruppiert() {
        val hinspiel = InternationaleSpiel(
            heim = "Team A",
            gast = "Team B",
            ergebnis = "2:0",
            heimTeamId = 11,
            gastTeamId = 22,
        )
        val anderesHinspiel = InternationaleSpiel(
            heim = "Team C",
            gast = "Team D",
            ergebnis = "1:1",
            heimTeamId = 33,
            gastTeamId = 44,
        )
        val rueckspiel = InternationaleSpiel(
            heim = "Team B",
            gast = "Team A",
            vorschauUrl = "https://os.ongapo.com/spielpreview.php?t1=22&t2=11",
            heimTeamId = 22,
            gastTeamId = 11,
        )

        val paarungen = qualifikationsPaarungen(listOf(hinspiel, anderesHinspiel, rueckspiel))

        assertEquals(2, paarungen.size)
        assertSame(hinspiel, paarungen[0].hinspiel)
        assertSame(rueckspiel, paarungen[0].rueckspiel)
        assertSame(anderesHinspiel, paarungen[1].hinspiel)
        assertNull(paarungen[1].rueckspiel)
    }
}
