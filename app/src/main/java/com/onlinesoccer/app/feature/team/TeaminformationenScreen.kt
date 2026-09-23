package com.onlinesoccer.app.feature.team

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.TeamInfoMenuEintrag

/** Icons der einzelnen Teaminformationen-Unterpunkte, abhängig vom Namen. */
internal fun teamInfoMenuIcon(label: String): ImageVector = when {
    label.contains("Teamübersicht", true) || label.contains("Kader", true) -> Icons.Filled.ViewList
    label.contains("Vertrag", true) -> Icons.Filled.Description
    label.contains("Einzelwerte", true) || label.contains("Stärken", true) -> Icons.Filled.TrendingUp
    label.contains("Teaminfo", true) -> Icons.Filled.Info
    label.contains("Saisonplan", true) -> Icons.Filled.Event
    label.contains("Tabellenplatz", true) -> Icons.Filled.Leaderboard
    label.contains("Statistik", true) -> Icons.Filled.BarChart
    label.contains("Saisonhistorie", true) -> Icons.Filled.History
    label.contains("Vereinshistorie", true) -> Icons.Filled.Groups
    label.contains("Transfer", true) -> Icons.Filled.SwapHoriz
    label.contains("Leih", true) -> Icons.Filled.ExitToApp
    else -> Icons.Filled.Info
}

/** Übersicht „Teaminformationen": Unterpunkte des Team-Menüs (dynamisch von der Website). */
@Composable
fun TeaminformationenScreen(
    onEintragClick: (TeamInfoMenuEintrag) -> Unit,
    onClose: () -> Unit,
    teamId: Long? = null,
    viewModel: TeaminformationenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(teamId) {
        viewModel.lade(teamId)
    }

    when {
        uiState.ladend -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        uiState.fehler != null -> Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(uiState.fehler!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = { viewModel.lade(uiState.teamId) }) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text("Erneut versuchen")
            }
        }

        else -> TeaminformationenInhalt(
            eintraege = uiState.menuEintraege,
            onEintragClick = onEintragClick,
            onClose = onClose,
        )
    }
}

@Composable
private fun TeaminformationenInhalt(
    eintraege: List<TeamInfoMenuEintrag>,
    onEintragClick: (TeamInfoMenuEintrag) -> Unit,
    onClose: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Teaminformationen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
                FilledTonalButton(onClick = onClose) {
                    Text("Zurück")
                }
            }
        }

        if (eintraege.isEmpty()) {
            item {
                Text(
                    "Keine Teaminformationen gefunden.",
                    Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@LazyColumn
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    eintraege.forEachIndexed { index, eintrag ->
                        if (index > 0) {
                            HorizontalDivider(Modifier.padding(start = 44.dp, end = 16.dp))
                        }
                        TeaminfoEintragZeile(eintrag, onClick = { onEintragClick(eintrag) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TeaminfoEintragZeile(
    eintrag: TeamInfoMenuEintrag,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            teamInfoMenuIcon(eintrag.label),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            eintrag.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}