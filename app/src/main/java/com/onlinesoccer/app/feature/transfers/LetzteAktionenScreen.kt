package com.onlinesoccer.app.feature.transfers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.LetzteAktionenZeile
import com.onlinesoccer.app.feature.server.FehlerBox

/**
 * Native „Letzten …"-Seiten unter „Transfers" (`Letzte Transfers`, `Letzte Leihen`,
 * `Letzte VM-Käufe`, `Letzte TM-Käufe`, `Letzte Schnelltransfers`).
 */
@Composable
fun LetzteAktionenScreen(
    onClose: () -> Unit = {},
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
) {
    val viewModel: LetzteAktionenViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
            }
            Text(
                viewModel.art.titel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        when {
            uiState.ladend -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.fehler != null -> FehlerBox(
                uiState.fehler!!,
                onRetry = viewModel::lade,
            )

            else -> Inhalt(uiState.ergebnis.zeilen, onSpielerClick, onTeamClick)
        }
    }
}

@Composable
private fun Inhalt(
    zeilen: List<LetzteAktionenZeile>,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (zeilen.isEmpty()) {
            item {
                Text(
                    "Keine Einträge.",
                    Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(zeilen, key = { it.spielerId }) { zeile ->
                LetzteAktionKarte(zeile, onSpielerClick, onTeamClick)
            }
        }
    }
}

@Composable
private fun LetzteAktionKarte(
    zeile: LetzteAktionenZeile,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (zeile.datum.isNotBlank()) {
                Text(
                    zeile.datum,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val teamzeilen = when {
                zeile.von.isNotBlank() && zeile.zu.isNotBlank() -> listOfNotNull(
                    zeile.von to zeile.vonId,
                    zeile.zu to zeile.zuId,
                )

                zeile.team.isNotBlank() -> listOfNotNull(zeile.team to zeile.teamId)
                else -> emptyList()
            }
            if (teamzeilen.size == 2) {
                Text(
                    "${teamzeilen[0].first} → ${teamzeilen[1].first}",
                    Modifier.clickable {
                        teamzeilen.firstNotNullOfOrNull { it.second }?.let(onTeamClick)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            } else if (zeile.team.isNotBlank() || zeile.ziel.isNotBlank()) {
                Text(
                    listOf(zeile.team, zeile.ziel).filter { it.isNotBlank() }.joinToString(" · "),
                    Modifier.then(
                        if (teamzeilen.isNotEmpty()) {
                            Modifier.clickable { teamzeilen.firstNotNullOfOrNull { it.second }?.let(onTeamClick) }
                        } else {
                            Modifier
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (teamzeilen.isNotEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Medium,
                )
            }

            if (zeile.spieler.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        zeile.spieler,
                        Modifier
                            .weight(1f)
                            .clickable { onSpielerClick(zeile.spielerId) },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    val rechts = listOfNotNull(
                        zeile.position.takeIf { it.isNotBlank() },
                        zeile.betrag.takeIf { it.isNotBlank() }?.let { waehrung(it) },
                    )
                    if (rechts.isNotEmpty()) {
                        Text(
                            rechts.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            val details = listOfNotNull(
                zeile.dauer.takeIf { it.isNotBlank() }?.let { "Dauer $it" },
                zeile.anmerkung.takeIf { it.isNotBlank() },
            )
            if (details.isNotEmpty()) {
                Text(
                    details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Ergänzt die Währungsangabe, falls der Server sie nicht schon enthält. */
private fun waehrung(betrag: String): String =
    if (betrag.contains("Euro")) betrag else "$betrag Euro"