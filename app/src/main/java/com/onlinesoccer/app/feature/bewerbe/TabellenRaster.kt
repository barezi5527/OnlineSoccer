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
import androidx.compose.foundation.ScrollState
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
 * Breite, feste Breiten führen zu waagerechtem Scrollen. Markierte Spalten bleiben
 * links fixiert; bei Ranglisten sind dies Platz, Land und Club.
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
    val hatFixierteSpalten = spalten.any { it.fest }
    val fuelltBreite = spalten.all { it.gewicht != null }
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    Column(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (eigeneHoehenRollleiste) Modifier.weight(1f) else Modifier)
                .then(if (!hatFixierteSpalten && !fuelltBreite) Modifier.horizontalScroll(horizontalScroll) else Modifier),
        ) {
            TabellenKopfzeile(spalten, horizontalScroll = horizontalScroll.takeIf { hatFixierteSpalten })
            Column(
                Modifier.then(
                    if (eigeneHoehenRollleiste) Modifier.weight(1f).verticalScroll(verticalScroll)
                    else Modifier,
                ),
            ) {
                TabellenZeilen(
                    spalten,
                    zeilen,
                    teamIds,
                    onTeamClick,
                    zeilenHintergrund,
                    horizontalScroll = horizontalScroll.takeIf { hatFixierteSpalten },
                )
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
    horizontalScroll: ScrollState? = null,
) {
    RasterZeile(spalten, modifier, horizontalScroll) { spalte ->
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
    horizontalScroll: ScrollState? = null,
) {
    Column(modifier) {
        zeilen.forEachIndexed { index, zeile ->
            val teamId = teamIds.getOrNull(index)
            RasterZeile(spalten, Modifier.background(zeilenHintergrund(index)), horizontalScroll) { spalte ->
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

@Composable
private fun RasterZeile(
    spalten: List<TabellenSpalte>,
    modifier: Modifier,
    horizontalScroll: ScrollState?,
    zelle: @Composable RowScope.(TabellenSpalte) -> Unit,
) {
    val ersterScrollIndex = spalten.indexOfFirst { !it.fest }
        .let { if (it < 0) spalten.size else it }
    val festeSpalten = spalten.take(ersterScrollIndex)
    val scrollSpalten = spalten.drop(ersterScrollIndex)
    Row(modifier.fillMaxWidth()) {
        festeSpalten.forEach { zelle(it) }
        if (horizontalScroll != null && scrollSpalten.isNotEmpty()) {
            Row(Modifier.weight(1f).horizontalScroll(horizontalScroll)) {
                scrollSpalten.forEach { zelle(it) }
            }
        } else {
            scrollSpalten.forEach { zelle(it) }
        }
    }
}

/** Gewichtete Spalte füllt die Restbreite, feste Breite erzwingt waagerechten Scroll. */
private fun RowScope.spaltenBreite(spalte: TabellenSpalte): Modifier =
    if (spalte.gewicht != null) Modifier.weight(spalte.gewicht) else Modifier.width(spalte.breite)

/**
 * Spalten der Ranglisten genau so, wie `InternationaleRepository` sie liefert:
 * `#`, `Lnd`, `Club`, `Sum` und die Saisons als `S23` … Der Verein ist die
 * verlinkte Spalte. Erkannt wird die Vorlage an `Lnd`, das nur die Ranglisten
 * haben – sonst bekämen sie die Ligatabelle-Spalten (`Sp.`, `Tore`, `Pkt.`), die
 * es auf diesen Seiten gar nicht gibt.
 */
internal fun rankingTabellenSpalten(header: List<String>): List<TabellenSpalte>? {
    if (header.none { it.equals("Lnd", ignoreCase = true) }) return null
    val istClubRanking = header.any { it.equals("Club", ignoreCase = true) }
    return header.mapIndexed { stelle, kopf ->
        val istPlatz = kopf.equals("#", ignoreCase = true)
        val istClub = kopf.equals("Club", ignoreCase = true)
        val istSumme = kopf.equals("Sum", ignoreCase = true)
        TabellenSpalte(
            kopf = kopf,
            headIdx = stelle,
            team = istClub,
            breite = when {
                istPlatz -> 44.dp
                kopf.equals("Lnd", ignoreCase = true) -> 68.dp
                istClub -> 150.dp
                istSumme -> 64.dp
                else -> 64.dp
            },
            gewicht = null,
            wert = { getOrNull(stelle)?.let { v ->
                val t = v.replace(' ', ' ').trim()
                if (t.isEmpty()) return@let "–"
                var s2 = t
                val dot = s2.lastIndexOf('.')
                if (dot >= 0) {
                    val f = s2.substring(dot + 1).trimEnd('0')
                    val i = s2.substring(0, dot)
                    s2 = if (f.isEmpty()) i else "$i.$f"
                } else {
                    val c = s2.lastIndexOf(',')
                    if (c >= 0) {
                        val f = s2.substring(c + 1).trimEnd('0')
                        val i = s2.substring(0, c)
                        s2 = if (f.isEmpty()) i else "$i.$f"
                    }
                }
                if (istClubRanking && !istPlatz && !istClub) s2.replace(".", "") else s2
            } ?: "–" },
            fest = istPlatz || kopf.equals("Lnd", ignoreCase = true) || istClub,
        )
    }
}

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
