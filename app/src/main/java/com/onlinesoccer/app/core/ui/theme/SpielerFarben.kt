package com.onlinesoccer.app.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlinesoccer.app.data.model.SpielerPosition
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

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
    "MIT", "MITTELFELD", "MF" -> SpielerPosition.MIT
    "OMI", "OFF. MITTELFELD", "OFFENSIVES MITTELFELD" -> SpielerPosition.OMI
    "STU", "STURM", "STÜRMER", "ST" -> SpielerPosition.STU
    else -> null
}

/** WCAG-Relativ-Luminanz einer Farbe (nur für den Kontrastvergleich genutzt). */
private fun Color.luminanz(): Double {
    fun linear(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)
}

/** WCAG-Kontrastverhältnis zweier Farben (1:1 … 21:1). */
private fun kontrastVerhaeltnis(a: Color, b: Color): Double {
    val la = a.luminanz()
    val lb = b.luminanz()
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

/**
 * Liest die Textfarbe für ein Positions-Kästchen anhand des Hintergrunds aus.
 * Nutzt Weiß oder das bestehende KACHEL_TEXT und wählt den besseren Kontrast –
 * dadurch bleiben auch helle Positionsfarben (Gelb, Hellblau) gut lesbar.
 */
internal fun kastenTextFarbe(hintergrund: Color): Color {
    val weiss = Color.White
    val dunkel = KACHEL_TEXT
    return if (kontrastVerhaeltnis(hintergrund, weiss) >= kontrastVerhaeltnis(hintergrund, dunkel)) {
        weiss
    } else {
        dunkel
    }
}

/**
 * Feste Statusfarben für Badges auf den Spielerkacheln (Transferliste/Transfermarkt).
 * Der helle Pill-Hintergrund arbeitet in beiden Modi; [KACHEL_TEXT] dient außerdem als
 * Kontrast-Basisschwarz für [kastenTextFarbe].
 */
internal val KACHEL_TEXT = Color(0xFF191C19)
internal val KACHEL_BADGE_HINTERGRUND = Color(0xFFDDE5DE)
internal val KACHEL_BADGE_TEXT = Color(0xFF081F13)

/** Positionskürzel für das Kästchen (z. B. „TOR“, „ABW“). */
internal fun positionsKuerzel(position: SpielerPosition): String = when (position) {
    SpielerPosition.TOR -> "TOR"
    SpielerPosition.ABW -> "ABW"
    SpielerPosition.DMI -> "DMI"
    SpielerPosition.MIT -> "MIT"
    SpielerPosition.OMI -> "OMI"
    SpielerPosition.STU -> "STU"
    SpielerPosition.AMATEUR -> "A"
}

/**
 * Kleines Positionskästchen – eine Komponente für alle Bereiche der App
 * (Team/ZAT/Teamformationen-Statistiken/Transfer): Hintergrund = [trikotFarbe],
 * Text = das Positionskürzel mit automatisch gewähltem Kontrast.
 */
@Composable
internal fun PositionsBadge(position: SpielerPosition) {
    PositionsBadge(positionsKuerzel(position))
}

@Composable
internal fun PositionsBadge(positionText: String) {
    val text = positionText.trim().uppercase()
    if (text.isEmpty()) return
    val hintergrund = trikotFarbe(positionVonText(positionText) ?: SpielerPosition.AMATEUR)
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(hintergrund)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = kastenTextFarbe(hintergrund),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}