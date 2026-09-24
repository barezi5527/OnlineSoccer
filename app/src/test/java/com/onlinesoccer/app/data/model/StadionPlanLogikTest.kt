package com.onlinesoccer.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/** Logiktests für den Stadionplan (reine Datenlogik, ohne UI). */
class StadionPlanLogikTest {

    @Test
    fun aggregat_ohneBereiche_bautTribuenenNachGröße() {
        // Kleines Stadion: nur Haupt- und Gegentribüne, noch keine Kurven.
        val klein = StadionPlanDaten(
            stehplaetze = 3_000,
            sitzplaetze = 7_000,
        )
        val zonen = StadionPlanLogik.zonen(klein)

        assertEquals("Kleines Stadion: 2 Tribünen (noch keine Kurven)", 2, zonen.size)
        assertEquals(
            listOf("Haupttribüne", "Gegentribüne"),
            zonen.map { it.name },
        )
        assertEquals(
            listOf(StadionLage.WEST, StadionLage.OST),
            zonen.map { it.lage },
        )
        assertEquals("Kurzseiten sind noch unbestückt", 0, zonen.count { it.istKurve })
        zonen.forEach { zone ->
            assertTrue("Jede Zone hat geschätzte Plätze", zone.hatAufteilung)
        }
        assertEquals("Sitz-Summe exakt", 7_000, zonen.sumOf { it.sitzplaetze!! })
        assertEquals("Steh-Summe exakt", 3_000, zonen.sumOf { it.stehplaetze!! })

        // Großes Stadion ab 20.000: geschlossener Ring mit beiden Kurven.
        val gross = StadionPlanDaten(
            stehplaetze = 10_000,
            sitzplaetze = 30_000,
        )
        val zonenGross = StadionPlanLogik.zonen(gross)
        assertTrue("Südkurve ist Kurve", zonenGross.first { it.name == "Südkurve" }.istKurve)
        assertTrue("Nordkurve ist Kurve", zonenGross.first { it.name == "Nordkurve" }.istKurve)
        val sued = zonenGross.first { it.lage == StadionLage.SUED }
        val nord = zonenGross.first { it.lage == StadionLage.NORD }
        assertTrue("Südkurve trägt geschätzte Stehplätze", sued.hatAufteilung && (sued.stehplaetze ?: 0) > 0)
        assertTrue("Nordkurve trägt geschätzte Stehplätze", nord.hatAufteilung && (nord.stehplaetze ?: 0) > 0)
        assertEquals("Sitz-Summe exakt", 30_000, zonenGross.sumOf { it.sitzplaetze!! })
        assertEquals("Steh-Summe exakt", 10_000, zonenGross.sumOf { it.stehplaetze!! })
    }

    @Test
    fun detaillierungsGrad_hängtVonDerKapazitätAb() {
        assertEquals(Detaillierungsgrad.KLEIN, StadionPlanLogik.detaillierungsGrad(5_000))
        assertEquals(Detaillierungsgrad.MITTEL, StadionPlanLogik.detaillierungsGrad(20_000))
        assertEquals(Detaillierungsgrad.GROSS, StadionPlanLogik.detaillierungsGrad(45_000))
        assertEquals(Detaillierungsgrad.SEHR_GROSS, StadionPlanLogik.detaillierungsGrad(90_000))
    }

    @Test
    fun sehrGrosseStadien_zeigenAlleSonderbereiche() {
        val gross = StadionPlanLogik.kategorien(
            StadionPlanDaten(stehplaetze = 8_000, sitzplaetze = 47_000),
        ).map { it.anzeigeName }
        assertTrue("Groß (55k): Gästeblock vorhanden", "Gäste" in gross)
        assertTrue("Groß (55k): VIP vorhanden", "VIP- und Business-Bereich" in gross)
        assertTrue("Groß (55k): Barrierefrei vorhanden", "Barrierefreie Plätze" in gross)

        val sehrGross = StadionPlanLogik.zonen(
            StadionPlanDaten(stehplaetze = 10_000, sitzplaetze = 60_000),
        )
        assertTrue("Kurven-Zonen tragen Plätze", sehrGross.filter { it.istKurve }.all { it.hatAufteilung && it.sitzplaetze!! >= 0 })
        assertEquals("Sitzsumme exakt", 60_000, sehrGross.sumOf { it.sitzplaetze ?: 0 })
        assertEquals("Stehsumme exakt", 10_000, sehrGross.sumOf { it.stehplaetze ?: 0 })
    }

    @Test
    fun gaesteblock_liegtInEckeOderAlsSektorInDerSuedkurve() {
        val plan = StadionPlanDaten(
            stehplaetze = 9_000,
            sitzplaetze = 36_000,
        )
        val gaeste = StadionPlanLogik.zonen(plan).first { it.name == "Gäste" }
        val ecke = StadionPlanLogik.variante(plan).gaesteblockEcke
        if (ecke == null) {
            assertEquals("Sektordarstellung auf der Südseite (ohne Ecke)", StadionLage.SUED, gaeste.lage)
            assertTrue("Sektor ist keine Kurve", !gaeste.istKurve)
        } else {
            assertEquals("Eckblock liegt auf der Seite der Ecke", ecke.lage, gaeste.lage)
            assertEquals(ecke, gaeste.ecke)
            assertTrue("Eckblock ist Kurven-Zone", gaeste.istKurve)
        }
    }

    @Test
    fun gaesteblockEcke_nurAbGeschlossenemRingUndDeterministisch() {
        assertEquals("Kleines Stadion: kein Eck-Gästeblock", null, StadionPlanLogik.variante(plan(9_000, 10_000)).gaesteblockEcke)
        assertEquals("Kleines Stadion: kein Eck-Gästeblock", null, StadionPlanLogik.variante(plan(5_000, 14_000)).gaesteblockEcke)
        val gross = plan(9_000, 36_000)
        val v = StadionPlanLogik.variante(gross)
        if (v.grundform == Grundform.OFFENE_ECKEN) {
            assertNull("Offene Ecken: kein Eck-Gästeblock", v.gaesteblockEcke)
        } else {
            assertNotNull("Großes Stadion: Eck-Gästeblock", v.gaesteblockEcke)
            assertTrue(
                "Gästeblock sitzt immer an der Nordkurve",
                v.gaesteblockEcke!!.lage == StadionLage.NORD,
            )
        }
        assertEquals(
            "Deterministisch je Plan",
            StadionPlanLogik.variante(gross).gaesteblockEcke,
            StadionPlanLogik.variante(gross).gaesteblockEcke,
        )
    }

    private fun plan(steh: Int, sitz: Int): StadionPlanDaten =
        StadionPlanDaten(stehplaetze = steh, sitzplaetze = sitz)

    @Test
    fun keinGasteblockUnterDerSchwelle() {
        val plan = StadionPlanDaten(
            stehplaetze = 2_000,
            sitzplaetze = 8_000,
        )
        assertTrue(
            "Kein Gästeblock unter 12.000",
            StadionPlanLogik.zonen(plan).none { it.name == "Gäste" },
        )
    }

    @Test
    fun keineDoppelzaehlungVipUndBarrierefrei() {
        val plan = StadionPlanDaten(
            stehplaetze = 9_000,
            sitzplaetze = 36_000,
        )
        val zonen = StadionPlanLogik.zonen(plan)
        val belegt = zonen.filter { it.hatAufteilung }
        val kategorien = StadionPlanLogik.kategorien(plan)

        assertEquals(
            "Alle Zonen haben Aufteilung",
            listOf("Haupttribüne", "Gegentribüne", "Südkurve", "Nordkurve", "Gäste", "VIP- und Business-Bereich", "Barrierefreie Plätze"),
            belegt.map { it.name },
        )
        assertEquals("Sitzsumme exakt (ohne Doppelzählung)", 36_000, belegt.sumOf { it.sitzplaetze ?: 0 })
        assertEquals("Stehsumme exakt", 9_000, belegt.sumOf { it.stehplaetze ?: 0 })

        val vip = belegt.first { it.name == "VIP- und Business-Bereich" }
        val barriere = belegt.first { it.name == "Barrierefreie Plätze" }
        val haupttribuene = belegt.first { it.name == "Haupttribüne" }
        assertEquals(
            "VIP-Kategorie zählt exakt die VIP-Zone",
            kategorien.first { it.name == "VIP- und Business-Bereich" }.plaetze,
            vip.kapazitaet!!,
        )
        assertEquals(
            "Barrierefrei-Kategorie zählt exakt die Barrierefrei-Zone",
            kategorien.first { it.name == "Barrierefreie Plätze" }.plaetze,
            barriere.kapazitaet!!,
        )
        assertTrue("VIP hat eigene Plätze", vip.kapazitaet!! > 0)
        assertTrue("Barrierefrei hat eigene Plätze", barriere.kapazitaet!! > 0)
        assertTrue("VIP belegt keinen eigenen Sektor", vip.vipStreifen)
        assertTrue("Barrierefrei ist ein eigener Bereich", !barriere.vipStreifen)
        assertTrue("Barrierefrei ist größer als der VIP-Streifen", barriere.kapazitaet!! > vip.kapazitaet!!)
    }

    @Test
    fun vip_istAlsStreifenTeilDerHaupttribüne() {
        val plan = StadionPlanDaten(
            stehplaetze = 9_000,
            sitzplaetze = 36_000,
        )
        val zonen = StadionPlanLogik.zonen(plan)

        val vip = zonen.first { it.name == "VIP- und Business-Bereich" }
        val haupt = zonen.first { it.name == "Haupttribüne" }
        assertEquals("VIP liegt auf der Westseite", StadionLage.WEST, vip.lage)
        assertTrue("VIP ist reine Sitzzone", vip.stehplaetze == 0 && vip.sitzAnteil == 1f)
        assertEquals("VIP = oberste zwei Reihen (2 %) der Sitzplätze", (plan.sitzplaetze * 0.02f).roundToInt(), vip.kapazitaet!!)
        assertTrue("Haupttribüne bleibt Sitz-dominant", haupt.kapazitaet!! > vip.kapazitaet!!)
        assertEquals(
            "Sitzsumme aller Zonen exakt",
            plan.sitzplaetze,
            zonen.filter { it.hatAufteilung }.sumOf { it.sitzplaetze ?: 0 },
        )
    }

    @Test
    fun kappung_sehrGekappt_summiertOhneUeberziehung() {
        // Steh-dominantes Stadion: VIP/Barrierefrei kappen mehr als Sitzplätze
        // vorhanden. Die Aufteilung darf nie mehr Sitzplätze ausweisen, als
        // tatsächlich existieren (keine Doppelzählung), egal wie die Rundung fällt.
        val plan = StadionPlanDaten(
            stehplaetze = 35_750,
            sitzplaetze = 155,
        )
        val zonen = StadionPlanLogik.zonen(plan)
        val belegt = zonen.filter { it.hatAufteilung }

        val vip = belegt.firstOrNull { it.name == "VIP- und Business-Bereich" }
        val barriere = belegt.firstOrNull { it.name == "Barrierefreie Plätze" }

        assertTrue("VIP wird gebildet (≥ 35.000)", vip != null)
        assertTrue("Barrierefrei wird gebildet (≥ 15.000)", barriere != null)
        assertEquals(
            "VIP + Barrierefrei exakt auf vorhandene Sitzplätze gekappt",
            plan.sitzplaetze,
            (vip!!.sitzplaetze ?: 0) + (barriere!!.sitzplaetze ?: 0),
        )
        assertEquals("Sitzsumme über alle Zonen exakt", plan.sitzplaetze, belegt.sumOf { it.sitzplaetze ?: 0 })
        assertEquals("Stehsumme über alle Zonen exakt", 35_750, belegt.sumOf { it.stehplaetze ?: 0 })
    }

    @Test
    fun grosseStadien_summierenExaktUeberAlleZonen() {
        val plan = StadionPlanDaten(
            stehplaetze = 9_000,
            sitzplaetze = 36_000,
            stehUeberdacht = 4_000,
            sitzUeberdacht = 22_000,
        )
        val zonen = StadionPlanLogik.zonen(plan)
        val belegt = zonen.filter { it.hatAufteilung }

        assertTrue("Gästeblock vorhanden (≥ 12.000)", belegt.any { it.name == "Gäste" })
        assertEquals("Sitzsumme über alle Zonen exakt", 36_000, belegt.sumOf { it.sitzplaetze ?: 0 })
        assertEquals("Stehsumme über alle Zonen exakt", 9_000, belegt.sumOf { it.stehplaetze ?: 0 })
    }

    @Test
    fun bereiche_mitLage_erzeugenEchteZonen() {
        val plan = StadionPlanDaten(
            stehplaetze = 8_000,
            sitzplaetze = 37_000,
            bereiche = listOf(
                StadionBereich(name = "Nordtribüne", lage = StadionLage.NORD, sitzplaetze = 15_000),
                StadionBereich(name = "Südtribüne", lage = StadionLage.SUED, sitzplaetze = 10_000),
                StadionBereich(name = "Osttribüne", lage = StadionLage.OST, sitzplaetze = 8_000),
                StadionBereich(name = "Westtribüne", lage = StadionLage.WEST, sitzplaetze = 12_000),
            ),
        )
        val zonen = StadionPlanLogik.zonen(plan)
        val seiten = zonen.filter { it.lage != null }

        assertTrue("Echte Bereichsdaten werden genutzt", StadionPlanLogik.nutztEchteBereiche(plan))
        assertEquals("4 echte Tribünen", 4, seiten.size)
        assertEquals("Keine Kurven-Zonen bei echten Bereichen", 0, zonen.count { it.istKurve })
        assertEquals(4, zonen.size)
        seiten.forEach { zone ->
            assertTrue("Bereichs-Zone hat echte Plätze", zone.hatAufteilung)
            assertEquals("Sitz-Anteil 100 %", 1f, zone.sitzAnteil, 0.001f)
            assertEquals(0f, zone.stehAnteil, 0.001f)
            assertEquals(zone.kapazitaet, zone.sitzplaetze)
        }
        assertEquals(15_000, seiten.first { it.lage == StadionLage.NORD }.kapazitaet)
        assertEquals("Sektorbreite proportional zur Kapazität", 15_000f, seiten.first { it.lage == StadionLage.NORD }.gewicht)
    }

    @Test
    fun bereiche_mitFehlendenLagen_fallenAufAggregatZurueck() {
        val plan = StadionPlanDaten(
            stehplaetze = 2_000,
            sitzplaetze = 8_000,
            bereiche = listOf(
                StadionBereich(name = "Haupttribüne", sitzplaetze = 8_000),
            ),
        )
        assertFalse(StadionPlanLogik.nutztEchteBereiche(plan))
        val zonen = StadionPlanLogik.zonen(plan)

        assertEquals("10k: 2 Tribünen, noch keine Kurven", 2, zonen.size)
        assertEquals(0, zonen.count { it.istKurve })
        assertTrue(zonen.all { it.hatAufteilung })
        assertEquals(8_000, zonen.sumOf { it.sitzplaetze!! })
        assertEquals(2_000, zonen.sumOf { it.stehplaetze!! })
    }

    @Test
    fun kategorien_nurVorhandeneKategorien() {
        val reinSitz = StadionPlanDaten(
            stehplaetze = 0,
            sitzplaetze = 90_500,
            sitzUeberdacht = 90_500,
        )
        val kategorien = StadionPlanLogik.kategorien(reinSitz, Detaillierungsgrad.SEHR_GROSS)

        assertEquals(
            listOf(
                "Sitzplätze",
                "Haupttribüne",
                "Gegentribüne",
                "Südkurve",
                "Nordkurve",
                "Gäste",
                "VIP- und Business-Bereich",
                "Barrierefreie Plätze",
            ),
            kategorien.map { it.anzeigeName },
        )
        val sitze = kategorien.first()
        assertEquals(90_500, sitze.plaetze)
        assertEquals(90_500, sitze.davonUeberdacht)
        assertEquals(0, sitze.nichtUeberdacht)
        assertFalse("Keine erfundenen Stehplätze", kategorien.any { it.typ == PlatzTyp.STEH })
    }

    @Test
    fun kategorien_mischerIstDynamischNachGröße() {
        val klein = StadionPlanDaten(stehplaetze = 2_000, sitzplaetze = 8_000)
        assertEquals(
            listOf("Stehplätze", "Sitzplätze", "Haupttribüne", "Gegentribüne"),
            StadionPlanLogik.kategorien(klein).map { it.anzeigeName },
        )

        val gross = StadionPlanDaten(stehplaetze = 9_000, sitzplaetze = 36_000)
        assertEquals(
            listOf(
                "Stehplätze",
                "Sitzplätze",
                "Haupttribüne",
                "Gegentribüne",
                "Südkurve",
                "Nordkurve",
                "Gäste",
                "VIP- und Business-Bereich",
                "Barrierefreie Plätze",
            ),
            StadionPlanLogik.kategorien(gross).map { it.anzeigeName },
        )
    }

    @Test
    fun kategorien_keineSonderbereicheUnterhalbDerSchwellen() {
        val klein = StadionPlanDaten(stehplaetze = 3_000, sitzplaetze = 7_000)
        val namenKlein = StadionPlanLogik.kategorien(klein).map { it.anzeigeName }
        assertTrue("Kein Gästeblock unter 12.000", "Gäste" !in namenKlein)
        assertTrue("Kein VIP unter 35.000", "VIP- und Business-Bereich" !in namenKlein)
        assertTrue("Kein Barrierefrei unter 15.000", "Barrierefreie Plätze" !in namenKlein)

        val gross = StadionPlanDaten(stehplaetze = 3_000, sitzplaetze = 27_000)
        val namenGross = StadionPlanLogik.kategorien(gross).map { it.anzeigeName }
        assertTrue("Gästeblock ab 12.000", "Gäste" in namenGross)
        assertTrue("Barrierefrei ab 15.000", "Barrierefreie Plätze" in namenGross)
        assertTrue("Noch kein VIP unter 35.000", "VIP- und Business-Bereich" !in namenGross)
    }

    @Test
    fun kategorien_echteBereiche_nurStadionweiteKategorien() {
        val plan = StadionPlanDaten(
            stehplaetze = 3_000,
            sitzplaetze = 12_000,
            bereiche = listOf(
                StadionBereich(name = "Nord", lage = StadionLage.NORD, sitzplaetze = 12_000),
            ),
        )
        assertTrue(StadionPlanLogik.nutztEchteBereiche(plan))
        assertEquals(
            listOf("Stehplätze", "Sitzplätze"),
            StadionPlanLogik.kategorien(plan).map { it.anzeigeName },
        )
    }

    @Test
    fun kategorien_leereListe_ohnePlaetze() {
        assertTrue(StadionPlanLogik.kategorien(StadionPlanDaten(stehplaetze = 0, sitzplaetze = 0)).isEmpty())
    }

    @Test
    fun kategorien_nichtUeberdachteNurBeiGemischterUeberdachung() {
        // Voller Überdachungsgrad → kein Eintrag für nicht überdachte Plätze.
        val voll = StadionPlanDaten(
            stehplaetze = 4_000,
            sitzplaetze = 20_000,
            stehUeberdacht = 4_000,
            sitzUeberdacht = 20_000,
        )
        assertTrue(
            "Voll überdacht: kein Eintrag",
            StadionPlanLogik.kategorien(voll).none { it.nurUnueberdacht },
        )

        // Keinerlei Überdachung → ebenfalls kein Eintrag (nichts hervorzuheben).
        val offen = StadionPlanDaten(stehplaetze = 4_000, sitzplaetze = 20_000)
        assertTrue(
            "Völlig offen: kein Eintrag zum Ausgrauen eines Dachs",
            StadionPlanLogik.kategorien(offen).none { it.nurUnueberdacht },
        )

        // Gemischte Überdachung → Kategorie erscheint mit korrekter Platzanzahl.
        val gemischt = StadionPlanDaten(
            stehplaetze = 4_000,
            sitzplaetze = 20_000,
            stehUeberdacht = 0,
            sitzUeberdacht = 14_000,
        )
        val nichtUeberdacht = StadionPlanLogik.kategorien(gemischt).first { it.nurUnueberdacht }
        assertEquals("Nicht überdachte Plätze", nichtUeberdacht.anzeigeName)
        assertEquals(10_000, nichtUeberdacht.plaetze)
        assertEquals(0, nichtUeberdacht.davonUeberdacht)
        assertEquals("Überdachte Flächen werden ausgegraut", true, "ü" in nichtUeberdacht.beschreibung)
    }

    @Test
    fun sektorName_beschreibtNurDieLage() {
        assertEquals("Nord", StadionPlanLogik.sektorName(StadionLage.NORD, 0, 1))
        assertEquals("Nord· West", StadionPlanLogik.sektorName(StadionLage.NORD, 0, 2))
        assertEquals("Nord· Ost", StadionPlanLogik.sektorName(StadionLage.NORD, 1, 2))
        assertEquals("West· Süd", StadionPlanLogik.sektorName(StadionLage.WEST, 1, 2))
        assertEquals("Süd· Mitte", StadionPlanLogik.sektorName(StadionLage.SUED, 1, 3))
    }

    @Test
    fun kapazitaetsFaktor_istMonotonUndBeschraenkt() {
        assertEquals(0f, StadionPlanLogik.kapazitaetsFaktor(0), 0.001f)
        val klein = StadionPlanLogik.kapazitaetsFaktor(5_000)
        val gross = StadionPlanLogik.kapazitaetsFaktor(90_000)
        assertTrue(klein in 0f..1f)
        assertTrue(gross > klein)
        assertTrue(gross < 1f)
    }

    @Test
    fun bandTiefen_reineSitzOhneSteh() {
        val (sitz, steh) = StadionPlanLogik.bandTiefen(1f, 0f, 100f)
        assertEquals(100f, sitz, 0.001f)
        assertEquals(0f, steh, 0.001f)
    }

    @Test
    fun bandTiefen_reineStehOhneSitz() {
        val (sitz, steh) = StadionPlanLogik.bandTiefen(0f, 1f, 100f)
        assertEquals(0f, sitz, 0.001f)
        assertEquals(100f, steh, 0.001f)
    }

    @Test
    fun bandTiefen_gemischtSummiertAufRingtiefe() {
        val ringTiefe = 48f
        val (sitz, steh) = StadionPlanLogik.bandTiefen(0.7f, 0.3f, ringTiefe)
        assertEquals(ringTiefe, sitz + steh, 0.001f)
        assertTrue("Sitz dominant", sitz > steh)
    }

    @Test
    fun bandTiefen_winzigerAnteilBleibtSichtbar() {
        val (sitz, steh) = StadionPlanLogik.bandTiefen(0.99f, 0.01f, 100f)
        assertTrue("Stehband behält Mindestbreite", sitz < 100f && steh > 0f)
        assertEquals(100f, sitz + steh, 0.001f)
    }

    @Test
    fun rasenMuster_kleinStadionImmerOhneMuster() {
        for (seed in 0 until 64) {
            assertEquals(
                "Kleines Stadion hat nie ein Muster",
                RasenMuster.KEINS,
                StadionPlanLogik.rasenMuster(5_000, seed),
            )
        }
    }

    @Test
    fun rasenMuster_mittelNurOhneOderBreiteStreifen() {
        val erlaubt = setOf(RasenMuster.KEINS, RasenMuster.STREIFEN_BREIT)
        for (seed in 0 until 64) {
            assertTrue(
                "Mittel-Stadion kennt nur einfache Muster",
                StadionPlanLogik.rasenMuster(20_000, seed) in erlaubt,
            )
        }
        // Beide Varianten sind erreichbar (Seed 0 und 1).
        assertEquals(RasenMuster.KEINS, StadionPlanLogik.rasenMuster(20_000, 0))
        assertEquals(RasenMuster.STREIFEN_BREIT, StadionPlanLogik.rasenMuster(20_000, 1))
    }

    @Test
    fun rasenMuster_grossOhneKreise() {
        val erlaubt = setOf(
            RasenMuster.STREIFEN_BREIT,
            RasenMuster.STREIFEN_SCHMAL,
            RasenMuster.KARRIERT,
        )
        for (seed in 0 until 64) {
            assertTrue(
                "Groß-Stadion kennt noch keine Kreise",
                StadionPlanLogik.rasenMuster(45_000, seed) in erlaubt,
            )
        }
    }

    @Test
    fun rasenMuster_sehrGrossKenntAlleFuenfVarianten() {
        val erreicht = (0 until 256).map { StadionPlanLogik.rasenMuster(90_000, it) }.toSet()
        assertEquals(
            "Mega-Stadion kann alle fünf Muster zeigen",
            RasenMuster.values().toSet(),
            erreicht,
        )
    }

    @Test
    fun variante_rasenIstDeterministischUndStufenkonform() {
        val plan = StadionPlanDaten(stehplaetze = 10_000, sitzplaetze = 30_000)
        assertEquals(
            "Gleiche Werte → gleiches Rasenmuster",
            StadionPlanLogik.variante(plan).rasen,
            StadionPlanLogik.variante(plan).rasen,
        )
        val klein = StadionPlanDaten(stehplaetze = 3_000, sitzplaetze = 7_000)
        assertEquals(
            "Kleines Stadion bekommt KEIN Muster zugewiesen",
            RasenMuster.KEINS,
            StadionPlanLogik.variante(klein).rasen,
        )
        // Ohne Kapazität: Standard-Variante, kein Muster.
        assertEquals(
            RasenMuster.KEINS,
            StadionPlanLogik.variante(StadionPlanDaten(stehplaetze = 0, sitzplaetze = 0)).rasen,
        )
    }

    @Test
    fun premiumAusstattung_nurMitBessererAnzeigetafelUndRasenheizung() {
        // Einfache Anzeigetafel + Rasenheizung reicht NICHT.
        assertFalse(
            StadionPlanDaten(stehplaetze = 0, sitzplaetze = 1_000, anzeigetafel = "Anzeigetafel", rasenheizung = true)
                .hatPremiumAusstattung,
        )
        // Videowürfel ohne Rasenheizung genügt nicht.
        assertFalse(
            StadionPlanDaten(stehplaetze = 0, sitzplaetze = 1_000, anzeigetafel = "Videowürfel").hatPremiumAusstattung,
        )
        // Multimediawürfel + Rasenheizung → Premium.
        assertTrue(
            StadionPlanDaten(
                stehplaetze = 0,
                sitzplaetze = 1_000,
                anzeigetafel = "Multimediawürfel",
                rasenheizung = true,
            ).hatPremiumAusstattung,
        )
        assertTrue(
            StadionPlanDaten(
                stehplaetze = 0,
                sitzplaetze = 1_000,
                anzeigetafel = "Megawürfel",
                rasenheizung = true,
            ).hatPremiumAusstattung,
        )
    }

    @Test
    fun rasenMuster_premiumKleinstadionImmerMuster() {
        for (seed in 0 until 64) {
            val muster = StadionPlanLogik.rasenMuster(5_000, seed, premiumAusstattung = true)
            assertTrue(
                "Kleines Stadion mit Premium-Ausstattung hat ein Muster (Seed $seed)",
                muster != RasenMuster.KEINS,
            )
        }
    }

    @Test
    fun rasenMuster_premiumWechseltMitSaison() {
        val plan = StadionPlanDaten(
            stehplaetze = 1_000,
            sitzplaetze = 4_000,
            anzeigetafel = "Multimediawürfel",
            rasenheizung = true,
        )
        val saison1 = StadionPlanLogik.variante(plan, saison = 3).rasen
        val saison2 = StadionPlanLogik.variante(plan, saison = 4).rasen
        assertNotEquals("Neue Saison → anderes Muster", saison1, saison2)
        // Gleiche Saison bleibt deterministisch.
        assertEquals(
            StadionPlanLogik.variante(plan, saison = 4).rasen,
            StadionPlanLogik.variante(plan, saison = 4).rasen,
        )
    }

    @Test
    fun rasenMuster_ohnePremiumSaisonSpinntNicht() {
        // Ein kleines Stadion ohne Premium bleibt ohne Muster – egal welche Saison.
        assertEquals(
            RasenMuster.KEINS,
            StadionPlanLogik.variante(StadionPlanDaten(stehplaetze = 3_000, sitzplaetze = 7_000), saison = 12).rasen,
        )
    }
}