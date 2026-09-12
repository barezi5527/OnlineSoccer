package com.onlinesoccer.app.feature.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.onlinesoccer.app.data.model.AktionFeld
import com.onlinesoccer.app.data.model.AktionFeldTyp
import com.onlinesoccer.app.data.model.AktionForm
import com.onlinesoccer.app.data.model.AktionZeile
import com.onlinesoccer.app.data.model.UebersichtZeile

/** Eine für eine Karte verfügbare Aktion (über den Zeilen-Schlüssel verknüpft). */
data class ZeilenAktion(
    val titel: String,
    val form: AktionForm,
    /** Die zugehörige Formular-Zeile, falls vorhanden (z. B. Training/Verträge). */
    val aktionZeile: AktionZeile? = null,
)

/**
 * Findet für eine Karte alle passenden Aktionen. Matching-Regeln:
 *  - Formular-Zeile mit gleichem `schluessel` (Jugendscouting, Training, Verträge)
 *  - Verstecktes `VERSTECKT`-Feld mit gleichem Wert (Trainer: `trainer=nr`)
 *  - Radio-Option mit gleichem Wert (Jugend: `ziehmich`)
 */
fun zeilenAktionen(
    zeile: UebersichtZeile,
    aktionen: List<AktionForm>,
): List<ZeilenAktion> {
    val schluessel = zeile.aktionSchluessel ?: return emptyList()
    val titel = zeile.aktionTitel

    return aktionen.mapNotNull { form ->
        val zeileImFormular = form.zeilen.firstOrNull { it.schluessel == schluessel }
        if (zeileImFormular != null) {
            return@mapNotNull ZeilenAktion(
                titel = zeileImFormular.button?.text ?: titel ?: form.titel ?: "Aktion",
                form = form,
                aktionZeile = zeileImFormular,
            )
        }
        val versteckt = form.felder.firstOrNull { it.typ == AktionFeldTyp.VERSTECKT }
        if (versteckt != null && versteckt.standard == schluessel) {
            return@mapNotNull ZeilenAktion(
                titel = titel ?: form.titel ?: "Aktion",
                form = form,
            )
        }
        val radio = form.felder.firstOrNull { it.typ == AktionFeldTyp.RADIO }
        if (radio != null && radio.optionen.any { it.wert == schluessel }) {
            return@mapNotNull ZeilenAktion(
                titel = titel ?: form.titel ?: "Aktion",
                form = form,
            )
        }
        null
    }
}

/**
 * Karte einer Übersichtszeile. Zeigt rechts ein ⋮-Menü, sobald für die Zeile
 * Aktionen verfügbar sind; die Auswahl öffnet den Bestätigungs-/Formulardialog.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AktionsFaehigeKarte(
    zeile: UebersichtZeile,
    verfuegbareAktionen: List<ZeilenAktion>,
    onSende: (String, List<Pair<String, String>>) -> Unit,
    onFormularOeffnen: (String) -> Unit,
) {
    var menuOffen by remember { mutableStateOf(false) }
    var gewaehlteAktion by remember { mutableStateOf<ZeilenAktion?>(null) }

    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    zeile.ueberschrift,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                zeile.untertitel?.takeIf { it.isNotBlank() }?.let { untertitel ->
                    Text(
                        untertitel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (zeile.werte.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        zeile.werte.forEach { (label, wert) ->
                            Column(
                                Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(6.dp),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    wert,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            if (verfuegbareAktionen.isNotEmpty()) {
                Box {
                    IconButton(onClick = { menuOffen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Aktionen")
                    }
                    DropdownMenu(expanded = menuOffen, onDismissRequest = { menuOffen = false }) {
                        verfuegbareAktionen.forEach { aktion ->
                            DropdownMenuItem(
                                text = { Text(aktion.titel) },
                                onClick = {
                                    menuOffen = false
                                    if (aktion.aktionZeile?.ziel != null) {
                                        onFormularOeffnen(aktion.aktionZeile.ziel)
                                    } else {
                                        gewaehlteAktion = aktion
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    gewaehlteAktion?.let { aktion ->
        ZeilenAktionDialog(
            aktion = aktion,
            zeilenName = zeile.ueberschrift,
            zeilenSchluessel = zeile.aktionSchluessel,
            onDismiss = { gewaehlteAktion = null },
            onSende = onSende,
        )
    }
}

/** Dialog mit den Feldern einer Zeilen-Aktion plus Bestätigen/Abbrechen. */
@Composable
private fun ZeilenAktionDialog(
    aktion: ZeilenAktion,
    zeilenName: String,
    zeilenSchluessel: String?,
    onDismiss: () -> Unit,
    onSende: (String, List<Pair<String, String>>) -> Unit,
) {
    val form = aktion.form
    val zeilenFelder = aktion.aktionZeile?.felder.orEmpty()
    val felder: List<AktionFeld> = remember(form, zeilenFelder) {
        if (aktion.aktionZeile != null) {
            form.felder.filter { it.typ == AktionFeldTyp.VERSTECKT } + zeilenFelder
        } else {
            form.felder
        }
    }

    val werte = remember(felder) {
        mutableStateMapOf<String, String>().apply {
            felder.forEach { if (it.name.isNotBlank()) put(it.name, it.standard) }
        }
    }

    // Jugendszenario: globales Radio (ziehmich) wird durch die angetippte Karte belegt.
    val globalesRadio = felder.singleOrNull { it.typ == AktionFeldTyp.RADIO && it.name.isNotBlank() }
    if (aktion.aktionZeile == null && globalesRadio != null && zeilenSchluessel != null) {
        globalesRadio.optionen.firstOrNull { it.wert == zeilenSchluessel }
            ?.let { werte[globalesRadio.name] = it.wert }
    }
    val nurBestaetigung = globalesRadio != null && felder.size == 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(aktion.titel, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (nurBestaetigung) {
                    Text("Aktion für „$zeilenName“ wirklich ausführen?")
                } else {
                    felder.forEach { feld ->
                        when (feld.typ) {
                            AktionFeldTyp.VERSTECKT -> Unit
                            else -> FeldControl(feld, werte[feld.name] ?: "", onChange = { werte[feld.name] = it })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
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
                            onDismiss()
                            onSende(form.ziel, feldListe)
                        },
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(button.text)
                    }
                }
            }
        },
    )
}