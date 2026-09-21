package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.TopteamZeile

private val Spalten = listOf(
    StatistikFixSpalte("#", 26.dp, TextAlign.End),
    StatistikFixSpalte("Land", 42.dp),
    StatistikFixSpalte("Wert", 78.dp, TextAlign.End),
)

private val PinKopf = "Team"
private const val PinBreiteDp = 180

/** Wertvollste Teams (Schnitt/Skill/Marktwert/…) wie `osneu/statteam`. */
@Composable
fun TopteamsScreen(
    onClose: () -> Unit,
    onTeamClick: (Long) -> Unit = {},
    viewModel: TopteamsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var filterOffen by remember { mutableStateOf(false) }

    when {
        uiState.ladend -> StatistikLaden()
        uiState.fehler != null -> StatistikFehler(uiState.fehler!!, viewModel::lade)
        else -> Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatistikHeader(
                    titel = "Topteams",
                    untertitel = "Wertvollste Teams nach Filter",
                    onClose = onClose,
                )
                Spacer(Modifier.weight(1f))
                StatistikFilterToggle(filterOffen) { filterOffen = !filterOffen }
            }
            if (filterOffen && (uiState.laender.isNotEmpty() || uiState.ligas.isNotEmpty())) {
                StatistikFilterCard {
                    StatistikDropdown("Land", uiState.laender, uiState.land, false) { viewModel.landWaehlen(it); filterOffen = false }
                    StatistikDropdown("Liga", uiState.ligas, uiState.liga, false) { viewModel.ligaWaehlen(it); filterOffen = false }
                }
                StatistikFilterCard {
                    StatistikDropdown("Statistik", uiState.statistiken, uiState.statistik, false) { viewModel.statistikWaehlen(it); filterOffen = false }
                    StatistikDropdown("Anzeige", uiState.anzeigen, uiState.anzeige, false) { viewModel.anzeigeWaehlen(it); filterOffen = false }
                }
            }
            StatistikScrollTabelle(
                pinKopf = PinKopf,
                pinBreite = PinBreiteDp.dp,
                spalten = Spalten,
                zeilen = uiState.zeilen.map { zeile ->
                    StatistikScrollZeile(
                        pin = {
                            StatistikTeamZelle(zeile.team, zeile.teamId, onTeamClick)
                        },
                        zellen = {
                            StatistikFixZelle(Spalten[0]) { StatistikZahl(zeile.nr?.toString() ?: "–") }
                            StatistikFixZelle(Spalten[1]) { FlaggenText(zeile.land) }
                            StatistikFixZelle(Spalten[2]) { StatistikWert(zeile.wert) }
                        },
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}