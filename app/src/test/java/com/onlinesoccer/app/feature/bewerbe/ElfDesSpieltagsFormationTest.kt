package com.onlinesoccer.app.feature.bewerbe

import com.onlinesoccer.app.data.model.ElfSpieler
import com.onlinesoccer.app.data.model.SpielerPosition
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Geometrie-Tests für die Elf-des-Spieltags-Aufstellung: Die Formation wird
 * als taktisches Board angeordnet – jeder Spieler mit eigener X- UND
 * Y-Koordinate, gestaffelt innerhalb der Positionsgruppen. Es wird geprüft,
 * dass vollständige Spielerelemente (Bewertung, Wappen, Nachname, Vereinsname)
 * sich auf keiner Gerätegröße überlappen und alle 11 gleichzeitig sichtbar sind.
 */
class ElfDesSpieltagsFormationTest {

    private fun spieler(name: String, position: SpielerPosition) = ElfSpieler(
        name = "$name $position",
        verein = "$name City",
        position = position,
        bewertung = 8.0,
        teamId = 1,
    )

    private fun linie(index: Int, count: Int, position: SpielerPosition): SpielerLine {
        val liste = (0 until count).map { j -> spieler("Spieler$index$j", position) }
        return SpielerLine(index, liste)
    }

    /**
     * 3-4-3: STU 3, MIT 4, ABW 3, TOR 1 – das Zielbild aus der Anforderung.
     */
    private fun formation343(): List<SpielerLine> = listOf(
        linie(0, 3, SpielerPosition.STU),
        linie(1, 4, SpielerPosition.MIT),
        linie(2, 3, SpielerPosition.ABW),
        linie(3, 1, SpielerPosition.TOR),
    )

    /** Die übrigen üblichen Formationen als Regressionsprüfung. */
    private fun formations(): Map<String, List<SpielerLine>> = mapOf(
        "3-4-3" to formation343(),
        "4-2-3-1" to listOf(
            linie(0, 1, SpielerPosition.STU),
            linie(1, 3, SpielerPosition.OMI),
            linie(2, 2, SpielerPosition.DMI),
            linie(3, 4, SpielerPosition.ABW),
            linie(4, 1, SpielerPosition.TOR),
        ),
        "4-1-4-1" to listOf(
            linie(0, 1, SpielerPosition.STU),
            linie(1, 4, SpielerPosition.MIT),
            linie(2, 1, SpielerPosition.DMI),
            linie(3, 4, SpielerPosition.ABW),
            linie(4, 1, SpielerPosition.TOR),
        ),
        "4-4-2 Raute" to listOf(
            linie(0, 2, SpielerPosition.STU),
            linie(1, 1, SpielerPosition.OMI),
            linie(2, 2, SpielerPosition.MIT),
            linie(3, 1, SpielerPosition.DMI),
            linie(4, 4, SpielerPosition.ABW),
            linie(5, 1, SpielerPosition.TOR),
        ),
        "4-3-3" to listOf(
            linie(0, 3, SpielerPosition.STU),
            linie(1, 3, SpielerPosition.MIT),
            linie(2, 4, SpielerPosition.ABW),
            linie(3, 1, SpielerPosition.TOR),
        ),
        "4-4-2" to listOf(
            linie(0, 2, SpielerPosition.STU),
            linie(1, 4, SpielerPosition.MIT),
            linie(2, 4, SpielerPosition.ABW),
            linie(3, 1, SpielerPosition.TOR),
        ),
        "3-5-2" to listOf(
            linie(0, 2, SpielerPosition.STU),
            linie(1, 5, SpielerPosition.MIT),
            linie(2, 3, SpielerPosition.ABW),
            linie(3, 1, SpielerPosition.TOR),
        ),
    )

    /** Gerätegrößen (dp) → Pixel bei Dichte 3,0. */
    private fun geraete(): Map<String, Pair<Int, Int>> = mapOf(
        "Smartphone Portrait" to ((360 * 3) to (640 * 3)),
        "Smartphone Landscape" to ((640 * 3) to (360 * 3)),
        "Tablet Portrait" to ((768 * 3) to (1024 * 3)),
        "Tablet Landscape" to ((1024 * 3) to (768 * 3)),
    )

    private val density = 3.0f

    private fun berechne(
        linien: List<SpielerLine>,
        breite: Int,
        hoehe: Int,
    ): List<FormationPlatz> {
        val randPx = (breite * SPIELFELD_RAND_ANTEIL).roundToInt()
        val abstandPx = (4 * density).roundToInt()
        val minSlot = (40 * density).roundToInt()
        val maxSlot = (150 * density).roundToInt()
        val ankerY = zeilenAnkerY(linien.size)

        val slotBreiten = IntArray(linien.size) { i ->
            slotBreiteFuerLinie(linien[i].spieler.size, breite, randPx, abstandPx, minSlot, maxSlot)
        }
        // Wie die UI: Elemente werden auf ihren Slot begrenzt gemessen.
        val elementHoehe = (formationElementMaxHoeheDp(hoehe / density, linien.size) * density).roundToInt()
        val breiten = linien.flatMapIndexed { i, liste -> List(liste.spieler.size) { slotBreiten[i] } }
        val hoehen = List(breiten.size) { elementHoehe }

        return berechneFormationPlaetze(
            linien = linien,
            ankerY = ankerY,
            breite = breite,
            hoehe = hoehe,
            randPx = randPx,
            abstandPx = abstandPx,
            slotBreiten = slotBreiten,
            elementBreiten = breiten,
            elementHoehen = hoehen,
        )
    }

    private fun rechtecke(
        plaetze: List<FormationPlatz>,
        breiten: List<Int>,
        hoehen: List<Int>,
    ): List<IntArray> = plaetze.mapIndexed { i, p ->
        intArrayOf(
            p.x - breiten[i] / 2,
            p.y - hoehen[i] / 2,
            p.x + (breiten[i] + 1) / 2,
            p.y + (hoehen[i] + 1) / 2,
        )
    }

    private fun ueberlappen(a: IntArray, b: IntArray): Boolean =
        a[0] < b[2] && a[2] > b[0] && a[1] < b[3] && a[3] > b[1]

    @Test
    fun dreivierdrei_alleElevenSichtbarUndUeberlappungsfrei() {
        val formation = formation343()
        assertEquals(11, formation.sumOf { it.spieler.size })

        geraete().forEach { (name, groesse) ->
            val (breite, hoehe) = groesse
            val randPx = (breite * SPIELFELD_RAND_ANTEIL).roundToInt()
            val lineCounts = formation.map { it.spieler.size }
            // 3-4-3 → Zeilengrößen 3, 4, 3, 1.
            assertEquals(listOf(3, 4, 3, 1), lineCounts)

            val plaetze = berechne(formation, breite, hoehe)
            assertEquals(11, plaetze.size)
            val breiten = slotBreiten(formation, breite)
            val hoehen = List(11) { elementHoehePx(formation.size, hoehe) }
            val rects = rechtecke(plaetze, breiten, hoehen)

            // Vollständig im Feld und Randabstand.
            rects.forEach { r ->
                assertTrue("$name: Element ragt links heraus $r", r[0] >= 0)
                assertTrue("$name: Element ragt oben heraus $r", r[1] >= 0)
                assertTrue("$name: Element ragt rechts heraus $r", r[2] <= breite)
                assertTrue("$name: Element ragt unten heraus $r", r[3] <= hoehe)
            }

            // Keine zwei vollständigen Spielerelemente überlappen sich.
            for (i in rects.indices) {
                for (j in i + 1 until rects.size) {
                    assertTrue(
                        "$name: Spieler $i und $j überlappen (${rects[i].toList()} vs ${rects[j].toList()})",
                        !ueberlappen(rects[i], rects[j]),
                    )
                }
            }

            // Saubere Ausrichtung: Spieler derselben Positionsgruppe liegen auf
            // derselben horizontalen Linie (Sturm 0-2, Mittelfeld 3-6, Abwehr 7-9).
            assertTrue("$name: Sturm nicht auf einer Linie", plaetze[0].y == plaetze[1].y && plaetze[1].y == plaetze[2].y)
            assertTrue("$name: Mittelfeld nicht auf einer Linie", plaetze[3].y == plaetze[4].y && plaetze[4].y == plaetze[5].y && plaetze[5].y == plaetze[6].y)
            assertTrue("$name: Abwehr nicht auf einer Linie", plaetze[7].y == plaetze[8].y && plaetze[8].y == plaetze[9].y)

            // Torwart exakt zentral und am tiefsten Punkt der Elf.
            val tw = rects[10]
            val mitte = breite / 2
            assertTrue("$name: Torwart nicht zentral (${tw[0]}..${tw[2]}})", tw[0] < mitte && tw[2] > mitte)
            rects.take(10).forEach { r ->
                assertTrue("$name: Torwart nicht tiefster Spieler", r[3] <= tw[3])
            }
        }
    }

    @Test
    fun alleFormationenUeberlappungsfreiAufAllenGeraeten() {
        formations().forEach { (bezeichner, formation) ->
            geraete().forEach { (gerät, groesse) ->
                val (breite, hoehe) = groesse
                val plaetze = berechne(formation, breite, hoehe)
                if (formation.sumOf { it.spieler.size } != 11) return@forEach
                val breiten = slotBreiten(formation, breite)
                val hoehen = List(plaetze.size) { elementHoehePx(formation.size, hoehe) }
                val rects = rechtecke(plaetze, breiten, hoehen)
                for (i in rects.indices) {
                    for (j in i + 1 until rects.size) {
                        assertTrue(
                            "$bezeichner@$gerät: Spieler $i und $j überlappen " +
                                "(${rects[i].toList()} vs ${rects[j].toList()})",
                            !ueberlappen(rects[i], rects[j]),
                        )
                    }
                }
            }
        }
    }

    private fun slotBreiten(linien: List<SpielerLine>, breite: Int): List<Int> {
        val randPx = (breite * SPIELFELD_RAND_ANTEIL).roundToInt()
        val abstandPx = (4 * density).roundToInt()
        val minSlot = (40 * density).roundToInt()
        val maxSlot = (150 * density).roundToInt()
        val slots = IntArray(linien.size) { i ->
            slotBreiteFuerLinie(linien[i].spieler.size, breite, randPx, abstandPx, minSlot, maxSlot)
        }
        return linien.flatMapIndexed { i, liste -> List(liste.spieler.size) { slots[i] } }
    }

    private fun elementHoehePx(anzahlZeilen: Int, hoehe: Int): Int =
        (formationElementMaxHoeheDp(hoehe / density, anzahlZeilen) * density).roundToInt()
}