package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.TopteamZeile

private val Spalten = listOf(
    StatistikSpalte("#", 0.6f, TextAlign.End),
    StatistikSpalte("Team", 3f),
    StatistikSpalte("Land", 1f),
    StatistikSpalte("Wert", 1.3f, TextAlign.End),
)

/** Wertvollste Teams (Schnitt/Skill/Marktwert/…) wie `osneu/statteam`. */
@Composable
fun TopteamsScreen(
    onClose: () -> Unit,
    onTeamClick: (Long) -> Unit = {},
    viewModel: TopteamsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.ladend -> StatistikLaden()
        uiState.fehler != null -> StatistikFehler(uiState.fehler!!, viewModel::lade)
        else -> LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatistikHeader(
                    titel = "Topteams",
                    untertitel = "Wertvollste Teams nach Filter",
                    onClose = onClose,
                )
            }
            if (uiState.laender.isNotEmpty() || uiState.ligas.isNotEmpty()) {
                item {
                    StatistikFilterCard {
                        StatistikDropdown("Land", uiState.laender, uiState.land, false, viewModel::landWaehlen)
                        StatistikDropdown("Liga", uiState.ligas, uiState.liga, false, viewModel::ligaWaehlen)
                    }
                }
                item {
                    StatistikFilterCard {
                        StatistikDropdown("Statistik", uiState.statistiken, uiState.statistik, false, viewModel::statistikWaehlen)
                        StatistikDropdown("Anzeige", uiState.anzeigen, uiState.anzeige, false, viewModel::anzeigeWaehlen)
                    }
                }
            }
            item {
                StatistikTabelle(spalten = Spalten, zeilen = uiState.zeilen.map { zeile ->
                    { TopteamZeileSpalten(zeile, onTeamClick) }
                })
            }
        }
    }
}

@Composable
private fun RowScope.TopteamZeileSpalten(zeile: TopteamZeile, onTeamClick: (Long) -> Unit) {
    StatistikSpalte(Spalten[0]) { Text(zeile.nr?.toString() ?: "–") }
    StatistikSpalte(Spalten[1]) { StatistikTeamZelle(zeile.team, zeile.teamId, onTeamClick) }
    StatistikSpalte(Spalten[2]) { FlaggenText(zeile.land) }
    StatistikSpalte(Spalten[3]) { StatistikWert(zeile.wert) }
}