package com.onlinesoccer.app.feature.transfers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.VmSetzenEintrag

/** „Auf den VM setzen" (`vmsetzen.php`): eigene Spieler samt Startpreis-Staffeln, nur lesend.
 *  Es wird bewusst kein Formular abgesendet – das Einstellen selbst erfolgt im Browser. */
@Composable
fun VmSetzenScreen(
    onClose: () -> Unit = {},
    onSpielerClick: (Long) -> Unit = {},
) {
    val viewModel: VmSetzenViewModel = hiltViewModel()
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
                            + "Das Einstellen selbst ist nur im Browser möglich – die App sendet hier keine Formulare.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(uiState.ergebnis.eintraege, key = { it.spielerId }) { eintrag ->
                    VmSetzenKarte(eintrag, onSpielerClick)
                }
            }
        }
    }
}

@Composable
private fun VmSetzenKarte(
    eintrag: VmSetzenEintrag,
    onSpielerClick: (Long) -> Unit,
) {
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