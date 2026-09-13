package com.onlinesoccer.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.onlinesoccer.app.data.model.SpielerPosition

/**
 * Zentrale Spielerpositionsfarben – eine Quelle für alle Bereiche der App.
 * TW, ABW, DMI, MIT, OMI und STU werden überall identisch dargestellt.
 */
internal fun trikotFarbe(position: SpielerPosition): Color = when (position) {
    SpielerPosition.TOR -> Color(0xFFF9A825)
    SpielerPosition.ABW -> Color(0xFF43A047)
    SpielerPosition.DMI -> Color(0xFF1E88E5)
    SpielerPosition.MIT -> Color(0xFF36BFF9)
    SpielerPosition.OMI -> Color(0xFFE040FB)
    SpielerPosition.STU -> Color(0xFFE53935)
    SpielerPosition.AMATEUR -> Color(0xFF9E9E9E)
}

/** Ordnet einen Positions-Text (Kürzel oder Name) der zentralen Position zu. */
internal fun positionVonText(text: String): SpielerPosition? = when (text.trim().uppercase()) {
    "TW", "TOR", "TORWART", "TORHÜTER" -> SpielerPosition.TOR
    "ABW", "ABWEHR" -> SpielerPosition.ABW
    "DMI", "DEF. MITTELFELD", "DEFENSIVES MITTELFELD" -> SpielerPosition.DMI
    "MIT", "MITTELFELD" -> SpielerPosition.MIT
    "OMI", "OFF. MITTELFELD", "OFFENSIVES MITTELFELD" -> SpielerPosition.OMI
    "STU", "STURM", "STÜRMER" -> SpielerPosition.STU
    else -> null
}

/** Helle Basis-Oberfläche, gegen die die Kacheltönung immer gemischt wird (fester Farbton). */
private val HELLE_OBERFLAECHE = Color(0xFFF8FAF8)

/**
 * Feste dunkle Textfarben für die Spielerkacheln (Transferliste/Transfermarkt).
 * Die Kachelhintergründe sind dezent pastellfarben – der Text ist deshalb in beiden
 * Modi schwarz/dunkel, unabhängig vom Theme.
 */
internal val KACHEL_TEXT = Color(0xFF191C19)
internal val KACHEL_TEXT_SEKUNDAER = Color(0xFF414942)
internal val KACHEL_TEXT_AKZENT = Color(0xFF0B6E3B)
internal val KACHEL_BADGE_HINTERGRUND = Color(0xFFDDE5DE)
internal val KACHEL_BADGE_TEXT = Color(0xFF081F13)

/** Dunkelmodus: Kachelfarbe nur leicht abdunkeln – Farbton bleibt identisch. */
private const val DUNKELMODUS_FAKTOR = 0.85f

/** Grober Helligkeitscheck des Hintergrunds (dunkles Farbschema = Dunkelmodus). */
private fun Color.istDunklerModus(): Boolean = (red + green + blue) / 3f < 0.5f

/** Gleiche Farbe, aber nur leicht abgedunkelt (RGB-Formel, Farbton bleibt erhalten). */
private fun Color.leichtAbdunkeln(faktor: Float): Color =
    Color(red = red * faktor, green = green * faktor, blue = blue * faktor, alpha = alpha)

/**
 * Sehr dezent eingefärbte Kachel für Transferliste/Transfermarkt (~15 % Positionsfarbe).
 * Die Kacheltönung wird immer gegen eine feste helle Oberfläche gemischt, damit der
 * Farbton in beiden Modi identisch bleibt. Im Dunkelmodus wird sie nur leicht
 * abgedunkelt dargestellt.
 */
@Composable
internal fun positionsKachelFarbe(positionsText: String): Color {
    val position = positionVonText(positionsText) ?: return Color.Transparent
    val hell = lerp(HELLE_OBERFLAECHE, trikotFarbe(position), 0.15f)
    return if (MaterialTheme.colorScheme.background.istDunklerModus()) {
        hell.leichtAbdunkeln(DUNKELMODUS_FAKTOR)
    } else {
        hell
    }
}