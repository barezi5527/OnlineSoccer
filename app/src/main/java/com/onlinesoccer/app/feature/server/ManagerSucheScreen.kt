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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.ManagerSucheZeile

/** „Team-/Managersuche": Formular (Manager/Operator/Team) + Trefferliste (nur lesend).
 *  Treffer werden von der Website per POST geholt; Team-Tap öffnet den Vereins-Eintrag. */
@Composable
fun ManagerSucheScreen(
    onClose: () -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
) {
    val viewModel: ManagerSucheViewModel = hiltViewModel()
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
                "Team-/Managersuche",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SuchFormular(uiState, viewModel) }

            when {
                uiState.ladend -> item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                } }

                uiState.fehler != null -> item {
                    Text(uiState.fehler!!, color = MaterialTheme.colorScheme.error)
                }

                !uiState.gesucht -> item {
                    Text(
                        "Gib einen Manager- und/oder Team-Namen ein.",
                        Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                uiState.daten.zeilen.isEmpty() -> item {
                    Text(
                        "Kein Treffer",
                        Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> items(uiState.daten.zeilen, key = { it.managerId to it.team }) { zeile ->
                    TrefferZeile(zeile, onTeamClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuchFormular(
    uiState: ManagerSucheUiState,
    viewModel: ManagerSucheViewModel,
) {
    var operatorOffen by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val teamFocus = remember { FocusRequester() }
    val operatorWerte = listOf("and" to "und", "or" to "oder")
    val operatorLabel = operatorWerte.firstOrNull { it.first == uiState.operator }?.second ?: "und"

    fun suchenUndVerdecken() {
        keyboard?.hide()
        viewModel.suchen()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = uiState.managerText,
                onValueChange = viewModel::managerTextAendern,
                label = { Text("Manager") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { teamFocus.requestFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.teamText,
                onValueChange = viewModel::teamTextAendern,
                label = { Text("Team") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { suchenUndVerdecken() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(teamFocus),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExposedDropdownMenuBox(
                    expanded = operatorOffen,
                    onExpandedChange = { operatorOffen = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = operatorLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Operator") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = operatorOffen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = operatorOffen, onDismissRequest = { operatorOffen = false }) {
                        operatorWerte.forEach { (wert, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    operatorOffen = false
                                    viewModel.operatorWaehlen(wert)
                                },
                            )
                        }
                    }
                }
                FilledTonalButton(onClick = ::suchenUndVerdecken) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Suchen")
                }
            }
        }
    }
}

@Composable
private fun TrefferZeile(zeile: ManagerSucheZeile, onTeamClick: (Long) -> Unit) {
    val teamId = zeile.teamId
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = teamId != null) { teamId?.let(onTeamClick) },
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                zeile.manager,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            if (zeile.team.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    zeile.team,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}