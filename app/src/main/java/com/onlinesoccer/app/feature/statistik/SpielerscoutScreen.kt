package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.ScoutSpieler
import com.onlinesoccer.app.data.model.SpielerscoutDaten

/** Eine Kategorie des Spielerscouts mit Titel, Beschreibung und Wert-Spaltenkopf. */
internal enum class ScoutKategorie(
    val titel: String,
    val beschreibung: String,
    val wertKopf: String,
    val wertSpalte: Dp = 74.dp,
) {
    TALENTE(
        titel = "Talente",
        beschreibung = "Bis 23 Jahre mit bereits stark ausgeprägten Fähigkeiten – Opti Skill ≥ 70.",
        wertKopf = "Opti Skill",
    ),
    ALLROUNDER(
        titel = "Allrounder",
        beschreibung = "Ausgeglichene Alleskönner ohne klare Schwächen – Skill ≥ 70.",
        wertKopf = "Skill",
    ),
    TOP_SPIELER(
        titel = "Top-Spieler",
        beschreibung = "Die besten etablierten Spieler – Opti Skill ≥ 92.",
        wertKopf = "Opti Skill",
    ),
    SUPERSTAR(
        titel = "Superstars",
        beschreibung = "Die absolute Elite der Spielwelt – Opti Skill ≥ 100.",
        wertKopf = "Opti Skill",
    ),
    WERTANLAGE(
        titel = "Wertanlagen",
        beschreibung = "Starke Spieler zum günstigen Preis – Opti Skill ≥ 85, sortiert nach Marktwert.",
        wertKopf = "Marktwert",
        wertSpalte = 92.dp,
    ),
    VETERAN(
        titel = "Veteranen",
        beschreibung = "Erfahrene Altstars ab 30 Jahren – Opti Skill ≥ 90.",
        wertKopf = "Opti Skill",
    ),
}

/** Spieler einer Spielerscout-Kategorie. */
internal fun kategorieSpieler(daten: SpielerscoutDaten, kategorie: ScoutKategorie): List<ScoutSpieler> =
    when (kategorie) {
        ScoutKategorie.TALENTE -> daten.talente
        ScoutKategorie.ALLROUNDER -> daten.allrounder
        ScoutKategorie.TOP_SPIELER -> daten.topSpieler
        ScoutKategorie.SUPERSTAR -> daten.superstars
        ScoutKategorie.WERTANLAGE -> daten.wertanlagen
        ScoutKategorie.VETERAN -> daten.veteranen
    }

/** Spielerscout – die vielversprechendsten Spieler in sechs Kategorien. */
@Composable
fun SpielerscoutScreen(
    onClose: () -> Unit,
    onSpielerClick: (Long) -> Unit = {},
    onTeamClick: (Long) -> Unit = {},
    viewModel: SpielerscoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatistikHeader(
            titel = "Spielerscout",
            untertitel = "Die vielversprechendsten Spieler weltweit je Kategorie",
            onClose = onClose,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.ladend -> CircularProgressIndicator(Modifier.size(24.dp))
                uiState.fehler != null -> StatistikFehler(uiState.fehler!!, viewModel::lade)
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
                ) {
                    items(ScoutKategorie.entries) { kategorie ->
                        ScoutKategorieKarte(
                            kategorie = kategorie,
                            zeilen = kategorieSpieler(uiState.daten, kategorie),
                            onSpielerClick = onSpielerClick,
                            onTeamClick = onTeamClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoutKategorieKarte(
    kategorie: ScoutKategorie,
    zeilen: List<ScoutSpieler>,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    val spalten = listOf(
        StatistikFixSpalte("Spieler", 160.dp),
        StatistikFixSpalte("#", 30.dp, TextAlign.End),
        StatistikFixSpalte("Alter", 40.dp, TextAlign.End),
        StatistikFixSpalte("Pos", 44.dp),
        StatistikFixSpalte("Nat", 44.dp),
        StatistikFixSpalte("Verein", 150.dp),
        StatistikFixSpalte(kategorie.wertKopf, kategorie.wertSpalte, TextAlign.End),
    )
    val gesamtBreite: Dp = spalten.fold(0.dp) { summe, spalte -> summe + spalte.breite }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    kategorie.titel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "  (${zeilen.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                kategorie.beschreibung,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (zeilen.isEmpty()) {
                Text(
                    "Keine Treffer.",
                    Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    Column(Modifier.width(gesamtBreite)) {
                        Row {
                            spalten.forEach { spalte ->
                                StatistikFixZelle(spalte) {
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
                        zeilen.forEach { spieler ->
                            Row {
                                StatistikFixZelle(spalten[0]) {
                                    StatistikSpielerZelle(spieler.name, spieler.position, spieler.pid, onSpielerClick)
                                }
                                StatistikFixZelle(spalten[1]) { StatistikZahl(spieler.nr?.toString() ?: "–") }
                                StatistikFixZelle(spalten[2]) { StatistikZahl(spieler.alter) }
                                StatistikFixZelle(spalten[3]) { StatistikZahl(spieler.position?.uppercase() ?: "–") }
                                StatistikFixZelle(spalten[4]) { FlaggenText(spieler.nation) }
                                StatistikFixZelle(spalten[5]) { StatistikTeamZelle(spieler.team, spieler.teamId, onTeamClick) }
                                StatistikFixZelle(spalten[6]) { StatistikWert(spieler.wert) }
                            }
                        }
                    }
                }
            }
        }
    }
}