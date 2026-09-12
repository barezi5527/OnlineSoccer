package com.onlinesoccer.app.feature.team

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.KaderSpieler
import com.onlinesoccer.app.data.model.SonderFaehigkeit
import com.onlinesoccer.app.data.model.SpielerPosition
import com.onlinesoccer.app.data.model.StaerkeZeile
import com.onlinesoccer.app.data.model.StatistikZeile
import com.onlinesoccer.app.data.model.Teaminfo
import com.onlinesoccer.app.data.model.VertragZeile
import com.onlinesoccer.app.feature.taktik.TaktikEditor
import com.onlinesoccer.app.feature.taktik.TaktikViewModel
import com.onlinesoccer.app.feature.team.friendly.FreundschaftScreen
import com.onlinesoccer.app.ui.HubTabs

private enum class TeamBereich { KADER, VERTRAEGE, STAERKEN, STATISTIK, TEAMINFO, TAKTIK, FREUNDSCHAFT }

@Composable
fun TeamScreen(
    onSpielerClick: (Long) -> Unit,
    onSeiteClick: (String) -> Unit,
    viewModel: TeamViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var bereich by remember { mutableStateOf(TeamBereich.KADER) }

    LaunchedEffect(bereich, uiState.statistikGesamt) {
        when (bereich) {
            TeamBereich.KADER -> if (uiState.kader.isEmpty()) viewModel.ladeKader()
            TeamBereich.VERTRAEGE -> if (uiState.vertraege == null) viewModel.ladeVertraege()
            TeamBereich.STAERKEN -> if (uiState.staerken == null) viewModel.ladeStaerken()
            TeamBereich.STATISTIK -> viewModel.ladeStatistik(uiState.statistikGesamt)
            TeamBereich.TEAMINFO -> if (uiState.teaminfo == null) viewModel.ladeTeaminfo()
            TeamBereich.TAKTIK -> Unit
            TeamBereich.FREUNDSCHAFT -> Unit
        }
    }

    Column(Modifier.fillMaxSize()) {
        HubTabs(
            tabs = remember { listOf(
                "Training" to "training.php",
                "Trainer" to "trainer.php",
                "Taktik-Editor" to TeamBereich.TAKTIK,
                "Verträge verlängern" to "vt.php",
                "Freundschaftsspiele" to TeamBereich.FREUNDSCHAFT,
            ) },
            selected = bereich,
            onSelect = { value ->
                when (value) {
                    is String -> onSeiteClick(value)
                    is TeamBereich -> bereich = value
                }
            },
            compact = true,
        )

        HubTabs(
            tabs = remember { listOf(
                "Kader" to TeamBereich.KADER,
                "Stärken" to TeamBereich.STAERKEN,
                "Verträge" to TeamBereich.VERTRAEGE,
                "Statistik" to TeamBereich.STATISTIK,
                "Teaminfo" to TeamBereich.TEAMINFO,
            ) },
            selected = bereich,
            onSelect = { bereich = it as TeamBereich },
            compact = true,
        )

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                bereich == TeamBereich.TAKTIK -> TeamTaktikEditor()
                bereich == TeamBereich.FREUNDSCHAFT -> FreundschaftScreen()
                uiState.ladend && !hatDaten(bereich, uiState) -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.fehler != null && !hatDaten(bereich, uiState) -> {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.fehler.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(onClick = { neuLaden(bereich, viewModel) }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.padding(start = 4.dp))
                            Text("Erneut versuchen")
                        }
                    }
                }
                else -> when (bereich) {
                    TeamBereich.KADER -> KaderAnsicht(uiState, viewModel, onSpielerClick)
                    TeamBereich.VERTRAEGE -> VertraegeAnsicht(uiState.vertraege.orEmpty(), onSpielerClick)
                    TeamBereich.STAERKEN -> StaerkenAnsicht(uiState.staerken.orEmpty())
                    TeamBereich.STATISTIK -> StatistikAnsicht(
                        uiState.statistik.orEmpty(),
                        uiState.statistikGesamt,
                        viewModel::ladeStatistik,
                    )
                    TeamBereich.TEAMINFO -> TeaminfoAnsicht(uiState.teaminfo)
                    TeamBereich.TAKTIK -> TeamTaktikEditor()
                    TeamBereich.FREUNDSCHAFT -> FreundschaftScreen()
                }
            }
        }
    }
}

private fun hatDaten(bereich: TeamBereich, uiState: TeamUiState): Boolean = when (bereich) {
    TeamBereich.KADER -> uiState.kader.isNotEmpty()
    TeamBereich.VERTRAEGE -> uiState.vertraege != null
    TeamBereich.STAERKEN -> uiState.staerken != null
    TeamBereich.STATISTIK -> uiState.statistik != null
    TeamBereich.TEAMINFO -> uiState.teaminfo != null
    TeamBereich.TAKTIK -> true
    TeamBereich.FREUNDSCHAFT -> true
}

private fun neuLaden(bereich: TeamBereich, viewModel: TeamViewModel) = when (bereich) {
    TeamBereich.KADER -> viewModel.ladeKader()
    TeamBereich.VERTRAEGE -> viewModel.ladeVertraege()
    TeamBereich.STAERKEN -> viewModel.ladeStaerken()
    TeamBereich.STATISTIK -> viewModel.ladeStatistik(viewModel.uiState.value.statistikGesamt)
    TeamBereich.TEAMINFO -> viewModel.ladeTeaminfo()
    TeamBereich.TAKTIK -> Unit
    TeamBereich.FREUNDSCHAFT -> Unit
}

@Composable
private fun TeamTaktikEditor(
    viewModel: TaktikViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TaktikEditor(
        uiState = uiState,
        onToggle = viewModel::toggleCode,
        onNameChange = viewModel::setName,
        onSpeichern = viewModel::speichere,
        onLeeren = viewModel::leereRaster,
        onWaehleStandard = viewModel::waehleStandard,
        onWaehleEigene = viewModel::waehleEigene,
        onLaden = viewModel::ladeGewaehlte,
        onLoeschen = viewModel::loescheGewaehlte,
    )
}

@Composable
private fun KaderAnsicht(
    uiState: TeamUiState,
    viewModel: TeamViewModel,
    onSpielerClick: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        FilterLeiste(uiState.positionsFilter, viewModel::setzeFilter)
        SortLeiste(uiState.sortierung, viewModel::setzeSortierung)
        Text(
            "${uiState.gefiltert.size} Spieler",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(uiState.gefiltert, key = { it.pid }) { spieler ->
                SpielerZeile(spieler) { onSpielerClick(spieler.pid) }
            }
        }
    }
}

@Composable
private fun FilterLeiste(
    filter: SpielerPosition?,
    onFilter: (SpielerPosition?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        item {
            FilterChip(selected = filter == null, onClick = { onFilter(null) }, label = { Text("Alle") })
        }
        items(
            listOf(
                SpielerPosition.TOR,
                SpielerPosition.ABW,
                SpielerPosition.DMI,
                SpielerPosition.MIT,
                SpielerPosition.OMI,
                SpielerPosition.STU,
            )
        ) { position ->
            FilterChip(
                selected = filter == position,
                onClick = { onFilter(position) },
                label = { Text(posName(position)) },
            )
        }
    }
}

@Composable
private fun SortLeiste(
    sortierung: Sortierung,
    onSortierung: (Sortierung) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Sortierung.entries.forEachIndexed { index, s ->
            SegmentedButton(
                selected = sortierung == s,
                onClick = { onSortierung(s) },
                shape = SegmentedButtonDefaults.itemShape(index, Sortierung.entries.size),
            ) {
                Text(
                    when (s) {
                        Sortierung.POSITION -> "Position"
                        Sortierung.SKILL -> "Skill"
                        Sortierung.OPTI -> "Opti"
                        Sortierung.ALTER -> "Alter"
                        Sortierung.NUMMER -> "Nr."
                    },
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
fun SpielerZeile(
    spieler: KaderSpieler,
    onClick: () -> Unit,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(trikotFarbe(spieler.position), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(spieler.nummer, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(spieler.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    "${posName(spieler.position)} · ${spieler.alter} Jahre",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (spieler.sonderFaehigkeiten.isNotEmpty() || spieler.gesperrt) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        spieler.sonderFaehigkeiten.forEach { sf ->
                            SonderBadge(sf)
                            Spacer(Modifier.width(4.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        if (spieler.gesperrt) {
                            SperreStatus(spieler.sperre)
                        }
                    }
                }
            }
            WertSaeule("Skill", "${(spieler.skill * 10).toInt() / 10.0}")
            Spacer(Modifier.width(10.dp))
            WertSaeule("Opti", "${(spieler.opti * 10).toInt() / 10.0}")
            Spacer(Modifier.width(10.dp))
            WertSaeule("Fit", "${spieler.fit}")
            Spacer(Modifier.width(10.dp))
            WertSaeule("Mor", "${spieler.mor}")
        }
    }
}

@Composable
private fun WertSaeule(label: String, wert: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(wert, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SonderBadge(sf: SonderFaehigkeit) {
    Box(
        Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            sf.kuerzel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** Sperre wie auf der Website (z. B. „1L“) rechts neben den Sonderfähigkeiten. */
@Composable
private fun SperreStatus(sperre: String) {
    Text(
        sperre,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun VertraegeAnsicht(
    vertraege: List<VertragZeile>,
    onSpielerClick: (Long) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(vertraege, key = { it.pid }) { v ->
            Card(Modifier.fillMaxWidth().clickable { onSpielerClick(v.pid) }) {
                Column(Modifier.padding(12.dp)) {
                    Text(v.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gehalt", style = MaterialTheme.typography.labelSmall)
                        Text(v.gehalt ?: "–", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Laufzeit", style = MaterialTheme.typography.labelSmall)
                        Text(v.laufzeit ?: "–", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Marktwert", style = MaterialTheme.typography.labelSmall)
                        Text(v.marktwert ?: "–", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Geburtstag", style = MaterialTheme.typography.labelSmall)
                        Text(v.geburtstag ?: "–", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StaerkenAnsicht(staerken: List<StaerkeZeile>) {
    val skills = staerken.firstOrNull()?.werte?.keys?.toList().orEmpty()
    if (skills.isEmpty()) {
        Text("Keine Stärkenwerte gefunden.", Modifier.padding(24.dp))
        return
    }
    Column(Modifier.fillMaxSize()) {
        Text(
            "${staerken.size} Spieler · ${skills.size} Einzelwerte",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.width(130.dp)) {
                Text("Spieler", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                staerken.forEach { s ->
                    Text(
                        s.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(vertical = 9.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            skills.forEach { skill ->
                Column(Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        skill,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    staerken.forEach { s ->
                        Text(
                            s.werte[skill] ?: "–",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatistikAnsicht(
    statistik: List<StatistikZeile>,
    gesamt: Boolean,
    onGesamt: (Boolean) -> Unit,
) {
    val header = statistik.firstOrNull()?.werte?.keys?.toList().orEmpty()
    Column(Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            listOf(false to "Saison", true to "Gesamt").forEachIndexed { index, (istGesamt, label) ->
                SegmentedButton(
                    selected = gesamt == istGesamt,
                    onClick = { onGesamt(istGesamt) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                ) {
                    Text(label)
                }
            }
        }
        if (header.isEmpty()) {
            Text("Keine Statistik gefunden.", Modifier.padding(24.dp))
            return
        }
        Row(
            Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.width(130.dp)) {
                Text("Spieler", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                statistik.forEach { s ->
                    Text(
                        s.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(vertical = 9.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            header.forEach { kopf ->
                Column(Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        kopf,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    statistik.forEach { s ->
                        Text(
                            s.werte[kopf] ?: "–",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeaminfoAnsicht(teaminfo: Teaminfo?) {
    if (teaminfo == null) {
        Text("Keine Team-Informationen gefunden.", Modifier.padding(24.dp))
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    teaminfo.zeilen.forEach { (label, wert) ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(0.42f),
                            )
                            Text(
                                wert,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.58f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun posName(position: SpielerPosition): String = when (position) {
    SpielerPosition.TOR -> "Torwart"
    SpielerPosition.ABW -> "Abwehr"
    SpielerPosition.DMI -> "Def. Mittelfeld"
    SpielerPosition.MIT -> "Mittelfeld"
    SpielerPosition.OMI -> "Off. Mittelfeld"
    SpielerPosition.STU -> "Sturm"
    SpielerPosition.AMATEUR -> "Amateur"
}

internal fun trikotFarbe(position: SpielerPosition): Color = when (position) {
    SpielerPosition.TOR -> Color(0xFFF9A825)
    SpielerPosition.ABW -> Color(0xFF43A047)
    SpielerPosition.DMI -> Color(0xFF1E88E5)
    SpielerPosition.MIT -> Color(0xFF36BFF9)
    SpielerPosition.OMI -> Color(0xFFE040FB)
    SpielerPosition.STU -> Color(0xFFE53935)
    SpielerPosition.AMATEUR -> Color(0xFF9E9E9E)
}
