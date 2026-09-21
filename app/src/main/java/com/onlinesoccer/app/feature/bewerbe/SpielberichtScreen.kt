package com.onlinesoccer.app.feature.bewerbe

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.core.ui.theme.trikotFarbe
import com.onlinesoccer.app.data.model.BerichtAufstellung
import com.onlinesoccer.app.data.model.BerichtEinstellungen
import com.onlinesoccer.app.data.model.BerichtEreignisTyp
import com.onlinesoccer.app.data.model.BerichtSpielerStatistikEintrag
import com.onlinesoccer.app.data.model.SpielerPosition
import com.onlinesoccer.app.data.repository.ElfAuswertung
import com.onlinesoccer.app.data.repository.ElfBewertung
import com.onlinesoccer.app.ui.components.SpielverlaufEreignisKarte
import com.onlinesoccer.app.ui.components.SpielverlaufLegende
import com.onlinesoccer.app.ui.components.kartenNameFarbe
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast

/** Nativer Spielbericht (wie die Berichtsseite der Website). */
@Composable
fun SpielberichtScreen(
    onClose: () -> Unit,
    viewModel: SpielberichtViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.ladend -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        uiState.fehler != null -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(uiState.fehler!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = viewModel::lade) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text("Erneut versuchen")
            }
        }

        uiState.bericht != null -> BerichtsAnsicht(uiState.bericht!!, onClose)
    }
}

@Composable
private fun BerichtsAnsicht(bericht: com.onlinesoccer.app.data.model.SpielBericht, onClose: () -> Unit) {
    var zeigePressekonferenz by remember { mutableStateOf(false) }
    var markierterName by remember { mutableStateOf<String?>(null) }
    val noteProName = remember(bericht) {
        ElfAuswertung.kandidatenAusBericht(bericht)
            .associate { it.name.lowercase() to ElfBewertung.bewerten(it).gesamt }
    }
    val spielerKlick: (String) -> Unit = { name ->
        markierterName = if (markierterName == name) null else name
    }
    val bekannteNamen = remember(bericht) {
        buildSet {
            bericht.heimAufstellung?.spieler?.forEach { add(it.name) }
            bericht.gastAufstellung?.spieler?.forEach { add(it.name) }
        }
    }
    val kartenEreignisse = remember(bericht, bekannteNamen) {
        ElfAuswertung.kartenEreignisse(bericht.ereignisse, bekannteNamen)
    }
    val nameJeKartenIndex = remember(kartenEreignisse) { kartenEreignisse.associate { it.first to it.second } }
    val kartenTypProName = remember(bericht, bekannteNamen) {
        ElfAuswertung.kartenJeSpieler(bericht.ereignisse, bekannteNamen)
    }
    val torschuetzenJeEreignis = remember(bericht, bekannteNamen) {
        ElfAuswertung.torschuetzenJeEreignis(bericht.ereignisse, bekannteNamen)
    }
    val verletzteSpieler = remember(bericht, bekannteNamen) {
        ElfAuswertung.verletzteSpieler(bericht.ereignisse, bekannteNamen)
    }
    val verletzteSpielerNamen = remember(bericht, bekannteNamen) {
        ElfAuswertung.verletzteJeSpieler(bericht.ereignisse, bekannteNamen)
    }
    val torschuetzen = remember(bericht, bekannteNamen) {
        ElfAuswertung.torschuetzenEreignisse(bericht.ereignisse, bekannteNamen)
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                }
                Text("Spielbericht", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${bericht.heim ?: "?"} - ${bericht.gast ?: "?"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item {
            val meta = listOfNotNull(
                bericht.datum?.let { "Datum: $it" },
                bericht.stadion?.let { "Stadion: $it" },
                bericht.spielart?.let { "Spielart: $it" },
                bericht.zuschauer?.let { "Zuschaueranzahl: $it" },
            ).joinToString("   ")
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            AufstellungsVergleich(
                heim = bericht.heim,
                gast = bericht.gast,
                heimAufstellung = bericht.heimAufstellung,
                gastAufstellung = bericht.gastAufstellung,
                markierterName = markierterName,
                onSpielerKlick = spielerKlick,
            )
        }

        bericht.rohtext?.let {
            if (bericht.ereignisse.isEmpty() && !bericht.url.contains("bericht.php")) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }

        if (bericht.ereignisse.isNotEmpty()) {
            item { KiKommentarButton(bericht) }
            item { Text("Spielverlauf", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { SpielverlaufLegende() }
            itemsIndexed(bericht.ereignisse, key = { _, ereignis -> "${ereignis.typ}-${ereignis.minute}-${ereignis.text}" }) { index, ereignis ->
                SpielverlaufEreignisKarte(
                    minute = ereignis.minute,
                    text = ereignis.text,
                    typ = ereignis.typ,
                    spielerName = nameJeKartenIndex[index]
                        ?: torschuetzenJeEreignis[index]
                        ?: verletzteSpieler[index],
                )
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Endergebnis", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(bericht.heim ?: "Heim", modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                        Text("  ${bericht.ergebnis ?: "–"}  ", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
                        Text(bericht.gast ?: "Gast", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    }
                    if (torschuetzen.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Torschützen", style = MaterialTheme.typography.labelMedium)
                        torschuetzen.forEach { torschuetze ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    torschuetze.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    torschuetze.minute?.let { "$it'" } ?: "–",
                                    modifier = Modifier.width(36.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    torschuetze.spielstand ?: "–",
                                    modifier = Modifier.width(40.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.End,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }

        bericht.statistik?.let { stat ->
            item { Text("Statistik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Spacer(Modifier.weight(1f))
                            Text("Heim", modifier = Modifier.weight(0.35f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall)
                            Text("Gast", modifier = Modifier.weight(0.35f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall)
                        }
                        StatistikZeile("Abseits", stat.abseits)
                        StatistikZeile("Ecken", stat.ecken)
                        StatistikZeile("Fouls", stat.fouls)
                        StatistikZeile("Elfmeter", stat.elfmeter)
                        StatistikZeile("Ballbesitz", stat.ballbesitz)
                        StatistikZeile("Ø Skill", stat.schnittSkill)
                        StatistikZeile("Ø Opt.Skill", stat.schnittOpti)
                        StatistikZeile("Fitness", stat.fitness)
                        StatistikZeile("Moral", stat.moral)
                    }
                }
            }
        }

        if (bericht.heimSpielerStatistikListe.isNotEmpty() || bericht.gastSpielerStatistikListe.isNotEmpty()) {
            item {
                SpielerstatistikenVergleich(
                    heim = bericht.heim,
                    gast = bericht.gast,
                    heimEintraege = bericht.heimSpielerStatistikListe,
                    gastEintraege = bericht.gastSpielerStatistikListe,
                    heimAufstellung = bericht.heimAufstellung,
                    gastAufstellung = bericht.gastAufstellung,
                    kartenTypProName = kartenTypProName,
                    verletzteProName = verletzteSpielerNamen,
                    noteProName = noteProName,
                    markierterName = markierterName,
                    onSpielerKlick = spielerKlick,
                )
            }
        }

        item {
            KiPressekonferenzButton(onOeffnen = { zeigePressekonferenz = true })
        }
    }

    if (zeigePressekonferenz) {
        PressekonferenzDialog(bericht, onDismiss = { zeigePressekonferenz = false })
    }
}

@Composable
private fun KiKommentarButton(bericht: com.onlinesoccer.app.data.model.SpielBericht) {
    val context = LocalContext.current
    FilledTonalButton(
        onClick = {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, KiKommentarPrompt.ausBericht(bericht))
                putExtra(Intent.EXTRA_TITLE, "KI-Live-Kommentar")
            }
            try {
                if (sendIntent.resolveActivity(context.packageManager) == null) {
                    throw ActivityNotFoundException()
                }
                context.startActivity(Intent.createChooser(sendIntent, "KI-App auswählen"))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "Keine kompatible KI-App gefunden.", Toast.LENGTH_LONG).show()
            } catch (_: SecurityException) {
                Toast.makeText(context, "Die KI-App konnte nicht geöffnet werden.", Toast.LENGTH_LONG).show()
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.Mic, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("KI-Live-Kommentar")
    }
}

@Composable
private fun KiPressekonferenzButton(onOeffnen: () -> Unit) {
    FilledTonalButton(
        onClick = onOeffnen,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("KI-Pressekonferenz")
    }
}

/** Zeigt die Aussagen beider Trainer samt Umschalter und Kopier-Button fürs Forum. */
@Composable
private fun PressekonferenzDialog(
    bericht: com.onlinesoccer.app.data.model.SpielBericht,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var trainer by remember { mutableStateOf(KiPressekonferenz.Trainer.HEIM) }
    val aussage = remember(bericht, trainer) { KiPressekonferenz.aussage(bericht, trainer) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(
                        KiPressekonferenz.Trainer.HEIM to (bericht.heim ?: "Heim"),
                        KiPressekonferenz.Trainer.GAST to (bericht.gast ?: "Gast"),
                    ).forEachIndexed { index, (option, label) ->
                        SegmentedButton(
                            selected = trainer == option,
                            onClick = { trainer = option },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                label,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Text(aussage.ueberschrift, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(aussage.text, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Beitrag kopieren und im Forum posten.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(aussage.ganz()))
                    Toast.makeText(context, "Beitrag in die Zwischenablage kopiert.", Toast.LENGTH_LONG).show()
                },
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Kopieren")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        },
    )
}

@Composable
private fun AufstellungsVergleich(
    heim: String?,
    gast: String?,
    heimAufstellung: com.onlinesoccer.app.data.model.BerichtAufstellung?,
    gastAufstellung: com.onlinesoccer.app.data.model.BerichtAufstellung?,
    markierterName: String? = null,
    onSpielerKlick: (String) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Taktische Aufstellungen und Spieleraufgebot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(heim ?: "Heimteam", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = Color(0xFFFF1744))
            Text(gast ?: "Auswärtsteam", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = Color(0xFFFF1744))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) { FormationSpielfeld(heimAufstellung) }
            Box(Modifier.weight(1f)) { FormationSpielfeld(gastAufstellung) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) { SpielerListe(heimAufstellung, markierterName, onSpielerKlick) }
            Box(Modifier.weight(1f)) { SpielerListe(gastAufstellung, markierterName, onSpielerKlick) }
        }
        EinstellungenTabelle(heim, gast, heimAufstellung?.einstellungen ?: BerichtEinstellungen(), gastAufstellung?.einstellungen ?: BerichtEinstellungen())
    }
}

@Composable
private fun Aufstellung(
    heim: String?,
    gast: String?,
    heimAufstellung: com.onlinesoccer.app.data.model.BerichtAufstellung?,
    gastAufstellung: com.onlinesoccer.app.data.model.BerichtAufstellung?,
) {
    var heimAktiv by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    val aufstellung = if (heimAktiv) heimAufstellung else gastAufstellung
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Aufstellung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (heimAktiv) {
                FilledTonalButton(onClick = { heimAktiv = false }, modifier = Modifier.weight(1f)) { Text(heim ?: "Heim") }
            } else {
                OutlinedButton(onClick = { heimAktiv = true }, modifier = Modifier.weight(1f)) { Text(heim ?: "Heim") }
            }
            if (!heimAktiv) {
                FilledTonalButton(onClick = { heimAktiv = true }, modifier = Modifier.weight(1f)) { Text(gast ?: "Gast") }
            } else {
                OutlinedButton(onClick = { heimAktiv = false }, modifier = Modifier.weight(1f)) { Text(gast ?: "Gast") }
            }
        }
        FormationSpielfeld(aufstellung)
        SpielerListe(aufstellung)
        EinstellungenTabelle(heim, gast, heimAufstellung?.einstellungen ?: BerichtEinstellungen(), gastAufstellung?.einstellungen ?: BerichtEinstellungen())
    }
}

@Composable
private fun FormationSpielfeld(
    aufstellung: BerichtAufstellung?,
) {
    val spieler = aufstellung?.startspieler.orEmpty()
    if (spieler.none { it.feldzeile != null && it.feldspalte != null }) {
        Text("Keine Rasterpositionen im Spielbericht gefunden.", style = MaterialTheme.typography.bodySmall)
        return
    }
    val marker = spieler.mapNotNull { player ->
        val row = player.feldzeile ?: return@mapNotNull null
        val column = player.feldspalte ?: return@mapNotNull null
        Triple(row, column, player)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(11f / 15f)
            .background(Color(0xFF176B2C), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFB8E0BD), RoundedCornerShape(12.dp))
            .padding(4.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(20.dp)) {
            Spacer(Modifier.width(24.dp))
            repeat(11) { column ->
                Text("${column + 1}", Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.width(24.dp).fillMaxSize()) {
                "ONMLKJIHGFEDCBA".forEach { rowLabel ->
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(rowLabel.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxSize()) {
                repeat(15) { row ->
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        repeat(11) { column ->
                            val marker = marker.firstOrNull { it.first == row && it.second == column }
                            val player = marker?.third
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .border(0.5.dp, Color(0x6688BB8C)),
                                contentAlignment = Alignment.Center,
                            ) {
                                player?.let {
                                    Box(
                                        Modifier
                                            .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
                                            .background(markerColor(it.position), RoundedCornerShape(5.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            it.nummer ?: "?",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        marker.firstOrNull { it.first == 15 }?.third?.let { goalkeeper ->
            Row(Modifier.fillMaxWidth().height(28.dp)) {
                Spacer(Modifier.width(24.dp))
                repeat(11) { column ->
                    Box(
                        Modifier.weight(1f).fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (column == 5) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(3.dp)
                                    .background(markerColor(goalkeeper.position), RoundedCornerShape(5.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    goalkeeper.nummer ?: "T",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun markerColor(position: String?): Color = when (position) {
    "Sturm" -> trikotFarbe(SpielerPosition.STU)
    "Mittelfeld" -> trikotFarbe(SpielerPosition.MIT)
    "Abwehr" -> trikotFarbe(SpielerPosition.ABW)
    "Torwart" -> trikotFarbe(SpielerPosition.TOR)
    else -> Color(0xFFE0E0E0)
}

@Composable
private fun SpielerListe(
    aufstellung: BerichtAufstellung?,
    markierterName: String? = null,
    onSpielerKlick: (String) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Spieleraufgebot", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (aufstellung?.spieler.isNullOrEmpty()) {
            Text("Keine Spielerdaten im Bericht gefunden.", style = MaterialTheme.typography.bodySmall)
        } else {
            aufstellung!!.spieler.forEach { spieler ->
                val markiert = spieler.name.lowercase() == markierterName
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (markiert) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        )
                        .clickable { onSpielerKlick(spieler.name.lowercase()) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        spieler.nummer.orEmpty(),
                        modifier = Modifier.width(32.dp),
                        fontWeight = FontWeight.Bold,
                        color = if (markiert) MaterialTheme.colorScheme.onPrimaryContainer else LocalContentColor.current,
                    )
                    Text(
                        spieler.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (markiert) FontWeight.Bold else FontWeight.Normal,
                        color = if (markiert) MaterialTheme.colorScheme.onPrimaryContainer else LocalContentColor.current,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EinstellungenTabelle(
    heim: String?,
    gast: String?,
    heimWerte: BerichtEinstellungen,
    gastWerte: BerichtEinstellungen,
) {
    Text("Start-Einstellungen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(0.8f))
                Text(
                    heim ?: "Heim",
                    modifier = Modifier.weight(1.1f),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    gast ?: "Gast",
                    modifier = Modifier.weight(1.1f),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            EinstellungZeile("Einsatz", heimWerte.einsatz, gastWerte.einsatz)
            EinstellungZeile("Härte", heimWerte.haerte, gastWerte.haerte)
            EinstellungZeile("Spielweise", heimWerte.spielweise, gastWerte.spielweise)
            EinstellungZeile("Taktik - Sturm", heimWerte.taktikSturm, gastWerte.taktikSturm)
            EinstellungZeile("Taktik - Mittelfeld", heimWerte.taktikMittelfeld, gastWerte.taktikMittelfeld)
            EinstellungZeile("Taktik - Abwehr", heimWerte.taktikAbwehr, gastWerte.taktikAbwehr)
        }
    }
}

@Composable
private fun EinstellungZeile(label: String, heim: String?, gast: String?) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Text(
            heim ?: "–",
            modifier = Modifier.weight(1.1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Text(
            gast ?: "–",
            modifier = Modifier.weight(1.1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatistikZeile(label: String, wert: String?) {
    wert?.let {
        val werte = it.split(" : ", limit = 2)
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(werte[0], modifier = Modifier.weight(0.35f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(werte.getOrNull(1).orEmpty(), modifier = Modifier.weight(0.35f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}

/** Übersicht aller eingesetzten Spieler je Team (wie „Es folgen die Spielerstatistiken"). */
@Composable
private fun SpielerstatistikenVergleich(
    heim: String?,
    gast: String?,
    heimEintraege: List<BerichtSpielerStatistikEintrag>,
    gastEintraege: List<BerichtSpielerStatistikEintrag>,
    heimAufstellung: BerichtAufstellung?,
    gastAufstellung: BerichtAufstellung?,
    kartenTypProName: Map<String, BerichtEreignisTyp>,
    verletzteProName: Map<String, BerichtEreignisTyp>,
    noteProName: Map<String, Double>,
    markierterName: String? = null,
    onSpielerKlick: (String) -> Unit = {},
) {
    val textMeasurer = rememberTextMeasurer()
    val spaltenBreiten = statSpaltenBreiten(textMeasurer, heimEintraege + gastEintraege, noteProName)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Spielerstatistiken", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SpielerstatistikTabelle(
            teamName = heim ?: "Heimteam",
            eintraege = heimEintraege,
            aufstellung = heimAufstellung,
            noteProName = noteProName,
            spaltenBreiten = spaltenBreiten,
            markierterName = markierterName,
            onSpielerKlick = onSpielerKlick,
            kartenTypProName = kartenTypProName,
            verletzteProName = verletzteProName,
        )
        SpielerstatistikTabelle(
            teamName = gast ?: "Gastteam",
            eintraege = gastEintraege,
            aufstellung = gastAufstellung,
            noteProName = noteProName,
            spaltenBreiten = spaltenBreiten,
            markierterName = markierterName,
            onSpielerKlick = onSpielerKlick,
            kartenTypProName = kartenTypProName,
            verletzteProName = verletzteProName,
        )
    }
}

private val nummernBreite = 24.dp
private val spielerNameBreite = 120.dp

/** Auf die Inhalte gemessene Spaltenbreiten der Spielerstatistik-Tabelle. */
private data class StatSpaltenBreiten(
    val note: androidx.compose.ui.unit.Dp,
    val zk: androidx.compose.ui.unit.Dp,
    val quote: androidx.compose.ui.unit.Dp,
    val tore: androidx.compose.ui.unit.Dp,
    val vorlagen: androidx.compose.ui.unit.Dp,
    val schuesse: androidx.compose.ui.unit.Dp,
    val aufsTor: androidx.compose.ui.unit.Dp,
)

@Composable
private fun statSpaltenBreiten(
    textMeasurer: TextMeasurer,
    eintraege: List<BerichtSpielerStatistikEintrag>,
    noteProName: Map<String, Double>,
): StatSpaltenBreiten {
    val density = LocalDensity.current
    val kopfStil = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
    val zellenStil = MaterialTheme.typography.bodySmall
    fun breite(label: String, werte: List<String>): androidx.compose.ui.unit.Dp = with(density) {
        val labelPx = textMeasurer.measure(label, kopfStil).size.width
        val wertPx = werte.maxOfOrNull { textMeasurer.measure(it, zellenStil).size.width } ?: 0
        maxOf(labelPx, wertPx).toDp() + 12.dp
    }
    fun noteText(eintrag: BerichtSpielerStatistikEintrag): String =
        noteProName[eintrag.name.lowercase()]?.let(ElfBewertung::formatiere) ?: "–"
    val note = breite("Note", eintraege.map { noteText(it) })
    val zk = breite("ZK", eintraege.map { it.statistik.zweikaempfe.toString() })
    val quote = breite("Quote", eintraege.map { it.statistik.zweikampfQuote.formatProzent() })
    val tore = breite("Tore", eintraege.map { it.statistik.tore.toString() })
    val vorlagen = breite("Vorl.", eintraege.map { it.statistik.vorlagen.toString() })
    val schuesse = breite("Schüsse", eintraege.map { it.statistik.schuesse.toString() })
    val aufsTor = breite("aufs Tor", eintraege.map { it.statistik.aufsTor.toString() })
    return StatSpaltenBreiten(note, zk, quote, tore, vorlagen, schuesse, aufsTor)
}

@Composable
private fun SpielerstatistikTabelle(
    teamName: String,
    eintraege: List<BerichtSpielerStatistikEintrag>,
    aufstellung: BerichtAufstellung?,
    noteProName: Map<String, Double>,
    spaltenBreiten: StatSpaltenBreiten,
    markierterName: String? = null,
    onSpielerKlick: (String) -> Unit = {},
    kartenTypProName: Map<String, BerichtEreignisTyp>,
    verletzteProName: Map<String, BerichtEreignisTyp>,
) {
    if (eintraege.isEmpty()) return
    val scrollState = rememberScrollState()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(teamName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(nummernBreite))
                Text(
                    "Spieler",
                    modifier = Modifier.width(spielerNameBreite),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Row(Modifier.weight(1f).horizontalScroll(scrollState)) {
                    StatKopfZelle("Note", spaltenBreiten.note)
                    StatKopfZelle("ZK", spaltenBreiten.zk)
                    StatKopfZelle("Quote", spaltenBreiten.quote)
                    StatKopfZelle("Tore", spaltenBreiten.tore)
                    StatKopfZelle("Vorl.", spaltenBreiten.vorlagen)
                    StatKopfZelle("Schüsse", spaltenBreiten.schuesse)
                    StatKopfZelle("aufs Tor", spaltenBreiten.aufsTor)
                }
            }
            eintraege.forEach { eintrag ->
                SpielerstatistikZeile(
                    eintrag = eintrag,
                    aufstellung = aufstellung,
                    noteProName = noteProName,
                    spaltenBreiten = spaltenBreiten,
                    markierterName = markierterName,
                    onSpielerKlick = onSpielerKlick,
                    kartenTypProName = kartenTypProName,
                    verletzteProName = verletzteProName,
                    scrollState = scrollState,
                )
            }
        }
    }
}

@Composable
private fun StatKopfZelle(label: String, breite: androidx.compose.ui.unit.Dp) {
    Text(
        label,
        modifier = Modifier.width(breite),
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}

@Composable
private fun SpielerstatistikZeile(
    eintrag: BerichtSpielerStatistikEintrag,
    aufstellung: BerichtAufstellung?,
    noteProName: Map<String, Double>,
    spaltenBreiten: StatSpaltenBreiten,
    markierterName: String? = null,
    onSpielerKlick: (String) -> Unit = {},
    kartenTypProName: Map<String, BerichtEreignisTyp>,
    verletzteProName: Map<String, BerichtEreignisTyp>,
    scrollState: ScrollState,
) {
    val spieler = aufstellung?.spieler?.firstOrNull { it.name.equals(eintrag.name, ignoreCase = true) }
    val stat = eintrag.statistik
    val nameSchluessel = (spieler?.name ?: eintrag.name).lowercase()
    val markiert = nameSchluessel == markierterName
    val grundNameFarbe = kartenTypProName[nameSchluessel]?.let { kartenNameFarbe(it) }
        ?: verletzteProName[nameSchluessel]?.let { kartenNameFarbe(it) }
    val nameFarbe = if (markiert) MaterialTheme.colorScheme.onPrimaryContainer else grundNameFarbe ?: LocalContentColor.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (markiert) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { onSpielerKlick(nameSchluessel) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (spieler?.nummer != null) {
            Box(
                Modifier
                    .size(18.dp)
                    .background(markerColor(spieler.position), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(spieler.nummer, color = if (markiert) MaterialTheme.colorScheme.primary else Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.width(6.dp))
        } else {
            Spacer(Modifier.width(nummernBreite))
        }
        Text(
            eintrag.name,
            modifier = Modifier.width(spielerNameBreite),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (markiert) FontWeight.Bold else FontWeight.Medium,
            color = nameFarbe,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Row(Modifier.weight(1f).horizontalScroll(scrollState)) {
            val note = noteProName[nameSchluessel]?.let(ElfBewertung::formatiere) ?: "–"
            StatWertZelle(note, spaltenBreiten.note)
            StatWertZelle(stat.zweikaempfe.toString(), spaltenBreiten.zk)
            StatWertZelle(stat.zweikampfQuote.formatProzent(), spaltenBreiten.quote)
            StatWertZelle(stat.tore.toString(), spaltenBreiten.tore)
            StatWertZelle(stat.vorlagen.toString(), spaltenBreiten.vorlagen)
            StatWertZelle(stat.schuesse.toString(), spaltenBreiten.schuesse)
            StatWertZelle(stat.aufsTor.toString(), spaltenBreiten.aufsTor)
        }
    }
}

@Composable
private fun StatWertZelle(wert: String, breite: androidx.compose.ui.unit.Dp) {
    Text(
        wert,
        modifier = Modifier.width(breite),
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}

private fun Double.formatProzent(): String =
    String.format(Locale.GERMANY, "%.1f", this).removeSuffix(",0")
