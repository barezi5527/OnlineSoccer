package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.LetzteAktionenArt
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressionstests für die öffentlichen Listen-Screens gegen echte Server-Dumps
 * (`app/src/test/resources/dumps/freieteams*.html`, `managerliste*.html`).
 */
class ServerRepositoryParseTest {

    private val repo = ServerRepository(okhttp3.OkHttpClient())

    private fun dump(name: String): String =
        File("src/test/resources/dumps/$name.html").readText()

    @Test
    fun freieTeams_ohneLand_liefertAnzahlLaenderUndKeineZeilen() {
        val daten = repo.parseFreieTeams(dump("freieteams"))

        assertEquals("Zur Zeit sind 213 Teams frei.", daten.anzahlText)
        assertEquals("37 Länder", 37, daten.laender.size)
        assertTrue("ohne Land keine Zeilen", daten.zeilen.isEmpty())
    }

    @Test
    fun freieTeams_mitLand_parstZeilenInklusiveTeamIds() {
        val daten = repo.parseFreieTeams(dump("freieteams_de"))

        assertTrue(daten.zeilen.isNotEmpty())
        val burghausen = daten.zeilen.first { it.verein == "1. FC Burghausen" }
        assertEquals(1062L, burghausen.teamId)
        assertEquals("Deutschland", burghausen.land)
        assertEquals("3. Liga A", burghausen.liga)
        assertTrue(daten.laender.all { it.id != "0" })
    }

    @Test
    fun freieTeams_laenderLabelsEnthaltenAnzahl() {
        val daten = repo.parseFreieTeams(dump("freieteams"))

        val deutschland = daten.laender.first { it.label.startsWith("Deutschland") }
        assertEquals("6", deutschland.id)
    }

    @Test
    fun managerliste_ohneFilter_keineZeilenAbEx12HeaderUnkritisch() {
        val daten = repo.parseManagerliste(dump("managerliste"))

        assertTrue(daten.laender.isNotEmpty())
        assertTrue("7 Ligas", daten.ligas.size == 7)
        assertTrue(daten.zeilen.isEmpty())
    }

    @Test
    fun managerliste_mitLandKeineLiga_liefertKeinTrefferZeilenLeer() {
        val daten = repo.parseManagerliste(dump("managerliste_de"))

        assertTrue(daten.zeilen.isEmpty())
    }

    @Test
    fun managerliste_mitLandUndLiga_parstZeilen() {
        val daten = repo.parseManagerliste(dump("managerliste_de_liga"))

        assertTrue(daten.zeilen.isNotEmpty())
        val schunk = daten.zeilen.first { it.manager == "Michael Schunk" }
        assertEquals(97L, schunk.managerId)
        assertEquals(513L, schunk.teamId)
        assertEquals("1911 Baunatal", schunk.team)
        assertEquals("2", schunk.nmrGesamt)
        assertEquals("---", schunk.zugabgabe)

        val frei = daten.zeilen.first { it.manager == "Team ist frei" }
        assertEquals(-1L, frei.managerId)
        assertEquals(1046L, frei.teamId)
    }

    @Test
    fun freieZweitteams_ohneLand_liefertAnzahlUndLaender() {
        val daten = repo.parseFreieZweitteams(dump("fzt"))

        assertEquals("Derzeit sind 56 Zweitteams zur Bewerbung freigegeben", daten.anzahlText)
        assertTrue("29 Länder", daten.laender.size == 29)
        assertEquals("6", daten.laender.first { it.label.startsWith("Deutschland") }.id)
        assertTrue("ohne Land keine Zeilen", daten.zeilen.isEmpty())
    }

    @Test
    fun freieZweitteams_mitLand_parstZeilen() {
        val daten = repo.parseFreieZweitteams(dump("fzt_de"))

        val magdeburg = daten.zeilen.firstOrNull { it.verein == "FC 1965 Magdeburg" }
        assertTrue("Magdeburg-Zeile", magdeburg != null)
        assertEquals(1018L, magdeburg!!.teamId)
        assertEquals("Deutschland", magdeburg.land)
        assertEquals("2. Liga A", magdeburg.liga)
    }

    @Test
    fun managersuche_liefertTrefferzeilenMitIds() {
        val daten = repo.parseManagerSuche(dump("managersuche"))

        val treffer = daten.zeilen.firstOrNull { it.manager == "Rainer Nürge" }
        assertTrue("Treffer vorhanden", treffer != null)
        assertEquals(1954L, treffer!!.managerId)
        assertEquals(1018L, treffer.teamId)
        assertEquals("FC 1965 Magdeburg", treffer.team)
    }

    @Test
    fun managersuche_keinTreffer_liefertLeereZeilen() {
        val daten = repo.parseManagerSuche(dump("managersuche_leer"))

        assertTrue(daten.zeilen.isEmpty())
    }

    @Test
    fun transferliste_formular_liefertFilterOhneZeilen() {
        val ergebnis = repo.parseTransferliste(dump("transferliste"))

        assertTrue(ergebnis.filter.isNotEmpty())
        assertTrue("10 Filterfelder", ergebnis.filter.size == 10)
        assertTrue("ohne Suche keine Zeilen", ergebnis.zeilen.isEmpty())
        assertTrue(!ergebnis.gesucht)
        assertNotNull(ergebnis.hinweis)
    }

    @Test
    fun transferliste_suche_parstZeilenUndPagination() {
        val ergebnis = repo.parseTransferliste(dump("transferliste_suche"))

        assertTrue(ergebnis.gesucht)
        assertEquals(227, ergebnis.treffer)
        assertEquals(10, ergebnis.gesamtSeiten)
        assertTrue("Zeilen vorhanden", ergebnis.zeilen.isNotEmpty())
        assertTrue("ab 20 Zeilen", ergebnis.zeilen.size >= 20)

        val kiikeri = ergebnis.zeilen.first { it.name == "Juhani Kiikeri" }
        assertEquals(130312L, kiikeri.spielerId)
        assertEquals(1078L, kiikeri.teamId)
        assertEquals("FIN", kiikeri.land)
    }

    @Test
    fun transfermarkt_formular_liefertFilterOhneEintraege() {
        val ergebnis = repo.parseTransfermarkt(dump("transfermarkt"))

        assertTrue(ergebnis.filter.isNotEmpty())
        assertTrue("5 Filterfelder", ergebnis.filter.size == 5)
        assertTrue(ergebnis.eintraege.isEmpty())
        assertTrue(!ergebnis.gesucht)
    }

    @Test
    fun transfermarkt_suche_parstEintraegeMitGeboten() {
        val ergebnis = repo.parseTransfermarkt(dump("transfermarkt_suche"))

        assertTrue(ergebnis.gesucht)
        assertTrue("Einträge vorhanden", ergebnis.eintraege.isNotEmpty())

        val catone = ergebnis.eintraege.first { it.name == "Salvatore Catone" }
        assertEquals(114490L, catone.spielerId)
        assertEquals("1.015.615", catone.gebot)

        val mahmudov = ergebnis.eintraege.first { it.name == "Oqtay Mahmudov" }
        assertEquals(1923L, mahmudov.bieterTeamId)
        assertTrue("Bieter", mahmudov.bieter.contains("Torpedo"))
    }

    @Test
    fun versteigerungsmarkt_formular_liefertFilterOhneEintraege() {
        val ergebnis = repo.parseVersteigerungsmarkt(dump("viewvm_formular"))

        assertTrue(ergebnis.filter.isNotEmpty())
        assertTrue("5 Filterfelder", ergebnis.filter.size == 5)
        assertTrue(ergebnis.eintraege.isEmpty())
        assertTrue(!ergebnis.gesucht)
        assertNotNull("Hinweis mit Spieleranzahl", ergebnis.hinweis)
        assertTrue(ergebnis.hinweis.orEmpty().contains("Versteigerungsmarkt"))
    }

    @Test
    fun versteigerungsmarkt_suche_parstEintraegeMitProzentMwUndBieter() {
        val ergebnis = repo.parseVersteigerungsmarkt(dump("viewvm_suche"))

        assertTrue(ergebnis.gesucht)
        assertTrue("Einträge vorhanden", ergebnis.eintraege.isNotEmpty())

        val linley = ergebnis.eintraege.first { it.name == "Barry Linley" }
        assertEquals(105220L, linley.spielerId)
        assertEquals("MIT", linley.position)
        assertEquals("8.054.301", linley.gebot)
        assertEquals("70.25", linley.prozentMw)
        assertEquals("31", linley.alter)

        val sevilla = ergebnis.eintraege.first { it.name == "Sulevi Venäläinen" }
        assertEquals("Deportivo Sevilla", sevilla.bieter)
        assertEquals(1919L, sevilla.bieterTeamId)
    }

    @Test
    fun vmSetzen_parstEigeneSpielerMitStartpreisStaffeln() {
        val ergebnis = repo.parseVmSetzen(dump("vmsetzen"))

        assertTrue("Spieler vorhanden", ergebnis.eintraege.isNotEmpty())

        val dajan = ergebnis.eintraege.first { it.name == "Egnatius Dajan" }
        assertEquals(161985L, dajan.spielerId)
        assertEquals("22", dajan.alter)
        assertEquals("GHA", dajan.land)
        assertEquals("32.24", dajan.skill)
        assertEquals("46.37", dajan.opti)
        assertEquals("1.404.056", dajan.marktwert)
        assertEquals("70.203", dajan.gebuehr)
        assertEquals("16 Startpreise (25–100)", 16, dajan.startpreise.size)
        assertEquals("351.014", dajan.startpreise.first().label)
        assertEquals("1.404.056", dajan.startpreise.last().label)

        val simao = ergebnis.eintraege.first { it.name == "Tomás Simao" }
        assertEquals(121678L, simao.spielerId)
        assertTrue("Startpreise vorhanden", simao.startpreise.isNotEmpty())
    }

    @Test
    fun eigeneGebote_leereListe_liefertSummeUndKeineZeilen() {
        val ergebnis = repo.parseEigeneGebote(dump("eigenegebote"))

        assertTrue(ergebnis.zeilen.isEmpty())
        assertEquals("0", ergebnis.summe)
    }

    @Test
    fun leihUebersicht_parstAbschnitteUndGeliehenenSpieler() {
        val ergebnis = repo.parseLeihUebersicht(dump("leihuebersicht"))

        assertTrue("keine verliehenen Spieler", ergebnis.verliehen.isEmpty())
        assertEquals(1, ergebnis.geliehen.size)

        val vincez = ergebnis.geliehen.first()
        assertEquals(97944L, vincez.spielerId)
        assertEquals("Lars Vincez", vincez.name)
        assertEquals("33", vincez.alter)
        assertEquals("SUI", vincez.land)
        assertEquals("STU", vincez.position)
        assertEquals("61.47", vincez.skill)
        assertEquals("89.37", vincez.optSkill)
        assertEquals("146.561", vincez.leihgebuehr)
        assertEquals("Adler Koblenz", vincez.leihclub)
        assertEquals(842L, vincez.leihclubId)
    }

    @Test
    fun letzteTransfers_parstBloeckeMitSpielernUndZahlungen() {
        val ergebnis = repo.parseLetzteAktionen(dump("lasttrans"), LetzteAktionenArt.TRANSFERS)

        assertTrue("Blöcke vorhanden", ergebnis.zeilen.size >= 30)

        val bregovic = ergebnis.zeilen.first { it.spieler == "Marko Bregovic" }
        assertEquals(171277L, bregovic.spielerId)
        assertEquals("MIT", bregovic.position)
        assertEquals("10.09.2026 08:01", bregovic.datum)
        assertEquals("Vitoria Lourinhanense", bregovic.von)
        assertEquals(3459L, bregovic.vonId)
        assertEquals("Atletico Benito", bregovic.zu)
        assertEquals(3376L, bregovic.zuId)
        assertEquals("4.245.998 Euro", bregovic.betrag)
        assertTrue("Zahlungsdetails", bregovic.anmerkung.contains("Gesamt"))
    }

    @Test
    fun letzteLeihen_parstSpielerTeamsBetragUndDauer() {
        val ergebnis = repo.parseLetzteAktionen(dump("lastleih"), LetzteAktionenArt.LEIHEN)

        val hindersberg = ergebnis.zeilen.first { it.spieler == "Andreas Hindsberg" }
        assertEquals(127213L, hindersberg.spielerId)
        assertEquals("09.09.2026 08:59", hindersberg.datum)
        assertEquals("FC Helsingoer", hindersberg.von)
        assertEquals(725L, hindersberg.vonId)
        assertEquals("SC Äänekosk", hindersberg.zu)
        assertEquals(1090L, hindersberg.zuId)
        assertEquals("140.064", hindersberg.betrag)
        assertEquals("72 ZATs", hindersberg.dauer)
    }

    @Test
    fun letzteVermoegensmarktKaeufe_parstZeilenMitTeamBetrag() {
        val ergebnis = repo.parseLetzteAktionen(dump("lastvm"), LetzteAktionenArt.VM)

        val rogochiy = ergebnis.zeilen.first { it.spieler == "Lev Rogochiy" }
        assertEquals(171222L, rogochiy.spielerId)
        assertEquals("Zorynik Kirovograd", rogochiy.von)
        assertEquals("CF Castellon", rogochiy.zu)
        assertEquals(376L, rogochiy.zuId)
        assertEquals("3.841.344", rogochiy.betrag)
    }

    @Test
    fun letzteTransfermarktKaeufe_parstZeilenMitTeamUndPreis() {
        val ergebnis = repo.parseLetzteAktionen(dump("lasttm"), LetzteAktionenArt.TM)

        val bajtera = ergebnis.zeilen.first { it.spieler == "Pedro Bajtera" }
        assertEquals(154073L, bajtera.spielerId)
        assertEquals("Real Cadiz", bajtera.team)
        assertEquals(1452L, bajtera.teamId)
        assertEquals("980.810", bajtera.betrag)
    }

    @Test
    fun letzteSchnelltransfers_parstZeilenMitTeamZielUndZahlung() {
        val ergebnis = repo.parseLetzteAktionen(dump("lastblitz"), LetzteAktionenArt.BLITZ)

        val ivanov = ergebnis.zeilen.first { it.spieler == "Valerian Ivanov" }
        assertEquals(169963L, ivanov.spielerId)
        assertEquals("SpVgg Saarbrücken", ivanov.team)
        assertEquals(1053L, ivanov.teamId)
        assertEquals("Madagaskar", ivanov.ziel)
        assertEquals("1.518.940", ivanov.betrag)
    }

    @Test
    fun gebotInfo_parstKennzahlenUndSubmitFormular() {
        val info = repo.parseGebotInfo(dump("gebot"), 114490L)

        assertEquals(114490L, info.spielerId)
        assertEquals("Salvatore Catone", info.name)
        assertEquals("29", info.alter)
        assertEquals("Italien", info.nationalitaet)
        assertEquals("OMI", info.position)
        assertEquals("1.354.153", info.marktwert)
        assertEquals("15.09.2026", info.angeboteBis)
        assertEquals("1.015.615", info.hoechstgebot)
        assertEquals("8.136", info.gehalt)
        assertEquals("Gebot", info.submitName)
        assertEquals("Gebot abgeben als SC Viktoria Ulm", info.submitValue)
    }

    @Test
    fun parseGebotErgebnis_erkenntFehlerhinweis() {
        val fehler = repo.parseGebotErgebnis("<html><body>Du bist gesperrt und kannst kein Gebot abgeben.</body></html>")

        assertEquals(false, fehler.erfolg)
        assertTrue(fehler.meldung.contains("gesperrt"))
    }

    @Test
    fun parseGebotErgebnis_wertetGebotsantwortAlsErfolg() {
        val ergebnis = repo.parseGebotErgebnis(
            "<html><body><p>Ihr Gebot wurde erfolgreich abgegeben.</p></body></html>",
        )

        assertEquals(true, ergebnis.erfolg)
        assertTrue(ergebnis.meldung.contains("Gebot"))
    }

    @Test
    fun versteigerungsmarkt_suche_parstGebotsanzahl() {
        val ergebnis = repo.parseVersteigerungsmarkt(dump("viewvm_suche"))

        val royBayes = ergebnis.eintraege.first { it.name == "Roy Bayes" }
        assertEquals("7", royBayes.anzahl)
    }

    /**
     * Synthetisches `vmgebot.php`-Fixture: Der echte Aufbau ist laut Analyse
     * dokumentiert, aber ohne Zugriff auf die Website entsteht der Test aus der
     * bekannten Struktur (`Geld`-Eingabefeld wie bei `juscout.php`).
     */
    @Test
    fun vmGebotInfo_parstKennzahlenBetragsfeldUndSubmit() {
        val html = """
            <html><body><div>
            <b>Du kannst durch Klick auf "Gebot abgeben" ein Gebot auf den Versteigerungsmarkt abgeben.<b>
            <form method="POST" action="vmgebot.php?s=105220">
            <table border="0" width="100%">
                <tr><td align="right">Name:</td><td align="right">Barry Linley</td><td align="right">Alter:</td><td align="right">31</td></tr>
                <tr><td align="right">Nationalit&auml;t:</td><td align="right">England</td><td align="right">Stammposition:</td><td align="right">MIT</td></tr>
                <tr><td align="right">Marktwert:</td><td align="right">11.465.947</td><td align="right">Angebote bis:</td><td align="right">20.09.2026</td></tr>
                <tr><td align="right">H&ouml;chstgebot:</td><td align="right">8.054.301</td><td align="right">Gehalt:</td><td align="right">89.850</td></tr>
                <tr><td align="right">Bieter:</td><td align="right"></td></tr>
                <tr><td align="right"><input type="text" name="Geld" value="8054301" />&euro;</td><td align="right"><input type="submit" value="Gebot abgeben" name="Gebot"></td></tr>
            </table>
            </form>
            </div></body></html>
        """.trimIndent()

        val info = repo.parseVmGebotInfo(html, 105220L)

        assertEquals(105220L, info.spielerId)
        assertEquals("Barry Linley", info.name)
        assertEquals("31", info.alter)
        assertEquals("MIT", info.position)
        assertEquals("8.054.301", info.hoechstgebot)
        assertEquals("Geld", info.betragName)
        assertEquals("8054301", info.betragWert)
        assertEquals("Gebot", info.submitName)
        assertEquals("Gebot abgeben", info.submitValue)
    }

    @Test
    fun parseVmSetzenErgebnis_erkenntErfolgWennSpielerNichtMehrGelistet() {
        val html = """
            <html><body><table border="0">
            <tr><td>Name</td><td>Alter</td><td>Land</td><td>U</td><td>Skill</td><td>Opti</td><td>Marktwert</td><td>Geb&uuml;hr</td><td>Startpreis</td><td align="center">Aktion</td></tr><form method="POST">
            <tr>
                <td class="ABW"><a href="javascript:spielerinfo(999999);">Anderer Spieler</a></td>
                <td>20</td><td>DEU</td><td>#</td><td>20.00</td><td>30.00</td><td>1.000.000</td><td>50.000</td>
                <td><select name="startpreis"><option value="25">250.000</option></select></td>
                <td><input type="submit" value="auf den VM setzen"></td>
            </tr>
            </form></table></body></html>
        """.trimIndent()

        val ergebnis = repo.parseVmSetzenErgebnis(html, 161985L)

        assertEquals(true, ergebnis.erfolg)
        assertTrue(ergebnis.meldung.contains("Versteigerungsmarkt"))
    }

    @Test
    fun parseVmSetzenErgebnis_meldetFehlerWennSpielerNochGelistet() {
        val ergebnis = repo.parseVmSetzenErgebnis(dump("vmsetzen"), 161985L)

        assertEquals(false, ergebnis.erfolg)
    }

    @Test
    fun parseVmSetzenErgebnis_erkenntFehlerhinweis() {
        val ergebnis = repo.parseVmSetzenErgebnis(
            "<html><body>Der Spieler ist gesperrt und kann nicht auf den VM gesetzt werden.</body></html>",
            161985L,
        )

        assertEquals(false, ergebnis.erfolg)
        assertTrue(ergebnis.meldung.contains("gesperrt"))
    }
}