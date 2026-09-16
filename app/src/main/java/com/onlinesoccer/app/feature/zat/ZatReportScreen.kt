package com.onlinesoccer.app.feature.zat

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.core.ui.theme.PositionsBadge
import com.onlinesoccer.app.data.model.ZatReport
import com.onlinesoccer.app.data.model.ZatReportEinnahme
import com.onlinesoccer.app.data.model.ZatReportTraining

private val ErfolgtColor = Color(0xFF2E7D32)
private val FehlgeschlagenColor = Color(0xFFC62828)

/** Nativer ZAT-Report (wie `zar.php` auf der Website). */
@Composable
fun ZatReportScreen(
    onClose: () -> Unit,
    onSpielerClick: (Long) -> Unit = {},
    viewModel: ZatReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.ladend -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        uiState.fehler != null -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(uiState.fehler!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = viewModel::lade) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text("Erneut versuchen")
            }
        }

        uiState.report != null -> ZatReportInhalt(uiState.report!!, onClose, onSpielerClick)
    }
}

@Composable
private fun ZatReportInhalt(report: ZatReport, onClose: () -> Unit, onSpielerClick: (Long) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                }
                Column(Modifier.weight(1f)) {
                    Text("ZAT-Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    report.saison?.let {
                        Text(
                            "Saison $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (report.zat != null || report.saison != null) {
                    Text(
                        "ZAT ${report.zat ?: "–"}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item { BerichtsAbschnittTitel("1. Einnahmen / Ausgaben") }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    report.einnahmen.forEachIndexed { index, eintrag ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        EinnahmeZeile(eintrag)
                    }
                }
            }
        }

        item { BerichtsAbschnittTitel("2. Trainingserfolge") }

        if (report.trainingserfolge.isEmpty()) {
            item {
                Text(
                    "Keine Trainingserfolge für diesen ZAT.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(report.trainingserfolge, key = { "${it.pid ?: it.name}-${it.beschreibung}" }) { training ->
                TrainingZeile(training, onSpielerClick)
            }
        }
    }
}

@Composable
private fun BerichtsAbschnittTitel(titel: String) {
    Text(titel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun EinnahmeZeile(eintrag: ZatReportEinnahme) {
    val gesamt = eintrag.label.contains("Gesamtsumme", ignoreCase = true)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            eintrag.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            fontWeight = if (gesamt) FontWeight.Bold else FontWeight.Normal,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            eintrag.wert,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TrainingZeile(training: ZatReportTraining, onSpielerClick: (Long) -> Unit) {
    val erfolgreich = !training.beschreibung.contains("erfolglos", ignoreCase = true)
    val erfolglos = training.beschreibung.contains("erfolglos", ignoreCase = true)
    Card(
        Modifier
            .fillMaxWidth()
            .then(if (training.pid != null) Modifier.clickable { onSpielerClick(training.pid) } else Modifier),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (training.position != null) {
                PositionsBadge(training.position)
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    training.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    training.beschreibung,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        erfolglos -> FehlgeschlagenColor
                        erfolgreich -> ErfolgtColor
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            training.wert?.let {
                Spacer(Modifier.width(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}