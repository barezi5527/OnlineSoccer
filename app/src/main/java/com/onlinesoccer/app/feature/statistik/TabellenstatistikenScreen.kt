package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.RekordZeile

/** Tabellenstatistiken (`osneu/userstatistics`) – Rekorde der Ligageschichte. */
@Composable
fun TabellenstatistikenScreen(
    onClose: () -> Unit,
    onTeamClick: (Long) -> Unit,
    viewModel: TabellenstatistikenViewModel = hiltViewModel(),
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
                    titel = "Tabellenstatistiken",
                    untertitel = "Rekorde aus der Welt der Ligen",
                    onClose = onClose,
                )
            }
            uiState.daten.abschnitte.forEach { abschnitt ->
                item { RekordabschnittKarte(abschnitt.titel, abschnitt.untertitel, abschnitt.zeilen, onTeamClick) }
            }
            if (uiState.daten.abschnitte.isEmpty()) {
                item {
                    Text(
                        "Keine Daten.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RekordabschnittKarte(
    titel: String,
    untertitel: String?,
    zeilen: List<RekordZeile>,
    onTeamClick: (Long) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                titel,
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (!untertitel.isNullOrBlank()) {
                Text(
                    untertitel,
                    Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            zeilen.forEachIndexed { index, zeile ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 12.dp))
                val klickbar = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .then(
                        if (zeile.teamId != null) {
                            Modifier.clickable { onTeamClick(zeile.teamId) }
                        } else {
                            Modifier
                        },
                    )
                Column(klickbar) {
                    val teamtext = zeile.text
                    Text(
                        teamtext,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (zeile.teamId != null) FontWeight.Bold else FontWeight.Normal,
                    )
                    if (zeile.teamId != null) {
                        Text(
                            "Zum Team ›",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}