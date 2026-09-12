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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.onlinesoccer.app.data.model.LaenderOption
import com.onlinesoccer.app.data.model.ManagerZeile

/** Öffentliche „Team-/Managerliste" mit Land/Liga-Filter (nur lesend). */
@Composable
fun ManagerlisteScreen(
    onClose: () -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
) {
    val viewModel: ManagerlisteViewModel = hiltViewModel()
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
                "Team-/Managerliste",
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
                onRetry = { viewModel.landWaehlen(uiState.landId) },
            )

            else -> Inhalt(uiState, viewModel::landWaehlen, viewModel::ligaWaehlen, onTeamClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Inhalt(
    uiState: ManagerlisteUiState,
    onLandWaehlen: (String) -> Unit,
    onLigaWaehlen: (String) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    val daten = uiState.daten
    val land = daten.laender.firstOrNull { it.id == uiState.landId }
    val liga = daten.ligas.firstOrNull { it.id == uiState.ligaId }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AuswahlBox(
                    Modifier.weight(1f),
                    label = "Land",
                    wert = land?.label ?: "--- Land ---",
                    optionen = daten.laender,
                    onGewaehlt = onLandWaehlen,
                )
                AuswahlBox(
                    Modifier.weight(1f),
                    label = "Liga",
                    wert = liga?.label ?: "--- Liga ---",
                    optionen = daten.ligas,
                    onGewaehlt = onLigaWaehlen,
                )
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
            items(daten.zeilen, key = { it.managerId to it.team }) { zeile ->
                ManagerZeileCard(zeile, onTeamClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuswahlBox(
    modifier: Modifier = Modifier,
    label: String,
    wert: String,
    optionen: List<LaenderOption>,
    onGewaehlt: (String) -> Unit,
) {
    var offen by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = offen,
        onExpandedChange = { offen = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = wert,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = offen) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            optionen.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label) },
                    onClick = {
                        offen = false
                        onGewaehlt(opt.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun ManagerZeileCard(zeile: ManagerZeile, onTeamClick: (Long) -> Unit) {
    val teamId = zeile.teamId
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = teamId != null) { teamId?.let(onTeamClick) },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        zeile.manager,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        zeile.team,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Kennwert("NMR 5", zeile.nmr5)
                Kennwert("NMR 20", zeile.nmr20)
                Kennwert("NMR ges.", zeile.nmrGesamt)
                Kennwert("Sperre", zeile.wechselsperre)
            }
            if (zeile.zugabgabe.isNotBlank() && zeile.zugabgabe != "---") {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Zugabgabe: ${zeile.zugabgabe}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Kennwert(label: String, wert: String) {
    if (wert.isBlank()) return
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            wert,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}