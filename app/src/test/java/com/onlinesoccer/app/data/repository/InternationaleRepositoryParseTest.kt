package com.onlinesoccer.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InternationaleRepositoryParseTest {
    private val repository = InternationaleRepository(okhttp3.OkHttpClient())

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
        assertEquals(listOf("Platz", "Summe", "Land", "Saison 23", "Saison 22", "Saison 21"), tabelle.header)
        assertEquals(listOf("1.", "30.08", "Spanien", "9.75", "10.45", "9.88"), tabelle.zeilen.single())
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
        assertEquals(listOf("Platz", "Summe", "Land", "Club", "Saison 23", "Saison 22", "Saison 21"), tabelle.header)
        assertEquals(listOf("1.", "14.136", "FLAG", "Club A", "8.833", "7.955", "0"), tabelle.zeilen.single())
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
