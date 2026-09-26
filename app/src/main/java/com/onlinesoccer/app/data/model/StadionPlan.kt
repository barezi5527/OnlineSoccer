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

/**
 * Größenband des Stadions, an die realen Kapazitäten von OS angepasst
 * (deutsche Ligen 1–5: 22.000–106.350, globales Minimum ~15.000).
 * Bestimmt zusammen mit der [Grundform] den Bautyp (Archetyp) des Plans.
 */
enum class StadionBand(val anzeigeName: String, val von: Int, val bis: Int) {
    A("Kleines Stadion", 0, 30_000),
    B("Kompakt-Stadion", 30_000, 45_000),
    C("Mittel-Stadion", 45_000, 60_000),
    D("Groß-Stadion", 60_000, 75_000),
    E("Mega-Stadion", 75_000, Int.MAX_VALUE);

    /** `true`, wenn [kapazitaet] in dieses Band fällt (`[von, bis)`). */
    fun enthaelt(kapazitaet: Int): Boolean = kapazitaet >= von && kapazitaet < bis

    /** Anzahl der Ränge dieses Bands (A/B einrangig, C/D zweirangig, E dreirangig). */
    val rangzahl: Int
        get() = when (this) {
            A, B -> 1
            C, D -> 2
            E -> 3
        }

    companion object {
        fun fuer(kapazitaet: Int): StadionBand =
            StadionBand.values().firstOrNull { it.enthaelt(kapazitaet) } ?: A
    }
}

/**
 * Grundform (Bowl-Form) des Stadions. Zusammen mit dem [StadionBand] ergibt jede
 * Kombination einen eigenen Archetyp (5 × 4 = 20). Die Form ist deterministisch
 * aus den Stadionwerten abgeleitet und daher in beiden Screens identisch.
 *
 * [eckFaktor] skaliert den Eckenradius des abgerundeten Bowls – klein (Achteck),
 * neutral (Kasten) oder groß (Oval). Bei [offeneEcken] werden die Eckbereiche des
 * Rings nicht bestückt, die Ecken bleiben offen (Laufbahn-Optik).
 */
enum class Grundform(val anzeigeName: String, val eckFaktor: Float, val offeneEcken: Boolean = false) {
    KASTEN("Kasten", 1f),
    ACHTECK("Achteck", 0.55f),
    OVAL("Oval", 1.7f),
    OFFENE_ECKEN("Offene Ecken", 1f, offeneEcken = true),
}

/** Wie der VIP-/Business-Bereich im Plan platziert ist (nur bei Kapazität ≥ 35.000). */
enum class VipPlatzierung(val anzeigeName: String) {
    KEIN("Kein VIP-Bereich"),
    STREIFEN("VIP an der äußeren Kante"),
    MITTELRANG("VIP im Mittelrang"),
    VERSTREUT("VIP über den ganzen Ring verteilt"),
}

/**
 * Mäh-/Druckmuster des Rasens. Kleine Stadien bleiben bewusst schlicht, größere
 * (ab [Detaillierungsgrad.MITTEL]) erhalten zunehmend ausgefallenere Muster –
 * deterministisch über den [StadionPlanLogik.variante]-Seed, damit der Plan in
 * „Stadionausbau" und „Teaminformation" identisch aussieht.
 */
enum class RasenMuster(val anzeigeName: String) {
    KEINS("Ohne Muster"),
    STREIFEN_BREIT("Breite Streifen"),
    STREIFEN_SCHMAL("Schmale Streifen"),
    KARRIERT("Karriert"),
    KREISE("Kreise"),
}

/**
 * Die vier Ecken des abgerundeten Stadium-Bowls. Bestimmt, in welchem
 * Eck-Viertelbereich der Gästeblock großer Stadien sitzt. Die Wahl ist
 * deterministisch aus den Stadionwerten abgeleitet und dadurch in „Stadionausbau"
 * und „Teaminformation" identisch – und wechselt von Stadion zu Stadion.
 */
enum class Ecke(val anzeigeName: String) {
    NORDWEST("Nordwest-Ecke"),
    NORDOST("Nordost-Ecke"),
    SUEDWEST("Südwest-Ecke"),
    SUEDOST("Südost-Ecke");

    /** Die Stadionseite, der diese Ecke angehört (Langseiten-Hälfte). */
    val lage: StadionLage
        get() = when (this) {
            NORDWEST, NORDOST -> StadionLage.NORD
            SUEDWEST, SUEDOST -> StadionLage.SUED
        }
}

/**
 * Die gewählte Bauform des Stadions. Wird ausschließlich aus den Gesamtwerten
 * eines [StadionPlanDaten] abgeleitet ([StadionPlanLogik.variante]) – dadurch
 * stellen „Stadionausbau" und „Teaminformation" denselben Plan dar.
 */
data class StadionVariante(
    val band: StadionBand,
    val grundform: Grundform,
    val vip: VipPlatzierung = VipPlatzierung.KEIN,
    val rangzahl: Int = band.rangzahl,
    /** Übergroße Südkurve (nur mit Stehplätzen in Band C bis E möglich). */
    val riesenKurve: Boolean = false,
    /** Teildach: überdachter Anteil < Gesamtkapazität. */
    val teilDach: Boolean = false,
    /** Mäh-/Druckmuster des Rasens (deterministisch aus dem Varianten-Seed). */
    val rasen: RasenMuster = RasenMuster.KEINS,
    /**
     * Ecke, in der der Gästeblock großer Stadien als eigener Eck-Viertelbereich
     * sitzt. `null`, wenn kein Gästeblock oder kein geschlossener Ring vorhanden
     * ist (dann bleibt der Gästeblock ein proportionaler Sektor im Band).
     */
    val gaesteblockEcke: Ecke? = null,
) {
    val offeneEcken: Boolean get() = grundform.offeneEcken

    /** Kurze Bautyp-Beschriftung, z. B. „Mittel-Stadion · Achteck – Riesenkurve". */
    val bautypZeile: String get() {
        val basis = "${band.anzeigeName} · ${grundform.anzeigeName}"
        val besonderheiten = buildList {
            if (riesenKurve) add("Riesenkurve")
            if (teilDach) add("Teildach")
        }
        return if (besonderheiten.isEmpty()) {
            basis
        } else {
            "$basis – ${besonderheiten.joinToString(", ")}"
        }
    }
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
    /**
     * `true`, wenn die Kategorie nur die NICHT überdachten Plätze umfasst
     * („Nicht überdachte Plätze"). Die überdachten Flächen werden dann im Plan
     * ausgegraut, die freien bleiben hervorgehoben.
     */
    val nurUnueberdacht: Boolean = false,
) {
    /** Plätze dieser Kategorie ohne Überdachung. */
    val nichtUeberdacht: Int get() = (plaetze - davonUeberdacht).coerceAtLeast(0)

    val anzeigeName: String get() = name ?: when {
        nurUnueberdacht -> "Nicht überdachte ${typ?.bezeichnung ?: "Plätze"}"
        typ == null -> "Überdacht"
        ueberdacht -> "Überdachte ${typ.bezeichnung}"
        else -> typ.bezeichnung
    }

    val beschreibung: String get() = when {
        nurUnueberdacht ->
            "Anteil der Plätze ohne Überdachung – alle überdachten Flächen werden im Plan ausgegraut."
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
    /**
     * Ausgebauter Anzeigetyp (z. B. „Videowürfel“, „Multimediawürfel“ oder
     * „Megawürfel“); `null` bzw. die schlichte „Anzeigetafel“ zählen nicht als
     * Premium. Steuert zusammen mit [rasenheizung] das Rasenmuster.
     */
    val anzeigetafel: String? = null,
    /** Ob eine Rasenheizung installiert ist. */
    val rasenheizung: Boolean = false,
) {
    val kapazitaet: Int get() = stehplaetze + sitzplaetze

    /**
     * Premium-Ausstattung: eine bessere Anzeigetafel als die einfache und eine
     * installierte Rasenheizung. Damit bekommt der Rasen garantiert ein Muster
     * (auch in kleinen Stadien), das sich mit der Saison ändert.
     */
    val hatPremiumAusstattung: Boolean
        get() = rasenheizung &&
            !anzeigetafel.isNullOrBlank() &&
            !anzeigetafel.equals("Anzeigetafel", ignoreCase = true)
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
    /**
     * Wenn gesetzt, besetzt diese Zone eine komplette Eck-Viertelfläche des
     * abgerundeten Rings (der Gästeblock großer Stadien). Die Zone belegt dann
     * kein Seitenband und trägt genau diese eine Kurven-Ecke.
     */
    val ecke: Ecke? = null,
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
    // Mit der Stadiongröße wachsen auch die Tribünen: Erst die Haupttribüne (West),
    // dann die Gegentribüne (Ost), danach die Südkurve und zuletzt die Nordkurve.
    private const val GEGENTRIBUENE_AB = 6_000
    private const val SUEDKURVE_AB = 12_000
    private const val NORDKURVE_AB = 20_000
    // Ab geschlossenem Ring (alle vier Ecken bestückt) wandert der Gästeblock in
    // eine Ecke; darunter bleibt er ein proportionaler Sektor in der Südkurve.
    private const val GAESTEBLOCK_ECK_AB = NORDKURVE_AB

    /** Determiniert den Detaillierungsgrad aus der Gesamtkapazität. */
    fun detaillierungsGrad(kapazitaet: Int): Detaillierungsgrad = when {
        kapazitaet >= 60_000 -> Detaillierungsgrad.SEHR_GROSS
        kapazitaet >= 30_000 -> Detaillierungsgrad.GROSS
        kapazitaet >= 12_000 -> Detaillierungsgrad.MITTEL
        else -> Detaillierungsgrad.KLEIN
    }

    /**
     * Bestimmt deterministisch die Bauform ([StadionVariante]) eines Stadions aus
     * seinen Gesamtwerten. Grundlage ist nur das, was beide Datenquellen
     * („Stadionausbau" und Teaminfo `s=5`) liefern – dadurch bleibt der Plan in
     * beiden Screens identisch. Die [Grundform] wird über einen stabilen Seed aus
     * Kapazität, Sitz-/Stehplätzen und überdachten Anteilen gewählt; gleiche Werte
     * ergeben immer dieselbe Bauform.
     *
     * [saison] fließt nur in die Wahl des [RasenMuster]s ein (Muster-Wechsel je
     * Saison); `0` (unbekannt) bedeutet jeweils die Basiswahl ohne Saison-Einfluss.
     */
    fun variante(plan: StadionPlanDaten, saison: Int = 0): StadionVariante {
        if (plan.kapazitaet <= 0) {
            return StadionVariante(band = StadionBand.A, grundform = Grundform.KASTEN)
        }
        val band = StadionBand.fuer(plan.kapazitaet)
        val formen = Grundform.values()
        val seed = varianteSeed(
            kapazitaet = plan.kapazitaet,
            sitzplaetze = plan.sitzplaetze,
            stehplaetze = plan.stehplaetze,
            sitzUeberdacht = plan.sitzUeberdacht,
            stehUeberdacht = plan.stehUeberdacht,
        )
        val grundform = formen[seed % formen.size]
        val vip = when (band) {
            StadionBand.A -> VipPlatzierung.KEIN
            StadionBand.B, StadionBand.C -> VipPlatzierung.STREIFEN
            StadionBand.D -> VipPlatzierung.MITTELRANG
            StadionBand.E -> if (seed % 2 == 0) VipPlatzierung.VERSTREUT else VipPlatzierung.MITTELRANG
        }
        val riesenKurve =
            plan.stehplaetze > 0 &&
                band.ordinal >= StadionBand.C.ordinal &&
                seed % 8 == 0
        val teilDach = (plan.sitzUeberdacht + plan.stehUeberdacht) < plan.kapazitaet
        // Gästeblock in einer Ecke der Nordkurve (traditioneller Gästetribünen-
        // Bereich): nur bei geschlossenem Ring und nicht, wenn die Ecken ohnehin
        // offen bleiben. Zwischen Nordwest und Nordost wechselt die Wahl
        // deterministisch (`seed / 4`), kleinere Stadien ohne Nordkurve behalten
        // den proportionalen Sektor in der Südkurve.
        val gaesteblockEcke = if (
            grundform != Grundform.OFFENE_ECKEN &&
            plan.kapazitaet >= GAESTEBLOCK_ECK_AB
        ) {
            val nordEcken = listOf(Ecke.NORDWEST, Ecke.NORDOST)
            nordEcken[(seed / 4) % nordEcken.size]
        } else {
            null
        }
        return StadionVariante(
            band = band,
            grundform = grundform,
            vip = vip,
            rangzahl = band.rangzahl,
            riesenKurve = riesenKurve,
            teilDach = teilDach,
            rasen = rasenMuster(plan.kapazitaet, seed, saison, plan.hatPremiumAusstattung),
            gaesteblockEcke = gaesteblockEcke,
        )
    }

    /**
     * Wählt das [RasenMuster] deterministisch anhand der Gesamtkapazität und des
     * Varianten-Seeds. Kleine Stadien ([Detaillierungsgrad.KLEIN]) haben bewusst
     * kein Muster; mit wachsender Stadionklasse kommen zunehmend ausgefallenere
     * Muster dazu, bis in der obersten Klasse alle fünf möglich sind.
     * Mit [premiumAusstattung] (bessere Anzeigetafel + Rasenheizung) bekommt auch
     * ein kleines Stadion garantiert ein Muster (nie [RasenMuster.KEINS]) und das
     * Muster wechselt mit jeder [saison] (Saison-Rotation). Ohne Saison (0) bleibt
     * es bei der reinen Seed-Basiswahl.
     */
    fun rasenMuster(
        kapazitaet: Int,
        seed: Int,
        saison: Int = 0,
        premiumAusstattung: Boolean = false,
    ): RasenMuster {
        val grad = detaillierungsGrad(kapazitaet)
        val muster = when {
            !premiumAusstattung && grad == Detaillierungsgrad.KLEIN ->
                listOf(RasenMuster.KEINS)
            !premiumAusstattung && grad == Detaillierungsgrad.MITTEL ->
                listOf(RasenMuster.KEINS, RasenMuster.STREIFEN_BREIT)
            !premiumAusstattung && grad == Detaillierungsgrad.GROSS ->
                listOf(RasenMuster.STREIFEN_BREIT, RasenMuster.STREIFEN_SCHMAL, RasenMuster.KARRIERT)
            !premiumAusstattung ->
                RasenMuster.values().toList()
            grad == Detaillierungsgrad.KLEIN || grad == Detaillierungsgrad.MITTEL ->
                listOf(
                    RasenMuster.STREIFEN_BREIT,
                    RasenMuster.STREIFEN_SCHMAL,
                    RasenMuster.KARRIERT,
                )
            grad == Detaillierungsgrad.GROSS ->
                listOf(
                    RasenMuster.STREIFEN_BREIT,
                    RasenMuster.STREIFEN_SCHMAL,
                    RasenMuster.KARRIERT,
                    RasenMuster.KREISE,
                )
            else ->
                listOf(
                    RasenMuster.STREIFEN_BREIT,
                    RasenMuster.STREIFEN_SCHMAL,
                    RasenMuster.KARRIERT,
                    RasenMuster.KREISE,
                )
        }
        val index = (seed + saison) % muster.size
        return muster[index]
    }

    /** Stabiler, nicht-negativer Seed für die Wahl der [Grundform]. */
    private fun varianteSeed(
        kapazitaet: Int,
        sitzplaetze: Int,
        stehplaetze: Int,
        sitzUeberdacht: Int,
        stehUeberdacht: Int,
    ): Int {
        var h = kapazitaet * 31 + sitzplaetze
        h = h * 31 + stehplaetze
        h = h * 31 + sitzUeberdacht
        h = h * 31 + stehUeberdacht
        return h and 0x7fffffff
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
        variannt: StadionVariante = variante(plan),
    ): List<PlatzKategorie> = buildList {
        if (plan.kapazitaet <= 0) return@buildList
        val modell = tribuenenModell(plan, grad, variannt)

        if (plan.stehplaetze > 0) {
            add(PlatzKategorie(PlatzTyp.STEH, ueberdacht = false, plan.stehplaetze, plan.stehUeberdacht))
        }
        if (plan.sitzplaetze > 0) {
            add(PlatzKategorie(PlatzTyp.SITZ, ueberdacht = false, plan.sitzplaetze, plan.sitzUeberdacht))
        }

        // Stadionweite Kategorie der nicht überdachten Plätze – nur bei
        // gemischter Überdachung: teils überdacht UND teils offen.
        val ueberdachtGesamt = plan.sitzUeberdacht + plan.stehUeberdacht
        val unueberdachtGesamt = (plan.kapazitaet - ueberdachtGesamt).coerceAtLeast(0)
        if (ueberdachtGesamt > 0 && unueberdachtGesamt > 0) {
            add(
                PlatzKategorie(
                    typ = null,
                    ueberdacht = false,
                    plaetze = unueberdachtGesamt,
                    davonUeberdacht = 0,
                    nurUnueberdacht = true,
                    name = "Nicht überdachte Plätze",
                ),
            )
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
            val lage = variannt.gaesteblockEcke?.lage ?: StadionLage.SUED
            add(kategorie("Gäste", hauptTyp(modell.gaesteblock!!), modell.gaesteblock, lage))
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
    fun zonen(
        plan: StadionPlanDaten,
        variannt: StadionVariante = variante(plan),
    ): List<StadionPlanZone> {
        val echte = echteBereiche(plan)
        val nutzbar = echte.isNotEmpty() && echte.size == plan.bereiche.size
        return if (nutzbar) {
            echte.map { zoneVonBereich(it) }
        } else {
            aggregatZonen(plan, variannt)
        }
    }

    private fun aggregatZonen(
        plan: StadionPlanDaten,
        variannt: StadionVariante,
    ): List<StadionPlanZone> {
        val grad = detaillierungsGrad(plan.kapazitaet)
        val modell = tribuenenModell(plan, grad, variannt)
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
        modell.gaesteblock?.let { gaest ->
            if (gaest.kapazitaet > 0) {
                // Große Stadien: Gästeblock besetzt eine komplette Eck-Viertelfläche
                // (deterministische Ecke aus der Variante). Kleine Stadien: weiterhin
                // ein proportionaler Sektor in der Südkurve. Bei offenen Ecken gibt
                // es keine Eck-Gäste (Ring-Ecken fehlen).
                zonen += if (variannt.gaesteblockEcke != null && !variannt.offeneEcken) {
                    zoneVonTribuene("Gäste", gaest).copy(
                        lage = variannt.gaesteblockEcke!!.lage,
                        istKurve = true,
                        ecke = variannt.gaesteblockEcke,
                    )
                } else {
                    zoneVonTribuene("Gäste", gaest)
                }
            }
        }

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
     * Die Tribünen wachsen mit der Stadiongröße: Kleine Stadien haben nur die
     * Haupttribüne (West), dann kommen Gegentribüne (Ost), Südkurve und zuletzt die
     * Nordkurve hinzu. Langseiten überwiegend Sitzplätze, Kurzseiten überwiegend
     * Stehplätze; der Gästeblock sitzt als eigener Bereich in der Südkurve.
     * Sonderbereiche (VIP, barrierefrei) erscheinen nur ab der jeweiligen Größe.
     * Die Summen entsprechen exakt den Stadiondaten.
     */
    fun tribuenenModell(
        plan: StadionPlanDaten,
        grad: Detaillierungsgrad = detaillierungsGrad(plan.kapazitaet),
        variannt: StadionVariante = variante(plan),
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

        // Präsenz je Seite: Tribünen wachsen sanft mit der Stadiongröße, statt
        // hart bei einer Schwelle zu erscheinen. West ist immer da; Ost/Süd/Nord
        // blenden über einen Bereich ein (0..1), damit kein sprunghafter
        // Kapazitätswechsel beim Ausbau entsteht.
        fun rampe(ab: Int, voll: Int): Float = when {
            kap <= ab -> 0f
            kap >= voll -> 1f
            else -> (kap - ab).toFloat() / (voll - ab).toFloat()
        }
        // Reihenfolge der vier Seiten: West, Ost, Süd, Nord.
        val praesenz = listOf(
            1f,
            rampe(GEGENTRIBUENE_AB, GEGENTRIBUENE_AB + 4_000),
            rampe(SUEDKURVE_AB, SUEDKURVE_AB + 4_000),
            rampe(NORDKURVE_AB, NORDKURVE_AB + 4_000),
        )

        // Sonderbereiche nur ab typischer Größe – vorher nicht vorhanden.
        // VIP = oberste zwei Reihen der Haupttribüne (≈ 2 % der Sitzplätze).
        val vipZiel = if (kap >= VIP_AB) (sitz * 0.02f).roundToInt() else 0
        // Barrierefrei übernimmt die Fläche/Anzahl, die vormals der separate
        // VIP-Sektor belegt hat.
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

        // Gästeblock: überwiegend Sitzplätze (ca. 60 %), Rest Stehplätze – wie in
        // echten Gästeblöcken, statt früher „zuerst alles aus Stehplätzen".
        val gaestZiel = if (kap >= GAESTEBLOCK_AB) ((kap * 0.05f).roundToInt()).coerceIn(400, 6000) else 0
        val gaestSitzZiel = (gaestZiel * 0.6f).roundToInt()
        val gaestStehZiel = gaestZiel - gaestSitzZiel
        val gaestSitz = min(gaestSitzZiel, sitzOhneSonder.coerceAtLeast(0))
        val gaestSteh = min(gaestStehZiel, steh)
        val stehOhneGaest = steh - gaestSteh
        val sitzVerteilt = sitzOhneSonder - gaestSitz

        // Seiten-Gewichte je Ausbaustufe; mit der Stadionklasse wachsen die Kurven.
        val lagenGew = lagenGewichte(grad, variannt.riesenKurve)
        // Sitz-/Steh-Präferenz je Seite (West/Ost/Süd/Nord). Reine Sitzplatz-
        // Stadien (All-Seater) werden ausgewogen verteilt; sonst gelten die
        // Bundesliga-typischen Stehkurven (Langseiten sitzen, Kurven stehen).
        val sitzPraef = if (steh == 0) {
            listOf(0.25f, 0.25f, 0.25f, 0.25f)
        } else {
            listOf(0.90f, 0.80f, 0.40f, 0.40f)
        }
        val stehPraef = listOf(0.10f, 0.20f, 0.75f, 0.75f)

        val wSitz = normalisiere(lagenGew.indices.map { lagenGew[it] * praesenz[it] * sitzPraef[it] })
        val wSteh = normalisiere(lagenGew.indices.map { lagenGew[it] * praesenz[it] * stehPraef[it] })

        val sitzJe = verteile(sitzVerteilt, wSitz)
        val stehJe = verteile(stehOhneGaest, wSteh)

        // Überdachte Plätze je Position (getrennt nach Sitz/Steh), anteilig.
        // Index-Reihenfolge: West, Ost, Süd, Nord, dann Gäste.
        val udGewichteSitz = buildList {
            for (i in 0..3) add((sitzJe[i] + if (i == 0) vip + barrierefrei else 0).toFloat())
            add(gaestSitz.toFloat())
        }
        val udSitz = verteile(plan.sitzUeberdacht.coerceAtLeast(0), udGewichteSitz)
        val udWest = verteile(
            udSitz.getOrElse(0) { 0 },
            listOf(vip.toFloat(), barrierefrei.toFloat(), sitzJe.getOrElse(0) { 0 }.toFloat()),
        )
        val udSteh = verteile(
            plan.stehUeberdacht.coerceAtLeast(0),
            buildList {
                for (i in 0..3) add(stehJe.getOrElse(i) { 0 }.toFloat())
                add(gaestSteh.toFloat())
            },
        )

        fun tribuene(lage: StadionLage, sitzUeber: Int, stehUeber: Int): Zonentribuene {
            val i = when (lage) {
                StadionLage.WEST -> 0
                StadionLage.OST -> 1
                StadionLage.SUED -> 2
                StadionLage.NORD -> 3
            }
            return Zonentribuene(
                lage = lage,
                sitz = Platzangabe(sitzJe[i], sitzUeber),
                steh = Platzangabe(stehJe[i], stehUeber),
            )
        }

        return TribuenenModell(
            haupttribuene = tribuene(StadionLage.WEST, udWest.getOrElse(2) { 0 }, udSteh.getOrElse(0) { 0 }),
            gegentribuene = tribuene(StadionLage.OST, udSitz.getOrElse(1) { 0 }, udSteh.getOrElse(1) { 0 }),
            suedkurve = tribuene(StadionLage.SUED, udSitz.getOrElse(2) { 0 }, udSteh.getOrElse(2) { 0 }),
            nordkurve = tribuene(StadionLage.NORD, udSitz.getOrElse(3) { 0 }, udSteh.getOrElse(3) { 0 }),
            gaesteblock = if (gaestSitz + gaestSteh > 0) {
                Zonentribuene(
                    variannt.gaesteblockEcke?.lage ?: StadionLage.SUED,
                    Platzangabe(gaestSitz, udSitz.getOrElse(4) { 0 }),
                    Platzangabe(gaestSteh, udSteh.getOrElse(4) { 0 }),
                )
            } else {
                null
            },
            vip = Platzangabe(vip, udWest.getOrElse(0) { 0 }),
            barrierefrei = Platzangabe(barrierefrei, udWest.getOrElse(1) { 0 }),
        )
    }

    /** Anteilige Gewichte der vier Stadionseiten je Ausbaustufe. */
    private fun lagenGewichte(
        grad: Detaillierungsgrad,
        riesenKurve: Boolean = false,
    ): List<Float> {
        val basis = when (grad) {
            Detaillierungsgrad.KLEIN -> listOf(0.34f, 0.30f, 0.18f, 0.18f)
            Detaillierungsgrad.MITTEL -> listOf(0.32f, 0.28f, 0.20f, 0.20f)
            Detaillierungsgrad.GROSS -> listOf(0.31f, 0.27f, 0.21f, 0.21f)
            Detaillierungsgrad.SEHR_GROSS -> listOf(0.30f, 0.26f, 0.22f, 0.22f)
        }
        // Riesenkurve (Dortmund-Typ): Die Südkurve trägt deutlich mehr Plätze,
        // dafür West/Ost/Nord etwas weniger.
        if (!riesenKurve) return basis
        return normalisiere(
            listOf(
                basis[0] * 0.92f,
                basis[1] * 0.88f,
                basis[2] * 1.35f,
                basis[3] * 0.72f,
            ),
        )
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