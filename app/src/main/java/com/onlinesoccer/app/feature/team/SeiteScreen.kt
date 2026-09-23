package com.onlinesoccer.app.feature.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.AktionForm
import com.onlinesoccer.app.data.model.AktionFeld
import com.onlinesoccer.app.data.model.AktionFeldTyp
import com.onlinesoccer.app.data.model.SeitenAnsicht
import com.onlinesoccer.app.data.model.UebersichtAbschnitt
import com.onlinesoccer.app.data.model.UebersichtZeile
import com.onlinesoccer.app.data.model.WertTabelle

/** Anzeige einer beliebigen Team-Seite (Jugendteam, Training, Stadion, …) samt Aktionsformularen. */
@Composable
fun SeiteScreen(
    onClose: () -> Unit,
    viewModel: SeiteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
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

            uiState.seite != null -> SeitenAnsicht(
                ansicht = uiState.seite!!,
                onClose = onClose,
                aktioFehler = uiState.aktioFehler,
                onSende = viewModel::sendeAktion,
                onFormularOeffnen = viewModel::ladeFormular,
            )
        }

        if (uiState.sendend) {
            LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }

    uiState.dialogFormular?.let { form ->
        Dialog(onDismissRequest = viewModel::schliesseDialog) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        form.titel ?: "Formular",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    AktionFormView(
                        form = form,
                        onSende = viewModel::sendeAktion,
                        onFormularOeffnen = viewModel::ladeFormular,
                    )
                    OutlinedButton(onClick = viewModel::schliesseDialog) {
                        Text("Abbrechen")
                    }
                }
            }
        }
    }
}

@Composable
private fun SeitenAnsicht(
    ansicht: SeitenAnsicht,
    onClose: () -> Unit,
    aktioFehler: String?,
    onSende: (String, List<Pair<String, String>>) -> Unit,
    onFormularOeffnen: (String) -> Unit,
) {
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
                Text(
                    ansicht.titel ?: "Seite",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (aktioFehler != null) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        aktioFehler,
                        Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        if (ansicht.abschnitte.isNotEmpty()) {
            items(ansicht.abschnitte) { abschnitt ->
                AbschnittView(abschnitt, onSende, onFormularOeffnen)
            }
        } else {
            if (ansicht.absaetze.isEmpty() && ansicht.tabellen.isEmpty()) {
                item {
                    Text(
                        "Kein Inhalt gefunden.",
                        Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ansicht.absaetze.forEach { text ->
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Text(text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            items(ansicht.tabellen) { tabelle ->
                GenerischeTabelle(tabelle)
            }
        }
    }
}

@Composable
private fun AbschnittView(
    abschnitt: UebersichtAbschnitt,
    onSende: (String, List<Pair<String, String>>) -> Unit,
    onFormularOeffnen: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        abschnitt.titel?.let { titel ->
            Text(
                titel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        abschnitt.stadionPlan?.let { plan -> StadionPlanView(plan) }

        if (abschnitt.stadionPlan != null) {
            // Stadionseite: Gesamtdaten werden übersichtlich in Gruppen dargestellt.
            StadionDatenKarte(abschnitt.infoZeilen)
        } else if (abschnitt.infoZeilen.isNotEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    abschnitt.infoZeilen.forEach { (label, wert) ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(0.5f),
                            )
                            Text(
                                wert,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.5f),
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                }
            }
        }

        if (abschnitt.punkte.isNotEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    abschnitt.punkte.forEach { punkt ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Text("• ", style = MaterialTheme.typography.bodySmall)
                            Text(
                                punkt,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        val zeilenFormen = abschnitt.zeilen.flatMap { zeile ->
            zeilenAktionen(zeile, abschnitt.aktionen).mapNotNull { it.form }
        }.toSet()

        abschnitt.zeilen.forEach { zeile ->
            AktionsFaehigeKarte(
                zeile = zeile,
                verfuegbareAktionen = zeilenAktionen(zeile, abschnitt.aktionen),
                onSende = onSende,
                onFormularOeffnen = onFormularOeffnen,
            )
        }

        abschnitt.aktionen.filterNot { it in zeilenFormen }.forEach { form ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    form.titel?.let { titel ->
                        Text(
                            titel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    AktionFormView(form, onSende, onFormularOeffnen)
                }
            }
        }
    }
}

/** Gruppiert die Stadion-Gesamtdaten in die Bereiche STADION / AUSSTATTUNG / FINANZEN. */
private val stadionGruppenBestellung = listOf(
    "STADION",
    "AUSSTATTUNG",
    "FINANZEN",
)

private fun stadionZeilenGruppen(infoZeilen: List<Pair<String, String>>): List<Pair<String, List<Pair<String, String>>>> {
    val gruppen = linkedMapOf<String, MutableList<Pair<String, String>>>(
        "STADION" to mutableListOf(),
        "AUSSTATTUNG" to mutableListOf(),
        "FINANZEN" to mutableListOf(),
    )
    val zuordnung = mapOf(
        "Fassungsvermögen" to "STADION",
        "Sitzplätze" to "STADION",
        "Stehplätze" to "STADION",
        "davon überdacht (Sitz)" to "STADION",
        "davon überdacht (Steh)" to "STADION",
        "Anzeigetafel" to "AUSSTATTUNG",
        "Rasenheizung" to "AUSSTATTUNG",
        "Kontostand" to "FINANZEN",
    )
    infoZeilen.forEach { (label, wert) ->
        // Unbekannte Labels gehen in die Stadion-Gruppe, damit nichts verloren geht.
        gruppen[zuordnung[label] ?: "STADION"]?.add(label to wert)
    }
    return stadionGruppenBestellung.mapNotNull { name ->
        val zeilen = gruppen[name].orEmpty()
        if (zeilen.isEmpty()) null else name to zeilen
    }
}

/** Übersichtliche Gruppierung der Stadion-Gesamtdaten unterhalb des Stadionplans. */
@Composable
private fun StadionDatenKarte(infoZeilen: List<Pair<String, String>>) {
    if (infoZeilen.isEmpty()) return
    val gruppen = remember(infoZeilen) { stadionZeilenGruppen(infoZeilen) }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            gruppen.forEach { (gruppenTitel, zeilen) ->
                Text(
                    gruppenTitel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                zeilen.forEach { (label, wert) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.5f),
                        )
                        Text(
                            wert,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(0.5f),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AktionFormView(
    form: AktionForm,
    onSende: (String, List<Pair<String, String>>) -> Unit,
    onFormularOeffnen: (String) -> Unit,
) {
    val felder: List<AktionFeld> = remember(form) { form.felder + form.zeilen.flatMap { it.felder } }
    val werte = remember(form) {
        mutableStateMapOf<String, String>().apply {
            felder.forEach { if (it.name.isNotBlank()) put(it.name, it.standard) }
        }
    }
    var zuSendendes by remember(form) {
        mutableStateOf<Pair<com.onlinesoccer.app.data.model.AktionsButton, List<Pair<String, String>>>?>(null)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        form.felder.forEach { feld ->
            FeldControl(feld, werte[feld.name] ?: "", onChange = { werte[feld.name] = it })
        }

        form.zeilen.filter { it.ziel == null }.filter { it.felder.isNotEmpty() }.forEach { zeile ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        zeile.bezeichnung,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    zeile.felder.forEach { feld ->
                        FeldControl(feld, werte[feld.name] ?: "", onChange = { werte[feld.name] = it })
                    }
                }
            }
        }

        form.zeilen.filter { it.ziel != null }.forEach { zeile ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    zeile.bezeichnung,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { onFormularOeffnen(zeile.ziel!!) }) {
                    Text(zeile.button?.text ?: "Öffnen")
                }
            }
        }

        val globalRadioOffen = form.felder.any { it.typ == AktionFeldTyp.RADIO && (werte[it.name] ?: "").isBlank() }

        form.buttons.forEach { button ->
            Button(
                onClick = {
                    val feldListe = buildList {
                        felder.forEach { feld ->
                            when (feld.typ) {
                                AktionFeldTyp.VERSTECKT -> add(feld.name to feld.standard)
                                else -> (werte[feld.name] ?: "").takeIf { it.isNotBlank() }
                                    ?.let { add(feld.name to it) }
                            }
                        }
                        if (!button.name.isNullOrBlank()) add(button.name to button.text)
                    }
                    if (button.bestaetigung) {
                        zuSendendes = button to feldListe
                    } else {
                        onSende(form.ziel, feldListe)
                    }
                },
                enabled = !globalRadioOffen,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(button.text)
            }
        }
    }

    zuSendendes?.let { (button, feldListe) ->
        AlertDialog(
            onDismissRequest = { zuSendendes = null },
            title = { Text("Aktion ausführen?") },
            text = { Text("Soll „${button.text}“ wirklich ausgeführt werden?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        zuSendendes = null
                        onSende(form.ziel, feldListe)
                    },
                ) {
                    Text("Ausführen")
                }
            },
            dismissButton = {
                TextButton(onClick = { zuSendendes = null }) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

@Composable
internal fun FeldControl(
    feld: AktionFeld,
    wert: String,
    onChange: (String) -> Unit,
) {
    when (feld.typ) {
        AktionFeldTyp.VERSTECKT -> return
        AktionFeldTyp.RADIO -> RadioGruppe(feld, wert, onChange)
        AktionFeldTyp.AUSWAHL -> AuswahlFeld(feld, wert, onChange)
        AktionFeldTyp.NUMMER,
        AktionFeldTyp.TEXT,
        -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    feld.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = wert,
                    onValueChange = onChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (feld.typ == AktionFeldTyp.NUMMER) KeyboardType.Number else KeyboardType.Text,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RadioGruppe(
    feld: AktionFeld,
    wert: String,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            feld.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            feld.optionen.forEach { option ->
                FilterChip(
                    selected = wert == option.wert,
                    onClick = { onChange(if (wert == option.wert) "" else option.wert) },
                    label = { Text(option.label) },
                )
            }
        }
    }
}

@Composable
internal fun AuswahlFeld(
    feld: AktionFeld,
    wert: String,
    onChange: (String) -> Unit,
) {
    var offen by remember(feld) { mutableStateOf(false) }
    val label = feld.optionen.firstOrNull { it.wert == wert }?.label ?: wert
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            feld.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            OutlinedButton(
                onClick = { offen = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(label.ifBlank { "—"}, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
                feld.optionen.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onChange(option.wert)
                            offen = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun GenerischeTabelle(tabelle: WertTabelle) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            if (tabelle.header.isNotEmpty()) {
                Row(Modifier.fillMaxWidth()) {
                    tabelle.header.forEach { kopf ->
                        Text(
                            kopf,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            tabelle.zeilen.forEach { zeile ->
                Row(Modifier.fillMaxWidth()) {
                    zeile.forEach { zelle ->
                        Text(
                            zelle,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}