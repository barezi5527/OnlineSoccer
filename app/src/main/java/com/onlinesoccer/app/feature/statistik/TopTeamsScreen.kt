package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val TEAM_SPALTE = 180.dp
private val NR_SPALTE = 36.dp
private val LAND_SPALTE = 52.dp
private val WERT_SPALTE = 96.dp

/** „Top-Teams": Wertvollste Teams nach Land/Liga/Statistik/Anzeige (`osneu/statteam`). */
@Composable
fun TopTeamsScreen(
    onClose: () -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
    viewModel: TopTeamsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        StatistikHeader(
            titel = "Top-Teams",
            untertitel = "Wertvollste Teams nach Filter",
            onClose = onClose,
        )

        TopTeamsFilterLeiste(
            uiState = uiState,
            onLand = viewModel::landWaehlen,
            onLiga = viewModel::ligaWaehlen,
            onStatistik = viewModel::statistikWaehlen,
            onAnzeige = viewModel::anzeigeWaehlen,
        )

        Column(Modifier.fillMaxWidth().weight(1f)) {
            when {
                uiState.ladend -> StatistikLaden()
                uiState.fehler != null -> StatistikFehler(uiState.fehler!!, viewModel::lade)
                else -> TopTeamsTabelle(uiState = uiState, onTeamClick = onTeamClick)
            }
        }
    }
}

/** Filter-Leiste (Land/Liga, Statistik/Anzeige) – immer sichtbar mit Sofort-Neuladen. */
@Composable
private fun TopTeamsFilterLeiste(
    uiState: TopTeamsUiState,
    onLand: (String) -> Unit,
    onLiga: (String) -> Unit,
    onStatistik: (String) -> Unit,
    onAnzeige: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatistikDropdown("Land", uiState.laender, uiState.land, uiState.ladend, onLand)
            StatistikDropdown("Liga", uiState.ligas, uiState.liga, uiState.ladend, onLiga)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatistikDropdown("Statistik", uiState.statistiken, uiState.statistik, uiState.ladend, onStatistik)
            StatistikDropdown("Anzeige", uiState.anzeigen, uiState.anzeige, uiState.ladend, onAnzeige)
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

/** Fixierbare Tabelle mit Markierung der ausgewählten Zeile. */
@Composable
private fun TopTeamsTabelle(
    uiState: TopTeamsUiState,
    onTeamClick: (Long) -> Unit,
) {
    var markierterTeamId by rememberSaveable { mutableStateOf<Long?>(null) }

    val spalten = listOf(
        StatistikFixSpalte("Nr", NR_SPALTE, TextAlign.End),
        StatistikFixSpalte("Land", LAND_SPALTE, TextAlign.Center),
        StatistikFixSpalte("Wert", WERT_SPALTE, TextAlign.End),
    )
    val zeilen = uiState.zeilen.map { zeile ->
        StatistikScrollZeile(
            pin = {
                StatistikTeamZelle(zeile.team, zeile.teamId) { teamId ->
                    markierterTeamId = if (teamId == markierterTeamId) null else teamId
                    onTeamClick(teamId)
                }
            },
            zellen = {
                StatistikFixZelle(spalten[0]) { StatistikZahl(zeile.nr?.toString() ?: "–") }
                StatistikFixZelle(spalten[1]) { FlaggenText(zeile.land) }
                StatistikFixZelle(spalten[2]) { StatistikWert(zeile.wert) }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            zusatzInfo(uiState),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatistikScrollTabelle(
            pinKopf = "Team",
            pinBreite = TEAM_SPALTE,
            spalten = spalten,
            zeilen = zeilen,
            markierung = { index -> uiState.zeilen[index].teamId == markierterTeamId },
            modifier = Modifier.fillMaxWidth().fillMaxSize(),
        )
    }
}