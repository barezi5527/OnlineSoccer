package com.onlinesoccer.app.feature.bewerbe

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.core.ui.theme.ligaTabellenZeilenHintergrund
import com.onlinesoccer.app.data.model.LigaOption
import com.onlinesoccer.app.data.model.LigaSpiel
import com.onlinesoccer.app.data.model.LigaSpieltag
import com.onlinesoccer.app.data.model.LigaTabellenKlasse
import com.onlinesoccer.app.data.model.LigaTabelle
import com.onlinesoccer.app.data.model.PokalAnsicht
import com.onlinesoccer.app.ui.HubTabs

private enum class BewerbeBereich { TABELLE, SPIELTAGE, POKAL }

/** Unter-Schalter innerhalb des Menüpunkts „Spieltage". */
private enum class SpieltageUnteransicht { SPIELTAGE, ELF_DE_SPIELTAGS }

@Composable
fun BewerbeScreen(
    onSpielbericht: (String?, String?) -> Unit,
    onTeamClick: (Long) -> Unit = {},
    onSpielerKarte: (Long) -> Unit = {},
    demo: Boolean = false,
    viewModel: BewerbeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var bereich by rememberSaveable { mutableStateOf(BewerbeBereich.TABELLE) }
    var spieltageUnteransicht by rememberSaveable { mutableStateOf(SpieltageUnteransicht.SPIELTAGE) }
    var ergebnisseSichtbar by rememberSaveable { mutableStateOf(false) }
    var pokalErgebnisseSichtbar by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(demo) {
        viewModel.setDemo(demo)
    }

    LaunchedEffect(bereich) {
        when (bereich) {
            BewerbeBereich.TABELLE -> viewModel.ladeTabelle()
            BewerbeBereich.SPIELTAGE -> viewModel.ladeSpieltag()
            BewerbeBereich.POKAL -> viewModel.ladePokal()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HubTabs(
                tabs = listOf(
                    "Ligatabelle" to BewerbeBereich.TABELLE,
                    "Spieltage" to BewerbeBereich.SPIELTAGE,
                    "Landespokal" to BewerbeBereich.POKAL,
                ),
                selected = bereich,
                onSelect = {
                    val gewaehlt = it as BewerbeBereich
                    if (gewaehlt != BewerbeBereich.SPIELTAGE) {
                        spieltageUnteransicht = SpieltageUnteransicht.SPIELTAGE
                    }
                    bereich = gewaehlt
                },
                modifier = Modifier.weight(1f),
            )
            if (bereich == BewerbeBereich.SPIELTAGE && spieltageUnteransicht == SpieltageUnteransicht.SPIELTAGE) {
                ErgebnisCheckbox(ergebnisseSichtbar) { ergebnisseSichtbar = it }
            }
            if (bereich == BewerbeBereich.POKAL) {
                ErgebnisCheckbox(pokalErgebnisseSichtbar) { pokalErgebnisseSichtbar = it }
            }
            if (bereich == BewerbeBereich.SPIELTAGE && spieltageUnteransicht == SpieltageUnteransicht.ELF_DE_SPIELTAGS) {
                IconButton(onClick = { spieltageUnteransicht = SpieltageUnteransicht.SPIELTAGE }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück zu den Spieltagen")
                }
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (bereich == BewerbeBereich.SPIELTAGE && spieltageUnteransicht == SpieltageUnteransicht.ELF_DE_SPIELTAGS) {
                ElfDesSpieltagsAnsicht(
                    onSpielerKarte = onSpielerKarte,
                    onVerein = onTeamClick,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                when {
                    uiState.ladend -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                    uiState.fehler != null -> {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
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
                        BewerbeBereich.TABELLE -> TabelleAnsicht(
                            tabelle = uiState.tabelle,
                            onSaison = viewModel::ladeTabelle,
                            onLiga = viewModel::waehleTabelleLiga,
                            onLand = viewModel::waehleTabelleLand,
                            onTab = viewModel::waehleTabelleTab,
                            onTeamClick = onTeamClick,
                            demo = demo,
                        )
                        BewerbeBereich.SPIELTAGE -> SpieltageAnsicht(
                            spieltag = uiState.spieltag,
                            onZat = viewModel::ladeSpieltag,
                            onSaison = viewModel::waehleSpieltagSaison,
                            onLiga = viewModel::waehleSpieltagLiga,
                            onLand = viewModel::waehleSpieltagLand,
                            ergebnisseSichtbar = ergebnisseSichtbar,
                            onErgebnisseSichtbar = { ergebnisseSichtbar = it },
                            onSpielbericht = onSpielbericht,
                            onElf = { spieltageUnteransicht = SpieltageUnteransicht.ELF_DE_SPIELTAGS },
                            demo = demo,
                        )
                        BewerbeBereich.POKAL -> PokalAnsicht(
                            pokal = uiState.pokal,
                            ergebnisseSichtbar = pokalErgebnisseSichtbar,
                            onSpielbericht = onSpielbericht,
                            onSaison = viewModel::waehlePokalSaison,
                            onRunde = viewModel::waehlePokalRunde,
                            onLand = viewModel::waehlePokalLand,
                            demo = demo,
                        )
                    }
                }
            }
        }
    }
}

private fun neuLaden(bereich: BewerbeBereich, viewModel: BewerbeViewModel) = when (bereich) {
    BewerbeBereich.TABELLE -> viewModel.ladeTabelle(force = true)
    BewerbeBereich.SPIELTAGE -> viewModel.ladeSpieltag(force = true)
    BewerbeBereich.POKAL -> viewModel.erneutLadePokal()
}

/** Einheitlicher Dropdown-Filter für die Bewerbe-Ansichten. */
@Composable
private fun FilterAuswahl(
    selected: Int,
    optionen: List<LigaOption>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leerLabel: String = "Auswahl",
) {
    var offen by remember { mutableStateOf(false) }
    val text = optionen.firstOrNull { it.wert == selected }?.label ?: leerLabel
    Box(modifier) {
        OutlinedButton(onClick = { offen = true }, modifier = Modifier.fillMaxWidth()) {
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(6.dp))
            Text("▾", fontSize = 10.sp)
        }
        DropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            optionen.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label) },
                    onClick = {
                        offen = false
                        if (opt.wert != selected) onSelect(opt.wert)
                    },
                )
            }
        }
    }
}

/** Einheitliches „Ergeb."-Kästchen in der Bewerbe-Kopfzeile. */
@Composable
private fun ErgebnisCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Text(
            "Ergeb.",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clickable { onCheckedChange(!checked) },
        )
    }
}

@Composable
private fun TabelleAnsicht(
    tabelle: LigaTabelle?,
    onSaison: (Int) -> Unit,
    onLiga: (Int) -> Unit,
    onLand: (Int) -> Unit,
    onTab: (Int) -> Unit,
    onTeamClick: (Long) -> Unit = {},
    demo: Boolean = false,
) {
    if (tabelle == null) {
        Text("Keine Tabelle gefunden.", Modifier.padding(24.dp))
        return
    }

    val kompakt = kompakteTabellenSpalten(tabelle.header)
    val cols = if (kompakt != null) kompakt else {
        val header = tabelle.header
        val clubIdx = header.indexOfFirst { it.equals("Club", ignoreCase = true) }.takeIf { it >= 0 } ?: 2
        buildList {
            header.forEachIndexed { idx, kopf ->
                val label = kopf.trim()
                if (label.isEmpty()) return@forEachIndexed
                val width = when {
                    idx == clubIdx -> 156.dp
                    idx == 0 -> 40.dp
                    else -> 56.dp
                }
                add(TabellenSpalte(label, idx, idx == clubIdx, width, wert = { getOrNull(idx) ?: "–" }))
            }
        }
    }
    val fülltBreite = kompakt != null
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        TabelleFilterLeiste(
            tabelle = tabelle,
            onSaison = onSaison,
            onLiga = onLiga,
            onLand = onLand,
            onTab = onTab,
        )
        if (cols.isEmpty()) {
            Text(
                if (demo) "Bitte zuerst Land und Liga auswählen."
                else "Keine Tabelle gefunden.",
                Modifier.padding(24.dp),
            )
            return@Column
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(if (!fülltBreite) Modifier.horizontalScroll(horizontalScroll) else Modifier),
        ) {
            Row {
                cols.forEach { sp ->
                    Text(
                        sp.kopf,
                        modifier = Modifier
                            .then(if (sp.gewicht != null) Modifier.weight(sp.gewicht!!) else Modifier.width(sp.breite))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = if (sp.team) TextAlign.Start else TextAlign.Center,
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(verticalScroll),
            ) {
                tabelle.zeilen.forEachIndexed { i, zeile ->
                    val teamId = tabelle.zeilenTeamIds.getOrNull(i)
                    val klasse = tabelle.zeilenKlasse.getOrNull(i)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(ligaTabellenZeilenHintergrund(klasse)),
                    ) {
                        cols.forEach { sp ->
                            val clickable = sp.team && teamId != null
                            Text(
                                sp.wert(zeile),
                                modifier = Modifier
                                    .then(if (sp.gewicht != null) Modifier.weight(sp.gewicht!!) else Modifier.width(sp.breite))
                                    .then(if (clickable) Modifier.clickable { onTeamClick(teamId) } else Modifier)
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (sp.team) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1,
                                textAlign = if (sp.team) TextAlign.Start else TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
        TabellenLegende()
    }
}

/** Kurzbezeichnung für die Legende unter der Tabelle. */
private fun legendeLabel(klasse: LigaTabellenKlasse): String = when (klasse) {
    LigaTabellenKlasse.OSC -> "Aufstieg/OSC"
    LigaTabellenKlasse.OSCQ -> "Rel. Auf/OSCQ"
    LigaTabellenKlasse.OSE -> "OSE"
    LigaTabellenKlasse.OSEQ -> "OSEQ"
    LigaTabellenKlasse.RELE -> "Rel. Ab"
    LigaTabellenKlasse.AB -> "Abstieg"
}

/** Legende der Platz-Farben unter der Ligatabelle (wie auf der Website). */
@Composable
private fun TabellenLegende() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LigaTabellenKlasse.entries.take(3).forEach { klasse ->
            Legendeneintrag(klasse, Modifier.weight(1f))
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LigaTabellenKlasse.entries.drop(3).forEach { klasse ->
            Legendeneintrag(klasse, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Legendeneintrag(klasse: LigaTabellenKlasse, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(ligaTabellenZeilenHintergrund(klasse), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            legendeLabel(klasse),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Filter-Leiste oberhalb der Ligatabelle: Liga, Land, Tabellenart, Saison. */
@Composable
private fun TabelleFilterLeiste(
    tabelle: LigaTabelle,
    onSaison: (Int) -> Unit,
    onLiga: (Int) -> Unit,
    onLand: (Int) -> Unit,
    onTab: (Int) -> Unit,
) {
    val filter = tabelle.filter
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterAuswahl(
                selected = filter?.liga ?: 0,
                optionen = tabelle.ligaOptionen,
                onSelect = onLiga,
                modifier = Modifier.weight(1f),
                leerLabel = "Liga",
            )
            FilterAuswahl(
                selected = filter?.land ?: 0,
                optionen = tabelle.landOptionen,
                onSelect = onLand,
                modifier = Modifier.weight(1f),
                leerLabel = "Land",
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterAuswahl(
                selected = filter?.tab ?: 0,
                optionen = tabelle.tabOptionen,
                onSelect = onTab,
                modifier = Modifier.weight(1f),
                leerLabel = "Tabelle",
            )
            FilterAuswahl(
                selected = tabelle.saison,
                optionen = saisonOptionen(tabelle.saisonen),
                onSelect = onSaison,
                modifier = Modifier.weight(1f),
                leerLabel = "Saison",
            )
        }
    }
}

/** Saison-Auswahl als einheitlich beschriftete Optionen („Saison 24“). */
private fun saisonOptionen(saisonen: List<com.onlinesoccer.app.data.model.LigaSaison>): List<LigaOption> =
    saisonen.mapNotNull { s ->
        if (s.wert <= 0) null
        else LigaOption(s.wert, if (s.label.startsWith("Saison")) s.label else "Saison ${s.label.trim()}")
    }

internal data class TabellenSpalte(
    val kopf: String,
    val headIdx: Int,
    val team: Boolean,
    val breite: androidx.compose.ui.unit.Dp,
    val wert: List<String>.() -> String,
    val gewicht: Float? = null,
)

/**
 * Reduzierte Ligatabelle wie gewünscht (kompakte Spalten):
 * Platz, Team, Sp., Tore („5:1"), Diff., Pkt. – mit kurzen Kopfbezeichnungen.
 */
internal fun kompakteTabellenSpalten(header: List<String>): List<TabellenSpalte>? {
    fun idx(vararg alias: String): Int? {
        for (full in alias) {
            val i = header.indexOfFirst { it.equals(full, ignoreCase = true) || it.trim() == full }
            if (i >= 0) return i
        }
        return null
    }
    val platz = idx("#") ?: 0
    val team = idx("Club", "Team", "Verein")
    val spiele = idx("Spiele", "Sp.")
    val torePlus = idx("Tore+", "Tore +", "T+")
    val toreMinus = idx("Tore-", "Tore -", "T-", "Gegentore")
    val diff = idx("Tore +/-", "Diff.", "Tore/Sp.", "Tordifferenz")
    val punkte = idx("Punkte", "Pkt.")
    if (team == null) return null

    fun holen(spalte: Int?): List<String>.() -> String = { getOrNull(spalte ?: -1) ?: "–" }

    return buildList {
        add(TabellenSpalte("#", platz, team = false, breite = 44.dp, gewicht = 0.8f, wert = { holen(platz)() }))
        add(TabellenSpalte("Team", team, team = true, breite = 160.dp, gewicht = 3.2f, wert = { holen(team)() }))
        add(TabellenSpalte("Sp.", spiele ?: -1, team = false, breite = 40.dp, gewicht = 0.9f, wert = { holen(spiele)() }))
        add(TabellenSpalte("Tore", torePlus ?: -1, team = false, breite = 60.dp, gewicht = 1.2f, wert = {
            val a = torePlus?.let { getOrNull(it) }?.takeIf { it.isNotBlank() } ?: "–"
            val b = toreMinus?.let { getOrNull(it) }?.takeIf { it.isNotBlank() } ?: "–"
            if (a == "–" && b == "–") "–" else "$a:$b"
        }))
        add(TabellenSpalte("Diff.", diff ?: -1, team = false, breite = 52.dp, gewicht = 1.0f, wert = { holen(diff)() }))
        add(TabellenSpalte("Pkt.", punkte ?: -1, team = false, breite = 44.dp, gewicht = 0.9f, wert = { holen(punkte)() }))
    }
}

@Composable
private fun SpieltageAnsicht(
    spieltag: LigaSpieltag?,
    onZat: (Int) -> Unit,
    onSaison: (Int) -> Unit,
    onLiga: (Int) -> Unit,
    onLand: (Int) -> Unit,
    ergebnisseSichtbar: Boolean,
    onErgebnisseSichtbar: (Boolean) -> Unit,
    onSpielbericht: (String?, String?) -> Unit,
    onElf: (() -> Unit)? = null,
    demo: Boolean = false,
) {
    if (spieltag == null) {
        Text("Keine Spieltage gefunden.", Modifier.padding(24.dp))
        return
    }
    if (spieltag.spiele.isEmpty() && demo) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterAuswahl(
                selected = spieltag.liga,
                optionen = spieltag.ligaOptionen,
                onSelect = onLiga,
                modifier = Modifier.weight(1f),
                leerLabel = "Liga",
            )
            FilterAuswahl(
                selected = spieltag.land,
                optionen = spieltag.landOptionen,
                onSelect = onLand,
                modifier = Modifier.weight(1f),
                leerLabel = "Land",
            )
        }
        Text("Bitte zuerst Land und Liga auswählen.", Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        return
    }
    if (spieltag.spiele.isEmpty()) {
        Text("Keine Spieltage gefunden.", Modifier.padding(24.dp))
        return
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterAuswahl(
                selected = spieltag.liga,
                optionen = spieltag.ligaOptionen,
                onSelect = onLiga,
                modifier = Modifier.weight(1f),
                leerLabel = "Liga",
            )
            FilterAuswahl(
                selected = spieltag.land,
                optionen = spieltag.landOptionen,
                onSelect = onLand,
                modifier = Modifier.weight(1f),
                leerLabel = "Land",
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ZatSelector(spieltag, onZat)
            FilterAuswahl(
                selected = spieltag.saison,
                optionen = spieltag.saisonOptionen,
                onSelect = onSaison,
                leerLabel = "Saison",
            )
            if (onElf != null) {
                FilterChip(
                    selected = false,
                    onClick = onElf,
                    label = { Text("Elf des Spieltags") },
                )
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(spieltag.spiele, key = { "${it.heim}-${it.gast}" }) { spiel ->
                SpielZeile(spiel, ergebnisseSichtbar, onSpielbericht)
            }
        }
    }
}

/** Spieltag-Auswahl (stauswahl) für die Spieltage-Liste. */
@Composable
private fun ZatSelector(
    spieltag: LigaSpieltag,
    onZat: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("Spieltag ${spieltag.zat}")
            Spacer(Modifier.width(6.dp))
            Text("▾", fontSize = 10.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            spieltag.zatOptionen.forEach { zat ->
                DropdownMenuItem(
                    text = { Text("Spieltag $zat") },
                    onClick = {
                        expanded = false
                        if (zat != spieltag.zat) onZat(zat)
                    },
                )
            }
        }
    }
}

@Composable
private fun SpielZeile(
    spiel: LigaSpiel,
    ergebnisseSichtbar: Boolean,
    onSpielbericht: (String?, String?) -> Unit,
) {
    val keinBericht = spiel.berichtSid == null && spiel.berichtUrl == null
    Card(
        Modifier
            .fillMaxWidth()
            .then(
                if (!keinBericht) Modifier.clickable { onSpielbericht(spiel.berichtSid, spiel.berichtUrl) } else Modifier,
            ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    spiel.heim,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (spiel.eigenerVerein) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .padding(horizontal = 6.dp)
                        .background(
                            if (spiel.gespielt) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        if (!spiel.gespielt || !ergebnisseSichtbar) "vs."
                        else "${spiel.toreHeim ?: "–"}:${spiel.toreGast ?: "–"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    spiel.gast,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (spiel.eigenerVerein) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
            if (!keinBericht) {
                Text(
                    if (spiel.gespielt) "Spielbericht öffnen" else "Vorschau",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun PokalAnsicht(
    pokal: PokalAnsicht?,
    ergebnisseSichtbar: Boolean,
    onSpielbericht: (String?, String?) -> Unit,
    onSaison: (Int) -> Unit,
    onRunde: (Int) -> Unit,
    onLand: (Int) -> Unit,
    demo: Boolean = false,
) {
    if (pokal == null) {
        Text("Keine Pokalrunden gefunden.", Modifier.padding(24.dp))
        return
    }
    if (pokal.runden.isEmpty() && demo) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterAuswahl(
                        selected = pokal.land,
                        optionen = pokal.landOptionen,
                        onSelect = onLand,
                        modifier = Modifier.weight(1f),
                        leerLabel = "Land",
                    )
                    FilterAuswahl(
                        selected = pokal.saison,
                        optionen = saisonOptionen(pokal.saisonen),
                        onSelect = onSaison,
                        modifier = Modifier.weight(1f),
                        leerLabel = "Saison",
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterAuswahl(
                        selected = pokal.runde,
                        optionen = pokal.rundenOptionen.map { LigaOption(it.wert, it.label) },
                        onSelect = onRunde,
                        modifier = Modifier.weight(1f),
                        leerLabel = "Runde",
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
            Text("Bitte zuerst ein Land auswählen.", Modifier.padding(vertical = 12.dp))
        }
        return
    }
    if (pokal.runden.isEmpty()) {
        Text("Keine Pokalrunden gefunden.", Modifier.padding(24.dp))
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            "Landespokal · Saison ${pokal.saison.takeIf { it > 0 } ?: "–"}" +
                " · ${if (pokal.runde > 0) "Runde ${pokal.runde}" else "Runde –"}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterAuswahl(
                    selected = pokal.land,
                    optionen = pokal.landOptionen,
                    onSelect = onLand,
                    modifier = Modifier.weight(1f),
                    leerLabel = "Land",
                )
                FilterAuswahl(
                    selected = pokal.saison,
                    optionen = saisonOptionen(pokal.saisonen),
                    onSelect = onSaison,
                    modifier = Modifier.weight(1f),
                    leerLabel = "Saison",
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterAuswahl(
                    selected = pokal.runde,
                    optionen = pokal.rundenOptionen.map { LigaOption(it.wert, it.label) },
                    onSelect = onRunde,
                    modifier = Modifier.weight(1f),
                    leerLabel = "Runde",
                )
                Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
        pokal.runden.forEachIndexed { index, runde ->
            Text(
                if (pokal.runden.size > 1) "Runde ${index + 1}" else "Spiele",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            runde.spiele.forEach { spiel ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .then(
                            if (spiel.berichtUrl != null) {
                                Modifier.clickable { onSpielbericht(null, spiel.berichtUrl) }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            spiel.heim,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (ergebnisseSichtbar) spiel.ergebnis ?: "vs." else "vs.",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        Text(
                            spiel.gast,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End,
                        )
                    }
                    if (spiel.berichtUrl != null) {
                        Text(
                            "Spielbericht öffnen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}