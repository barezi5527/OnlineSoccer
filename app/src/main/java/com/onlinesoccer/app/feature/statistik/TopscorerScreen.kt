package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

/** Kurzbezeichnung der Werte-Spalte passend zur gewählten Statistik; sonst „Wert". */
internal fun topscorerWertKopf(statistikId: String): String = when (statistikId) {
    "1" -> "Tore"
    "2" -> "Vorlagen"
    "3" -> "Scorerpkt."
    "4" -> "Tore/Spiel"
    "5" -> "Vorl./Spiel"
    "6" -> "Min/Tor"
    "7" -> "Spielmin."
    "8" -> "Treter"
    "9" -> "Karten/Spiel"
    "10" -> "Karten/Min"
    else -> "Wert"
}

private val PIN_SPALTE = 150.dp
private val NR_SPALTE = 30.dp
private val ALTER_SPALTE = 36.dp
private val SKILL_SPALTE = 56.dp
private val OPTI_SPALTE = 64.dp
private val LAND_SPALTE = 44.dp
private val WERT_SPALTE = 104.dp
private val TEAM_SPALTE = 120.dp
private val LIGA_SPALTE = 104.dp

/** Topscorer-Liste (`topscorer.php`) – immer nur die Top 100. */
@Composable
fun TopscorerScreen(
    onClose: () -> Unit = {},
    onSpielerClick: (Long, Long?) -> Unit = { _, _ -> },
    onTeamClick: (Long) -> Unit = {},
    viewModel: TopscorerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var filterOffen by remember { mutableStateOf(false) }

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

        if (filterOffen) {
            StatistikFilterCard {
                StatistikDropdown("Land", uiState.laender, uiState.land, uiState.ladend) {
                    viewModel.landWaehlen(it)
                    filterOffen = false
                }
                StatistikDropdown("Liga", uiState.ligas, uiState.liga, uiState.ladend) {
                    viewModel.ligaWaehlen(it)
                    filterOffen = false
                }
            }
            StatistikFilterCard {
                StatistikDropdown("Statistik", uiState.statistiken, uiState.statistik, uiState.ladend) {
                    viewModel.statistikWaehlen(it)
                    filterOffen = false
                }
                StatistikDropdown("Position", uiState.positionen, uiState.position, uiState.ladend) {
                    viewModel.positionWaehlen(it)
                    filterOffen = false
                }
            }
            StatistikFilterCard {
                StatistikDropdown("Saison", uiState.saisons, uiState.saison, uiState.ladend) {
                    viewModel.saisonWaehlen(it)
                    filterOffen = false
                }
                StatistikDropdown("Art", uiState.arten, uiState.art, uiState.ladend) {
                    viewModel.artWaehlen(it)
                    filterOffen = false
                }
            }
        }
        TopscorerZusammenfassung(uiState)

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                uiState.ladend -> StatistikLaden()
                uiState.fehler != null -> StatistikFehler(uiState.fehler!!, viewModel::lade)
                uiState.zeilen.isEmpty() -> StatistikKeineTreffer()
                else -> TopscorerTabelle(
                    uiState = uiState,
                    onSpielerClick = onSpielerClick,
                    onTeamClick = onTeamClick,
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

/** Fixierbare Topscorer-Tabelle: Spieler fixiert, Werte-Spalte dynamisch benannt. */
@Composable
private fun TopscorerTabelle(
    uiState: TopscorerUiState,
    onSpielerClick: (Long, Long?) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    val spalten = listOf(
        StatistikFixSpalte("#", NR_SPALTE, TextAlign.End),
        StatistikFixSpalte("Alter", ALTER_SPALTE, TextAlign.End),
        StatistikFixSpalte("Skill", SKILL_SPALTE, TextAlign.End),
        StatistikFixSpalte("Optimal", OPTI_SPALTE, TextAlign.End),
        StatistikFixSpalte("Land", LAND_SPALTE, TextAlign.Center),
        StatistikFixSpalte(topscorerWertKopf(uiState.statistik), WERT_SPALTE, TextAlign.End),
        StatistikFixSpalte("Verein", TEAM_SPALTE, TextAlign.Start),
        StatistikFixSpalte("Liga", LIGA_SPALTE, TextAlign.Start),
    )
    val zeilen = uiState.zeilen.map { zeile ->
        StatistikScrollZeile(
            pin = {
                StatistikSpielerZelle(zeile.name, zeile.position, zeile.pid) { pid ->
                    onSpielerClick(pid, zeile.teamId)
                }
            },
            zellen = {
                StatistikFixZelle(spalten[0]) { StatistikZahl(zeile.nr?.toString() ?: "–") }
                StatistikFixZelle(spalten[1]) { StatistikZahl(zeile.alter) }
                StatistikFixZelle(spalten[2]) { StatistikZahl(zeile.skill) }
                StatistikFixZelle(spalten[3]) { StatistikZahl(zeile.opti) }
                StatistikFixZelle(spalten[4]) { FlaggenText(zeile.land) }
                StatistikFixZelle(spalten[5]) { StatistikWert(zeile.wert) }
                StatistikFixZelle(spalten[6]) { StatistikTeamZelle(zeile.team, zeile.teamId, onTeamClick) }
                StatistikFixZelle(spalten[7]) { StatistikZahl(zeile.liga) }
            },
        )
    }

    StatistikScrollTabelle(
        pinKopf = "Spieler",
        pinBreite = PIN_SPALTE,
        spalten = spalten,
        zeilen = zeilen,
        modifier = Modifier.fillMaxWidth().fillMaxSize(),
    )
}