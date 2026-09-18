package com.onlinesoccer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.onlinesoccer.app.data.model.BerichtEreignisTyp
import androidx.compose.runtime.remember

/**
 * Einheitliche Darstellung eines Spielverlauf-Ereignisses (Spielbericht und
 * Live-Spiel). Die Hintergrundfarbe ist absichtlich dezent (~15 % im Light
 * Mode), damit der Text lesbar bleibt; im Dunkelmodus wird sie kräftiger
 * gewählt, da sich sonst nichts von der dunklen Oberfläche abhebt.
 * Tore sind durch ein ⚽-„TOR"-Badge und den fett hervorgehobenen Spielstand
 * eindeutig erkennbar. Bei Karten wird optional der Name des Spielers in der
 * Kartenfarbe (dunkelgelb/rot) hervorgehoben.
 */

/** Intensität der Ereignisfarbe im Light Mode (~15 % – dezent, Text bleibt lesbar). */
private const val LICHT_INTENSITAET = 0.15f

/** Intensität im Dunkelmodus – dunkler Fläche entgegen, aber weiterhin dezent. */
private const val DUNKEL_INTENSITAET = 0.40f

/** Hebt den Spielstand („Neuer Spielstand: 2:1") in Tor-Texten extra fett hervor. */
private val spielstandHighlightRegex = Regex("Neuer Spielstand:\\s*\\d+:\\s*\\d+")

private val BerichtEreignisTyp.isKartenEreignis: Boolean
    get() = this == BerichtEreignisTyp.GELBE_KARTE || this == BerichtEreignisTyp.ROTE_KARTE

/** Kräftige Basistöne je Ereignistyp (Punkt/Badge + Grundlage für den dezenten Hintergrund). */
private val ereignisBasisFarben: Map<BerichtEreignisTyp, Color> = mapOf(
    BerichtEreignisTyp.TOR to Color(0xFF2E7D32),
    BerichtEreignisTyp.GELBE_KARTE to Color(0xFFF9A825),
    BerichtEreignisTyp.ROTE_KARTE to Color(0xFFC62828),
    BerichtEreignisTyp.VERLETZUNG to Color(0xFF7B1FA2),
    BerichtEreignisTyp.ELFMETER to Color(0xFF1565C0),
)

@Composable
private fun ereignisBasis(typ: BerichtEreignisTyp): Color =
    ereignisBasisFarben[typ] ?: MaterialTheme.colorScheme.onSurfaceVariant

/**
 * Hintergrund der Ereignis-Karte: themenabhängige Oberfläche + Basisfarbe mit
 * [LICHT_INTENSITAET] (Light) bzw. [DUNKEL_INTENSITAET] (Dunkelmodus).
 */
@Composable
internal fun ereignisHintergrund(typ: BerichtEreignisTyp): Color {
    val basis = ereignisBasis(typ)
    val dunklerModus = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val intensitaet = if (dunklerModus) DUNKEL_INTENSITAET else LICHT_INTENSITAET
    return lerp(MaterialTheme.colorScheme.surface, basis, intensitaet)
}

/**
 * Textfarbe für den Namen eines kartenbelasteten Spielers. „Dunkelgelb" im
 * Light Mode (`0xFF7A5F00`, ~5,8:1 Kontrast) für die Gelbe Karte, im
 * Dunkelmodus helles Ambergelb (`0xFFFFD54F`, ~13:1); für Rot `0xFFC62828`
 * (hell: ~5,4:1) bzw. `0xFFEF5350` (dunkel: ~5,3:1) – jeweils WCAG-AA.
 */
@Composable
internal fun kartenNameFarbe(typ: BerichtEreignisTyp): Color {
    val dunklerModus = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (typ) {
        BerichtEreignisTyp.GELBE_KARTE -> if (dunklerModus) Color(0xFFFFD54F) else Color(0xFF7A5F00)
        BerichtEreignisTyp.ROTE_KARTE -> if (dunklerModus) Color(0xFFEF5350) else Color(0xFFC62828)
        BerichtEreignisTyp.TOR -> if (dunklerModus) Color(0xFF81C784) else Color(0xFF1B5E20)
        BerichtEreignisTyp.VERLETZUNG -> if (dunklerModus) Color(0xFFCE93D8) else Color(0xFF6A1B9A)
        else -> MaterialTheme.colorScheme.onSurface
    }
}

/** Ereigniskarte im Spielverlauf (einheitlich Spielbericht + Live-Spiel). */
@Composable
fun SpielverlaufEreignisKarte(
    minute: String?,
    text: String,
    typ: BerichtEreignisTyp,
    modifier: Modifier = Modifier,
    spielerName: String? = null,
) {
    val hervorgehoben = typ != BerichtEreignisTyp.SONSTIGES
    val spielerFarbe = if (spielerName != null && (typ.isKartenEreignis || typ == BerichtEreignisTyp.TOR || typ == BerichtEreignisTyp.VERLETZUNG)) kartenNameFarbe(typ) else null
    val annotatedText = remember(text, typ, spielerName, spielerFarbe) {
        spielverlaufText(text, typ, spielerName, spielerFarbe)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ereignisHintergrund(typ),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (minute != null) {
                Text(
                    "$minute'",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.width(44.dp),
                )
            } else {
                Spacer(Modifier.width(44.dp))
            }
            when (typ) {
                BerichtEreignisTyp.TOR -> TorBadge()
                BerichtEreignisTyp.GELBE_KARTE,
                BerichtEreignisTyp.ROTE_KARTE,
                BerichtEreignisTyp.VERLETZUNG,
                BerichtEreignisTyp.ELFMETER,
                -> EreignisDot(typ)
                BerichtEreignisTyp.SONSTIGES -> Unit
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                fontWeight = if (hervorgehoben) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

/** Tor-Markierung: ⚽ + Badge, damit ein Tor im Text sofort ins Auge fällt. */
@Composable
private fun TorBadge() {
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ereignisBasis(BerichtEreignisTyp.TOR))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.SportsSoccer,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(13.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            "TOR",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Kleiner Farbpunkt in der Basisfarbe, verknüpft Karte und Legende. */
@Composable
private fun EreignisDot(typ: BerichtEreignisTyp) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(ereignisBasis(typ)))
}

/**
 * Hebt den Spielstand („Neuer Spielstand: 2:1") in Tor-Texten extra fett hervor
 * und färbt bei Karten-Ereignissen den Spielernamen in der Kartenfarbe ein.
 */
private fun spielverlaufText(
    text: String,
    typ: BerichtEreignisTyp,
    spielerName: String? = null,
    spielerFarbe: androidx.compose.ui.graphics.Color? = null,
): AnnotatedString {
    if (spielerName != null && spielerFarbe != null && (typ.isKartenEreignis || typ == BerichtEreignisTyp.TOR || typ == BerichtEreignisTyp.VERLETZUNG)) {
        val idx = text.indexOf(spielerName, ignoreCase = true)
        if (idx >= 0) {
            return buildAnnotatedString {
                append(text.substring(0, idx))
                withStyle(SpanStyle(color = spielerFarbe, fontWeight = FontWeight.Bold)) {
                    append(text.substring(idx, idx + spielerName.length))
                }
                append(text.substring(idx + spielerName.length))
            }
        }
    }
    if (typ != BerichtEreignisTyp.TOR) return AnnotatedString(text)
    val spielstand = spielstandHighlightRegex.find(text) ?: return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, spielstand.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
            append(spielstand.value)
        }
        append(text.substring(spielstand.range.last + 1))
    }
}

/** Legende der Ereignisfarben unter dem Spielverlauf (wie bei der Ligatabelle). */
@Composable
fun SpielverlaufLegende() {
    val eintraege = listOf(
        BerichtEreignisTyp.TOR to "Tor",
        BerichtEreignisTyp.GELBE_KARTE to "Gelbe Karte",
        BerichtEreignisTyp.ROTE_KARTE to "Rote Karte",
        BerichtEreignisTyp.VERLETZUNG to "Verletzung",
        BerichtEreignisTyp.ELFMETER to "Elfmeter",
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Legende",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            eintraege.take(3).forEach { (typ, label) ->
                LegendenEintrag(typ, label, Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            eintraege.drop(3).forEach { (typ, label) ->
                LegendenEintrag(typ, label, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LegendenEintrag(typ: BerichtEreignisTyp, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ereignisHintergrund(typ))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EreignisDot(typ)
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
        )
    }
}
