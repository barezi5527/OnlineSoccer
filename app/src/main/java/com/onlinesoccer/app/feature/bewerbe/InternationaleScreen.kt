package com.onlinesoccer.app.feature.bewerbe

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.InternationaleAnsicht
import com.onlinesoccer.app.data.model.InternationaleFilter
import com.onlinesoccer.app.data.model.InternationaleOption
import com.onlinesoccer.app.data.model.WertTabelle
import com.onlinesoccer.app.ui.HubTabs

private enum class InternationaleBereich(val label: String, val path: String) {
    OS_RANKING("OS-Ranking", "/osneu/osranking"),
    CLUB_RANKING("Club-Ranking", "/osneu/clubranking"),
    TEILNEHMER("Internationale Teilnehmer", "/osneu/intTeilnehmer"),
    OSC_QUALI("OSC Qualifikation", "/osneu/oscq"),
    OSC_GRUPPE("OS Championscup GP", "/osneu/oscgp"),
    OSC_FINAL("OS Championscup FR", "/osneu/oscfr"),
    OSE_QUALI("OSE Qualifikation", "/osneu/oseq"),
    OSE_GRUPPE("OS Europacup GP", "/osneu/osegp"),
    OSE_FINAL("OS Europacup FR", "/osneu/osefr"),
    SUPERCUP("Supercup", "/osneu/supercup"),
}

/** Bereiche, die Begegnungs-Ergebnisse anzeigen und daher standardmäßig ausgeblendet werden. */
private val ergebnisBereiche = setOf(
    InternationaleBereich.OSC_QUALI,
    InternationaleBereich.OSC_GRUPPE,
    InternationaleBereich.OSC_FINAL,
    InternationaleBereich.OSE_QUALI,
    InternationaleBereich.OSE_GRUPPE,
    InternationaleBereich.OSE_FINAL,
    InternationaleBereich.SUPERCUP,
)

@Composable
fun InternationaleScreen(
    onSpielbericht: (String?, String?) -> Unit,
    viewModel: InternationaleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var bereich by rememberSaveable { mutableStateOf(InternationaleBereich.OS_RANKING) }
    var ausgewaehltesRankingTeam by remember { mutableStateOf<String?>(null) }
    var ergebnisseSichtbar by rememberSaveable { mutableStateOf(false) }
    val ansicht = uiState.ansicht

    LaunchedEffect(bereich) {
        ausgewaehltesRankingTeam = null
        viewModel.lade(bereich.path)
    }

    Column(Modifier.fillMaxSize()) {
        HubTabs(
            tabs = InternationaleBereich.entries.map { it.label to it },
            selected = bereich,
            onSelect = { bereich = it as InternationaleBereich },
        )
        Box(Modifier.fillMaxSize().weight(1f)) {
            when {
                uiState.fehler != null && ansicht == null -> FehlerAnsicht(uiState.fehler!!, onRetry = { viewModel.lade(bereich.path) })
                ansicht != null -> InternationaleAnsichtContent(
                    ansicht = ansicht,
                    istOsRanking = bereich == InternationaleBereich.OS_RANKING,
                    istErgebnisBereich = bereich in ergebnisBereiche,
                    ergebnisseSichtbar = ergebnisseSichtbar,
                    onErgebnisseSichtbar = { ergebnisseSichtbar = it },
                    ausgewaehlteZeile = ausgewaehltesRankingTeam,
                    onRankingTeam = { ausgewaehltesRankingTeam = it },
                    onFilter = {
                        ausgewaehltesRankingTeam = null
                        viewModel.lade(bereich.path, it)
                    },
                    onSpielbericht = onSpielbericht,
                )
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            if (uiState.ladend && ansicht != null) {
                CircularProgressIndicator(Modifier.align(Alignment.TopCenter).padding(8.dp))
            }
        }
    }
}

@Composable
private fun FehlerAnsicht(fehler: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(fehler, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.padding(start = 4.dp))
            Text("Erneut versuchen")
        }
    }
}

@Composable
private fun InternationaleAnsichtContent(
    ansicht: InternationaleAnsicht,
    istOsRanking: Boolean,
    istErgebnisBereich: Boolean,
    ergebnisseSichtbar: Boolean,
    onErgebnisseSichtbar: (Boolean) -> Unit,
    ausgewaehlteZeile: String?,
    onRankingTeam: (String) -> Unit,
    onFilter: (InternationaleFilter) -> Unit,
    onSpielbericht: (String?, String?) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(ansicht.titel ?: "Internationale Bewerbe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (istOsRanking) {
            Card(
                Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Länderranking", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Tippe auf ein Land, um es in der Tabelle hervorzuheben.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    ausgewaehlteZeile?.let { name ->
                        Text("Ausgewählt: $name", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        FilterLeiste(ansicht, onFilter, zeigeErgebnisFilter = !istErgebnisBereich)
        if (istErgebnisBereich) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = ergebnisseSichtbar,
                    onCheckedChange = onErgebnisseSichtbar,
                )
                Text(
                    "Ergebnisse anzeigen",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { onErgebnisseSichtbar(!ergebnisseSichtbar) },
                )
            }
        }
        if (ansicht.spiele.isNotEmpty()) {
            Text("Begegnungen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            ansicht.spiele.forEach { spiel ->
                BegegnungCard(spiel.heim, spiel.gast, spiel.ergebnis, spiel.berichtUrl, ergebnisseSichtbar, onSpielbericht)
            }
        }
        ansicht.tabellen.forEachIndexed { index, tabelle ->
            val istRanking = istRankingTabelle(tabelle)
            Tabelle(
                tabelle = tabelle,
                selectable = istRanking && index == 0,
                selectedRow = ausgewaehlteZeile,
                onRowClick = onRankingTeam,
            )
        }
        if (ansicht.abschnitte.isNotEmpty()) {
            Text("Weitere Informationen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            ansicht.abschnitte.forEach { abschnitt ->
                Card(Modifier.fillMaxWidth()) {
                    Text(abschnitt, Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (ansicht.abschnitte.isEmpty() && ansicht.spiele.isEmpty() && ansicht.tabellen.isEmpty()) {
            Text("Keine Daten für diese Auswahl gefunden.", Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun BegegnungCard(
    heim: String,
    gast: String,
    ergebnis: String?,
    berichtUrl: String?,
    ergebnisseSichtbar: Boolean,
    onSpielbericht: (String?, String?) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    heim,
                    Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )
                val resultat = ergebnis?.takeIf { ergebnisseSichtbar }
                Text(
                    resultat ?: "vs.",
                    Modifier.padding(horizontal = 10.dp),
                    fontWeight = FontWeight.Bold,
                    color = if (resultat != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    gast,
                    Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (berichtUrl != null) {
                TextButton(
                    onClick = { onSpielbericht(null, berichtUrl) },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Spielbericht öffnen")
                }
            }
        }
    }
}

@Composable
private fun FilterLeiste(
    ansicht: InternationaleAnsicht,
    onFilter: (InternationaleFilter) -> Unit,
    zeigeErgebnisFilter: Boolean = true,
) {
    val filter = ansicht.filter
    val grundAuswahl = listOf(
        FilterAuswahl("Saison", ansicht.saisonen, filter.saison) { value -> onFilter(filter.copy(saison = value)) },
        FilterAuswahl("Runde", ansicht.runden, filter.runde) { value -> onFilter(filter.copy(runde = value)) },
        FilterAuswahl("Gruppe", ansicht.gruppen, filter.gruppe) { value -> onFilter(filter.copy(gruppe = value)) },
    )
    val auswahl = (if (zeigeErgebnisFilter) {
        grundAuswahl + FilterAuswahl("Ergebnisse", ansicht.ergebnisOptionen, filter.ergebnisse) { value ->
            onFilter(filter.copy(ergebnisse = value))
        }
    } else {
        grundAuswahl
    }).filter { it.optionen.size >= 2 }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        auswahl.chunked(2).forEach { zeile ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                zeile.forEach { (label, optionen, selected, onSelect) ->
                    Auswahl(label, optionen, selected, onSelect, Modifier.weight(1f))
                }
                if (zeile.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class FilterAuswahl(
    val label: String,
    val optionen: List<InternationaleOption>,
    val selected: String?,
    val onSelect: (String) -> Unit,
)

@Composable
private fun Auswahl(
    label: String,
    optionen: List<InternationaleOption>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var offen by remember { mutableStateOf(false) }
    val text = optionen.firstOrNull { it.wert == selected }?.label ?: label
    Box(modifier) {
        OutlinedButton(onClick = { offen = true }, modifier = Modifier.fillMaxWidth()) {
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            optionen.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = { offen = false; if (option.wert != selected) onSelect(option.wert) },
                )
            }
        }
    }
}

@Composable
private fun Tabelle(
    tabelle: WertTabelle,
    selectable: Boolean = false,
    selectedRow: String? = null,
    onRowClick: (String) -> Unit = {},
) {
    val istRanking = istRankingTabelle(tabelle)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.horizontalScroll(rememberScrollState()).padding(8.dp)) {
            Row {
                tabelle.header.forEachIndexed { index, text ->
                    Text(
                        text,
                        Modifier.then(
                            if (istRanking) Modifier.width(rankingSpaltenbreite(index, tabelle.header)) else Modifier,
                        ).padding(4.dp),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                tabelle.zeilen.take(250).forEach { zeile ->
                    val rowKey = rankingName(zeile, tabelle.header)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .then(if (selectable) Modifier.clickable { onRowClick(rowKey) } else Modifier)
                            .background(
                                if (selectable && selectedRow == rowKey) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                },
                            ),
                    ) {
                        zeile.forEachIndexed { index, text ->
                            Text(
                                text,
                                Modifier.then(
                                    if (istRanking) Modifier.width(rankingSpaltenbreite(index, tabelle.header)) else Modifier,
                                ).padding(4.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun istRankingTabelle(tabelle: WertTabelle): Boolean =
    tabelle.header.size >= 2 && tabelle.header.firstOrNull() == "Platz" &&
        "Summe" in tabelle.header

private fun rankingSpaltenbreite(index: Int, header: List<String>): Dp {
    val spalte = header.getOrNull(index) ?: return 88.dp
    return when (spalte) {
        "Platz" -> 56.dp
        "Club" -> 180.dp
        "Land" -> if ("Club" in header) 56.dp else 180.dp
        else -> 88.dp
    }
}

private fun rankingName(zeile: List<String>, header: List<String>): String =
    zeile.getOrNull(header.indexOf("Club").takeIf { it >= 0 } ?: 1)?.takeIf { it.isNotBlank() }
        ?: zeile.getOrNull(header.indexOf("Land").takeIf { it >= 0 } ?: 1)?.takeIf { it.isNotBlank() }
        ?: "Team"
