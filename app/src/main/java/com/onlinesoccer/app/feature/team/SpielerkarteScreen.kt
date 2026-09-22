package com.onlinesoccer.app.feature.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.core.ui.theme.trikotFarbe
import com.onlinesoccer.app.data.model.SpielerKarte
import com.onlinesoccer.app.data.model.TeamTrainer

/** Spielerkarte (wie das Spielerprofil auf der Website, `sp.php`). */
@Composable
fun SpielerkarteScreen(
    onClose: () -> Unit,
    viewModel: SpielerkarteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bietDialog by viewModel.bietDialog.collectAsStateWithLifecycle()
    val trainerNachricht by viewModel.trainerNachricht.collectAsStateWithLifecycle()
    var trainerDialog by remember { mutableStateOf<TeamTrainer.Besetzt?>(null) }

    when {
        uiState.ladend -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        uiState.fehler != null -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(uiState.fehler ?: "", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = viewModel::lade) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text("Erneut versuchen")
            }
        }

        uiState.karte != null -> {
            val k = uiState.karte ?: return
            KartenAnsicht(
                karte = k,
                onClose = onClose,
                bietenMoeglich = uiState.bietenMoeglich,
                onBietenKlick = viewModel::onBietenKlick,
                onTrainerKlick = { trainerDialog = it },
            )
        }
    }

    BietDialog(
        zustand = bietDialog,
        onBestatigen = viewModel::gebotAbgeben,
        onAbbrechen = viewModel::bietDialogSchliessen,
    )

    trainerDialog?.let { trainer ->
        TrainerNachrichtDialog(
            trainer = trainer,
            zustand = trainerNachricht,
            onDismiss = {
                trainerDialog = null
                viewModel.trainerNachrichtReset()
            },
            onSenden = { text -> viewModel.sendeTrainerNachricht(trainer.name, trainer.id, text) },
        )
    }
}

@Composable
private fun BietDialog(
    zustand: BietDialogState,
    onBestatigen: () -> Unit,
    onAbbrechen: () -> Unit,
) {
    when (zustand) {
        BietDialogState.Verborgen -> Unit

        BietDialogState.Laedt -> AlertDialog(
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

        is BietDialogState.Bereit -> {
            val info = zustand.info
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
                                info.angeboteBis.takeIf { it.isNotBlank() }?.let { "Angebote bis: $it" },
                            )
                            zeilen.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Die Laufzeit des Vertrags beträgt 36 Monate, das Gehalt ist fest definiert.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                        onClick = onBestatigen,
                        enabled = info?.submitName?.isNotBlank() == true,
                    ) {
                        Text("Gebot verbindlich abgeben")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onAbbrechen) { Text("Abbrechen") }
                },
            )
        }

        is BietDialogState.Ergebnis -> AlertDialog(
            onDismissRequest = onAbbrechen,
            title = { Text(if (zustand.erfolg) "Gebot gesendet" else "Nicht möglich") },
            text = { Text(zustand.meldung.ifBlank { if (zustand.erfolg) "Gebot wurde gesendet." else "Unbekannter Fehler." }) },
            confirmButton = {
                TextButton(onClick = onAbbrechen) { Text("OK") }
            },
        )
    }
}

@Composable
private fun KartenAnsicht(
    karte: SpielerKarte,
    onClose: () -> Unit,
    bietenMoeglich: Boolean,
    onBietenKlick: () -> Unit,
    onTrainerKlick: (TeamTrainer.Besetzt) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Spielerkarte",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    TrainerHeaderZeile(karte.trainer, onKlick = onTrainerKlick)
                }
                if (bietenMoeglich) {
                    FilledTonalButton(onClick = onBietenKlick) {
                        Text("Bieten")
                    }
                }
            }
        }

        item { KopfKarte(karte) }
        item { ZustandKarte(karte) }

        if (karte.sonderFaehigkeiten.isNotEmpty()) {
            item { Text("Sonderfähigkeiten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { SonderfaehigkeitenKarte(karte.sonderFaehigkeiten) }
        }

        if (karte.staerken.isNotEmpty()) {
            item { Text("Stärken", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { StaerkenKarte(karte) }
        }

        if (karte.vertrag != null) {
            item { Text("Vertrag", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { VertragKarte(karte) }
        }

        item { Text("Statistik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { StatistikKarte(karte) }
    }
}

@Composable
private fun KopfKarte(karte: SpielerKarte) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(trikotFarbe(karte.position), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(karte.nummer.ifBlank { "–" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(karte.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${posName(karte.position)}${karte.alter?.let { " · $it Jahre" }.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ZustandKarte(karte: SpielerKarte) {
    val zustaende = buildList {
        add("Skill" to "${(karte.skill * 10).toInt() / 10.0}")
        add("Opti" to "${(karte.opti * 10).toInt() / 10.0}")
        if (karte.fit > 0) add("Fitness" to "${karte.fit}%")
        if (karte.mor > 0) add("Moral" to "${karte.mor}%")
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            zustaende.forEach { (label, wert) ->
                ZustandWert(label, wert)
            }
        }
    }
}

@Composable
private fun ZustandWert(label: String, wert: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(wert, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SonderfaehigkeitenKarte(sonderfaehigkeiten: List<com.onlinesoccer.app.data.model.SonderFaehigkeit>) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sonderfaehigkeiten.forEach { sf ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(sf.kuerzel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sf.bezeichnung,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    sf.positionsHinweis?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        sf.attribute.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun StaerkenKarte(karte: SpielerKarte) {
    Card(Modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(karte.staerken.toList()) { (label, wert) ->
                Column(
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(wert, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun VertragKarte(karte: SpielerKarte) {
    val v = karte.vertrag ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            VertragsZeile("Gehalt", v.gehalt ?: "–")
            VertragsZeile("Vertragslaufzeit", v.laufzeit ?: "–")
            VertragsZeile("Marktwert", v.marktwert ?: "–")
            VertragsZeile("Geburtstag", v.geburtstag ?: "–")
        }
    }
}

@Composable
private fun VertragsZeile(label: String, wert: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(wert, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatistikKarte(karte: SpielerKarte) {
    var gesamt by remember { mutableStateOf(false) }
    val daten = if (gesamt) karte.statistikGesamt else karte.statistikSaison
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        listOf(false to "Saison", true to "Gesamt").forEachIndexed { index, (istGesamt, label) ->
            SegmentedButton(
                selected = gesamt == istGesamt,
                onClick = { gesamt = istGesamt },
                shape = SegmentedButtonDefaults.itemShape(index, 2),
            ) {
                Text(label)
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Card(Modifier.fillMaxWidth()) {
        if (daten.isEmpty()) {
            Text("Keine Statistik vorhanden.", Modifier.padding(14.dp))
        } else {
            Column(
                Modifier.fillMaxWidth(),
            ) {
                daten.forEach { (label, wert) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        Text(wert, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}