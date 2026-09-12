package com.onlinesoccer.app.feature.taktik

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlinesoccer.app.data.model.AuswahlOption

/** Zeilen des Taktik-Editors (O oben/Sturm bis A unten/Abwehr), Spalten 1–11 wie auf der OS-Seite. */
private val TAKTIK_ZEILEN = listOf('O', 'N', 'M', 'L', 'K', 'J', 'I', 'H', 'G', 'F', 'E', 'D', 'C', 'B', 'A')
private val TAKTIK_SPALTEN = (1..11).toList()

/** Positionsbuchstaben wie auf der OS-Seite (A–H, K, L – I und J werden übersprungen). */
private val TAKTIK_BUCHSTABEN = listOf('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'K', 'L')

/** Vergibt die Buchstaben wie die JS-Logik: Lesereihenfolge oben→unten, links→rechts. */
internal fun taktikBuchstaben(codes: Set<String>): Map<String, String> {
    val ergebnis = mutableMapOf<String, String>()
    var i = 0
    for (zeile in TAKTIK_ZEILEN) {
        for (spalte in 1..11) {
            val code = "$zeile$spalte"
            if (code in codes && i < TAKTIK_BUCHSTABEN.size) {
                ergebnis[code] = TAKTIK_BUCHSTABEN[i].toString()
                i++
            }
        }
    }
    return ergebnis
}

private fun zeilenFarbe(index: Int): Color = when {
    index < 5 -> Color(0xFFCC0033)
    index < 10 -> Color(0xFF3377FF)
    else -> Color(0xFF009933)
}

/**
 * Taktik-Editor: Raster (O..A × 1..11) mit antippbaren Feldern, Positionsbuchstaben,
 * fixer Torwart-Zelle, Laden/Löschen gespeicherter Taktiken und Speichern.
 */
@Composable
fun TaktikEditor(
    uiState: TaktikUiState,
    onToggle: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onSpeichern: () -> Unit,
    onLeeren: () -> Unit,
    onWaehleStandard: (String) -> Unit,
    onWaehleEigene: (String) -> Unit,
    onLaden: () -> Unit,
    onLoeschen: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        when {
            uiState.ladende -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.fehler != null && uiState.taktik.istLeer -> Text(
                uiState.fehler.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )

            else -> {
                TaktikVerwaltung(
                    standardTaktiken = uiState.taktik.standardTaktiken,
                    eigeneTaktiken = uiState.taktik.eigeneTaktiken,
                    gewaehlteStandardId = uiState.gewaehlteStandardId,
                    gewaehlteEigeneId = uiState.gewaehlteEigeneId,
                    ladeAktion = uiState.ladeAktion,
                    loeschende = uiState.loeschende,
                    onWaehleStandard = onWaehleStandard,
                    onWaehleEigene = onWaehleEigene,
                    onLaden = onLaden,
                    onLoeschen = onLoeschen,
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${uiState.taktik.codes.size} Positionen belegt · Torwart (T) fest",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    TextButton(onClick = onLeeren, enabled = uiState.taktik.codes.isNotEmpty()) {
                        Text("Leeren")
                    }
                }
                TaktikRaster(
                    codes = uiState.taktik.codes,
                    onToggle = onToggle,
                )
                uiState.fehler?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = { Text("Taktik-Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Button(
                    onClick = onSpeichern,
                    enabled = !uiState.speichernd,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
                ) {
                    if (uiState.speichernd) {
                        CircularProgressIndicator(Modifier.width(22.dp).height(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Taktik speichern")
                    }
                }
                uiState.meldung?.let {
                    Text(
                        it,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TaktikVerwaltung(
    standardTaktiken: List<AuswahlOption>,
    eigeneTaktiken: List<AuswahlOption>,
    gewaehlteStandardId: String,
    gewaehlteEigeneId: String,
    ladeAktion: Boolean,
    loeschende: Boolean,
    onWaehleStandard: (String) -> Unit,
    onWaehleEigene: (String) -> Unit,
    onLaden: () -> Unit,
    onLoeschen: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Gespeicherte Taktiken", style = MaterialTheme.typography.titleSmall)
            AuswahlFeldTaktik(
                label = "Standardtaktiken",
                optionen = standardTaktiken,
                ausgewaehlt = gewaehlteStandardId,
                onAuswaehlen = onWaehleStandard,
            )
            AuswahlFeldTaktik(
                label = "Eigene Taktiken",
                optionen = eigeneTaktiken,
                ausgewaehlt = gewaehlteEigeneId,
                onAuswaehlen = onWaehleEigene,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onLaden,
                    enabled = !ladeAktion && !loeschende &&
                        (gewaehlteStandardId.isNotEmpty() || gewaehlteEigeneId.isNotEmpty()),
                    modifier = Modifier.weight(1f),
                ) {
                    if (ladeAktion) {
                        CircularProgressIndicator(Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Laden")
                    }
                }
                OutlinedButton(
                    onClick = onLoeschen,
                    enabled = !ladeAktion && !loeschende && gewaehlteEigeneId.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    if (loeschende) {
                        CircularProgressIndicator(Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Löschen")
                    }
                }
            }
        }
    }
}

@Composable
private fun AuswahlFeldTaktik(
    label: String,
    optionen: List<AuswahlOption>,
    ausgewaehlt: String,
    onAuswaehlen: (String) -> Unit,
) {
    var offen by remember { mutableStateOf(false) }
    val gewaehlt = optionen.firstOrNull { it.id == ausgewaehlt }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(
            onClick = { offen = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(gewaehlt?.label ?: "— auswählen —", modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            optionen.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onAuswaehlen(option.id)
                        offen = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TaktikRaster(codes: Set<String>, onToggle: (String) -> Unit) {
    val buchstaben = remember(codes) { taktikBuchstaben(codes) }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        val zellenBreite: Dp = maxWidth / (TAKTIK_SPALTEN.size + 1)
        val zellenHoehe: Dp = 26.dp

        Column {
            Row(Modifier.padding(start = zellenBreite)) {
                TAKTIK_SPALTEN.forEach { s ->
                    Box(Modifier.width(zellenBreite).height(zellenHoehe), contentAlignment = Alignment.Center) {
                        Text("$s", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            TAKTIK_ZEILEN.forEachIndexed { index, zeile ->
                Row {
                    Box(
                        Modifier
                            .width(zellenBreite)
                            .height(zellenHoehe)
                            .background(zeilenFarbe(index), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$zeile", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    TAKTIK_SPALTEN.forEach { spalte ->
                        val code = "$zeile$spalte"
                        val buchstabe = buchstaben[code]
                        val gefaerbt = code in codes
                        Box(
                            Modifier
                                .width(zellenBreite)
                                .height(zellenHoehe)
                                .padding(2.dp)
                                .background(
                                    if (gefaerbt) zeilenFarbe(index) else Color(0xFF005C2B),
                                    RoundedCornerShape(4.dp),
                                )
                                .border(1.dp, Color(0xFF9CCC65), RoundedCornerShape(4.dp))
                                .clickable { onToggle(code) },
                            contentAlignment = Alignment.Center,
                        ) {
                            buchstabe?.let {
                                Text(it, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            Row {
                Box(Modifier.width(zellenBreite).height(zellenHoehe))
                TAKTIK_SPALTEN.forEach { spalte ->
                    if (spalte == 6) {
                        Box(
                            Modifier
                                .width(zellenBreite)
                                .height(zellenHoehe)
                                .padding(2.dp)
                                .background(Color(0xFFFFFF00), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("T", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    } else {
                        Spacer(Modifier.width(zellenBreite))
                    }
                }
            }
        }
    }
}