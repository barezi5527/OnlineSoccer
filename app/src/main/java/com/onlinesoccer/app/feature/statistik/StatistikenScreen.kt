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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
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

/** Eine Unterseite des Statistiken-Bereichs. */
enum class StatistikBereich(
    val titel: String,
    val untertitel: String? = null,
) {
    TOP_TEAMS("Top-Teams", "Die wertvollsten Teams nach Filter"),
}

/** Ein Einstiegspunkt im Bereich „Statistiken". */
data class StatistikMenuEintrag(
    val label: String,
    val untertitel: String,
    val bereich: StatistikBereich,
    val icon: ImageVector,
)

/** Statische Abbildung des Website-Untermenüs „Statistiken" (umgesetzte Einträge). */
object StatistikMenu {
    val eintraege: List<StatistikMenuEintrag> = listOf(
        StatistikMenuEintrag(
            "Top-Teams",
            "Die wertvollsten Teams nach Filter",
            StatistikBereich.TOP_TEAMS,
            Icons.Filled.BarChart,
        ),
    )
}

/** Tab-Wurzel des Bereichs „Statistiken": native Menüliste der Unterpunkte (analog zur Website). */
@Composable
fun StatistikenScreen(
    onEintrag: (StatistikBereich) -> Unit = {},
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Statistiken",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Bereich „Statistiken“ der Website",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    StatistikMenu.eintraege.forEachIndexed { index, eintrag ->
                        if (index > 0) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        }
                        EintragZeile(eintrag, onEintrag)
                    }
                }
            }
        }
    }
}

@Composable
private fun EintragZeile(
    eintrag: StatistikMenuEintrag,
    onEintrag: (StatistikBereich) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onEintrag(eintrag.bereich) }
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
            if (!eintrag.untertitel.isNullOrBlank()) {
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