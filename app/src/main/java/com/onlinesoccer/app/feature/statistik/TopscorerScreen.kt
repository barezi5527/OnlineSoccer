package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.LaenderOption
import com.onlinesoccer.app.feature.server.FehlerBox

private val NR_SPALTE = 36.dp
private val NAME_SPALTE = 160.dp
private val LAND_SPALTE = 52.dp
private val VEREIN_SPALTE = 140.dp
private val LIGA_SPALTE = 120.dp
private val ALTER_SPALTE = 48.dp
private val SKILL_SPALTE = 56.dp
private val OPTI_SPALTE = 56.dp
private val WERT_SPALTE = 64.dp
private val ZEILEN_HOEHE = 40.dp

/** Kategorie-ID „Treter" (`statistik=8`): Fairplay-Wertung der Karten. */
private const val TRETER_ID = "8"

/** „Topscorer": Beste Spieler nach Land/Liga/Statistik/Position/Saison/Art (`topscorer.php`). */
@Composable
fun TopscorerScreen(
    onClose: () -> Unit = {},
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
    viewModel: TopscorerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
            }
            Column(Modifier.padding(start = 4.dp)) {
                Text(
                    "Topscorer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Beste Spieler nach Filter (Top 20)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        TopscorerFilterLeiste(
            uiState = uiState,
            onLand = viewModel::landWaehlen,
            onLiga = viewModel::ligaWaehlen,
            onStatistik = viewModel::statistikWaehlen,
            onPos = viewModel::posWaehlen,
            onSaison = viewModel::saisonWaehlen,
            onArt = viewModel::artWaehlen,
        )

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                uiState.ladend -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.fehler != null -> FehlerBox(uiState.fehler!!, viewModel::lade)
                else -> TopscorerTabelle(uiState = uiState, onSpielerClick = onSpielerClick, onTeamClick = onTeamClick)
            }
        }
    }
}

/** Filter-Leiste: Land/Liga, Kategorie/Position, Saison/Spieltag. */
@Composable
private fun TopscorerFilterLeiste(
    uiState: TopscorerUiState,
    onLand: (String) -> Unit,
    onLiga: (String) -> Unit,
    onStatistik: (String) -> Unit,
    onPos: (String) -> Unit,
    onSaison: (String) -> Unit,
    onArt: (String) -> Unit,
) {
    val enabled = !uiState.ladend
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopscorerFilterAuswahl(
                leerLabel = "Land",
                optionen = uiState.laender,
                wert = uiState.land,
                enabled = enabled,
                onWaehlen = onLand,
                modifier = Modifier.weight(1f),
            )
            TopscorerFilterAuswahl(
                leerLabel = "Liga",
                optionen = uiState.ligas,
                wert = uiState.liga,
                enabled = enabled,
                onWaehlen = onLiga,
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopscorerFilterAuswahl(
                leerLabel = "Kategorie",
                optionen = uiState.statistiken,
                wert = uiState.statistik,
                enabled = enabled,
                onWaehlen = onStatistik,
                modifier = Modifier.weight(1f),
            )
            TopscorerFilterAuswahl(
                leerLabel = "Position",
                optionen = uiState.positionen,
                wert = uiState.pos,
                enabled = enabled,
                onWaehlen = onPos,
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopscorerFilterAuswahl(
                leerLabel = "Saison",
                optionen = uiState.saisons,
                wert = uiState.saison,
                enabled = enabled,
                onWaehlen = onSaison,
                modifier = Modifier.weight(1f),
            )
            TopscorerFilterAuswahl(
                leerLabel = "Spieltag",
                optionen = uiState.arten,
                wert = uiState.art,
                enabled = enabled,
                onWaehlen = onArt,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Kompakte Auswahl (OutlinedButton + Dropdown) wie in den Bewerbe-Filtern. */
@Composable
private fun TopscorerFilterAuswahl(
    leerLabel: String,
    optionen: List<LaenderOption>,
    wert: String,
    enabled: Boolean,
    onWaehlen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var offen by remember { mutableStateOf(false) }
    val gewaehlt = optionen.firstOrNull { it.id == wert }?.label ?: leerLabel
    val text = if (gewaehlt.startsWith(leerLabel, ignoreCase = true)) {
        gewaehlt
    } else {
        "$leerLabel: $gewaehlt"
    }
    Box(modifier) {
        OutlinedButton(
            onClick = { if (enabled) offen = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(6.dp))
            Text("▾", fontSize = 10.sp)
        }
        DropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            optionen.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        offen = false
                        if (option.id != wert) onWaehlen(option.id)
                    },
                )
            }
        }
    }
}

/** Tabellen-Kurzinfo (Kategorie · Saison · Spieltag · Anzahl). */
private fun zusatzInfo(uiState: TopscorerUiState): String {
    val statistik = uiState.statistiken.firstOrNull { it.id == uiState.statistik }?.label
    val saison = uiState.saisons.firstOrNull { it.id == uiState.saison }?.label
    val art = uiState.arten.firstOrNull { it.id == uiState.art }?.label
    return buildList {
        statistik?.let { add(it) }
        saison?.let { add(it) }
        art?.let { add(it) }
        add("${uiState.zeilen.size} Spieler")
    }.joinToString(" · ")
}

/** Fixierbare Tabelle: fixierte Name-Spalte + scrollbare Zusatzspalten. */
@Composable
private fun TopscorerTabelle(
    uiState: TopscorerUiState,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    val zeilen = uiState.zeilen
    if (zeilen.isEmpty()) {
        Text(
            "Keine Einträge",
            Modifier.padding(24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    var markierterSpielerId by rememberSaveable { mutableStateOf<Long?>(null) }
    val vertScroll = rememberScrollState()
    val horizScroll = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        Text(
            zusatzInfo(uiState),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (uiState.statistik == TRETER_ID) {
            Text(
                "Fairplay-Wertung: Gelb = 1 Punkt, Rot = 5 Punkte",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
            TabellenKopfZelle("Nr", NR_SPALTE, TextAlign.End)
            TabellenKopfZelle("Name", NAME_SPALTE, TextAlign.Start)
            Row(Modifier.horizontalScroll(horizScroll)) {
                TabellenKopfZelle("Land", LAND_SPALTE, TextAlign.Center)
                TabellenKopfZelle("Verein", VEREIN_SPALTE, TextAlign.Start)
                TabellenKopfZelle("Liga", LIGA_SPALTE, TextAlign.Start)
                TabellenKopfZelle("Alter", ALTER_SPALTE, TextAlign.End)
                TabellenKopfZelle("Skill", SKILL_SPALTE, TextAlign.End)
                TabellenKopfZelle("Opti", OPTI_SPALTE, TextAlign.End)
                TabellenKopfZelle("Wert", WERT_SPALTE, TextAlign.End)
            }
        }

        Row(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                Modifier
                    .width(NR_SPALTE + NAME_SPALTE)
                    .verticalScroll(vertScroll),
            ) {
                zeilen.forEachIndexed { index, zeile ->
                    val markiert = zeile.spielerId != null && zeile.spielerId == markierterSpielerId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(ZEILEN_HOEHE)
                            .background(zeilenFarbe(index, markiert)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .width(NR_SPALTE)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            if (markiert) {
                                Box(
                                    Modifier
                                        .fillMaxHeight()
                                        .width(3.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .align(Alignment.CenterStart),
                                )
                            }
                            Text(
                                zeile.nr?.toString() ?: "–",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.padding(end = 6.dp),
                            )
                        }
                        Text(
                            zeile.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (markiert) FontWeight.Bold else FontWeight.Normal,
                            color = if (markiert) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (zeile.spielerId != null) Modifier.clickable {
                                        markierterSpielerId = if (markiert) null else zeile.spielerId
                                        onSpielerClick(zeile.spielerId)
                                    } else Modifier,
                                )
                                .padding(horizontal = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Box(Modifier.fillMaxHeight().weight(1f)) {
                Row(Modifier.horizontalScroll(horizScroll)) {
                    Column(Modifier.verticalScroll(vertScroll)) {
                        zeilen.forEachIndexed { index, zeile ->
                            val markiert = zeile.spielerId != null && zeile.spielerId == markierterSpielerId
                            Row(
                                Modifier
                                    .height(ZEILEN_HOEHE)
                                    .background(zeilenFarbe(index, markiert)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StatistikZelle(LAND_SPALTE, TextAlign.Center) {
                                    FlaggenText(zeile.land)
                                }
                                StatistikZelle(VEREIN_SPALTE, TextAlign.Start) {
                                    val teamId = zeile.teamId
                                    Text(
                                        zeile.verein,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (markiert) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .then(
                                                if (teamId != null) Modifier.clickable {
                                                    onTeamClick(teamId)
                                                } else Modifier,
                                            )
                                            .padding(horizontal = 4.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                StatistikZelle(LIGA_SPALTE, TextAlign.Start) {
                                    Text(
                                        zeile.liga,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                StatistikZelle(ALTER_SPALTE, TextAlign.End) {
                                    Text(
                                        zeile.alter,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                    )
                                }
                                StatistikZelle(SKILL_SPALTE, TextAlign.End) {
                                    Text(
                                        zeile.skill,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                    )
                                }
                                StatistikZelle(OPTI_SPALTE, TextAlign.End) {
                                    Text(
                                        zeile.opti,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                    )
                                }
                                StatistikZelle(WERT_SPALTE, TextAlign.End) {
                                    Text(
                                        zeile.wert,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatistikZelle(breite: Dp, ausrichtung: TextAlign, content: @Composable () -> Unit) {
    Box(
        Modifier
            .width(breite)
            .height(ZEILEN_HOEHE),
        contentAlignment = when (ausrichtung) {
            TextAlign.End -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        },
    ) {
        content()
    }
}

@Composable
private fun TabellenKopfZelle(text: String, breite: Dp, ausrichtung: TextAlign) {
    Box(
        Modifier
            .width(breite)
            .padding(horizontal = 6.dp),
        contentAlignment = when (ausrichtung) {
            TextAlign.End -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        },
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Ländercode als kleines Text-Badge (keine Netzwerk-Bilder in der App). */
@Composable
private fun FlaggenText(code: String) {
    val text = code.trim().uppercase()
    if (text.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun zeilenFarbe(zahl: Int, markiert: Boolean): Color = when {
    markiert -> MaterialTheme.colorScheme.primaryContainer
    zahl % 2 == 1 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    else -> Color.Transparent
}
