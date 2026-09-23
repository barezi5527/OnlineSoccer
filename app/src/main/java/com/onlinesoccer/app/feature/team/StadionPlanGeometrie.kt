package com.onlinesoccer.app.feature.team

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.onlinesoccer.app.data.model.StadionLage
import com.onlinesoccer.app.data.model.StadionPlanDaten
import com.onlinesoccer.app.data.model.StadionPlanLogik
import com.onlinesoccer.app.data.model.StadionPlanZone
import kotlin.math.max
import kotlin.math.min

/** Ausrichtung eines Bereichs im Plan (abhängig von der Stadionseite). */
internal enum class ZoneAusrichtung {
    /** Waagerechter Bereich (Nord oben / Süd unten): Sitz-/Stehband übereinander. */
    WAAGERECHT,
    /** Senkrechter Bereich (West links / Ost rechts): Bänder nebeneinander. */
    SENKRECHT,
}

/** Fläche eines darzustellenden Seitenbereichs samt zugehöriger Zone. */
internal data class ZoneFlaeche(
    val zoneIndex: Int,
    val lage: StadionLage,
    val flaeche: Rect,
)

/**
 * Streifen eines Sonderbereichs, der kein eigenes Seitenband belegt (VIP):
 * ein dünner Abschnitt an der äußeren Kante der Haupttribüne derselben Seite.
 */
internal data class ZoneStreifen(
    val zoneIndex: Int,
    val rechteck: Rect,
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
): PlanGeometrie {
    val basis = min(breite, hoehe)
    val kapazitaetsFaktor = StadionPlanLogik.kapazitaetsFaktor(plan.kapazitaet)

    // Kleine Stadien belegen weniger Fläche (mehr Außenfläche), große füllen den Bowl.
    val aussenRand = basis * 0.02f + basis * 0.10f * (1f - kapazitaetsFaktor)
    val ringTiefe = basis * (0.085f + 0.10f * kapazitaetsFaktor)
    val eckRadius = basis * (0.035f + 0.055f * kapazitaetsFaktor)
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
        val bandZonen = gruppe.filterNot { it.vipStreifen }
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

    // VIP-Streifen: oberste zwei Reihen der Haupttribüne (äußere Kante des
    // ersten Bereichs derselben Seite, bei West die linke Kante). Die Tiefe
    // wird an das Sitzband der Tribüne geklemmt, damit der Streifen nie über
    // das Stehband oder den Rand hinaus zeichnet.
    val streifen = buildList {
        zonen.forEach { zone ->
            if (!zone.vipStreifen) return@forEach
            var owner = zonen.indexOfFirst { it.lage == zone.lage && it.name == "Haupttribüne" }
            if (owner < 0) {
                owner = zonen.indexOfFirst { it.lage == zone.lage && !it.vipStreifen }
            }
            val sektor = sektorJeZone[owner] ?: return@forEach
            val haengend = ausrichtung(zone.lage) == ZoneAusrichtung.SENKRECHT
            val tiefe = if (haengend) sektor.width else sektor.height
            if (tiefe <= 0f) return@forEach
            val ownerZone = zonen.getOrNull(owner)
            val (sitzT, _) = if (ownerZone != null) {
                StadionPlanLogik.bandTiefen(ownerZone.sitzAnteil, ownerZone.stehAnteil, tiefe)
            } else {
                tiefe to 0f
            }
            val streifenTiefe = min(tiefe * 0.14f, sitzT.coerceAtLeast(0f))
            if (streifenTiefe <= 0f) return@forEach
            val rechteck: Rect = when (zone.lage) {
                StadionLage.WEST -> Rect(sektor.left, sektor.top, sektor.left + streifenTiefe, sektor.bottom)
                StadionLage.OST -> Rect(sektor.right - streifenTiefe, sektor.top, sektor.right, sektor.bottom)
                StadionLage.NORD -> Rect(sektor.left, sektor.top, sektor.right, sektor.top + streifenTiefe)
                StadionLage.SUED -> Rect(sektor.left, sektor.bottom - streifenTiefe, sektor.right, sektor.bottom)
            }
            add(ZoneStreifen(zoneIndex = zonen.indexOfFirst { it === zone }, rechteck = rechteck))
        }
    }

    val kurven = zonen.filter { it.istKurve }.mapNotNull { zone ->
        val lage = zone.lage
        val ecken = when (lage) {
            StadionLage.NORD -> listOf(
                EckGeometrie(Offset(aussen.left + eckRadius, aussen.top + eckRadius), 1f, 1f, 180f),
                EckGeometrie(Offset(aussen.right - eckRadius, aussen.top + eckRadius), -1f, 1f, 270f),
            )
            StadionLage.SUED -> listOf(
                EckGeometrie(Offset(aussen.left + eckRadius, aussen.bottom - eckRadius), 1f, -1f, 90f),
                EckGeometrie(Offset(aussen.right - eckRadius, aussen.bottom - eckRadius), -1f, -1f, 0f),
            )
            else -> emptyList()
        }
        if (ecken.isEmpty()) return@mapNotNull null
        KurvenGeometrie(
            zoneIndex = zonen.indexOfFirst { it === zone },
            lage = lage!!,
            ecken = ecken,
        )
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
    )
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