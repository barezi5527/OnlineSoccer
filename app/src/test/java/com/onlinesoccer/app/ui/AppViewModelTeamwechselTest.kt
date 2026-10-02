package com.onlinesoccer.app.ui

import com.onlinesoccer.app.core.state.AenderungBereich
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

    // --- T38a: Wechselsperre -------------------------------------------------

    @Test
    fun ohneSperreIstDerWechselFreigegeben() {
        val stand = zustand(teamId = 1216, zweitTeamId = 1216)

        assertFalse(stand.gesperrt)
        assertEquals(WechselAktion.Starten, wechselAktion(stand))
    }

    @Test
    fun sperrzeitSperrtDenNaechstenWechsel() {
        val stand = zustand(teamId = 1216, zweitTeamId = 1216).copy(sperrRestSekunden = 12)

        assertTrue(stand.gesperrt)
        assertEquals(WechselAktion.Gesperrt(12), wechselAktion(stand))
    }

    @Test
    fun abgelaufeneSperrzeitIstKeineSperre() {
        val stand = zustand(teamId = 1216, zweitTeamId = 1216).copy(sperrRestSekunden = 0)

        assertFalse(stand.gesperrt)
        assertEquals(WechselAktion.Starten, wechselAktion(stand))
    }

    @Test
    fun laufenderWechselSchlaegtDieSperre() {
        // Sonst meldete ein Tipp mitten im Wechsel „in 15 s möglich", obwohl
        // gerade der Wechsel läuft, der die Sperre überhaupt erst auslöst.
        val stand = zustand(teamId = 1216, zweitTeamId = 1216).copy(
            laeuft = true,
            sperrRestSekunden = WECHSEL_SPERRE_SEKUNDEN,
        )

        assertEquals(WechselAktion.Lauft, wechselAktion(stand))
    }

    @Test
    fun sperrMeldungNenntDieRestzeit() {
        assertEquals("Teamwechsel in 24 s möglich.", teamwechselSperrMeldung(24))
        assertEquals("Teamwechsel in 1 s möglich.", teamwechselSperrMeldung(1))
        // Untergrenze 1: bei 0 ist die Sperre vorbei, „in 0 s" wäre Unsinn.
        assertEquals("Teamwechsel in 1 s möglich.", teamwechselSperrMeldung(0))
    }

    @Test
    fun countdownZaehltHerunterUndEndetBeiNull() {
        assertEquals(14, restzeitNachTakt(WECHSEL_SPERRE_SEKUNDEN))
        assertEquals(1, restzeitNachTakt(2))
        assertEquals(0, restzeitNachTakt(1))
        assertEquals(0, restzeitNachTakt(0))
    }

    @Test
    fun nachDerSperreIstDerWechselWiederFreigegeben() {
        // Der ganze Zweck der Sperre: 15 Takte später ist der Weg zurück offen.
        var stand = zustand(teamId = 1216, zweitTeamId = 1216)
            .copy(sperrRestSekunden = WECHSEL_SPERRE_SEKUNDEN)

        repeat(WECHSEL_SPERRE_SEKUNDEN) {
            stand = stand.copy(sperrRestSekunden = restzeitNachTakt(stand.sperrRestSekunden))
        }

        assertFalse(stand.gesperrt)
        assertEquals(WechselAktion.Starten, wechselAktion(stand))
    }

    // ---------- T33/T34: offene, ungespeicherte Änderungen ----------

    @Test
    fun offeneAenderungBlockiertDenWechselBisZurBestaetigung() {
        val stand = zustand(teamId = 3449, zweitTeamId = 1216)
            .copy(offeneBereiche = setOf(AenderungBereich.ZUGABABE))

        val aktion = wechselAktion(stand)
        assertTrue(aktion is WechselAktion.Bestaetigen)
        // Der Dialog benennt Bereich **und** Folge — sonst klickt der Nutzer ahnungslos
        // auf „Ja" und wundert sich später über den Verlust.
        val text = (aktion as WechselAktion.Bestaetigen).text
        assertTrue(text.contains("Zugababe"))
        assertTrue(text.contains("wirklich wechseln"))
        assertTrue(text.contains("Nicht gespeicherte Änderungen gehen verloren"))
    }

    @Test
    fun zweiOffeneBereicheNennenBeide() {
        val stand = zustand(teamId = 3449, zweitTeamId = 1216).copy(
            offeneBereiche = setOf(AenderungBereich.ZUGABABE, AenderungBereich.TAKTIK),
        )

        val text = (wechselAktion(stand) as WechselAktion.Bestaetigen).text
        assertTrue(text.contains("Zugababe"))
        assertTrue(text.contains("Taktik"))
    }

    @Test
    fun ohneOffeneAenderungStartetDerWechselDirekt() {
        // Gegenprobe zum vorigen Test: derselbe Zustand ohne `offeneBereiche` schaltet
        // ohne Umweg — der Dialog darf nicht aus einem Leerstand heraus erscheinen.
        val stand = zustand(teamId = 3449, zweitTeamId = 1216)

        assertEquals(WechselAktion.Starten, wechselAktion(stand))
    }

    @Test
    fun offenerDialogBleibtStabilUndSpringtNicht() {
        // Solange die Änderung offen ist, liefert jede Prüfung denselben Dialog:
        // der erste Tipp stellt ihn, der Bestätigungs-Tipp startet den Wechsel. Ein
        // Zustand, der zwischen zwei Prüfungen umschaltet, würde den Dialog flackern
        // lassen oder — schlimmer — den Wechsel ohne Rückfrage durchlassen.
        val stand = zustand(teamId = 3449, zweitTeamId = 1216)
            .copy(offeneBereiche = setOf(AenderungBereich.TAKTIK))

        val erste = wechselAktion(stand)
        val zweite = wechselAktion(stand)
        assertEquals(erste, zweite)
        assertEquals(
            (erste as WechselAktion.Bestaetigen).text,
            (zweite as WechselAktion.Bestaetigen).text,
        )
    }

    @Test
    fun dialogOhneOffeneAenderungVerriegeltDenWechselNicht() {
        // Verteidigungsfall: `bestaetigung != null` bei leerem `offeneBereiche` kann der
        // Collector nicht erzeugen (er setzt beides zusammen) — träte sie trotzdem ein,
        // darf sie den Wechsel **nicht** blockieren, sonst wäre die App nach einem
        // unerklärten Dialogzustand dauerhaft teamwechsel-gesperrt.
        val stand = zustand(teamId = 3449, zweitTeamId = 1216)
            .copy(bestaetigung = "Zugababe offen — wirklich wechseln? Nicht gespeicherte Änderungen gehen verloren.")

        assertEquals(WechselAktion.Starten, wechselAktion(stand))
    }

    @Test
    fun laufenderWechselSchlaegtOffeneAenderung() {
        // `laeuft` zuerst: während ein Wechsel läuft, gibt es nichts zu bestätigen.
        val stand = zustand(teamId = 3449, zweitTeamId = 1216).copy(
            laeuft = true,
            offeneBereiche = setOf(AenderungBereich.ZUGABABE),
        )

        assertEquals(WechselAktion.Lauft, wechselAktion(stand))
    }

    @Test
    fun wechselsperreSchlaegtOffeneAenderung() {
        // Auch die Sperre zuerst: sie kann ohnehin nicht schalten. Ein Dialog, der ins
        // Leere führte, wäre schlimmer als die Restzeit-Meldung.
        val stand = zustand(teamId = 3449, zweitTeamId = 1216).copy(
            sperrRestSekunden = 9,
            offeneBereiche = setOf(AenderungBereich.ZUGABABE),
        )

        val aktion = wechselAktion(stand)
        assertTrue(aktion is WechselAktion.Gesperrt)
        assertEquals(9, (aktion as WechselAktion.Gesperrt).restSekunden)
    }
}