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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.LaenderOption
import com.onlinesoccer.app.data.model.TopspielerZeile

/**
 * Kurzbezeichnung der Werte-Spalte passend zur gewählten Statistik
 * (`osneu/statspieler`: `16` Opt Skill, `26` Skill, `33` Marktwert, `32` Gehalt); sonst „Wert".
 */
internal fun topspielerWertKopf(statistikId: String): String = when (statistikId) {
    "16" -> "Opt Skill"
    "26" -> "Skill"
    "33" -> "Marktwert"
    "32" -> "Gehalt"
    else -> "Wert"
}

/** Breite der Werte-Spalte – „Marktwert"/„Gehalt" brauchen Platz für Beträge. */
internal fun topspielerWertBreite(statistikId: String): Dp = when (statistikId) {
    "33", "32" -> 120.dp
    else -> 96.dp
}

private fun topspielerSpalten(wertKopf: String, wertBreite: Dp) = listOf(
    StatistikFixSpalte("#", 30.dp, TextAlign.End),
    StatistikFixSpalte("Team", 150.dp),
    StatistikFixSpalte("Alt", 40.dp, TextAlign.End),
    StatistikFixSpalte("Nat", 44.dp),
    StatistikFixSpalte(wertKopf, wertBreite, TextAlign.End),
)

private val PinKopf = "Spieler"
private const val PinBreiteDp = 148

/** Beste Spieler nach Land/Liga/Statistik/Position/Anzeige wie `osneu/statspieler`. */
@Composable
fun TopspielerScreen(
    onClose: () -> Unit,
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
    viewModel: TopspielerViewModel = hiltViewModel(),
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
                titel = "Topspieler",
                untertitel = "Beste Spieler nach Filter",
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
                StatistikDropdown("Anzeige", uiState.anzeigen, uiState.anzeige, uiState.ladend) { viewModel.anzeigeWaehlen(it); filterOffen = false }
            }
        }
        TopspielerZusammenfassung(uiState)
        val spalten = topspielerSpalten(topspielerWertKopf(uiState.statistik), topspielerWertBreite(uiState.statistik))
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
                        topspielerScrollZeile(zeile, spalten, onSpielerClick, onTeamClick)
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

private fun topspielerScrollZeile(
    zeile: TopspielerZeile,
    spalten: List<StatistikFixSpalte>,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
) = StatistikScrollZeile(
    pin = {
        StatistikSpielerZelle(zeile.name, zeile.position, zeile.pid, onSpielerClick)
    },
    zellen = {
        StatistikFixZelle(spalten[0]) { StatistikZahl(zeile.nr?.toString() ?: "–") }
        StatistikFixZelle(spalten[1]) { StatistikTeamZelle(zeile.team, zeile.teamId, onTeamClick) }
        StatistikFixZelle(spalten[2]) { StatistikZahl(zeile.alter) }
        StatistikFixZelle(spalten[3]) { FlaggenText(zeile.nation) }
        StatistikFixZelle(spalten[4]) { StatistikWert(zeile.wert) }
    },
)

/** Kompakte Zeile mit den aktuell angewendeten Filtern (übersicht bei geschlossener Filtermaske). */
@Composable
private fun TopspielerZusammenfassung(uiState: TopspielerUiState) {
    val teile = listOf(
        topspielerOptionLabel(uiState.laender, uiState.land),
        topspielerOptionLabel(uiState.ligas, uiState.liga),
        topspielerOptionLabel(uiState.statistiken, uiState.statistik),
        topspielerOptionLabel(uiState.positionen, uiState.position),
        topspielerOptionLabel(uiState.anzeigen, uiState.anzeige),
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

internal fun topspielerOptionLabel(optionen: List<LaenderOption>, id: String): String? =
    optionen.firstOrNull { it.id == id }?.label