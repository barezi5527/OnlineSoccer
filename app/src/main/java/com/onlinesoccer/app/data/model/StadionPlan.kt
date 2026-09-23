package com.onlinesoccer.app.data.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Himmelsrichtung einer Tribüne im Stadionplan (Draufsicht: Nord = oben). */
enum class StadionLage {
    NORD,
    SUED,
    OST,
    WEST,
}

/**
 * Ausbaustufe eines Stadions. Bestimmt, wie viele Zuschauerbereiche realistisch
 * belegt sind: Erst die Langseiten, dann die Kurven (Stehplätze), ab sehr großer
 * Kapazität mit ausgeprägten Kurven und Sonderbereichen.
 */
enum class Detaillierungsgrad(val anzeigeName: String) {
    KLEIN("Einfach"),
    MITTEL("Detailliert"),
    GROSS("Groß"),
    SEHR_GROSS("Sehr groß"),
}

/** Grundlegende Platzart eines Bandes im Plan. */
enum class PlatzTyp(val bezeichnung: String) {
    SITZ("Sitzplätze"),
    STEH("Stehplätze"),
}

/**
 * Eine auswählbare Platzkategorie. Neben den stadionweiten Kategorien „Stehplätze"
 * und „Sitzplätze" werden – wie bei Fußballklubs der Bundesliga üblich – die
 * einzelnen Tribünen angeboten (Haupt-/Gegentribüne, Kurven, Gästeblock, VIP- und
 * Business-Bereich, barrierefreie Plätze). [lage]/[zoneName] ordnen eine physische
 * Kategorie ihren Bereichen im Plan zu; bei `null` wirkt die Kategorie stadionweit
 * über [typ]. [name] ist bei physischen Kategorien gesetzt und überschreibt die
 * automatische Bezeichnung.
 */
data class PlatzKategorie(
    val typ: PlatzTyp?,
    val ueberdacht: Boolean,
    val plaetze: Int,
    val davonUeberdacht: Int,
    val name: String? = null,
    val lage: StadionLage? = null,
    val zoneName: String? = null,
) {
    /** Plätze dieser Kategorie ohne Überdachung. */
    val nichtUeberdacht: Int get() = (plaetze - davonUeberdacht).coerceAtLeast(0)

    val anzeigeName: String get() = name ?: when {
        typ == null -> "Überdacht"
        ueberdacht -> "Überdachte ${typ.bezeichnung}"
        else -> typ.bezeichnung
    }

    val beschreibung: String get() = when {
        name != null -> "$anzeigeName im Stadionring – anteilig nach typischen Bundesliga-Stadien berechnet."
        typ == null -> "Überdachte Plätze für Zuschauer."
        ueberdacht -> "Überdachte ${typ.bezeichnung} für Zuschauer."
        else -> "${typ.bezeichnung} für Zuschauer."
    }
}

/**
 * Eine echte Tribüne/Bereich des Stadions. Die Stadionausbau-Seite liefert aktuell
 * nur Gesamtwerte (Sitz-/Stehplätze je Stadion); sobald sie eine Aufteilung nach
 * Tribünen liefert (z. B. „Nordtribüne", „Osttribüne"), wird diese hier über
 * [StadionPlanDaten.bereiche] in den Plan übernommen. [lage] kann `null` sein,
 * wenn noch keine eindeutige Zuordnung vorhanden ist – dann fällt die Darstellung
 * auf die Bundesliga-typische Gesamtansicht zurück.
 */
data class StadionBereich(
    val name: String,
    val lage: StadionLage? = null,
    val sitzplaetze: Int = 0,
    val stehplaetze: Int = 0,
    val sitzUeberdacht: Int = 0,
    val stehUeberdacht: Int = 0,
) {
    val kapazitaet: Int get() = sitzplaetze + stehplaetze
}

/**
 * Kennzahlen des eigenen Stadions, aus denen die App im Bereich „Stadionausbau"
 * einen bildhaften Stadionplan zeichnet. Die Werte kommen ausschließlich aus der
 * Stadionausbau-Seite (Fassungsvermögen, Sitz-/Stehplätze, überdachte Anteile).
 */
data class StadionPlanDaten(
    val stehplaetze: Int,
    val sitzplaetze: Int,
    val stehUeberdacht: Int = 0,
    val sitzUeberdacht: Int = 0,
    /** Vom Server gemeldetes Fassungsvermögen (kann vom Summenwert abweichen). */
    val fassungsvermoegen: Int? = null,
    /**
     * Echte Tribünen-/Bereichsdaten, falls die Datenquelle sie liefert.
     * Leer bedeutet: Es ist nur die Gesamtaufteilung bekannt.
     */
    val bereiche: List<StadionBereich> = emptyList(),
) {
    val kapazitaet: Int get() = stehplaetze + sitzplaetze
}

/**
 * Eine im Plan dargestellte Zone (eine Stadionseite, eine Tribüne oder eine Kurve).
 * In der aggregierten (geschätzten) Ansicht trägt [sitzplaetze] die anteiligen
 * Plätze der Tribüne. Kurven-Zonen ([istKurve]) umfassen zusätzlich ihre
 * Eckbereiche des abgerundeten Rings.
 */
data class StadionPlanZone(
    /** Seite der Zone (Nord/Süd/Ost/West). */
    val lage: StadionLage,
    val name: String,
    /**
     * `true`, wenn die Zone eine Kurzseiten-Kurve ist (Südkurve/Nordkurve). Ihre
     * Kurvenbereiche (Ecken) des abgerundeten Rings werden zusammen mit dem
     * Seitenband gezeichnet.
     */
    val istKurve: Boolean = false,
    /** Bandbreiten-Anteil Sitzplätze (0..1), relative Angabe für die Zeichnung. */
    val sitzAnteil: Float,
    /** Bandbreiten-Anteil Stehplätze (0..1). */
    val stehAnteil: Float,
    /** Überdachter Anteil des Sitzbandes (0..1). */
    val sitzUeberdachtAnteil: Float = 0f,
    /** Überdachter Anteil des Stehbandes (0..1). */
    val stehUeberdachtAnteil: Float = 0f,
    /** Sitzplätze der Zone (immer gesetzt; `null` nur bei künftigen Daten ohne Aufteilung). */
    val sitzplaetze: Int? = null,
    /** Stehplätze der Zone – `null` nur bei künftigen Daten ohne Aufteilung. */
    val stehplaetze: Int? = null,
    val sitzUeberdacht: Int? = null,
    val stehUeberdacht: Int? = null,
    /** Gewicht der Zone (z. B. Kapazität) – bestimmt die Blockbreite entlang der Seite. */
    val gewicht: Float = 1f,
    /**
     * `true`, wenn die Zone kein eigenes Seitenband belegt, sondern als dünner
     * Streifen auf den äußersten (obersten) zwei Reihen der Haupttribüne
     * derselben Seite liegt – der VIP-Bereich.
     */
    val vipStreifen: Boolean = false,
) {
    val kapazitaet: Int?
        get() = if (sitzplaetze == null && stehplaetze == null) {
            null
        } else {
            (sitzplaetze ?: 0) + (stehplaetze ?: 0)
        }

    /** `true`, wenn echte/geschätzte Plätze je Bereich vorliegen. */
    val hatAufteilung: Boolean get() = sitzplaetze != null
}

/** Platzangabe (gesamt + überdacht) für eine Tribüne oder einen Sonderbereich. */
data class Platzangabe(
    val plaetze: Int,
    val ueberdacht: Int,
)

/** Eine Tribüne entlang einer Stadionseite (Langseite oder Kurzseite/Kurve). */
data class Zonentribuene(
    val lage: StadionLage,
    val sitz: Platzangabe,
    val steh: Platzangabe,
) {
    val kapazitaet: Int get() = sitz.plaetze + steh.plaetze
}

/**
 * Geschätzte Tribünen-Aufteilung des Stadions nach dem Vorbild typischer
 * Bundesliga-Stadien (u. a. FC Bayern, Borussia Dortmund, VfB Stuttgart).
 * Die Summe aller Tribünen ergibt exakt die Gesamtkapazität (Sitz + Steh).
 * Sonderbereiche erscheinen nur, wenn die Stadiongröße dafür typisch ist:
 * der VIP-Bereich als oberste zwei Reihen der Haupttribüne (kein eigener
 * Sektor), die barrierefreien Plätze als eigener Bereich auf der Westseite.
 */
data class TribuenenModell(
    val haupttribuene: Zonentribuene,
    val gegentribuene: Zonentribuene,
    val suedkurve: Zonentribuene,
    val nordkurve: Zonentribuene,
    val gaesteblock: Zonentribuene? = null,
    val vip: Platzangabe = Platzangabe(0, 0),
    val barrierefrei: Platzangabe = Platzangabe(0, 0),
) {
    val hatGaesteblock: Boolean get() = gaesteblock != null && gaesteblock.kapazitaet > 0
    val hatVip: Boolean get() = vip.plaetze > 0
    val hatBarrierefrei: Boolean get() = barrierefrei.plaetze > 0
}

/**
 * Reine Planlogik: leitet aus den [StadionPlanDaten] die darzustellenden Zonen,
 * den Detaillierungsgrad und die auswählbaren Kategorien ab. Bewusst frei von
 * UI-/Zeichencode.
 */
object StadionPlanLogik {

    // Größen-Schwellen nach dem Vorbild größerer Bundesliga-Stadien.
    private const val GAESTEBLOCK_AB = 12_000
    private const val BARRIEREFREI_AB = 15_000
    private const val VIP_AB = 35_000

    /** Determiniert den Detaillierungsgrad aus der Gesamtkapazität. */
    fun detaillierungsGrad(kapazitaet: Int): Detaillierungsgrad = when {
        kapazitaet >= 60_000 -> Detaillierungsgrad.SEHR_GROSS
        kapazitaet >= 30_000 -> Detaillierungsgrad.GROSS
        kapazitaet >= 12_000 -> Detaillierungsgrad.MITTEL
        else -> Detaillierungsgrad.KLEIN
    }

    /**
     * Die auswählbaren Platzkategorien – dynamisch: Nur Kategorien, die das
     * Stadion (realistischerweise) hat, werden aufgeführt. Neben den stadionweiten
     * Steh-/Sitzplätzen sind das die Tribünen des Rings: Haupt-/Gegentribüne
     * (Langseiten), Südkurve/Nordkurve (Kurzseiten inkl. Eckbereichen), der
     * Gästeblock in der Südkurve, der VIP- und Business-Bereich (ab 35.000
     * Plätzen) sowie barrierefreie Plätze.
     */
    fun kategorien(
        plan: StadionPlanDaten,
        grad: Detaillierungsgrad = detaillierungsGrad(plan.kapazitaet),
    ): List<PlatzKategorie> = buildList {
        if (plan.kapazitaet <= 0) return@buildList
        val modell = tribuenenModell(plan, grad)

        if (plan.stehplaetze > 0) {
            add(PlatzKategorie(PlatzTyp.STEH, ueberdacht = false, plan.stehplaetze, plan.stehUeberdacht))
        }
        if (plan.sitzplaetze > 0) {
            add(PlatzKategorie(PlatzTyp.SITZ, ueberdacht = false, plan.sitzplaetze, plan.sitzUeberdacht))
        }

        if (nutztEchteBereiche(plan)) return@buildList

        if (modell.haupttribuene.kapazitaet > 0) {
            add(kategorie("Haupttribüne", PlatzTyp.SITZ, modell.haupttribuene, StadionLage.WEST))
        }
        if (modell.gegentribuene.kapazitaet > 0) {
            add(kategorie("Gegentribüne", hauptTyp(modell.gegentribuene), modell.gegentribuene, StadionLage.OST))
        }
        if (modell.suedkurve.kapazitaet > 0) {
            add(kategorie("Südkurve", hauptTyp(modell.suedkurve), modell.suedkurve, StadionLage.SUED))
        }
        if (modell.nordkurve.kapazitaet > 0) {
            add(kategorie("Nordkurve", hauptTyp(modell.nordkurve), modell.nordkurve, StadionLage.NORD))
        }
        if (modell.hatGaesteblock) {
            add(kategorie("Gästeblock", hauptTyp(modell.gaesteblock!!), modell.gaesteblock, StadionLage.SUED))
        }
        if (modell.hatVip) {
            add(
                PlatzKategorie(
                    typ = PlatzTyp.SITZ,
                    ueberdacht = false,
                    plaetze = modell.vip.plaetze,
                    davonUeberdacht = modell.vip.ueberdacht,
                    name = "VIP- und Business-Bereich",
                    lage = StadionLage.WEST,
                    zoneName = "VIP- und Business-Bereich",
                ),
            )
        }
        if (modell.hatBarrierefrei) {
            add(
                PlatzKategorie(
                    typ = PlatzTyp.SITZ,
                    ueberdacht = false,
                    plaetze = modell.barrierefrei.plaetze,
                    davonUeberdacht = modell.barrierefrei.ueberdacht,
                    name = "Barrierefreie Plätze",
                    lage = StadionLage.WEST,
                    zoneName = "Barrierefreie Plätze",
                ),
            )
        }
    }

    private fun kategorie(
        name: String,
        typ: PlatzTyp,
        tribuene: Zonentribuene,
        lage: StadionLage,
    ): PlatzKategorie =
        PlatzKategorie(
            typ = typ,
            ueberdacht = false,
            plaetze = tribuene.kapazitaet,
            davonUeberdacht = tribuene.sitz.ueberdacht + tribuene.steh.ueberdacht,
            name = name,
            lage = lage,
            zoneName = name,
        )

    private fun hauptTyp(tribuene: Zonentribuene): PlatzTyp =
        if (tribuene.steh.plaetze >= tribuene.sitz.plaetze) PlatzTyp.STEH else PlatzTyp.SITZ

    /**
     * Leitet die Zonen des Plans ab:
     * - Echte Bereichsdaten → je Bereich eine Zone (reale Plätze, Gewicht = Kapazität).
     * - Sonst → geschätzte Bundesliga-Tribünen samt optionalem Gästeblock und
     *   Sonderbereichen; die Kurzseiten-Kurven (Südkurve/Nordkurve) umfassen
     *   ihre Eckbereiche direkt mit.
     */
    fun zonen(plan: StadionPlanDaten): List<StadionPlanZone> {
        val echte = echteBereiche(plan)
        val nutzbar = echte.isNotEmpty() && echte.size == plan.bereiche.size
        return if (nutzbar) {
            echte.map { zoneVonBereich(it) }
        } else {
            aggregatZonen(plan)
        }
    }

    private fun aggregatZonen(plan: StadionPlanDaten): List<StadionPlanZone> {
        val grad = detaillierungsGrad(plan.kapazitaet)
        val modell = tribuenenModell(plan, grad)
        val zonen = mutableListOf<StadionPlanZone>()
        if (modell.haupttribuene.kapazitaet > 0) {
            zonen += zoneVonTribuene("Haupttribüne", modell.haupttribuene)
        }
        if (modell.gegentribuene.kapazitaet > 0) {
            zonen += zoneVonTribuene("Gegentribüne", modell.gegentribuene)
        }
        if (modell.suedkurve.kapazitaet > 0) {
            zonen += zoneVonTribuene("Südkurve", modell.suedkurve, istKurve = true)
        }
        if (modell.nordkurve.kapazitaet > 0) {
            zonen += zoneVonTribuene("Nordkurve", modell.nordkurve, istKurve = true)
        }
        modell.gaesteblock?.let { if (it.kapazitaet > 0) zonen += zoneVonTribuene("Gästeblock", it) }

        // Sonderbereiche: Der VIP-Bereich liegt als oberste zwei Reihen auf der
        // Haupttribüne (kein eigener Sektor mehr); der barrierefreie Bereich
        // übernimmt die zuvor vom VIP-Sektor belegte Fläche als eigenen, nun
        // größeren Sektor auf der Westseite – mit Mindestbreite für die Sichtbarkeit.
        val westKap = modell.haupttribuene.kapazitaet + modell.barrierefrei.plaetze
        if (modell.hatVip) {
            zonen += vipStreifenZone(modell.vip)
        }
        if (modell.hatBarrierefrei) {
            zonen += sonderBereichZone(
                name = "Barrierefreie Plätze",
                angebote = modell.barrierefrei,
                gewicht = max(modell.barrierefrei.plaetze.toFloat(), westKap * 0.04f),
            )
        }
        return zonen
    }

    private fun zoneVonTribuene(
        name: String,
        tribuene: Zonentribuene,
        istKurve: Boolean = false,
    ): StadionPlanZone {
        val kap = tribuene.kapazitaet
        val sitzAnt = if (kap > 0) tribuene.sitz.plaetze.toFloat() / kap else 0f
        val stehAnt = if (kap > 0) tribuene.steh.plaetze.toFloat() / kap else 0f
        return StadionPlanZone(
            lage = tribuene.lage,
            name = name,
            istKurve = istKurve,
            sitzAnteil = sitzAnt,
            stehAnteil = stehAnt,
            sitzUeberdachtAnteil = anteil(tribuene.sitz.ueberdacht, tribuene.sitz.plaetze),
            stehUeberdachtAnteil = anteil(tribuene.steh.ueberdacht, tribuene.steh.plaetze),
            sitzplaetze = tribuene.sitz.plaetze,
            stehplaetze = tribuene.steh.plaetze,
            sitzUeberdacht = tribuene.sitz.ueberdacht,
            stehUeberdacht = tribuene.steh.ueberdacht,
            gewicht = kap.toFloat(),
        )
    }

    /** Eine Zone für einen eingebetteten Sonderbereich (barrierefrei). */
    private fun sonderBereichZone(
        name: String,
        angebote: Platzangabe,
        gewicht: Float,
    ): StadionPlanZone =
        StadionPlanZone(
            lage = StadionLage.WEST,
            name = name,
            sitzAnteil = 1f,
            stehAnteil = 0f,
            sitzUeberdachtAnteil = anteil(angebote.ueberdacht, angebote.plaetze),
            stehUeberdachtAnteil = 0f,
            sitzplaetze = angebote.plaetze,
            stehplaetze = 0,
            sitzUeberdacht = angebote.ueberdacht,
            stehUeberdacht = 0,
            gewicht = gewicht,
        )

    /**
     * Die Zone des VIP-Bereichs: kein eigener Seitenabschnitt, sondern ein
     * dünner Streifen auf den obersten zwei Reihen der Haupttribüne.
     */
    private fun vipStreifenZone(angebote: Platzangabe): StadionPlanZone =
        StadionPlanZone(
            lage = StadionLage.WEST,
            name = "VIP- und Business-Bereich",
            sitzAnteil = 1f,
            stehAnteil = 0f,
            sitzUeberdachtAnteil = anteil(angebote.ueberdacht, angebote.plaetze),
            stehUeberdachtAnteil = 0f,
            sitzplaetze = angebote.plaetze,
            stehplaetze = 0,
            sitzUeberdacht = angebote.ueberdacht,
            stehUeberdacht = 0,
            gewicht = 0f,
            vipStreifen = true,
        )

    private fun zoneVonBereich(bereich: StadionBereich): StadionPlanZone {
        val gesamt = bereich.kapazitaet.toFloat()
        // Nur echte Bereiche mit Lage werden hier übernommen (siehe echteBereiche).
        val lage = requireNotNull(bereich.lage)
        return StadionPlanZone(
            lage = lage,
            name = bereich.name,
            sitzAnteil = if (gesamt > 0f) bereich.sitzplaetze / gesamt else 0f,
            stehAnteil = if (gesamt > 0f) bereich.stehplaetze / gesamt else 0f,
            sitzUeberdachtAnteil = anteil(bereich.sitzUeberdacht, bereich.sitzplaetze),
            stehUeberdachtAnteil = anteil(bereich.stehUeberdacht, bereich.stehplaetze),
            sitzplaetze = bereich.sitzplaetze,
            stehplaetze = bereich.stehplaetze,
            sitzUeberdacht = bereich.sitzUeberdacht,
            stehUeberdacht = bereich.stehUeberdacht,
            gewicht = gesamt,
        )
    }

    /** `true`, wenn echte Bereichsdaten (mit Lage) für das ganze Stadion vorliegen. */
    fun nutztEchteBereiche(plan: StadionPlanDaten): Boolean {
        val bereiche = plan.bereiche
        return bereiche.isNotEmpty() && echteBereiche(plan).size == bereiche.size
    }

    private fun echteBereiche(plan: StadionPlanDaten): List<StadionBereich> =
        plan.bereiche.filter { it.lage != null && it.kapazitaet > 0 }

    /**
     * Geschätzte Tribünen-Aufteilung nach dem Vorbild typischer Bundesliga-Stadien.
     * Langseiten (Haupt-/Gegentribüne) überwiegend Sitzplätze, Kurzseiten
     * (Süd-/Nordkurve) überwiegend Stehplätze; der Gästeblock sitzt als eigener
     * Bereich in der Südkurve. Sonderbereiche (VIP, barrierefrei) erscheinen nur
     * ab der jeweiligen Größe. Die Summen entsprechen exakt den Stadiondaten.
     */
    fun tribuenenModell(
        plan: StadionPlanDaten,
        grad: Detaillierungsgrad = detaillierungsGrad(plan.kapazitaet),
    ): TribuenenModell {
        val sitz = plan.sitzplaetze.coerceAtLeast(0)
        val steh = plan.stehplaetze.coerceAtLeast(0)
        val kap = sitz + steh

        val leer = { lage: StadionLage -> Zonentribuene(lage, Platzangabe(0, 0), Platzangabe(0, 0)) }
        if (kap <= 0) {
            return TribuenenModell(
                haupttribuene = leer(StadionLage.WEST),
                gegentribuene = leer(StadionLage.OST),
                suedkurve = leer(StadionLage.SUED),
                nordkurve = leer(StadionLage.NORD),
            )
        }

        // Sonderbereiche nur ab typischer Größe – vorher nicht vorhanden.
        // VIP = oberste zwei Reihen der Haupttribüne (≈ 2 % der Sitzplätze).
        val vipZiel = if (kap >= VIP_AB) (sitz * 0.02f).roundToInt() else 0
        // Barrierefrei ist größer als früher: Es übernimmt die Fläche/Anzahl,
        // die vormals der separate VIP-Sektor belegt hat.
        val barriereZiel = if (kap >= BARRIEREFREI_AB) {
            ((kap * 0.005f).roundToInt() + vipZiel).coerceIn(24, 1_500)
        } else {
            0
        }
        val kappung = vipZiel + barriereZiel
        // Bei kappung > sitz werden VIP/Barrierefrei exakt und restlos auf die
        // vorhandenen Sitzplätze skaliert (größter Rest zuerst), damit die Summe
        // trotz Rundung nie überzieht (kein Sitz mehr als vorhanden).
        val sonder = if (kappung > sitz) {
            verteile(sitz, listOf(vipZiel.toFloat(), barriereZiel.toFloat()))
        } else {
            listOf(vipZiel, barriereZiel)
        }
        val vip = sonder[0]
        val barrierefrei = sonder[1]
        val sitzOhneSonder = (sitz - vip - barrierefrei).coerceAtLeast(0)

        // Gästeblock: bevorzugt aus Stehplätzen (Kurven), Rest aus Sitzplätzen.
        val gaestZiel = if (kap >= GAESTEBLOCK_AB) ((kap * 0.05f).roundToInt()).coerceIn(400, 6000) else 0
        val gaestSteh = min(gaestZiel, steh)
        val gaestSitz = min(gaestZiel - gaestSteh, sitzOhneSonder.coerceAtLeast(0))
        val stehOhneGaest = steh - gaestSteh
        val sitzVerteilt = sitzOhneSonder - gaestSitz

        // Bundesliga-typische Ausstattung: Langseiten eher Sitz, Kurzseiten eher Steh.
        val (wWest, wOst, wSued, wNord) = lagenGewichte(grad)
        val sitzPraef = listOf(0.90f, 0.80f, 0.30f, 0.25f)
        val stehPraef = listOf(0.10f, 0.20f, 0.70f, 0.75f)
        val seitenGewichte = listOf(wWest, wOst, wSued, wNord)
        val wSitz = normalisiere(seitenGewichte.mapIndexed { i, w -> w * sitzPraef[i] })
        val wSteh = normalisiere(seitenGewichte.mapIndexed { i, w -> w * stehPraef[i] })

        val sitzJeSeite = verteile(sitzVerteilt, wSitz) // West, Ost, Sued, Nord
        val stehJeSeite = verteile(stehOhneGaest, wSteh)

        // Überdachte Plätze je Position (getrennt nach Sitz/Steh), anteilig.
        val udGewichteSitz = listOf(
            (sitzJeSeite[0] + vip + barrierefrei).toFloat(),
            sitzJeSeite[1].toFloat(),
            sitzJeSeite[2].toFloat(),
            sitzJeSeite[3].toFloat(),
            gaestSitz.toFloat(),
        )
        val udSitz = verteile(plan.sitzUeberdacht.coerceAtLeast(0), udGewichteSitz)
        val udWest = verteile(
            udSitz.getOrElse(0) { 0 },
            listOf(vip.toFloat(), barrierefrei.toFloat(), sitzJeSeite[0].toFloat()),
        )
        val udSteh = verteile(
            plan.stehUeberdacht.coerceAtLeast(0),
            listOf(
                stehJeSeite[0].toFloat(),
                stehJeSeite[1].toFloat(),
                stehJeSeite[2].toFloat(),
                stehJeSeite[3].toFloat(),
                gaestSteh.toFloat(),
            ),
        )

        return TribuenenModell(
            haupttribuene = Zonentribuene(
                lage = StadionLage.WEST,
                sitz = Platzangabe(sitzJeSeite[0], udWest[2]),
                steh = Platzangabe(stehJeSeite[0], udSteh[0]),
            ),
            gegentribuene = Zonentribuene(
                StadionLage.OST,
                Platzangabe(sitzJeSeite[1], udSitz[1]),
                Platzangabe(stehJeSeite[1], udSteh[1]),
            ),
            suedkurve = Zonentribuene(
                StadionLage.SUED,
                Platzangabe(sitzJeSeite[2], udSitz[2]),
                Platzangabe(stehJeSeite[2], udSteh[2]),
            ),
            nordkurve = Zonentribuene(
                StadionLage.NORD,
                Platzangabe(sitzJeSeite[3], udSitz[3]),
                Platzangabe(stehJeSeite[3], udSteh[3]),
            ),
            gaesteblock = if (gaestSitz + gaestSteh > 0) {
                Zonentribuene(
                    StadionLage.SUED,
                    Platzangabe(gaestSitz, udSitz.getOrElse(4) { 0 }),
                    Platzangabe(gaestSteh, udSteh.getOrElse(4) { 0 }),
                )
            } else {
                null
            },
            vip = Platzangabe(vip, udWest[0]),
            barrierefrei = Platzangabe(barrierefrei, udWest[1]),
        )
    }

    /** Anteilige Gewichte der vier Stadionseiten je Ausbaustufe. */
    private fun lagenGewichte(grad: Detaillierungsgrad): List<Float> = when (grad) {
        Detaillierungsgrad.KLEIN -> listOf(0.34f, 0.30f, 0.19f, 0.17f)
        Detaillierungsgrad.MITTEL -> listOf(0.32f, 0.28f, 0.22f, 0.18f)
        Detaillierungsgrad.GROSS -> listOf(0.31f, 0.27f, 0.23f, 0.19f)
        Detaillierungsgrad.SEHR_GROSS -> listOf(0.30f, 0.26f, 0.24f, 0.20f)
    }

    private fun normalisiere(werte: List<Float>): List<Float> {
        val gesamt = werte.sum()
        return if (gesamt > 0f) werte.map { it / gesamt } else List(werte.size) { 1f }
    }

    /**
     * Verteilt [summe] anteilig auf die Gewichte [anteile] (größter Rest zuerst).
     * Die Summe der Ergebnisse entspricht exakt [summe].
     */
    private fun verteile(summe: Int, anteile: List<Float>): List<Int> {
        if (summe <= 0 || anteile.isEmpty()) return List(anteile.size) { 0 }
        val gesamt = anteile.sum()
        if (gesamt <= 0f) return List(anteile.size) { 0 }
        val roh = anteile.map { summe.toFloat() * it / gesamt }
        val ergebnis = roh.map { it.toInt() }.toMutableList()
        var rest = summe - ergebnis.sum()
        while (rest < 0) {
            val kandidat = ergebnis.indexOfFirst { it > 0 }
            if (kandidat < 0) break
            ergebnis[kandidat] -= 1
            rest += 1
        }
        val reihenfolge = roh.indices.sortedByDescending { roh[it] - ergebnis[it] }
        var i = 0
        while (rest > 0 && i < reihenfolge.size) {
            ergebnis[reihenfolge[i]] += 1
            rest -= 1
            i++
        }
        return ergebnis
    }

    /**
     * Neutraler Name einer Seite bzw. eines Sektors entlang einer Seite.
     */
    fun sektorName(lage: StadionLage, index: Int, anzahl: Int): String {
        val basis = lageName(lage)
        if (anzahl <= 1) return basis
        val vertikal = lage == StadionLage.WEST || lage == StadionLage.OST
        val positionen = when (anzahl) {
            2 -> if (vertikal) listOf("Nord", "Süd") else listOf("West", "Ost")
            3 -> if (vertikal) listOf("Nord", "Mitte", "Süd") else listOf("West", "Mitte", "Ost")
            else -> if (vertikal) {
                listOf("Nord", "Mitte-Nord", "Mitte-Süd", "Süd")
            } else {
                listOf("West", "Mitte-West", "Mitte-Ost", "Ost")
            }
        }
        return "$basis· ${positionen[index.coerceIn(0, positionen.lastIndex)]}"
    }

    fun lageName(lage: StadionLage): String = when (lage) {
        StadionLage.NORD -> "Nord"
        StadionLage.SUED -> "Süd"
        StadionLage.OST -> "Ost"
        StadionLage.WEST -> "West"
    }

    /**
     * Glatter, monotoner Größefaktor (0..1): kleine Stadien kompakt, große Stadien
     * mit tiefem Rang und ausgedehntem Bowl. Keine festen Größenstufen.
     */
    fun kapazitaetsFaktor(kapazitaet: Int): Float {
        val k = kapazitaet.coerceAtLeast(0).toFloat()
        return sqrt(k / (k + 25_000f))
    }

    /** Relativer überdachter Anteil (0..1), abgesichert gegen fehlende Werte. */
    fun anteil(ueberdacht: Int, gesamt: Int): Float =
        if (gesamt <= 0) 0f else ueberdacht.coerceIn(0, gesamt).toFloat() / gesamt

    /**
     * Verteilt die Ringtiefe auf Sitz- und Stehband proportional zu den Anteilen.
     * Bänder mit sehr kleinem Anteil bleiben sichtbar (Mindestbreite), die Summe
     * ergibt immer die volle Ringtiefe.
     */
    fun bandTiefen(sitzAnteil: Float, stehAnteil: Float, ringTiefe: Float): Pair<Float, Float> {
        val sitz = sitzAnteil.coerceIn(0f, 1f)
        val steh = stehAnteil.coerceIn(0f, 1f)
        val summe = sitz + steh
        val (ns, nt) = if (summe <= 0f) 1f to 0f else (sitz / summe) to (steh / summe)

        val minAnteil = 0.12f
        var sitzT = ringTiefe * ns
        var stehT = ringTiefe * nt
        if (ns > 0f) sitzT = max(sitzT, ringTiefe * minAnteil)
        if (nt > 0f) stehT = max(stehT, ringTiefe * minAnteil)

        val summeBands = sitzT + stehT
        if (summeBands > ringTiefe && summeBands > 0f) {
            val korrektur = ringTiefe / summeBands
            sitzT *= korrektur
            stehT *= korrektur
        }
        return sitzT to stehT
    }
}