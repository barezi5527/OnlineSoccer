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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.core.ui.theme.PositionsBadge
import com.onlinesoccer.app.data.model.VersteigerungsmarktEintrag
import com.onlinesoccer.app.feature.server.FehlerBox

/** Native Darstellung des „Versteigerungsmarkts" (`viewvm.php`): Filter wählen, Einträge anzeigen und geboten. */
@Composable
fun VersteigerungsmarktScreen(
    onClose: () -> Unit = {},
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
    demo: Boolean = false,
) {
    val viewModel: VersteigerungsmarktViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bietDialog by viewModel.bietDialog.collectAsStateWithLifecycle()

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
                "Versteigerungsmarkt",
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
                onSpielerClick = onSpielerClick,
                onTeamClick = onTeamClick,
                onBieten = viewModel::onBietenKlick,
                bietDialog = bietDialog,
                onBietenAbgeben = viewModel::vmGebotAbgeben,
                onBietDialogSchliessen = viewModel::bietDialogSchliessen,
                demo = demo,
            )
        }
    }
}

@Composable
private fun Inhalt(
    uiState: VersteigerungsmarktUiState,
    onFilterWaehlen: (String, String) -> Unit,
    onAnzeigen: () -> Unit,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
    onBieten: (VersteigerungsmarktEintrag) -> Unit,
    bietDialog: VmBietDialogState,
    onBietenAbgeben: (String?) -> Unit,
    onBietDialogSchliessen: () -> Unit,
    demo: Boolean = false,
) {
    val ergebnis = uiState.ergebnis

    Column(Modifier.fillMaxSize()) {
        TransferFilterPanel(
            filter = ergebnis.filter,
            wahl = uiState.wahl,
            onFilterWaehlen = onFilterWaehlen,
            onAnzeigen = onAnzeigen,
            anzeigenLabel = "Spieler anzeigen",
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

            if (!ergebnis.gesucht) {
                item {
                    Text(
                        "Wähle oben Kriterien und tippe „Spieler anzeigen“.",
                        Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (ergebnis.eintraege.isEmpty()) {
                item {
                    Text(
                        "Keine Einträge",
                        Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(ergebnis.eintraege, key = { it.spielerId }) { eintrag ->
                    VersteigerungsmarktKarte(
                        eintrag,
                        onSpielerClick,
                        onTeamClick,
                        onBieten,
                        demo,
                    )
                }
            }
        }
    }

    VmBietDialog(
        zustand = bietDialog,
        onBestatigen = onBietenAbgeben,
        onAbbrechen = onBietDialogSchliessen,
    )
}

@Composable
private fun VersteigerungsmarktKarte(
    eintrag: VersteigerungsmarktEintrag,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
    onBieten: (VersteigerungsmarktEintrag) -> Unit,
    demo: Boolean = false,
) {
    var menuOffen by remember { mutableStateOf(false) }
    val kern = listOf(eintrag.alter, eintrag.position, eintrag.land).filter { it.isNotBlank() }
    val skillZeilen = listOfNotNull(
        eintrag.skill.takeIf { it.isNotBlank() }?.let { "Skill $it" },
        eintrag.optSkill.takeIf { it.isNotBlank() }?.let { "Opt. Skill $it" },
    )
    val finanz = listOfNotNull(
        eintrag.gebot.takeIf { it.isNotBlank() }?.let { "Gebot $it" },
        eintrag.prozentMw.takeIf { it.isNotBlank() }?.let { "$it % MW" },
        eintrag.anzahl.takeIf { it.isNotBlank() }?.let { "Gebote: $it" },
    )
    val gesamtZeile = listOfNotNull(
        eintrag.gehalt.takeIf { it.isNotBlank() }?.let { "Gehalt $it" },
        eintrag.dauer.takeIf { it.isNotBlank() }?.let { "bis $it" },
    ).joinToString(" · ")

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PositionsBadge(eintrag.position)
                Spacer(Modifier.padding(start = 8.dp))
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
                                text = { Text("Gebot abgeben") },
                                onClick = {
                                    menuOffen = false
                                    onBieten(eintrag)
                                },
                            )
                        }
                    }
                }
            }
            if (kern.isNotEmpty()) {
                Text(
                    kern.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (skillZeilen.isNotEmpty()) {
                Text(
                    skillZeilen.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (finanz.isNotEmpty()) {
                Text(
                    finanz.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (eintrag.bieter.isNotBlank()) {
                if (eintrag.bieterTeamId != null) {
                    Text(
                        "von " + eintrag.bieter,
                        Modifier.clickable { eintrag.bieterTeamId?.let(onTeamClick) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Start,
                    )
                } else {
                    Text(
                        eintrag.bieter,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (gesamtZeile.isNotEmpty()) {
                Text(
                    gesamtZeile,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VmBietDialog(
    zustand: VmBietDialogState,
    onBestatigen: (String?) -> Unit,
    onAbbrechen: () -> Unit,
) {
    when (zustand) {
        VmBietDialogState.Verborgen -> Unit

        VmBietDialogState.Laedt -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Gebot abgeben") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Lade Gebots-Informationen…")
                }
            },
            confirmButton = {},
        )

        is VmBietDialogState.Bereit -> {
            val info = zustand.info
            val hatBetragFeld = info?.betragName?.isNotBlank() == true
            var betrag by remember { mutableStateOf(info?.betragWert.orEmpty()) }
            AlertDialog(
                onDismissRequest = onAbbrechen,
                title = { Text("Gebot für ${info?.name ?: "den Spieler"}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (info != null) {
                            val zeilen = listOfNotNull(
                                "Höchstgebot: ${info.hoechstgebot.ifBlank { "–" }}",
                                "Marktwert: ${info.marktwert.ifBlank { "–" }}",
                                "Gehalt: ${info.gehalt.ifBlank { "–" }}",
                                "Bieter: ${info.bieter.ifBlank { "–" }}",
                                info.angeboteBis.takeIf { it.isNotBlank() }?.let { "Angebote bis: $it" },
                            )
                            zeilen.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                        if (hatBetragFeld) {
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = betrag,
                                onValueChange = { betrag = it },
                                label = { Text("Dein Gebot") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                "Gib den Betrag ein, den du bieten möchtest.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Ein bestätigtes Gebot wird sofort an die Website gesendet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onBestatigen(betrag) },
                        enabled = if (hatBetragFeld) betrag.isNotBlank() else info?.submitName?.isNotBlank() == true,
                    ) {
                        Text("Gebot verbindlich abgeben")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onAbbrechen) { Text("Abbrechen") }
                },
            )
        }

        is VmBietDialogState.Ergebnis -> AlertDialog(
            onDismissRequest = onAbbrechen,
            title = { Text(if (zustand.erfolg) "Gebot gesendet" else "Nicht möglich") },
            text = { Text(zustand.meldung.ifBlank { if (zustand.erfolg) "Gebot wurde gesendet." else "Unbekannter Fehler." }) },
            confirmButton = {
                TextButton(onClick = onAbbrechen) { Text("OK") }
            },
        )
    }
}