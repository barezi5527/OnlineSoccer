package com.onlinesoccer.app.feature.team

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.onlinesoccer.app.data.model.Ecke
import com.onlinesoccer.app.data.model.StadionLage
import com.onlinesoccer.app.data.model.StadionPlanDaten
import com.onlinesoccer.app.data.model.StadionPlanLogik
import com.onlinesoccer.app.data.model.StadionPlanZone
import com.onlinesoccer.app.data.model.StadionVariante
import com.onlinesoccer.app.data.model.VipPlatzierung
import kotlin.math.max
import kotlin.math.min

/** Ausrichtung eines Bereichs im Plan (abhängig von der Stadionseite). */
internal enum class ZoneAusrichtung {
    /** Waagerechter Bereich (Nord oben / Süd unten): Sitz-/Stehband übereinander. */
    WAAGERECHT,
    /** Senkrechter Bereich (West links / Ost rechts): Bänder nebeneinander. */
    SENKRECHT,
}

/** Lage eines VIP-Streifens innerhalb der Sitzbänder. */
internal enum class StreifenPosition {
    /** Äußerste Kante (oberste Reihen) der Tribüne. */
    AUSSEN,
    /** In der Mitte des Sitzbands (Mittelrang-VIP). */
    MITTELRANG,
}

/** Fläche eines darzustellenden Seitenbereichs samt zugehöriger Zone. */
internal data class ZoneFlaeche(
    val zoneIndex: Int,
    val lage: StadionLage,
    val flaeche: Rect,
)

/**
 * Streifen eines Sonderbereichs, der kein eigenes Seitenband belegt (VIP):
 * ein Abschnitt an der äußeren Kante [StreifenPosition.AUSSEN] oder in der Mitte
 * des Sitzbands [StreifenPosition.MITTELRANG] der jeweiligen Stadionseite.
 */
internal data class ZoneStreifen(
    val zoneIndex: Int,
    val rechteck: Rect,
    val position: StreifenPosition = StreifenPosition.AUSSEN,
)

/**
 * Geometrie einer Ecke des abgerundeten Stadionbowls. Der Eckbereich ist ein
 * 90°-Kreisbogen (Ringsegment) um [mitte] – den Eckmittelpunkt des Bowl – plus
 * den L-förmigen Restbereich bis zur Umlauf-Ecke. [sx]/[sy] geben die Richtung
 * von der Bowl-Ecke aus an (NW = (1,1), NO = (-1,1), SO = (-1,-1), SW = (1,-1)).
 */
internal data class EckGeometrie(
    val mitte: Offset,
    val sx: Float,
    val sy: Float,
    val startWinkel: Float,
)

/** Geometrie einer Kurzseiten-Kurve (Südkurve/Nordkurve) samt ihrer zwei Ecken. */
internal data class KurvenGeometrie(
    val zoneIndex: Int,
    val lage: StadionLage,
    val ecken: List<EckGeometrie>,
)

/**
 * Berechnete Geometrie des Stadionplans in Pixel-Koordinaten. Wird sowohl zum
 * Zeichnen als auch für das Antippen der Bereiche verwendet.
 */
internal data class PlanGeometrie(
    /** Äußere Kante des Stadion-Bowls (Draufsicht). */
    val aussen: Rect,
    /** Innere Kante des Rings (Umlauf direkt um das Spielfeld). */
    val innen: Rect,
    /** Die Bereichsflächen des Rings (Bandabschnitte der Seiten). */
    val flaechen: List<ZoneFlaeche>,
    /** Zusätzliche Streifen auf den obersten Reihen (VIP) ohne eigenen Sektor. */
    val streifen: List<ZoneStreifen>,
    /** Die Kurzseiten-Kurven des Rings mit ihren zwei Eckbereichen. */
    val kurven: List<KurvenGeometrie>,
    /** Tiefe des Rings (äußere zu innerer Kante). */
    val ringTiefe: Float,
    /** Eckenradius der äußeren Bowl-Kante. */
    val eckRadius: Float,
    /** Eckenradius der inneren Kante (Umlauf). */
    val innenRadius: Float,
    /** Spielfeld (68:105, vertikal) im Zentrum. */
    val pitch: Rect,
    val zonen: List<StadionPlanZone>,
    /** Anzahl der Ränge (Trennlinien) dieser Bauform. */
    val rangzahl: Int,
    /** Die zugrunde liegende Bauform (Band, Grundform, VIP-Modus). */
    val variante: StadionVariante,
) {
    init {
        require(flaechen.all { it.zoneIndex in zonen.indices }) {
            "Bereichsflächen verweisen auf gültige Zonen-Indizes"
        }
        require(streifen.all { it.zoneIndex in zonen.indices }) {
            "Streifen verweisen auf gültige Zonen-Indizes"
        }
        require(kurven.all { it.zoneIndex in zonen.indices }) {
            "Kurvenflächen verweisen auf gültige Zonen-Indizes"
        }
    }
}

private fun ausrichtung(lage: StadionLage): ZoneAusrichtung =
    if (lage == StadionLage.WEST || lage == StadionLage.OST) ZoneAusrichtung.SENKRECHT else ZoneAusrichtung.WAAGERECHT

/**
 * Leitet aus den Plan-Daten und der Canvas-Größe die Geometrie ab. Alle Maße
 * sind relative Anteile der kleineren Canvas-Kante – keine festen Stadiongrößen.
 */
internal fun berechneGeometrie(
    breite: Float,
    hoehe: Float,
    plan: StadionPlanDaten,
    zonen: List<StadionPlanZone>,
    variante: StadionVariante,
): PlanGeometrie {
    val basis = min(breite, hoehe)
    val kapazitaetsFaktor = StadionPlanLogik.kapazitaetsFaktor(plan.kapazitaet)

    // Kleine Stadien belegen weniger Fläche (mehr Außenfläche), große füllen den Bowl.
    val aussenRand = basis * 0.02f + basis * 0.10f * (1f - kapazitaetsFaktor)
    val ringTiefe = basis * (0.085f + 0.10f * kapazitaetsFaktor)
    // Eckenradius je Grundform: Achteck kantig, Kasten neutral, Oval stark gerundet.
    val eckBasis = basis * (0.030f + 0.050f * kapazitaetsFaktor)
    val eckRadius = (eckBasis * variante.grundform.eckFaktor).coerceAtMost(basis * 0.45f)
    val innenRadius = max(0f, eckRadius - ringTiefe)

    val aussen = Rect(
        aussenRand,
        aussenRand,
        breite - aussenRand,
        hoehe - aussenRand,
    )
    val innen = Rect(
        aussen.left + ringTiefe,
        aussen.top + ringTiefe,
        aussen.right - ringTiefe,
        aussen.bottom - ringTiefe,
    )

    val abstand = min(innen.width, innen.height) * 0.055f
    val verfuegbarBreite = (innen.width - 2f * abstand).coerceAtLeast(0f)
    val verfuegbarHoehe = (innen.height - 2f * abstand).coerceAtLeast(0f)

    // Vertikales Spielfeld mit korrekten Proportionen (68:105).
    var pitchBreite: Float
    var pitchHoehe: Float
    if (verfuegbarBreite * 105f / 68f <= verfuegbarHoehe) {
        pitchBreite = verfuegbarBreite
        pitchHoehe = pitchBreite * 105f / 68f
    } else {
        pitchHoehe = verfuegbarHoehe
        pitchBreite = pitchHoehe * 68f / 105f
    }
    val pitch = Rect(
        innen.center.x - pitchBreite / 2f,
        innen.center.y - pitchHoehe / 2f,
        innen.center.x + pitchBreite / 2f,
        innen.center.y + pitchHoehe / 2f,
    )

    val luecke = basis * 0.008f
    val flaechen = mutableListOf<ZoneFlaeche>()
    val sektorJeZone = mutableMapOf<Int, Rect>()
    zonen.filter { it.lage != null }.groupBy { it.lage!! }.forEach { (lage, gruppe) ->
        val trasse = when (lage) {
            StadionLage.NORD -> Rect(innen.left, aussen.top, innen.right, innen.top)
            StadionLage.SUED -> Rect(innen.left, innen.bottom, innen.right, aussen.bottom)
            StadionLage.WEST -> Rect(aussen.left, innen.top, innen.left, innen.bottom)
            StadionLage.OST -> Rect(innen.right, innen.top, aussen.right, innen.bottom)
        }
        val haengend = ausrichtung(lage) == ZoneAusrichtung.SENKRECHT
        val laenge = (if (haengend) trasse.height else trasse.width).coerceAtLeast(0f)
        // Zonen, die eine ganze Ecke besetzen (Gästeblock), belegen kein Seitenband.
        val bandZonen = gruppe.filterNot { it.vipStreifen || it.ecke != null }
        val anteile = gewichtAnteile(bandZonen)
        var start = if (haengend) trasse.top else trasse.left
        bandZonen.forEach { zone ->
            val sektorLaenge = laenge * anteile[bandZonen.indexOf(zone)]
            val sektor = if (haengend) {
                Rect(
                    trasse.left,
                    start + luecke / 2f,
                    trasse.right,
                    start + sektorLaenge - luecke / 2f,
                )
            } else {
                Rect(
                    start + luecke / 2f,
                    trasse.top,
                    start + sektorLaenge - luecke / 2f,
                    trasse.bottom,
                )
            }
            if (sektorLaenge > luecke) {
                val zoneIndex = zonen.indexOfFirst { it === zone }
                flaechen += ZoneFlaeche(
                    zoneIndex = zoneIndex,
                    lage = lage,
                    flaeche = sektor,
                )
                sektorJeZone[zoneIndex] = sektor
            }
            start += sektorLaenge
        }
    }

    // VIP-Streifen: je Modus auf einer oder mehreren Stadionseiten. Der Streifen
    // liegt an der äußeren Kante ([STREIFEN]) bzw. in der Mitte des Sitzbands
    // ([MITTELRANG]) der jeweiligen Tribüne; bei [VERSTREUT] wird der VIP-Bereich
    // über den ganzen Ring (West Mitte, Ost/Nord/Süd außen) verteilt. Die Tiefe
    // wird an das Sitzband der Tribüne geklemmt, damit er nie über das Stehband
    // oder den Rand hinaus zeichnet.
    val vipZonen = zonen.filter { it.vipStreifen }
    val streifen = buildList {
        vipZonen.forEach { zone ->
            val modus = variante.vip
            if (modus == VipPlatzierung.KEIN) return@forEach
            val seiten: List<StadionLage> = when (modus) {
                VipPlatzierung.STREIFEN, VipPlatzierung.MITTELRANG -> listOf(StadionLage.WEST)
                VipPlatzierung.VERSTREUT -> listOf(
                    StadionLage.WEST, StadionLage.OST, StadionLage.NORD, StadionLage.SUED,
                )
                VipPlatzierung.KEIN -> emptyList()
            }
            val zoneIndex = zonen.indexOfFirst { it === zone }
            seiten.forEach { seite ->
                val position = if (
                    modus == VipPlatzierung.MITTELRANG ||
                    (modus == VipPlatzierung.VERSTREUT && seite == StadionLage.WEST)
                ) {
                    StreifenPosition.MITTELRANG
                } else {
                    StreifenPosition.AUSSEN
                }
                var owner = zonen.indexOfFirst { it.lage == seite && it.name == "Haupttribüne" }
                if (owner < 0) {
                    owner = zonen.indexOfFirst { it.lage == seite && !it.vipStreifen }
                }
                if (owner < 0) return@forEach
                val sektor = sektorJeZone[owner] ?: return@forEach
                val haengend = ausrichtung(seite) == ZoneAusrichtung.SENKRECHT
                val tiefe = if (haengend) sektor.width else sektor.height
                if (tiefe <= 0f) return@forEach
                val ownerZone = zonen.getOrNull(owner)
                val (sitzT, _) = if (ownerZone != null) {
                    StadionPlanLogik.bandTiefen(ownerZone.sitzAnteil, ownerZone.stehAnteil, tiefe)
                } else {
                    tiefe to 0f
                }
                val kanteFaktor = if (position == StreifenPosition.MITTELRANG) 0.20f else 0.14f
                val streifenTiefe = min(tiefe * kanteFaktor, sitzT.coerceAtLeast(0f))
                if (streifenTiefe <= 0f) return@forEach
                val abstand = if (position == StreifenPosition.MITTELRANG) {
                    max(0f, (sitzT - streifenTiefe) / 2f)
                } else {
                    0f
                }
                val rechteck: Rect = when (seite) {
                    StadionLage.WEST ->
                        Rect(sektor.left + abstand, sektor.top, sektor.left + abstand + streifenTiefe, sektor.bottom)
                    StadionLage.OST ->
                        Rect(sektor.right - abstand - streifenTiefe, sektor.top, sektor.right - abstand, sektor.bottom)
                    StadionLage.NORD ->
                        Rect(sektor.left, sektor.top + abstand, sektor.right, sektor.top + abstand + streifenTiefe)
                    StadionLage.SUED ->
                        Rect(sektor.left, sektor.bottom - abstand - streifenTiefe, sektor.right, sektor.bottom - abstand)
                }
                add(ZoneStreifen(zoneIndex = zoneIndex, rechteck = rechteck, position = position))
            }
        }
    }

    // Kurven nur, wenn die Ecken bestückt sind: Bei „Offene Ecken" bleiben die
    // Eckbereiche des Rings frei (Laufbahn-Optik). Eine vom Gästeblock belegte
    // Ecke wird aus der jeweiligen Kurven-Zone herausgelöst und dem Gästeblock
    // als komplette Eck-Viertelfläche zugeordnet.
    val kurven = if (variante.offeneEcken) {
        emptyList()
    } else {
        val besetzteEcken = zonen.filter { it.ecke != null }.associateBy { it.ecke!! }
        buildList {
            zonen.filter { it.istKurve && it.ecke == null }.forEach { zone ->
                val lage = zone.lage ?: return@forEach
                val ecken = eckenDerSeite(lage, aussen, eckRadius)
                    .filter { (ecke, _) -> !besetzteEcken.containsKey(ecke) }
                    .map { it.second }
                if (ecken.isNotEmpty()) {
                    add(
                        KurvenGeometrie(
                            zoneIndex = zonen.indexOfFirst { it === zone },
                            lage = lage,
                            ecken = ecken,
                        ),
                    )
                }
            }
            besetzteEcken.forEach { (ecke, zone) ->
                val ecken = eckenDerSeite(ecke.lage, aussen, eckRadius)
                    .filter { it.first == ecke }
                    .map { it.second }
                add(
                    KurvenGeometrie(
                        zoneIndex = zonen.indexOfFirst { it === zone },
                        lage = ecke.lage,
                        ecken = ecken,
                    ),
                )
            }
        }
    }

    return PlanGeometrie(
        aussen = aussen,
        innen = innen,
        flaechen = flaechen,
        streifen = streifen,
        kurven = kurven,
        ringTiefe = ringTiefe,
        eckRadius = eckRadius,
        innenRadius = innenRadius,
        pitch = pitch,
        zonen = zonen,
        rangzahl = variante.rangzahl,
        variante = variante,
    )
}

/**
 * Die beiden Eck-Viertelkreise einer Kurzseite, jeweils benannt nach der [Ecke].
 * Reihenfolge: erst die West-, dann die Ost-Ecke.
 */
private fun eckenDerSeite(
    lage: StadionLage,
    aussen: Rect,
    eckRadius: Float,
): List<Pair<Ecke, EckGeometrie>> = when (lage) {
    StadionLage.NORD -> listOf(
        Ecke.NORDWEST to EckGeometrie(Offset(aussen.left + eckRadius, aussen.top + eckRadius), 1f, 1f, 180f),
        Ecke.NORDOST to EckGeometrie(Offset(aussen.right - eckRadius, aussen.top + eckRadius), -1f, 1f, 270f),
    )
    StadionLage.SUED -> listOf(
        Ecke.SUEDWEST to EckGeometrie(Offset(aussen.left + eckRadius, aussen.bottom - eckRadius), 1f, -1f, 90f),
        Ecke.SUEDOST to EckGeometrie(Offset(aussen.right - eckRadius, aussen.bottom - eckRadius), -1f, -1f, 0f),
    )
    else -> emptyList()
}

/** Relative Anteile einer Zonengruppe entlang einer Seite (Gewicht = Kapazität). */
private fun gewichtAnteile(zonen: List<StadionPlanZone>): List<Float> {
    var gesamt = 0f
    zonen.forEach { gesamt += it.gewicht.coerceAtLeast(0f) }
    if (gesamt <= 0f) {
        val gleich = 1f / zonen.size.coerceAtLeast(1)
        return List(zonen.size) { gleich }
    }
    val anteile = mutableListOf<Float>()
    zonen.forEach { anteile += it.gewicht.coerceAtLeast(0f) / gesamt }
    return anteile
}

/**
 * Ordnet einen Antipp-Punkt (in Canvas-Koordinaten) einer Zone zu.
 * `null` = außerhalb des Stadionrings (Spielfeld, Concourse oder Umfeld).
 */
internal fun zoneAnPunkt(p: Offset, geometrie: PlanGeometrie): Int? {
    if (!inAbgerundetemRechteck(p, geometrie.aussen, geometrie.eckRadius)) return null
    if (inAbgerundetemRechteck(p, geometrie.innen, geometrie.innenRadius)) return null

    geometrie.kurven.firstOrNull { kg -> kg.ecken.any { inEckbereich(p, geometrie, it) } }
        ?.let { return it.zoneIndex }

    geometrie.streifen.firstOrNull { it.rechteck.contains(p) }?.let { return it.zoneIndex }

    return geometrie.flaechen.firstOrNull { it.flaeche.contains(p) }?.zoneIndex
}

/**
 * Liegt der Punkt im Eckbereich dieser Ecke? Das ist das Quadrat zwischen der
 * Bowl-Ecke und dem Umlauf (Viertelkreis-Sektor plus L-förmiger Restbereich).
 */
private fun inEckbereich(p: Offset, geometrie: PlanGeometrie, eck: EckGeometrie): Boolean {
    val a = geometrie.aussen
    val i = geometrie.innen
    val left = if (eck.sx > 0f) a.left else i.right
    val right = if (eck.sx > 0f) i.left else a.right
    val top = if (eck.sy > 0f) a.top else i.bottom
    val bottom = if (eck.sy > 0f) i.top else a.bottom
    return p.x >= left && p.x <= right && p.y >= top && p.y <= bottom
}

/**
 * Enthalten-Test für ein abgerundetes Rechteck (z. B. den Bowl-Außenrand).
 * Punkte im „weggeschnittenen" Bereich der Eckenrundung gelten als außerhalb.
 */
private fun inAbgerundetemRechteck(p: Offset, rechteck: Rect, radius: Float): Boolean {
    if (radius <= 0f) return rechteck.contains(p)
    if (p.x < rechteck.left || p.x > rechteck.right) return false
    if (p.y < rechteck.top || p.y > rechteck.bottom) return false
    val cx = p.x.coerceIn(rechteck.left + radius, rechteck.right - radius)
    val cy = p.y.coerceIn(rechteck.top + radius, rechteck.bottom - radius)
    val dx = p.x - cx
    val dy = p.y - cy
    return dx * dx + dy * dy <= radius * radius
}