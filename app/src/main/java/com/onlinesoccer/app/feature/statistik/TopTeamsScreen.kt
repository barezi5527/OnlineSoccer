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

private val TEAM_SPALTE = 180.dp
private val NR_SPALTE = 36.dp
private val LAND_SPALTE = 52.dp
private val WERT_SPALTE = 96.dp
private val ZEILEN_HOEHE = 40.dp

/** „Top-Teams": Wertvollste Teams nach Land/Liga/Statistik/Anzeige (`osneu/statteam`). */
@Composable
fun TopTeamsScreen(
    onClose: () -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
    viewModel: TopTeamsViewModel = hiltViewModel(),
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
                    "Top-Teams",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Wertvollste Teams nach Filter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        TopTeamsFilterLeiste(
            uiState = uiState,
            onLand = viewModel::landWaehlen,
            onLiga = viewModel::ligaWaehlen,
            onStatistik = viewModel::statistikWaehlen,
            onAnzeige = viewModel::anzeigeWaehlen,
        )

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                uiState.ladend -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.fehler != null -> FehlerBox(uiState.fehler!!, viewModel::lade)
                else -> TopTeamsTabelle(uiState = uiState, onTeamClick = onTeamClick)
            }
        }
    }
}

/** Filter-Leiste (Land/Liga, Statistik/Anzeige) – wie Liga-/Tabellenfilter der App. */
@Composable
private fun TopTeamsFilterLeiste(
    uiState: TopTeamsUiState,
    onLand: (String) -> Unit,
    onLiga: (String) -> Unit,
    onStatistik: (String) -> Unit,
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
            TopTeamsFilterAuswahl(
                leerLabel = "Land",
                optionen = uiState.laender,
                wert = uiState.land,
                enabled = enabled,
                onWaehlen = onLand,
                modifier = Modifier.weight(1f),
            )
            TopTeamsFilterAuswahl(
                leerLabel = "Liga",
                optionen = uiState.ligas,
                wert = uiState.liga,
                enabled = enabled,
                onWaehlen = onLiga,
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopTeamsFilterAuswahl(
                leerLabel = "Statistik",
                optionen = uiState.statistiken,
                wert = uiState.statistik,
                enabled = enabled,
                onWaehlen = onStatistik,
                modifier = Modifier.weight(1f),
            )
            TopTeamsFilterAuswahl(
                leerLabel = "Anzeige",
                optionen = uiState.anzeigen,
                wert = uiState.anzeige,
                enabled = enabled,
                onWaehlen = onAnzeige,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Kompakte Auswahl (OutlinedButton + Dropdown) wie in den Bewerbe-Filtern. */
@Composable
private fun TopTeamsFilterAuswahl(
    leerLabel: String,
    optionen: List<LaenderOption>,
    wert: String,
    enabled: Boolean,
    onWaehlen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var offen by remember { mutableStateOf(false) }
    val text = optionen.firstOrNull { it.id == wert }?.label ?: leerLabel
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

/** Tabellen-Kurzinfo (Anzeige · Statistik · Anzahl). */
private fun zusatzInfo(uiState: TopTeamsUiState): String {
    val anzeige = uiState.anzeigen.firstOrNull { it.id == uiState.anzeige }?.label
    val statistik = uiState.statistiken.firstOrNull { it.id == uiState.statistik }?.label
    return buildList {
        anzeige?.let { add(it) }
        statistik?.let { add(it) }
        add("${uiState.zeilen.size} Teams")
    }.joinToString(" · ")
}

/** Fixierbare Tabelle: fixierte Team-Spalte + scrollbare Zusatzspalten, markierte Zeile zeilenübergreifend. */
@Composable
private fun TopTeamsTabelle(
    uiState: TopTeamsUiState,
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

    var markierterTeamId by rememberSaveable { mutableStateOf<Long?>(null) }
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
            TabellenKopfZelle("Team", TEAM_SPALTE, TextAlign.Start)
            Row(Modifier.horizontalScroll(horizScroll)) {
                TabellenKopfZelle("Nr", NR_SPALTE, TextAlign.End)
                TabellenKopfZelle("Land", LAND_SPALTE, TextAlign.Center)
                TabellenKopfZelle("Wert", WERT_SPALTE, TextAlign.End)
            }
        }

        Row(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                Modifier
                    .width(TEAM_SPALTE)
                    .verticalScroll(vertScroll),
            ) {
                zeilen.forEachIndexed { index, zeile ->
                    val teamId = zeile.teamId
                    val markiert = teamId != null && teamId == markierterTeamId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(ZEILEN_HOEHE)
                            .background(zeilenFarbe(index, markiert))
                            .then(
                                if (teamId != null) Modifier.clickable {
                                    markierterTeamId = if (markiert) null else teamId
                                    onTeamClick(teamId)
                                } else Modifier,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (markiert) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .width(3.dp)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                        Text(
                            zeile.team,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (markiert) FontWeight.Bold else FontWeight.Normal,
                            color = if (markiert) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
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
                            val markiert = zeile.teamId != null && zeile.teamId == markierterTeamId
                            Row(
                                Modifier
                                    .height(ZEILEN_HOEHE)
                                    .background(zeilenFarbe(index, markiert)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StatistikZelle(NR_SPALTE, TextAlign.End) {
                                    Text(
                                        zeile.nr?.toString() ?: "–",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                    )
                                }
                                StatistikZelle(LAND_SPALTE, TextAlign.Center) {
                                    FlaggenText(zeile.land)
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