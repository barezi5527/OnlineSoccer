package com.onlinesoccer.app.feature.team

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stadium
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Ein Einstiegspunkt im Bereich „Verein". */
private data class VereinMenuEintrag(
    val label: String,
    val untertitel: String,
    val path: String,
    val icon: ImageVector,
)

/** Statische Abbildung des Verein-Menüs der Website (Pfade live verifiziert). */
private object VereinMenu {
    val eintraege: List<VereinMenuEintrag> = listOf(
        VereinMenuEintrag(
            "Jugendteam",
            "Jugendspieler, Training und Spiele",
            "ju.php",
            Icons.Filled.Groups,
        ),
        VereinMenuEintrag(
            "Jugendscouting",
            "Talente des eigenen Jugendbereichs",
            "juscout.php",
            Icons.Filled.Search,
        ),
        VereinMenuEintrag(
            "Stadionausbau",
            "Stadion erweitern und ausbauen",
            "osneu/stadion",
            Icons.Filled.Stadium,
        ),
        VereinMenuEintrag(
            "Kontoauszug",
            "Ein- und Ausgaben des Teams",
            "ka.php",
            Icons.Filled.Receipt,
        ),
        VereinMenuEintrag(
            "Steuerübersicht",
            "Steuern und Abgaben überblicken",
            "steuer.php",
            Icons.Filled.Calculate,
        ),
        VereinMenuEintrag(
            "Teamübersicht (Website)",
            "Vereinsübersicht auf der Website",
            "showteam.php?s=0",
            Icons.Filled.Public,
        ),
    )
}

/** Tab-Wurzel des Bereichs „Verein": native Menüliste der Unterpunkte (analog zur Website). */
@Composable
fun VereinBereichScreen(
    onSeiteClick: (String) -> Unit = {},
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Verein",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Bereich „Verein“ der Website – Anzeigen sind nur lesend; Aktionen (z. B. Stadionausbau, Scouting) werden nur nach ausdrücklicher Bestätigung im Dialog gesendet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    VereinMenu.eintraege.forEachIndexed { index, eintrag ->
                        if (index > 0) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        }
                        EintragZeile(eintrag, onSeiteClick)
                    }
                }
            }
        }

        item {
            Text(
                "Stadionausbau, Scouting-Gebote und A-Team-Berufungen sendet die App ausschließlich nach Bestätigung im Dialog – "
                    + "unaufgeforderte Schreibaktionen werden nicht ausgeführt.",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EintragZeile(
    eintrag: VereinMenuEintrag,
    onSeiteClick: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onSeiteClick(eintrag.path) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            eintrag.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                eintrag.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (eintrag.untertitel.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    eintrag.untertitel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}