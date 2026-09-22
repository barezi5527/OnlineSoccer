package com.onlinesoccer.app.feature.pm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.PmNachricht
import com.onlinesoccer.app.data.model.PmEmpfaengerVorschlag

@Composable
fun PmScreen(
    onClose: () -> Unit,
    viewModel: PmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var loeschenBestaetigen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                if (uiState.detail != null) viewModel.zurueck() else onClose()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
            }
            Text("Nachrichten", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (uiState.detail == null && uiState.neueNachricht == null) {
                TextButton(onClick = viewModel::starteNeueNachricht) { Text("Neue Nachricht") }
            }
            if (uiState.detail != null && uiState.antwort == null) {
                TextButton(onClick = viewModel::starteAntwort, enabled = !uiState.antwortLadend) {
                    Text(if (uiState.antwortLadend) "Lade …" else "Antworten")
                }
                TextButton(onClick = { loeschenBestaetigen = true }, enabled = !uiState.detailLadend) {
                    Text(if (uiState.detailLadend) "Lösche …" else "Löschen")
                }
            }
        }

        when {
            uiState.neueNachricht != null -> NeueNachrichtAnsicht(
                uiState = uiState,
                onEmpfaenger = viewModel::sucheEmpfaenger,
                onWaehleEmpfaenger = viewModel::waehleEmpfaenger,
                onBetreff = viewModel::setNeueBetreff,
                onText = viewModel::setNeueText,
                onSenden = viewModel::sendeNeueNachricht,
                onAbbrechen = viewModel::abbrechenNeueNachricht,
            )

            uiState.detail != null -> DetailAnsicht(
                uiState = uiState,
                onSenden = viewModel::sendeAntwort,
                onAbbrechenAntwort = viewModel::abbrechenAntwort,
                onBetreff = viewModel::setAntwortBetreff,
                onText = viewModel::setAntwortText,
            )

            uiState.detailLadend -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            uiState.ladende -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.fehler != null && uiState.detail == null -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(uiState.fehler.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
                Button(onClick = viewModel::lade) { Text("Erneut laden") }
            }

            uiState.liste.isEmpty() && uiState.fehler != null -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    uiState.fehler.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
                Button(onClick = viewModel::lade) { Text("Erneut laden") }
            }

            uiState.liste.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Keine Nachrichten.")
            }

            else -> {
                val posteingang = uiState.liste.filter { it.ausgehend.not() }
                val gesendete = uiState.liste.filter { it.ausgehend }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (posteingang.isNotEmpty()) {
                        item(key = "header-eingang") { AbschnittHeader("Posteingang") }
                        items(posteingang, key = { it.id }) { nachricht ->
                            PmZeile(nachricht = nachricht, onOpen = { viewModel.oeffne(nachricht.id) })
                        }
                    }
                    if (gesendete.isNotEmpty()) {
                        item(key = "header-ausgang") { AbschnittHeader("Gesendete Nachrichten") }
                        items(gesendete, key = { it.id }) { nachricht ->
                            PmZeile(nachricht = nachricht, onOpen = { viewModel.oeffne(nachricht.id) })
                        }
                    }
                }
            }
        }
    }
    if (loeschenBestaetigen) {
        AlertDialog(
            onDismissRequest = { loeschenBestaetigen = false },
            title = { Text("Nachricht löschen?") },
            text = { Text("Diese Nachricht wird dauerhaft gelöscht.") },
            confirmButton = {
                TextButton(onClick = { loeschenBestaetigen = false; viewModel.loescheDetail() }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { loeschenBestaetigen = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun AbschnittHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun PmZeile(nachricht: PmNachricht, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (nachricht.gelesen) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                nachricht.sender?.let { "Von: $it" }
                    ?: nachricht.empfänger?.let { "An: $it" }
                    ?: "Nachricht #${nachricht.id}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                nachricht.betreff ?: "Ohne Betreff",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (nachricht.gelesen) FontWeight.Normal else FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            nachricht.datum?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailAnsicht(
    uiState: PmUiState,
    onSenden: () -> Unit,
    onAbbrechenAntwort: () -> Unit,
    onBetreff: (String) -> Unit,
    onText: (String) -> Unit,
) {
    val detail = uiState.detail
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            detail?.nachricht?.betreff?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        item {
            detail?.nachricht?.sender?.let {
                Text("Von: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (uiState.antwort != null) {
            item {
                OutlinedTextField(
                    value = uiState.antwort.betreff,
                    onValueChange = onBetreff,
                    label = { Text("Betreff") },
                    singleLine = true,
                    enabled = !uiState.sendend,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.antwort.text,
                    onValueChange = onText,
                    label = { Text("Antwort") },
                    minLines = 8,
                    enabled = !uiState.sendend,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSenden, enabled = !uiState.sendend, modifier = Modifier.weight(1f)) {
                        Text(if (uiState.sendend) "Sende …" else "Senden")
                    }
                    TextButton(onClick = onAbbrechenAntwort, modifier = Modifier.weight(1f)) { Text("Abbrechen") }
                }
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        detail?.body ?: "Nachricht leer oder nicht lesbar.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
        uiState.meldung?.let { meldung -> item { Text(meldung, color = MaterialTheme.colorScheme.primary) } }
        uiState.fehler?.let { fehler -> item { Text(fehler, color = MaterialTheme.colorScheme.error) } }
    }
}

@Composable
private fun NeueNachrichtAnsicht(
    uiState: PmUiState,
    onEmpfaenger: (String) -> Unit,
    onWaehleEmpfaenger: (PmEmpfaengerVorschlag) -> Unit,
    onBetreff: (String) -> Unit,
    onText: (String) -> Unit,
    onSenden: () -> Unit,
    onAbbrechen: () -> Unit,
) {
    val formular = uiState.neueNachricht ?: return
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(
                value = formular.empfaenger,
                onValueChange = onEmpfaenger,
                label = { Text("Empfänger (ab 3 Zeichen suchen)") },
                singleLine = true,
                enabled = !uiState.sendend,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (uiState.empfaengerSucheLadend) {
            item { Text("Suche …", style = MaterialTheme.typography.bodySmall) }
        } else {
            items(uiState.empfaengerSuche, key = { it.id }) { vorschlag ->
                Card(onClick = { onWaehleEmpfaenger(vorschlag) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        vorschlag.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = formular.betreff,
                onValueChange = onBetreff,
                label = { Text("Betreff") },
                singleLine = true,
                enabled = !uiState.sendend,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = formular.text,
                onValueChange = onText,
                label = { Text("Nachricht") },
                minLines = 8,
                enabled = !uiState.sendend,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSenden, enabled = !uiState.sendend, modifier = Modifier.weight(1f)) {
                    Text(if (uiState.sendend) "Sende …" else "Senden")
                }
                TextButton(onClick = onAbbrechen, modifier = Modifier.weight(1f)) { Text("Abbrechen") }
            }
        }
        uiState.meldung?.let { meldung -> item { Text(meldung, color = MaterialTheme.colorScheme.primary) } }
        uiState.fehler?.let { fehler -> item { Text(fehler, color = MaterialTheme.colorScheme.error) } }
    }
}
