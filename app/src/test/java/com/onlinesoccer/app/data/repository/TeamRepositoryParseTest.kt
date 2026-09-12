package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.SonderFaehigkeit
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressionstests für die Kaderübersicht (`showteam.php?s=0`) gegen echte
 * Server-Dumps (app/src/test/resources/dumps/kader.html).
 */
class TeamRepositoryParseTest {

    private val repo = TeamRepository(okhttp3.OkHttpClient(), ZugabgabeRepository(okhttp3.OkHttpClient()))

    private fun dump(name: String): String =
        File("src/test/resources/dumps/$name.html").readText()

    @Test
    fun kader_spalteS_liefertSonderfaehigkeiten() {
        val kader = repo.parseKader(dump("kader"))

        val byPid = kader.associateBy { it.pid }
        assertEquals(
            "Torwart Lars Vincez hat Elfmeterkiller",
            listOf(SonderFaehigkeit.ELFMETERKILLER),
            byPid[97944]?.sonderFaehigkeiten,
        )
        assertEquals(
            "Kristo Dragiev hat Libero",
            listOf(SonderFaehigkeit.LIBERO),
            byPid[139649]?.sonderFaehigkeiten,
        )
        assertEquals(
            "Oskari Konsa hat Libero und Spielmacher",
            listOf(SonderFaehigkeit.LIBERO, SonderFaehigkeit.SPIELMACHER),
            byPid[121743]?.sonderFaehigkeiten,
        )
        assertEquals(
            "Janar Pihel hat Spielmacher, Freistoss-Spezialist und Flankengott",
            listOf(SonderFaehigkeit.SPIELMACHER, SonderFaehigkeit.FREISTOSSSPEZIALIST, SonderFaehigkeit.FLANKENGOTT),
            byPid[116286]?.sonderFaehigkeiten,
        )
        assertEquals(
            "Camilo Baruco hat Freistoss-Spezialist und Torinstinkt",
            listOf(SonderFaehigkeit.FREISTOSSSPEZIALIST, SonderFaehigkeit.TORINSTINKT),
            byPid[132483]?.sonderFaehigkeiten,
        )
    }

    @Test
    fun kader_spielerOhneSonderfaehigkeitIstLeer() {
        val kader = repo.parseKader(dump("kader"))
        val ohne = kader.first { it.pid == 174521L } // Fabian Röwer (OMI)
        assertTrue("Spieler ohne Sonderfähigkeit: leere Liste", ohne.sonderFaehigkeiten.isEmpty())
    }

    @Test
    fun kader_spalteSperre_liefertAktiveSperren() {
        val kader = repo.parseKader(dump("kader"))

        val tommas = kader.first { it.pid == 121678L } // Tomás Simao: „1L“
        assertEquals("1L", tommas.sperre)
        assertTrue("„1L“ = gesperrt", tommas.gesperrt)

        val olev = kader.first { it.pid == 126675L } // Olev Tikerpää: „2I“
        assertEquals("2I", olev.sperre)
        assertTrue("„2I“ = gesperrt", olev.gesperrt)

        val harald = kader.first { it.pid == 169711L } // Harald Philippi: „2P“
        assertEquals("2P", harald.sperre)
        assertTrue("„2P“ = gesperrt", harald.gesperrt)
    }

    @Test
    fun kader_ohneSperre_istNichtGesperrt() {
        val kader = repo.parseKader(dump("kader"))

        val lars = kader.first { it.pid == 97944L } // Lars Vincez, FIT=97
        assertEquals("0", lars.sperre)
        assertFalse("Lars Vincez nicht gesperrt", lars.gesperrt)
    }

    @Test
    fun kader_fitness0_zeigtKeinenStatusWieAufDerWebsite() {
        val kader = repo.parseKader(dump("kader"))

        // Flavio Zurlinden hat FIT=0; die Website zeigt in der Sperre-Spalte keinen
        // Hinweis (Sperre „0“, kein abbr/title) – die App darf daher keinen
        // Verletzt-Status ableiten.
        val flavio = kader.first { it.pid == 115904L } // Flavio Zurlinden, FIT=0
        assertEquals(0, flavio.fit)
        assertEquals("0", flavio.sperre)
        assertFalse("Flavio nicht gesperrt", flavio.gesperrt)
    }

    @Test
    fun kader_unbekanntesKuerzelWirdIgnoriert() {
        val html = """
            <table id="team">
              <tr><td>#</td><td>Nr.</td><td>Name</td><td>Alter</td><td>Pos</td>
                  <td>MOR</td><td>FIT</td><td>Skillschnitt</td><td>Opt.Skill</td><td>S</td></tr>
              <tr>
                <td>1</td><td>10</td><td><a href="sp.php?s=999">Test</a></td><td>25</td><td>MIT</td>
                <td>80</td><td>90</td><td>70</td><td>85</td>
                <td><abbr title="Unbekannt">X</abbr><abbr title="Spielmacher">S</abbr></td>
              </tr>
            </table>
        """.trimIndent()

        val kader = repo.parseKader(html)

        assertEquals("Unbekanntes Kürzel wird ignoriert, S bleibt", listOf(SonderFaehigkeit.SPIELMACHER), kader.single().sonderFaehigkeiten)
    }

    @Test
    fun fremderKader_st902_liefertVereinUndKader() {
        val fremd = repo.parseFremdesTeam(dump("st902"), 902L)
        assertEquals("Olympique Mansfeldia", fremd.name)
        assertEquals("1. Liga", fremd.liga)
        assertTrue("Kader nicht leer", fremd.kader.isNotEmpty())
        val ullrich = fremd.kader.first { it.pid == 86001L }
        assertEquals("Tom Ullrich", ullrich.name)
        assertEquals(com.onlinesoccer.app.data.model.SpielerPosition.TOR, ullrich.position)
        assertEquals(0, ullrich.fit)
        assertEquals(0, ullrich.mor)
    }

    @Test
    fun fremderSpieler_sp86001_liefertProfil() {
        val profil = repo.parseSpielerProfil(dump("sp86001"))
        assertEquals("Tom Ullrich", profil.name)
        assertEquals(35, profil.alter)
        assertEquals(com.onlinesoccer.app.data.model.SpielerPosition.TOR, profil.position)
        assertEquals("8.669.243 EUR", profil.vertrag?.marktwert)
        assertTrue(profil.staerken.containsKey("Abstoss"))
        assertEquals("56", profil.staerken["Abstoss"])
        assertTrue("Stärken gefüllt", profil.staerken.isNotEmpty())
        assertTrue("Statistik Saison gefüllt", profil.statistikSaison.isNotEmpty())
        assertTrue("Statistik Karriere gefüllt", profil.statistikGesamt.isNotEmpty())
        assertEquals("154", profil.statistikGesamt["Spiele (LI)"])
    }

    @Test
    fun transfermarktSpieler_sp114490_liefertVollstaendigesProfil() {
        val profil = repo.parseSpielerProfil(dump("sp114490"))

        assertEquals("Salvatore Catone", profil.name)
        assertEquals(29, profil.alter)
        assertEquals(com.onlinesoccer.app.data.model.SpielerPosition.OMI, profil.position)
        assertEquals("1.354.153 EUR", profil.vertrag?.marktwert)
        assertEquals("13.103 EUR", profil.vertrag?.gehalt)
        assertTrue(profil.staerken.containsKey("Skillschnitt"))
        assertEquals("38.76", profil.staerken["Skillschnitt"])
        assertEquals("50.93", profil.staerken["Opt. Skill"])
        assertTrue("Stärken gefüllt", profil.staerken.size >= 18)
        assertTrue("Statistik Karriere gefüllt", profil.statistikGesamt.isNotEmpty())
        assertEquals("3", profil.statistikGesamt["Tore (FS)"])
    }
}