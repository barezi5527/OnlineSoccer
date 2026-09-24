package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onlinesoccer.app.core.ui.theme.PositionsBadge
import com.onlinesoccer.app.data.model.LaenderOption

/** Einheitliche Zeilenhöhe der fixierbaren Statistik-Tabelle. */
internal val StatistikScrollZeilenHoehe = 40.dp

/** Kopfzeile mit Zurück-Pfeil, Titel und Untertitel. */
@Composable
fun StatistikHeader(
    titel: String,
    untertitel: String? = null,
    onClose: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
        }
        Column(Modifier.padding(start = 4.dp)) {
            Text(
                titel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!untertitel.isNullOrBlank()) {
                Text(
                    untertitel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Zentrierte Ladeanzeige. */
@Composable
fun StatistikLaden() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Fehleranzeige mit „Erneut versuchen"-Button. */
@Composable
fun StatistikFehler(fehler: String, onErneut: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(fehler, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onErneut) {
            Text("Erneut versuchen")
        }
    }
}

/** Hinweis „keine Treffer". */
@Composable
fun StatistikKeineTreffer(text: String = "Keine Treffer.") {
    Card(Modifier.fillMaxWidth()) {
        Text(
            text,
            Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Auswahl-Eingabe (ExposedDropdownMenuBox) wie in den übrigen App-Filtern. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatistikDropdown(
    label: String,
    optionen: List<LaenderOption>,
    wert: String,
    ladend: Boolean,
    onWaehlen: (String) -> Unit,
) {
    var offen by remember { mutableStateOf(false) }
    val auswahl = optionen.firstOrNull { it.id == wert }?.label ?: "–"
    ExposedDropdownMenuBox(
        expanded = offen,
        onExpandedChange = { if (!ladend && optionen.isNotEmpty()) offen = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = auswahl,
            onValueChange = {},
            readOnly = true,
            enabled = !ladend && optionen.isNotEmpty(),
            label = { Text(label) },
            maxLines = 1,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = offen) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            optionen.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        offen = false
                        if (option.id != wert) onWaehlen(option.id)
                    },
                )
            }
        }
    }
}

/** Karte für Filterreihen (jeweils ein [StatistikDropdown] pro Zeile). */
@Composable
fun StatistikFilterCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) { content() }
    }
}

/** Umschalter zum Ein-/Ausblenden der Filterkarten (neben der Überschrift). */
@Composable
fun StatistikFilterToggle(offen: Boolean, onToggle: () -> Unit) {
    FilledTonalButton(onClick = onToggle) {
        Icon(
            if (offen) Icons.Filled.ExpandLess else Icons.Filled.FilterList,
            contentDescription = null,
        )
        Spacer(Modifier.width(4.dp))
        Text(if (offen) "Filter ausblenden" else "Filter")
    }
}

/** Ländercode als kleines Text-Badge (keine Netzwerk-Bilder in der App). */
@Composable
fun FlaggenText(code: String) {
    val text = code.trim().uppercase()
    if (text.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

/** Spielername mit Positions-Badge; klickbar, wenn eine Player-ID vorliegt. */
@Composable
fun StatistikSpielerZelle(
    name: String,
    position: String?,
    pid: Long?,
    onSpielerClick: ((Long) -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (pid != null && onSpielerClick != null) {
            Modifier.clickable { onSpielerClick(pid) }
        } else {
            Modifier
        },
    ) {
        if (!position.isNullOrBlank()) {
            PositionsBadge(position)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Teamname; klickbar, wenn eine Team-ID vorliegt. */
@Composable
fun StatistikTeamZelle(
    team: String,
    teamId: Long?,
    onTeamClick: ((Long) -> Unit)?,
) {
    Text(
        team,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = if (teamId != null && onTeamClick != null) {
            Modifier.clickable { onTeamClick(teamId) }
        } else {
            Modifier
        },
    )
}

/** Zahlenwert rechtsbündig. */
@Composable
fun StatistikWert(wert: String) {
    Text(
        wert,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Kompakte Zahl innerhalb einer Tabellenzelle. */
@Composable
fun StatistikZahl(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Spalte der fixierbaren Tabelle mit fester Breite. */
data class StatistikFixSpalte(
    val kopf: String,
    val breite: Dp,
    val ausrichtung: TextAlign = TextAlign.Start,
)

/** Zeile der fixierbaren Tabelle: fixierte Spalte + restliche Spalten. */
data class StatistikScrollZeile(
    val pin: @Composable () -> Unit,
    val zellen: @Composable () -> Unit,
)

/**
 * Statistik-Tabelle mit fixierter Kopfzeile (bleibt beim Vertikal-Scroll stehen)
 * und fixierter erster Spalte (bleibt beim Horizontal-Scroll stehen). Die Zeilen
 * sind virtualisiert (LazyColumn); die Fixierung folgt per graphicsLayer-Übersetzung
 * den Scroll-Zuständen – der bewährte Sync auch für 100 Zeilen.
 */
@Composable
fun StatistikScrollTabelle(
    pinKopf: String,
    pinBreite: Dp,
    spalten: List<StatistikFixSpalte>,
    zeilen: List<StatistikScrollZeile>,
    modifier: Modifier = Modifier,
    markierung: ((Int) -> Boolean)? = null,
) {
    val hScroll = rememberScrollState()
    val listState = rememberLazyListState()
    val flaeche = MaterialTheme.colorScheme.surfaceVariant
    val zellenBreite = spalten.fold(0.dp) { summe, spalte -> summe + spalte.breite }
    val zeilenHoehe = StatistikScrollZeilenHoehe
    val density = LocalDensity.current
    val zeilenHoehePx = with(density) { zeilenHoehe.toPx() }

    var pinX by remember { mutableFloatStateOf(0f) }
    var pinY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> pinY = -(index * zeilenHoehePx + offset) }
    }
    LaunchedEffect(hScroll) {
        snapshotFlow { hScroll.value }.collect { wert -> pinX = -wert.toFloat() }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(flaeche)
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatistikScrollKopfZelle(pinKopf, pinBreite, TextAlign.Start)
            Box(
                Modifier
                    .weight(1f)
                    .clipToBounds(),
            ) {
                Row(
                    Modifier
                        .width(zellenBreite)
                        .graphicsLayer { translationX = pinX },
                ) {
                    spalten.forEach { spalte -> StatistikScrollKopfZelle(spalte.kopf, spalte.breite, spalte.ausrichtung) }
                }
            }
        }
        HorizontalDivider()
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds(),
        ) {
            if (zeilen.isEmpty()) {
                Text(
                    "Keine Treffer.",
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .width(pinBreite)
                            .fillMaxHeight()
                            .clipToBounds(),
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .graphicsLayer { translationY = pinY },
                        ) {
                            zeilen.forEachIndexed { index, zeile ->
                                val markiert = markierung?.invoke(index) == true
                                Row(
                                    Modifier
                                        .width(pinBreite)
                                        .height(zeilenHoehe)
                                        .background(statistikZeilenFarbe(index, markiert)),
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
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .padding(horizontal = 6.dp),
                                    ) { zeile.pin() }
                                }
                            }
                        }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clipToBounds(),
                    ) {
                        Row(Modifier.horizontalScroll(hScroll)) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .width(zellenBreite)
                                    .fillMaxHeight(),
                            ) {
                                items(count = zeilen.size) { index ->
                                    val markiert = markierung?.invoke(index) == true
                                    Row(
                                        Modifier
                                            .width(zellenBreite)
                                            .height(zeilenHoehe)
                                            .background(statistikZeilenFarbe(index, markiert)),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        zeilen[index].zellen()
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

/** Einheitlicher Farbhintergrund der Zeilen (Zebra, Hervorhebung der Markierung). */
@Composable
internal fun statistikZeilenFarbe(zahl: Int, markiert: Boolean): Color = when {
    markiert -> MaterialTheme.colorScheme.primaryContainer
    zahl % 2 == 1 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    else -> Color.Transparent
}

/** Kopfzelle der fixierbaren Tabelle. */
@Composable
private fun StatistikScrollKopfZelle(text: String, breite: Dp, ausrichtung: TextAlign) {
    Box(
        Modifier
            .width(breite)
            .padding(horizontal = 6.dp),
        contentAlignment = when (ausrichtung) {
            TextAlign.End -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        },
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Eine fix-breite Zelle innerhalb einer Zeile der fixierbaren Tabelle. */
@Composable
fun StatistikFixZelle(spalte: StatistikFixSpalte, content: @Composable () -> Unit) {
    Box(
        Modifier
            .width(spalte.breite)
            .height(StatistikScrollZeilenHoehe)
            .padding(horizontal = 6.dp),
        contentAlignment = when (spalte.ausrichtung) {
            TextAlign.End -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        },
    ) {
        content()
    }
}