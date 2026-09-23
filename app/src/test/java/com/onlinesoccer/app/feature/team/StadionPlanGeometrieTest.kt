package com.onlinesoccer.app.feature.team

import androidx.compose.ui.geometry.Offset
import com.onlinesoccer.app.data.model.StadionBereich
import com.onlinesoccer.app.data.model.StadionLage
import com.onlinesoccer.app.data.model.StadionPlanDaten
import com.onlinesoccer.app.data.model.StadionPlanLogik
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Geometrie- und Detailtests für den Stadionplan (reine Berechnung, ohne Zeichnung). */
class StadionPlanGeometrieTest {

    private val canvasBreite = 414f
    private val canvasHoehe = 575f

    private fun geometrie(
        plan: StadionPlanDaten,
        breite: Float = canvasBreite,
        hoehe: Float = canvasHoehe,
    ): PlanGeometrie {
        val zonen = StadionPlanLogik.zonen(plan)
        return berechneGeometrie(breite, hoehe, plan, zonen)
    }

    private val aggregatPlan = StadionPlanDaten(
        stehplaetze = 3_000,
        sitzplaetze = 7_000,
        sitzUeberdacht = 4_000,
    )

    @Test
    fun pitch_hatSichtProportion() {
        val geo = geometrie(aggregatPlan)

        kotlin.math.abs(geo.pitch.width / geo.pitch.height - 68f / 105f) in 0f..0.02f
        assertEquals(68f / 105f, geo.pitch.width / geo.pitch.height, 0.02f)
    }

    @Test
    fun flaechen_deckenJedeSeiteAb() {
        val geo = geometrie(aggregatPlan)

        assertTrue("Bereichsflächen vorhanden", geo.flaechen.size >= 4)
        assertEquals(
            listOf(StadionLage.NORD, StadionLage.SUED, StadionLage.WEST, StadionLage.OST).toSet(),
            geo.flaechen.map { it.lage }.toSet(),
        )
        geo.flaechen.forEach { zf ->
            assertTrue("Fläche liegt im Bowl", zf.flaeche.left >= geo.aussen.left)
            assertTrue(zf.flaeche.top >= geo.aussen.top)
            assertTrue(zf.flaeche.right <= geo.aussen.right)
            assertTrue(zf.flaeche.bottom <= geo.aussen.bottom)
        }
    }

    @Test
    fun bereiche_erzeugenEigeneSektoren() {
        val plan = StadionPlanDaten(
            stehplaetze = 0,
            sitzplaetze = 45_000,
            bereiche = listOf(
                StadionBereich(name = "Nordtribüne West", lage = StadionLage.NORD, sitzplaetze = 10_000),
                StadionBereich(name = "Nordtribüne Ost", lage = StadionLage.NORD, sitzplaetze = 20_000),
                StadionBereich(name = "Südtribüne", lage = StadionLage.SUED, sitzplaetze = 15_000),
                StadionBereich(name = "Osttribüne", lage = StadionLage.OST, sitzplaetze = 8_000),
                StadionBereich(name = "Westtribüne", lage = StadionLage.WEST, sitzplaetze = 12_000),
            ),
        )
        val geo = geometrie(plan)

        assertEquals(5, geo.flaechen.size)
        val nordZwei = geo.flaechen.filter { it.lage == StadionLage.NORD }
        val sued = geo.flaechen.first { it.lage == StadionLage.SUED }.flaeche
        val west = geo.flaechen.first { it.lage == StadionLage.WEST }.flaeche
        val ost = geo.flaechen.first { it.lage == StadionLage.OST }.flaeche

        assertEquals("Zwei Nordsektoren", 2, nordZwei.size)
        val nordGross = nordZwei.map { it.flaeche }.maxBy { it.width }
        val nordKlein = nordZwei.map { it.flaeche }.minBy { it.width }

        assertTrue("Nordsektor 20k deutlich breiter als 10k", nordGross.width > nordKlein.width * 1.5f)
        assertTrue("Nordsektoren sitzen geschlossen im Bowl", nordKlein.left >= geo.aussen.left && nordGross.right <= geo.aussen.right)
        assertTrue("Südsektor fast voll breit (ein Sektor)", geo.innen.width - sued.width <= geo.innen.width * 0.02f)
        assertTrue("Ost- und Westsektor fast voll hoch", geo.innen.height - west.height <= geo.innen.height * 0.02f)
        assertTrue(geo.innen.height - ost.height <= geo.innen.height * 0.02f)
    }

    @Test
    fun zoneAnPunkt_trifftSektorUndNullAufPitch() {
        val geo = geometrie(aggregatPlan)

        geo.flaechen.forEach { zf ->
            val mitte = Offset(zf.flaeche.left + zf.flaeche.width / 2f, zf.flaeche.top + zf.flaeche.height / 2f)
            val index = zoneAnPunkt(mitte, geo)
            assertEquals("Mitte der Fläche trifft ihre Zone", zf.zoneIndex, index)
        }

        assertNull("Spielfeldmitte ist keine Zone", zoneAnPunkt(geo.pitch.center, geo))
    }

    @Test
    fun zoneAnPunkt_kurvenEckeWirdDerKurvenZoneZugeordnet() {
        val geo = geometrie(aggregatPlan)

        assertEquals("Nord- und Südkurve tragen Eckbereiche", 2, geo.kurven.size)
        assertEquals("Nordkurve oben", setOf(1f, -1f).intersect(geo.kurven.first { it.lage == StadionLage.NORD }.ecken.map { it.sx }), setOf(1f, -1f))
        assertTrue("Südkurve unten", geo.kurven.all { it.lage == StadionLage.NORD || it.lage == StadionLage.SUED })
        geo.kurven.forEach { kg ->
            assertTrue("Kurven-Zone ist als istKurve markiert", geo.zonen[kg.zoneIndex].istKurve)
            assertEquals("Kurven-Zone liegt auf der passenden Seite", kg.lage, geo.zonen[kg.zoneIndex].lage)
            kg.ecken.forEach { eck ->
                val mittelWinkel = (eck.startWinkel + 45f) * Math.PI / 180.0
                val radius = (geo.innenRadius + geo.eckRadius) / 2f
                val punkt = Offset(
                    eck.mitte.x + radius * kotlin.math.cos(mittelWinkel).toFloat(),
                    eck.mitte.y + radius * kotlin.math.sin(mittelWinkel).toFloat(),
                )
                val index = zoneAnPunkt(punkt, geo)
                assertNotNull("Eckmitte trifft ihre Zone", index)
                assertEquals("Ecke gehört zur Kurven-Zone", kg.zoneIndex, index)
            }
        }
    }

    @Test
    fun zoneAnPunkt_erkenntAuchDenLförmigenEckRestbereich() {
        val geo = geometrie(aggregatPlan)
        val nord = geo.kurven.first { it.lage == StadionLage.NORD }
        val nordwest = nord.ecken.first { it.sx > 0f && it.sy > 0f }

        // Punkt auf der Diagonalen zur Umlauf-Ecke, außerhalb des Viertelkreises (radius > eckRadius),
        // aber noch innerhalb des Bowl (nicht im weggeschnittenen Eck-Zwickel).
        val c = nordwest.mitte
        val dI = kotlin.math.hypot(geo.innen.left - c.x, geo.innen.top - c.y)
        val radius = (geo.eckRadius + dI) / 2f
        val dirX = (geo.innen.left - c.x) / dI
        val dirY = (geo.innen.top - c.y) / dI
        val punkt = Offset(c.x + radius * dirX, c.y + radius * dirY)
        assertTrue("Radius liegt außerhalb des Sektors", radius > geo.eckRadius)
        assertTrue("Punkt liegt noch im Bowl", punkt.x < geo.innen.left && punkt.y < geo.innen.top)

        assertEquals("L-förmiger Eckbereich gehört zur Kurven-Zone", nord.zoneIndex, zoneAnPunkt(punkt, geo))
    }

    @Test
    fun detailInhalt_tribueneInAggregatNenntSchätzung() {
        val plan = aggregatPlan
        val zone = StadionPlanLogik.zonen(plan).first { it.lage != null }
        val (titel, hinweis, zeilen) = detailInhalt(zone, plan)

        assertEquals("Haupttribüne", titel)
        assertTrue("Hinweis nennt die Schätzung", hinweis.contains("geschätzt"))
        assertTrue("Hinweis nennt Bundesliga-Vorbild", hinweis.contains("Bundesliga"))
        assertTrue(zeilen.toMap().containsKey("Sitzplätze"))
        assertTrue(zeilen.toMap().containsKey("Stehplätze"))
        assertTrue(zeilen.toMap().containsKey("Kapazität"))
    }

    @Test
    fun detailInhalt_kurvenZoneNenntSchätzungUndEigeneWerte() {
        val plan = aggregatPlan
        val kurvenZone = StadionPlanLogik.zonen(plan).first { it.istKurve && it.lage == StadionLage.NORD }
        val (titel, hinweis, zeilen) = detailInhalt(kurvenZone, plan)

        assertEquals("Nordkurve", titel)
        assertTrue("Hinweis nennt die Schätzung", hinweis.contains("geschätzt"))
        assertTrue("Hinweis nennt Bundesliga-Vorbild", hinweis.contains("Bundesliga"))
        assertTrue(zeilen.toMap().containsKey("Sitzplätze"))
        assertTrue(zeilen.toMap().containsKey("Stehplätze"))
        assertTrue(zeilen.toMap().containsKey("Kapazität"))
        assertEquals(kurvenZone.kapazitaet!!, kurvenZone.sitzplaetze!! + kurvenZone.stehplaetze!!)
    }

    @Test
    fun vip_liegtAlsStreifenAufDerHaupttribüne() {
        val plan = StadionPlanDaten(
            stehplaetze = 9_000,
            sitzplaetze = 36_000,
        )
        val geo = geometrie(plan)
        val vipZone = geo.zonen.first { it.name == "VIP- und Business-Bereich" }
        val vipIndex = geo.zonen.indexOf(vipZone)
        assertTrue("VIP ist Streifen ohne Sektor", vipZone.vipStreifen)

        // Westen teilt sich nur in Haupttribüne und Barrierebereich.
        val west = geo.flaechen.filter { it.lage == StadionLage.WEST }
        assertEquals(
            listOf("Haupttribüne", "Barrierefreie Plätze"),
            west.map { geo.zonen[it.zoneIndex].name },
        )
        assertTrue("VIP-Zone hat keine eigene Fläche", west.none { it.zoneIndex == vipIndex })

        val streifen = geo.streifen.first { it.zoneIndex == vipIndex }
        val haupt = west.first { geo.zonen[it.zoneIndex].name == "Haupttribüne" }.flaeche
        assertEquals("Streifen an der äußeren (linken) Kante", haupt.left, streifen.rechteck.left, 0.001f)
        assertTrue(streifen.rechteck.right >= haupt.left)
        assertTrue(streifen.rechteck.top >= haupt.top - 0.01f)
        assertTrue(streifen.rechteck.bottom <= haupt.bottom + 0.01f)
        assertTrue("Streifen deutlich schmaler als die Tribüne", streifen.rechteck.width < haupt.width * 0.5f)

        val mitteStreifen = Offset(streifen.rechteck.left + streifen.rechteck.width / 2f, streifen.rechteck.center.y)
        assertEquals("Streifenmitte trifft VIP", vipIndex, zoneAnPunkt(mitteStreifen, geo))

        val mitteHaupt = Offset(haupt.left + haupt.width * 0.6f, haupt.center.y)
        assertEquals("Mitte der Haupttribüne trifft Haupttribüne", west.first { geo.zonen[it.zoneIndex].name == "Haupttribüne" }.zoneIndex, zoneAnPunkt(mitteHaupt, geo))
    }

    @Test
    fun detailInhalt_eigenerBereichZeigtEchteWerte() {
        val plan = StadionPlanDaten(
            stehplaetze = 0,
            sitzplaetze = 15_000,
            bereiche = listOf(
                StadionBereich(name = "Nordtribüne", lage = StadionLage.NORD, sitzplaetze = 15_000, sitzUeberdacht = 15_000),
            ),
        )
        val zone = StadionPlanLogik.zonen(plan).first()
        val (titel, hinweis, zeilen) = detailInhalt(zone, plan)

        assertEquals("Nordtribüne", titel)
        assertEquals("", hinweis)
        assertEquals(15_000, zone.kapazitaet)
        assertEquals("15.000", zeilen.toMap()["Sitzplätze"])
        assertEquals("15.000", zeilen.toMap()["Kapazität"])
    }
}