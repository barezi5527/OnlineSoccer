package com.onlinesoccer.app.feature.zat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlinesoccer.app.data.model.AuswahlOption
import com.onlinesoccer.app.data.model.ZugabgabeElementTyp
import com.onlinesoccer.app.data.model.ZugabgabeFormular

/**
 * Gemeinsamer Editor für „Aktionen“ (p=1) und „Einstellungen“ (p=2) der klassischen
 * Zugabgabe. Bildet die Website-Formulare nach: neue Aktion/Einstellung wählen,
 * Felder füllen und serverseitig „anlegen“; bestehende Elemente markieren + löschen.
 */
@Composable
fun ZatElementeEditor(
    uiState: ZatEditorUiState,
    typen: List<ZugabgabeElementTyp>,
    titel: String,
    onLade: () -> Unit,
    onTyp: (ZugabgabeElementTyp) -> Unit,
    onWert: (String, String) -> Unit,
    onAnlegen: () -> Unit,
    onToggleAuswahl: (String) -> Unit,
    onLoeschen: () -> Unit,
) {
    when {
        uiState.ladend && uiState.kopfinfo == null && uiState.elemente.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        else -> Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.fehler?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            NeueElementKarte(
                uiState = uiState,
                typen = typen,
                titel = titel,
                onTyp = onTyp,
                onWert = onWert,
                onAnlegen = onAnlegen,
                onLade = onLade,
            )

            uiState.meldung?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            VorhandeneKarte(
                uiState = uiState,
                titel = titel,
                onToggleAuswahl = onToggleAuswahl,
                onLoeschen = onLoeschen,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NeueElementKarte(
    uiState: ZatEditorUiState,
    typen: List<ZugabgabeElementTyp>,
    titel: String,
    onTyp: (ZugabgabeElementTyp) -> Unit,
    onWert: (String, String) -> Unit,
    onAnlegen: () -> Unit,
    onLade: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Neue $titel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            TypDropdown(label = "$titel auswählen", typen = typen, gewaehlt = uiState.typ, onTyp = onTyp)

            uiState.typ?.let { typ ->
                val formular = uiState.formular
                if (formular == null) {
                    if (uiState.ladend) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.padding(start = 8.dp))
                            Text("Formular wird geladen…", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Formular fehlgeschlagen.", color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onLade) { Text("Erneut") }
                        }
                    }
                } else {
                    FormularFelder(formular = formular, werte = uiState.werte, onWert = onWert)
                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = onAnlegen,
                        enabled = !uiState.speichernd,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uiState.speichernd) {
                            CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(formular.saveButton.trim().ifEmpty { if (typ.istEinstellung) "Einstellung festlegen" else "Aktion anlegen" })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypDropdown(
    label: String,
    typen: List<ZugabgabeElementTyp>,
    gewaehlt: ZugabgabeElementTyp?,
    onTyp: (ZugabgabeElementTyp) -> Unit,
) {
    var offen by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = offen, onExpandedChange = { offen = it }) {
        OutlinedTextField(
            value = gewaehlt?.label ?: "– wählen –",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = offen) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            typen.forEach { typ ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(typ.label) },
                    onClick = {
                        offen = false
                        onTyp(typ)
                    },
                )
            }
        }
    }
}

@Composable
private fun FormularFelder(
    formular: ZugabgabeFormular,
    werte: Map<String, String>,
    onWert: (String, String) -> Unit,
) {
    val zeilen = formular.positionsZeilen
    val spalten = formular.positionsSpalten
    when (formular.typ) {
        ZugabgabeElementTyp.EINWECHSLUNG -> {
            WerteDropdown("Einwechselspieler", formular.spieler, werte["zao_einspieler"], onWert) { "zao_einspieler" }
            WerteDropdown("Auswechselspieler", formular.spieler, werte["zao_spieler"], onWert) { "zao_spieler" }
        }
        ZugabgabeElementTyp.POSITIONSWECHSEL,
        ZugabgabeElementTyp.MANNDECKUNG,
        -> WerteDropdown("Spieler", formular.spieler, werte["zao_spieler"], onWert) { "zao_spieler" }
        else -> Unit
    }
    if (formular.typ.istSpielerAuswahl) {
        WerteDropdown(formular.typ.label, formular.spieler, werte["spieler_id"], onWert) { "spieler_id" }
    }
    if (formular.typ == ZugabgabeElementTyp.MANNDECKUNG) {
        WerteDropdown("Gegenspieler", formular.gegenspieler, werte["P1"], onWert) { "P1" }
    }
    if (!formular.typ.istSpielerAuswahl) {
        MinutenSlider(minuten = formular.minuten, wert = werte["zao_minute"], onWert = onWert)
        WerteDropdown("Abhängigkeit", formular.abhaengigkeiten, werte["zao_abhaengigkeit"], onWert) { "zao_abhaengigkeit" }
    }
    if (formular.typ.brauchtPosition) {
        WerteDropdown("Zeile", zeilen, werte["P1"], onWert) { "P1" }
        WerteDropdown("Spalte", spalten, werte["P2"], onWert) { "P2" }
        if (formular.positionsSonder.isNotEmpty()) {
            WerteDropdown("Sonder", formular.positionsSonder, werte["P3"], onWert) { "P3" }
        }
    }
    if (formular.werte.isNotEmpty()) {
        WerteDropdown(formular.typ.label, formular.werte, werte["P1"], onWert) { "P1" }
    }
}

@Composable
private fun MinutenSlider(
    minuten: List<AuswahlOption>,
    wert: String?,
    onWert: (String, String) -> Unit,
) {
    val ids = minuten.mapNotNull { it.id.toIntOrNull() }
    val max = ids.maxOrNull() ?: 90
    val min = ids.minOrNull() ?: 1
    val aktuell = wert?.toFloatOrNull()?.coerceIn(min.toFloat(), max.toFloat()) ?: min.toFloat()
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Spielminute", style = MaterialTheme.typography.labelLarge)
            Text("${aktuell.toInt()}.", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = aktuell,
            onValueChange = { onWert("zao_minute", it.toInt().toString()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min - 1).coerceAtLeast(0),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WerteDropdown(
    label: String,
    optionen: List<AuswahlOption>,
    wert: String?,
    onWert: (String, String) -> Unit,
    feldname: () -> String,
) {
    var offen by remember { mutableStateOf(false) }
    val gewaehlt = optionen.firstOrNull { it.id == wert }
    ExposedDropdownMenuBox(expanded = offen, onExpandedChange = { offen = it }) {
        OutlinedTextField(
            value = gewaehlt?.label ?: if (wert.isNullOrBlank()) "– wählen –" else wert,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = offen) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            optionen.forEach { opt ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(opt.label) },
                    onClick = {
                        offen = false
                        onWert(feldname(), opt.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun VorhandeneKarte(
    uiState: ZatEditorUiState,
    titel: String,
    onToggleAuswahl: (String) -> Unit,
    onLoeschen: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Vorhandene $titel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (uiState.elemente.isEmpty()) {
                Text(
                    "Noch keine $titel angelegt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.elemente.forEach { eintrag ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = eintrag.relaId in uiState.auswahl,
                            onCheckedChange = { onToggleAuswahl(eintrag.relaId) },
                        )
                        Text(eintrag.text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (uiState.auswahl.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = onLoeschen,
                        enabled = !uiState.speichernd,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(uiState.loeschLabel?.trim()?.ifEmpty { "Markierte löschen" } ?: "Markierte löschen")
                    }
                }
            }
        }
    }
}