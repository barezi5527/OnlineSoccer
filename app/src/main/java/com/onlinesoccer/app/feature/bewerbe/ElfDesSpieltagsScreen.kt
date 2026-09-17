package com.onlinesoccer.app.feature.bewerbe

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.ui.theme.PositionsBadge
import com.onlinesoccer.app.data.model.BewertungsZeile
import com.onlinesoccer.app.data.model.ElfErgebnis
import com.onlinesoccer.app.data.model.ElfSpieler
import com.onlinesoccer.app.data.model.LigaOption
import com.onlinesoccer.app.data.model.SpielerPosition
import com.onlinesoccer.app.data.repository.ElfAuswahl
import com.onlinesoccer.app.data.repository.ElfBewertung
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val rasenFarbe = Color(0xFF2E7D32)
private val linienFarbe = Color(0xFFE8F5E9)
/** Dezenter Wappen-Platzhalter, falls das Bild des Vereins nicht ladbar ist. */
private val wappenPlatzhalterFarbe = Color(0x33102A16)
private val wappenPlatzhalterTextFarbe = Color(0xFFEDF7EE)

/**
 * Vertikale Grenzen des Spielfelds (0 = oben, 1 = unten) für die Spielerplatzierung.
 * Großzügig geöffnet, damit die Aufstellung im Feld bleibt.
 */
internal const val SPIELFELD_OBEN = 0.06f
internal const val SPIELFELD_UNTEN = 0.96f
/** Sicherer Seitenabstand der Spieler zum Rasenrand (Anteil der Breite). */
internal const val SPIELFELD_RAND_ANTEIL = 0.04f
/** Mindest- & Maximalbreite eines Spielerelements (Slot) in dp. */
private val MIN_SLOT_BREITE = 40.dp
private val MAX_SLOT_BREITE = 150.dp
/** Feste, einheitliche Wappen-Größe in dp – unabhängig von Gerät und Formation. */
private val WAPPEN_GROESSE = 40.dp
/** Horizontaler Abstand zwischen zwei Spielern (dp). */
private val SPIELER_ABSTAND = 8.dp
/** Platz für die Hinweiszeile über dem Spielfeld (damit die Elf eine Seite füllt). */
private val HINWEIS_RESERVE = 48.dp

/**
 * Erklärender Text der „So wurde bewertet"-Sektion (exakt wie gefordert).
 */
private const val SO_WURDE_BEWERTET_TEXT =
    "Die Bewertung startet bei 5,5 (Startelf) bzw. 4,0 (Einwechslung) und baut auf fünf " +
        "Kategorien aus dem Spielbericht auf: Direkter Impact (Tore, Vorlagen, Elfmeter), " +
        "Effizienz (Schussquote, Abschlüsse, Auffälligkeit), Zweikämpfe (gewonnene Zweikämpfe, " +
        "Quote; beim Torwart gehaltene Bälle), Ergebnis (Sieg/Unentschieden/Niederlage, Zu null, " +
        "nur bei bekanntem Endstand) und die Bericht-Note. Karten zählen als Abzug. Die " +
        "Einsatzzeit selbst ist kein Bonus – nicht eingesetzte Spieler bleiben von der Elf " +
        "ausgeschlossen. Bewertet wird von 1,0 bis 10,0; eine 10,0 erfordert eine ungerundete " +
        "Rohnote von mindestens 9,95. Es findet keine Normalisierung statt."

/**
 * „Elf des Spieltags": Spieler aus den echten Spielberichten des gewählten
 * Spieltags, lokal und transparent bewertet (1,0–10,0) und in einer klassischen,
 * dynamisch gewählten Formation auf dem Spielfeld dargestellt. Die eigene Liga
 * wird dynamisch aus dem Benutzerkontext ermittelt. Ein Klick auf einen Spieler
 * öffnet dessen Bewertungsdetails samt Sprung zur vorhandenen Spielerkarte.
 */
@Composable
fun ElfDesSpieltagsAnsicht(
    onSpielerKarte: (Long) -> Unit = {},
    onVerein: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ElfDesSpieltagsViewModel = hiltViewModel(),
) {
    // Das Spielfeld braucht genügend Höhe, damit alle Markierungen (Avatar mit
    // Overlays, Nachname, Vereinsname) vollständig und überlappungsfrei stehen.
    // Deshalb wird die Ansicht im Hochformat gehalten – beim Verlassen wird die
    // vorherige Ausrichtung wiederhergestellt.
    HochformatErzwingen()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.start() }

    var detailSpieler by remember { mutableStateOf<ElfSpieler?>(null) }
    var zeigeUebersicht by remember { mutableStateOf(false) }

    Column(modifier.then(Modifier.fillMaxSize())) {
        when {
            // Kontext (Liga) konnte nicht ermittelt werden.
            uiState.fehler != null && !uiState.kontextGeladen -> {
                MeldungBlock(
                    text = uiState.fehler.orEmpty(),
                    schaden = true,
                    onErneut = viewModel::erneutVersuchen,
                )
            }

            // Kontext wird noch geladen.
            !uiState.kontextGeladen -> {
                FortschrittBlock(
                    uiState.ladephase,
                    null,
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
                )
            }

            else -> {
                ElfKopfzeile(uiState, viewModel::waehleLand, viewModel::waehleLiga, viewModel::waehleZat)
                when {
                    uiState.ladend -> FortschrittBlock(
                        uiState.ladephase,
                        uiState.fortschritt,
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )

                    uiState.fehler != null -> MeldungBlock(
                        text = uiState.fehler.orEmpty(),
                        schaden = true,
                        onErneut = viewModel::erneutLaden,
                    )

                    uiState.ergebnis?.spieler.isNullOrEmpty() -> MeldungBlock(
                        text = uiState.ladephase ?: "Keine Daten für diesen Spieltag.",
                        schaden = uiState.ergebnis?.begegnungen != 0,
                        onErneut = if (uiState.ergebnis?.begegnungen == 0) null else viewModel::erneutLaden,
                    )

                    else -> uiState.ergebnis?.let { erg ->
                        ElfErgebnisInhalt(
                            ergebnis = erg,
                            hinweis = uiState.ladephase,
                            onSpielerKlick = { detailSpieler = it },
                            onDetailsAnzeigen = { zeigeUebersicht = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (zeigeUebersicht) {
        uiState.ergebnis?.let { erg ->
            BewertungsUebersichtDialog(
                ergebnis = erg,
                onSpielerKlick = {
                    zeigeUebersicht = false
                    detailSpieler = it
                },
                onSchliessen = { zeigeUebersicht = false },
            )
        }
    }

    detailSpieler?.let { spieler ->
        BewertungsDetailDialog(
            spieler = spieler,
            onSchliessen = { detailSpieler = null },
            onSpielerkarte = {
                if (spieler.spielerId != null) {
                    detailSpieler = null
                    onSpielerKarte(spieler.spielerId)
                }
            },
            onVerein = {
                if (spieler.teamId != null) {
                    detailSpieler = null
                    onVerein(spieler.teamId)
                }
            },
        )
    }
}

/**
 * Hält die Aktivität, solange die Elf-Ansicht sichtbar ist, im Hochformat
 * (das Spielfeld braucht die Höhe für vollständige Markierungen). Beim
 * Verlassen wird die vorherige Ausrichtung wiederhergestellt, damit alle
 * übrigen Ansichten weiterhin querdrehbar bleiben.
 */
@Composable
private fun HochformatErzwingen() {
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        val vorher = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = vorher
        }
    }
}

/** Kopfzeile mit Titel, Land-/Liga-/Spieltag-Auswahl und Saison-Kontext. */
@Composable
private fun ElfKopfzeile(
    uiState: ElfDesSpieltagsUiState,
    onLand: (Int) -> Unit,
    onLiga: (Int) -> Unit,
    onZat: (Int) -> Unit,
) {
    val aktiv = !uiState.ladend
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "⭐ Elf des Spieltags",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (uiState.saison > 0) {
                    Text(
                        "Saison ${uiState.saison}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            ElfZatAuswahl(
                zat = uiState.zat,
                zatOptionen = uiState.zatOptionen,
                aktiv = aktiv,
                onZat = onZat,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ElfFilterAuswahl(
                selected = uiState.landId,
                optionen = uiState.landOptionen,
                onSelect = onLand,
                modifier = Modifier.weight(1f),
                leerLabel = "Land",
                aktiv = aktiv,
            )
            ElfFilterAuswahl(
                selected = uiState.ligaId,
                optionen = uiState.ligaOptionen,
                onSelect = onLiga,
                modifier = Modifier.weight(1f),
                leerLabel = "Liga",
                aktiv = aktiv,
            )
        }
    }
}

/** Einheitlicher Land-/Liga-Dropdown der Elf-Auswahl (like Bewerbe `FilterAuswahl`). */
@Composable
private fun ElfFilterAuswahl(
    selected: Int,
    optionen: List<LigaOption>,
    onSelect: (Int) -> Unit,
    aktiv: Boolean,
    modifier: Modifier = Modifier,
    leerLabel: String = "Auswahl",
) {
    var offen by remember { mutableStateOf(false) }
    val text = optionen.firstOrNull { it.wert == selected }?.label ?: leerLabel
    Box(modifier) {
        OutlinedButton(
            onClick = { if (aktiv) offen = true },
            enabled = aktiv,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            optionen.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label) },
                    onClick = {
                        offen = false
                        if (opt.wert != selected) onSelect(opt.wert)
                    },
                )
            }
        }
    }
}

/** Spieltag-Auswahl (ähnlich der Spieltage-Ansicht, eigenes ViewModel). */
@Composable
private fun ElfZatAuswahl(
    zat: Int,
    zatOptionen: List<Int>,
    aktiv: Boolean,
    onZat: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { if (aktiv) expanded = true },
            enabled = aktiv,
        ) {
            Text("Spieltag ${zat.takeIf { it > 0 } ?: "–"}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            zatOptionen.forEach { option ->
                DropdownMenuItem(
                    text = { Text("Spieltag $option") },
                    onClick = {
                        expanded = false
                        if (option != zat) onZat(option)
                    },
                )
            }
        }
    }
}

/** Fortschritts-/Ladeblock mit Spinner und Prüf-Fortschritt. */
@Composable
private fun FortschrittBlock(
    ladephase: String?,
    fortschritt: Pair<Int, Int>?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        if (ladephase != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                ladephase,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        if (fortschritt != null && fortschritt.second > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                "${fortschritt.first} von ${fortschritt.second} Begegnungen geprüft",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Zentrierte Meldung (Fehler oder Hinweis) mit optionalem Neu-Laden. */
@Composable
private fun MeldungBlock(
    text: String,
    schaden: Boolean,
    onErneut: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (schaden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (onErneut != null) {
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onErneut) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Erneut versuchen")
            }
        }
    }
}

/**
 * Standard-„So wurde bewertet"-Kasten: Spieler des Spieltags, Erklärtext und
 * der Button „Bewertungsdetails anzeigen".
 */
@Composable
private fun SoWurdeBewertet(
    ergebnis: ElfErgebnis,
    onDetailsAnzeigen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ergebnis.spielerDesSpieltags?.let { bester ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "🏆 Spieler des Spieltags",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "${bester.name} · ${bester.verein}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${ElfBewertung.formatiere(bester.bewertung)} / 10",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "So wurde bewertet",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    SO_WURDE_BEWERTET_TEXT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        FilledTonalButton(
            onClick = onDetailsAnzeigen,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Bewertungsdetails anzeigen")
        }
    }
}

/**
 * Hinweiszeile über dem Spielfeld (Anzahl Begegnungen, Vollständigkeit).
 *
 * Das Spielfeld füllt fast den gesamten verfügbaren Platz (nur die dezente
 * Hinweiszeile bleibt oben stehen), sodass die Elf auf einem Smartphone eine
 * ganze Seite einnimmt. Die feste „So wurde bewertet"-Sektion liegt darunter
 * und bleibt über die Scroll-Spalte erreichbar.
 */
@Composable
private fun ElfErgebnisInhalt(
    ergebnis: ElfErgebnis,
    hinweis: String?,
    onSpielerKlick: (ElfSpieler) -> Unit,
    onDetailsAnzeigen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        // Das Spielfeld nutzt fast den gesamten verfügbaren Platz – nur eine
        // schmale Zeile bleibt für den Hinweis oben stehen, damit die Elf
        // auf einem Smartphone eine ganze Seite füllt.
        val feldHoehe = (maxHeight - HINWEIS_RESERVE).coerceAtLeast(320.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
        ) {
            Text(
                (ergebnis.formation?.let { "Formation $it · " } ?: "") +
                    "${ergebnis.begegnungen} Begegnungen · ${ergebnis.berichteErfolgreich} ausgewertet" +
                    (hinweis?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = if (ergebnis.vollstaendig) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Spielfeld(
                ergebnis = ergebnis,
                onSpielerKlick = onSpielerKlick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(feldHoehe),
            )
            Spacer(Modifier.height(10.dp))
            SoWurdeBewertet(
                ergebnis = ergebnis,
                onDetailsAnzeigen = onDetailsAnzeigen,
            )
        }
    }
}

/** Eine Positionsgruppe (Feldlinie) mit ihren Spielern und dem Linien-Index. */
internal data class SpielerLine(
    val index: Int,
    val spieler: List<ElfSpieler>,
)

/**
 * Spieler-Karte auf der Taktiktafel: Das Vereinswappen (Club-Badge) ist das
 * zentrale Avatar-Motiv (statt eines Fotos) und wird mittig, proportional
 * gefüllt dargestellt. Über dem Avatar sitzen die Bewertung (oben rechts) und
 * bei Bedarf die Kapitäns-Binde (C) oben links; ein Fach mit Wechsel-Pfeil und
 * Karten (Gelb/Rot) liegt unten links. Darunter folgen in zwei Zeilen der
 * Nachname und – kleiner & dezenter – der Vereinsname.
 */
@Composable
private fun ElfSpielerMarkierung(
    spieler: ElfSpieler,
    wappenGroesse: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(wappenGroesse),
            contentAlignment = Alignment.Center,
        ) {
            VereinsWappen(
                teamId = spieler.teamId,
                verein = spieler.verein,
                size = wappenGroesse,
            )
            BewertungsBadge(
                bewertung = spieler.bewertung,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            if (spieler.kapitän) {
                KapitänBadge(Modifier.align(Alignment.TopStart))
            }
            val wechsel = !spieler.startelf || (spieler.minuten?.let { it in 1 until 90 } == true)
            if (wechsel || spieler.gelbeKarten > 0 || spieler.roteKarten > 0) {
                AktionenBadge(
                    wechsel = wechsel,
                    gelbeKarten = spieler.gelbeKarten,
                    roteKarten = spieler.roteKarten,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            nachname(spieler.name),
            style = MaterialTheme.typography.labelMedium.copy(shadow = spielerTextSchatten),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            spieler.verein,
            style = MaterialTheme.typography.labelSmall.copy(shadow = spielerTextSchatten),
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Bewertungs-Pille oben rechts auf dem Avatar (dunkel, immer lesbar). */
@Composable
private fun BewertungsBadge(
    bewertung: Double,
    modifier: Modifier = Modifier,
) {
    Text(
        ElfBewertung.formatiere(bewertung),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC000000))
            .border(0.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 3.dp, vertical = 0.5.dp),
        style = MaterialTheme.typography.labelSmall.copy(shadow = spielerTextSchatten),
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

/** Kapitäns-Binde (C) oben links auf dem Avatar. */
@Composable
private fun KapitänBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(Color(0xCC000000))
            .border(0.5.dp, Color(0xFFFBC02D), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "C",
            style = MaterialTheme.typography.labelSmall.copy(shadow = spielerTextSchatten),
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFBC02D),
            maxLines = 1,
        )
    }
}

/**
 * Aktions-Fach unten links auf dem Avatar: Wechsel-Pfeil (Ein-/Auswechslung)
 * und Gelb-/Rote-Karten. Nur sichtbar, wenn der Bericht dafür Hinweise liefert.
 */
@Composable
private fun AktionenBadge(
    wechsel: Boolean,
    gelbeKarten: Int,
    roteKarten: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xB3000000))
            .padding(horizontal = 2.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (wechsel) {
            Icon(
                Icons.Filled.SwapVert,
                contentDescription = "Ein-/Auswechslung",
                tint = Color.White,
                modifier = Modifier.size(9.dp),
            )
        }
        repeat(gelbeKarten) {
            KartenBadge(gelb = true)
        }
        repeat(roteKarten) {
            KartenBadge(gelb = false)
        }
    }
}

/** Miniatur-Karte (Gelb oder Rot) für das Aktions-Fach. */
@Composable
private fun KartenBadge(gelb: Boolean) {
    Box(
        Modifier
            .size(width = 4.5.dp, height = 6.5.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(if (gelb) Color(0xFFFBC02D) else Color(0xFFE53935))
            .border(0.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(1.dp)),
    )
}

/** Dezenter dunkler Schatten, damit Texte auf dem Rasen immer lesbar bleiben. */
private val spielerTextSchatten = Shadow(
    color = Color(0x99000000),
    offset = Offset(0f, 1f),
    blurRadius = 3f,
)

/** In-Memory-Cache der geladenen Wappen je Team-ID (spart Wiederholungen über den Spieltag). */
private val wappenCache = java.util.concurrent.ConcurrentHashMap<Long, ImageBitmap>()

/**
 * Lädt ein Vereinswappen: probiert die Kandidaten-URLs der Team-ID in
 * Reihenfolge png → gif → jpg durch (der Server speichert verschiedene
 * Vereine unter verschiedenen Endungen) und nimmt das erste lesbare Bild.
 * GegenHTTP-403 setzt der Request den [OsApi.USER_AGENT] – genau wie der
 * Rest der App. Fehlt die Team-ID oder schlägt alles fehl, wird null geliefert.
 */
private suspend fun ladeVereinsWappen(teamId: Long): ImageBitmap? = withContext(Dispatchers.IO) {
    wappenCache[teamId] ?: run {
        val bild = OsApi.wappenUrls(teamId).firstNotNullOfOrNull { url ->
            runCatching {
                val verbindung = URL(url).openConnection() as HttpURLConnection
                try {
                    verbindung.connectTimeout = 8_000
                    verbindung.readTimeout = 8_000
                    verbindung.setRequestProperty("User-Agent", OsApi.USER_AGENT)
                    verbindung.inputStream.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                } finally {
                    verbindung.disconnect()
                }
            }.getOrNull()
        }
        bild?.also { wappenCache[teamId] = it }
    }
}

/**
 * Vereinswappen der Elf: lädt das Bild wie das Dashboard über die Team-ID
 * (Endungs-Fallback png/gif/jpg, BitmapFactory). Steht keine Team-ID (oder
 * XML) zur Verfügung oder schlägt das Laden fehl, wird ein dezenter Kreis
 * mit der Vereinsinitialen gezeigt – ohne zusätzliche Karte oder Rahmen.
 */
@Composable
private fun VereinsWappen(
    teamId: Long?,
    verein: String,
    size: Dp,
) {
    val anfang = teamId?.let(wappenCache::get)
    val wappen by produceState<ImageBitmap?>(initialValue = anfang, teamId) {
        value = if (teamId == null) null else ladeVereinsWappen(teamId)
    }
    val bild = wappen
    if (bild != null) {
        Image(
            bitmap = bild,
            contentDescription = "Vereinswappen $verein",
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(wappenPlatzhalterFarbe),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initiale(verein),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = wappenPlatzhalterTextFarbe,
                maxLines = 1,
            )
        }
    }
}

/** Letzter Namensbestandteil (Nachname) für die Markierung. */
private fun nachname(name: String): String {
    val getrimmt = name.trim()
    val letzte = getrimmt.substringAfterLast(' ')
    return letzte.ifEmpty { getrimmt }
}

/** Erste sichtbare Zeichen der Vereinsbezeichnung (für den Wappen-Platzhalter). */
private fun initiale(verein: String): String =
    verein.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "–"

/**
 * Spielfeld-Darstellung der Elf: grüner Rasen mit Linien und darüber die
 * Spieler als saubere, taktische Formation. Jeder Spieler erhält eine
 * EIGENE X- und Y-Position – Spieler einer Positionsgruppe liegen auf einer
 * gemeinsamen horizontalen Linie (wie auf einem echten taktischen Board),
 * nur die Halbzeit-Trennlinie hebt die vorderen Gruppen weiter oben an.
 * Die Positionierung ist proportional zur Feldbreite und -höhe und damit auf
 * allen Gerätegrößen identisch. Die Markierungen werden mit ihrer echten
 * Höhe gemessen (Avatar mit Overlays, Nachname, Vereinsname vollständig) und
 * die anschließende Kollisionsauflösung verhindert, dass sich vollständige
 * Spielerelemente jemals überlappen.
 */
@Composable
private fun Spielfeld(
    ergebnis: ElfErgebnis,
    onSpielerKlick: (ElfSpieler) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(rasenFarbe),
    ) {
        // Feldlinien nach Position (0 = Sturm oben … 3 = Torwart unten).
        val zeilen = ergebnis.spieler.groupBy { ElfAuswahl.feldZeile(it.position) }
        val linien = zeilen.keys.sorted().mapNotNull { index ->
            zeilen[index].orEmpty().takeIf { it.isNotEmpty() }?.let { SpielerLine(index, it) }
        }
        // Zeilen-Anker flexibel: besetzte Zonen werden zwischen SPIELFELD_OBEN
        // und SPIELFELD_UNTEN gleichmäßig verteilt (auch bei „unvollen" Formationen).
        val ankerY = zeilenAnkerY(linien.size)

        val density = LocalDensity.current
        val randPx = with(density) { (maxWidth * SPIELFELD_RAND_ANTEIL).roundToPx() }
        val abstandPx = with(density) { SPIELER_ABSTAND.roundToPx() }
        val minSlotPx = with(density) { MIN_SLOT_BREITE.roundToPx() }
        val maxSlotPx = with(density) { MAX_SLOT_BREITE.roundToPx() }
        // Das Wappen ist fest dimensioniert und ruht damit auf jedem Gerät in
        // derselben Größe auf dem Rasen. Die Elemente tragen ihre echte Höhe,
        // sodass Vereinsname & Co. nie abgeschnitten werden.
        val wappenGroesse = WAPPEN_GROESSE

        Canvas(Modifier.fillMaxSize()) { zeichneSpielfeld() }
        SpielfeldSpieler(
            linien = linien,
            ankerY = ankerY,
            minSlotPx = minSlotPx,
            maxSlotPx = maxSlotPx,
            randPx = randPx,
            abstandPx = abstandPx,
            wappenGroesse = wappenGroesse,
            onSpielerKlick = onSpielerKlick,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Maximale Höhe der kompletten Spielermarkierung (Avatar mit Overlays, Nachname,
 * Vereinsname). Die Elemente aller besetzten Reihen müssen gemeinsam
 * in der nutzbaren Feldhöhe Platz finden – der Stagger-Puffer (Faktor 1,15)
 * lässt dabei genug Spielraum, dass sich Reihen leicht versetzen dürfen, ohne
 * dass sich Markierungen berühren. Die Untergrenze ist bewusst niedrig, damit
 * auch klassische Formationen mit vielen Linien (4-2-3-1, 4-4-2 Raute: 5-6
 * Reihen) auf kleinen Querformats-Geräten überlappungsfrei bleiben.
 */
internal fun formationElementMaxHoeheDp(feldHoeheDp: Float, anzahlZeilen: Int): Float {
    if (anzahlZeilen <= 0) return MAX_SLOT_BREITE.value
    val nutzbar = (SPIELFELD_UNTEN - SPIELFELD_OBEN).coerceAtLeast(0.5f)
    // Der Faktor 1,45 reserviert den Stagger-Spielraum, damit sich benachbarte
    // Reihen diagonal versetzen können, ohne die Feldgrenzen zu verlassen.
    val hoehe = nutzbar * feldHoeheDp / (anzahlZeilen * 1.45f)
    return hoehe.coerceIn(36f, 150f)
}

/**
 * Gleichmäßige Vertikal-Anker für [anzahlZeilen] besetzte Zeilen im Feld.
 */
internal fun zeilenAnkerY(anzahlZeilen: Int): FloatArray = when {
    anzahlZeilen <= 1 -> floatArrayOf(0.5f)
    else -> FloatArray(anzahlZeilen) { i ->
        SPIELFELD_OBEN + (SPIELFELD_UNTEN - SPIELFELD_OBEN) * i / (anzahlZeilen - 1)
    }
}

/**
 * Verteilt die Spieler über das Spielfeld. Jeder Spieler bekommt eine eigene
 * X- und Y-Koordinate: Die X-Positionen stammen aus den natürlichen Fraktionen
 * der Gruppe (offene Flügel, dichtes Zentrum), die Y-Position je Positionsgruppe
 * liegt auf [ankerY] – alle Spieler einer Gruppe teilen sich dieselbe horizontale
 * Linie. Die Elemente werden mit ihrer ECHTEN Höhe gemessen (Avatar mit Overlays,
 * Nachname, Vereinsname vollständig – nichts wird abgeschnitten). Danach werden
 * eventuelle Überlappungen aufgelöst – eine Markierung wird vertikal so weit nach
 * unten geschoben, bis sie kein anderes vollständiges Element mehr berührt. Das
 * Ergebnis ist deterministisch und proportional zur Feldgröße.
 */
@Composable
private fun SpielfeldSpieler(
    linien: List<SpielerLine>,
    ankerY: FloatArray,
    minSlotPx: Int,
    maxSlotPx: Int,
    randPx: Int,
    abstandPx: Int,
    wappenGroesse: Dp,
    onSpielerKlick: (ElfSpieler) -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            linien.forEach { linie ->
                linie.spieler.forEach { spieler ->
                    key(spieler.spielerId, spieler.name, spieler.verein) {
                        ElfSpielerMarkierung(spieler, wappenGroesse, onClick = { onSpielerKlick(spieler) })
                    }
                }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight

        // Slot-Breite je Positionsgruppe aus den natürlichen X-Fraktionen – so
        // können Spieler derselben Gruppe nie horizontal kollidieren.
        val slotBreiten = IntArray(linien.size) { i ->
            slotBreiteFuerLinie(linien[i].spieler.size, w, randPx, abstandPx, minSlotPx, maxSlotPx)
        }

        val gemessen = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var index = 0
        linien.forEachIndexed { zeile, linie ->
            val maxW = slotBreiten[zeile]
            linie.spieler.forEach { _ ->
                gemessen += measurables[index].measure(
                    // Nur die Breite wird begrenzt – die Höhe bleibt frei, damit
                    // die Markierung in voller Höhe (inkl. Vereinsname) gemessen
                    // und die Kollisionsauflösung mit der echten Größe rechnet.
                    Constraints(maxWidth = maxW),
                )
                index++
            }
        }

        val breiten = gemessen.map { it.width }
        val hoehen = gemessen.map { it.height }
        val plaetze = berechneFormationPlaetze(
            linien = linien,
            ankerY = ankerY,
            breite = w,
            hoehe = h,
            randPx = randPx,
            abstandPx = abstandPx,
            slotBreiten = slotBreiten,
            elementBreiten = breiten,
            elementHoehen = hoehen,
        )

        layout(w, h) {
            gemessen.forEachIndexed { i, karte ->
                val platz = plaetze[i]
                karte.place(platz.x - karte.width / 2, platz.y - karte.height / 2)
            }
        }
    }
}

/**
 * Maximal zulässige Elementbreite einer Positionsgruppe. Die natürlichen
 * X-Fraktionen werden auf die nutzbare Breite (nach [randPx]-Randabstand)
 * skaliert – der Slot ist der kleinste Abstand benachbarter Zentren minus
 * Mindestabstand und zugleich durch den Randreserve begrenzt. Damit können
 * Spieler derselben Gruppe nie horizontal kollidieren.
 */
internal fun slotBreiteFuerLinie(
    anzahl: Int,
    breite: Int,
    randPx: Int,
    abstandPx: Int,
    minSlotPx: Int,
    maxSlotPx: Int,
): Int {
    if (anzahl <= 0) return 0
    if (anzahl == 1) return (breite * 0.4f).roundToInt().coerceIn(minSlotPx, maxSlotPx)
    val nutzbar = (breite - randPx * 2).coerceAtLeast(1)
    val fraktionen = natuerlicheXFraktionen(anzahl)
    val kleinsterAbstand = (1 until anzahl).minOf { fraktionen[it] - fraktionen[it - 1] }
    val randReserve = (2 * nutzbar * minOf(fraktionen.first(), 1f - fraktionen.last())).toInt()
    val slot = minOf(
        (kleinsterAbstand * nutzbar - abstandPx).roundToInt(),
        randReserve,
    )
    return slot.coerceIn(minSlotPx, maxSlotPx)
}

/**
 * Natürliche X-Positionen einer Positionsgruppe (Anteile 0..1 der Breite):
 * die Flügel stehen breit, die zentrale Achse bleibt besetzt – das ergibt das
 * offene taktische Aussehen statt starrer Tabellenzeilen.
 */
internal fun natuerlicheXFraktionen(anzahl: Int): FloatArray = when (anzahl) {
    1 -> floatArrayOf(0.5f)
    2 -> floatArrayOf(0.36f, 0.64f)
    3 -> floatArrayOf(0.18f, 0.5f, 0.82f)
    4 -> floatArrayOf(0.16f, 0.40f, 0.60f, 0.84f)
    5 -> floatArrayOf(0.12f, 0.31f, 0.50f, 0.69f, 0.88f)
    6 -> floatArrayOf(0.10f, 0.27f, 0.44f, 0.56f, 0.73f, 0.90f)
    else -> FloatArray(anzahl) { (it + 0.5f) / anzahl }
}

/**
 * Zielkoordinaten (Zentrum) eines Spielerelements in Pixeln.
 */
internal data class FormationPlatz(val x: Int, val y: Int)

/**
 * Berechnet die Zentrumspositionen aller Spieler – jeder Spieler erhält seine
 * eigene X- UND Y-Koordinate. Grundlage sind die natürlichen X-Fraktionen der
 * Gruppe und eine gemeinsame Y-Linie je Positionsgruppe. Danach werden
 * Kollisionen aufgelöst: Eine Markierung wird – falls nötig – vertikal
 * nach unten geschoben, bis ihr komplettes Element (Avatar mit Overlays,
 * Nachname, Vereinsname) kein anderes Element mehr berührt.
 */
internal fun berechneFormationPlaetze(
    linien: List<SpielerLine>,
    ankerY: FloatArray,
    breite: Int,
    hoehe: Int,
    randPx: Int,
    abstandPx: Int,
    slotBreiten: IntArray,
    elementBreiten: List<Int>,
    elementHoehen: List<Int>,
): List<FormationPlatz> {
    // Zielpositionen in derselben Reihenfolge, in der die Markierungen emittiert werden.
    val ziele = mutableListOf<FormationPlatz>()
    var idx = 0
    val nutzbareBreite = (breite - randPx * 2).coerceAtLeast(1)
    linien.forEachIndexed { zeile, linie ->
        val n = linie.spieler.size
        if (n == 0) return@forEachIndexed
        val mitteY = ankerY[zeile] * hoehe
        val fraktionen = natuerlicheXFraktionen(n)
        linie.spieler.forEachIndexed { j, _ ->
            ziele += FormationPlatz(
                x = (randPx + nutzbareBreite * fraktionen[j]).roundToInt(),
                y = mitteY.roundToInt(),
            )
            idx++
        }
    }

    val ergebnis = MutableList<FormationPlatz?>(ziele.size) { null }
    val gelegt = mutableListOf<PlatzRechteck>()
    val pad = (abstandPx / 2).coerceAtLeast(1)
    idx = 0
    linien.forEachIndexed { zeile, linie ->
        val cnt = linie.spieler.size
        if (cnt == 0) return@forEachIndexed
        for (j in 0 until cnt) {
            val g = idx + j
            val bw = elementBreiten[g]
            val bh = elementHoehen[g]
            val xMin = randPx + bw / 2
            val xMax = (breite - randPx - bw / 2).coerceAtLeast(xMin)
            val cx = ziele[g].x.coerceIn(xMin, xMax)
            var cy = ziele[g].y
            val cyMin = randPx + bh / 2
            val cyMax = (hoehe - randPx - bh / 2).coerceAtLeast(cyMin)
            while (cy <= cyMax && gelegt.any { schneidet(it, cx, cy, bw, bh, pad) }) {
                cy++
            }
            cy = cy.coerceIn(cyMin, cyMax)
            ergebnis[g] = FormationPlatz(cx, cy)
            gelegt += PlatzRechteck(
                links = cx - bw / 2,
                oben = cy - bh / 2,
                rechts = cx + (bw + 1) / 2,
                unten = cy + (bh + 1) / 2,
            )
        }
        idx += cnt
    }

    // Zweite Phase von unten nach oben: Der Torwart steht am unteren Feldrand und
    // kann nicht weiter nach unten rutschen. Deshalb wird jede darüber liegende
    // Positionsgruppe als Ganzes so weit nach oben gehoben (nur so weit wie nötig),
    // bis ihre kompletten Elemente alle tiefer liegenden Elemente frei lassen.
    // Die gemeinsame horizontale Linie der Gruppe bleibt dabei vollständig erhalten.
    val gelegt2 = mutableListOf<PlatzRechteck>()
    var b = ziele.size
    for (zeile in (0 until linien.size).reversed()) {
        val cnt = linien[zeile].spieler.size
        if (cnt == 0) continue
        b -= cnt
        var baseline = ankerY[zeile] * hoehe
        if (gelegt2.isNotEmpty()) {
            // Gültigkeits-Korridor der Gruppenlinie: Sie wird höchstens so weit
            // angehoben, dass jeder Spieler komplett über den horizontal
            // überlappenden, bereits platzierten Elementen liegt, aber nie weiter
            // herunter, als das Feld es zulässt.
            var minBaseline = Float.NEGATIVE_INFINITY
            var maxBaseline = Float.POSITIVE_INFINITY
            for (j in 0 until cnt) {
                val g = b + j
                val bw = elementBreiten[g]
                val bh = elementHoehen[g]
                val cx = ziele[g].x.coerceIn(
                    randPx + bw / 2,
                    (breite - randPx - bw / 2).coerceAtLeast(randPx + bw / 2),
                )
                val unten = gelegt2.filter {
                    it.links < cx + bw / 2 && it.rechts > cx - bw / 2
                }
                val freieTiefe = unten.minOfOrNull { it.oben - pad } ?: hoehe
                val erlaubt = minOf(
                    freieTiefe - bh / 2,
                    hoehe - randPx - bh / 2,
                ).toFloat()
                minBaseline = maxOf(minBaseline, (randPx + bh / 2).toFloat())
                maxBaseline = minOf(maxBaseline, erlaubt)
            }
            baseline = if (maxBaseline < minBaseline) {
                // Kein gültiger Korridor (extrem kleine Geräte): tiefer liegende
                // Elemente haben Vorrang, damit nichts überlagert wird.
                maxBaseline
            } else {
                baseline.coerceIn(minBaseline, maxBaseline)
            }
        }
        for (j in 0 until cnt) {
            val g = b + j
            val bw = elementBreiten[g]
            val bh = elementHoehen[g]
            val cx = ziele[g].x.coerceIn(
                randPx + bw / 2,
                (breite - randPx - bw / 2).coerceAtLeast(randPx + bw / 2),
            )
            val cy = baseline.roundToInt().coerceIn(
                randPx + bh / 2,
                (hoehe - randPx - bh / 2).coerceAtLeast(randPx + bh / 2),
            )
            ergebnis[g] = FormationPlatz(cx, cy)
            gelegt2 += PlatzRechteck(
                links = cx - bw / 2,
                oben = cy - bh / 2,
                rechts = cx + (bw + 1) / 2,
                unten = cy + (bh + 1) / 2,
            )
        }
    }
    return ergebnis.mapNotNull { it }
}

/** Rechteck eines bereits platzierten Spielerelements (in Pixeln). */
private data class PlatzRechteck(
    val links: Int,
    val oben: Int,
    val rechts: Int,
    val unten: Int,
)

/**
 * Prüft, ob eine Markierung der Breite [bw]/Höhe [bh] um das Zentrum (cx, cy)
 * mit dem bereits platzierten Rechteck kollidiert (inklusive [pad]-Abstand).
 */
private fun schneidet(r: PlatzRechteck, cx: Int, cy: Int, bw: Int, bh: Int, pad: Int): Boolean {
    val links = cx - bw / 2 - pad
    val rechts = cx + (bw + 1) / 2 + pad
    val oben = cy - bh / 2 - pad
    val unten = cy + (bh + 1) / 2 + pad
    return links < r.rechts && rechts > r.links && oben < r.unten && unten > r.oben
}

/**
 * Übersichtsdialog „Bewertungsdetails anzeigen": alle Spieler der Elf mit
 * ihrer Note; ein Tipp öffnet die Einzelaufschlüsselung des Spielers.
 */
@Composable
private fun BewertungsUebersichtDialog(
    ergebnis: ElfErgebnis,
    onSpielerKlick: (ElfSpieler) -> Unit,
    onSchliessen: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onSchliessen,
        title = { Text("Bewertungsdetails") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Alle Spieler der Elf mit ihrer Bewertung auf der Skala 1,0–10,0. " +
                        "Tippe auf einen Spieler für die nachvollziehbare Berechnung.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                ergebnis.spieler.forEach { spieler ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSpielerKlick(spieler) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PositionsBadge(spieler.position)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                spieler.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                spieler.verein,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${ElfBewertung.formatiere(spieler.bewertung)} / 10",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSchliessen) { Text("Schließen") }
        },
    )
}

/**
 * Detaildialog eines Spielers: Gesamtnote und die transparente Aufschlüsselung
 * der Kriterien (nur wirklich berichtete Werte erscheinen) sowie die Option,
 * die vorhandene Spielerkarte der App zu öffnen (nur mit echter Spieler-ID).
 */
@Composable
private fun BewertungsDetailDialog(
    spieler: ElfSpieler,
    onSchliessen: () -> Unit,
    onSpielerkarte: () -> Unit,
    onVerein: () -> Unit,
) {
    val bewertung = remember(spieler) { ElfBewertung.bewerten(ElfBewertung.kandidatVon(spieler)) }
    val nameKlickbar = spieler.spielerId != null
    val vereinKlickbar = spieler.teamId != null
    val nameFarbe = if (nameKlickbar) MaterialTheme.colorScheme.primary else Color.Unspecified
    val vereinFarbe = if (vereinKlickbar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = onSchliessen,
        title = {
            Column {
                Text(
                    "⭐ Spieltagsbewertung",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    spieler.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = nameFarbe,
                    modifier = Modifier
                        .clickable(enabled = nameKlickbar, onClick = onSpielerkarte)
                        .padding(vertical = 2.dp),
                )
                Text(
                    spieler.verein,
                    style = MaterialTheme.typography.labelLarge,
                    color = vereinFarbe,
                    modifier = Modifier.clickable(enabled = vereinKlickbar, onClick = onVerein),
                )
            }
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PositionsBadge(spieler.position)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${ElfBewertung.formatiere(bewertung.gesamt)} / 10",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    einsatzText(spieler),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))

                Column(Modifier.verticalScroll(rememberScrollState())) {
                    bewertung.zeilen.forEach { zeile ->
                        BewertungsZeileRow(zeile)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Gesamt",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            ElfBewertung.formatiere(bewertung.gesamt),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (!nameKlickbar) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Der Spielbericht liefert keine Spieler-ID – die Spielerkarte kann nicht geöffnet werden.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSchliessen) { Text("Schließen") }
        },
    )
}

/** Kurzer, ehrlicher Einsatzzeit-Hinweis (nur berichtete Daten sonst Hinweis). */
private fun einsatzText(spieler: ElfSpieler): String = when {
    spieler.minuten != null -> "Einsatz: ${spieler.minuten} Minuten"
    spieler.startelf -> "Einsatz: Startelf (volle Partie)"
    else -> "Einsatz: eingewechselt, Dauer im Bericht nicht genannt"
}

/** Eine Kriterienzeile der Bewertungsaufschlüsselung (Kriterium + Beitrag). */
@Composable
private fun BewertungsZeileRow(zeile: BewertungsZeile) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            zeile.kriterium,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            ElfBewertung.formatiereBeitrag(zeile.beitrag),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Zeichnet ein Fußballfeld (20%-Spielfeld mit Mittelkreis, Straf- und Torräumen). */
private fun DrawScope.zeichneSpielfeld() {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    drawRect(rasenFarbe)
    val lw = 2.dp.toPx()
    val stroke = Stroke(width = lw)

    // Außenlinien
    drawRoundRect(
        color = linienFarbe,
        topLeft = Offset(lw / 2f, lw / 2f),
        size = Size(w - lw, h - lw),
        style = stroke,
    )
    // Mittellinie + Mittelkreis
    drawLine(linienFarbe, Offset(0f, h / 2f), Offset(w, h / 2f), lw)
    drawCircle(
        linienFarbe,
        radius = min(w * 0.14f, h * 0.22f),
        center = Offset(w / 2f, h / 2f),
        style = stroke,
    )
    // Strafräume oben und unten
    val paH = h * 0.16f
    val paW = w * 0.62f
    drawRect(linienFarbe, topLeft = Offset(w / 2f - paW / 2f, 0f), size = Size(paW, paH), style = stroke)
    drawRect(linienFarbe, topLeft = Offset(w / 2f - paW / 2f, h - paH), size = Size(paW, paH), style = stroke)
    // Torräume oben und unten
    val gaH = h * 0.075f
    val gaW = w * 0.36f
    drawRect(linienFarbe, topLeft = Offset(w / 2f - gaW / 2f, 0f), size = Size(gaW, gaH), style = stroke)
    drawRect(linienFarbe, topLeft = Offset(w / 2f - gaW / 2f, h - gaH), size = Size(gaW, gaH), style = stroke)
}