package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.feature.bewerbe.kompakteTabellenSpalten
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regressionstests gegen echte Server-Ausgaben (s. Ordner dumps). */
class BewerbeRepositoryParseTest {

    private val repo = BewerbeRepository(okhttp3.OkHttpClient())

    private fun dump(name: String): String =
        File("src/test/resources/dumps/$name.html").readText()

    @Test
    fun spieltag_geladeneZatMitResultatUndBericht() {
        val spieltag = runCatching { repo.parseSpieltag(dump("ls1")) }.getOrNull()
        assertNotNull("ls1 parse failed", spieltag)
        assertTrue("stauswahl selected = ZAT 1", spieltag!!.zat == 1)
        assertEquals("Saison 24", 24, spieltag.saison)
        assertTrue("stauswahl optionen vorhanden", spieltag.zatOptionen.isNotEmpty())
        assertTrue("spiele vorhanden", spieltag.spiele.isNotEmpty())

        val viktoria = spieltag.spiele.firstOrNull { it.heim.contains("SC Viktoria Ulm") || it.gast.contains("SC Viktoria Ulm") }
        assertNotNull("SC Viktoria Ulm im 1. Spieltag", viktoria)
        assertTrue("ZAT 1 gespielt", viktoria!!.gespielt)
        assertTrue("Ergebnis 2:2", viktoria.toreHeim == 2 && viktoria.toreGast == 2)

        val erstes = spieltag.spiele.first()
        assertNotNull(erstes.heimId)
        assertNotNull(erstes.gastId)
        // 1. Spiel lautet 80 (TSV 1999 Wehen) gegen 1050 (Bonner SpVgg), ZAT 2, Saison 24.
        assertEquals("https://os.ongapo.com/rep/saison/24/2/80-1050.html", erstes.berichtUrl)
        // Positionszusatz „(17.)“/„(2.)“ wird fürs UI entfernt.
        assertEquals("TSV 1999 Wehen", erstes.heim)
        assertEquals("Bonner SpVgg", erstes.gast)
    }

    @Test
    fun spieltag_zukunftOhneErgebnis() {
        val spieltag = runCatching { repo.parseSpieltag(dump("ls2")) }.getOrNull()
        assertNotNull("ls2 parse failed", spieltag)
        assertTrue("stauswahl selected = ZAT 2", spieltag!!.zat == 2)
        val viktoria = spieltag.spiele.firstOrNull { it.heim.contains("SC Viktoria Ulm") || it.gast.contains("SC Viktoria Ulm") }
        assertNotNull("SC Viktoria Ulm im 2. Spieltag", viktoria)
        assertTrue("ZAT 2 noch nicht gespielt", !viktoria!!.gespielt)
        assertTrue("kein Ergebnis", viktoria.toreHeim == null && viktoria.toreGast == null)
        assertTrue("keine tote Bericht-URL", viktoria.berichtUrl == null)
        assertTrue("Vorschau-Kennung vorhanden", !viktoria.vorschauId.isNullOrBlank())
    }

    @Test
    fun tabelle_mitEigenerZeile() {
        val tabelle = runCatching { repo.parseLigatabelle(dump("lt"), 3449) }.getOrNull()
        assertNotNull("lt parse failed", tabelle)
        assertTrue("18+ Zeilen", tabelle!!.zeilen.size >= 18)
        assertTrue(tabelle.header.isNotEmpty())

        val heute = tabelle.saison
        assertTrue("aktuelle Saison wählbar", heute in tabelle.saisonen.map { it.wert })

        val eigeneId = tabelle.eigenZeile
        assertNotNull("eigene Zeile (3449) gefunden", eigeneId)
        val eigeneZeile = tabelle.zeilen[eigeneId!!]
        assertTrue("eigener Verein in Zeile", eigeneZeile.any { it.contains("SC Viktoria Ulm") })
    }

    @Test
    fun tabelle_zeilenTeamIdsProZeile() {
        val tabelle = runCatching { repo.parseLigatabelle(dump("lt"), 3449) }.getOrNull()
        assertNotNull("lt parse failed", tabelle)
        assertTrue("18+ Zeilen", tabelle!!.zeilen.size >= 18)
        assertEquals(
            "je Zeile eine Team-Id",
            tabelle.zeilen.size,
            tabelle.zeilenTeamIds.size,
        )
        assertEquals("eigene Zeile hat Team-Id 3449", 3449L, tabelle.zeilenTeamIds[tabelle.eigenZeile!!])
        assertTrue("alle Team-Ids gefüllt", tabelle.zeilenTeamIds.all { it != null })
    }

    @Test
    fun pokalRundenMitSpielen() {
        val pokal = runCatching { repo.parsePokal(dump("lp2")) }.getOrNull()
        assertNotNull("lp2 parse failed", pokal)
        assertTrue("mindestens eine Runde", pokal!!.runden.isNotEmpty())
        assertEquals("Saison 24", 24, pokal.saison)
        assertEquals("1. Runde", 1, pokal.runde)
        assertTrue("Saisonauswahl vorhanden", pokal.saisonen.any { it.wert == 24 })
        assertTrue("Rundenauswahl vorhanden", pokal.rundenOptionen.any { it.wert == 7 && it.label == "Finale" })
        val erste = pokal.runden.first()
        assertTrue("Runde enthält Spiele", erste.spiele.isNotEmpty())
        val spiel = erste.spiele.first()
        assertTrue("Paarung vorhanden", spiel.heim.isNotBlank() && spiel.gast.isNotBlank())
        assertTrue("Vorschau ohne Ergebnis hat keinen Bericht", spiel.berichtUrl == null)
    }

    @Test
    fun tabelle_kompakteSpaltenErzeugenToreMitDoppelpunkt() {
        val spalten = kompakteTabellenSpalten(listOf("#", "", "Club", "Spiele", "Si.", "Un.", "Ni.", "Tore+", "Tore-", "Tore +/-", "Punkte"))
        assertNotNull("Kompaktspalten erkannt", spalten)
        assertEquals("6 kompakte Spalten", 6, spalten!!.size)
        assertEquals("#", spalten[0].kopf)
        assertEquals("Team", spalten[1].kopf)
        assertEquals("Sp.", spalten[2].kopf)
        assertEquals("Tore", spalten[3].kopf)
        assertEquals("Diff.", spalten[4].kopf)
        assertEquals("Pkt.", spalten[5].kopf)
        val zeile = listOf("3.", "", "Aachener BSC", "1", "1", "0", "0", "3", "2", "1", "3")
        assertEquals("Platz-Wert", "3.", spalten[0].wert(zeile))
        assertEquals("Team-Wert", "Aachener BSC", spalten[1].wert(zeile))
        assertEquals("Sp.-Wert", "1", spalten[2].wert(zeile))
        assertEquals("Tore als 5:1-Format", "3:2", spalten[3].wert(zeile))
        assertEquals("Diff.", "1", spalten[4].wert(zeile))
        assertEquals("Pkt.", "3", spalten[5].wert(zeile))
    }

    @Test
    fun tabelle_kompakteSpaltenIgnorierenUnbekannteHeader() {
        assertNull("Unbekannte Tabelle -> keine Kompaktspalten", kompakteTabellenSpalten(listOf("Foo", "Bar")))
    }

    @Test
    fun osBerichtUrlKonstruktion() {
        assertTrue(
            repo.osBerichtUrl("os_bericht(80,1050,2,24)") == "https://os.ongapo.com/rep/saison/24/2/80-1050.html",
        )
        assertTrue(repo.osBerichtUrl("spielpreview(80,1050,2)") == null)
    }

    @Test
    fun pokalGespieltesSpielHatBerichtUrl() {
        val pokal = repo.parsePokal(
            """
            <select name="saauswahl"><option value="24" selected>24</option></select>
            <select name="stauswahl"><option value="3" selected>3. Runde</option></select>
            <select name="landauswahl"><option value="6" selected>Deutschland</option></select>
            <table><tr>
              <td><a href="javascript:teaminfo(3449)">SC Viktoria Ulm</a></td><td>-</td>
              <td><a href="javascript:teaminfo(38)">SC Kaiserslautern</a></td><td>2 : 2</td>
              <td><a href="javascript:os_bericht(3449,38,3,24)">Klick</a></td>
            </tr></table>
            """.trimIndent(),
        )
        assertEquals(
            "https://os.ongapo.com/rep/saison/24/3/3449-38.html",
            pokal.runden.single().spiele.single().berichtUrl,
        )
    }

    @Test
    fun pokalErgebnisMitZusatzHatBerichtUrl() {
        val pokal = repo.parsePokal(
            """
            <select name="saauswahl"><option value="24" selected>24</option></select>
            <select name="stauswahl"><option value="3" selected>3. Runde</option></select>
            <select name="landauswahl"><option value="6" selected>Deutschland</option></select>
            <table><tr>
              <td><a href="javascript:teaminfo(3449)">SC Viktoria Ulm</a></td><td>-</td>
              <td><a href="javascript:teaminfo(38)">SC Kaiserslautern</a></td><td>2 : 1 n.V.</td>
              <td><a href="javascript:os_bericht(3449,38,3,24)">Klick</a></td>
            </tr></table>
            """.trimIndent(),
        )
        assertEquals(
            "https://os.ongapo.com/rep/saison/24/3/3449-38.html",
            pokal.runden.single().spiele.single().berichtUrl,
        )
    }

    @Test
    fun tabelle_filterUndOptionenAusDerSeite() {
        val tabelle = runCatching { repo.parseLigatabelle(dump("lt"), 3449) }.getOrNull()
        assertNotNull("lt parse failed", tabelle)
        // Eigenkonfiguration des Servers (2. Liga B / Deutschland / Saison 24).
        assertEquals(3, tabelle!!.filter?.liga)
        assertEquals(6, tabelle.filter?.land)
        assertEquals(24, tabelle.filter?.saison)
        // tabauswahl hat auf der Standardseite kein selected -> Standard „Gesamttabelle“.
        assertEquals(0, tabelle.filter?.tab)
        // Platzhalter-Optionen („---Ligaauswahl---“ u. Ä.) werden nicht übernommen.
        assertTrue("Liga-Optionen vorhanden", tabelle.ligaOptionen.size >= 7)
        assertTrue("1. Liga wählbar", tabelle.ligaOptionen.any { it.wert == 1 && it.label == "1. Liga" })
        assertTrue("2. Liga B wählbar", tabelle.ligaOptionen.any { it.wert == 3 && it.label == "2. Liga B" })
        assertTrue("Deutschland wählbar", tabelle.landOptionen.any { it.wert == 6 && it.label == "Deutschland" })
        assertTrue("Gesamttabelle wählbar", tabelle.tabOptionen.any { it.wert == 0 && it.label == "Gesamttabelle" })
        assertTrue("Kreuztabelle wählbar", tabelle.tabOptionen.any { it.wert == 10 })
        assertTrue("kein Platzhalter in Liga-Optionen", tabelle.ligaOptionen.none { it.label.startsWith("---") })
    }

    @Test
    fun spieltag_filterUndOptionenAusDerSeite() {
        val spieltag = runCatching { repo.parseSpieltag(dump("ls1")) }.getOrNull()
        assertNotNull("ls1 parse failed", spieltag)
        assertEquals(3, spieltag!!.liga)
        assertEquals(6, spieltag.land)
        assertEquals(24, spieltag.saison)
        assertTrue("Liga-Optionen vorhanden", spieltag.ligaOptionen.any { it.wert == 2 && it.label == "2. Liga A" })
        assertTrue("Land-Optionen vorhanden", spieltag.landOptionen.any { it.wert == 1 && it.label == "England" })
        assertTrue("Saison-Optionen vorhanden", spieltag.saisonOptionen.any { it.wert == 24 && it.label == "Saison 24" })
    }

    @Test
    fun pokal_landFilterUndOptionenAusDerSeite() {
        val pokal = runCatching { repo.parsePokal(dump("lp2")) }.getOrNull()
        assertNotNull("lp2 parse failed", pokal)
        assertEquals(6, pokal!!.land)
        assertTrue("Land-Optionen vorhanden", pokal.landOptionen.any { it.wert == 1 && it.label == "England" })
        assertTrue("Deutschland wählbar", pokal.landOptionen.any { it.wert == 6 && it.label == "Deutschland" })
        assertTrue("kein Platzhalter in Land-Optionen", pokal.landOptionen.none { it.label.startsWith("---") })
    }
}
