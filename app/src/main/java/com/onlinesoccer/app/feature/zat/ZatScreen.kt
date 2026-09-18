package com.onlinesoccer.app.feature.zat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.ZugabgabeElementTyp
import com.onlinesoccer.app.feature.zugabgabe.ZugabgabeScreen
import com.onlinesoccer.app.feature.zugabgabe.ZugabgabeViewModel
import com.onlinesoccer.app.ui.HubTabs

private enum class ZatBereich { ZUSATZ, AUFSTELLUNG, AKTIONEN, EINSTELLUNGEN }

@Composable
fun ZatScreen(
    viewModel: ZatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val zugabgabeViewModel: ZugabgabeViewModel = hiltViewModel()
    val zugabgabeState by zugabgabeViewModel.uiState.collectAsStateWithLifecycle()
    var bereich by rememberSaveable { mutableStateOf(ZatBereich.AUFSTELLUNG) }

    val aktionenViewModel: AktionenViewModel = hiltViewModel()
    val einstellungenViewModel: EinstellungenViewModel = hiltViewModel()
    val aktionenState by aktionenViewModel.uiState.collectAsStateWithLifecycle()
    val einstellungenState by einstellungenViewModel.uiState.collectAsStateWithLifecycle()
    val zuzuViewModel: ZuzuViewModel = hiltViewModel()
    val zuzuState by zuzuViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(bereich) {
        when (bereich) {
            ZatBereich.ZUSATZ -> zuzuViewModel.lade()
            ZatBereich.AKTIONEN -> aktionenViewModel.lade()
            ZatBereich.EINSTELLUNGEN -> einstellungenViewModel.lade()
            ZatBereich.AUFSTELLUNG -> Unit
        }
    }

    LaunchedEffect(uiState.checkzaErgebnis) {
        uiState.checkzaErgebnis?.let { ergebnis ->
            zugabgabeViewModel.aktualisiereStatus(
                if (ergebnis.gueltig) "Gültig" else "ungültig",
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = bereich == ZatBereich.ZUSATZ,
                onClick = { bereich = ZatBereich.ZUSATZ },
                label = { Text("Zugabgabe Zusatz", style = MaterialTheme.typography.labelMedium) },
            )
            zugabgabeState.aufstellung?.let { aufstellung ->
                Spacer(Modifier.width(8.dp))
                Text(
                    "ZAT ${aufstellung.zat ?: "–"}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when (aufstellung.status) {
                        "Gültig" -> "✓ Gültig"
                        else -> "✗ ${aufstellung.status ?: "unbekannt"}"
                    },
                    color = if (aufstellung.status == "Gültig") Color(0xFF2E7D32) else Color(0xFFC62828),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                HubTabs(
                    tabs = listOf(
                        "Aufstellung" to ZatBereich.AUFSTELLUNG,
                        "Aktionen" to ZatBereich.AKTIONEN,
                        "Einstellungen" to ZatBereich.EINSTELLUNGEN,
                    ),
                    selected = bereich,
                    onSelect = { bereich = it as ZatBereich },
                )
            }
            Button(
                onClick = viewModel::zugabgabeSpeichern,
                enabled = !uiState.speichernd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.speicherErfolgreich) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                modifier = Modifier.width(74.dp).height(38.dp).padding(end = 4.dp),
            ) {
                if (uiState.speichernd) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Speichern", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
        if (uiState.meldung != null || uiState.fehler != null) {
            Text(
                uiState.meldung ?: uiState.fehler.orEmpty(),
                color = if (uiState.fehler != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (bereich) {
                ZatBereich.ZUSATZ -> ZuzuZusatzInhalt(
                    uiState = zuzuState,
                    onPreis = zuzuViewModel::setPreis,
                    onPreiseSpeichern = zuzuViewModel::preiseSpeichern,
                    onToggleAuswahl = zuzuViewModel::toggleAuswahl,
                    onPhysioSchicken = zuzuViewModel::physioSchicken,
                )
                ZatBereich.AUFSTELLUNG -> ZugabgabeScreen(
                    viewModel = zugabgabeViewModel,
                    showModusToggle = false,
                )
                ZatBereich.AKTIONEN -> ZatElementeEditor(
                    uiState = aktionenState,
                    typen = ZugabgabeElementTyp.aktionen,
                    titel = "Aktion",
                    onLade = aktionenViewModel::lade,
                    onTyp = aktionenViewModel::waehleTyp,
                    onWert = aktionenViewModel::setWert,
                    onAnlegen = aktionenViewModel::spieleAnlegen,
                    onToggleAuswahl = aktionenViewModel::toggleAuswahl,
                    onLoeschen = aktionenViewModel::loescheMarkierte,
                )
                ZatBereich.EINSTELLUNGEN -> ZatElementeEditor(
                    uiState = einstellungenState,
                    typen = ZugabgabeElementTyp.einstellungen,
                    titel = "Einstellung",
                    onLade = einstellungenViewModel::lade,
                    onTyp = einstellungenViewModel::waehleTyp,
                    onWert = einstellungenViewModel::setWert,
                    onAnlegen = einstellungenViewModel::spieleAnlegen,
                    onToggleAuswahl = einstellungenViewModel::toggleAuswahl,
                    onLoeschen = einstellungenViewModel::loescheMarkierte,
                )
            }
        }
    }

    if (uiState.speichernd) {
        CheckzaLadeDialog()
    }

    uiState.checkzaErgebnis?.let { ergebnis ->
        CheckzaReportDialog(
            ergebnis = ergebnis,
            onDismiss = viewModel::schliesseCheckzaErgebnis,
        )
    }
}
