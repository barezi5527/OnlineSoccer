package com.onlinesoccer.app.feature.bewerbe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Tabellenraster der Ligatabelle: feste Kopfzeile, linksbündige Teamspalte mit
 * Kaderaufruf, zentrierte Zahlen. Spalten mit Gewicht füllen die verfügbare
 * Breite, feste Breiten führen zu waagerechtem Scrollen.
 *
 * @param eigeneHoehenRollleiste true, wenn der Aufrufer eine begrenzte Höhe vorgibt
 *   (z. B. `Modifier.weight(1f)`), damit die Kopfzeile sichtbar bleibt.
 */
@Composable
internal fun TabellenRaster(
    spalten: List<TabellenSpalte>,
    zeilen: List<List<String>>,
    teamIds: List<Long?> = emptyList(),
    onTeamClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    eigeneHoehenRollleiste: Boolean = false,
    zeilenHintergrund: (Int) -> Color = { Color.Transparent },
) {
    if (spalten.isEmpty()) return
    val fuelltBreite = spalten.all { it.gewicht != null }
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    Column(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (eigeneHoehenRollleiste) Modifier.weight(1f) else Modifier)
                .then(if (fuelltBreite) Modifier else Modifier.horizontalScroll(horizontalScroll)),
        ) {
            TabellenKopfzeile(spalten)
            Column(
                Modifier.then(
                    if (eigeneHoehenRollleiste) {
                        Modifier.weight(1f).verticalScroll(verticalScroll)
                    } else {
                        Modifier
                    },
                ),
            ) {
                TabellenZeilen(spalten, zeilen, teamIds, onTeamClick, zeilenHintergrund)
            }
        }
    }
}

/**
 * Spaltenüberschrift des Rasters. Getrennt von [TabellenZeilen], damit Aufrufer die
 * Kopfzeile außerhalb des rollbaren Bereichs halten und sie beim Scrollen fixiert
 * anzeigen können.
 */
@Composable
internal fun TabellenKopfzeile(
    spalten: List<TabellenSpalte>,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth()) {
        spalten.forEach { spalte ->
            Text(
                spalte.kopf,
                modifier = spaltenBreite(spalte).padding(horizontal = 6.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (spalte.team) TextAlign.Start else TextAlign.Center,
            )
        }
    }
}

/**
 * Datenzeilen des Rasters mit optionaler Klickfläche auf die Teamspalte und
 * zeilenweiser Hintergrundfarbe (Platzierung der Website).
 */
@Composable
internal fun TabellenZeilen(
    spalten: List<TabellenSpalte>,
    zeilen: List<List<String>>,
    teamIds: List<Long?> = emptyList(),
    onTeamClick: (Long) -> Unit = {},
    zeilenHintergrund: (Int) -> Color = { Color.Transparent },
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        zeilen.forEachIndexed { index, zeile ->
            val teamId = teamIds.getOrNull(index)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(zeilenHintergrund(index)),
            ) {
                spalten.forEach { spalte ->
                    val klickbar = spalte.team && teamId != null
                    Text(
                        spalte.wert(zeile),
                        modifier = spaltenBreite(spalte)
                            .then(if (klickbar) Modifier.clickable { onTeamClick(teamId) } else Modifier)
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (spalte.team) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (spalte.team) TextAlign.Start else TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Gewichtete Spalte füllt die Restbreite, feste Breite erzwingt waagerechten Scroll. */
private fun RowScope.spaltenBreite(spalte: TabellenSpalte): Modifier =
    if (spalte.gewicht != null) Modifier.weight(spalte.gewicht) else Modifier.width(spalte.breite)

/**
 * Kompakte Spalten für Ranglisten (`osranking`): Platz, Team bzw. Land und Summe.
 * Die vielen Saisonspalten der Website werden bewusst nicht übernommen.
 */
internal fun ranglistenTabellenSpalten(header: List<String>): List<TabellenSpalte>? {
    fun idx(vararg alias: String): Int? = alias.firstNotNullOfOrNull { kandidat ->
        header.indexOfFirst { it.equals(kandidat, ignoreCase = true) || it.trim() == kandidat }
            .takeIf { it >= 0 }
    }
    val platz = idx("Platz", "#", "Pos.") ?: 0
    val team = idx("Club", "Team", "Verein") ?: idx("Land", "Nation")
    if (team == null) return null
    val summe = idx("Summe", "Gesamt", "Punkte")

    return buildList {
        add(
            TabellenSpalte(
                kopf = "#",
                headIdx = platz,
                team = false,
                breite = 44.dp,
                gewicht = 0.8f,
                wert = { getOrNull(platz) ?: "–" },
            ),
        )
        add(
            TabellenSpalte(
                kopf = "Team",
                headIdx = team,
                team = true,
                breite = 160.dp,
                gewicht = 4.6f,
                wert = { getOrNull(team) ?: "–" },
            ),
        )
        if (summe != null) {
            add(
                TabellenSpalte(
                    kopf = "Summe",
                    headIdx = summe,
                    team = false,
                    breite = 72.dp,
                    gewicht = 1.4f,
                    wert = { getOrNull(summe) ?: "–" },
                ),
            )
        }
    }
}