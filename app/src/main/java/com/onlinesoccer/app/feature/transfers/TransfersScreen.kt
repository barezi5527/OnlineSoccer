package com.onlinesoccer.app.feature.transfers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.SwapHoriz
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

/** Einstiegspunkte des Bereichs „Transfers" (alle mit nativem Screen). */
enum class TransferBereich {
    TRANSFERLISTE,
    TRANSFERMARKT,
    EIGENE_GEBOTE,
    LEIH_UEBERSICHT,
    TRANSFERSTATUS,
    LETZTE_TRANSFERS,
    LETZTE_LEIHEN,
    LETZTE_VM,
    LETZTE_TM,
    LETZTE_BLITZ,
}

/** Ein Einstiegspunkt im Bereich „Transfers". */
data class TransferMenuEintrag(
    val label: String,
    val untertitel: String? = null,
    val bereich: TransferBereich,
    val icon: ImageVector,
)

/** Statische Abbildung des Website-Untermenüs „Transfers" (Pfade live verifiziert). */
object TransfersMenu {
    val eintraege: List<TransferMenuEintrag> = listOf(
        TransferMenuEintrag(
            "Transferliste",
            "Spieler mit Alter-, Skill- und Ablöse-Filtern",
            TransferBereich.TRANSFERLISTE,
            Icons.Filled.SwapHoriz,
        ),
        TransferMenuEintrag(
            "Transfermarkt",
            "Internationaler Transfermarkt mit Geboten",
            TransferBereich.TRANSFERMARKT,
            Icons.Filled.Storefront,
        ),
        TransferMenuEintrag(
            "Eigene Gebote",
            "Übersicht meiner Transfermarkt-Gebote",
            TransferBereich.EIGENE_GEBOTE,
            Icons.Filled.Gavel,
        ),
        TransferMenuEintrag(
            "Leihspieler Übersicht",
            "Verliehene und geliehene Spieler",
            TransferBereich.LEIH_UEBERSICHT,
            Icons.Filled.SwapCalls,
        ),
        TransferMenuEintrag(
            "Transferstatus",
            "Status der eigenen Spieler verwalten (U/N/A/T)",
            TransferBereich.TRANSFERSTATUS,
            Icons.AutoMirrored.Filled.FactCheck,
        ),
        TransferMenuEintrag(
            "Letzte Transfers",
            "Die letzten Transfers im Überblick",
            TransferBereich.LETZTE_TRANSFERS,
            Icons.Filled.History,
        ),
        TransferMenuEintrag(
            "Letzte Leihen",
            "Die letzten Leihgeschäfte",
            TransferBereich.LETZTE_LEIHEN,
            Icons.Filled.Repeat,
        ),
        TransferMenuEintrag(
            "Letzte VM-Käufe",
            "Letzte Käufe vom Vereinsmarkt",
            TransferBereich.LETZTE_VM,
            Icons.Filled.ShoppingCart,
        ),
        TransferMenuEintrag(
            "Letzte TM-Käufe",
            "Letzte Käufe vom Transfermarkt",
            TransferBereich.LETZTE_TM,
            Icons.Filled.AttachMoney,
        ),
        TransferMenuEintrag(
            "Letzte Schnelltransfers",
            "Letzte Transfers über den Schnelltransfer",
            TransferBereich.LETZTE_BLITZ,
            Icons.Filled.Bolt,
        ),
    )
}

/** Tab-Wurzel des Bereichs „Transfers": native Menüliste der Unterpunkte (analog zur Website). */
@Composable
fun TransfersScreen(
    onEintrag: (TransferBereich) -> Unit = {},
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Transfers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Bereich „Transfers“ der Website – Änderungen (Gebote, Transferstatus) werden erst nach ausdrücklicher Bestätigung im Dialog gesendet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    TransfersMenu.eintraege.forEachIndexed { index, eintrag ->
                        if (index > 0) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        }
                        EintragZeile(eintrag, onEintrag)
                    }
                }
            }
        }

        item {
            Text(
                "Spieler wechseln oder verleihen ist nur im Browser möglich – dieser verwendet deine eigenen Login-Daten. "
                    + "Transfermarkt-Gebote sendet die App ausschließlich nach Bestätigung im „Bieten“-Dialog.",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EintragZeile(
    eintrag: TransferMenuEintrag,
    onEintrag: (TransferBereich) -> Unit,
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