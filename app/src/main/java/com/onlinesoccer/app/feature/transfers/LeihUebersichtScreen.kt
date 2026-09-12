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
import com.onlinesoccer.app.data.model.LeihUebersichtZeile
import com.onlinesoccer.app.feature.server.FehlerBox

/** Native „Leihspieler Übersicht" (`viewleih.php`): verliehene und geliehene Spieler. */
@Composable
fun LeihUebersichtScreen(
    onClose: () -> Unit = {},
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
) {
    val viewModel: LeihUebersichtViewModel = hiltViewModel()
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
                "Leihspieler Übersicht",
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

            else -> Inhalt(
                verliehen = uiState.ergebnis.verliehen,
                geliehen = uiState.ergebnis.geliehen,
                onSpielerClick = onSpielerClick,
                onTeamClick = onTeamClick,
            )
        }
    }
}

@Composable
private fun Inhalt(
    verliehen: List<LeihUebersichtZeile>,
    geliehen: List<LeihUebersichtZeile>,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (verliehen.isEmpty() && geliehen.isEmpty()) {
            item {
                Text(
                    "Keine verliehenen oder geliehenen Spieler.",
                    Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@LazyColumn
        }

        if (verliehen.isNotEmpty()) {
            item { AbschnittTitel("Verliehene Spieler (${verliehen.size})") }
            items(verliehen, key = { it.spielerId }) { zeile ->
                LeihKarte(zeile, onSpielerClick, onTeamClick)
            }
        }

        if (geliehen.isNotEmpty()) {
            item { AbschnittTitel("Geliehene Spieler (${geliehen.size})") }
            items(geliehen, key = { it.spielerId }) { zeile ->
                LeihKarte(zeile, onSpielerClick, onTeamClick)
            }
        }
    }
}

@Composable
private fun AbschnittTitel(text: String) {
    Text(
        text,
        Modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun LeihKarte(
    zeile: LeihUebersichtZeile,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    zeile.name,
                    Modifier
                        .weight(1f)
                        .clickable { onSpielerClick(zeile.spielerId) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
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
                )
            }

            val leihdetails = listOfNotNull(
                zeile.dauer.takeIf { it.isNotBlank() }?.let { "Leihdauer $it" },
                zeile.gehalt.takeIf { it.isNotBlank() }?.let { "Gehalt $it" },
                zeile.leihgebuehr.takeIf { it.isNotBlank() }?.let { "Leihgebühr $it" },
            )
            if (leihdetails.isNotEmpty()) {
                Text(
                    leihdetails.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (zeile.leihclubId != null) {
                Text(
                    zeile.leihclub,
                    Modifier.clickable { zeile.leihclubId?.let(onTeamClick) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            } else if (zeile.leihclub.isNotBlank()) {
                Text(
                    zeile.leihclub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}