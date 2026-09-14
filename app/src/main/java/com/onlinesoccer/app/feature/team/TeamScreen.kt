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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.core.ui.theme.PositionsBadge
import com.onlinesoccer.app.core.ui.theme.trikotFarbe
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

private enum class TeamBereich { MANNSCHAFT, TAKTIK, FREUNDSCHAFT, TEAMINFORMATIONEN }

@Composable
fun TeamScreen(
    onSpielerClick: (Long) -> Unit,
    onSeiteClick: (String) -> Unit,
    onTeaminformationenClick: () -> Unit,
    viewModel: TeamViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var bereich by remember { mutableStateOf(TeamBereich.MANNSCHAFT) }

    LaunchedEffect(bereich) {
        when (bereich) {
            TeamBereich.MANNSCHAFT -> if (uiState.kader.isEmpty()) viewModel.ladeKader()
            else -> Unit
        }
    }

    Column(Modifier.fillMaxSize()) {
        HubTabs(
            tabs = remember { listOf(
                "Mannschaft" to TeamBereich.MANNSCHAFT,
                "Training" to "training.php",
                "Trainer" to "trainer.php",
                "Taktik-Editor" to TeamBereich.TAKTIK,
                "Verträge verlängern" to "vt.php",
                "Freundschaftsspiele" to TeamBereich.FREUNDSCHAFT,
                "Teaminformationen" to TeamBereich.TEAMINFORMATIONEN,
            ) },
            selected = bereich,
            onSelect = { value ->
                when (value) {
                    is String -> onSeiteClick(value)
                    is TeamBereich -> when (value) {
                        TeamBereich.TEAMINFORMATIONEN -> onTeaminformationenClick()
                        else -> bereich = value
                    }
                }
            },
            compact = true,
        )

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                bereich == TeamBereich.MANNSCHAFT -> when {
                    uiState.ladend && uiState.kader.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.fehler != null && uiState.kader.isEmpty() -> {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(uiState.fehler.orEmpty(), color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(12.dp))
                            FilledTonalButton(onClick = viewModel::ladeKader) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.padding(start = 4.dp))
                                Text("Erneut versuchen")
                            }
                        }
                    }
                    else -> KaderAnsicht(uiState, viewModel, onSpielerClick)
                }
                bereich == TeamBereich.TAKTIK -> TeamTaktikEditor()
                bereich == TeamBereich.FREUNDSCHAFT -> FreundschaftScreen()
                else -> Unit
            }
        }
    }
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
internal fun KaderAnsicht(
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
                    "${posName(spieler.position)} · ${spieler.alter?.let { "$it Jahre" } ?: "Alter k. A."}",
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
internal fun VertraegeAnsicht(
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (v.position != SpielerPosition.AMATEUR) {
                            PositionsBadge(v.position)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(v.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    }
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
internal fun StaerkenAnsicht(staerken: List<StaerkeZeile>) {
    val skills = staerken.firstOrNull()?.werte?.keys?.toList().orEmpty()
    if (skills.isEmpty()) {
        Text("Keine Stärkenwerte gefunden.", Modifier.padding(24.dp))
        return
    }
    SynchronWerteTabelle(
        pids = staerken.map { it.pid },
        namen = staerken.map { it.name },
        positionen = staerken.map { it.position },
        kopfzeilen = skills,
        wert = { zeile, spalte -> staerken[zeile].werte[skills[spalte]] ?: "–" },
        zusatzInfo = "${staerken.size} Spieler · ${skills.size} Einzelwerte",
    )
}

@Composable
internal fun StatistikAnsicht(
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
        SynchronWerteTabelle(
            pids = statistik.map { it.pid },
            namen = statistik.map { it.name },
            positionen = statistik.map { it.position },
            kopfzeilen = header,
            wert = { zeile, spalte -> statistik[zeile].werte[header[spalte]] ?: "–" },
            zusatzInfo = "${statistik.size} Spieler · ${header.size} Statistikwerte",
        )
    }
}

private val NAME_SPALTE = 144.dp
private val WERT_SPALTE = 60.dp
private val ZEILEN_HOEHE = 40.dp

/**
 * Synchron scrolldende Wertetabelle mit fixierter Kopfzeile und fixierter Namensspalte.
 * Eine angetippte Spielerzeile wird zeilenübergreifend markiert (gute Lesbarkeit).
 */
@Composable
private fun SynchronWerteTabelle(
    pids: List<Long>,
    namen: List<String>,
    kopfzeilen: List<String>,
    wert: (zeile: Int, spalte: Int) -> String,
    zusatzInfo: String = "",
    positionen: List<SpielerPosition>? = null,
) {
    if (pids.isEmpty()) {
        Text("Keine Daten gefunden.", Modifier.padding(24.dp))
        return
    }

    var markierterPid by rememberSaveable { mutableStateOf<Long?>(null) }
    val vertScroll = rememberScrollState()
    val horizScroll = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        if (zusatzInfo.isNotBlank()) {
            Text(
                zusatzInfo,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
            TabellenKopfZelle("Spieler", NAME_SPALTE)
            Row(Modifier.horizontalScroll(horizScroll)) {
                kopfzeilen.forEach { kopf ->
                    TabellenKopfZelle(kopf, WERT_SPALTE)
                }
            }
        }

        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .width(NAME_SPALTE)
                    .verticalScroll(vertScroll),
            ) {
                namen.forEachIndexed { zeile, name ->
                    val pid = pids[zeile]
                    val markiert = pid == markierterPid
                    val position = positionen?.getOrNull(zeile)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(ZEILEN_HOEHE)
                            .background(zeilenFarbe(zeile, markiert))
                            .clickable { markierterPid = pid },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (markiert) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .width(3.dp)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                        if (position != null && position != SpielerPosition.AMATEUR) {
                            Spacer(Modifier.width(6.dp))
                            PositionsBadge(position)
                        }
                        Text(
                            name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (markiert) FontWeight.Bold else FontWeight.Normal,
                            color = if (markiert) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.horizontalScroll(horizScroll)) {
                    Column(Modifier.verticalScroll(vertScroll)) {
                        pids.forEachIndexed { zeile, pid ->
                            val markiert = pid == markierterPid
                            Row(
                                Modifier
                                    .height(ZEILEN_HOEHE)
                                    .background(zeilenFarbe(zeile, markiert))
                                    .clickable { markierterPid = pid },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                kopfzeilen.forEachIndexed { spalte, _ ->
                                    Box(
                                        Modifier
                                            .width(WERT_SPALTE)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            wert(zeile, spalte),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (markiert) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (markiert) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Clip,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabellenKopfZelle(text: String, breite: Dp) {
    Box(
        Modifier
            .width(breite)
            .height(ZEILEN_HOEHE)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun zeilenFarbe(zahl: Int, markiert: Boolean): Color = when {
    markiert -> MaterialTheme.colorScheme.primaryContainer
    zahl % 2 == 1 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    else -> Color.Transparent
}

@Composable
internal fun TeaminfoAnsicht(teaminfo: Teaminfo?) {
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
