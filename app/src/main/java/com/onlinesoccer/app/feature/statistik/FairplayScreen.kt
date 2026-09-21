package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.FairplayZeile

private val Spalten = listOf(
    StatistikFixSpalte("#", 26.dp, TextAlign.End),
    StatistikFixSpalte("Gelb", 38.dp, TextAlign.End),
    StatistikFixSpalte("G/R", 42.dp, TextAlign.End),
    StatistikFixSpalte("Rot", 38.dp, TextAlign.End),
    StatistikFixSpalte("Sp.", 42.dp, TextAlign.End),
    StatistikFixSpalte("Pkt", 46.dp, TextAlign.End),
)

private val PinKopf = "Team"
private const val PinBreiteDp = 160

/** Fairplaytabelle der Erstligisten (`fpt.php`). */
@Composable
fun FairplayScreen(
    onClose: () -> Unit,
    onTeamClick: (Long) -> Unit = {},
    viewModel: FairplayViewModel = hiltViewModel(),
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
                titel = "Fairplaytabelle",
                untertitel = "Aktuelle Platzierung zur Vergabe der OSE-Startplätze",
                onClose = onClose,
            )
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
                            StatistikFixZelle(Spalten[1]) { StatistikZahl(zeile.gelb?.toString() ?: "–") }
                            StatistikFixZelle(Spalten[2]) { StatistikZahl(zeile.gelbRot?.toString() ?: "–") }
                            StatistikFixZelle(Spalten[3]) { StatistikZahl(zeile.rot?.toString() ?: "–") }
                            StatistikFixZelle(Spalten[4]) { StatistikZahl(zeile.spiele?.toString() ?: "–") }
                            StatistikFixZelle(Spalten[5]) {
                                Text(
                                    zeile.punkte?.let { it.toString().replace('.', ',') } ?: "–",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
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