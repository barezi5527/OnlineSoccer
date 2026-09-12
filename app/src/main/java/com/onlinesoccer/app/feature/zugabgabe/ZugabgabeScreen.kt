package com.onlinesoccer.app.feature.zugabgabe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.Aufstellung
import com.onlinesoccer.app.data.model.AufstellungSlot
import com.onlinesoccer.app.data.model.AufstellungSpieler
import com.onlinesoccer.app.data.model.AufstellungsWerte
import com.onlinesoccer.app.data.model.AuswahlOption
import com.onlinesoccer.app.data.model.ERSATZBANK_BUCHSTABEN
import com.onlinesoccer.app.data.model.ERSATZTORWART_BANK_INDEX
import com.onlinesoccer.app.data.model.SpielerPosition
import com.onlinesoccer.app.feature.taktik.TaktikEditor
import com.onlinesoccer.app.feature.taktik.TaktikViewModel

private enum class ZugabgabeModus { AUFSTELLUNG, TAKTIK }

// Die Beta-Zugabgabe verwendet dieses feste Online-Soccer-Raster.
private const val FELD_ZEILEN = 15
private const val FELD_SPALTEN = 11

private val spielerPositionFilter = listOf(
    SpielerPosition.TOR,
    SpielerPosition.ABW,
    SpielerPosition.DMI,
    SpielerPosition.MIT,
    SpielerPosition.OMI,
    SpielerPosition.STU,
)

private data class RasterZelle(
    val zeile: Int,
    val spalte: Int,
    val torwart: Boolean = false,
    val bankIndex: Int? = null,
) {
    val label: String
        get() = if (bankIndex != null) ERSATZBANK_BUCHSTABEN[bankIndex].toString()
        else "${('A' + FELD_ZEILEN - 1 - zeile)}${spalte + 1}"
}

@Composable
fun ZugabgabeScreen(
    viewModel: ZugabgabeViewModel = hiltViewModel(),
    taktikViewModel: TaktikViewModel = hiltViewModel(),
    showModusToggle: Boolean = true,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val taktikUiState by taktikViewModel.uiState.collectAsStateWithLifecycle()
    var modus by remember { mutableStateOf(ZugabgabeModus.AUFSTELLUNG) }

    when {
        uiState.ladende -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        uiState.aufstellung == null -> Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Zugabgabe", style = MaterialTheme.typography.headlineMedium)
            uiState.message?.let {
                Text(
                    it.split("\\s+".toRegex()).joinToString(" "),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp),
                )
            }
            uiState.fehler?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::ladeAufstellung) { Text("Erneut versuchen") }
            }
        }

        else -> Column(Modifier.fillMaxSize()) {
            if (showModusToggle) {
                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    ZugabgabeModus.entries.forEachIndexed { index, m ->
                        SegmentedButton(
                            selected = modus == m,
                            onClick = { modus = m },
                            shape = SegmentedButtonDefaults.itemShape(index, ZugabgabeModus.entries.size),
                        ) {
                            Text(if (m == ZugabgabeModus.AUFSTELLUNG) "Aufstellung" else "Taktik")
                        }
                    }
                }
            }
            when (modus) {
                ZugabgabeModus.AUFSTELLUNG -> AufstellungAnsicht(
                    aufstellung = uiState.aufstellung!!,
                    taktikLadend = uiState.taktikLadend,
                    zatLadend = uiState.zatLadend,
                    kaderSpeichernd = uiState.kaderSpeichernd,
                    message = uiState.message,
                    onTaktik = viewModel::wendeTaktikAn,
                    onZat = viewModel::wendeZatAn,
                    onSetzeKaderSlot = viewModel::setzeKaderSlot,
                    onSpeichereKader = viewModel::speichereKader,
                    onLoescheKader = viewModel::loescheKader,
                    onSetzeFeld = viewModel::setzeAufFeld,
                    onSetzeTorwart = viewModel::setzeAufTorwart,
                    onSetzeAufBank = viewModel::setzeAufBank,
                    onAktualisieren = viewModel::ladeAufstellung,
                )
                ZugabgabeModus.TAKTIK -> TaktikEditor(
                    uiState = taktikUiState,
                    onToggle = taktikViewModel::toggleCode,
                    onNameChange = taktikViewModel::setName,
                    onSpeichern = taktikViewModel::speichere,
                    onLeeren = taktikViewModel::leereRaster,
                    onWaehleStandard = taktikViewModel::waehleStandard,
                    onWaehleEigene = taktikViewModel::waehleEigene,
                    onLaden = taktikViewModel::ladeGewaehlte,
                    onLoeschen = taktikViewModel::loescheGewaehlte,
                )
            }
        }
    }
}

@Composable
private fun AufstellungAnsicht(
    aufstellung: Aufstellung,
    taktikLadend: Boolean,
    zatLadend: Boolean,
    kaderSpeichernd: Boolean,
    message: String?,
    onTaktik: (String) -> Unit,
    onZat: (String) -> Unit,
    onSetzeKaderSlot: (Long, String) -> Unit,
    onSpeichereKader: () -> Unit,
    onLoescheKader: () -> Unit,
    onSetzeFeld: (Long, Int, Int) -> Unit,
    onSetzeTorwart: (Long) -> Unit,
    onSetzeAufBank: (Long, Int) -> Unit,
    onAktualisieren: () -> Unit,
) {
    var taktikBestaetigt by remember { mutableStateOf<String?>(null) }
    var zatBestaetigt by remember { mutableStateOf<String?>(null) }
    var kaderBestaetigt by remember { mutableStateOf(false) }
    var kaderLoeschenBestaetigt by remember { mutableStateOf(false) }
    var aktiveZelle by remember { mutableStateOf<RasterZelle?>(null) }
    var spielerAuswahl by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        TaktikAuswahl(
            taktiken = aufstellung.taktiken,
            ladend = taktikLadend,
            onAnwenden = { taktikBestaetigt = it },
        )

        Spacer(Modifier.height(8.dp))

        ZatAuswahl(
            zats = aufstellung.zatOptionen,
            ladend = zatLadend,
            onAnwenden = { zatBestaetigt = it },
        )

        Spacer(Modifier.height(8.dp))

         FeldView(
             aufstellung = aufstellung,
             onZelleTippen = { zeile, spalte ->
                 aktiveZelle = RasterZelle(zeile, spalte)
                 spielerAuswahl = false
             },
             onTorwartTippen = {
                 aktiveZelle = RasterZelle(FELD_ZEILEN - 1, FELD_SPALTEN / 2, torwart = true)
                 spielerAuswahl = false
             },
         )

        Spacer(Modifier.height(12.dp))

        BankView(
            aufstellung = aufstellung,
            onZelleTippen = { index ->
                aktiveZelle = RasterZelle(0, 0, bankIndex = index)
                spielerAuswahl = false
            },
        )

        Spacer(Modifier.height(12.dp))

        ZatKontrolle(aufstellung)

        Spacer(Modifier.height(12.dp))

        AufstellungsWerteAnsicht(aufstellung.aufstellungsWerte)

        Spacer(Modifier.height(12.dp))

        KaderZuordnung(
            aufstellung = aufstellung,
            speichernd = kaderSpeichernd,
            onSetzeSlot = onSetzeKaderSlot,
            onSpeichern = { kaderBestaetigt = true },
            onLoeschen = { kaderLoeschenBestaetigt = true },
        )

        Spacer(Modifier.height(12.dp))
        Text(
            "Position antippen, um im Auswahlmenü einen Spieler für sie zu wählen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = if (it.startsWith("✓")) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
            )
        }

        TextButton(onClick = onAktualisieren, modifier = Modifier.fillMaxWidth()) {
            Text("Aktuelle ZAT-Daten aktualisieren")
        }
    }

    aktiveZelle?.let { zelle ->
        val belegt = aufstellung.spieler.firstOrNull {
            when {
                zelle.torwart -> it.slot == AufstellungSlot.Torwart
                zelle.bankIndex != null -> it.slot == AufstellungSlot.Ersatz(zelle.bankIndex)
                else -> it.slot == AufstellungSlot.Feld(zelle.zeile, zelle.spalte)
            }
        }
        SpielerZellSheet(
            zelle = zelle,
            belegt = belegt,
            spieler = aufstellung.spieler,
            auswahl = spielerAuswahl,
            onWechseln = { spielerAuswahl = true },
            onAuswahl = { pid ->
                when {
                    zelle.torwart -> onSetzeTorwart(pid)
                    zelle.bankIndex != null -> onSetzeAufBank(pid, zelle.bankIndex)
                    else -> onSetzeFeld(pid, zelle.zeile, zelle.spalte)
                }
                aktiveZelle = null
            },
            onDismiss = { aktiveZelle = null },
        )
    }

    taktikBestaetigt?.let { taktikId ->
        val taktik = aufstellung.taktiken.firstOrNull { it.id == taktikId }
        AlertDialog(
            onDismissRequest = { taktikBestaetigt = null },
            title = { Text("Taktik laden") },
            text = {
                Text(
                    "Die Formation \u201E${taktik?.label ?: taktikId}\u201C wird auf dem Server als Zugabgabe-Vorlage geladen " +
                        "und ersetzt die aktuelle Aufstellung. Fortfahren?",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        taktikBestaetigt = null
                        onTaktik(taktikId)
                    },
                ) { Text("Ja, laden") }
            },
            dismissButton = {
                TextButton(onClick = { taktikBestaetigt = null }) { Text("Abbrechen") }
            },
        )
    }

    zatBestaetigt?.let { zatId ->
        val zat = aufstellung.zatOptionen.firstOrNull { it.id == zatId }
        AlertDialog(
            onDismissRequest = { zatBestaetigt = null },
            title = { Text("Laden aus ZAT") },
            text = {
                Text(
                    "Alle Einstellungen und die Aufstellung des ZAT \u201E${zat?.label ?: zatId}\u201C werden übernommen " +
                        "und ersetzen die aktuelle Zugabgabe. Fortfahren?",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        zatBestaetigt = null
                        onZat(zatId)
                    },
                ) { Text("Ja, laden") }
            },
            dismissButton = {
                TextButton(onClick = { zatBestaetigt = null }) { Text("Abbrechen") }
            },
        )
    }

    if (kaderBestaetigt) {
        AlertDialog(
            onDismissRequest = { kaderBestaetigt = false },
            title = { Text("Aufstellung speichern") },
            text = {
                Text("Die zugewiesenen Raster-Positionen der Kaderspieler werden als Aufstellung gespeichert und ersetzen die bisherige Aufstellung. Fortfahren?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        kaderBestaetigt = false
                        onSpeichereKader()
                    },
                ) { Text("Ja, speichern") }
            },
            dismissButton = {
                TextButton(onClick = { kaderBestaetigt = false }) { Text("Abbrechen") }
            },
        )
    }

    if (kaderLoeschenBestaetigt) {
        AlertDialog(
            onDismissRequest = { kaderLoeschenBestaetigt = false },
            title = { Text("Aufstellung löschen") },
            text = { Text("Die gespeicherte Kader-Aufstellung wird auf der Website gelöscht. Fortfahren?") },
            confirmButton = {
                Button(
                    onClick = {
                        kaderLoeschenBestaetigt = false
                        onLoescheKader()
                    },
                ) { Text("Ja, löschen") }
            },
            dismissButton = {
                TextButton(onClick = { kaderLoeschenBestaetigt = false }) { Text("Abbrechen") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaktikAuswahl(
    taktiken: List<AuswahlOption>,
    ladend: Boolean,
    onAnwenden: (String) -> Unit,
) {
    var offen by remember { mutableStateOf(false) }
    var gewaehlteId by remember { mutableStateOf<String?>(null) }
    val gewaehlt = taktiken.firstOrNull { it.id == gewaehlteId }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExposedDropdownMenuBox(
            expanded = offen,
            onExpandedChange = { offen = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = gewaehlt?.label ?: "Taktik auswählen …",
                onValueChange = {},
                readOnly = true,
                label = { Text("Formation / Taktik") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = offen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
                taktiken.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.label) },
                        onClick = {
                            offen = false
                            gewaehlteId = opt.id
                        },
                    )
                }
            }
        }
        Button(
            onClick = { gewaehlteId?.let(onAnwenden) },
            enabled = gewaehlteId != null && !ladend,
        ) {
            if (ladend) {
                CircularProgressIndicator(
                    Modifier.size(18.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Laden")
            }
        }
    }
    if (taktiken.isEmpty()) {
        Text(
            "Keine Taktikauswahl auf der Server-Seite gefunden.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZatAuswahl(
    zats: List<AuswahlOption>,
    ladend: Boolean,
    onAnwenden: (String) -> Unit,
) {
    var offen by remember { mutableStateOf(false) }
    var gewaehlteId by remember { mutableStateOf<String?>(null) }
    val gewaehlt = zats.firstOrNull { it.id == gewaehlteId }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExposedDropdownMenuBox(
            expanded = offen,
            onExpandedChange = { offen = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = gewaehlt?.label ?: "ZAT wählen …",
                onValueChange = {},
                readOnly = true,
                label = { Text("Laden aus ZAT") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = offen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
                zats.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.label) },
                        onClick = {
                            offen = false
                            gewaehlteId = opt.id
                        },
                    )
                }
            }
        }
        Button(
            onClick = { gewaehlteId?.let(onAnwenden) },
            enabled = gewaehlteId != null && !ladend,
        ) {
            if (ladend) {
                CircularProgressIndicator(
                    Modifier.size(18.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Laden")
            }
        }
    }
    if (zats.isEmpty()) {
        Text(
            "Keine weiteren ZATs zum Übernehmen gefunden.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KaderZuordnung(
    aufstellung: Aufstellung,
    speichernd: Boolean,
    onSetzeSlot: (Long, String) -> Unit,
    onSpeichern: () -> Unit,
    onLoeschen: () -> Unit,
) {
    if (aufstellung.kaderSlots.isEmpty() || aufstellung.spieler.isEmpty()) {
        return
    }
    Text("Mannschaftskader", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(6.dp))

    // player_0 ist der Website-Platzhalter für einen Amateur, kein Kaderspieler.
    val kaderSpieler = aufstellung.spieler.filter { it.pid != 0L }
    val slotOptionen = aufstellung.kaderSlots
    val belegtKader = kaderSpieler.count { it.raSlot != null }

    kaderSpieler.forEach { spieler ->
        var offen by remember(spieler.pid) { mutableStateOf(false) }
        val gewaehlteId = spieler.raSlot.orEmpty()
        val gewaehlt = slotOptionen.firstOrNull { it.id == gewaehlteId }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        append(spieler.name)
                        if (spieler.nummer.isNotBlank()) append("  (Nr. ${spieler.nummer})")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
                Text(
                    "${positionsName(spieler.position)}  •  Skill ${spieler.skill}/${spieler.opti}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Fitness ${spieler.fit}  •  Moral ${spieler.mor}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                spieler.slot?.let {
                    Text(
                        "Aktuell: ${aufstellung.slotText(spieler)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            ExposedDropdownMenuBox(
                expanded = offen,
                onExpandedChange = { offen = it },
            ) {
                OutlinedTextField(
                    value = gewaehlt?.let { kaderSlotLabel(it) } ?: "-",
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier
                        .width(56.dp)
                        .menuAnchor(),
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
                    DropdownMenuItem(
                        text = { Text("-") },
                        onClick = {
                            offen = false
                            onSetzeSlot(spieler.pid, "")
                        },
                    )
                    slotOptionen.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(kaderSlotLabel(opt)) },
                            onClick = {
                                offen = false
                                onSetzeSlot(spieler.pid, opt.id)
                            },
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Text(
        "Zugeordnet: $belegtKader von ${kaderSpieler.size}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onSpeichern,
        enabled = belegtKader > 0 && !speichernd,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (speichernd) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text("Aufstellung speichern (Kader)")
        }
    }
    TextButton(
        onClick = onLoeschen,
        enabled = !speichernd,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Aufstellung löschen (Kader)", color = MaterialTheme.colorScheme.error)
    }
}

private fun kaderSlotLabel(opt: AuswahlOption): String = opt.id.takeIf { it.isNotBlank() } ?: "-"

@Composable
private fun FeldView(
    aufstellung: Aufstellung,
    onZelleTippen: (Int, Int) -> Unit,
    onTorwartTippen: () -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(11f / 16f)
            .background(Color(0xFF1B5E20)),
    ) {
        val aktivePositionen = aufstellung.rasterPositionen.values
            .map { it.zeile to it.spalte }
            .toSet()
        val aktiveBuchstaben = aufstellung.rasterPositionen.entries.associate { (buchstabe, position) ->
            (position.zeile to position.spalte) to buchstabe
        }
        val belegtePositionen = aufstellung.feldspieler
            .mapNotNull { (it.slot as? AufstellungSlot.Feld)?.let { slot -> slot.zeile to slot.spalte } }
            .toSet()
        val w = maxWidth
        val h = maxHeight
        val textMeasurer = rememberTextMeasurer()
        // Rand für die Matrix-Beschriftung (außerhalb des Spielfelds)
        val randLinks = w * 0.10f
        val randOben = h * 0.08f

        Canvas(Modifier.fillMaxSize()) {
            val line = Color.White
            val r = size.width / 11f
            val feldLinks = randLinks.toPx()
            val feldOben = randOben.toPx()
            val feldW = size.width - feldLinks
            val feldH = size.height - feldOben

            // Spielfeld (etwas verkleinert)
            val gitter = Color.White.copy(alpha = 0.22f)
            for (i in 1..10) {
                drawLine(gitter, Offset(feldLinks + feldW * i / 11f, feldOben), Offset(feldLinks + feldW * i / 11f, feldOben + feldH), 1f)
            }
            for (j in 1..14) {
                drawLine(gitter, Offset(feldLinks, feldOben + feldH * j / 15f), Offset(feldLinks + feldW, feldOben + feldH * j / 15f), 1f)
            }

            drawRect(color = line, topLeft = Offset(feldLinks + r * 0.2f, feldOben + r * 0.2f), size = Size(feldW - r * 0.4f, feldH - r * 0.4f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
            drawLine(line, Offset(feldLinks, feldOben + feldH / 2), Offset(feldLinks + feldW, feldOben + feldH / 2), 2f)
            drawCircle(color = line, radius = r * 1.5f, center = Offset(feldLinks + feldW / 2, feldOben + feldH / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
            drawRect(color = line, topLeft = Offset(feldLinks + feldW * 0.2f, feldOben + r * 0.2f), size = Size(feldW * 0.6f, r * 1.8f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
            drawRect(color = line, topLeft = Offset(feldLinks + feldW * 0.2f, feldOben + feldH - r * 2f), size = Size(feldW * 0.6f, r * 1.8f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
            drawRect(color = line, topLeft = Offset(feldLinks + feldW * 0.4f, feldOben), size = Size(feldW * 0.2f, r * 0.7f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
            drawRect(color = line, topLeft = Offset(feldLinks + feldW * 0.4f, feldOben + feldH - r * 0.7f), size = Size(feldW * 0.2f, r * 0.7f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))

            // Matrix-Beschriftung außerhalb des Spielfelds, größer (Sturm oben rechts, Abwehr unten)
            val labelStyle = TextStyle(
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            for (spalte in 1..11) {
                val cx = feldLinks + feldW * (spalte - 0.5f) / 11f
                val layout = textMeasurer.measure(spalte.toString(), labelStyle)
                drawText(
                    layout,
                    topLeft = Offset(cx - layout.size.width / 2f, (feldOben - layout.size.height) / 2f),
                )
            }
            for (zeile in 1..15) {
                val cy = feldOben + feldH * (zeile - 0.5f) / 15f
                val layout = textMeasurer.measure(('A' + 15 - zeile).toChar().toString(), labelStyle)
                drawText(
                    layout,
                    topLeft = Offset((feldLinks - layout.size.width) / 2f, cy - layout.size.height / 2f),
                )
            }
        }

        // Das Raster bleibt das technische Online-Soccer-Raster. Die transparenten
        // Touch-Flächen liegen über dem Feld, die Trikotmarker darüber.
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = randLinks, top = randOben),
        ) {
            repeat(FELD_ZEILEN) { zeile ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    repeat(FELD_SPALTEN) { spalte ->
                        val position = zeile to spalte
                        val istFreieAktiveZelle = position in aktivePositionen && position !in belegtePositionen
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    if (istFreieAktiveZelle) Color(0xFFFFB74D).copy(alpha = 0.82f) else Color.Transparent,
                                    RoundedCornerShape(4.dp),
                                )
                                .clickable(enabled = istFreieAktiveZelle) { onZelleTippen(zeile, spalte) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (istFreieAktiveZelle) {
                                Text(
                                    aktiveBuchstaben[position].orEmpty(),
                                    color = Color(0xFF4E342E),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }

        val markerSize = w * 0.085f
        aufstellung.feldspieler.forEach { spieler ->
            val slot = spieler.slot as AufstellungSlot.Feld
            val x = randLinks + (w - randLinks) * ((slot.spalte + 0.5f) / 11f) - markerSize / 2f
            val y = randOben + (h - randOben) * ((slot.zeile + 0.5f) / 15f) - markerSize / 2f
                Trikot(
                    label = spieler.raSlot ?: aktiveBuchstaben[slot.zeile to slot.spalte] ?: spieler.nummer,
                farbe = trikotFarbe(spieler.position),
                ausgewaehlt = false,
                    modifier = Modifier
                        .offset { IntOffset(x.toPx().toInt(), y.toPx().toInt()) }
                        .size(markerSize)
                        .clickable { onZelleTippen(slot.zeile, slot.spalte) },
            )
        }

        // Torwart positioniert fix auf A6 (unterste Feldzeile, mittlere Spalte).
        // Ist kein Torwart aufgestellt, bleibt eine gestrichelte "T"-Fläche zum Nachbesetzen.
        val twX = randLinks + (w - randLinks) * ((5 + 0.5f) / 11f) - markerSize / 2f
        val twY = randOben + (h - randOben) * ((FELD_ZEILEN - 1 + 0.5f) / FELD_ZEILEN) - markerSize / 2f
        val torwart = aufstellung.torwart
        if (torwart != null) {
            Trikot(
                label = torwart.raSlot ?: torwart.nummer,
                farbe = trikotFarbe(torwart.position),
                ausgewaehlt = false,
                modifier = Modifier
                    .offset { IntOffset(twX.toPx().toInt(), twY.toPx().toInt()) }
                    .size(markerSize)
                    .clickable { onTorwartTippen() },
            )
        } else {
            Box(
                Modifier
                    .offset { IntOffset(twX.toPx().toInt(), twY.toPx().toInt()) }
                    .size(markerSize)
                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .clickable { onTorwartTippen() },
                contentAlignment = Alignment.Center,
            ) {
                Text("T", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
            }
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Sturm ↑", style = MaterialTheme.typography.labelSmall)
        Text("Abwehr ↓", style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpielerZellSheet(
    zelle: RasterZelle,
    belegt: AufstellungSpieler?,
    spieler: List<AufstellungSpieler>,
    auswahl: Boolean,
    onWechseln: () -> Unit,
    onAuswahl: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var suche by remember { mutableStateOf("") }
    var positionsFilter by remember(zelle) { mutableStateOf<SpielerPosition?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (belegt != null && !auswahl) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    "${zelle.label} · ${belegt.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${positionsName(belegt.position)} · Alter ${belegt.alter} · Skill ${belegt.skill} · Opti ${belegt.opti}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("Spielerdetails", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                Text("Fitness ${belegt.fit} · Moral ${belegt.mor}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onWechseln, modifier = Modifier.fillMaxWidth()) { Text("Spieler wechseln") }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Abbrechen") }
            }
        } else {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("${zelle.label} – Spieler auswählen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = suche,
                    onValueChange = { suche = it },
                    label = { Text("Spieler suchen") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                )
                val relevantPositionen = when {
                    zelle.torwart -> listOf(SpielerPosition.TOR)
                    zelle.bankIndex != null -> spielerPositionFilter
                    else -> spielerPositionFilter - SpielerPosition.TOR
                }
                Text("Position", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                ) {
                    item {
                        FilterChip(
                            selected = positionsFilter == null,
                            onClick = { positionsFilter = null },
                            label = { Text("Alle") },
                        )
                    }
                    items(relevantPositionen) { position ->
                        FilterChip(
                            selected = positionsFilter == position,
                            onClick = { positionsFilter = position },
                            label = { Text(positionKurzname(position)) },
                        )
                    }
                }
                val gefiltert = spieler.filter {
                    it.position in relevantPositionen &&
                        it.name.contains(suche, ignoreCase = true) &&
                        (positionsFilter == null || it.position == positionsFilter)
                }
                LazyColumn(Modifier.height(360.dp)) {
                    items(gefiltert, key = { it.pid }) { kandidat ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onAuswahl(kandidat.pid) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(kandidat.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "${positionsName(kandidat.position)} · ${kandidat.alter} · ${kandidat.skill} · ${kandidat.opti} · Fit ${kandidat.fit} · Mor ${kandidat.mor}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                kandidat.raSlot?.takeIf { it.isNotBlank() } ?: "-",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Abbrechen") }
            }
        }
    }
}

private fun positionKurzname(position: SpielerPosition): String = when (position) {
    SpielerPosition.TOR -> "TOR"
    SpielerPosition.ABW -> "ABW"
    SpielerPosition.DMI -> "DMI"
    SpielerPosition.MIT -> "MIT"
    SpielerPosition.OMI -> "OMI"
    SpielerPosition.STU -> "STU"
    SpielerPosition.AMATEUR -> "A"
}

@Composable
private fun ZatKontrolle(aufstellung: Aufstellung) {
    val feld = aufstellung.feldspieler.size
    val bank = aufstellung.bank.size
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("ZAT-Kontrolle", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Kontrollzeile(aufstellung.torwart != null, "Torwart vorhanden")
            Kontrollzeile(feld == 10, "$feld von 10 Feldspielern aufgestellt")
            Kontrollzeile(bank == 6, "$bank von 6 Ersatzplätzen belegt")
        }
    }
}

@Composable
private fun Kontrollzeile(gueltig: Boolean, text: String) {
    Text(
        "${if (gueltig) "✓" else "✗"} $text",
        color = if (gueltig) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun AufstellungsWerteAnsicht(werte: AufstellungsWerte?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Ermittelte Aufstellungswerte", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (werte == null || werte.istLeer) {
                Text(
                    "Noch keine Werte von der Website geliefert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    WebsiteWert("Opt. Skill", werte.optiSkill)
                    WebsiteWert("Skillschnitt", werte.skillSchnitt)
                    WebsiteWert("Fitness", werte.fitness)
                    WebsiteWert("Moral", werte.moral)
                }
            }
        }
    }
}

@Composable
private fun RowScope.WebsiteWert(label: String, wert: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(wert ?: "–", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun BankView(
    aufstellung: Aufstellung,
    onZelleTippen: (Int) -> Unit,
) {
    Text("Ersatzbank", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ERSATZBANK_BUCHSTABEN.forEachIndexed { index, buchstabe ->
            val spieler = aufstellung.spielerAufBankSlot(index)
            val belegt = spieler != null
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (belegt) Color(0x11000000) else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onZelleTippen(index) }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
            ) {
                if (!belegt) {
                    Text(
                        buchstabe.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                if (belegt) {
                    val istErsatztor = index == ERSATZTORWART_BANK_INDEX
                    Trikot(
                        label = spieler!!.raSlot.orEmpty().ifEmpty { spieler.nummer },
                        farbe = if (istErsatztor) Color(0xFFF9A825) else Color(0xFF1A237E),
                        ausgewaehlt = false,
                        modifier = Modifier.size(34.dp),
                    )
                } else {
                    Box(
                        Modifier
                            .size(34.dp)
                            .background(Color(0x33000000), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "–",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    if (belegt) {
                        buildString {
                            append(spieler!!.name.split(" ").lastOrNull().orEmpty())
                            if (index == ERSATZTORWART_BANK_INDEX) append("\nTW")
                        }
                    } else {
                        "frei"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Trikot(
    label: String,
    farbe: Color,
    ausgewaehlt: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .background(
                color = farbe,
                shape = RoundedCornerShape(6.dp),
            )
            .then(
                if (ausgewaehlt) Modifier
                    .padding(2.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (ausgewaehlt) farbe else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

private fun positionsName(position: SpielerPosition): String = when (position) {
    SpielerPosition.TOR -> "Torwart"
    SpielerPosition.ABW -> "Abwehr"
    SpielerPosition.DMI -> "Def. Mittelfeld"
    SpielerPosition.MIT -> "Mittelfeld"
    SpielerPosition.OMI -> "Off. Mittelfeld"
    SpielerPosition.STU -> "Sturm"
    SpielerPosition.AMATEUR -> "Amateur"
}

private fun trikotFarbe(position: SpielerPosition): Color = when (position) {
    SpielerPosition.TOR -> Color(0xFFF9A825)
    SpielerPosition.ABW -> Color(0xFF43A047)
    SpielerPosition.DMI -> Color(0xFF1E88E5)
    SpielerPosition.MIT -> Color(0xFF36BFF9)
    SpielerPosition.OMI -> Color(0xFFE040FB)
    SpielerPosition.STU -> Color(0xFFE53935)
    SpielerPosition.AMATEUR -> Color(0xFF9E9E9E)
}

private fun Aufstellung.slotText(spieler: AufstellungSpieler): String = when (val slot = spieler.slot) {
    is AufstellungSlot.Feld -> "Feld (Zeile ${('A' + 14 - slot.zeile).toChar()}, Spalte ${slot.spalte + 1})"
    is AufstellungSlot.Ersatz -> if (slot.index == ERSATZTORWART_BANK_INDEX) "Ersatztorwart (${spieler.raSlot ?: ""})" else "Ersatzbank (${spieler.raSlot ?: ""})"
    AufstellungSlot.Torwart -> "Torwart (${spieler.raSlot ?: ""})"
    null -> ""
}
