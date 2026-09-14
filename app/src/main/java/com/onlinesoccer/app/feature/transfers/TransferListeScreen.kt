package com.onlinesoccer.app.feature.transfers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.core.ui.theme.KACHEL_BADGE_HINTERGRUND
import com.onlinesoccer.app.core.ui.theme.KACHEL_BADGE_TEXT
import com.onlinesoccer.app.core.ui.theme.PositionsBadge
import com.onlinesoccer.app.data.model.TransferListeErgebnis
import com.onlinesoccer.app.data.model.TransferListeZeile
import com.onlinesoccer.app.feature.server.FehlerBox

/** Native „Transferliste": Filter wählen, „Anzeigen", Ergebnis blättern (nur lesend). */
@Composable
fun TransferListeScreen(
    onClose: () -> Unit = {},
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
) {
    val viewModel: TransferListeViewModel = hiltViewModel()
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
                "Transferliste",
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
                onRetry = viewModel::ladeFormular,
            )

            else -> Inhalt(
                uiState = uiState,
                onFilterWaehlen = viewModel::filterWaehlen,
                onAnzeigen = viewModel::anzeigen,
                onSeite = viewModel::seiteWaehlen,
                onSpielerClick = onSpielerClick,
                onTeamClick = onTeamClick,
            )
        }
    }
}

@Composable
private fun Inhalt(
    uiState: TransferListeUiState,
    onFilterWaehlen: (String, String) -> Unit,
    onAnzeigen: () -> Unit,
    onSeite: (Int) -> Unit,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    val ergebnis = uiState.ergebnis

    Column(Modifier.fillMaxSize()) {
        TransferFilterPanel(
            filter = ergebnis.filter,
            wahl = uiState.wahl,
            onFilterWaehlen = onFilterWaehlen,
            onAnzeigen = onAnzeigen,
        )

        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ergebnis.hinweis?.let {
            item {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }

        when {
            !ergebnis.gesucht -> item {
                Text(
                    "Wähle oben Kriterien und tippe „Anzeigen“.",
                    Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ergebnis.zeilen.isEmpty() -> item {
                Text(
                    "Keine Einträge",
                    Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> items(ergebnis.zeilen, key = { it.spielerId }) { zeile ->
                TransferListeKarte(zeile, onSpielerClick, onTeamClick)
            }
        }

        if (ergebnis.gesucht && ergebnis.gesamtSeiten > 1) {
            item {
                PaginationZeile(
                    ergebnis = ergebnis,
                    aktuelleSeite = uiState.seite,
                    onSeite = onSeite,
                )
            }
        }
        }
    }
}

@Composable
private fun PaginationZeile(
    ergebnis: TransferListeErgebnis,
    aktuelleSeite: Int,
    onSeite: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            enabled = aktuelleSeite > 1,
            onClick = { onSeite(aktuelleSeite - 1) },
        ) {
            Text("‹ Zurück")
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                "Seite $aktuelleSeite von ${ergebnis.gesamtSeiten}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${ergebnis.treffer} Treffer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            enabled = aktuelleSeite < ergebnis.gesamtSeiten,
            onClick = { onSeite(aktuelleSeite + 1) },
        ) {
            Text("Weiter ›")
        }
    }
}

@Composable
private fun TransferListeKarte(
    zeile: TransferListeZeile,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PositionsBadge(zeile.position)
                Spacer(Modifier.padding(start = 8.dp))
                Text(
                    zeile.name,
                    Modifier
                        .weight(1f)
                        .clickable { onSpielerClick(zeile.spielerId) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.padding(start = 8.dp))
                TransferBadge(zeile.status, zeile.details)
            }

            val kern = listOf(zeile.alter, zeile.position, zeile.land).filter { it.isNotBlank() }
            if (kern.isNotEmpty()) {
                Text(
                    kern.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val skillZeilen = listOfNotNull(
                zeile.skill.takeIf { it.isNotBlank() }?.let { "Skill $it" },
                zeile.optSkill.takeIf { it.isNotBlank() }?.let { "Opt. Skill $it" },
            )
            if (skillZeilen.isNotEmpty()) {
                Text(
                    skillZeilen.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (zeile.teamId != null) {
                Text(
                    zeile.team,
                    Modifier.clickable { zeile.teamId?.let(onTeamClick) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            } else if (zeile.team.isNotBlank()) {
                Text(
                    zeile.team,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val preise = listOfNotNull(
                zeile.abloese.takeIf { it.isNotBlank() }?.let { "Ablöse $it" },
                zeile.datum.takeIf { it.isNotBlank() }?.let { "ZAT $it" },
            )
            if (preise.isNotEmpty()) {
                Text(
                    preise.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TransferBadge(status: String, details: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(status, details).filter { it.isNotBlank() }.forEach { text ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = KACHEL_BADGE_HINTERGRUND,
                contentColor = KACHEL_BADGE_TEXT,
            ) {
                Text(
                    text,
                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}