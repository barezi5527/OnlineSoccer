package com.onlinesoccer.app.feature.statistik

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Eine Unterseite des Statistiken-Bereichs. */
enum class StatistikBereich(
    val titel: String,
    val untertitel: String? = null,
) {
    TOPTSCORER("Topscorer", "Die besten Torschützen der Spielwelt"),
    TOP_SPIELER("Top-Spieler", "Die wertvollsten Spieler"),
    TOP_TEAMS("Top-Teams", "Die erfolgreichsten Teams"),
    FAIRPLAY("Fairplay", "Fairplay-Wertung der Ligen"),
    SPIELERSUCHE("Spielersuche", "Suche mit beliebigen Kriterien"),
    SPIELERVERGLEICH("Spielervergleich", "Zwei Spieler direkt vergleichen"),
    SPIELSTATISTIKEN("Spielstatistiken", "Kennzahlen der gesamten Spielwelt"),
    TABELLENSTATISTIKEN("Tabellenstatistiken", "Rekorde aus der Welt der Ligen"),
}

/** Übersichtsseite „Statistiken" mit allen Unterseiten. */
@Composable
fun StatistikenScreen(
    onClose: () -> Unit = {},
    onEintrag: (StatistikBereich) -> Unit = {},
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                }
                Column(Modifier.padding(start = 4.dp)) {
                    Text(
                        "Statistiken",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Tabellen, Wertungen und Rekorde",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(StatistikBereich.entries) { bereich ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onEintrag(bereich) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        bereich.titel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (bereich.untertitel != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            bereich.untertitel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Öffnen",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}