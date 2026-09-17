package com.onlinesoccer.app.feature.bewerbe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.core.ui.theme.trikotFarbe
import com.onlinesoccer.app.data.model.BerichtEreignisTyp
import com.onlinesoccer.app.data.model.BerichtAufstellung
import com.onlinesoccer.app.data.model.BerichtEinstellungen
import com.onlinesoccer.app.data.model.SpielerPosition
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

        item { AufstellungsVergleich(bericht.heim, bericht.gast, bericht.heimAufstellung, bericht.gastAufstellung) }

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
            items(bericht.ereignisse) { ereignis ->
                val istHervorgehoben = ereignis.typ != BerichtEreignisTyp.SONSTIGES
                val ereignisFarbe = when (ereignis.typ) {
                    BerichtEreignisTyp.TOR -> Color(0xFFB9F6CA)
                    BerichtEreignisTyp.GELBE_KARTE -> Color(0xFFFFE082)
                    BerichtEreignisTyp.ROTE_KARTE -> Color(0xFFFFB4AB)
                    BerichtEreignisTyp.VERLETZUNG -> Color(0xFFD0BCFF)
                    BerichtEreignisTyp.ELFMETER -> Color(0xFFB3E5FC)
                    BerichtEreignisTyp.SONSTIGES -> MaterialTheme.colorScheme.surfaceVariant
                }
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ereignisFarbe,
                        contentColor = if (istHervorgehoben) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Row(Modifier.padding(10.dp)) {
                        ereignis.minute?.let {
                            Text(
                                "$it'",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(48.dp),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            ereignis.text,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            fontWeight = if (ereignis.typ != BerichtEreignisTyp.SONSTIGES) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
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
            Box(Modifier.weight(1f)) { SpielerListe(heimAufstellung) }
            Box(Modifier.weight(1f)) { SpielerListe(gastAufstellung) }
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
private fun FormationSpielfeld(aufstellung: BerichtAufstellung?) {
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
                                        Text(it.nummer ?: "?", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
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
                                Text(goalkeeper.nummer ?: "T", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
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
private fun SpielerListe(aufstellung: BerichtAufstellung?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Spieleraufgebot", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (aufstellung?.spieler.isNullOrEmpty()) {
            Text("Keine Spielerdaten im Bericht gefunden.", style = MaterialTheme.typography.bodySmall)
        } else {
            aufstellung!!.spieler.forEach { spieler ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(spieler.nummer.orEmpty(), modifier = Modifier.width(32.dp), fontWeight = FontWeight.Bold)
                    Text(spieler.name, style = MaterialTheme.typography.bodySmall)
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
    heimEintraege: List<com.onlinesoccer.app.data.model.BerichtSpielerStatistikEintrag>,
    gastEintraege: List<com.onlinesoccer.app.data.model.BerichtSpielerStatistikEintrag>,
    heimAufstellung: BerichtAufstellung?,
    gastAufstellung: BerichtAufstellung?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Spielerstatistiken", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SpielerstatistikTabelle(heim ?: "Heimteam", heimEintraege, heimAufstellung)
        SpielerstatistikTabelle(gast ?: "Gastteam", gastEintraege, gastAufstellung)
    }
}

private val noteBreite = 34.dp
private val zkBreite = 26.dp
private val zkProzentBreite = 44.dp
private val schuesseBreite = 38.dp
private val aufsTorBreite = 46.dp
private val toreBreite = 30.dp
private val vorlagenBreite = 40.dp
private val nummernBreite = 24.dp

@Composable
private fun SpielerstatistikTabelle(
    teamName: String,
    eintraege: List<com.onlinesoccer.app.data.model.BerichtSpielerStatistikEintrag>,
    aufstellung: BerichtAufstellung?,
) {
    if (eintraege.isEmpty()) return
    val zeigeNote = eintraege.any { !it.statistik.note.isNullOrBlank() }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(teamName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(nummernBreite))
                Text(
                    "Spieler",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                )
                if (zeigeNote) StatKopfZelle("Note", noteBreite)
                StatKopfZelle("ZK", zkBreite)
                StatKopfZelle("ZK-%", zkProzentBreite)
                StatKopfZelle("Schüsse", schuesseBreite)
                StatKopfZelle("aufs Tor", aufsTorBreite)
                StatKopfZelle("Tore", toreBreite)
                StatKopfZelle("Vorl.", vorlagenBreite)
            }
            eintraege.forEach { eintrag ->
                SpielerstatistikZeile(eintrag, aufstellung, zeigeNote)
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
        maxLines = 2,
    )
}

@Composable
private fun SpielerstatistikZeile(
    eintrag: com.onlinesoccer.app.data.model.BerichtSpielerStatistikEintrag,
    aufstellung: BerichtAufstellung?,
    zeigeNote: Boolean,
) {
    val spieler = aufstellung?.spieler?.firstOrNull { it.name.equals(eintrag.name, ignoreCase = true) }
    val stat = eintrag.statistik
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (spieler?.nummer != null) {
            Box(
                Modifier
                    .size(18.dp)
                    .background(markerColor(spieler.position), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(spieler.nummer, color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.width(6.dp))
        } else {
            Spacer(Modifier.width(nummernBreite))
        }
        Text(
            eintrag.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
        if (zeigeNote) {
            StatWertZelle(stat.note ?: "–", noteBreite)
        }
        StatWertZelle(stat.zweikaempfe.toString(), zkBreite)
        StatWertZelle(stat.zweikampfQuote.formatProzent(), zkProzentBreite)
        StatWertZelle(stat.schuesse.toString(), schuesseBreite)
        StatWertZelle(stat.aufsTor.toString(), aufsTorBreite)
        StatWertZelle(stat.tore.toString(), toreBreite)
        StatWertZelle(stat.vorlagen.toString(), vorlagenBreite)
    }
}

@Composable
private fun StatWertZelle(wert: String, breite: androidx.compose.ui.unit.Dp) {
    Text(
        wert,
        modifier = Modifier.width(breite),
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun Double.formatProzent(): String {
    val ganze = this % 1.0 == 0.0
    return if (ganze) toInt().toString() else toString()
}
