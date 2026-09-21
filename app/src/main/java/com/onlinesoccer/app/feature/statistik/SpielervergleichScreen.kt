package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.SpielerVorschlag
import com.onlinesoccer.app.data.model.VergleichZeile

/** Spielervergleich (`osneu/spielervergleich`). */
@Composable
fun SpielervergleichScreen(
    onClose: () -> Unit,
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
    viewModel: SpielervergleichViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var auswahlOffen by remember { mutableStateOf(true) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatistikHeader(
                    titel = "Spielervergleich",
                    untertitel = "Zwei Spieler direkt vergleichen",
                    onClose = onClose,
                )
                Spacer(Modifier.weight(1f))
                StatistikFilterToggle(auswahlOffen) { auswahlOffen = !auswahlOffen }
            }
        }

        if (auswahlOffen) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SpielerAuswahl(
                        label = "Spieler 1",
                        name = uiState.name1,
                        vorschlaege = uiState.vorschlaege1,
                        onTextAendern = { viewModel.textAendern(it, spieler2 = false) },
                        onVorschlag = { viewModel.vorschlagWaehlen(it, spieler2 = false) },
                    )
                    SpielerAuswahl(
                        label = "Spieler 2",
                        name = uiState.name2,
                        vorschlaege = uiState.vorschlaege2,
                        onTextAendern = { viewModel.textAendern(it, spieler2 = true) },
                        onVorschlag = { viewModel.vorschlagWaehlen(it, spieler2 = true) },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.ansicht == 0,
                            onClick = { viewModel.ansichtWechseln(0) },
                            label = { Text("Normal") },
                        )
                        FilterChip(
                            selected = uiState.ansicht == 1,
                            onClick = { viewModel.ansichtWechseln(1) },
                            label = { Text("Differenz") },
                        )
                    }
                    FilledTonalButton(
                        onClick = { viewModel.vergleichen(); auswahlOffen = false },
                        enabled = uiState.id1 != null && uiState.id2 != null && !uiState.ladendVergleich,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uiState.ladendVergleich) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.padding(start = 8.dp))
                            Text("Wird verglichen …")
                        } else {
                            Text("Vergleichen")
                        }
                    }
                    uiState.fehler?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        }

        uiState.vergleich?.let { vergleich ->
            item {
                Vergleichstabelle(
                    vergleich = vergleich,
                    onSpielerClick = onSpielerClick,
                    onTeamClick = onTeamClick,
                )
            }
        }
    }
}

@Composable
private fun SpielerAuswahl(
    label: String,
    name: String,
    vorschlaege: List<SpielerVorschlag>,
    onTextAendern: (String) -> Unit,
    onVorschlag: (SpielerVorschlag) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = name,
            onValueChange = onTextAendern,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (vorschlaege.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    vorschlaege.take(8).forEachIndexed { index, vorschlag ->
                        if (index > 0) HorizontalDivider()
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onVorschlag(vorschlag) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                vorschlag.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (vorschlag.zusatz.isNotBlank()) {
                                Text(
                                    vorschlag.zusatz,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Vergleichstabelle(
    vergleich: com.onlinesoccer.app.data.model.Spielervergleich,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                KopfSpalte(vergleich.name1, vergleich.pid1, vergleich.team1, vergleich.team1Id, onSpielerClick, onTeamClick)
                KopfSpalte(vergleich.name2, vergleich.pid2, vergleich.team2, vergleich.team2Id, onSpielerClick, onTeamClick)
            }
            HorizontalDivider()
            vergleich.zeilen.forEachIndexed { index, zeile ->
                if (index > 0 && zeile.wert1.isNotEmpty()) HorizontalDivider()
                VergleichZeileRow(zeile)
            }
            if (vergleich.zeilen.isEmpty()) {
                Text(
                    "Keine Vergleichsdaten.",
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RowScope.KopfSpalte(
    name: String?,
    pid: Long?,
    team: String?,
    teamId: Long?,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    Column(
        Modifier
            .weight(1f)
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            name ?: "–",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = if (pid != null) Modifier.clickable { onSpielerClick(pid) } else Modifier,
        )
        if (!team.isNullOrBlank()) {
            Text(
                team,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (teamId != null) Modifier.clickable { onTeamClick(teamId) } else Modifier,
            )
        }
    }
}

@Composable
private fun VergleichZeileRow(zeile: VergleichZeile) {
    if (zeile.wert1.isEmpty()) {
        if (zeile.label.isNotBlank()) {
            BoxFullWidth {
                Text(
                    zeile.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            zeile.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.1f),
        )
        Text(
            zeile.wert1,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f),
            fontWeight = if (zeile.besser == 1) FontWeight.Bold else FontWeight.Normal,
            color = if (zeile.besser == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(
            zeile.wert2,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f),
            fontWeight = if (zeile.besser == 2) FontWeight.Bold else FontWeight.Normal,
            color = if (zeile.besser == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BoxFullWidth(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        content()
    }
}