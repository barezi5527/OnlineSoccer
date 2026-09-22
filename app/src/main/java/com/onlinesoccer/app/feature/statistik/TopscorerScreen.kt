package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.LaenderOption

/**
 * Kurzbezeichnung der Werte-Spalte passend zur gewählten Statistik
 * (`statistik=1` Tore … `10` Karten pro Minute); sonst „Wert".
 */
internal fun topscorerWertKopf(statistikId: String): String = when (statistikId) {
    "1" -> "Tore"
    "2" -> "Vorlagen"
    "3" -> "Punkte"
    "4" -> "Tore/Spiel"
    "5" -> "Vorlagen/Spiel"
    "6" -> "Minuten/Tor"
    "7" -> "Minuten"
    "8" -> "Treter"
    "9" -> "Karten/Spiel"
    "10" -> "Karten/Min"
    else -> "Wert"
}

private fun topscorerSpalten(wertKopf: String) = listOf(
    StatistikFixSpalte("#", 26.dp, TextAlign.End),
    StatistikFixSpalte("Alt", 36.dp, TextAlign.End),
    StatistikFixSpalte("Sk", 40.dp, TextAlign.End),
    StatistikFixSpalte("Opt", 40.dp, TextAlign.End),
    StatistikFixSpalte("Nat", 42.dp),
    StatistikFixSpalte("Verein", 128.dp),
    StatistikFixSpalte("Liga", 100.dp),
    StatistikFixSpalte(wertKopf, 104.dp, TextAlign.End),
)

private val PinKopf = "Spieler"
private const val PinBreiteDp = 140

/** Topscorer-Liste (`topscorer.php`) – immer nur die Top 100. */
@Composable
fun TopscorerScreen(
    onClose: () -> Unit,
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
    viewModel: TopscorerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var filterOffen by remember { mutableStateOf(false) }

    if (uiState.fehler == null && uiState.laender.isEmpty() && uiState.ligas.isEmpty()) {
        StatistikLaden()
        return
    }

    Column(
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
                titel = "Topscorer",
                untertitel = "Es werden immer nur die Top 100 ausgegeben.",
                onClose = onClose,
            )
            Spacer(Modifier.weight(1f))
            StatistikFilterToggle(filterOffen) { filterOffen = !filterOffen }
        }
        if (filterOffen && (uiState.laender.isNotEmpty() || uiState.ligas.isNotEmpty())) {
            StatistikFilterCard {
                StatistikDropdown("Land", uiState.laender, uiState.land, uiState.ladend) { viewModel.landWaehlen(it); filterOffen = false }
                StatistikDropdown("Liga", uiState.ligas, uiState.liga, uiState.ladend) { viewModel.ligaWaehlen(it); filterOffen = false }
            }
            StatistikFilterCard {
                StatistikDropdown("Statistik", uiState.statistiken, uiState.statistik, uiState.ladend) { viewModel.statistikWaehlen(it); filterOffen = false }
                StatistikDropdown("Position", uiState.positionen, uiState.position, uiState.ladend) { viewModel.positionWaehlen(it); filterOffen = false }
            }
            StatistikFilterCard {
                StatistikDropdown("Saison", uiState.saisons, uiState.saison, uiState.ladend) { viewModel.saisonWaehlen(it); filterOffen = false }
                StatistikDropdown("Art", uiState.arten, uiState.art, uiState.ladend) { viewModel.artWaehlen(it); filterOffen = false }
            }
        }
        TopscorerZusammenfassung(uiState)
        val spalten = topscorerSpalten(topscorerWertKopf(uiState.statistik))
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
                    spalten = spalten,
                    zeilen = uiState.zeilen.map { zeile ->
                        StatistikScrollZeile(
                            pin = {
                                StatistikSpielerZelle(zeile.name, zeile.position, zeile.pid, onSpielerClick)
                            },
                            zellen = {
                                StatistikFixZelle(spalten[0]) { StatistikZahl(zeile.nr?.toString() ?: "–") }
                                StatistikFixZelle(spalten[1]) { StatistikZahl(zeile.alter) }
                                StatistikFixZelle(spalten[2]) { StatistikZahl(zeile.skill) }
                                StatistikFixZelle(spalten[3]) { StatistikZahl(zeile.opti) }
                                StatistikFixZelle(spalten[4]) { FlaggenText(zeile.land) }
                                StatistikFixZelle(spalten[5]) { StatistikTeamZelle(zeile.team, zeile.teamId, onTeamClick) }
                                StatistikFixZelle(spalten[6]) { StatistikZahl(zeile.liga) }
                                StatistikFixZelle(spalten[7]) { StatistikWert(zeile.wert) }
                            },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                )
            }
        }
    }
}

/** Kompakte Zeile mit den aktuell angewendeten Filtern (übersicht bei geschlossener Filtermaske). */
@Composable
private fun TopscorerZusammenfassung(uiState: TopscorerUiState) {
    val teile = listOf(
        optionLabel(uiState.ligas, uiState.liga),
        optionLabel(uiState.statistiken, uiState.statistik),
        optionLabel(uiState.positionen, uiState.position),
        optionLabel(uiState.saisons, uiState.saison),
        optionLabel(uiState.arten, uiState.art),
    ).filterNot { it.isNullOrBlank() || it == "Alle" }
    if (teile.isEmpty()) return
    Text(
        text = teile.joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun optionLabel(optionen: List<LaenderOption>, id: String): String? =
    optionen.firstOrNull { it.id == id }?.label