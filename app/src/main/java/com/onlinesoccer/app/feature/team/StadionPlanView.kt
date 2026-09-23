package com.onlinesoccer.app.feature.team

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlinesoccer.app.data.model.PlatzKategorie
import com.onlinesoccer.app.data.model.PlatzTyp
import com.onlinesoccer.app.data.model.StadionLage
import com.onlinesoccer.app.data.model.StadionPlanDaten
import com.onlinesoccer.app.data.model.StadionPlanLogik
import com.onlinesoccer.app.data.model.StadionPlanZone
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Farbwelt des Plans: harmoniert mit dem grünen App-Design. Sitz (Blau) und
// Steh (Amber) sind auch für Farbsehschwächen klar unterscheidbar; zusätzlich
// unterscheiden sich die Bereiche durch Textur (Sitz: gestrichelt, Steh: durchgehend).
private val hintergrundFarbe = Color(0xFF132A17)
private val umlaufFarbe = Color(0xFF2E7D32)
private val rasenHell = Color(0xFF43A047)
private val rasenDunkel = Color(0xFF388E3C)
private val linieFarbe = Color(0xFFF1F8E9)
private val sitzFarbe = Color(0xFF1565C0)
private val sitzZeile = Color(0xFF5C9ADB)
private val vipFarbe = Color(0xFF7B1FA2)
private val stehFarbe = Color(0xFFE07B1E)
private val stehZeile = Color(0xFFF2AB54)
private val dachFarbe = Color(0xFF0B1D10)
private val basisFarbe = Color(0xFF24382A)
private val lueckeFarbe = Color(0xFF1C311F)
private val bruestungFarbe = Color(0xFF08210E)
private val markierungFarbe = Color(0xFF9EF0B5)
private val pilleHintergrund = Color(0xF0142418)
private val pilleText = Color(0xFFF2F7F2)
private val bowlKanteFarbe = Color(0xFF08210E)
// Dezente Ausgrauung nicht relevanter Bereiche bei aktiver Kategorie-Auswahl.
private val ausgegrautFarbe = Color(0xFF7E8983)
private val kurveGesamtFarbe = Color(0xFF5C6F66)

/** Auswahlzustand unterhalb des Plans: eine Kategorie oder ein angetippter Bereich. */
private sealed interface PlanAuswahl {
    data class Kategorie(val kategorie: PlatzKategorie) : PlanAuswahl
    data class Zone(val index: Int) : PlanAuswahl
}

/**
 * Dynamischer Stadionplan (Draufsicht) für den Bereich „Stadionausbau".
 * Liefert die Datenquelle eine Aufteilung nach Tribünen, erscheinen echte
 * Bereiche mit ihren Kapazitäten; sonst zeigt der Plan die Gesamtwerte als
 * geschätzte Bundesliga-Tribünen (Haupt-/Gegentribüne, Kurven inkl. ihrer
 * Eckbereiche, Gästeblock in der Südkurve, VIP als oberste zwei Reihen der
 * Haupttribüne, barrierefreie Plätze als eigener Bereich) an. Sitz- und
 * Stehplätze sowie die einzelnen Tribünen lassen sich über die Legende
 * auswählen. Kurven und Seiten bilden einen geschlossenen Stadionring.
 */
@Composable
fun StadionPlanView(plan: StadionPlanDaten, modifier: Modifier = Modifier) {
    val zonen = remember(plan) { StadionPlanLogik.zonen(plan) }
    val grad = remember(plan) { StadionPlanLogik.detaillierungsGrad(plan.kapazitaet) }
    val kategorien = remember(plan, grad) { StadionPlanLogik.kategorien(plan, grad) }
    val textMeasurer = rememberTextMeasurer()
    var auswahl by remember { mutableStateOf<PlanAuswahl?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StadionPlanCanvas(
                plan = plan,
                zonen = zonen,
                textMeasurer = textMeasurer,
                auswahl = auswahl,
                onSelect = { auswahl = it },
            )

            KategorieAuswahl(
                kategorien = kategorien,
                ausgewaehlt = (auswahl as? PlanAuswahl.Kategorie)?.kategorie,
                onSelect = { kategorie ->
                    auswahl = if ((auswahl as? PlanAuswahl.Kategorie)?.kategorie == kategorie) {
                        null
                    } else {
                        PlanAuswahl.Kategorie(kategorie)
                    }
                },
            )

            when (val aktuell = auswahl) {
                null -> Text(
                    "Tippe auf eine Kategorie oder einen Bereich für Details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is PlanAuswahl.Kategorie -> KategorieInfo(
                    kategorie = aktuell.kategorie,
                    onClose = { auswahl = null },
                )
                is PlanAuswahl.Zone -> zonen.getOrNull(aktuell.index)?.let { zone ->
                    BereichDetails(
                        zone = zone,
                        plan = plan,
                        onClose = { auswahl = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun StadionPlanCanvas(
    plan: StadionPlanDaten,
    zonen: List<StadionPlanZone>,
    textMeasurer: TextMeasurer,
    auswahl: PlanAuswahl?,
    onSelect: (PlanAuswahl?) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Hochkant auf dem Smartphone; auf breiten Displays größer, aber proportional.
        val hoeheMax = if (maxWidth >= 600.dp) 760.dp else 560.dp
        val hoehe = minOf(maxWidth / PlanSeitenverhaeltnis, hoeheMax)
        val breite = minOf(maxWidth, hoehe * PlanSeitenverhaeltnis)

        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val breitePx = with(density) { breite.toPx() }
        val hoehePx = with(density) { hoehe.toPx() }
        val geometrie = remember(plan, zonen, breitePx, hoehePx) {
            berechneGeometrie(breitePx, hoehePx, plan, zonen)
        }
        val auswahlAktuell = rememberUpdatedState(auswahl)
        val onSelectAktuell = rememberUpdatedState(onSelect)

        // Der gesamte Plan wird einmalig in eine Bitmap gerendert (nur bei
        // Daten-/Auswahlwechsel neu). Beim Scrollen wird pro Frame nur noch die
        // fertige Bitmap wiedergegeben – statt hunderter Zeichenaufrufe.
        val planBild = remember(plan, zonen, geometrie, breitePx, hoehePx, layoutDirection, density, auswahl) {
            val breite = breitePx.roundToInt().coerceAtLeast(1)
            val hoehe = hoehePx.roundToInt().coerceAtLeast(1)
            val bitmap = ImageBitmap(breite, hoehe)
            androidx.compose.ui.graphics.Canvas(bitmap).let { canvas ->
                CanvasDrawScope().draw(density, layoutDirection, canvas, Size(breitePx, hoehePx)) {
                    val filter = (auswahl as? PlanAuswahl.Kategorie)?.kategorie
                    val gewaehlteZone = (auswahl as? PlanAuswahl.Zone)?.index
                    zeichneStadionPlan(geometrie, plan, zonen, gewaehlteZone, filter, textMeasurer)
                }
            }
            bitmap
        }

        Box(Modifier.align(Alignment.Center).size(breite, hoehe)) {
            Canvas(
                Modifier.fillMaxSize()
                    .pointerInput(plan, zonen, geometrie) {
                        detectTapGestures { tap ->
                            val index = zoneAnPunkt(tap, geometrie)
                            val aktuell = auswahlAktuell.value
                            val neu: PlanAuswahl? = if (index == null) {
                                null
                            } else {
                                if ((aktuell as? PlanAuswahl.Zone)?.index == index) null
                                else PlanAuswahl.Zone(index)
                            }
                            onSelectAktuell.value(neu)
                        }
                    },
            ) {
                drawImage(planBild, topLeft = Offset.Zero)
            }
        }
    }
}

private val PlanSeitenverhaeltnis = 0.72f

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KategorieAuswahl(
    kategorien: List<PlatzKategorie>,
    ausgewaehlt: PlatzKategorie?,
    onSelect: (PlatzKategorie) -> Unit,
) {
    if (kategorien.isEmpty()) return
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        kategorien.forEach { kategorie ->
            val aktiv = kategorie == ausgewaehlt
            FilterChip(
                selected = aktiv,
                onClick = { onSelect(kategorie) },
                label = { Text(kategorie.anzeigeName, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { KategorieSwatch(kategorie) },
            )
        }
    }
}

@Composable
private fun KategorieSwatch(kategorie: PlatzKategorie) {
    val farbe = when {
        kategorie.zoneName == "VIP- und Business-Bereich" -> vipFarbe
        kategorie.typ == PlatzTyp.SITZ -> sitzFarbe
        kategorie.typ == PlatzTyp.STEH -> stehFarbe
        else -> kurveGesamtFarbe
    }
    Box(Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(farbe)) {
        if (kategorie.ueberdacht) {
            Canvas(Modifier.fillMaxSize()) {
                val s = size.height
                drawLine(dachFarbe, Offset(0f, s), Offset(s, 0f), strokeWidth = 1.5.dp.toPx())
                drawLine(dachFarbe, Offset(0f, s / 2f), Offset(s / 2f, 0f), strokeWidth = 1.5.dp.toPx())
                drawLine(dachFarbe, Offset(s / 2f, s), Offset(s, 0f), strokeWidth = 1.5.dp.toPx())
            }
        }
    }
}

/** Kompakte Informationskarte für eine ausgewählte Platzkategorie. */
@Composable
private fun KategorieInfo(
    kategorie: PlatzKategorie,
    onClose: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    kategorie.anzeigeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClose) {
                    Text("Schließen", style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                "${formatAnzahl(kategorie.plaetze)} Plätze",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                kategorie.beschreibung,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            InfoZeile("Überdacht", formatAnzahl(kategorie.davonUeberdacht))
            InfoZeile("Nicht überdacht", formatAnzahl(kategorie.nichtUeberdacht))
        }
    }
}

@Composable
private fun InfoZeile(label: String, wert: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(0.6f),
        )
        Text(
            wert,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(0.4f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun BereichDetails(
    zone: StadionPlanZone,
    plan: StadionPlanDaten,
    onClose: () -> Unit,
) {
    val (titel, hinweis, zeilen) = remember(zone, plan) { detailInhalt(zone, plan) }
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClose) {
                    Text("Schließen", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (hinweis.isNotBlank()) {
                Text(
                    hinweis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.fillMaxWidth().sizeIn(maxHeight = 72.dp),
                )
            }
            zeilen.forEach { (label, wert) ->
                InfoZeile(label, wert)
            }
        }
    }
}

/**
 * Inhalt des Detailfelds: Bei echter Bereichsaufteilung die realen Werte der
 * Zone; in der aggregierten Ansicht die geschätzte Tribüne samt Hinweis.
 */
internal fun detailInhalt(
    zone: StadionPlanZone,
    plan: StadionPlanDaten,
): Triple<String, String, List<Pair<String, String>>> {
    val echteBereiche = StadionPlanLogik.nutztEchteBereiche(plan)
    if (zone.hatAufteilung) {
        val zeilen = mutableListOf<Pair<String, String>>()
        zone.sitzplaetze?.let { zeilen += "Sitzplätze" to formatAnzahl(it) }
        zone.stehplaetze?.let { zeilen += "Stehplätze" to formatAnzahl(it) }
        val ueberdacht = (zone.sitzUeberdacht ?: 0) + (zone.stehUeberdacht ?: 0)
        if (ueberdacht > 0) zeilen += "Überdacht" to formatAnzahl(ueberdacht)
        zeilen += "Kapazität" to formatAnzahl(zone.kapazitaet ?: 0)
        val hinweis = if (echteBereiche) {
            ""
        } else {
            "Platzverteilung geschätzt nach typischen Bundesliga-Stadien (u. a. FC Bayern, " +
                "Borussia Dortmund, VfB Stuttgart) – Grundlage sind die Gesamtwerte deines Stadions."
        }
        return Triple(zone.name, hinweis, zeilen)
    }
    // Fallback (z. B. künftige Daten ohne Platzaufteilung): Gesamtwerte des Stadions.
    val zeilen = buildList {
        add("Sitzplätze" to formatAnzahl(plan.sitzplaetze))
        add("Stehplätze" to formatAnzahl(plan.stehplaetze))
        val ueberdacht = plan.sitzUeberdacht + plan.stehUeberdacht
        if (ueberdacht > 0) add("Überdacht" to formatAnzahl(ueberdacht))
        add("Fassungsvermögen" to formatAnzahl(plan.fassungsvermoegen ?: plan.kapazitaet))
    }
    val hinweis = if (echteBereiche) {
        "Die Datenquelle liefert für diesen Bereich keine getrennte Platzaufteilung. " +
            "Angezeigt werden die Gesamtwerte des Stadions."
    } else {
        "Für diesen Bereich ist keine Platzaufteilung hinterlegt – angezeigt werden die " +
            "Gesamtwerte des Stadions."
    }
    return Triple("${zone.name} · Gesamtwerte", hinweis, zeilen)
}

private val anzahlFormat = NumberFormat.getIntegerInstance(Locale.GERMANY)

private fun formatAnzahl(n: Int): String = anzahlFormat.format(n)

// ==================== Zeichnung ====================

/** Zustand eines Sitz-/Stehbandes bei aktiver Kategorie-Auswahl. */
private enum class BandZustand {
    /** Band wird normal dargestellt (zur Kategorie gehörend bzw. keine Auswahl). */
    VOLL,
    /** Nur der überdachte Teil des Bandes gehört zur Kategorie. */
    UEBERDACHT,
    /** Band gehört nicht zur Kategorie und wird ausgegraut. */
    GRAU,
}

/** Gehört die Zone zur gewählten (physischen oder stadionweiten) Kategorie? */
private fun zoneGehoertZu(filter: PlatzKategorie, zone: StadionPlanZone): Boolean {
    val lage = filter.lage ?: return true // stadionweite Kategorie (nur nach Typ)
    if (zone.lage != lage) return false
    val zoneName = filter.zoneName ?: return false
    return zone.name == zoneName
}

private fun bandZustand(
    filter: PlatzKategorie?,
    zone: StadionPlanZone,
    typ: PlatzTyp,
    ueberdachtAnteil: Float,
): BandZustand {
    if (filter == null) return BandZustand.VOLL
    if (filter.lage != null) {
        // Physische Tribüne: trifft die ganze Zone (beide Bänder) oder nichts.
        if (!zoneGehoertZu(filter, zone)) return BandZustand.GRAU
        return BandZustand.VOLL
    }
    // Stadionweite Kategorie (Sitz-/Stehplätze): wirkt je Bandtyp.
    val typPasst = filter.typ == null || filter.typ == typ
    if (!typPasst) return BandZustand.GRAU
    if (!filter.ueberdacht) return BandZustand.VOLL
    if (ueberdachtAnteil > 0f) return BandZustand.UEBERDACHT
    return BandZustand.GRAU
}

private fun DrawScope.zeichneStadionPlan(
    geometrie: PlanGeometrie,
    plan: StadionPlanDaten,
    zonen: List<StadionPlanZone>,
    gewaehlteZone: Int?,
    filter: PlatzKategorie?,
    textMeasurer: TextMeasurer,
) {
    val basis = min(size.width, size.height)
    if (basis <= 0f) return

    drawRect(hintergrundFarbe)

    val aussenPath = Path().apply {
        addRoundRect(
            RoundRect(
                geometrie.aussen.left,
                geometrie.aussen.top,
                geometrie.aussen.right,
                geometrie.aussen.bottom,
                CornerRadius(geometrie.eckRadius),
            ),
        )
    }

    val hatPlaetze = plan.kapazitaet > 0

    clipPath(aussenPath) {
        // Grundfläche des Rings.
        drawRect(basisFarbe, topLeft = geometrie.aussen.topLeft, size = geometrie.aussen.size)

        if (hatPlaetze) {
            // Ecken der Kurven zuerst, dann die geraden Seitenblöcke.
            geometrie.kurven.forEach { kg ->
                if (kg.zoneIndex in zonen.indices) {
                    zeichneKurvenBereich(kg, zonen[kg.zoneIndex], geometrie, basis, filter)
                }
            }
            geometrie.flaechen.forEach { zf ->
                if (zf.zoneIndex in zonen.indices) {
                    zeichneBereich(zf, zonen[zf.zoneIndex], geometrie, basis, filter, textMeasurer)
                }
            }

            // VIP-Streifen über der äußeren Kante der Haupttribüne.
            geometrie.streifen.forEach { sf ->
                if (sf.zoneIndex in zonen.indices) {
                    zeichneVipStreifen(sf, zonen[sf.zoneIndex], basis, filter)
                }
            }

            gewaehlteZone?.let { index ->
                zeichneAuswahlHervorhebung(index, geometrie)
            }
        }
    }

    // Umlauf (Innenraum) als abgerundetes Loch über dem Ring.
    drawRoundRect(
        umlaufFarbe,
        topLeft = geometrie.innen.topLeft,
        size = geometrie.innen.size,
        cornerRadius = CornerRadius(geometrie.innenRadius),
    )
    drawRoundRect(
        bruestungFarbe,
        topLeft = geometrie.innen.topLeft,
        size = geometrie.innen.size,
        cornerRadius = CornerRadius(geometrie.innenRadius),
        style = Stroke(width = basis * 0.006f),
    )

    zeichneSpielfeld(geometrie.pitch, basis)

    // Äußere Bowl-Kante.
    drawRoundRect(
        bowlKanteFarbe,
        topLeft = geometrie.aussen.topLeft,
        size = geometrie.aussen.size,
        cornerRadius = CornerRadius(geometrie.eckRadius),
        style = Stroke(width = basis * 0.008f),
    )
}

private fun DrawScope.zeichneAuswahlHervorhebung(index: Int, geometrie: PlanGeometrie) {
    geometrie.kurven.firstOrNull { it.zoneIndex == index }?.let { kg ->
        kg.ecken.forEach { eck ->
            drawPath(eckElbowPath(geometrie, eck), markierungFarbe.copy(alpha = 0.10f))
            val path = annulusPath(eck.mitte, geometrie.innenRadius, geometrie.eckRadius, eck.startWinkel, 90f)
            drawPath(path, markierungFarbe.copy(alpha = 0.14f))
            val mittelR = (geometrie.innenRadius + geometrie.eckRadius) / 2f
            drawArc(
                markierungFarbe,
                eck.startWinkel,
                90f,
                useCenter = false,
                topLeft = Offset(eck.mitte.x - mittelR, eck.mitte.y - mittelR),
                size = Size(mittelR * 2f, mittelR * 2f),
                style = Stroke(width = min(size.width, size.height) * 0.007f),
            )
        }
    }
    geometrie.streifen.firstOrNull { it.zoneIndex == index }?.let { sf ->
        drawRect(
            markierungFarbe.copy(alpha = 0.30f),
            topLeft = sf.rechteck.topLeft,
            size = sf.rechteck.size,
        )
        drawRect(
            markierungFarbe,
            topLeft = sf.rechteck.topLeft,
            size = sf.rechteck.size,
            style = Stroke(width = min(size.width, size.height) * 0.007f),
        )
    }

    geometrie.flaechen.firstOrNull { it.zoneIndex == index }?.let { zf ->
        drawRect(
            markierungFarbe.copy(alpha = 0.12f),
            topLeft = zf.flaeche.topLeft,
            size = zf.flaeche.size,
        )
        drawRect(
            markierungFarbe,
            topLeft = zf.flaeche.topLeft,
            size = zf.flaeche.size,
            style = Stroke(width = min(size.width, size.height) * 0.007f),
        )
    }
}

/** Ein Bereichsband (Sitz außen / Steh innen) einer geraden Stadionseite. */
private fun DrawScope.zeichneBereich(
    zf: ZoneFlaeche,
    zone: StadionPlanZone,
    geometrie: PlanGeometrie,
    basis: Float,
    filter: PlatzKategorie?,
    textMeasurer: TextMeasurer,
) {
    val rechteck = zf.flaeche
    val haengend = zf.lage == StadionLage.WEST || zf.lage == StadionLage.OST
    val tiefe = if (haengend) rechteck.width else rechteck.height
    if (tiefe <= 0f) return

    val (sitzT, stehT) = StadionPlanLogik.bandTiefen(zone.sitzAnteil, zone.stehAnteil, tiefe)

    if (sitzT > 0f) {
        val sitzRect = bandRect(rechteck, sitzT, zf.lage, aussenBand = true)
        val zustand = bandZustand(filter, zone, PlatzTyp.SITZ, zone.sitzUeberdachtAnteil)
        zeichneRechteckBand(sitzRect, zf.lage, PlatzTyp.SITZ, zone.sitzUeberdachtAnteil, basis, zustand)
    }
    if (stehT > 0f) {
        val stehRect = bandRect(rechteck, stehT, zf.lage, aussenBand = false)
        val zustand = bandZustand(filter, zone, PlatzTyp.STEH, zone.stehUeberdachtAnteil)
        zeichneRechteckBand(stehRect, zf.lage, PlatzTyp.STEH, zone.stehUeberdachtAnteil, basis, zustand)
    }

    if (sitzT > 0f && stehT > 0f) {
        // Wand zwischen den Bändern.
        val trennung = bandGrenze(rechteck, zf.lage, sitzT)
        drawLine(
            lerp(sitzFarbe, Color.Black, 0.35f),
            trennung.first,
            trennung.second,
            strokeWidth = 1.5.dp.toPx(),
        )
    }

    zeichneGänge(rechteck, zf.lage, basis)
    zeichneBeschriftung(rechteck, zone, zf.lage, basis, textMeasurer)
}

/** Zeichnet ein rechteckiges Sitz-/Stehband mit Reihen, Überdachung und Filterzustand. */
private fun DrawScope.zeichneRechteckBand(
    band: Rect,
    lage: StadionLage,
    typ: PlatzTyp,
    ueberdachtAnteil: Float,
    basis: Float,
    zustand: BandZustand,
) {
    val farbe = if (typ == PlatzTyp.SITZ) sitzFarbe else stehFarbe
    val zeileFarbe = if (typ == PlatzTyp.SITZ) sitzZeile else stehZeile
    val gestrichelt = typ == PlatzTyp.SITZ
    val tiefe = if (lage == StadionLage.NORD || lage == StadionLage.SUED) band.height else band.width

    if (zustand == BandZustand.GRAU) {
        drawRect(farbe.copy(alpha = 0.15f), topLeft = band.topLeft, size = band.size)
        return
    }

    drawRect(farbe, topLeft = band.topLeft, size = band.size)
    zeichneReihen(band, lage, ueberdachtAnteil, zeileFarbe, gestrichelt, basis)

    if (zustand == BandZustand.UEBERDACHT && ueberdachtAnteil > 0f) {
        val decke = tiefe * ueberdachtAnteil.coerceIn(0f, 1f)
        val rest = bandOhneUeberdachung(band, lage, decke)
        drawRect(ausgegrautFarbe, topLeft = rest.topLeft, size = rest.size)
    }
}

/** Der nicht überdachte Rest eines Bandes (Gesamtband abzüglich der Dachfläche). */
private fun bandOhneUeberdachung(band: Rect, lage: StadionLage, decke: Float): Rect = when (lage) {
    StadionLage.NORD -> Rect(band.left, band.top + decke, band.right, band.bottom)
    StadionLage.SUED -> Rect(band.left, band.top, band.right, band.bottom - decke)
    StadionLage.WEST -> Rect(band.left + decke, band.top, band.right, band.bottom)
    StadionLage.OST -> Rect(band.left, band.top, band.right - decke, band.bottom)
}

/**
 * Zeichnet den VIP-Bereich als dünnen Streifen auf den obersten zwei Reihen
 * der Haupttribüne (äußere Kante), mit Reihenlinien, Dachschraffur und
 * Filterzustand.
 */
private fun DrawScope.zeichneVipStreifen(
    sf: ZoneStreifen,
    zone: StadionPlanZone,
    basis: Float,
    filter: PlatzKategorie?,
) {
    val band = sf.rechteck
    val waagerecht = zone.lage == StadionLage.NORD || zone.lage == StadionLage.SUED
    if ((if (waagerecht) band.height else band.width) <= 0f) return

    val zustand = bandZustand(filter, zone, PlatzTyp.SITZ, zone.sitzUeberdachtAnteil)
    if (zustand == BandZustand.GRAU) {
        drawRect(vipFarbe.copy(alpha = 0.15f), topLeft = band.topLeft, size = band.size)
        return
    }

    drawRect(vipFarbe, topLeft = band.topLeft, size = band.size)

    val laenge = if (waagerecht) band.width else band.height
    if (laenge > 0f) {
        val anzahl = ((laenge / (basis * 0.022f)).roundToInt()).coerceIn(2, 8)
        val dash = PathEffect.dashPathEffect(floatArrayOf(basis * 0.012f, basis * 0.008f))
        for (i in 1 until anzahl) {
            val pos = if (waagerecht) band.top + band.height * i / anzahl
            else band.left + band.width * i / anzahl
            val start = if (waagerecht) Offset(band.left, pos) else Offset(pos, band.top)
            val ende = if (waagerecht) Offset(band.right, pos) else Offset(pos, band.bottom)
            drawLine(
                Color.White.copy(alpha = 0.30f),
                start,
                ende,
                strokeWidth = 1.dp.toPx(),
                pathEffect = dash,
            )
        }
    }

    if (zone.sitzUeberdachtAnteil > 0f) {
        clipRect(band.left, band.top, band.right, band.bottom) {
            val schritt = basis * 0.02f
            var x = band.left - band.height
            while (x <= band.right) {
                drawLine(
                    dachFarbe.copy(alpha = 0.45f),
                    Offset(x, band.bottom),
                    Offset(x + band.height, band.top),
                    strokeWidth = 1.2.dp.toPx(),
                )
                x += schritt
            }
        }
    }
}

/** Rechteck eines Bandes innerhalb eines Bereichs (Sitz außen / Steh innen). */
private fun bandRect(rechteck: Rect, tiefe: Float, lage: StadionLage, aussenBand: Boolean): Rect =
    when (lage) {
        StadionLage.NORD ->
            if (aussenBand) {
                Rect(rechteck.left, rechteck.top, rechteck.right, rechteck.top + tiefe)
            } else {
                Rect(rechteck.left, rechteck.bottom - tiefe, rechteck.right, rechteck.bottom)
            }
        StadionLage.SUED ->
            if (aussenBand) {
                Rect(rechteck.left, rechteck.bottom - tiefe, rechteck.right, rechteck.bottom)
            } else {
                Rect(rechteck.left, rechteck.top, rechteck.right, rechteck.top + tiefe)
            }
        StadionLage.WEST ->
            if (aussenBand) {
                Rect(rechteck.left, rechteck.top, rechteck.left + tiefe, rechteck.bottom)
            } else {
                Rect(rechteck.right - tiefe, rechteck.top, rechteck.right, rechteck.bottom)
            }
        StadionLage.OST ->
            if (aussenBand) {
                Rect(rechteck.right - tiefe, rechteck.top, rechteck.right, rechteck.bottom)
            } else {
                Rect(rechteck.left, rechteck.top, rechteck.left + tiefe, rechteck.bottom)
            }
    }

/** Die Grenzlinie (Start/Ende) zwischen den beiden Bändern eines Bereichs. */
private fun bandGrenze(rechteck: Rect, lage: StadionLage, sitzTiefe: Float): Pair<Offset, Offset> =
    when (lage) {
        StadionLage.NORD -> Offset(rechteck.left, rechteck.top + sitzTiefe) to
            Offset(rechteck.right, rechteck.top + sitzTiefe)
        StadionLage.SUED -> Offset(rechteck.left, rechteck.bottom - sitzTiefe) to
            Offset(rechteck.right, rechteck.bottom - sitzTiefe)
        StadionLage.WEST -> Offset(rechteck.left + sitzTiefe, rechteck.top) to
            Offset(rechteck.left + sitzTiefe, rechteck.bottom)
        StadionLage.OST -> Offset(rechteck.right - sitzTiefe, rechteck.top) to
            Offset(rechteck.right - sitzTiefe, rechteck.bottom)
    }

/** Reihen (Sitz gestrichelt / Steh durchgehend) und Überdachungs-Schraffur. */
private fun DrawScope.zeichneReihen(
    band: Rect,
    lage: StadionLage,
    ueberdachtAnteil: Float,
    zeileFarbe: Color,
    gestrichelt: Boolean,
    basis: Float,
) {
    val waagerecht = lage == StadionLage.NORD || lage == StadionLage.SUED
    val tiefe = if (waagerecht) band.height else band.width
    if (tiefe <= 0f) return
    val anzahl = ((tiefe / (basis * 0.022f)).roundToInt()).coerceIn(2, 8)

    val dash = if (gestrichelt) {
        PathEffect.dashPathEffect(floatArrayOf(basis * 0.012f, basis * 0.008f))
    } else {
        null
    }
    for (i in 1 until anzahl) {
        val pos = if (waagerecht) band.top + band.height * i / anzahl
        else band.left + band.width * i / anzahl
        val start = if (waagerecht) Offset(band.left, pos) else Offset(pos, band.top)
        val ende = if (waagerecht) Offset(band.right, pos) else Offset(pos, band.bottom)
        drawLine(
            zeileFarbe.copy(alpha = 0.55f),
            start,
            ende,
            strokeWidth = 1.dp.toPx(),
            pathEffect = dash,
        )
    }

    if (ueberdachtAnteil > 0f) {
        val decke = tiefe * ueberdachtAnteil.coerceIn(0f, 1f)
        val abdeckung = when (lage) {
            StadionLage.NORD -> Rect(band.left, band.top, band.right, band.top + decke)
            StadionLage.SUED -> Rect(band.left, band.bottom - decke, band.right, band.bottom)
            StadionLage.WEST -> Rect(band.left, band.top, band.left + decke, band.bottom)
            StadionLage.OST -> Rect(band.right - decke, band.top, band.right, band.bottom)
        }
        clipRect(abdeckung.left, abdeckung.top, abdeckung.right, abdeckung.bottom) {
            val schritt = basis * 0.02f
            var x = abdeckung.left - abdeckung.height
            while (x <= abdeckung.right) {
                drawLine(
                    dachFarbe.copy(alpha = 0.45f),
                    Offset(x, abdeckung.bottom),
                    Offset(x + abdeckung.height, abdeckung.top),
                    strokeWidth = 1.2.dp.toPx(),
                )
                x += schritt
            }
        }
        // Vorderkante des Dachs.
        when (lage) {
            StadionLage.NORD -> drawLine(
                dachFarbe.copy(alpha = 0.7f),
                Offset(band.left, band.top + decke),
                Offset(band.right, band.top + decke),
                strokeWidth = 2.dp.toPx(),
            )
            StadionLage.SUED -> drawLine(
                dachFarbe.copy(alpha = 0.7f),
                Offset(band.left, band.bottom - decke),
                Offset(band.right, band.bottom - decke),
                strokeWidth = 2.dp.toPx(),
            )
            StadionLage.WEST -> drawLine(
                dachFarbe.copy(alpha = 0.7f),
                Offset(band.left + decke, band.top),
                Offset(band.left + decke, band.bottom),
                strokeWidth = 2.dp.toPx(),
            )
            StadionLage.OST -> drawLine(
                dachFarbe.copy(alpha = 0.7f),
                Offset(band.right - decke, band.top),
                Offset(band.right - decke, band.bottom),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

/** Gänge (Blocktrennung) entlang eines Bereichs. */
private fun DrawScope.zeichneGänge(rechteck: Rect, lage: StadionLage, basis: Float) {
    val waagerecht = lage == StadionLage.NORD || lage == StadionLage.SUED
    val laenge = if (waagerecht) rechteck.width else rechteck.height
    if (laenge <= 0f) return
    // Dicht an dicht gestapelte Blöcke zeigen nur wenige innere Gänge.
    val bloecke = ((laenge / (basis * 0.38f)).roundToInt()).coerceIn(1, 4)
    if (bloecke < 2) return
    val luecke = basis * 0.008f
    for (i in 1 until bloecke) {
        val pos = laenge * i / bloecke
        if (waagerecht) {
            drawRect(
                lueckeFarbe,
                topLeft = Offset(rechteck.left + pos - luecke / 2f, rechteck.top),
                size = Size(luecke, rechteck.height),
            )
        } else {
            drawRect(
                lueckeFarbe,
                topLeft = Offset(rechteck.left, rechteck.top + pos - luecke / 2f),
                size = Size(rechteck.width, luecke),
            )
        }
    }
}

/** Beschriftung (Bereichsname + Kapazität) als Pille auf dem Bereich. */
private fun DrawScope.zeichneBeschriftung(
    rechteck: Rect,
    zone: StadionPlanZone,
    lage: StadionLage,
    basis: Float,
    textMeasurer: TextMeasurer,
) {
    val fontSize = (basis * 0.030f).coerceAtLeast(8f).toSp()
    val kapazitaetFs = (basis * 0.024f).coerceAtLeast(7f).toSp()

    val nameLayout = textMeasurer.measure(
        zone.name,
        TextStyle(color = pilleText, fontSize = fontSize, fontWeight = FontWeight.SemiBold),
    )
    val kapazitaet = zone.kapazitaet
    val kapLayout = kapazitaet?.let {
        textMeasurer.measure(
            formatAnzahl(it),
            TextStyle(color = pilleText.copy(alpha = 0.85f), fontSize = kapazitaetFs, fontWeight = FontWeight.Medium),
        )
    }

    val textBreite = max(
        (nameLayout.size?.width ?: 0).toFloat(),
        (kapLayout?.size?.width ?: 0).toFloat(),
    )
    val textHoehe = (nameLayout.size?.height ?: 0).toFloat() + (kapLayout?.size?.height ?: 0).toFloat()
    val padX = basis * 0.020f
    val padY = basis * 0.012f
    val pillBreite = textBreite + 2f * padX
    val pillHoehe = textHoehe + 2f * padY

    val seitlich = lage == StadionLage.WEST || lage == StadionLage.OST
    if (!seitlich) {
        if (pillHoehe > rechteck.height - padY) return
        zeichnePille(rechteck.center, nameLayout, kapLayout, pillBreite, pillHoehe, rotation = 0f)
        return
    }
    when {
        pillBreite <= rechteck.width && pillHoehe <= rechteck.height - padY -> {
            zeichnePille(rechteck.center, nameLayout, kapLayout, pillBreite, pillHoehe, rotation = 0f)
        }
        pillBreite <= rechteck.height && pillHoehe <= rechteck.width - padY -> {
            val winkel = if (lage == StadionLage.WEST) -90f else 90f
            zeichnePille(rechteck.center, nameLayout, kapLayout, pillBreite, pillHoehe, rotation = winkel)
        }
    }
}

/**
 * Zeichnet die Eckbereiche einer Kurzseiten-Kurve (Südkurve/Nordkurve). Der
 * eigentliche Seitenabschnitt wird als gerades Band über [zeichneBereich]
 * gezeichnet; hier kommen die beiden Eckbögen des abgerundeten Rings hinzu.
 */
private fun DrawScope.zeichneKurvenBereich(
    kg: KurvenGeometrie,
    zone: StadionPlanZone,
    geometrie: PlanGeometrie,
    basis: Float,
    filter: PlatzKategorie?,
) {
    kg.ecken.forEach { eck ->
        zeichneKurvenEcke(eck, zone, geometrie, basis, filter)
    }
}

/**
 * Zeichnet eine Ecke des Rings als ringförmiges Segmentband des abgerundeten
 * Stadionbowls – Sitz außen, Steh innen – samt L-förmigem Restbereich.
 */
private fun DrawScope.zeichneKurvenEcke(
    eck: EckGeometrie,
    zone: StadionPlanZone,
    geometrie: PlanGeometrie,
    basis: Float,
    filter: PlatzKategorie?,
) {
    val aussenRadius = geometrie.eckRadius
    val innenRadius = geometrie.innenRadius
    val tiefe = aussenRadius - innenRadius
    if (tiefe <= 0f) return
    val (sitzT, stehT) = StadionPlanLogik.bandTiefen(zone.sitzAnteil, zone.stehAnteil, tiefe)

    if (sitzT > 0f) {
        val zustand = bandZustand(filter, zone, PlatzTyp.SITZ, zone.sitzUeberdachtAnteil)
        zeichneKurvenBand(
            eck.mitte, aussenRadius - sitzT, aussenRadius,
            eck.startWinkel, PlatzTyp.SITZ, zone.sitzUeberdachtAnteil, basis, zustand,
        )
    }
    if (stehT > 0f) {
        val zustand = bandZustand(filter, zone, PlatzTyp.STEH, zone.stehUeberdachtAnteil)
        zeichneKurvenBand(
            eck.mitte, innenRadius, aussenRadius - sitzT,
            eck.startWinkel, PlatzTyp.STEH, zone.stehUeberdachtAnteil, basis, zustand,
        )
    }

    if (sitzT > 0f && stehT > 0f) {
        drawArc(
            lerp(sitzFarbe, Color.Black, 0.35f),
            eck.startWinkel,
            90f,
            useCenter = false,
            topLeft = Offset(eck.mitte.x - (aussenRadius - sitzT), eck.mitte.y - (aussenRadius - sitzT)),
            size = Size((aussenRadius - sitzT) * 2f, (aussenRadius - sitzT) * 2f),
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }

    if (sitzT > 0f || stehT > 0f) {
        val typ = if (sitzT > 0f) PlatzTyp.SITZ else PlatzTyp.STEH
        val ueberdacht = if (typ == PlatzTyp.SITZ) zone.sitzUeberdachtAnteil else zone.stehUeberdachtAnteil
        zeichneEckElbow(geometrie, eck, zone, typ, ueberdacht, basis, filter)
    }
}

/**
 * Füllt den L-förmigen Eckbereich (Eck-Quadrat minus Kurven-Viertelkreis), der
 * den Stadionring an den Ecken schließt, mit der äußeren Bandfarbe der Zone.
 */
private fun DrawScope.zeichneEckElbow(
    geometrie: PlanGeometrie,
    eck: EckGeometrie,
    zone: StadionPlanZone,
    typ: PlatzTyp,
    ueberdachtAnteil: Float,
    basis: Float,
    filter: PlatzKategorie?,
) {
    val zustand = bandZustand(filter, zone, typ, ueberdachtAnteil)
    val farbe = if (typ == PlatzTyp.SITZ) sitzFarbe else stehFarbe
    val path = eckElbowPath(geometrie, eck)
    if (zustand == BandZustand.GRAU) {
        drawPath(path, farbe.copy(alpha = 0.15f))
        return
    }
    drawPath(path, farbe)

    if (ueberdachtAnteil > 0f) {
        val b = path.getBounds()
        clipPath(path) {
            val schritt = basis * 0.02f
            var x = b.left - b.height
            while (x <= b.right) {
                drawLine(
                    dachFarbe.copy(alpha = 0.45f),
                    Offset(x, b.bottom + b.height),
                    Offset(x + b.height * 2f, b.top - b.height),
                    strokeWidth = 1.2.dp.toPx(),
                )
                x += schritt
            }
        }
    }
}

/**
 * Geschlossener Pfad des L-förmigen Eckbereichs einer Ecke (Eck-Quadrat
 * [aussen-Ecke … Umlauf-Ecke] abzüglich des Viertelkreises).
 */
private fun eckElbowPath(geometrie: PlanGeometrie, eck: EckGeometrie): Path {
    val sx = eck.sx
    val sy = eck.sy
    val bx = if (sx > 0f) geometrie.aussen.left else geometrie.aussen.right
    val by = if (sy > 0f) geometrie.aussen.top else geometrie.aussen.bottom
    val r = geometrie.eckRadius
    val t = geometrie.ringTiefe
    fun p(u: Float, v: Float): Offset = Offset(bx + sx * u, by + sy * v)

    val c = eck.mitte
    val v0 = p(0f, r) - c
    val v1 = p(r, 0f) - c
    val a0 = atan2(v0.y.toDouble(), v0.x.toDouble())
    var a1 = atan2(v1.y.toDouble(), v1.x.toDouble())
    var delta = a1 - a0
    while (delta > PI) delta -= 2.0 * PI
    while (delta < -PI) delta += 2.0 * PI

    val segmente = 12
    val path = Path()
    path.moveTo(p(0f, t).x, p(0f, t).y)
    for (i in 1..segmente) {
        val a = a0 + delta * i / segmente
        path.lineTo(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
    }
    path.lineTo(p(t, 0f).x, p(t, 0f).y)
    path.lineTo(p(t, t).x, p(t, t).y)
    path.close()
    return path
}

/** Ein ringförmiges Sitz-/Stehband einer Kurve (mit Reihen, Dach und Filterzustand). */
private fun DrawScope.zeichneKurvenBand(
    mitte: Offset,
    innerR: Float,
    outerR: Float,
    startWinkel: Float,
    typ: PlatzTyp,
    ueberdachtAnteil: Float,
    basis: Float,
    zustand: BandZustand,
) {
    val farbe = if (typ == PlatzTyp.SITZ) sitzFarbe else stehFarbe
    val zeileFarbe = if (typ == PlatzTyp.SITZ) sitzZeile else stehZeile
    val tiefe = outerR - innerR
    if (tiefe <= 0f) return

    if (zustand == BandZustand.GRAU) {
        drawPath(
            annulusPath(mitte, innerR, outerR, startWinkel, 90f),
            farbe.copy(alpha = 0.15f),
        )
        return
    }

    drawPath(annulusPath(mitte, innerR, outerR, startWinkel, 90f), farbe)

    // Konzentrische Reihen entlang des Kurvenbogens.
    val anzahl = ((tiefe / (basis * 0.022f)).roundToInt()).coerceIn(2, 8)
    for (i in 1 until anzahl) {
        val r = innerR + tiefe * i / anzahl
        drawArc(
            zeileFarbe.copy(alpha = 0.55f),
            startWinkel,
            90f,
            useCenter = false,
            topLeft = Offset(mitte.x - r, mitte.y - r),
            size = Size(r * 2f, r * 2f),
            style = Stroke(width = 1.dp.toPx()),
        )
    }

    val decke = tiefe * ueberdachtAnteil.coerceIn(0f, 1f)
    if (decke > 0f) {
        // Schraffur nur im überdachten Teil (äußerer Bereich des Bandes).
        clipPath(annulusPath(mitte, outerR - decke, outerR, startWinkel, 90f)) {
            val schritt = basis * 0.02f
            var x = mitte.x - outerR
            while (x <= mitte.x + outerR) {
                drawLine(
                    dachFarbe.copy(alpha = 0.45f),
                    Offset(x, mitte.y + outerR),
                    Offset(x + outerR * 2f, mitte.y - outerR),
                    strokeWidth = 1.2.dp.toPx(),
                )
                x += schritt
            }
        }
        // Vorderkante des Dachs.
        drawArc(
            dachFarbe.copy(alpha = 0.7f),
            startWinkel,
            90f,
            useCenter = false,
            topLeft = Offset(mitte.x - (outerR - decke), mitte.y - (outerR - decke)),
            size = Size((outerR - decke) * 2f, (outerR - decke) * 2f),
            style = Stroke(width = 2.dp.toPx()),
        )
    }

    if (zustand == BandZustand.UEBERDACHT && decke > 0f) {
        // Nicht überdachten Teil des Bandes ausgrauen.
        drawPath(
            annulusPath(mitte, innerR, outerR - decke, startWinkel, 90f),
            ausgegrautFarbe,
        )
    }
}

/**
 * Ringförmiger Sektor (Path) zwischen zwei Radien über einen Winkelbereich –
 * Grundform der Kurven-Bereiche und ihrer Bänder.
 */
private fun annulusPath(mitte: Offset, innerR: Float, outerR: Float, startWinkel: Float, sweep: Float): Path {
    val segmente = 14
    val path = Path()
    fun punkt(radius: Float, winkel: Float): Offset {
        val rad = Math.toRadians(winkel.toDouble())
        return Offset(mitte.x + radius * cos(rad).toFloat(), mitte.y + radius * sin(rad).toFloat())
    }
    var erster = true
    for (i in 0..segmente) {
        val p = punkt(outerR, startWinkel + sweep * i / segmente)
        if (erster) {
            path.moveTo(p.x, p.y)
            erster = false
        } else {
            path.lineTo(p.x, p.y)
        }
    }
    for (i in segmente downTo 0) {
        val p = punkt(innerR, startWinkel + sweep * i / segmente)
        path.lineTo(p.x, p.y)
    }
    path.close()
    return path
}

private fun DrawScope.zeichnePille(
    center: Offset,
    nameLayout: androidx.compose.ui.text.TextLayoutResult,
    kapLayout: androidx.compose.ui.text.TextLayoutResult?,
    pillBreite: Float,
    pillHoehe: Float,
    rotation: Float,
) {
    rotate(rotation, pivot = center) {
        val topLeft = Offset(center.x - pillBreite / 2f, center.y - pillHoehe / 2f)
        drawRoundRect(
            pilleHintergrund,
            topLeft = topLeft,
            size = Size(pillBreite, pillHoehe),
            cornerRadius = CornerRadius(pillHoehe / 2f),
        )
        var textY = topLeft.y + padYInPille()
        drawText(
            nameLayout,
            topLeft = Offset(center.x - (nameLayout.size?.width ?: 0).toFloat() / 2f, textY),
        )
        textY += (nameLayout.size?.height ?: 0).toFloat()
        if (kapLayout != null) {
            drawText(
                kapLayout,
                topLeft = Offset(center.x - (kapLayout.size?.width ?: 0).toFloat() / 2f, textY),
            )
        }
    }
}

private fun DrawScope.padYInPille(): Float = (size.height * 0.012f)

/** Vertikal ausgerichtetes Spielfeld (68:105), Tore oben/unten. */
private fun DrawScope.zeichneSpielfeld(pitch: Rect, basis: Float) {
    drawRect(bruestungFarbe, topLeft = pitch.topLeft, size = pitch.size)

    // Rasenstreifen.
    val streifenzahl = 10
    val streifenh = pitch.height / streifenzahl
    for (i in 0 until streifenzahl) {
        val farbe = if (i % 2 == 0) rasenHell else rasenDunkel
        drawRect(
            farbe,
            topLeft = Offset(pitch.left, pitch.top + i * streifenh),
            size = Size(pitch.width, streifenh),
        )
    }

    val linie = linieFarbe.copy(alpha = 0.85f)
    val lw = basis * 0.004f

    // Außenlinie + Mittellinie (horizontal).
    drawRect(linie, topLeft = pitch.topLeft, size = Size(pitch.width, lw))
    drawRect(linie, topLeft = Offset(pitch.left, pitch.bottom - lw), size = Size(pitch.width, lw))
    val mitteY = pitch.top + pitch.height / 2f
    drawRect(linie, topLeft = Offset(pitch.left, mitteY - lw / 2f), size = Size(pitch.width, lw))
    drawRect(linie, topLeft = Offset(pitch.left, pitch.top), size = Size(lw, pitch.height))
    drawRect(linie, topLeft = Offset(pitch.right - lw, pitch.top), size = Size(lw, pitch.height))

    // Mittelkreis.
    drawCircle(
        linie,
        radius = pitch.width * 0.097f,
        center = Offset(pitch.center.x, mitteY),
        style = Stroke(width = lw),
    )

    // Strafräume oben/unten (Tiefe 16.5/105, Breite 40.32/68 der Spielfeldmaße).
    val ftiefe = pitch.height * (16.5f / 105f)
    val fbreite = pitch.width * (40.32f / 68f)
    drawRect(
        linie,
        topLeft = Offset(pitch.center.x - fbreite / 2f, pitch.top),
        size = Size(fbreite, ftiefe),
        style = Stroke(width = lw),
    )
    drawRect(
        linie,
        topLeft = Offset(pitch.center.x - fbreite / 2f, pitch.bottom - ftiefe),
        size = Size(fbreite, ftiefe),
        style = Stroke(width = lw),
    )

    // Tore.
    val torBreite = pitch.width * 0.10f
    val torTiefe = pitch.width * 0.018f
    val tor = linieFarbe
    drawRect(tor, topLeft = Offset(pitch.center.x - torBreite / 2f, pitch.top - torTiefe), size = Size(torBreite, torTiefe))
    drawRect(tor, topLeft = Offset(pitch.center.x - torBreite / 2f, pitch.bottom), size = Size(torBreite, torTiefe))
}