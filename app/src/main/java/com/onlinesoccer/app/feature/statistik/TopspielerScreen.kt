package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.TopspielerZeile

private val Spalten = listOf(
    StatistikFixSpalte("#", 30.dp, TextAlign.End),
    StatistikFixSpalte("Team", 150.dp),
    StatistikFixSpalte("Alt", 40.dp, TextAlign.End),
    StatistikFixSpalte("Pos", 44.dp),
    StatistikFixSpalte("Nat", 46.dp),
    StatistikFixSpalte("Wert", 90.dp, TextAlign.End),
)

private val PinKopf = "Spieler"
private const val PinBreiteDp = 150

/** Beste Spieler nach Land/Liga/Statistik/Position wie `osneu/statspieler`. */
@Composable
fun TopspielerScreen(
    onClose: () -> Unit,
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
    viewModel: TopspielerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.ladend -> StatistikLaden()
        uiState.fehler != null -> StatistikFehler(uiState.fehler!!, viewModel::lade)
        else -> Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatistikHeader(
                titel = "Topspieler",
                untertitel = "Beste Spieler nach Filter",
                onClose = onClose,
            )
            if (uiState.laender.isNotEmpty() || uiState.ligas.isNotEmpty()) {
                StatistikFilterCard {
                    StatistikDropdown("Land", uiState.laender, uiState.land, false, viewModel::landWaehlen)
                    StatistikDropdown("Liga", uiState.ligas, uiState.liga, false, viewModel::ligaWaehlen)
                }
                StatistikFilterCard {
                    StatistikDropdown("Statistik", uiState.statistiken, uiState.statistik, false, viewModel::statistikWaehlen)
                    StatistikDropdown("Position", uiState.positionen, uiState.position, false, viewModel::positionWaehlen)
                    StatistikDropdown("Anzeige", uiState.anzeigen, uiState.anzeige, false, viewModel::anzeigeWaehlen)
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
                            StatistikFixZelle(Spalten[0]) { Text(zeile.nr?.toString() ?: "–") }
                            StatistikFixZelle(Spalten[1]) { StatistikTeamZelle(zeile.team, zeile.teamId, onTeamClick) }
                            StatistikFixZelle(Spalten[2]) { Text(zeile.alter) }
                            StatistikFixZelle(Spalten[3]) { Text(zeile.position?.ifEmpty { "–" }?.uppercase() ?: "–") }
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