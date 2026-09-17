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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
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
import com.onlinesoccer.app.data.model.VmSetzenEintrag

/** „Auf den VM setzen" (`vmsetzen.php`): eigene Spieler samt Startpreis-Staffeln, mit Bestätigung auf den VM setzen. */
@Composable
fun VmSetzenScreen(
    onClose: () -> Unit = {},
    onSpielerClick: (Long) -> Unit = {},
    demo: Boolean = false,
) {
    val viewModel: VmSetzenViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()

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
                "Auf den VM setzen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        when {
            uiState.ladend -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.fehler != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.fehler!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.padding(top = 12.dp))
                    FilledTonalButton(onClick = viewModel::lade) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.padding(start = 4.dp))
                        Text("Erneut versuchen")
                    }
                }
            }

            uiState.ergebnis.eintraege.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Keine Spieler verfügbar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        "Die unten gelisteten Spieler kann dein Team auf den Versteigerungsmarkt setzen. "
                            + "Tippe auf das ⋮-Menü eines Spielers und bestätige den Startpreis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(uiState.ergebnis.eintraege, key = { it.spielerId }) { eintrag ->
                    VmSetzenKarte(eintrag, onSpielerClick, viewModel::aufVmSetzenKlick, demo)
                }
            }
        }
    }

    VmSetzenDialog(
        zustand = dialog,
        onBestatigen = viewModel::aufVmSetzen,
        onAbbrechen = viewModel::dialogSchliessen,
    )
}

@Composable
private fun VmSetzenKarte(
    eintrag: VmSetzenEintrag,
    onSpielerClick: (Long) -> Unit,
    onAufVmSetzen: (VmSetzenEintrag) -> Unit,
    demo: Boolean = false,
) {
    var menuOffen by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    eintrag.name,
                    Modifier
                        .weight(1f)
                        .clickable { onSpielerClick(eintrag.spielerId) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!demo) {
                    Box {
                        IconButton(onClick = { menuOffen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Aktionen")
                        }
                        DropdownMenu(expanded = menuOffen, onDismissRequest = { menuOffen = false }) {
                            DropdownMenuItem(
                                text = { Text("Auf den VM setzen") },
                                onClick = {
                                    menuOffen = false
                                    onAufVmSetzen(eintrag)
                                },
                            )
                        }
                    }
                }
            }

            val kern = listOf(eintrag.alter, eintrag.land, eintrag.u.takeIf { it.isNotBlank() }?.let { "U $it" })
                .filterNot { it.isNullOrBlank() }
            if (kern.isNotEmpty()) {
                Text(
                    kern.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val skill = listOfNotNull(
                eintrag.skill.takeIf { it.isNotBlank() }?.let { "Skill $it" },
                eintrag.opti.takeIf { it.isNotBlank() }?.let { "Opti $it" },
            ).joinToString(" · ")
            if (skill.isNotBlank()) {
                Text(
                    skill,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            val finanz = listOfNotNull(
                eintrag.marktwert.takeIf { it.isNotBlank() }?.let { "Marktwert $it" },
                eintrag.gebuehr.takeIf { it.isNotBlank() }?.let { "Gebühr $it" },
            ).joinToString(" · ")
            if (finanz.isNotBlank()) {
                Text(
                    finanz,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (eintrag.startpreise.isNotEmpty()) {
                val min = eintrag.startpreise.first().label
                val max = eintrag.startpreise.last().label
                Text(
                    "Startpreis $min – $max €",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VmSetzenDialog(
    zustand: VmSetzenDialogState,
    onBestatigen: (String) -> Unit,
    onAbbrechen: () -> Unit,
) {
    when (zustand) {
        VmSetzenDialogState.Verborgen -> Unit

        is VmSetzenDialogState.Bereit -> {
            val eintrag = zustand.eintrag
            var auswahl by remember {
                mutableStateOf(eintrag.startpreise.firstOrNull()?.wert ?: "")
            }
            var startpreisOffen by remember { mutableStateOf(false) }
            val gewaehlt = eintrag.startpreise.firstOrNull { it.wert == auswahl }
            AlertDialog(
                onDismissRequest = onAbbrechen,
                title = { Text("Auf den VM setzen") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${eintrag.name} · Skill ${eintrag.skill}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "Marktwert ${eintrag.marktwert.ifBlank { "–" }} · Gebühr ${eintrag.gebuehr.ifBlank { "–" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (eintrag.startpreise.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = startpreisOffen,
                                onExpandedChange = { startpreisOffen = it },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                OutlinedTextField(
                                    value = gewaehlt?.label ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Startpreis") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startpreisOffen) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                )
                                ExposedDropdownMenu(
                                    expanded = startpreisOffen,
                                    onDismissRequest = { startpreisOffen = false },
                                ) {
                                    eintrag.startpreise.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text("${option.label} €") },
                                            onClick = {
                                                startpreisOffen = false
                                                auswahl = option.wert
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Das Einstellen ist verbindlich und wird sofort an die Website gesendet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onBestatigen(auswahl) },
                        enabled = auswahl.isNotBlank(),
                    ) {
                        Text("Auf den VM setzen")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onAbbrechen) { Text("Abbrechen") }
                },
            )
        }

        VmSetzenDialogState.Sende -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Auf den VM setzen") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Wird an die Website gesendet…")
                }
            },
            confirmButton = {},
        )

        is VmSetzenDialogState.Ergebnis -> AlertDialog(
            onDismissRequest = onAbbrechen,
            title = { Text(if (zustand.erfolg) "Auf den VM gesetzt" else "Nicht möglich") },
            text = { Text(zustand.meldung.ifBlank { if (zustand.erfolg) "Spieler wurde auf den Versteigerungsmarkt gesetzt." else "Unbekannter Fehler." }) },
            confirmButton = {
                TextButton(onClick = onAbbrechen) { Text("OK") }
            },
        )
    }
}