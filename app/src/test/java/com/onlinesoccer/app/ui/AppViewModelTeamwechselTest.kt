package com.onlinesoccer.app.ui

import com.onlinesoccer.app.data.model.TeamIds
import com.onlinesoccer.app.data.model.TeamwechselErgebnis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Zustand des 1|2-Buttons und seine Meldung — ohne Netz und ohne Android.
 *
 * Behandelt werden genau die zwei Entscheidungen, die der Plan als Fehlerquellen
 * benennt: ein Index, der dem Server widerspricht, und eine Erfolgsmeldung, die
 * der Server nicht bestätigt hat.
 */
class AppViewModelTeamwechselTest {

    private fun zustand(
        teamId: Long?,
        zweitTeamId: Long?,
        teamName: String? = null,
        wechselMoeglich: Boolean = zweitTeamId != null,
    ) = TeamwechselUiState(
        wechselMoeglich = wechselMoeglich,
        teamId = teamId,
        teamName = teamName,
        zweitTeamId = zweitTeamId,
    )

    @Test
    fun hauptteamAktivZeigtEins() {
        assertEquals(1, zustand(teamId = 3449, zweitTeamId = 1216).aktiverIndex)
    }

    @Test
    fun zweitteamAktivZeigtZwei() {
        assertEquals(2, zustand(teamId = 1216, zweitTeamId = 1216).aktiverIndex)
    }

    @Test
    fun unbekannteTeamIdZeigtEins() {
        assertEquals(1, zustand(teamId = 1216, zweitTeamId = null).aktiverIndex)
    }

    @Test
    fun guterStateZeigtEins() {
        assertEquals(1, zustand(teamId = null, zweitTeamId = null).aktiverIndex)
    }

    @Test
    fun wechselHinterlaesstZweitTeamIdUndIndex() {
        // Was `wechsleTeam()` schreibt: nur teamId/teamName wandern, die beim
        // Anmelden ermittelte Zweitteam-ID bleibt stehen. Andernfalls fällt der
        // Index beim nächsten Mal auf 1 zurück.
        val vorher = zustand(teamId = 3449, zweitTeamId = 1216, teamName = "SC Viktoria Ulm")
        val nachher = vorher.copy(teamId = 1216, teamName = "NK Kamen Sesvete")

        assertEquals(1, vorher.aktiverIndex)
        assertEquals(2, nachher.aktiverIndex)
        assertEquals(1216L, nachher.zweitTeamId)
        assertTrue(nachher.wechselMoeglich)
    }

    @Test
    fun rollenkorrigierteIdsBleibenNachKaltstartRichtig() {
        // `showteam.php` ist rollenrelativ: steht Team 2 aktiv, meldet die Seite
        // „Mein Hauptteam" und liefert als Partner 3449. `zweitTeamIdAus()` muss
        // daraus trotzdem das feste Zweitteam 1216 machen — sonst zeigte der
        // Button nach einem Kaltstart auf Team 2 dauerhaft „1".
        val ids = TeamIds(teamId = 1216, partnerTeamId = 3449, partnerIstHauptteam = true)
        val zustand = zustand(teamId = ids.teamId, zweitTeamId = zweitTeamIdAus(ids))

        assertEquals(1216L, zustand.zweitTeamId)
        assertEquals(2, zustand.aktiverIndex)
        assertTrue(zustand.wechselMoeglich)
    }

    @Test
    fun dasFesteZweitteamIstInBeidenRollenlagenGleich() {
        assertEquals(
            1216L,
            zweitTeamIdAus(TeamIds(teamId = 3449, partnerTeamId = 1216, partnerIstHauptteam = false)),
        )
        assertEquals(
            1216L,
            zweitTeamIdAus(TeamIds(teamId = 1216, partnerTeamId = 3449, partnerIstHauptteam = true)),
        )
    }

    @Test
    fun ohneZweitteamOderMitUnsinnGibtEsKeinZweitesTeam() {
        assertEquals(null, zweitTeamIdAus(null))
        assertEquals(null, zweitTeamIdAus(TeamIds(teamId = 3449)))
        assertEquals(null, zweitTeamIdAus(TeamIds(teamId = null, partnerTeamId = 1216)))
        // Beide Anker zeigen auf dasselbe Team — kein echtes Paar.
        assertEquals(null, zweitTeamIdAus(TeamIds(teamId = 3449, partnerTeamId = 3449)))
    }

    @Test
    fun ohneZweitteamBleibtDerButtonAus() {
        val gast = TeamwechselUiState()

        assertFalse(gast.wechselMoeglich)
        assertEquals(1, gast.aktiverIndex)
    }

    @Test
    fun meldungNenntDenNeuenTeamnamen() {
        assertEquals(
            "Jetzt aktiv: NK Kamen Sesvete",
            teamwechselMeldung(TeamwechselErgebnis.Erfolgreich(1216, "NK Kamen Sesvete")),
        )
    }

    @Test
    fun meldungOhneTeamnamenBleibtStattLustig() {
        assertEquals(
            "Teamwechsel erfolgreich.",
            teamwechselMeldung(TeamwechselErgebnis.Erfolgreich(1216, null)),
        )
    }

    @Test
    fun unveraenderterServerMeldetNichtErfolg() {
        val text = teamwechselMeldung(TeamwechselErgebnis.Unveraendert)

        assertFalse("Meldung darf keinen Erfolg behaupten", text.contains("aktiv"))
    }

    @Test
    fun fehlerMeldungWirdWoertlichUebergeben() {
        assertEquals(
            "Teamwechsel nicht bestätigt – bitte erneut versuchen.",
            teamwechselMeldung(
                TeamwechselErgebnis.Fehler("Teamwechsel nicht bestätigt – bitte erneut versuchen.")
            ),
        )
    }
}