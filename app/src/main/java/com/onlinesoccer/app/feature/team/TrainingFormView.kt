package com.onlinesoccer.app.feature.team

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlinesoccer.app.core.ui.theme.kastenTextFarbe
import com.onlinesoccer.app.core.ui.theme.positionVonText
import com.onlinesoccer.app.core.ui.theme.trikotFarbe
import com.onlinesoccer.app.data.model.AktionFeld
import com.onlinesoccer.app.data.model.AktionForm
import com.onlinesoccer.app.data.model.AktionZeile
import com.onlinesoccer.app.data.model.UebersichtAbschnitt
import com.onlinesoccer.app.data.model.UebersichtZeile

/**
 * Training ist das einzige Zeilenformular, bei dem der Server die **komplette**
 * Auswahl erwartet. Fehlt ein Spieler im POST, löscht die Website dessen
 * Trainingseinstellung. Deshalb wird hier immer das Formular aller Spieler
 * gesendet – nicht nur die geänderte Zeile.
 */
internal fun istTrainingFormular(form: AktionForm): Boolean =
    form.buttons.any { it.name.equals("trainingspeichern", ignoreCase = true) } &&
        form.zeilen.any { zeile ->
            zeile.felder.any { it.name.startsWith("tr1") || it.name.startsWith("tr2") }
        }

/** Baut den vollständigen POST-Body inklusive aller Spielerzeilen und des Speichern-Buttons. */
internal fun trainingFormularFelder(
    form: AktionForm,
    werte: Map<String, String>,
): List<Pair<String, String>> {
    val button = form.buttons.firstOrNull() ?: return emptyList()
    return formularFelder(
        felder = form.felder + form.zeilen.flatMap { it.felder },
        werte = werte,
        button = button,
    )
}

/**
 * Auswahlzustand des Trainingsformulars. Er liegt außerhalb der Kacheln, damit
 * die feste Speicherleiste unten denselben Zustand sieht wie die Auswahlfelder.
 */
internal class TrainingFormularZustand internal constructor(
    val form: AktionForm,
    val zeilen: List<UebersichtZeile>,
) {
    private val felder = form.felder + form.zeilen.flatMap { it.felder }

    val werte = mutableStateMapOf<String, String>().apply {
        felder.forEach { feld -> if (feld.name.isNotBlank()) put(feld.name, feld.standard) }
    }

    var bestaetigungOffen by mutableStateOf(false)

    val button = form.buttons.firstOrNull()

    val geaenderteSpieler: Int
        get() = form.zeilen.count { zeile ->
            zeile.felder.any { feld -> werte[feld.name] != feld.standard }
        }

    fun wert(feld: AktionFeld): String = werte[feld.name] ?: feld.standard

    fun setze(feld: AktionFeld, wert: String) {
        werte[feld.name] = wert
    }
}

@Composable
internal fun rememberTrainingFormularZustand(
    form: AktionForm,
    zeilen: List<UebersichtZeile>,
): TrainingFormularZustand = remember(form) {
    TrainingFormularZustand(form, zeilen)
}

/**
 * Trainingskacheln mit direkter Auswahl von Trainer und trainiertem Skill.
 *
 * Die Auswahl liegt sichtbar in der Kachel (Drilldown) statt im ⋮-Menü. Alle
 * Änderungen werden gemeinsam über [TrainingSpeicherLeiste] gespeichert, damit
 * der Server die Einstellungen der übrigen Spieler nicht als „nicht
 * ausgewählt“ verwirft.
 */
@Composable
internal fun TrainingFormView(
    zeilen: List<UebersichtZeile>,
    form: AktionForm,
    zustand: TrainingFormularZustand,
    onSpielerClick: (Long) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        form.hinweise.forEach { hinweis ->
            Text(hinweis, style = MaterialTheme.typography.bodySmall)
        }

        form.zeilen.forEach { aktionZeile ->
            val anzeige = zeilen.firstOrNull { it.aktionSchluessel == aktionZeile.schluessel }
            TrainingSpielerKarte(
                zeile = anzeige,
                aktionZeile = aktionZeile,
                zustand = zustand,
                onSpielerClick = onSpielerClick,
            )
        }
    }
}

/**
 * Fixierte Kopfleiste der Trainingsseite: zeigt die Auslastung aller Trainer
 * kompakt als Chips und blendet eine Warnung ein, sobald ein Trainer mehr als
 * fünf Spieler betreut (die überzähligen werden vom Server nicht trainiert).
 */
@Composable
internal fun TrainerBetreuungLeiste(
    abschnitt: UebersichtAbschnitt,
    modifier: Modifier = Modifier,
) {
    val warnung = abschnitt.punkte.firstOrNull()
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                abschnitt.infoZeilen.forEach { (name, anzahl) ->
                    val uebervoll = (anzahl.toIntOrNull() ?: 0) > 5
                    val kuerzel = name
                        .removePrefix("Trainer ")
                        .removePrefix("Trainer:")
                        .trim()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (uebervoll) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        Text(
                            "T$kuerzel · $anzahl",
                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (uebervoll) FontWeight.Bold else FontWeight.Normal,
                            color = if (uebervoll) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
            if (warnung != null) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        warnung,
                        Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

/** Fest sichtbare Speicherleiste unterhalb der Trainingsliste. */
@Composable
internal fun TrainingSpeicherLeiste(
    zustand: TrainingFormularZustand,
    onSende: (String, List<Pair<String, String>>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val button = zustand.button ?: return
    val geaendert = zustand.geaenderteSpieler

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (geaendert > 0) {
                    "$geaendert von ${zustand.form.zeilen.size} Spielern geändert"
                } else {
                    "Noch keine Änderungen"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (geaendert > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Button(
                onClick = { zustand.bestaetigungOffen = true },
                enabled = geaendert > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(button.text.trim())
            }
        }
    }

    if (zustand.bestaetigungOffen) {
        AlertDialog(
            onDismissRequest = { zustand.bestaetigungOffen = false },
            title = { Text("Trainingseinstellungen speichern?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Die Auswahl aller ${zustand.form.zeilen.size} Spieler wird an den " +
                            "Server gesendet.",
                    )
                    Text(
                        if (geaendert == 1) {
                            "1 Spieler wurde geändert."
                        } else {
                            "$geaendert Spieler wurden geändert."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        zustand.bestaetigungOffen = false
                        onSende(
                            zustand.form.ziel,
                            trainingFormularFelder(zustand.form, zustand.werte),
                        )
                    },
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { zustand.bestaetigungOffen = false }) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

@Composable
private fun TrainingSpielerKarte(
    zeile: UebersichtZeile?,
    aktionZeile: AktionZeile,
    zustand: TrainingFormularZustand,
    onSpielerClick: (Long) -> Unit,
) {
    val farbe = kartenFarbe(zeile)
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val spielerPid = zeile?.spielerPid
            Column(
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (spielerPid != null) Modifier.clickable { onSpielerClick(spielerPid) } else Modifier,
                    ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val kopfModifier = if (farbe != null) {
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(farbe)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                } else {
                    Modifier.fillMaxWidth()
                }
                Row(
                    kopfModifier,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        zeile?.ueberschrift ?: aktionZeile.bezeichnung,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = farbe?.let(::kastenTextFarbe)
                            ?: MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    zeile?.werte?.firstOrNull { it.first == "Chance" }?.second
                        ?.takeIf { it.isNotBlank() }
                        ?.let { chance ->
                            Text(
                                chance,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(6.dp),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                }
                zeile?.untertitel?.takeIf { it.isNotBlank() }?.let { untertitel ->
                    Text(
                        untertitel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                aktionZeile.felder.forEach { feld ->
                    AuswahlFeld(
                        feld = feld,
                        wert = zustand.wert(feld),
                        onChange = { zustand.setze(feld, it) },
                        modifier = Modifier.weight(1f),
                        wertProOption = if (feld.name.startsWith("tr2")) {
                            skillWerte(zeile, feld)
                        } else {
                            emptyMap()
                        },
                    )
                }
            }
        }
    }
}

/**
 * Skillkürzel in der Spaltenreihenfolge der Einzelwerte-Seite (`showteam.php?s=2`).
 *
 * Der Server liefert für alle Positionen dieselbe Reihenfolge (Torwart hat keine
 * eigene Spaltensetzung). Reihenfolge nach echtem Dump:
 * `SCH BAK KOB ZWK DEC GES FUQ ERF AGG PAS AUS UEB WID SEL DIS ZUV EIN`.
 */
private val feldSkillKuerzel = listOf(
    "SCH", "BAK", "KOB", "ZWK", "DEC", "GES", "FUQ", "ERF", "AGG",
    "PAS", "AUS", "UEB", "WID", "SEL", "DIS", "ZUV", "EIN",
)

/**
 * Die 11 trainierbaren Skills in **Optionswert-Reihenfolge** (`tr2`-Wert 1…11).
 *
 * Der Server zeigt je Position andere Labels (Torwart `ABS STS FAN STB SPL REF`,
 * Feldspieler `SCH BAK KOB ZWK DEC GES`), ordnet sie aber auf dieselben
 * `s=2`-Spalten zu. Der Optionswert nennt daher die Position in dieser Liste,
 * die zugleich der Spaltenindex in [feldSkillKuerzel] ist.
 */
private val trainingSkillKuerzel = listOf(
    "SCH", "BAK", "KOB", "ZWK", "DEC", "GES", "AGG", "PAS", "AUS", "UEB", "ZUV",
)

const val SKILLWERT_PREFIX = "skillwert:"

/**
 * Für die Einzelwerte (`showteam.php?s=2`) werden alle Spieler in einer Session
 * vorgeladen und über `[Skillwert-Schlüssel][pid]` in den Zeilen hinterlegt.
 */
internal fun skillWertFeld(pid: String): String = "$SKILLWERT_PREFIX$pid"

/**
 * Skillwert je `tr2`-Optionswert (nur der Wert, z. B. `"45"`), sonst leer.
 *
 * Der Optionswert ist die **Position** des Skills in der `s=2`-Spaltenreihenfolge
 * (`1` → erste Spalte, `2` → zweite, …). Torwart und Feldspieler nutzen dieselben
 * Spalten, nur die vom Server gelieferten Labels unterscheiden sich
 * (`ABS/STS/…` bzw. `SCH/BAK/…`). Im Drilldown steht rechts nur der Wert; das
 * Kürzel liefert das Options-Label. Der Map-Schlüssel ist der Optionswert
 * (`"1"`, `"2"`, …): `AuswahlFeld` schlägt die Anzeige mit `option.wert` nach.
 */
internal fun skillWerte(zeile: UebersichtZeile?, feld: AktionFeld): Map<String, String> {
    val roh = zeile?.werte?.firstOrNull { it.first.startsWith(SKILLWERT_PREFIX) }?.second
        ?.takeIf { it.isNotBlank() } ?: return emptyMap()
    val werteNachKuerzel = skillWerteAusIndex(roh, feldSkillKuerzel)
    return feld.optionen.mapNotNull { option ->
        val index = option.wert.toIntOrNull() ?: return@mapNotNull null
        val kuerzel = trainingSkillKuerzel.getOrNull(index - 1) ?: return@mapNotNull null
        val wert = werteNachKuerzel[kuerzel]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        option.wert to wert
    }.toMap()
}

/**
 * Zerlegt den `s=2`-String `"SCH 55 BAK 75 …"` bzw. `"55 75 …"` in eine Map
 * Kürzel → Wert in Spaltenreihenfolge. Der String kann mit oder ohne Kürzel
 * kommen; fehlende Kürzel werden über [spalten] positionsbasiert ergänzt.
 */
internal fun skillWerteAusIndex(text: String, spalten: List<String>): Map<String, String> {
    val tokens = text.split(' ').filter { it.isNotBlank() }
    if (tokens.isEmpty()) return emptyMap()
    val istPaar = tokens.size % 2 == 0 && tokens.first().all { it.isLetter() }
    val werte = if (istPaar) tokens.chunked(2).map { it[1] }
    else if (tokens.first().toIntOrNull() != null) tokens
    else tokens.drop(1)
    return spalten.withIndex().mapNotNull { (i, k) ->
        werte.getOrNull(i)?.takeIf { it.isNotBlank() }?.let { k to it }
    }.toMap()
}

/** Positionsfarbe aus dem Untertitel („Abwehr · 31 Jahre · …“), sonst neutral. */
private fun kartenFarbe(zeile: UebersichtZeile?) =
    zeile?.untertitel
        ?.split('·')
        ?.firstNotNullOfOrNull { positionVonText(it) }
        ?.let(::trikotFarbe)
