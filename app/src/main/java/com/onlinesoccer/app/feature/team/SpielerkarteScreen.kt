package com.onlinesoccer.app.feature.team

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.core.ui.theme.trikotFarbe
import com.onlinesoccer.app.data.model.SpielerKarte
import com.onlinesoccer.app.data.model.SpielerHistorienTabelle
import com.onlinesoccer.app.data.model.TeamTrainer
import com.onlinesoccer.app.data.model.TransferDetails

/** Spielerkarte (wie das Spielerprofil auf der Website, `sp.php`). */
@Composable
fun SpielerkarteScreen(
    onClose: () -> Unit,
    onTeamClick: (Long) -> Unit = {},
    viewModel: SpielerkarteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bietDialog by viewModel.bietDialog.collectAsStateWithLifecycle()
    val trainerNachricht by viewModel.trainerNachricht.collectAsStateWithLifecycle()
    val transferDetails by viewModel.transferDetails.collectAsStateWithLifecycle()
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
            Text(uiState.fehler ?: "", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = viewModel::lade) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text("Erneut versuchen")
            }
        }

        uiState.karte != null -> {
            val k = uiState.karte ?: return
            KartenAnsicht(
                karte = k,
                onClose = onClose,
                onTeamClick = onTeamClick,
                bietenMoeglich = uiState.bietenMoeglich,
                onBietenKlick = viewModel::onBietenKlick,
                onTrainerKlick = { trainerDialog = it },
                onTransferDetails = viewModel::ladeTransferdetails,
            )
        }
    }

    BietDialog(
        zustand = bietDialog,
        onBestatigen = viewModel::gebotAbgeben,
        onAbbrechen = viewModel::bietDialogSchliessen,
    )

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

    TransferDetailsDialog(
        zustand = transferDetails,
        onTeamClick = onTeamClick,
        onDismiss = viewModel::transferDetailsSchliessen,
    )
}

@Composable
private fun BietDialog(
    zustand: BietDialogState,
    onBestatigen: () -> Unit,
    onAbbrechen: () -> Unit,
) {
    when (zustand) {
        BietDialogState.Verborgen -> Unit

        BietDialogState.Laedt -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Gebot abgeben") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Lade Gebots-Informationen…")
                }
            },
            confirmButton = {},
        )

        is BietDialogState.Bereit -> {
            val info = zustand.info
            AlertDialog(
                onDismissRequest = onAbbrechen,
                title = { Text("Gebot für ${info?.name ?: "den Spieler"}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (info != null) {
                            val zeilen = listOfNotNull(
                                "Höchstgebot: ${info.hoechstgebot.ifBlank { "–" }}",
                                "Marktwert: ${info.marktwert.ifBlank { "–" }}",
                                "Gehalt: ${info.gehalt.ifBlank { "–" }}",
                                info.angeboteBis.takeIf { it.isNotBlank() }?.let { "Angebote bis: $it" },
                            )
                            zeilen.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Die Laufzeit des Vertrags beträgt 36 Monate, das Gehalt ist fest definiert.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Ein bestätigtes Gebot wird sofort an die Website gesendet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onBestatigen,
                        enabled = info?.submitName?.isNotBlank() == true,
                    ) {
                        Text("Gebot verbindlich abgeben")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onAbbrechen) { Text("Abbrechen") }
                },
            )
        }

        is BietDialogState.Ergebnis -> AlertDialog(
            onDismissRequest = onAbbrechen,
            title = { Text(if (zustand.erfolg) "Gebot gesendet" else "Nicht möglich") },
            text = { Text(zustand.meldung.ifBlank { if (zustand.erfolg) "Gebot wurde gesendet." else "Unbekannter Fehler." }) },
            confirmButton = {
                TextButton(onClick = onAbbrechen) { Text("OK") }
            },
        )
    }
}

@Composable
private fun KartenAnsicht(
    karte: SpielerKarte,
    onClose: () -> Unit,
    onTeamClick: (Long) -> Unit,
    bietenMoeglich: Boolean,
    onBietenKlick: () -> Unit,
    onTrainerKlick: (TeamTrainer.Besetzt) -> Unit,
    onTransferDetails: (Long) -> Unit,
) {
    var bereich by remember { mutableStateOf(SpielerkarteBereich.DATEN) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Spielerkarte",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    TrainerHeaderZeile(karte.trainer, onKlick = onTrainerKlick)
                }
                if (bietenMoeglich) {
                    FilledTonalButton(onClick = onBietenKlick) {
                        Text("Bieten")
                    }
                }
            }
        }

        item { KopfKarte(karte) }
        item { SpielerkarteMenues(bereich) { bereich = it } }
        when (bereich) {
            SpielerkarteBereich.DATEN -> {
                item { ZustandKarte(karte) }
                if (karte.sonderFaehigkeiten.isNotEmpty()) {
                    item { Text("Sonderfähigkeiten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    item { SonderfaehigkeitenKarte(karte.sonderFaehigkeiten) }
                }
                if (karte.staerken.isNotEmpty()) {
                    item { Text("Stärken", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    item { StaerkenKarte(karte) }
                }
                if (karte.vertrag != null) {
                    item { Text("Vertrag", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    item { VertragKarte(karte, onTeamClick) }
                }
                item { Text("Statistik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                item { StatistikKarte(karte) }
            }
            SpielerkarteBereich.TRANSFER -> item {
                HistorienKarte(
                    titel = "Transferhistorie",
                    tabelle = karte.transferhistorie,
                    onDetails = onTransferDetails,
                    onTeamClick = onTeamClick,
                )
            }
            SpielerkarteBereich.LEIHE -> item {
                HistorienKarte("Leihhistorie", karte.leihhistorie)
            }
            SpielerkarteBereich.HISTORIE -> item {
                HistorienKarte("Spielerhistorie", karte.spielerhistorie)
            }
        }
    }
}

@Composable
private fun TransferDetailsDialog(
    zustand: TransferDetailsState,
    onTeamClick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!zustand.ladend && zustand.daten == null && zustand.fehler == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transferdetails") },
        text = {
            when {
                zustand.ladend -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Lade Transferdetails …")
                }
                zustand.fehler != null -> Text(zustand.fehler ?: "Unbekannter Fehler", color = MaterialTheme.colorScheme.error)
                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    zustand.daten?.teams.orEmpty().forEach { team ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                team.name.ifBlank { "Verein" },
                                color = if (team.teamId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                modifier = team.teamId?.let { Modifier.clickable { onTeamClick(it) } } ?: Modifier,
                            )
                            team.positionen.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
    )
}

private enum class SpielerkarteBereich(val titel: String) {
    DATEN("Spielerdaten"),
    TRANSFER("Transferhistorie"),
    LEIHE("Leihhistorie"),
    HISTORIE("Spielerhistorie"),
}

@Composable
private fun SpielerkarteMenues(
    ausgewaehlt: SpielerkarteBereich,
    onAuswahl: (SpielerkarteBereich) -> Unit,
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SpielerkarteBereich.entries.forEach { bereich ->
            FilterChip(
                selected = bereich == ausgewaehlt,
                onClick = { onAuswahl(bereich) },
                label = { Text(bereich.titel) },
            )
        }
    }
}

@Composable
private fun KopfKarte(karte: SpielerKarte) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(trikotFarbe(karte.position), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(karte.nummer.ifBlank { "–" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(karte.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${posName(karte.position)}${karte.alter?.let { " · $it Jahre" }.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ZustandKarte(karte: SpielerKarte) {
    val zustaende = buildList {
        add("Skill" to "${(karte.skill * 10).toInt() / 10.0}")
        add("Opti" to "${(karte.opti * 10).toInt() / 10.0}")
        if (karte.fit > 0) add("Fitness" to "${karte.fit}%")
        if (karte.mor > 0) add("Moral" to "${karte.mor}%")
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            zustaende.forEach { (label, wert) ->
                ZustandWert(label, wert)
            }
        }
    }
}

@Composable
private fun ZustandWert(label: String, wert: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(wert, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SonderfaehigkeitenKarte(sonderfaehigkeiten: List<com.onlinesoccer.app.data.model.SonderFaehigkeit>) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sonderfaehigkeiten.forEach { sf ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(sf.kuerzel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sf.bezeichnung,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    sf.positionsHinweis?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        sf.attribute.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun StaerkenKarte(karte: SpielerKarte) {
    val staerken = buildList {
        if (karte.staerken.keys.none { it.equals("Skillschnitt", ignoreCase = true) }) {
            add("Skillschnitt" to staerkenWert(karte.skill))
        }
        if (karte.staerken.keys.none { it.equals("Opt. Skill", ignoreCase = true) }) {
            add("Opt. Skill" to staerkenWert(karte.opti))
        }
        addAll(karte.staerken.toList())
    }.sortedWith(
        compareBy<Pair<String, String>> {
            when {
                it.first.equals("Skillschnitt", ignoreCase = true) -> 0
                it.first.equals("Opt. Skill", ignoreCase = true) -> 1
                else -> 2
            }
        },
    )
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            staerken.forEach { (label, wert) ->
                val zahl = wert.replace(',', '.').toFloatOrNull()?.coerceIn(0f, 100f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(einzelwertTitel(label), style = MaterialTheme.typography.bodyMedium)
                        Text(wert, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    if (zahl != null) {
                        LinearProgressIndicator(
                            progress = { zahl / 100f },
                            modifier = Modifier.fillMaxWidth().height(7.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun staerkenWert(wert: Double): String =
    ((wert * 10).toInt() / 10.0).toString()

private fun einzelwertTitel(label: String): String {
    val original = label.trim()
    val kuerzel = einzelwertKuerzel[original]
        ?: einzelwertVollnamen[original.uppercase()]?.let { original.uppercase() }
    val vollname = einzelwertVollnamen[original.uppercase()] ?: original
    return if (kuerzel != null) "$vollname ($kuerzel)" else original
}

private val einzelwertKuerzel = mapOf(
    "Abstoss" to "ABS",
    "Abstoß" to "ABS",
    "Stellungsspiel" to "STS",
    "Fangsicherheit" to "FAN",
    "Strafraumbeh." to "STB",
    "Strafraumbeherrschung" to "STB",
    "Spiel auf der Linie" to "SPL",
    "Reflexe" to "REF",
    "Schießen" to "SCH",
    "Balleroberung" to "BAK",
    "Körperkontakt" to "KOB",
    "Zweikampf" to "ZWK",
    "Defensive" to "DEC",
    "Geschwindigkeit" to "GES",
    "Führungsqualität" to "FUQ",
    "Führungsfertigkeit" to "FUQ",
    "Erfahrung" to "ERF",
    "Aggressivität" to "AGG",
    "Passen" to "PAS",
    "Passgenauigkeit" to "PAS",
    "Ausdauer" to "AUS",
    "Übersicht" to "UEB",
    "Widerstandskraft" to "WID",
    "Willenskraft" to "WID",
    "Selbstbewusstsein" to "SEL",
    "Selbstvertrauen" to "SEL",
    "Disziplin" to "DIS",
    "Zuverlässigkeit" to "ZUV",
    "Einstellung" to "EIN",
    "Einschätzung" to "EIN",
)

private val einzelwertVollnamen = mapOf(
    "ABS" to "Abstoß",
    "STS" to "Stellungsspiel",
    "FAN" to "Fangsicherheit",
    "STB" to "Strafraumbeherrschung",
    "SPL" to "Spiel auf der Linie",
    "REF" to "Reflexe",
    "SCH" to "Schießen",
    "BAK" to "Balleroberung",
    "KOB" to "Körperkontakt",
    "ZWK" to "Zweikampf",
    "DEC" to "Defensive",
    "GES" to "Geschwindigkeit",
    "FUQ" to "Führungsqualität",
    "ERF" to "Erfahrung",
    "AGG" to "Aggressivität",
    "PAS" to "Passgenauigkeit",
    "AUS" to "Ausdauer",
    "UEB" to "Übersicht",
    "WID" to "Widerstandskraft",
    "SEL" to "Selbstbewusstsein",
    "DIS" to "Disziplin",
    "ZUV" to "Zuverlässigkeit",
    "EIN" to "Einstellung",
)

@Composable
private fun VertragKarte(karte: SpielerKarte, onTeamClick: (Long) -> Unit) {
    val v = karte.vertrag ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            VertragsZeile("Gehalt", v.gehalt ?: "–")
            VertragsZeile("Vertragslaufzeit", v.laufzeit ?: "–")
            VertragsZeile("Marktwert", v.marktwert ?: "–")
            VertragsZeile("Geburtstag", v.geburtstag ?: "–")
            karte.teamName?.let { teamName ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .then(karte.teamId?.let { Modifier.clickable { onTeamClick(it) } } ?: Modifier),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Aktuelles Team", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(teamName, color = if (karte.teamId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
            karte.teamLiga?.let { VertragsZeile("Liga", it) }
            karte.teamLand?.let { VertragsZeile("Land", it) }
        }
    }
}

@Composable
private fun VertragsZeile(label: String, wert: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(wert, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatistikKarte(karte: SpielerKarte) {
    var gesamt by remember { mutableStateOf(false) }
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        listOf(false to "Saison", true to "Karriere").forEachIndexed { index, (istGesamt, label) ->
            SegmentedButton(
                selected = gesamt == istGesamt,
                onClick = { gesamt = istGesamt },
                shape = SegmentedButtonDefaults.itemShape(index, 2),
            ) {
                Text(label)
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    StatistikTabelle(if (gesamt) karte.statistikGesamt else karte.statistikSaison)
}

private val statistikZeilen = listOf("Spiele", "Tore", "Vorlagen", "Gelbe Karten", "Rote Karten")
private val statistikSpalten = listOf("LI", "LP", "IP", "FS")

@Composable
private fun StatistikTabelle(daten: Map<String, String>) {
    val zeilen = statistikZeilen.map { metric ->
        listOf(metric) + statistikSpalten.map { spalte ->
            daten["$metric ($spalte)"] ?: daten["$metric/$spalte"] ?: "–"
        }
    }
    TabellenKarte(
        kopf = listOf("Statistik") + statistikSpalten,
        zeilen = if (daten.isEmpty()) emptyList() else zeilen,
    )
}

@Composable
private fun HistorienKarte(
    titel: String,
    tabelle: SpielerHistorienTabelle,
    onDetails: ((Long) -> Unit)? = null,
    onTeamClick: (Long) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(titel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (tabelle.kopf.isEmpty() || tabelle.zeilen.isEmpty()) {
            Text("Keine Einträge vorhanden.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            TabellenKarte(tabelle.kopf, tabelle.zeilen, tabelle.detailIds, tabelle.linkIds, onDetails, onTeamClick)
        }
    }
}

@Composable
private fun TabellenKarte(
    kopf: List<String>,
    zeilen: List<List<String>>,
    detailIds: List<Long?> = emptyList(),
    linkIds: List<List<Long?>> = emptyList(),
    onDetails: ((Long) -> Unit)? = null,
    onTeamClick: (Long) -> Unit = {},
) {
    val horizontal = rememberScrollState()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.horizontalScroll(horizontal)) {
            TabellenZeile(kopf, kopf = true)
            if (zeilen.isEmpty()) {
                Text("Keine Statistik vorhanden.", Modifier.padding(14.dp))
            } else {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    zeilen.forEachIndexed { index, zeile ->
                        TabellenZeile(
                            werte = zeile,
                            kopf = false,
                            detailSpalte = detailIds.getOrNull(index) != null,
                            onDetails = { detailIds.getOrNull(index)?.let { onDetails?.invoke(it) } },
                            linkIds = linkIds.getOrNull(index).orEmpty(),
                            onTeamClick = onTeamClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabellenZeile(
    werte: List<String>,
    kopf: Boolean,
    detailSpalte: Boolean = false,
    onDetails: () -> Unit = {},
    linkIds: List<Long?> = emptyList(),
    onTeamClick: (Long) -> Unit = {},
) {
    Row(Modifier.background(if (kopf) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)) {
        werte.forEachIndexed { index, wert ->
            Text(
                text = wert.ifBlank { "–" },
                modifier = Modifier
                    .width(if (index == 0) 142.dp else 92.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .then(
                        when {
                            detailSpalte && wert.equals("Details", ignoreCase = true) -> Modifier.clickable(onClick = onDetails)
                            linkIds.getOrNull(index) != null -> Modifier.clickable {
                                linkIds.getOrNull(index)?.let(onTeamClick)
                            }
                            else -> Modifier
                        },
                    ),
                style = if (kopf) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = if (kopf || index == 0 || (detailSpalte && wert.equals("Details", true))) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                color = if (detailSpalte && wert.equals("Details", true)) {
                    MaterialTheme.colorScheme.primary
                } else if (linkIds.getOrNull(index) != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}
