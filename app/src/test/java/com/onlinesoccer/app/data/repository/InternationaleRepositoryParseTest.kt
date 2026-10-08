package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.InternationaleTabelleKlasse
import com.onlinesoccer.app.feature.bewerbe.kompakteTabellenSpalten
import com.onlinesoccer.app.feature.bewerbe.rankingTabellenSpalten
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InternationaleRepositoryParseTest {
    private val repository = InternationaleRepository(okhttp3.OkHttpClient())

    @Test
    fun teilnehmerlisteWirdNachWettbewerbGruppiert() {
        val ansicht = repository.parse(File("src/test/resources/dumps/int_teilnehmer.html").readText())

        // Leere Wettbewerbsabschnitte (hier OSE) werden nicht aufgenommen.
        assertEquals(listOf("OSC", "OSCQ", "OSEQ"), ansicht.teilnehmer.map { it.bezeichnung })

        val osc = ansicht.teilnehmer.first()
        assertEquals("Admira Pinkafeld", osc.teams.single().name)
        assertEquals(224L, osc.teilnehmerId())
        assertTrue(osc.teams.single().zusatz.contains("Skillschnitt: 62.34 - Optischnitt: 90.15"))
        assertTrue(osc.teams.single().zusatz.none { it.contains("Admira Pinkafeld") })
        assertTrue(ansicht.abschnitte.isEmpty())
    }

    private fun com.onlinesoccer.app.data.model.InternationaleTeilnehmerGruppe.teilnehmerId(): Long? =
        teams.single().teamId

    @Test
    fun gruppenphaseGliedertPaarungenNachSpieltag() {
        val ansicht = repository.parse(File("src/test/resources/dumps/oscgp23.html").readText())

        assertEquals(listOf("ZAT 17", "ZAT 19"), ansicht.zatOptionen.map { it.label })
        assertEquals(2, ansicht.spiele.size)
        assertEquals("ZAT 17", ansicht.spiele[0].zat)
        assertEquals("ZAT 19", ansicht.spiele[1].zat)
        assertEquals("Eintracht Wuppertal", ansicht.spiele[0].heim)
        assertEquals(164L, ansicht.spiele[0].heimTeamId)
        assertEquals("Mödling OSC", ansicht.spiele[1].heim)
        assertEquals(1309L, ansicht.spiele[1].heimTeamId)
        assertEquals(1, ansicht.spiele.count { it.zat == "ZAT 17" })
    }

    @Test
    fun gruppenphaseOhneSpieltagUeberschriftenBleibtOhneZatFilter() {
        val ansicht = repository.parse(File("src/test/resources/dumps/oscq.html").readText())

        assertTrue("Qualifikation kennt keine ZAT-Überschriften", ansicht.zatOptionen.isEmpty())
        assertTrue(ansicht.spieleAlleOhneZat())
    }

    private fun com.onlinesoccer.app.data.model.InternationaleAnsicht.spieleAlleOhneZat(): Boolean =
        spiele.all { it.zat == null }

    @Test
    fun gruppenphaseTabelleUebernimmtPlatzklassenDerWebsite() {
        val tabelle = repository.parse(File("src/test/resources/dumps/oscgp23.html").readText())
            .tabellen
            .single()

        assertEquals(
            listOf(
                InternationaleTabelleKlasse.TOP,
                InternationaleTabelleKlasse.MITTEL,
                InternationaleTabelleKlasse.UNTEN,
            ),
            tabelle.zeilenKlasse,
        )
        assertEquals(listOf(212L, 164L, 266L), tabelle.teamIds)
    }

    @Test
    fun finalrundeBlendetDieTurnierbaumOptionAus() {
        val ansicht = repository.parse(File("src/test/resources/dumps/oscfr23.html").readText())

        // Die Website bietet im Runde-Filter zusätzlich „Tableau" (`runde=99`).
        // Diese Auswahl liefert nur den Turnierbaum und damit keinen Inhalt für
        // die App, deshalb darf sie im Filter nicht auftauchen.
        assertEquals(
            listOf("---Runde---", "Zwischenrunde", "Achtelfinale", "Viertelfinale", "Halbfinale", "Finale"),
            ansicht.runden.map { it.label },
        )
        assertTrue(ansicht.runden.none { it.wert == "99" })

        // Die übrigen Runden der Seite bleiben unverändert lesbar.
        assertEquals("Finale", ansicht.runden.last().label)
        assertEquals("5", ansicht.filter.runde)
        assertEquals(1, ansicht.spiele.size)
        assertEquals("Mödling OSC", ansicht.spiele.single().heim)
    }

    @Test
    fun ranglistenTabelleHatKeinePlatzklassen() {
        val tabelle = repository.parse(
            """
            <table class="osranking">
              <tr><th>Platz</th><th colspan="2">Land</th><th>Saison 23</th><th></th><th>Summe</th></tr>
              <tr><td>1.</td><td><img src="../images/flaggen/FLAG.gif" /></td><td><a href="../st.php?c=7">Club A</a></td><td>8.833</td><td>Info</td><td>14.1363333</td></tr>
            </table>
            """.trimIndent(),
        ).tabellen.single()

        assertEquals(listOf(null), tabelle.zeilenKlasse)
    }

    @Test
    fun oscQualifikationListetHinUndRueckspieleMitVereinsIds() {
        val ansicht = repository.parse(File("src/test/resources/dumps/oscq.html").readText())

        assertEquals(4, ansicht.spiele.size)
        assertEquals("Beispiel A", ansicht.spiele[0].heim)
        assertEquals("Beispiel B", ansicht.spiele[0].gast)
        assertEquals(11L, ansicht.spiele[0].heimTeamId)
        assertEquals(22L, ansicht.spiele[0].gastTeamId)
        assertEquals("2:0", ansicht.spiele[0].ergebnis)
        assertEquals("Beispiel B", ansicht.spiele[1].heim)
        assertEquals("Beispiel A", ansicht.spiele[1].gast)
        assertEquals(22L, ansicht.spiele[1].heimTeamId)
        assertEquals("https://os.ongapo.com/rep/saison/24/7/22-11.html", ansicht.spiele[1].berichtUrl)
        assertEquals("Beispiel C", ansicht.spiele[2].heim)
        assertEquals("Beispiel D", ansicht.spiele[2].gast)
        assertEquals("Beispiel D", ansicht.spiele[3].heim)
        assertEquals("Beispiel C", ansicht.spiele[3].gast)
    }

    @Test
    fun oscQualifikationListetAuchPaarungenOhneSpielberichte() {
        val ansicht = repository.parse(File("src/test/resources/dumps/oscq_ohne_berichte.html").readText())

        assertEquals(3, ansicht.spiele.size)
        assertEquals("Team A", ansicht.spiele[0].heim)
        assertEquals("Team B", ansicht.spiele[0].gast)
        assertEquals(101L, ansicht.spiele[0].heimTeamId)
        assertEquals(202L, ansicht.spiele[0].gastTeamId)
        assertEquals(null, ansicht.spiele[0].berichtUrl)
        assertEquals("Team E", ansicht.spiele[2].heim)
        assertEquals("Team F", ansicht.spiele[2].gast)
    }

    @Test
    fun rueckspielVorschauWirdAlsNochOffeneBegegnungGeparst() {
        val ansicht = repository.parse(
            File("src/test/resources/dumps/oscq_rueckspiel_vorschau.html").readText(),
        )

        assertEquals(2, ansicht.spiele.size)
        val hinspiel = ansicht.spiele[0]
        assertEquals(11L, hinspiel.heimTeamId)
        assertEquals(22L, hinspiel.gastTeamId)
        assertEquals("2:0", hinspiel.ergebnis)
        assertNotNull(hinspiel.berichtUrl)

        val rueckspiel = ansicht.spiele[1]
        assertEquals(22L, rueckspiel.heimTeamId)
        assertEquals(11L, rueckspiel.gastTeamId)
        assertEquals(null, rueckspiel.ergebnis)
        assertEquals(null, rueckspiel.berichtUrl)
        assertEquals(
            "https://os.ongapo.com/spielpreview.php?t1=22&t2=11&type=4",
            rueckspiel.vorschauUrl,
        )
        assertEquals(null, rueckspiel.qualifiziertTeamId)
    }

    @Test
    fun qualifiziertesTeamWirdAusDerWebsiteHervorhebungUebernommen() {
        val ansicht = repository.parse(
            File("src/test/resources/dumps/oscq_entschieden.html").readText(),
        )

        assertEquals(2, ansicht.spiele.size)
        assertEquals(listOf(22L, 22L), ansicht.spiele.map { it.qualifiziertTeamId })
        assertEquals(listOf("0:0", "1:1"), ansicht.spiele.map { it.ergebnis })
    }

    @Test
    fun filterUndTabelleWerdenGelesen() {
        val ansicht = repository.parse(
            """
            <h1>OSC Gruppenphase</h1>
            <form>
              <select name="gruppe"><option value="2" selected>Gruppe 1</option><option value="3">Gruppe 2</option></select>
              <select name="season"><option value="24" selected>Saison 24</option></select>
              <select name="ergebnisse"><option value="1" selected>Ergebnisse anzeigen</option></select>
            </form>
            <table><tr><th>Club</th><th>Punkte</th></tr><tr><td>Team A</td><td>3</td></tr></table>
            """.trimIndent(),
        )

        assertEquals("OSC Gruppenphase", ansicht.titel)
        assertEquals("2", ansicht.filter.gruppe)
        assertEquals("24", ansicht.filter.saison)
        assertEquals("1", ansicht.filter.ergebnisse)
        assertTrue(ansicht.tabellen.single().zeilen.single().contains("Team A"))
    }

    @Test
    fun osRankingZeigtSummeVorn() {
        val ansicht = repository.parse(
            """
            <table class="osranking">
              <tr><th>Platz</th><th colspan="2">Land</th><th>Saison 23</th><th>Saison 22</th><th>Saison 21</th><th>Summe</th></tr>
              <tr><td>1.</td><td><img src="flag.gif" /></td><td>Spanien</td><td>9.75</td><td>10.45</td><td>9.88</td><td>30.08</td></tr>
            </table>
            """.trimIndent(),
        )

        val tabelle = ansicht.tabellen.single()
        assertEquals(listOf("#", "Lnd", "Sum", "S23", "S22", "S21"), tabelle.header)
        assertEquals(listOf("1.", "Spanien", "30.08", "9.75", "10.45", "9.88"), tabelle.zeilen.single())
    }

    @Test
    fun clubRankingZeigtSummeVornMitDreiNachkommastellen() {
        val ansicht = repository.parse(
            """
            <table class="osranking">
              <tr><th>Platz</th><th colspan="2">Land</th><th>Saison 23</th><th></th><th>Saison 22</th><th></th><th>Saison 21</th><th></th><th>Summe</th></tr>
              <tr><td>1.</td><td><img src="../images/flaggen/FLAG.gif" /></td><td><a href="../st.php?c=7">Club A</a></td><td>8.833</td><td><div class="info-wrapper">Details</div></td><td>7.955</td><td>Info</td><td>0</td><td>Info</td><td>14.1363333</td></tr>
            </table>
            """.trimIndent(),
        )

        val tabelle = ansicht.tabellen.single()
        assertEquals(listOf("#", "Lnd", "Club", "Sum", "S23", "S22", "S21"), tabelle.header)
        assertEquals(listOf("1.", "FLAG", "Club A", "14.136", "8.833", "7.955", "0"), tabelle.zeilen.single())
        assertEquals(listOf(7L), tabelle.teamIds)
    }

    @Test
    fun gruppenphasenTabelleLiefertSpaltenUndVereinsIds() {
        val ansicht = repository.parse(
            """
            <table class="intTabelle">
              <tr><th>#</th><th>Land</th><th>Club</th><th>Spiele</th><th>Si.</th><th>Un.</th><th>Ni.</th><th>Tore+</th><th>Tore-</th><th>Tore+/-</th><th>Punkte</th></tr>
              <tr><td>1.</td><td>AUT</td><td><a href="/st.php?c=11">Team A</a></td><td>6</td><td>5</td><td>1</td><td>0</td><td>12</td><td>3</td><td>9</td><td>16</td></tr>
            </table>
            """.trimIndent(),
        )

        val tabelle = ansicht.tabellen.single()
        assertEquals("Club", tabelle.header[2])
        assertEquals(11L, tabelle.teamIds.single())
    }

    @Test
    fun gruppenphasenTabelleNutztKompakteLigatabellenSpalten() {
        val tabelle = repository.parse(
            """
            <table class="intTabelle">
              <tr><th>#</th><th>Land</th><th>Club</th><th>Spiele</th><th>Si.</th><th>Un.</th><th>Ni.</th><th>Tore+</th><th>Tore-</th><th>Tore+/-</th><th>Punkte</th></tr>
              <tr><td>1.</td><td>AUT</td><td><a href="/st.php?c=11">Team A</a></td><td>6</td><td>5</td><td>1</td><td>0</td><td>12</td><td>3</td><td>9</td><td>16</td></tr>
            </table>
            """.trimIndent(),
        ).tabellen.single()

        val spalten = kompakteTabellenSpalten(tabelle.header)!!
        assertEquals(listOf("#", "Team", "Sp.", "Tore", "Diff.", "Pkt."), spalten.map { it.kopf })
        val zeile = tabelle.zeilen.single()
        assertEquals("1.", spalten[0].wert(zeile))
        assertEquals("Team A", spalten[1].wert(zeile))
        assertEquals("6", spalten[2].wert(zeile))
        assertEquals("12:3", spalten[3].wert(zeile))
        assertEquals("9", spalten[4].wert(zeile))
        assertEquals("16", spalten[5].wert(zeile))
        assertTrue("Alle Spalten sind gewichtet, damit die Tabelle die Breite füllt", spalten.all { it.gewicht != null })
    }

    /**
     * Das Club-Ranking der Website hat je Saison zwei Zellen: Punkte und den
     * Hover-Aufschluss. Angezeigt werden darf nur der Punktewert, und in der
     * Reihenfolge der Seite: Platz, Land, Club, Summe, dann die Saisons.
     */
    @Test
    fun clubRankingZeigtPlatzLandClubSummeUndSaisons() {
        val tabelle = repository.parse(File("src/test/resources/dumps/clubranking.html").readText())
            .tabellen
            .single()

        assertEquals(listOf("#", "Lnd", "Club", "Sum", "S23", "S22", "S21"), tabelle.header)
        assertEquals(
            listOf("1.", "HUN", "Banyas Budapest", "14.136", "8.833", "7.955", "0"),
            tabelle.zeilen.first(),
        )
        assertEquals(listOf(286L, 244L, 311L), tabelle.teamIds)
        // Der Hover-Aufschluss der Website darf in keiner Spalte landen.
        assertTrue(tabelle.zeilen.flatten().none { it.contains("info", true) || it.contains("Liga:") })

        val spalten = rankingTabellenSpalten(tabelle.header)!!
        assertEquals(listOf("#", "Lnd", "Club", "Sum", "S23", "S22", "S21"), spalten.map { it.kopf })
        assertEquals("Club", spalten.single { it.team }.kopf)
        assertTrue(spalten.take(3).all { it.fest })
        assertTrue(spalten.drop(3).none { it.fest })
        assertEquals("14136", spalten[3].wert(tabelle.zeilen.first()))
        assertEquals("8833", spalten[4].wert(tabelle.zeilen.first()))
        assertEquals("HUN", spalten[1].wert(tabelle.zeilen.first()))
    }

    @Test
    fun laenderRankingZeigtPlatzLandSummeUndSaisons() {
        val tabelle = repository.parse(File("src/test/resources/dumps/osranking.html").readText())
            .tabellen
            .single()

        assertEquals(listOf("#", "Lnd", "Sum", "S23", "S22", "S21"), tabelle.header)
        assertEquals(listOf("1.", "ESP", "30.08", "9.75", "10.45", "9.88"), tabelle.zeilen.first())
        assertEquals(3, tabelle.zeilen.size)
        assertTrue("Länder haben keine Vereins-ID", tabelle.teamIds.isEmpty())

        val spalten = rankingTabellenSpalten(tabelle.header)!!
        assertEquals(listOf("#", "Lnd", "Sum", "S23", "S22", "S21"), spalten.map { it.kopf })
        assertTrue(spalten.none { it.team })
        assertEquals("ESP", spalten[1].wert(tabelle.zeilen.first()))
    }

    @Test
    fun spielberichtImInternationalenWettbewerbWirdErkannt() {
        val ansicht = repository.parse(
            """
            <div id="international">
              <span><a href="/st.php?c=1">Team A</a></span>
              <span>-</span>
              <span><a href="/st.php?c=2">Team B</a></span>
              <span>2:1</span>
              <span><a href="/rep/saison/24/1/1-2.html">Klick</a></span>
            </div>
            """.trimIndent(),
        )

        val spiel = ansicht.spiele.singleOrNull()
        assertNotNull(spiel)
        assertEquals("Team A", spiel!!.heim)
        assertEquals("2:1", spiel.ergebnis)
        assertEquals("https://os.ongapo.com/rep/saison/24/1/1-2.html", spiel.berichtUrl)
    }

    @Test
    fun spielberichtOnClickWirdAlsUrlErkannt() {
        val ansicht = repository.parse(
            """
            <div id="international">
              <span><a href="/st.php?c=1">Team A</a></span>
              <span><a href="/st.php?c=2">Team B</a></span>
              <span>1:0</span>
              <span><a href="javascript:os_bericht(1, 2, 4, 24)">Klick</a></span>
            </div>
            """.trimIndent(),
        )

        assertEquals(
            "https://os.ongapo.com/rep/saison/24/4/1-2.html",
            ansicht.spiele.single().berichtUrl,
        )
    }
}
