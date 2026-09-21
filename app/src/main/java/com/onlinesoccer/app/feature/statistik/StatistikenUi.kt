package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onlinesoccer.app.core.ui.theme.PositionsBadge
import com.onlinesoccer.app.data.model.LaenderOption

/** Spaltendefinition einer Statistik-Tabelle (Kopf + relative Breite + Ausrichtung). */
data class StatistikSpalte(
    val kopf: String,
    val gewicht: Float,
    val ausrichtung: TextAlign = TextAlign.Start,
)

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

/** Ladeanzeige (zentriert). */
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
        Modifier.fillMaxSize().padding(24.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatistikDropdown(
    label: String,
    optionen: List<LaenderOption>,
    wert: String,
    ladend: Boolean,
    onWaehlen: (String) -> Unit,
) {
    StatistikAuswahl(
        optionen = optionen,
        wert = wert,
        onWaehlen = onWaehlen,
        label = label,
        enabled = !ladend && optionen.isNotEmpty(),
    )
}

/** Kompakte Auswahl (ohne Label) für Kriteriumszeilen der Spielersuche. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatistikAuswahl(
    optionen: List<LaenderOption>,
    wert: String,
    onWaehlen: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    var offen by remember { mutableStateOf(false) }
    val auswahl = optionen.firstOrNull { it.id == wert }?.label ?: "–"
    ExposedDropdownMenuBox(
        expanded = offen,
        onExpandedChange = { if (enabled && optionen.isNotEmpty()) offen = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = auswahl,
            onValueChange = {},
            readOnly = true,
            enabled = enabled && optionen.isNotEmpty(),
            label = label?.let { { Text(it) } },
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
                        onWaehlen(option.id)
                    },
                )
            }
        }
    }
}

/** Flaggen-Kürzel als kleines Text-Badge (keine Netzwerk-Bilder in der App). */
@Composable
fun FlaggenText(code: String) {
    val text = code.trim().uppercase()
    if (text.isEmpty()) return
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Komplette Statistik-Tabelle als Card: Kopfzeile, Trennlinie und Zeilen.
 * Jede Zeile ist ein [`RowScope`]-Lambda, das über [StatistikSpalte] die
 * Spalten befüllt. Bei leeren Zeilen wird „Keine Treffer." angezeigt.
 */
@Composable
fun StatistikTabelle(
    spalten: List<StatistikSpalte>,
    zeilen: List<@Composable RowScope.() -> Unit>,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                spalten.forEach { spalte ->
                    StatistikSpalte(spalte) {
                        Text(
                            spalte.kopf,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = spalte.ausrichtung,
                        )
                    }
                }
            }
            HorizontalDivider()
            if (zeilen.isEmpty()) {
                Text(
                    "Keine Treffer.",
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                zeilen.forEachIndexed { index, zeile ->
                    if (index > 0) HorizontalDivider()
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { zeile() }
                }
            }
        }
    }
}

/** Eine gewichtete Tabellenspalte innerhalb einer Zeile. */
@Composable
fun RowScope.StatistikSpalte(spalte: StatistikSpalte, content: @Composable () -> Unit) {
    Box(
        Modifier
            .weight(spalte.gewicht)
            .fillMaxWidth()
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
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Card für Filterreihen (jeweils ein [StatistikDropdown] pro Zeile). */
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

/** Einheitliche Zeilenhöhe – Grundlage für die Sync zwischen fixierter und scrollender Spalte. */
private val StatistikScrollZeilenHoehe = 40.dp

/**
 * Statistik-Tabelle mit fixierter Kopfzeile (bleibt beim Vertikal-Scroll stehen)
 * und fixierter erster Spalte (bleibt beim Horizontal-Scroll stehen).
 * Die restlichen Spalten scrollen horizontal, die Zeilen vertikal.
 */
@Composable
fun StatistikScrollTabelle(
    pinKopf: String,
    pinBreite: Dp,
    spalten: List<StatistikFixSpalte>,
    zeilen: List<StatistikScrollZeile>,
    modifier: Modifier = Modifier,
) {
    val vertikal = rememberScrollState()
    val horizontal = rememberScrollState()
    val flaeche = MaterialTheme.colorScheme.surfaceContainerLow
    val zellenBreite = spalten.fold(0.dp) { summe, spalte -> summe + spalte.breite }

    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = flaeche),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(flaeche)
                    .height(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatistikScrollPinKopf(pinKopf, pinBreite)
                Box(
                    Modifier
                        .weight(1f)
                        .clipToBounds(),
                ) {
                    Row(
                        Modifier
                            .width(zellenBreite)
                            .graphicsLayer { translationX = -horizontal.value.toFloat() },
                    ) {
                        spalten.forEach { spalte -> StatistikScrollSpaltenKopf(spalte) }
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
                Row(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .width(pinBreite)
                            .fillMaxHeight()
                            .background(flaeche)
                            .clipToBounds()
                            .graphicsLayer { translationY = -vertikal.value.toFloat() },
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            zeilen.forEach { z ->
                                Box(
                                    Modifier
                                        .width(pinBreite)
                                        .height(StatistikScrollZeilenHoehe),
                                    contentAlignment = Alignment.CenterStart,
                                ) { z.pin() }
                            }
                        }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(vertikal)
                                .horizontalScroll(horizontal),
                        ) {
                            zeilen.forEach { z ->
                                Row(
                                    Modifier
                                        .width(zellenBreite)
                                        .height(StatistikScrollZeilenHoehe),
                                ) { z.zellen() }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatistikScrollPinKopf(kopf: String, breite: Dp) {
    Box(
        Modifier
            .width(breite)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            kopf,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatistikScrollSpaltenKopf(spalte: StatistikFixSpalte) {
    Box(
        Modifier
            .width(spalte.breite)
            .padding(horizontal = 6.dp),
        contentAlignment = when (spalte.ausrichtung) {
            TextAlign.End -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        },
    ) {
        Text(
            spalte.kopf,
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