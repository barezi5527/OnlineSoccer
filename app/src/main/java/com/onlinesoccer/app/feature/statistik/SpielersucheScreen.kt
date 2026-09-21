package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.LaenderOption
import com.onlinesoccer.app.data.model.SucheAttribut
import com.onlinesoccer.app.data.model.SucheKriterium

private val Spalten = listOf(
    StatistikFixSpalte("#", 26.dp, TextAlign.End),
    StatistikFixSpalte("Alter", 46.dp, TextAlign.End),
    StatistikFixSpalte("Pos", 44.dp),
    StatistikFixSpalte("Nat", 46.dp),
    StatistikFixSpalte("Verein", 132.dp),
)

private val PinKopf = "Spieler"
private const val PinBreiteDp = 170

/** Spielersuche (`osneu/spielersuche`) – Basis-Filter, Kriterien, gespeicherte Abfragen. */
@Composable
fun SpielersucheScreen(
    onClose: () -> Unit,
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
    viewModel: SpielersucheViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var filterOffen by remember { mutableStateOf(false) }

    when {
        uiState.ladend -> StatistikLaden()
        uiState.fehler != null -> StatistikFehler(uiState.fehler!!, viewModel::ladeOptionen)
        else -> Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatistikHeader(
                    titel = "Spielersuche",
                    untertitel = "Nachname, Attribute und Transferdetails filtern",
                    onClose = onClose,
                )
                Spacer(Modifier.weight(1f))
                StatistikFilterToggle(filterOffen) { filterOffen = !filterOffen }
            }

            if (filterOffen) {
                AbfragenKarte(uiState, viewModel, onFilterGesetzt = { filterOffen = false })
                BasisFilterKarte(uiState, viewModel, onFilterGesetzt = { filterOffen = false })
                KriterienKarte(uiState, viewModel)
            }

            FilledTonalButton(
                onClick = { viewModel.suchen(); filterOffen = false },
                enabled = !uiState.sucht,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.sucht) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Suche läuft …")
                } else {
                    Text("Suchen")
                }
            }

            uiState.ergebnis?.let { ergebnis ->
                Text(
                    "Anzahl der gefundenen Spieler: ${ergebnis.anzahl}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatistikScrollTabelle(
                    pinKopf = PinKopf,
                    pinBreite = PinBreiteDp.dp,
                    spalten = Spalten,
                    zeilen = ergebnis.zeilen.map { zeile ->
                        StatistikScrollZeile(
                            pin = {
                                StatistikSpielerZelle(zeile.name, zeile.position, zeile.pid, onSpielerClick)
                            },
                            zellen = {
                                StatistikFixZelle(Spalten[0]) { StatistikZahl(zeile.nr?.toString() ?: "–") }
                                StatistikFixZelle(Spalten[1]) { StatistikZahl(zeile.alter) }
                                StatistikFixZelle(Spalten[2]) { StatistikZahl(zeile.position?.uppercase() ?: "–") }
                                StatistikFixZelle(Spalten[3]) { FlaggenText(zeile.nation) }
                                StatistikFixZelle(Spalten[4]) { StatistikTeamZelle(zeile.team, zeile.teamId, onTeamClick) }
                            },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AbfragenKarte(
    uiState: SpielersucheUiState,
    viewModel: SpielersucheViewModel,
    onFilterGesetzt: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatistikDropdown(
                label = "Abfrage laden",
                optionen = listOf(LaenderOption("0", "— Abfrage wählen —")) +
                    uiState.abfragen.map { LaenderOption(it.id.toString(), it.name) },
                wert = uiState.ausgewaehlteAbfrageId.toString(),
                ladend = false,
                onWaehlen = { id -> viewModel.abfrageLaden(id.toIntOrNull() ?: 0); onFilterGesetzt() },
            )
            OutlinedTextField(
                value = uiState.abfrageName,
                onValueChange = viewModel::abfrageNameAendern,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.abfrageFehler?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { viewModel.abfrageSpeichern(alsNeu = false) }) {
                    Text("Speichern")
                }
                TextButton(onClick = { viewModel.abfrageSpeichern(alsNeu = true) }) {
                    Text("Speichern unter…")
                }
                TextButton(
                    onClick = { viewModel.abfrageLoeschen(uiState.ausgewaehlteAbfrageId) },
                    enabled = uiState.ausgewaehlteAbfrageId > 0,
                ) {
                    Text("Löschen")
                }
            }
        }
    }
}

@Composable
private fun BasisFilterKarte(
    uiState: SpielersucheUiState,
    viewModel: SpielersucheViewModel,
    onFilterGesetzt: () -> Unit,
) {
    StatistikFilterCard {
        StatistikDropdown("Land", uiState.optionen.laender, uiState.basis.landId.toString(), false) { viewModel.landWaehlen(it); onFilterGesetzt() }
        StatistikDropdown("Liga", uiState.optionen.ligas, uiState.basis.ligaId.toString(), false) { viewModel.ligaWaehlen(it); onFilterGesetzt() }
        StatistikDropdown("Nation", uiState.optionen.nationen, uiState.basis.nationId.toString(), false) { viewModel.nationWaehlen(it); onFilterGesetzt() }
        StatistikDropdown("Anzeige", uiState.optionen.anzeigen, uiState.basis.anzeigeId.toString(), false) { viewModel.anzeigeWaehlen(it); onFilterGesetzt() }
    }
}

@Composable
private fun KriterienKarte(uiState: SpielersucheUiState, viewModel: SpielersucheViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Filterkriterien", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            uiState.kriterien.forEachIndexed { index, kriterium ->
                KriteriumZeile(
                    index = index,
                    kriterium = kriterium,
                    gesamt = uiState.kriterien.size,
                    attribute = uiState.optionen.attribute,
                    onAendern = { k -> viewModel.kriteriumAendern(index, k) },
                    onHoch = { viewModel.kriteriumHoch(index) },
                    onRunter = { viewModel.kriteriumRunter(index) },
                    onEntfernen = { viewModel.kriteriumEntfernen(index) },
                )
            }
            TextButton(onClick = viewModel::kriteriumHinzufuegen) {
                Text("+ Kriterium hinzufügen")
            }
        }
    }
}

@Composable
private fun KriteriumZeile(
    index: Int,
    kriterium: SucheKriterium,
    gesamt: Int,
    attribute: List<SucheAttribut>,
    onAendern: (SucheKriterium) -> Unit,
    onHoch: () -> Unit,
    onRunter: () -> Unit,
    onEntfernen: () -> Unit,
) {
    val attribut = attribute.firstOrNull { it.id == kriterium.attributId }
    val typ = attribut?.typ ?: "zahl"
    val istKategorie = typ == "kategorie"
    Column(Modifier.fillMaxWidth()) {
        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${index + 1}.",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(end = 6.dp),
            )
            StatistikAuswahl(
                optionen = attribute.map { LaenderOption(it.id.toString(), it.name) },
                wert = kriterium.attributId.toString(),
                onWaehlen = { id -> onAendern(kriterium.copy(attributId = id.toIntOrNull() ?: kriterium.attributId)) },
                modifier = Modifier.weight(2f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            StatistikAuswahl(
                optionen = erlaubteOperatoren(typ).map { LaenderOption(it, it) },
                wert = kriterium.opVon,
                onWaehlen = { op -> onAendern(kriterium.copy(opVon = op)) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            if (istKategorie) {
                StatistikAuswahl(
                    optionen = attribut?.werte.orEmpty(),
                    wert = kriterium.valVon,
                    onWaehlen = { v -> onAendern(kriterium.copy(valVon = v)) },
                    modifier = Modifier.weight(1.4f),
                )
            } else {
                OutlinedTextField(
                    value = kriterium.valVon,
                    onValueChange = { v -> onAendern(kriterium.copy(valVon = v)) },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
        if (typ == "zahl") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                StatistikAuswahl(
                    optionen = erlaubteOperatoren("zahl").map { LaenderOption(it, it) },
                    wert = kriterium.opBis,
                    onWaehlen = { op -> onAendern(kriterium.copy(opBis = op)) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                OutlinedTextField(
                    value = kriterium.valBis,
                    onValueChange = { v -> onAendern(kriterium.copy(valBis = v)) },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            StatistikAuswahl(
                optionen = listOf(
                    LaenderOption("", "keine Sortierung"),
                    LaenderOption("asc", "aufsteigend"),
                    LaenderOption("desc", "absteigend"),
                ),
                wert = kriterium.sort,
                onWaehlen = { s -> onAendern(kriterium.copy(sort = s)) },
                label = "Sortierung",
                modifier = Modifier.weight(1.4f),
            )
            Spacer(Modifier.weight(0.6f))
            IconButton(onClick = onHoch, enabled = index > 0) {
                Icon(Icons.Filled.ArrowDropUp, contentDescription = "Nach oben")
            }
            IconButton(onClick = onRunter, enabled = index < gesamt - 1) {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Nach unten")
            }
            IconButton(onClick = onEntfernen, enabled = gesamt > 1) {
                Icon(Icons.Filled.Close, contentDescription = "Kriterium entfernen")
            }
        }
    }
}