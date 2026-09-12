package com.onlinesoccer.app.feature.server

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.FreieTeamZeile

/** Öffentliche Liste „Freie Teams" mit Länder-Filter (nur lesend). */
@Composable
fun FreieTeamsScreen(
    onClose: () -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
) {
    val viewModel: FreieTeamsViewModel = hiltViewModel()
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
                "Freie Teams",
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
                onRetry = { viewModel.lade(uiState.gewaehltesLand) },
            )

            else -> Inhalt(uiState, viewModel::lade, onTeamClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Inhalt(
    uiState: FreieTeamsUiState,
    onLandWaehlen: (String) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    var offen by remember { mutableStateOf(false) }
    val daten = uiState.daten
    val gewaehlt = daten.laender.firstOrNull { it.id == uiState.gewaehltesLand }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            daten.anzahlText?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }

            ExposedDropdownMenuBox(
                expanded = offen,
                onExpandedChange = { offen = it },
            ) {
                OutlinedTextField(
                    value = gewaehlt?.label ?: "--- Land ---",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Land") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = offen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
                    daten.laender.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.label) },
                            onClick = {
                                offen = false
                                onLandWaehlen(opt.id)
                            },
                        )
                    }
                }
            }
        }

        if (daten.zeilen.isEmpty()) {
            item {
                Text(
                    "Kein Treffer",
                    Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(daten.zeilen, key = { it.verein }) { zeile ->
                TeamZeile(zeile, onTeamClick)
            }
        }
    }
}

@Composable
internal fun TeamZeile(zeile: FreieTeamZeile, onTeamClick: (Long) -> Unit) {
    val teamId = zeile.teamId
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = teamId != null) { teamId?.let(onTeamClick) },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    zeile.verein,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    listOf(zeile.land, zeile.liga).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun FehlerBox(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.padding(start = 4.dp))
            Text("Erneut versuchen")
        }
    }
}