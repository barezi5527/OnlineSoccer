package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.FairplayZeile
import java.util.Locale

private val Spalten = listOf(
    StatistikFixSpalte("#", 30.dp, TextAlign.End),
    StatistikFixSpalte("Gelb", 44.dp, TextAlign.End),
    StatistikFixSpalte("Gelb-Rot", 68.dp, TextAlign.End),
    StatistikFixSpalte("Rot", 40.dp, TextAlign.End),
    StatistikFixSpalte("Spiele", 52.dp, TextAlign.End),
    StatistikFixSpalte("Punkte", 60.dp, TextAlign.End),
)

private val PinKopf = "Team"
private const val PinBreiteDp = 160

/** Fairplay-Wertung der Erstligisten (`fpt.php`) zur Vergabe der drei OSE-Startplätze. */
@Composable
fun FairplayScreen(
    onClose: () -> Unit,
    onTeamClick: (Long) -> Unit = {},
    viewModel: FairplayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatistikHeader(
            titel = "Fairplaytabelle",
            untertitel = "Nur Erstligisten – relevant für die drei Startplätze im OSE",
            onClose = onClose,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.ladend -> CircularProgressIndicator(Modifier.size(22.dp))
                uiState.fehler != null -> StatistikFehler(uiState.fehler!!, viewModel::lade)
                uiState.zeilen.isEmpty() -> StatistikKeineTreffer()
                else -> StatistikScrollTabelle(
                    pinKopf = PinKopf,
                    pinBreite = PinBreiteDp.dp,
                    spalten = Spalten,
                    zeilen = uiState.zeilen.map { zeile ->
                        fairplayScrollZeile(zeile, onTeamClick)
                    },
                    auswaehlbar = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                )
            }
        }
    }
}

private fun fairplayScrollZeile(
    zeile: FairplayZeile,
    onTeamClick: (Long) -> Unit,
) = StatistikScrollZeile(
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
            StatistikWert(fairplayPunkteText(zeile.punkte))
        }
    },
)

/** Punkte-Wert mit 4 Nachkommastellen in deutschem Komma-Format (Server zeigt `0.0263`). */
internal fun fairplayPunkteText(punkte: Double?): String =
    punkte?.let { String.format(Locale.GERMANY, "%.4f", it) } ?: "–"