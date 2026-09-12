package com.onlinesoccer.app.feature.zat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import com.onlinesoccer.app.data.model.ZuzuSpieler

/** Inhalt des Tabs „Zugabgabe Zusatz“ (`zuzu.php`): Eintrittspreise + Physio-Liste. */
@Composable
fun ZuzuZusatzInhalt(
    uiState: ZuzuUiState,
    onPreis: (ZuzuPreisFeld, String) -> Unit,
    onPreiseSpeichern: () -> Unit,
    onToggleAuswahl: (Long) -> Unit,
    onPhysioSchicken: () -> Unit,
) {
    when {
        uiState.ladend && !uiState.geladen && uiState.spieler.isEmpty() -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        else -> Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.fehler?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            EintrittspreiseKarte(
                uiState = uiState,
                onPreis = onPreis,
                onSpeichern = onPreiseSpeichern,
            )

            uiState.meldung?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            PhysioKarte(
                uiState = uiState,
                onToggleAuswahl = onToggleAuswahl,
                onPhysioSchicken = onPhysioSchicken,
            )
        }
    }
}

@Composable
private fun EintrittspreiseKarte(
    uiState: ZuzuUiState,
    onPreis: (ZuzuPreisFeld, String) -> Unit,
    onSpeichern: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Eintrittspreise (Heimspiele)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            PreisFeld("Liga", uiState.preise.liga.orEmpty(), ZuzuPreisFeld.LIGA, onPreis)
            PreisFeld("Pokal", uiState.preise.pokal.orEmpty(), ZuzuPreisFeld.POKAL, onPreis)
            PreisFeld("International", uiState.preise.international.orEmpty(), ZuzuPreisFeld.INTERNATIONAL, onPreis)
            Button(
                onClick = onSpeichern,
                enabled = !uiState.speichernd,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.speichernd) {
                    CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Speichern")
                }
            }
        }
    }
}

@Composable
private fun PreisFeld(
    label: String,
    wert: String,
    feld: ZuzuPreisFeld,
    onPreis: (ZuzuPreisFeld, String) -> Unit,
) {
    OutlinedTextField(
        value = wert,
        onValueChange = { onPreis(feld, it) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PhysioKarte(
    uiState: ZuzuUiState,
    onToggleAuswahl: (Long) -> Unit,
    onPhysioSchicken: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Zum Physio schicken",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (uiState.spieler.isEmpty() && !uiState.ladend) {
                Text(
                    "Keine Spieler verfügbar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.spieler.forEach { spieler ->
                    PhysioZeile(spieler, uiState, onToggleAuswahl)
                }
                if (uiState.auswahl.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = onPhysioSchicken,
                        enabled = !uiState.speichernd,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("\u2191 ${uiState.auswahl.size} Spieler zum Physio")
                    }
                }
            }
        }
    }
}

@Composable
private fun PhysioZeile(
    spieler: ZuzuSpieler,
    uiState: ZuzuUiState,
    onToggleAuswahl: (Long) -> Unit,
) {
    val fitFarbe = when {
        spieler.fit >= 90 -> Color(0xFF2E7D32)
        spieler.fit >= 70 -> Color(0xFFF9A825)
        else -> Color(0xFFC62828)
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = spieler.pid in uiState.auswahl,
            onCheckedChange = { onToggleAuswahl(spieler.pid) },
        )
        Text(
            spieler.name,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "FIT ${spieler.fit}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fitFarbe,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            spieler.kosten,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}