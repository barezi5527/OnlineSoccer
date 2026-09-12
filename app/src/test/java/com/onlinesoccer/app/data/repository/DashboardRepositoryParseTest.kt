package com.onlinesoccer.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardRepositoryParseTest {
    private val repo = DashboardRepository(okhttp3.OkHttpClient())

    @Test
    fun berichtUrlVerwendetHeimGastZatUndSaison() {
        assertEquals(
            "https://os.ongapo.com/rep/saison/24/2/3449-38.html",
            repo.parseBerichtUrl("javascript:os_bericht(3449,38,2,24)"),
        )
    }

    @Test
    fun berichtUrlLehntUnvollstaendigeAufrufeAb() {
        assertEquals(null, repo.parseBerichtUrl("javascript:os_bericht(3449,38,2)"))
        assertEquals(null, repo.parseBerichtUrl("javascript:spielpreview(3449,38,2)"))
    }

    private val grundHtml = """
        <html><body>
        <img src="images/wappen/00000080.gif" alt="Wappen TSV 1999 Wehen" />
        <table><tr>
          <td valign="bottom" align="center"><b>Willkommen im Managerbüro von TSV 1999 Wehen</b><br>2. Liga B Deutschland<a href="?changetosecond=true"></a><br><br><a href="http://os.ongapo.com/forum/index.php?page=Board&amp;boardID=40" target="_blank">Zum Länderforum</a><br /></td>
        </tr></table>
        <b>Der nächste ZAT ist ZAT 3 und liegt auf Dienstag, 08.09.2026 ab 19:30 Uhr.</b>
        <table>
          <tr>
            <td style="color:orange"><b>Dein nächstes Spiel:&nbsp;</b></td>
            <td class="OMI">Friendly  Auswärts:&nbsp;</td>
            <td><a href="javascript:teaminfo(1050)">Bonner SpVgg</a> (Vorschau nicht möglich)</td>
          </tr>
        </table>
        <table border="1" cellpadding="5">
          <tr><td>Logins</td><td>Zugabgabe</td><td>Kontostand</td><td>PMs</td><td>FSS-Einladungen</td></tr>
          <tr><td>2767</td><td class="ABW"><a href="zugabgabe.php">Gültig</a></td><td class="ABW"><a href="ka.php">10.744.335 Euro</a></td><td class="LEI"><a href="/osneu/pm">0 neu</a></td><td class="LEI"><a href="friendly.php">0 neue Einladungen</a></td></tr>
        </table>
        </body></html>
    """.trimIndent()

    @Test
    fun teamNameKommenAusBegruessungNichtAusErstemTeaminfoLink() {
        val data = repo.parse(grundHtml)
        assertEquals("TSV 1999 Wehen", data.teamName)
        assertEquals("2. Liga B Deutschland", data.liga)
        assertEquals("3", data.zat)
        assertEquals("Dienstag, 08.09.2026 ab 19:30 Uhr.", data.zatDatum)
        assertEquals("Gültig", data.zugabgabeStatus)
        assertEquals("2767", data.logins)
        assertEquals("10.744.335 Euro", data.kontostand)
        assertEquals("0 neu", data.pmNeu)
        assertEquals("0 neue Einladungen", data.fssEinladungen)
    }

    @Test
    fun naechstesSpielWirdGeparstUndGegnerStehtImDashboardHeader() {
        val data = repo.parse(grundHtml)
        val next = data.naechstesSpiel
        assertEquals("Friendly Auswärts:", next?.art)
        assertEquals(false, next?.heim)
        assertEquals("Bonner SpVgg", next?.gegner)
        assertEquals(1050, next?.gegnerId)
        assertEquals("http://os.ongapo.com/forum/index.php?page=Board&boardID=40", data.forumUrl)
    }

    @Test
    fun teamNameFunktioniertAuchMitUmlautUndBlankolines() {
        val html = grundHtml.replace("TSV 1999 Wehen", "SC Viktoria Ulm")
        val data = repo.parse(html)
        assertEquals("SC Viktoria Ulm", data.teamName)
        assertEquals("2. Liga B Deutschland", data.liga)
    }

    @Test
    fun wappenLiefertTeamIdUndLogoUrl() {
        val data = repo.parse(grundHtml)
        assertEquals(80, data.teamId)
        assertTrue(data.teamLogoUrl?.endsWith("images/wappen/00000080.gif") == true)
    }

    @Test
    fun loginsStehtInZeilenUeberKontostand() {
        val data = repo.parse(grundHtml)
        val labels = data.rows.map { it.label }
        assertTrue(labels.indexOf("Logins") in 0 until labels.indexOf("Kontostand"))
        assertEquals("2767", data.rows.first { it.label == "Logins" }.value)
    }
}
