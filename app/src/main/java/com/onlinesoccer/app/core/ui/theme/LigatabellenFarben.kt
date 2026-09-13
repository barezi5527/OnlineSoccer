package com.onlinesoccer.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.onlinesoccer.app.data.model.LigaTabellenKlasse

/**
 * Zentrale Tabellenplatzfarben der Ligatabelle – eine Quelle für alle Ansichten.
 * Die Bedeutung je Tabellenplatz kommt ausschließlich vom Server: Er markiert jede
 * Tabellenzeile mit der für die jeweilige Liga geltenden Platzbedeutung (`lt.php`,
 * CSS-Klassen `osc`/`oscq`/`ose`/`oseq`/`rele`/`ab`). Diese Datei bildet nur die
 * dort vorgegebenen Farbtöne (`os_styles.css`) auf dezent eingefärbte Zeilen ab.
 *
 * Diese Farben gehören ausschließlich zur Ligatabelle und stehen bewusst getrennt
 * von den Spielerpositionsfarben (s. `SpielerFarben.kt`).
 */

/** Basisfarbtöne der Platzbedeutungen – identisch zur Website (`os_styles.css`). */
private val ligatabellenBasisFarben: Map<LigaTabellenKlasse, Color> = mapOf(
    LigaTabellenKlasse.OSC to Color(0xFF006400), // darkgreen
    LigaTabellenKlasse.OSCQ to Color(0xFF6B8E23), // olivedrab
    LigaTabellenKlasse.OSE to Color(0xFF556B2F), // darkolivegreen
    LigaTabellenKlasse.OSEQ to Color(0xFF8B864E), // rgb(139,134,78)
    LigaTabellenKlasse.RELE to Color(0xFFAA5656), // rgb(170,86,86)
    LigaTabellenKlasse.AB to Color(0xFFB22222), // rgb(178,34,34)
)

/** Intensität der Platzfarbe im Light Mode (~15 % – sehr dezent, Text bleibt lesbar). */
private const val LICHT_INTENSITAET = 0.15f

/** Intensität der Platzfarbe im Dunkelmodus – erkennbar, aber dunkler/dezenter als der Light Mode. */
private const val DUNKEL_INTENSITAET = 0.40f

/**
 * Zeilenhintergrund der Ligatabelle: dezent (Light ~15 %), Dunkelmodus dunkler und
 * dezent (~12 % gegen die dunkle Oberfläche) – nie kräftige Farbflächen.
 *
 * Die Basis ist immer die tatsächliche Theme-Oberfläche, damit der Farbton zum
 * App-Hintergrund passt (auch bei dynamischen Farben).
 */
@Composable
internal fun ligaTabellenZeilenHintergrund(klasse: LigaTabellenKlasse?): Color {
    val basis = klasse?.let { ligatabellenBasisFarben[it] } ?: return Color.Transparent
    val dunklerModus = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val intensitaet = if (dunklerModus) DUNKEL_INTENSITAET else LICHT_INTENSITAET
    return lerp(MaterialTheme.colorScheme.surface, basis, intensitaet)
}