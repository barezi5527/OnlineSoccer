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
private val TEAM_SPALTE = 150.dp
private val ALTER_SPALTE = 48.dp
private val POS_SPALTE = 52.dp
private val NATION_SPALTE = 52.dp
private val WERT_SPALTE = 64.dp
private val ZEILEN_HOEHE = 40.dp

/** „Topspieler": Beste Spieler nach Land/Liga/Statistik/Position/Anzeige (`osneu/statspieler`). */
@Composable
fun TopspielerScreen(
    onClose: () -> Unit = {},
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
    viewModel: TopspielerViewModel = hiltViewModel(),
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
                    "Topspieler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Beste Spieler nach Filter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        TopspielerFilterLeiste(
            uiState = uiState,
            onLand = viewModel::landWaehlen,
            onLiga = viewModel::ligaWaehlen,
            onStatistik = viewModel::statistikWaehlen,
            onPosition = viewModel::positionWaehlen,
            onAnzeige = viewModel::anzeigeWaehlen,
        )

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                uiState.ladend -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.fehler != null -> FehlerBox(uiState.fehler!!, viewModel::lade)
                else -> TopspielerTabelle(uiState = uiState, onSpielerClick = onSpielerClick, onTeamClick = onTeamClick)
            }
        }
    }
}

/** Filter-Leiste: Land/Liga, Statistik/Position, Anzeige. */
@Composable
private fun TopspielerFilterLeiste(
    uiState: TopspielerUiState,
    onLand: (String) -> Unit,
    onLiga: (String) -> Unit,
    onStatistik: (String) -> Unit,
    onPosition: (String) -> Unit,
    onAnzeige: (String) -> Unit,
) {
    val enabled = !uiState.ladend
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopspielerFilterAuswahl(
                leerLabel = "Land",
                optionen = uiState.laender,
                wert = uiState.land,
                enabled = enabled,
                onWaehlen = onLand,
                modifier = Modifier.weight(1f),
            )
            TopspielerFilterAuswahl(
                leerLabel = "Liga",
                optionen = uiState.ligas,
                wert = uiState.liga,
                enabled = enabled,
                onWaehlen = onLiga,
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopspielerFilterAuswahl(
                leerLabel = "Statistik",
                optionen = uiState.statistiken,
                wert = uiState.statistik,
                enabled = enabled,
                onWaehlen = onStatistik,
                modifier = Modifier.weight(1f),
            )
            TopspielerFilterAuswahl(
                leerLabel = "Position",
                optionen = uiState.positionen,
                wert = uiState.position,
                enabled = enabled,
                onWaehlen = onPosition,
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopspielerFilterAuswahl(
                leerLabel = "Anzeige",
                optionen = uiState.anzeigen,
                wert = uiState.anzeige,
                enabled = enabled,
                onWaehlen = onAnzeige,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

/** Kompakte Auswahl (OutlinedButton + Dropdown) wie in den Bewerbe-Filtern. */
@Composable
private fun TopspielerFilterAuswahl(
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

/** Tabellen-Kurzinfo (Statistik · Position · Anzeige · Anzahl). */
private fun zusatzInfo(uiState: TopspielerUiState): String {
    val statistik = uiState.statistiken.firstOrNull { it.id == uiState.statistik }?.label
    val position = uiState.positionen.firstOrNull { it.id == uiState.position }?.label
    val anzeige = uiState.anzeigen.firstOrNull { it.id == uiState.anzeige }?.label
    return buildList {
        statistik?.let { add(it) }
        position?.let { add(it) }
        anzeige?.let { add(it) }
        add("${uiState.zeilen.size} Spieler")
    }.joinToString(" · ")
}

/** Fixierbare Tabelle: fixierte Name-Spalte + scrollbare Zusatzspalten. */
@Composable
private fun TopspielerTabelle(
    uiState: TopspielerUiState,
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

        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
            TabellenKopfZelle("Nr", NR_SPALTE, TextAlign.End)
            TabellenKopfZelle("Spieler", NAME_SPALTE, TextAlign.Start)
            Row(Modifier.horizontalScroll(horizScroll)) {
                TabellenKopfZelle("Team", TEAM_SPALTE, TextAlign.Start)
                TabellenKopfZelle("Alter", ALTER_SPALTE, TextAlign.End)
                TabellenKopfZelle("Pos", POS_SPALTE, TextAlign.Center)
                TabellenKopfZelle("Nation", NATION_SPALTE, TextAlign.Center)
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
                                StatistikZelle(TEAM_SPALTE, TextAlign.Start) {
                                    val teamId = zeile.teamId
                                    Text(
                                        zeile.team,
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
                                StatistikZelle(ALTER_SPALTE, TextAlign.End) {
                                    Text(
                                        zeile.alter,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                    )
                                }
                                StatistikZelle(POS_SPALTE, TextAlign.Center) {
                                    Text(
                                        zeile.position,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                    )
                                }
                                StatistikZelle(NATION_SPALTE, TextAlign.Center) {
                                    FlaggenText(zeile.nation)
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
