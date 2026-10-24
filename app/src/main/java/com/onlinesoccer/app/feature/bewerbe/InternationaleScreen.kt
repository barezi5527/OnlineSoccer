package com.onlinesoccer.app.feature.bewerbe

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.core.ui.TeamWappen
import com.onlinesoccer.app.core.ui.theme.internationaleTabellenZeilenHintergrund
import com.onlinesoccer.app.data.model.InternationaleAnsicht
import com.onlinesoccer.app.data.model.InternationaleFilter
import com.onlinesoccer.app.data.model.InternationaleOption
import com.onlinesoccer.app.data.model.InternationaleSpiel
import com.onlinesoccer.app.data.model.InternationaleTabelleKlasse
import com.onlinesoccer.app.data.model.InternationaleTeilnehmerGruppe
import com.onlinesoccer.app.data.model.WertTabelle
import com.onlinesoccer.app.ui.HubTabs

/**
 * Internationale Wettbewerbe, gegliedert nach Bereichen.
 *
 * Statt zehn gleichrangiger Reiter gibt es fünf Bereiche ([InternationaleBereich]).
 * Jeder Bereich listet seine Ansichten als sichtbare Einträge auf – die Ansichten
 * selbst teilen sich in „Begegnungen" und „Tabelle", damit die Gruppenphasen-Tabelle
 * erreichbar bleibt, ohne einen weiteren Menüpunkt anzulegen.
 */
private enum class InternationaleBereich(val label: String, val beschreibung: String) {
    TEILNEHMER(
        "Teilnehmer",
        "Welche Vereine starten in den internationalen Wettbewerben",
    ),
    OSC(
        "OSC",
        "OS Championscup: Qualifikation, Gruppenphase und Finalrunde",
    ),
    OSE(
        "OSE",
        "OS Europacup: Qualifikation, Gruppenphase und Finalrunde",
    ),
    SUPERCUP(
        "Supercup",
        "Europapokal der Vereine gegen den Champion",
    ),
    RANKINGS(
        "Rankings",
        "OS- und Club-Ranking",
    ),
}

/** Seitenpfad einer Ansicht; `null` steht für die Bereichsübersicht. */
private enum class InternationaleAnsichtId(
    val label: String,
    val path: String?,
    val mitTabelle: Boolean = false,
    /** `<h2>`-Abschnitt der Teilnehmerseite, auf den die Ansicht beschränkt ist. */
    val teilnehmerAbschnitt: String? = null,
) {
    TEILNEHMER_OSC("OSC-Teilnehmer", "/osneu/intTeilnehmer", teilnehmerAbschnitt = "OSC"),
    TEILNEHMER_OSCQ("OSC-Qualifikation", "/osneu/intTeilnehmer", teilnehmerAbschnitt = "OSCQ"),
    TEILNEHMER_OSE("OSE-Teilnehmer", "/osneu/intTeilnehmer", teilnehmerAbschnitt = "OSE"),
    TEILNEHMER_OSEQ("OSE-Qualifikation", "/osneu/intTeilnehmer", teilnehmerAbschnitt = "OSEQ"),
    OSC_QUALI("Qualifikation", "/osneu/oscq"),
    OSC_GRUPPE("Gruppenphase", "/osneu/oscgp", mitTabelle = true),
    OSC_FINAL("Finalrunde", "/osneu/oscfr"),
    OSE_QUALI("Qualifikation", "/osneu/oseq"),
    OSE_GRUPPE("Gruppenphase", "/osneu/osegp", mitTabelle = true),
    OSE_FINAL("Finalrunde", "/osneu/osefr"),
    SUPERCUP("Supercup", "/osneu/supercup"),
    OS_RANKING("OS-Ranking", "/osneu/osranking"),
    CLUB_RANKING("Club-Ranking", "/osneu/clubranking"),
}

private val bereichAnsichten = mapOf(
    InternationaleBereich.TEILNEHMER to listOf(
        InternationaleAnsichtId.TEILNEHMER_OSC,
        InternationaleAnsichtId.TEILNEHMER_OSCQ,
        InternationaleAnsichtId.TEILNEHMER_OSE,
        InternationaleAnsichtId.TEILNEHMER_OSEQ,
    ),
    InternationaleBereich.OSC to listOf(
        InternationaleAnsichtId.OSC_QUALI,
        InternationaleAnsichtId.OSC_GRUPPE,
        InternationaleAnsichtId.OSC_FINAL,
    ),
    InternationaleBereich.OSE to listOf(
        InternationaleAnsichtId.OSE_QUALI,
        InternationaleAnsichtId.OSE_GRUPPE,
        InternationaleAnsichtId.OSE_FINAL,
    ),
    InternationaleBereich.SUPERCUP to listOf(
        InternationaleAnsichtId.SUPERCUP,
    ),
    InternationaleBereich.RANKINGS to listOf(
        InternationaleAnsichtId.OS_RANKING,
        InternationaleAnsichtId.CLUB_RANKING,
    ),
)

/** Bereiche, in denen Begegnungsergebnisse standardmäßig verborgen bleiben. */
private val ergebnisAnsichten = setOf(
    InternationaleAnsichtId.OSC_QUALI,
    InternationaleAnsichtId.OSC_GRUPPE,
    InternationaleAnsichtId.OSC_FINAL,
    InternationaleAnsichtId.OSE_QUALI,
    InternationaleAnsichtId.OSE_GRUPPE,
    InternationaleAnsichtId.OSE_FINAL,
    InternationaleAnsichtId.SUPERCUP,
)

@Composable
fun InternationaleScreen(
    onSpielbericht: (String?, String?) -> Unit,
    onTeamClick: (Long) -> Unit,
    viewModel: InternationaleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var bereich by rememberSaveable { mutableStateOf(InternationaleBereich.OSC) }
    // Ziel des Zurück-Pfeils: der zuletzt gewählte Bereich, wenn gerade eine
    // direkt geöffnete Einzelansicht (Supercup) offen ist.
    var vorherigerBereich by rememberSaveable { mutableStateOf(InternationaleBereich.OSC) }
    var ansicht by rememberSaveable { mutableStateOf<InternationaleAnsichtId?>(null) }
    var zeigeTabelle by rememberSaveable { mutableStateOf(false) }
    var ergebnisseSichtbar by rememberSaveable { mutableStateOf(false) }
    var gewaehlterZat by rememberSaveable { mutableStateOf<String?>(null) }
    var clubSuche by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(bereich, ansicht) {
        if (ansicht != InternationaleAnsichtId.CLUB_RANKING) clubSuche = ""
        val pfad = ansicht?.path ?: return@LaunchedEffect
        // Rückkehr aus dem Mannschaftskader: derselbe Bildschirm wird neu zusammengesetzt,
        // das ViewModel lebt weiter. Ohne diese Prüfung würden Tabelle, ZAT und Saison
        // zurückgesetzt und die Ansicht wieder im Begegnungsmodus starten.
        if (viewModel.istAnsichtGeladen(ansicht!!.name)) return@LaunchedEffect
        zeigeTabelle = false
        ergebnisseSichtbar = false
        gewaehlterZat = null
        viewModel.lade(ansicht!!.name, pfad)
    }

val geladen = uiState.ansicht

    // Supercup hat nur eine Ansicht: der Chip öffnet die Begegnungen direkt,
    // die Zwischenliste mit dem gleichnamigen Unterpunkt entfällt.
    val direktOffen = bereichAnsichten.getValue(bereich).singleOrNull()
    // Ziel des Zurück-Pfeils: aus einer direkt geöffneten Ansicht zurück in den
    // zuvor gewählten Bereich, sonst in die Liste des eigenen Bereichs.
    val zurueckNach = if (direktOffen != null) vorherigerBereich else bereich
    // Zurück-Pfeil und System-Zurück (Wischgeste/Taste) müssen denselben Schritt
    // gehen: aus einer Ansicht in die Übersicht des Bereichs, nicht direkt aus
    // der ganzen Route ins Dashboard.
    val zurueckZurUebersicht: () -> Unit = {
        bereich = zurueckNach
        ansicht = bereichAnsichten.getValue(zurueckNach).singleOrNull()
        viewModel.verwerfeAnsicht()
    }
    BackHandler(enabled = ansicht != null, onBack = zurueckZurUebersicht)

    Column(Modifier.fillMaxSize()) {
        if (ansicht == null) {
            HubTabs(
                tabs = InternationaleBereich.entries.map { it.label to it },
                selected = bereich,
                onSelect = {
                    val gewaehlt = it as InternationaleBereich
                    if (gewaehlt != bereich) vorherigerBereich = bereich
                    bereich = gewaehlt
                    ansicht = bereichAnsichten.getValue(gewaehlt).singleOrNull()
                },
            )
        } else {
            ZurueckZeile(
                titel = ansicht!!.label,
                bereich = bereich.label,
                zurueckNach = zurueckNach.label,
                zeigeErgebnisse = ansicht!! in ergebnisAnsichten,
                ergebnisseSichtbar = ergebnisseSichtbar,
                onErgebnisseSichtbar = { ergebnisseSichtbar = it },
                // Beim erneuten Öffnen soll wieder frisch geladen werden, nach dem
                // Zurück aus dem Kader bleibt die bisherige Ansicht dagegen erhalten.
                onZurueck = zurueckZurUebersicht,
            )
        }
        Box(Modifier.fillMaxSize().weight(1f)) {
            when {
                ansicht == null -> BereichUebersicht(
                    bereich = bereich,
                    onSelect = { ansicht = it },
                )
                uiState.fehler != null && geladen == null -> FehlerAnsicht(
                    fehler = uiState.fehler!!,
                    onRetry = { viewModel.lade(ansicht!!.name, ansicht!!.path!!) },
                )
                geladen != null -> AnsichtInhalt(
                    ansichtId = ansicht!!,
                    ansicht = geladen,
                    zeigeTabelle = zeigeTabelle,
                    onZeigeTabelle = { zeigeTabelle = it },
                    ergebnisseSichtbar = ergebnisseSichtbar,
                    gewaehlterZat = gewaehlterZat,
                    onZat = { gewaehlterZat = it },
                    clubSuche = clubSuche,
                    onClubSuche = { clubSuche = it },
                    onFilter = { viewModel.lade(ansicht!!.name, ansicht!!.path!!, it) },
                    onSpielbericht = onSpielbericht,
                    onTeamClick = onTeamClick,
                )
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            if (uiState.ladend && geladen != null && ansicht != null) {
                CircularProgressIndicator(Modifier.align(Alignment.TopCenter).padding(8.dp))
            }
        }
    }
}

/**
 * Kopfzeile einer Ansicht: zurück, Ansicht über Bereich und – bei den Wettbewerben
 * mit verborgenen Ergebnissen – rechts der Schalter „Ergebnisse anzeigen". Die
 * Überschrift des Inhalts ist damit entfernt, siehe [AnsichtInhalt].
 *
 * [zurueckNach] ist das Ziel des Pfeils und weicht bei direkt geöffneten
 * Einzelansichten vom eigenen [bereich] ab, etwa beim Supercup.
 */
@Composable
private fun ZurueckZeile(
    titel: String,
    bereich: String,
    zurueckNach: String,
    zeigeErgebnisse: Boolean,
    ergebnisseSichtbar: Boolean,
    onErgebnisseSichtbar: (Boolean) -> Unit,
    onZurueck: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onZurueck) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück zu $zurueckNach")
        }
        Column(Modifier.weight(1f)) {
            Text(
                titel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Beim Supercup tragen Bereich und Ansicht denselben Namen; die
            // Zeile wäre dann zweimal dieselbe Überschrift.
            if (bereich != titel) {
                Text(
                    bereich,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (zeigeErgebnisse) {
            CheckboxZeile("Ergebnisse anzeigen", ergebnisseSichtbar, onErgebnisseSichtbar)
        }
    }
}

/** Übersicht eines Bereichs: die Ansichten als große, sichtbare Einträge. Nur
 * Bereiche mit mehreren Ansichten brauchen diese Liste, beim Supercup mit nur
 * einer Ansicht öffnet der Chip die Begegnungen direkt. */
@Composable
private fun BereichUebersicht(
    bereich: InternationaleBereich,
    onSelect: (InternationaleAnsichtId) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Text(
                bereich.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                bereich.beschreibung,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        items(bereichAnsichten.getValue(bereich), key = { it.name }) { eintrag ->
            Card(
                Modifier.fillMaxWidth().clickable { onSelect(eintrag) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        eintrag.label,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Inhalt einer Ansicht. Die Überschrift steht in [ZurueckZeile] oben; das
 * Server-`<h1>` der Seite nicht noch einmal darüber setzen, sonst stünden
 * „Qualifikation / OSC" und „OSC-Qualifikation" doppelt im Kopf.
 */
@Composable
private fun AnsichtInhalt(
    ansichtId: InternationaleAnsichtId,
    ansicht: InternationaleAnsicht,
    zeigeTabelle: Boolean,
    onZeigeTabelle: (Boolean) -> Unit,
    ergebnisseSichtbar: Boolean,
    gewaehlterZat: String?,
    onZat: (String?) -> Unit,
    clubSuche: String,
    onClubSuche: (String) -> Unit,
    onFilter: (InternationaleFilter) -> Unit,
    onSpielbericht: (String?, String?) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
        val zeigeTabellenAnsicht = ansichtId.mitTabelle && zeigeTabelle && ansicht.tabellen.isNotEmpty()
    val spiele = if (gewaehlterZat == null) {
        ansicht.spiele
    } else {
        ansicht.spiele.filter { it.zat == gewaehlterZat }
    }

    if (zeigeTabellenAnsicht) {
        // Eigener Aufbau: Filter und Umschalter bleiben oben stehen, die
        // Spaltenüberschrift der Tabellen fixiert darüber, darunter rollt nur der
        // Tabellenteil. Ein Scrollen der ganzen Seite wäre hier doppelt.
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterLeiste(
                ansicht = ansicht,
                onFilter = onFilter,
                zeigeErgebnisFilter = false,
                gewaehlterZat = gewaehlterZat,
                onZat = onZat,
                zeigeZatFilter = false,
            )
            AnsichtUmschalter(zeigeTabelle, onZeigeTabelle)
            TabelleMitFixerKopfzeile(ansicht.tabellen, onTeamClick, Modifier.weight(1f))
            if (ansicht.tabellen.any { it.zeilenKlasse.any { klasse -> klasse != null } }) {
                InternationaleTabellenLegende()
            }
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterLeiste(
            ansicht = ansicht,
            onFilter = onFilter,
            zeigeErgebnisFilter = ansichtId !in ergebnisAnsichten,
            gewaehlterZat = gewaehlterZat,
            onZat = onZat,
        )
        if (ansichtId.mitTabelle && ansicht.tabellen.isNotEmpty()) {
            AnsichtUmschalter(zeigeTabelle, onZeigeTabelle)
        }

        if (ansicht.teilnehmer.isNotEmpty()) {
            val abschnitt = ansichtId.teilnehmerAbschnitt
            ansicht.teilnehmer
                .filter { abschnitt == null || it.bezeichnung.equals(abschnitt, ignoreCase = true) }
                .forEach { gruppe ->
                    TeilnehmerGruppe(gruppe, onTeamClick)
                }
        } else {
            if (spiele.isNotEmpty()) {
                Text(
                    "Begegnungen",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (ansichtId == InternationaleAnsichtId.OSC_QUALI ||
                    ansichtId == InternationaleAnsichtId.OSE_QUALI
                ) {
                    qualifikationsPaarungen(spiele).forEach { paarung ->
                        QualifikationsPaarungsKarte(
                            paarung = paarung,
                            ergebnisseSichtbar = ergebnisseSichtbar,
                            onSpielbericht = onSpielbericht,
                            onTeamClick = onTeamClick,
                        )
                    }
                } else {
                    spiele.forEach { spiel ->
                        BegegnungCard(spiel, ergebnisseSichtbar, onSpielbericht, onTeamClick)
                    }
                }
            }
            // In der Gruppenphase gibt es den Umschalter "Tabelle"; die Tabelle dort
            // nicht noch einmal unter den Begegnungen ausgeben.
            if (ansichtId == InternationaleAnsichtId.CLUB_RANKING) {
                ansicht.tabellen.firstOrNull()?.let { tabelle ->
                    ClubRankingTabelle(
                        tabelle = tabelle,
                        suchtext = clubSuche,
                        onSuchtext = onClubSuche,
                        onTeamClick = onTeamClick,
                    )
                }
            } else if (!ansichtId.mitTabelle) {
                ansicht.tabellen.forEach { tabelle ->
                    Tabelle(tabelle = tabelle, onTeamClick = onTeamClick)
                }
                if (ansicht.teilnehmer.isEmpty() &&
                    ansicht.tabellen.any { it.zeilenKlasse.any { klasse -> klasse != null } }
                ) {
                    InternationaleTabellenLegende()
                }
            }
            if (ansicht.abschnitte.isNotEmpty()) {
                Text(
                    "Weitere Informationen",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                ansicht.abschnitte.forEach { abschnitt ->
                    Card(Modifier.fillMaxWidth()) {
                        Text(abschnitt, Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (spiele.isEmpty() && ansicht.tabellen.isEmpty() && ansicht.abschnitte.isEmpty()) {
                Text(
                    if (ansichtId.mitTabelle || ansichtId in ergebnisAnsichten) {
                        "Zu dieser Auswahl sind noch keine Spiele eingetragen."
                    } else {
                        "Für diese Auswahl liegen derzeit keine Daten vor."
                    },
                    Modifier.padding(16.dp),
                )
            } else if (spiele.isEmpty() && ansicht.spiele.isNotEmpty()) {
                Text(
                    "Für ${gewaehlterZat.orEmpty()} sind keine Begegnungen eingetragen.",
                    Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun AnsichtUmschalter(zeigeTabelle: Boolean, onZeigeTabelle: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = !zeigeTabelle,
            onClick = { onZeigeTabelle(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) { Text("Begegnungen") }
        SegmentedButton(
            selected = zeigeTabelle,
            onClick = { onZeigeTabelle(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) { Text("Tabelle") }
    }
}

@Composable
private fun CheckboxZeile(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clickable { onCheckedChange(!checked) },
        )
    }
}

/** Teilnehmerliste: eine Card je Wettbewerb, darin je Verein mit Detailzeilen. */
@Composable
private fun TeilnehmerGruppe(
    gruppe: InternationaleTeilnehmerGruppe,
    onTeamClick: (Long) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Text(
                gruppe.bezeichnung,
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider()
            gruppe.teams.forEach { team ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (team.teamId != null) {
                                Modifier.clickable { onTeamClick(team.teamId) }
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        team.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (team.teamId != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    team.zusatz.forEach { zeile ->
                        Text(
                            zeile,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
private fun BegegnungCard(
    spiel: InternationaleSpiel,
    ergebnisseSichtbar: Boolean,
    onSpielbericht: (String?, String?) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MannschaftText(spiel.heim, spiel.heimTeamId, Modifier.weight(1f), TextAlign.Start, onTeamClick)
                val resultat = spiel.ergebnis?.takeIf { ergebnisseSichtbar }
                Text(
                    resultat ?: "vs.",
                    Modifier.padding(horizontal = 8.dp),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (resultat != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                MannschaftText(spiel.gast, spiel.gastTeamId, Modifier.weight(1f), TextAlign.End, onTeamClick)
            }
            if (spiel.berichtUrl != null) {
                TextButton(
                    onClick = { onSpielbericht(null, spiel.berichtUrl) },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Spielbericht öffnen")
                }
            }
        }
    }
}

/** Gemeinsame Karte der beiden Qualifikationsspiele einer Paarung. */
@Composable
private fun QualifikationsPaarungsKarte(
    paarung: InternationalePaarung,
    ergebnisseSichtbar: Boolean,
    onSpielbericht: (String?, String?) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val qualifiziertTeamId = paarung.hinspiel.qualifiziertTeamId
                ?: paarung.rueckspiel?.qualifiziertTeamId
            val qualifiziert = qualifiziertTeamId?.let { teamId ->
                listOfNotNull(paarung.hinspiel, paarung.rueckspiel).firstNotNullOfOrNull { spiel ->
                    when (teamId) {
                        spiel.heimTeamId -> spiel.heim
                        spiel.gastTeamId -> spiel.gast
                        else -> null
                    }
                }
            }
            // Wie auf der Website bleibt der Sieger an die Ergebnisanzeige gekoppelt,
            // damit der Schalter keine Entscheidung vorwegnimmt.
            if (ergebnisseSichtbar && qualifiziert != null) {
                Text(
                    "✓ Weiter: $qualifiziert",
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            } else if (ergebnisseSichtbar &&
                (paarung.rueckspiel == null || paarung.rueckspiel.vorschauUrl != null)
            ) {
                Text(
                    "Entscheidung offen",
                    Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            QualifikationsSpielZeile(
                titel = "Hinspiel",
                spiel = paarung.hinspiel,
                ergebnisseSichtbar = ergebnisseSichtbar,
                onSpielbericht = onSpielbericht,
                onTeamClick = onTeamClick,
            )
            HorizontalDivider()
            QualifikationsSpielZeile(
                titel = "Rückspiel",
                spiel = paarung.rueckspiel,
                ergebnisseSichtbar = ergebnisseSichtbar,
                onSpielbericht = onSpielbericht,
                onTeamClick = onTeamClick,
            )
        }
    }
}

@Composable
private fun QualifikationsSpielZeile(
    titel: String,
    spiel: InternationaleSpiel?,
    ergebnisseSichtbar: Boolean,
    onSpielbericht: (String?, String?) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        if (spiel == null) {
            Text("Noch nicht angesetzt", style = MaterialTheme.typography.bodySmall)
            return
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MannschaftText(spiel.heim, spiel.heimTeamId, Modifier.weight(1f), TextAlign.Start, onTeamClick)
            val resultat = spiel.ergebnis?.takeIf { ergebnisseSichtbar }
                ?: if (spiel.vorschauUrl != null) "offen" else "vs."
            Text(
                resultat,
                Modifier.padding(horizontal = 8.dp),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (spiel.ergebnis != null && ergebnisseSichtbar) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            MannschaftText(spiel.gast, spiel.gastTeamId, Modifier.weight(1f), TextAlign.End, onTeamClick)
        }
        spiel.berichtUrl?.let { url ->
            TextButton(
                onClick = { onSpielbericht(null, url) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Spielbericht öffnen")
            }
        }
    }
}

@Composable
private fun MannschaftText(
    name: String,
    teamId: Long?,
    modifier: Modifier,
    textAlign: TextAlign,
    onTeamClick: (Long) -> Unit,
) {
    val klickbar = teamId != null
    val textColor = if (klickbar) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    if (textAlign == TextAlign.Start) {
        Row(
            modifier = modifier.then(if (klickbar) Modifier.clickable { onTeamClick(teamId!!) } else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TeamWappen(teamId = teamId, verein = name, size = 18.dp)
            Text(
                name,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                textAlign = textAlign,
                color = textColor,
            )
        }
    } else {
        Row(
            modifier = modifier.then(if (klickbar) Modifier.clickable { onTeamClick(teamId!!) } else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        ) {
            Text(
                name,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                textAlign = textAlign,
                color = textColor,
            )
            TeamWappen(teamId = teamId, verein = name, size = 18.dp)
        }
    }
}

@Composable
private fun FilterLeiste(
    ansicht: InternationaleAnsicht,
    onFilter: (InternationaleFilter) -> Unit,
    zeigeErgebnisFilter: Boolean = true,
    gewaehlterZat: String? = null,
    onZat: (String?) -> Unit = {},
    zeigeZatFilter: Boolean = true,
) {
    val filter = ansicht.filter
    val grundAuswahl = mutableListOf(
        FilterAuswahl("Saison", ansicht.saisonen, filter.saison) { value -> onFilter(filter.copy(saison = value)) },
    )
    // Die Gruppenphase gliedert die Website nach Spieltag; die Auswahl filtert
    // die bereits geladenen Paarungen, ein erneutes Laden ist nicht nötig. Sie
    // gehört neben den Saisonfilter, damit beide Zeilen der Leiste belegt sind.
    if (zeigeZatFilter && ansicht.zatOptionen.size >= 2) {
        grundAuswahl += FilterAuswahl(
            label = "Alle Spieltage",
            optionen = listOf(InternationaleOption(ALLE_ZAT_WERT, "Alle Spieltage")) + ansicht.zatOptionen,
            selected = gewaehlterZat ?: ALLE_ZAT_WERT,
            onSelect = { value -> onZat(value.takeIf { it != ALLE_ZAT_WERT }) },
        )
    }
    grundAuswahl += listOf(
        FilterAuswahl("Runde", ansicht.runden, filter.runde) { value -> onFilter(filter.copy(runde = value)) },
        FilterAuswahl("Gruppe", ansicht.gruppen, filter.gruppe) { value -> onFilter(filter.copy(gruppe = value)) },
    )
    if (zeigeErgebnisFilter) {
        grundAuswahl += FilterAuswahl("Ergebnisse", ansicht.ergebnisOptionen, filter.ergebnisse) { value ->
            onFilter(filter.copy(ergebnisse = value))
        }
    }
    val auswahl = grundAuswahl.filter { it.optionen.size >= 2 }
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

/** Ersatzwert des Spieltagfilters für „alle Spieltage"; ZAT-Werte sind nie leer. */
private const val ALLE_ZAT_WERT = ""

private data class FilterAuswahl(
    val label: String,
    val optionen: List<InternationaleOption>,
    val selected: String?,
    val onSelect: (String?) -> Unit,
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
        DropdownMenu(expanded = offen, onDismissRequest = { offen = false }, modifier = Modifier.heightIn(max = 200.dp)) {
            optionen.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = { offen = false; if (option.wert != selected) onSelect(option.wert) },
                )
            }
        }
    }
}

/**
 * Tabellenansicht mit der Spaltenvorlage der Ligatabelle: Gruppenphasen zeigen
 * `#`, Team, Sp., Tore, Diff. und Pkt.; Ranglisten übernehmen ihre Saisons. Die
 * Ranglisten bleiben ab Club horizontal scrollbar, während Platz, Land und Club
 * fixiert bleiben. Ein Tipp auf einen Club öffnet den Kader wie in der Ligatabelle.
 * Die Zeilen sind nach der Bedeutung der Platzierung eingefärbt (Website: `top`,
 * `middle`, `bottom`), die Legende steht darunter.
 */
@Composable
private fun Tabelle(
    tabelle: WertTabelle,
    onTeamClick: (Long) -> Unit,
) {
    val spalten = rankingTabellenSpalten(tabelle.header)
        ?: kompakteTabellenSpalten(tabelle.header)
        ?: ranglistenTabellenSpalten(tabelle.header)
    if (spalten == null || tabelle.zeilen.isEmpty()) return

    val zeilenKlassen = tabelle.zeilenKlasse
    val zeilenFarben = List(zeilenKlassen.size) {
        internationaleTabellenZeilenHintergrund(zeilenKlassen[it])
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 4.dp)) {
            TabellenRaster(
                spalten = spalten,
                zeilen = tabelle.zeilen,
                teamIds = tabelle.teamIds,
                onTeamClick = onTeamClick,
                zeilenHintergrund = { index -> zeilenFarben.getOrNull(index) ?: Color.Transparent },
            )
        }
    }
}

/** Club-Ranking mit lokaler Suche, damit ein einzelner Verein in der langen Liste auffindbar ist. */
@Composable
private fun ClubRankingTabelle(
    tabelle: WertTabelle,
    suchtext: String,
    onSuchtext: (String) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    val gefiltert = remember(tabelle, suchtext) { clubRankingGefiltert(tabelle, suchtext) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = suchtext,
            onValueChange = onSuchtext,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Verein suchen") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (suchtext.isNotEmpty()) {
                    IconButton(onClick = { onSuchtext("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Suche leeren")
                    }
                }
            },
        )
        if (suchtext.isNotBlank()) {
            if (gefiltert.zeilen.isEmpty()) {
                Text(
                    "Kein Verein gefunden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "${gefiltert.zeilen.size} von ${tabelle.zeilen.size} Vereinen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (gefiltert.zeilen.isNotEmpty()) {
            Tabelle(tabelle = gefiltert, onTeamClick = onTeamClick)
        }
    }
}

/**
 * Tabellen mit fixierter Spaltenüberschrift: die Kopfzeile bleibt stehen, darunter
 * rollen die Datenzeilen. Alle Tabellen einer Ansicht teilen sich dieselben Spalten
 * (Gruppenphase, Rangliste), deshalb genügt eine Kopfzeile.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TabelleMitFixerKopfzeile(
    tabellen: List<WertTabelle>,
    onTeamClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spalten = tabellen
        .firstNotNullOfOrNull {
            rankingTabellenSpalten(it.header)
                ?: kompakteTabellenSpalten(it.header)
                ?: ranglistenTabellenSpalten(it.header)
        }
        ?: return
    val zeilenFarben = tabellen.map { tabelle ->
        List(tabelle.zeilenKlasse.size) { internationaleTabellenZeilenHintergrund(tabelle.zeilenKlasse[it]) }
    }

    Card(modifier.fillMaxWidth()) {
        val horizontalScroll = rememberScrollState()
        LazyColumn(Modifier.fillMaxSize()) {
            stickyHeader {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .zIndex(2f)
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    TabellenKopfzeile(
                        spalten,
                        horizontalScroll = horizontalScroll.takeIf { spalten.any { it.fest } },
                    )
                }
            }
            item {
                Column(Modifier.padding(bottom = 4.dp)) {
                    tabellen.forEachIndexed { tabelleIndex, tabelle ->
                        TabellenZeilen(
                            spalten = spalten,
                            zeilen = tabelle.zeilen,
                            teamIds = tabelle.teamIds,
                            onTeamClick = onTeamClick,
                            zeilenHintergrund = { index ->
                                zeilenFarben[tabelleIndex].getOrNull(index) ?: Color.Transparent
                            },
                            horizontalScroll = horizontalScroll.takeIf { spalten.any { it.fest } },
                        )
                    }
                }
            }
        }
    }
}

/** Legende der Platzbedeutungen unter den internationalen Tabellen (wie auf der Website). */
@Composable
private fun InternationaleTabellenLegende() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        InternationaleTabelleKlasse.entries.forEach { klasse ->
            Row(
                Modifier
                    .weight(1f)
                    .background(
                        internationaleTabellenZeilenHintergrund(klasse),
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    klasse.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                )
            }
        }
    }
}
