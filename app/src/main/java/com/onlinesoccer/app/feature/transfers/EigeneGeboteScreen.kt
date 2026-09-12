package com.onlinesoccer.app.feature.transfers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.EigeneGeboteZeile
import com.onlinesoccer.app.feature.server.FehlerBox

/** Native „Eigene Gebote" (`viewtm.php`): Übersicht über alle eigenen Transfermarkt-Gebote. */
@Composable
fun EigeneGeboteScreen(
    onClose: () -> Unit = {},
    onSpielerClick: (Long) -> Unit = {},
) {
    val viewModel: EigeneGeboteViewModel = hiltViewModel()
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
                "Eigene Gebote",
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
                onRetry = viewModel::lade,
            )

            else -> Inhalt(uiState.ergebnis.zeilen, uiState.ergebnis.summe, onSpielerClick)
        }
    }
}

@Composable
private fun Inhalt(
    zeilen: List<EigeneGeboteZeile>,
    summe: String,
    onSpielerClick: (Long) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (summe.isNotBlank()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Summe der Gebote: $summe Euro",
                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (zeilen.isEmpty()) {
            item {
                Text(
                    "Keine offenen Gebote.",
                    Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(zeilen, key = { it.spielerId }) { zeile ->
                EigeneGeboteKarte(zeile, onSpielerClick)
            }
        }
    }
}

@Composable
private fun EigeneGeboteKarte(zeile: EigeneGeboteZeile, onSpielerClick: (Long) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    zeile.name,
                    Modifier
                        .weight(1f)
                        .clickable { onSpielerClick(zeile.spielerId) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.padding(start = 8.dp))
                if (zeile.gebot.isNotBlank()) {
                    Text(
                        "${zeile.gebot} Euro",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            val kern = listOf(zeile.alter, zeile.position, zeile.land).filter { it.isNotBlank() }
            if (kern.isNotEmpty()) {
                Text(
                    kern.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val skillZeilen = listOfNotNull(
                zeile.skill.takeIf { it.isNotBlank() }?.let { "Skill $it" },
                zeile.optSkill.takeIf { it.isNotBlank() }?.let { "Opt. Skill $it" },
            )
            if (skillZeilen.isNotEmpty()) {
                Text(
                    skillZeilen.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            val sonstiges = listOfNotNull(
                zeile.laufzeit.takeIf { it.isNotBlank() }?.let { "Laufzeit $it" },
                zeile.gehalt.takeIf { it.isNotBlank() }?.let { "Gehalt $it" },
                zeile.transfertag.takeIf { it.isNotBlank() }?.let { "Transfertag $it" },
            )
            if (sonstiges.isNotEmpty()) {
                Text(
                    sonstiges.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}