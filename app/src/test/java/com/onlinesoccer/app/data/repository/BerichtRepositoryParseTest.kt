package com.onlinesoccer.app.data.repository

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BerichtRepositoryParseTest {
    private val repo = BerichtRepository(okhttp3.OkHttpClient())

    private fun dump(name: String): String =
        File("src/test/resources/dumps/$name.html").readText()

    @Test
    fun parseBericht_liestVereineUndStatistikAusEchterStruktur() {
        val bericht = repo.parseBericht(dump("bericht_real"), "test", null)

        assertEquals("FK 03 Lurup", bericht.heim)
        assertEquals("MSV Neugersdorf", bericht.gast)
        assertEquals("1:0", bericht.ergebnis)
        assertTrue(bericht.ereignisse.any { it.minute == "2" && it.tor })
        assertNotNull(bericht.statistik)
        assertEquals("5 : 6", bericht.statistik!!.abseits)
        assertEquals("49% : 51%", bericht.statistik!!.ballbesitz)
        assertEquals("Sehr hoch : Sehr hoch", bericht.statistik!!.fitness)
        assertEquals("Sehr hoch : Sehr hoch", bericht.statistik!!.moral)
    }

    @Test
    fun parseBericht_liestRasterSpielerUndStarteinstellungen() {
        val html = """
            <html><body>
            <h2>Heim FC - Gast FC</h2>
            <table>
              <tr><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
              <tr><td>N</td><td bgcolor="#cc0033"><b>A</b></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>N</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
              <tr><td>J</td><td bgcolor="#3377ff"><b>B</b></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>J</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
              <tr><td>D</td><td bgcolor="#009933"><b>C</b></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>D</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
              <tr><td>A</td><td bgcolor="#ffff00"><b>T</b></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>A</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
              <tr><td colspan="6"></td><td bgcolor="#ffff00"><b>T</b></td><td colspan="5"></td><td></td><td colspan="5"></td><td bgcolor="#ffff00"><b>T</b></td><td colspan="5"></td><td></td></tr>
              <tr><td><b class="H">A</b><b class="H">Angriff Heim</b></td></tr>
              <tr><td><b class="H">B</b><b class="H">Mitte Heim</b></td></tr>
              <tr><td><b class="H">C</b><b class="H">Abwehr Heim</b></td></tr>
              <tr><td><b class="H">T</b><b class="H">Tor Heim</b></td></tr>
              <tr><td><b class="G">A</b><b class="G">Angriff Gast</b></td></tr>
              <tr><td><b class="G">T</b><b class="G">Tor Gast</b></td></tr>
            </table>
            <table><tr><td></td><td>Heimteam</td><td></td><td>Auswärtsteam</td></tr>
              <tr><td>Einsatz</td><td>100%</td><td></td><td>90%</td></tr>
              <tr><td>Härte</td><td>Hart</td><td></td><td>Normal</td></tr>
              <tr><td>Spielweise</td><td>Offensiv</td><td></td><td>Defensiv</td></tr>
              <tr><td>Taktik - Sturm</td><td>Normal</td><td></td><td>Mittelstürmer</td></tr>
            </table>
            <div>Es folgt der Spielbericht :</div><div>Es folgen die Spielstatistiken</div>
            </body></html>
        """.trimIndent()

        val bericht = repo.parseBericht(html, "test", null)

        assertEquals(null, bericht.heimAufstellung!!.formation)
        assertEquals("Angriff Heim", bericht.heimAufstellung!!.spieler.first().name)
        assertEquals("Sturm", bericht.heimAufstellung!!.spieler.first().position)
        assertEquals(1, bericht.heimAufstellung!!.spieler.first().feldzeile)
        assertEquals(0, bericht.heimAufstellung!!.spieler.first().feldspalte)
        assertEquals("Torwart", bericht.heimAufstellung!!.spieler.last().position)
        assertEquals(15, bericht.heimAufstellung!!.spieler.last().feldzeile)
        assertEquals(5, bericht.heimAufstellung!!.spieler.last().feldspalte)
        assertEquals("100%", bericht.heimAufstellung!!.einstellungen.einsatz)
        assertEquals("Defensiv", bericht.gastAufstellung!!.einstellungen.spielweise)
        assertEquals(15, bericht.gastAufstellung!!.startspieler.last().feldzeile)
        assertEquals(5, bericht.gastAufstellung!!.startspieler.last().feldspalte)
    }

    @Test
    fun parseBericht_liestViktoriaUlmHeimrasterOhneSpielerlisteAlsRaster() {
        val positionen = mapOf(
            "A" to ("N" to 8), "B" to ("M" to 4), "C" to ("K" to 8),
            "D" to ("J" to 3), "E" to ("H" to 6), "F" to ("F" to 2),
            "G" to ("F" to 10), "H" to ("D" to 6), "K" to ("C" to 3),
            "L" to ("C" to 9),
        )
        val rollen = mapOf("A" to "#cc0033", "B" to "#cc0033", "C" to "#cc0033") +
            mapOf("D" to "#3377ff", "E" to "#3377ff", "F" to "#3377ff", "G" to "#3377ff") +
            mapOf("H" to "#009933", "K" to "#009933", "L" to "#009933")
        val auswaertsPositionen = mapOf(
            "A" to ("M" to 4), "B" to ("M" to 8), "C" to ("J" to 3),
            "D" to ("J" to 10), "E" to ("I" to 6), "F" to ("G" to 4),
            "G" to ("G" to 9), "H" to ("D" to 3), "K" to ("D" to 10),
            "L" to ("C" to 6),
        )
        val auswaertsRollen = mapOf("A" to "#cc0033", "B" to "#cc0033", "C" to "#3377ff", "D" to "#3377ff", "E" to "#3377ff", "F" to "#3377ff", "G" to "#3377ff", "H" to "#009933", "K" to "#009933", "L" to "#009933")
        fun rasterZeile(label: String): String = buildString {
            append("<tr><td>$label</td>")
            (1..11).forEach { column ->
                val slot = positionen.entries.firstOrNull { it.value.first == label && it.value.second == column }?.key
                if (slot == null) append("<td></td>")
                else append("<td bgcolor=\"${rollen.getValue(slot)}\"><b>$slot</b></td>")
            }
            append("<td>$label</td>")
            (1..11).forEach { column ->
                val slot = auswaertsPositionen.entries.firstOrNull { it.value.first == label && it.value.second == column }?.key
                if (slot == null) append("<td></td>")
                else append("<td bgcolor=\"${auswaertsRollen.getValue(slot)}\"><b>$slot</b></td>")
            }
            append("<td>$label</td></tr>")
        }
        val spieler = listOf(
            "A" to "Camilo Baruco", "B" to "Olev Tikerpää", "C" to "Ruslan Hryshukevich",
            "D" to "Alain Achten", "E" to "Tomás Simao", "F" to "Oskari Konsa",
            "G" to "Janar Pihel", "H" to "Flavio Zurlinden", "K" to "Licínio Maurício",
            "L" to "Alexey Zuyev", "T" to "Kuldar Mitt",
        )
        val auswaertsSpieler = listOf(
            "A" to "Francois Schneider", "B" to "Stuart Ward", "C" to "Yiorgos Christodoulou",
            "D" to "Thomas Siemund", "E" to "Silvan Heierli", "F" to "Thièrry Foucher",
            "G" to "Rüdiger Hauser", "H" to "Abieser Rimon", "K" to "Roy Pherson",
            "L" to "Samuel Romano", "T" to "Henry Dondelinger",
        )
        val html = buildString {
            append("<html><body><h2>SC Viktoria Ulm - SC Kaiserslautern</h2><table>")
            "ONMLKJIHGFEDCBA".forEach { append(rasterZeile(it.toString())) }
            append("<tr><td colspan=\"6\"></td><td bgcolor=\"#ffff00\"><b>T</b></td><td colspan=\"5\"></td><td></td><td colspan=\"5\"></td><td bgcolor=\"#ffff00\"><b>T</b></td><td colspan=\"5\"></td><td></td></tr>")
            spieler.forEach { (slot, name) -> append("<tr><td><b class=\"H\">$slot</b></td><td></td><td><b class=\"H\">$name</b></td><td colspan=\"8\"></td></tr>") }
            auswaertsSpieler.forEach { (slot, name) -> append("<tr><td><b class=\"G\">$slot</b></td><td></td><td><b class=\"G\">$name</b></td><td colspan=\"8\"></td></tr>") }
            append("</table><div>Es folgt der Spielbericht :</div><div>Es folgen die Spielstatistiken</div></body></html>")
        }

        val bericht = repo.parseBericht(html, "test", null)
        val gelesen = bericht.heimAufstellung!!.startspieler.associateBy { it.nummer }

        positionen.forEach { (slot, erwartet) ->
            assertEquals(erwartet.second - 1, gelesen[slot]!!.feldspalte)
            assertEquals("ONMLKJIHGFEDCBA".indexOf(erwartet.first), gelesen[slot]!!.feldzeile)
        }
        assertEquals(15, gelesen["T"]!!.feldzeile)
        assertEquals(5, gelesen["T"]!!.feldspalte)

        val gastGelesen = bericht.gastAufstellung!!.startspieler.associateBy { it.nummer }
        auswaertsPositionen.forEach { (slot, erwartet) ->
            assertEquals(erwartet.second - 1, gastGelesen[slot]!!.feldspalte)
            assertEquals("ONMLKJIHGFEDCBA".indexOf(erwartet.first), gastGelesen[slot]!!.feldzeile)
        }
        assertEquals(15, gastGelesen["T"]!!.feldzeile)
        assertEquals(5, gastGelesen["T"]!!.feldspalte)
    }
}
