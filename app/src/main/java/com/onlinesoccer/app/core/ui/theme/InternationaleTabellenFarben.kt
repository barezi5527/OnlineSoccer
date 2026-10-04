package com.onlinesoccer.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.onlinesoccer.app.data.model.InternationaleTabelleKlasse

/**
 * Tabellenplatzfarben der internationalen Wettbewerbe (OSC-/OSE-Gruppenphase).
 *
 * Bedeutung und Farbe kommen ausschließlich vom Server: Er markiert jede Zeile der
 * `table.intTabelle` mit `top`, `middle` oder `bottom` und färbt sie in
 * `osneu/css/style.css` mit `darkgreen`, `rgb(139,134,78)` und `rgb(178,34,34)`.
 * Die App bildet dieselben Töne – wie bei der Ligatabelle – dezent auf den
 * Zeilenhintergrund ab, damit der Text lesbar bleibt.
 */

/** Basisfarbtöne der internationalen Platzklassen – identisch zur Website. */
private val internationaleBasisFarben: Map<InternationaleTabelleKlasse, Color> = mapOf(
    InternationaleTabelleKlasse.TOP to Color(0xFF006400), // darkgreen
    InternationaleTabelleKlasse.MITTEL to Color(0xFF8B864E), // rgb(139,134,78)
    InternationaleTabelleKlasse.UNTEN to Color(0xFFB22222), // rgb(178,34,34)
)

/** Intensität wie in der Ligatabelle: dezent im Light Mode, etwas kräftiger im Dunkelmodus. */
private const val INTENSITAET_LICHT = 0.15f
private const val INTENSITAET_DUNKEL = 0.40f

/** Zeilenhintergrund einer internationalen Tabellenzeile (`null` = neutral). */
@Composable
internal fun internationaleTabellenZeilenHintergrund(
    klasse: InternationaleTabelleKlasse?,
): Color {
    val basis = klasse?.let { internationaleBasisFarben[it] } ?: return Color.Transparent
    val dunklerModus = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val intensitaet = if (dunklerModus) INTENSITAET_DUNKEL else INTENSITAET_LICHT
    return lerp(MaterialTheme.colorScheme.surface, basis, intensitaet)
}