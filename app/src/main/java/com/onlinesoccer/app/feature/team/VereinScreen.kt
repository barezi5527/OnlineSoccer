package com.onlinesoccer.app.feature.team

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.onlinesoccer.app.data.model.FremdesTeam
import com.onlinesoccer.app.data.model.TeamTrainer

/** Neues Fenster für einen fremden Verein (`st.php?c=<id>`): Kader mit klickbaren Spielern. */
@Composable
fun VereinScreen(
    onSpielerClick: (Long) -> Unit,
    onClose: () -> Unit,
    viewModel: VereinViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
            Text(uiState.fehler!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = viewModel::lade) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text("Erneut versuchen")
            }
        }

        uiState.team != null -> VereinAnsicht(
            team = uiState.team!!,
            onSpielerClick = onSpielerClick,
            onClose = onClose,
            onTrainerKlick = { trainerDialog = it },
        )
    }

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
private fun VereinAnsicht(
    team: FremdesTeam,
    onSpielerClick: (Long) -> Unit,
    onClose: () -> Unit,
    onTrainerKlick: (TeamTrainer.Besetzt) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Row(
                Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        team.name.ifBlank { "Verein ${team.teamId}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (team.liga.isNotBlank()) {
                        Text(
                            team.liga,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    TrainerHeaderZeile(team.trainer, onKlick = onTrainerKlick)
                }
            }
        }
        item {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    "Kader · ${team.kader.size} Spieler",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        items(team.kader, key = { it.pid }) { spieler ->
            SpielerZeile(spieler) { onSpielerClick(spieler.pid) }
        }
    }
}