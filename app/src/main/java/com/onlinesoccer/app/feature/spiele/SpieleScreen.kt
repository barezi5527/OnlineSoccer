package com.onlinesoccer.app.feature.spiele

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.onlinesoccer.app.data.model.DashboardData
import com.onlinesoccer.app.data.model.LivegameData
import com.onlinesoccer.app.ui.components.SpielverlaufEreignisKarte
import com.onlinesoccer.app.ui.components.SpielverlaufLegende

@Composable
fun SpieleScreen(
    viewModel: SpieleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.ladende -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        uiState.dashboard == null -> Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Spiele", style = MaterialTheme.typography.headlineMedium)
            uiState.fehler?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center,
                )
            }
            Button(onClick = viewModel::lade, modifier = Modifier.padding(top = 8.dp)) {
                Text("Erneut laden")
            }
        }

        else -> {
            val dashboard = uiState.dashboard ?: return
            val live = uiState.livegame
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Text("Dein nächstes Spiel", style = MaterialTheme.typography.titleMedium) }
                item {
                    if (live != null) {
                        Scorecard(live)
                    } else {
                        SpielfreierHinweis(dashboard)
                    }
                }
                if (live != null) {
                    item { MatchfactsKarte(live) }
                    item { TaktikKarte(live) }
                    item { StatistikKarte(live) }
                    val ereignisse = live.ereignisse
                    if (ereignisse.isNotEmpty()) {
                        item { Text("Spielverlauf", style = MaterialTheme.typography.titleMedium) }
                        item { SpielverlaufLegende() }
                        items(ereignisse, key = { "${it.minute}-${it.text}" }) { ereignis ->
                            SpielverlaufEreignisKarte(
                                minute = ereignis.minute.takeIf { it >= 0 }?.toString(),
                                text = ereignis.text,
                                typ = ereignis.typ,
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                }
                dashboard.letztesSpiel?.let { letztes ->
                    item {
                        Text("Dein letztes Spiel", style = MaterialTheme.typography.titleMedium)
                    }
                    item {
                        LetztesSpielKarte(letztes)
                    }
                }

                item {
                    Button(onClick = viewModel::lade, modifier = Modifier.fillMaxWidth()) {
                        Text("Aktualisieren")
                    }
                }
            }
        }
    }
}

@Composable
private fun Scorecard(live: LivegameData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    live.heimName ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
                Text(
                    "  ${live.heimTore} : ${live.gastTore}  ",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    live.gastName ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                if (live.played) "Endstand" else "Noch nicht gespielt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SpielfreierHinweis(dashboard: DashboardData) {
    val next = dashboard.naechstesSpiel
    Text(
        if (next != null) "${next.art ?: ""} – ${next.gegner ?: "?"}" else "Kein nächstes Spiel gefunden.",
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun LetztesSpielKarte(prev: DashboardData.MatchInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${prev.art ?: "Spiel"} – ${prev.gegner ?: "?"}",
                style = MaterialTheme.typography.titleSmall,
            )
            prev.berichtUrl?.let {
                Text(
                    "Ergebnis via Spielbericht",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MatchfactsKarte(live: LivegameData) {
    InfoKarte("Spielinfos") {
        listOfNotNull(
            live.zat?.let { "ZAT" to "$it" },
            live.spieltyp?.let { "Art" to it },
            live.datum?.let { "Datum" to it },
            live.stadion?.let { "Stadion" to it },
        ).forEach { (label, wert) ->
            KarteZeile(label, wert)
        }
    }
}

@Composable
private fun TaktikKarte(live: LivegameData) {
    val heim = live.heimTaktik
    val gast = live.gastTaktik
    if (heim == null || gast == null) return
    InfoKarte("Taktik") {
        listOf(
            "Commitment" to Pair(heim.commitment, gast.commitment),
            "Härte" to Pair(heim.hardness, gast.hardness),
            "Spieltyp" to Pair(heim.playtype, gast.playtype),
            "Abwehr" to Pair(heim.defence, gast.defence),
            "Mittelfeld" to Pair(heim.midfield, gast.midfield),
            "Sturm" to Pair(heim.offence, gast.offence),
        ).forEach { (label, werte) ->
            Row(Modifier.fillMaxWidth()) {
                Text(wertText(werte.first), Modifier.weight(1f), textAlign = TextAlign.Center)
                Text(label, modifier = Modifier.width(96.dp), textAlign = TextAlign.Center)
                Text(wertText(werte.second), Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun StatistikKarte(live: LivegameData) {
    val heim = live.heimStatistik
    val gast = live.gastStatistik
    if (heim == null || gast == null) return
    InfoKarte("Statistik") {
        listOf(
            "Tore" to Pair(heim.goals, gast.goals),
            "Ecken" to Pair(heim.corners, gast.corners),
            "Abseits" to Pair(heim.offside, gast.offside),
            "Fouls" to Pair(heim.fouls, gast.fouls),
            "Elfmeter" to Pair(heim.penalties, gast.penalties),
            "Ballbesitz" to Pair(heim.possession, gast.possession),
        ).forEach { (label, werte) ->
            Row(Modifier.fillMaxWidth()) {
                Text(wertText(werte.first), Modifier.weight(1f), textAlign = TextAlign.Center)
                Text(label, modifier = Modifier.width(96.dp), textAlign = TextAlign.Center)
                Text(wertText(werte.second), Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun InfoKarte(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun KarteZeile(label: String, wert: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(wert, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun wertText(v: Int?): String = if (v != null && v >= 0) "$v" else "–"
