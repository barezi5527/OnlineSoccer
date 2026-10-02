package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.data.model.TeamwechselErgebnis
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Auswertung des Teamwechsel-Toggles — komplett ohne Netz, gegen handgebaute
 * Werte. Deckt die Fehlerquellen ab, die laut Plan das eigentliche Risiko sind:
 * stiller Rück-Toggle und ein Erfolgsreport, den der Server nicht bestätigt hat.
 */
class TeamwechselAuswertungTest {

    private val repo = TeamwechselRepository(okhttp3.OkHttpClient())

    /** Rohtext der Toggle-Antwort — der Normalfall: der Server hat geantwortet. */
    private val antwort = "<html>Managerbüro</html>"

    private fun dump(name: String): String =
        File("src/test/resources/dumps/$name.html").readText()

    @Test
    fun teamwechselZwischenZweiTeamsGiltAlsErfolg() {
        val ergebnis = repo.werteAus(
            vorher = 3449L to "SC Viktoria Ulm",
            nachher = 1216L to "NK Kamen Sesvete",
            antwort = antwort,
        )
        assertEquals(TeamwechselErgebnis.Erfolgreich(1216L, "NK Kamen Sesvete"), ergebnis)
    }

    @Test
    fun gleicheTeamIdVorherUndNachherIstUnveraendert() {
        val ergebnis = repo.werteAus(
            vorher = 1216L to "NK Kamen Sesvete",
            nachher = 1216L to "NK Kamen Sesvete",
            antwort = antwort,
        )
        assertEquals(TeamwechselErgebnis.Unveraendert, ergebnis)
    }

    @Test
    fun fehlendeTeamIdMitGleichemNameIstKeinErfolg() {
        // Wappen nicht lesbar: gleicher Name ⇒ kein Beleg für einen Wechsel.
        val ergebnis = repo.werteAus(
            vorher = null to "NK Kamen Sesvete",
            nachher = null to "NK Kamen Sesvete",
            antwort = antwort,
        )
        assertEquals(TeamwechselErgebnis.Unveraendert, ergebnis)
    }

    @Test
    fun fehlendeTeamIdMitAnderemNameGiltAlsErfolg() {
        val ergebnis = repo.werteAus(
            vorher = null to "SC Viktoria Ulm",
            nachher = null to "NK Kamen Sesvete",
            antwort = antwort,
        )
        assertEquals(TeamwechselErgebnis.Erfolgreich(null, "NK Kamen Sesvete"), ergebnis)
    }

    @Test
    fun fehlenderVorOderNachzustandIstFehler() {
        assertTrueFehler(repo.werteAus(null, 1216L to "NK Kamen Sesvete", antwort))
        assertTrueFehler(repo.werteAus(3449L to "SC Viktoria Ulm", null, antwort))
        // Beide Seiten komplett ohne Befund.
        assertTrueFehler(repo.werteAus(null, null, antwort))
    }

    @Test
    fun teamIdIstUndNameFehltWirdUeberDieIdEntschieden() {
        val ergebnis = repo.werteAus(
            vorher = 3449L to null,
            nachher = 1216L to null,
            antwort = antwort,
        )
        assertEquals(TeamwechselErgebnis.Erfolgreich(1216L, null), ergebnis)
    }

    // --- T38b: der Toggle kam nicht an ---------------------------------------

    @Test
    fun gescheiterterToggleMeldetFehlerStattGleichemTeam() {
        // Gerätetest T38a: der Auftrag kam nicht an, der Server stand weiter auf
        // Team 2. „Der Server meldet weiterhin dasselbe Team" war sachlich richtig,
        // ließ den Nutzer aber im Blindflug erneut tippen.
        val ergebnis = repo.werteAus(
            vorher = 1216L to "NK Kamen Sesvete",
            nachher = 1216L to "NK Kamen Sesvete",
            antwort = null,
        )

        assertEquals(
            TeamwechselErgebnis.Fehler(
                "Wechsel nicht übernommen – bitte erneut tippen."
            ),
            ergebnis,
        )
    }

    @Test
    fun gescheiterterToggleBleibtAuchBeimNamenvergleichEinFehler() {
        val ergebnis = repo.werteAus(
            vorher = null to "NK Kamen Sesvete",
            nachher = null to "NK Kamen Sesvete",
            antwort = null,
        )

        assertTrueFehler(ergebnis)
    }

    @Test
    fun wechselZaehltAuchWennNurDieAntwortFehlt() {
        // Die Toggle-Antwort kann nach der Serveraktion verloren gehen
        // (Response abgeschnitten). Der Refetch ist die Server-Wahrheit — ein
        // belegter Wechsel darf daran nicht scheitern, dass die Antwort fehlt.
        val ergebnis = repo.werteAus(
            vorher = 3449L to "SC Viktoria Ulm",
            nachher = 1216L to "NK Kamen Sesvete",
            antwort = null,
        )

        assertEquals(TeamwechselErgebnis.Erfolgreich(1216L, "NK Kamen Sesvete"), ergebnis)
    }

    @Test
    fun gescheiterterToggleOhneBefundBleibtFehler() {
        val ergebnis = repo.werteAus(
            vorher = 1216L to "NK Kamen Sesvete",
            nachher = null,
            antwort = null,
        )

        assertEquals(
            TeamwechselErgebnis.Fehler("Teamwechsel nicht bestätigt – bitte erneut versuchen."),
            ergebnis,
        )
    }

    @Test
    fun endpunktToggleOhneZielParameter() {
        assertEquals("https://os.ongapo.com/haupt.php?changetosecond=true", OsApi.TEAMWECHSEL)
    }

    @Test
    fun verifikationLiestTeamIdUndNameAusServerAntwort() {
        assertEquals(
            3449L to "SC Viktoria Ulm",
            repo.aktivesTeamAusHtml(dump("haupt_team1_aktiv")),
        )
        assertEquals(
            1216L to "NK Kamen Sesvete",
            repo.aktivesTeamAusHtml(dump("haupt_team2_aktiv")),
        )
        assertEquals(
            3449L to "SC Viktoria Ulm",
            repo.aktivesTeamAusHtml(dump("haupt_team1_zurueck")),
        )
    }

    private fun assertTrueFehler(ergebnis: TeamwechselErgebnis) {
        assertEquals(true, ergebnis is TeamwechselErgebnis.Fehler)
    }
}