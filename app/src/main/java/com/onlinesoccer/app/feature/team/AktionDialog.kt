package com.onlinesoccer.app.feature.team

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.onlinesoccer.app.data.model.AktionsButton
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

/** Erkennt das Trainer-Einstellformular auch bei relativen URLs/abweichendem Submit-Text. */
internal fun istTrainerEinstellFormular(form: AktionForm, felder: List<AktionFeld>): Boolean {
    val pfad = form.ziel.substringBefore('?').trimEnd('/')
    val trainerZiel = pfad.endsWith("trainer.php", ignoreCase = true)
    val trainerFelder = listOf("trainer", "skill", "dauer").all { name ->
        felder.any { it.name.equals(name, ignoreCase = true) }
    }
    val einstellenButton = form.buttons.any { button ->
        button.name.equals("einstellen", ignoreCase = true) ||
            button.text.startsWith("einstellen als Trainer", ignoreCase = true)
    }
    return trainerZiel && trainerFelder && einstellenButton
}

/** Erkennt den serverseitigen Submit des Feuern-Formulars. */
internal fun istTrainerFeuerFormular(form: AktionForm, felder: List<AktionFeld>): Boolean {
    val pfad = form.ziel.substringBefore('?').trimEnd('/')
    val trainerZiel = pfad.endsWith("trainer.php", ignoreCase = true)
    val trainerFeld = felder.any { it.name.equals("trainer", ignoreCase = true) }
    val feuernButton = form.buttons.any { button ->
        button.name.equals("feuern", ignoreCase = true) ||
            button.text.contains("Trainer", ignoreCase = true) && button.text.contains("feuern", ignoreCase = true)
    }
    return trainerZiel && trainerFeld && feuernButton
}

/**
 * Satz für die Schlussbestätigung einer Zeilen-Aktion, die nur aus einem
 * vorbelegten Radio besteht (Jugend: „ins A-Team berufen“).
 *
 * Bewusst als reine Funktion: der Text ist die eigentliche Sicherheitsaussage des
 * Dialogs („welcher Spieler wird berufen?“) und soll ohne Android testbar sein.
 * Ohne bekannten Aktionstitel bleibt der neutrale Rückfalltext.
 */
internal fun zeilenBestaetigungstext(zeilenName: String, aktionsTitel: String?): String {
    val name = zeilenName.trim()
    return if (aktionsTitel.isNullOrBlank()) {
        "Aktion für „$name“ wirklich ausführen?"
    } else {
        "„$name“ wirklich ${aktionsTitel.trim().replaceFirstChar { it.lowercase() }}?"
    }
}

/**
 * Baut die Feldliste für den POST aus den Formularfeldern, den aktuell gewählten
 * Werten und dem gedrückten Button.
 *
 * Bewusst als reine Funktion: das ist die sicherheitskritische Stelle des Dialogs,
 * denn hier steht, **was** tatsächlich an den Server geht. Zwei Regeln, beide aus
 * der Website abgeleitet:
 *  - **Leere Werte werden nicht mitgesendet.** Ein unberührtes Auswahlfeld darf
 *    keinen Parameter mit leerem Wert schicken.
 *  - **Ein Button ohne `name` trägt nichts bei** (`ju.php?page=4`: die Absende-
 *    Buttons haben keinen Namen). Sonst schickte die App einen Parameter, den das
 *    Formular gar nicht kennt.
 *
 * Ohne Android testbar – der Test prüft den echten POST-Body gegen einen
 * aufzeichnenden OkHttp-Interceptor.
 */
internal fun formularFelder(
    felder: List<AktionFeld>,
    werte: Map<String, String>,
    button: AktionsButton,
): List<Pair<String, String>> = buildList {
    felder.forEach { feld ->
        when (feld.typ) {
            AktionFeldTyp.VERSTECKT -> add(feld.name to feld.standard)
            else -> (werte[feld.name] ?: "").takeIf { it.isNotBlank() }?.let { add(feld.name to it) }
        }
    }
    if (!button.name.isNullOrBlank()) add(button.name to button.text)
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
    onSpielerClick: (Long) -> Unit = {},
    trainerVorschau: TrainerAktionVorschauUi? = null,
    onTrainerVorschauLaden: (String, List<Pair<String, String>>) -> Unit = { _, _ -> },
    onTrainerVorschauZuruecksetzen: () -> Unit = {},
) {
    var menuOffen by remember { mutableStateOf(false) }
    var gewaehlteAktion by remember { mutableStateOf<ZeilenAktion?>(null) }

    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            val spielerPid = zeile.spielerPid ?: zeile.aktionSchluessel?.toLongOrNull()
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
                    .then(
                        if (spielerPid != null) Modifier.clickable { onSpielerClick(spielerPid) } else Modifier,
                    ),
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
                                    onTrainerVorschauZuruecksetzen()
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
            zeilenAktionTitel = zeile.aktionTitel,
            trainerVorschau = trainerVorschau,
            onTrainerVorschauLaden = onTrainerVorschauLaden,
            onTrainerVorschauZuruecksetzen = onTrainerVorschauZuruecksetzen,
            onDismiss = {
                gewaehlteAktion = null
                onTrainerVorschauZuruecksetzen()
            },
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
    zeilenAktionTitel: String?,
    trainerVorschau: TrainerAktionVorschauUi?,
    onTrainerVorschauLaden: (String, List<Pair<String, String>>) -> Unit,
    onTrainerVorschauZuruecksetzen: () -> Unit,
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
    val jugendBerufungsStart = form.ziel.substringBefore('?').endsWith("ju.php", ignoreCase = true) &&
        globalesRadio?.name == "ziehmich" && form.buttons.any { it.name == "ziehen" }
    val trainerEinstellung = istTrainerEinstellFormular(form, felder)
    val trainerFeuern = istTrainerFeuerFormular(form, felder)
    val trainerAktion = trainerEinstellung || trainerFeuern
    val trainerNr = felder.firstOrNull { it.name == "trainer" }?.standard.orEmpty()
    val submitName = form.buttons.firstOrNull()?.name.orEmpty()
    val vorschauStatus = trainerVorschau?.takeIf {
        it.trainerNr == trainerNr && it.aktionName == submitName
    }
    val serverVorschau = vorschauStatus?.vorschau

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    serverVorschau != null && trainerFeuern -> "Abfindung bestätigen"
                    serverVorschau != null && trainerEinstellung -> "Trainerkosten bestätigen"
                    else -> aktion.titel
                },
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (serverVorschau != null) {
                    Text(serverVorschau.meldung, fontWeight = FontWeight.Bold)
                    Text(
                        if (trainerFeuern) {
                            "Die Website bestätigt die Abfindung. Erst „Jetzt feuern“ entlässt den Trainer."
                        } else {
                            "Die Website bestätigt die Kosten. Erst „Jetzt einstellen“ stellt den Trainer ein."
                        },
                    )
                } else if (vorschauStatus?.ladend == true) {
                    Text("Kosten werden bei der Website abgefragt …")
                } else if (trainerAktion) {
                    vorschauStatus?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (trainerFeuern) {
                        Text("Die Website fragt die Abfindung ab, bevor der Trainer endgültig gefeuert wird.")
                    } else {
                        felder.forEach { feld ->
                            when (feld.typ) {
                                AktionFeldTyp.VERSTECKT -> Unit
                                else -> FeldControl(feld, werte[feld.name] ?: "", onChange = { werte[feld.name] = it })
                            }
                        }
                    }
                } else if (nurBestaetigung) {
                    if (jugendBerufungsStart) {
                        Text(
                            "Für „${zeilenName.trim()}“ werden zunächst die verfügbaren Positionen, " +
                                "Spielernamen und Vertragslaufzeiten vom Server geladen. Die Berufung erfolgt " +
                                "erst nach der weiteren Auswahl und Bestätigung.",
                        )
                    } else {
                        Text(zeilenBestaetigungstext(zeilenName, zeilenAktionTitel))
                    }
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
                TextButton(
                    onClick = {
                        if (serverVorschau != null || vorschauStatus?.fehler != null) {
                            onTrainerVorschauZuruecksetzen()
                        } else if (trainerAktion && vorschauStatus?.ladend == true) {
                            onDismiss()
                        } else {
                            onDismiss()
                        }
                    },
                ) {
                    Text(if (serverVorschau != null || vorschauStatus?.fehler != null) "Zurück" else "Abbrechen")
                }
                form.buttons.forEach { button ->
                    Button(
                        onClick = {
                            if (trainerAktion && serverVorschau != null) {
                                onDismiss()
                                onSende(form.ziel, serverVorschau.felder)
                            } else if (trainerAktion) {
                                onTrainerVorschauLaden(form.ziel, formularFelder(felder, werte, button))
                            } else {
                                val feldListe = formularFelder(felder, werte, button)
                                onDismiss()
                                onSende(form.ziel, feldListe)
                            }
                        },
                        enabled = vorschauStatus?.ladend != true,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(
                            when {
                                serverVorschau != null && trainerFeuern -> "Jetzt feuern"
                                serverVorschau != null && trainerEinstellung -> "Jetzt einstellen"
                                trainerAktion && vorschauStatus?.ladend == true -> "Bitte warten …"
                                trainerFeuern -> "Abfindung anzeigen"
                                trainerEinstellung -> if (vorschauStatus?.fehler != null) "Erneut prüfen" else "Kosten anzeigen"
                                jugendBerufungsStart -> "Auswahl laden"
                                // Nur Anzeigetext: der Serverwert bleibt `button.text`.
                                // „Markierten Spieler …“ waere in einer Zeilen-Bestätigung
                                // falsch, weil genau ein Spieler berufen wird.
                                nurBestaetigung && !zeilenAktionTitel.isNullOrBlank() -> zeilenAktionTitel
                                else -> button.text
                            },
                        )
                    }
                }
            }
        },
    )
}
