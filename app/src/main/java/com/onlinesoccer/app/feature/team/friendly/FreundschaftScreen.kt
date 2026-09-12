package com.onlinesoccer.app.feature.team.friendly

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.onlinesoccer.app.data.model.FreundschaftOption
import com.onlinesoccer.app.data.model.FreundschaftSpiel

@Composable
fun FreundschaftScreen(
    viewModel: FreundschaftViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        state.ladend && state.daten == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.daten == null -> Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.fehler ?: "Keine Freundschaftsspiele gefunden.", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = viewModel::lade) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text("Erneut versuchen")
            }
        }
        else -> FreundschaftInhalt(state, viewModel)
    }
}

@Composable
private fun FreundschaftInhalt(state: FreundschaftUiState, viewModel: FreundschaftViewModel) {
    val daten = state.daten ?: return
    var blindZat by remember { mutableStateOf<FreundschaftOption?>(null) }
    var land by remember { mutableStateOf<FreundschaftOption?>(null) }
    var liga by remember { mutableStateOf<FreundschaftOption?>(null) }
    var doppelt by remember { mutableStateOf(false) }
    var ausgewaehlteZats by remember { mutableStateOf(emptySet<String>()) }
    var loeschen by remember { mutableStateOf(false) }
    var storno by remember { mutableStateOf<FreundschaftSpiel?>(null) }
    val gueltigerBlindZat = blindZat?.takeIf { option -> daten.blindZats.any { it.value == option.value } }
    val gueltigeZats = ausgewaehlteZats.intersect(daten.reservierbareZats.map { it.value }.toSet())

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Freundschaftsspiele", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = viewModel::lade, enabled = !state.aktion && !state.ladend) { Text("Aktualisieren") }
        }
        state.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.meldung?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        daten.meldungen.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }

        if (daten.spiele.isNotEmpty()) {
            Abschnitt("Gebuchte Freundschaftsspiele")
            daten.spiele.forEach { spiel ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(spiel.gegner, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (spiel.zat != null || spiel.details.isNotBlank()) Text(
                            listOfNotNull(spiel.zat?.let { "ZAT $it" }, spiel.details.takeIf { it.isNotBlank() }).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (spiel.stornierbar) {
                            TextButton(onClick = { storno = spiel }) { Text("Stornieren", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }

        Abschnitt("Blinde Einladung")
        AuswahlButton("ZAT auswählen", gueltigerBlindZat, daten.blindZats) { blindZat = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = doppelt, onCheckedChange = { doppelt = it })
            Text("Doppelbelegung erlauben", style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = { gueltigerBlindZat?.let { viewModel.blindeEinladung(it.value, doppelt) } },
            enabled = gueltigerBlindZat != null && !state.aktion,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Einladung senden") }

        Abschnitt("ZAT reservieren")
        if (daten.reservierbareZats.isEmpty()) {
            Text("Keine reservierbaren ZATs gefunden.", style = MaterialTheme.typography.bodySmall)
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                daten.reservierbareZats.forEach { option ->
                    FilterChip(
                        selected = option.value in gueltigeZats,
                        onClick = { ausgewaehlteZats = if (option.value in ausgewaehlteZats) ausgewaehlteZats - option.value else ausgewaehlteZats + option.value },
                        label = { Text(option.label) },
                    )
                }
            }
            Button(
                onClick = { viewModel.reserviere(gueltigeZats.toList()) },
                enabled = gueltigeZats.isNotEmpty() && !state.aktion,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ZATs reservieren") }
        }
        if (daten.reservierteZats.isNotEmpty()) {
            Text("Reserviert: ${daten.reservierteZats.joinToString { it.label }}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { loeschen = true }, enabled = !state.aktion) {
                Text("Alle Reservierungen löschen", color = MaterialTheme.colorScheme.error)
            }
        }

        if (daten.teams.isNotEmpty() || daten.laender.isNotEmpty() || daten.ligen.isNotEmpty()) {
            Abschnitt("Teams suchen")
            if (daten.laender.isNotEmpty()) AuswahlButton("Land auswählen", land, daten.laender) { land = it }
            if (daten.ligen.isNotEmpty()) AuswahlButton("Liga auswählen", liga, daten.ligen) { liga = it }
            Button(
                onClick = { viewModel.zeigeTeams(land?.value.orEmpty(), liga?.value.orEmpty()) },
                enabled = !state.aktion && (land != null || liga != null),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Teams anzeigen") }
            daten.teams.take(40).forEach { team -> Text(team.label, style = MaterialTheme.typography.bodySmall) }
        }
    }

    if (loeschen) Bestatigung(
        titel = "Reservierungen löschen",
        text = "Alle reservierten Freundschaftsspiel-ZATs werden gelöscht.",
        onDismiss = { loeschen = false },
        onConfirm = { loeschen = false; viewModel.loescheReservierungen() },
    )
    storno?.let { spiel -> Bestatigung(
        titel = "Freundschaftsspiel stornieren",
        text = "Das Spiel gegen ${spiel.gegner} wird storniert.",
        onDismiss = { storno = null },
        onConfirm = { storno = null; spiel.id?.let(viewModel::storniere) },
    ) }
}

@Composable
private fun Abschnitt(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun AuswahlButton(
    placeholder: String,
    selected: FreundschaftOption?,
    options: List<FreundschaftOption>,
    onSelect: (FreundschaftOption) -> Unit,
) {
    var offen by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { offen = true }, modifier = Modifier.fillMaxWidth()) { Text(selected?.label ?: placeholder) }
        DropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.label) }, onClick = { offen = false; onSelect(option) })
            }
        }
    }
}

@Composable
private fun Bestatigung(titel: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titel) },
        text = { Text(text) },
        confirmButton = { Button(onClick = onConfirm) { Text("Bestätigen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
