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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.core.ui.theme.PositionsBadge
import com.onlinesoccer.app.data.model.VersteigerungsmarktEintrag
import com.onlinesoccer.app.feature.server.FehlerBox

/** Native Darstellung des „Versteigerungsmarkts" (`viewvm.php`): Filter wählen, Einträge anzeigen. */
@Composable
fun VersteigerungsmarktScreen(
    onClose: () -> Unit = {},
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
) {
    val viewModel: VersteigerungsmarktViewModel = hiltViewModel()
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
                VersteigerungsmarktKarte(eintrag, onSpielerClick, onTeamClick)
            }
        }
        }
    }
}

@Composable
private fun VersteigerungsmarktKarte(
    eintrag: VersteigerungsmarktEintrag,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    val kern = listOf(eintrag.alter, eintrag.position, eintrag.land).filter { it.isNotBlank() }
    val skillZeilen = listOfNotNull(
        eintrag.skill.takeIf { it.isNotBlank() }?.let { "Skill $it" },
        eintrag.optSkill.takeIf { it.isNotBlank() }?.let { "Opt. Skill $it" },
    )
    val finanz = listOfNotNull(
        eintrag.gebot.takeIf { it.isNotBlank() }?.let { "Gebot $it" },
        eintrag.prozentMw.takeIf { it.isNotBlank() }?.let { "$it % MW" },
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