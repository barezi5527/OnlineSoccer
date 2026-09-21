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
import com.onlinesoccer.app.data.model.TopspielerZeile

private val Spalten = listOf(
    StatistikFixSpalte("#", 26.dp, TextAlign.End),
    StatistikFixSpalte("Team", 136.dp),
    StatistikFixSpalte("Alt", 36.dp, TextAlign.End),
    StatistikFixSpalte("Pos", 40.dp),
    StatistikFixSpalte("Nat", 42.dp),
    StatistikFixSpalte("Wert", 84.dp, TextAlign.End),
)

private val PinKopf = "Spieler"
private const val PinBreiteDp = 140

/** Beste Spieler nach Land/Liga/Statistik/Position wie `osneu/statspieler`. */
@Composable
fun TopspielerScreen(
    onClose: () -> Unit,
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
    viewModel: TopspielerViewModel = hiltViewModel(),
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
                    titel = "Topspieler",
                    untertitel = "Beste Spieler nach Filter",
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
                    StatistikDropdown("Position", uiState.positionen, uiState.position, false) { viewModel.positionWaehlen(it); filterOffen = false }
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
                            StatistikSpielerZelle(zeile.name, zeile.position, zeile.pid, onSpielerClick)
                        },
                        zellen = {
                            StatistikFixZelle(Spalten[0]) { StatistikZahl(zeile.nr?.toString() ?: "–") }
                            StatistikFixZelle(Spalten[1]) { StatistikTeamZelle(zeile.team, zeile.teamId, onTeamClick) }
                            StatistikFixZelle(Spalten[2]) { StatistikZahl(zeile.alter) }
                            StatistikFixZelle(Spalten[3]) { StatistikZahl(zeile.position?.ifEmpty { "–" }?.uppercase() ?: "–") }
                            StatistikFixZelle(Spalten[4]) { FlaggenText(zeile.nation) }
                            StatistikFixZelle(Spalten[5]) { StatistikWert(zeile.wert) }
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