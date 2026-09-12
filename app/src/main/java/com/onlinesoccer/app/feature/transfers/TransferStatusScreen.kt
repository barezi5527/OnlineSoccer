package com.onlinesoccer.app.feature.transfers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.onlinesoccer.app.data.model.TransferDetail
import com.onlinesoccer.app.data.model.TransferStatus
import com.onlinesoccer.app.data.model.TransferStatusZeile
import com.onlinesoccer.app.feature.server.FehlerBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferStatusScreen(
    onBack: () -> Unit = {},
    viewModel: TransferStatusViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transferstatus") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    Icon(
                        Icons.AutoMirrored.Filled.FactCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            TransferStatusHinweis()

            when {
                uiState.ladend -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.fehler != null -> FehlerBox(
                    uiState.fehler ?: "Unbekannter Fehler",
                    onRetry = viewModel::lade,
                )
                else -> TransferStatusInhalt(
                    uiState = uiState,
                    onStatusWaehlen = viewModel::statusWaehlen,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        TransferStatusDialog(
            dialog = dialog,
            speichernd = uiState.speichernd,
            onBestaetigen = viewModel::bestaetigen,
            onSchliessen = viewModel::dialogSchliessen,
        )
    }
}

@Composable
private fun TransferStatusHinweis() {
    Text(
        text = "Hier legst du fest, ob ein Spieler im Kader bleibt oder auf der Transferliste steht. " +
            "Status über das ⋮-Menü wählen – Änderungen werden erst nach Bestätigung gespeichert. " +
            "A: auf Anfrage · T: Transferliste · U: unverkäuflich.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun TransferStatusInhalt(
    uiState: TransferStatusUiState,
    onStatusWaehlen: (Long, TransferStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zeilen = uiState.ergebnis.zeilen
    LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (zeilen.isEmpty()) {
            item { Text("Keine Spieler gefunden.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(zeilen, key = { it.spielerId }) { zeile ->
                TransferStatusKarte(zeile = zeile, onStatusWaehlen = onStatusWaehlen)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransferStatusKarte(
    zeile: TransferStatusZeile,
    onStatusWaehlen: (Long, TransferStatus) -> Unit,
) {
    var menuOffen by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        zeile.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    TransferStatusBadge(zeile.status)
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = listOfNotNull(
                        "MOR ${zeile.mor}",
                        "FIT ${zeile.fit}",
                        "Skill ${zeile.skillSchnitt}",
                        "Opti ${zeile.optSkill}".takeIf { zeile.optSkill.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (zeile.details.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        zeile.details.forEach { detail ->
                            DetailChip(detail)
                        }
                    }
                }

                if (zeile.mindestabloese.isNotBlank() && zeile.mindestabloese != "0") {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Mindestablöse: ${zeile.mindestabloese}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (zeile.transfertext.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = zeile.transfertext,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box {
                IconButton(onClick = { menuOffen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Status ändern")
                }
                DropdownMenu(expanded = menuOffen, onDismissRequest = { menuOffen = false }) {
                    TransferStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${status.kuerzel} – ${status.label}",
                                    color = if (status == zeile.status) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            leadingIcon = {
                                if (status == zeile.status) {
                                    Icon(Icons.Filled.Check, contentDescription = "Aktuell")
                                }
                            },
                            onClick = {
                                menuOffen = false
                                onStatusWaehlen(zeile.spielerId, status)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferStatusBadge(status: TransferStatus) {
    val (bg, fg) = when (status) {
        TransferStatus.T -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        TransferStatus.A -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        TransferStatus.U -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        TransferStatus.N -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(
            text = "${status.kuerzel} · ${status.label}",
            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = fg,
        )
    }
}

@Composable
private fun DetailChip(detail: TransferDetail) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text = "${detail.kuerzel}: ${detail.label}",
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransferStatusDialog(
    dialog: StatusDialogState,
    speichernd: Boolean,
    onBestaetigen: () -> Unit,
    onSchliessen: () -> Unit,
) {
    when (dialog) {
        is StatusDialogState.Verborgen -> Unit
        is StatusDialogState.Bestaetigung -> TransferStatusBestaetigungsDialog(
            zeile = dialog.zeile,
            neuerStatus = dialog.neuerStatus,
            speichernd = speichernd,
            onDismiss = onSchliessen,
            onBestaetigen = onBestaetigen,
        )
        is StatusDialogState.Ergebnis -> TransferStatusErgebnisDialog(
            erfolg = dialog.erfolg,
            meldung = dialog.meldung,
            onDismiss = onSchliessen,
        )
    }
}

@Composable
private fun TransferStatusBestaetigungsDialog(
    zeile: TransferStatusZeile,
    neuerStatus: TransferStatus,
    speichernd: Boolean,
    onDismiss: () -> Unit,
    onBestaetigen: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!speichernd) onDismiss() },
        title = { Text("Transferstatus ändern", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Soll der Transferstatus von „${zeile.name}“ von " +
                        "${zeile.status.kuerzel} – ${zeile.status.label} auf " +
                        "${neuerStatus.kuerzel} – ${neuerStatus.label} geändert werden?",
                )

                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = neuerStatus.beschreibung,
                        Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { if (!speichernd) onDismiss() }) {
                    Text("Abbrechen")
                }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = onBestaetigen,
                    enabled = !speichernd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    if (speichernd) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(16.dp).width(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Status ändern")
                }
            }
        },
    )
}

@Composable
private fun TransferStatusErgebnisDialog(
    erfolg: Boolean,
    meldung: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (erfolg) "Transferstatus geändert" else "Änderung fehlgeschlagen",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = if (erfolg) "$meldung Die Ansicht wird jetzt aktualisiert."
                else "$meldung Bitte prüfe den aktuellen Status und versuche es erneut.",
                color = if (erfolg) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.error,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}
