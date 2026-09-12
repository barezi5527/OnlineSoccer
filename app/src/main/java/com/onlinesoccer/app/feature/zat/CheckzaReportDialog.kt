package com.onlinesoccer.app.feature.zat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlinesoccer.app.data.model.CheckzaEintrag
import com.onlinesoccer.app.data.model.CheckzaErgebnis

private val GueltigColor = Color(0xFF2E7D32)
private val UngueltigColor = Color(0xFFC62828)

/**
 * Zeigt das strukturierte Checkza-Ergebnis als Dialog an,
 * analog zum Popup-Fenster auf der Website (checkza.php).
 */
@Composable
fun CheckzaReportDialog(
    ergebnis: CheckzaErgebnis,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Zugabgabe-Check",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    ergebnis.gesamtStatus,
                    color = if (ergebnis.gueltig) GueltigColor else UngueltigColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )

                HorizontalDivider()

                CheckzaSection(
                    titel = "1. Aufstellung",
                    eintraege = ergebnis.aufstellung,
                )

                CheckzaSection(
                    titel = "2. Aktionen",
                    eintraege = ergebnis.aktionen,
                )

                CheckzaSection(
                    titel = "3. Einstellungen",
                    eintraege = ergebnis.einstellungen,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schlie\u00dfen")
            }
        },
    )
}

@Composable
private fun CheckzaSection(
    titel: String,
    eintraege: List<CheckzaEintrag>,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                titel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))

            if (eintraege.isEmpty()) {
                Text(
                    "Keine Eintr\u00e4ge",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                eintraege.forEach { eintrag ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            if (eintrag.gueltig) "\u2713" else "\u2717",
                            color = if (eintrag.gueltig) GueltigColor else UngueltigColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(20.dp),
                        )
                        Text(
                            eintrag.text,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lade-Dialog der angezeigt wird, waehrend checkza.php aufgerufen wird.
 */
@Composable
fun CheckzaLadeDialog() {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                "Zugabgabe-Check",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Text("Zugabgabe wird gecheckt\u2026")
            }
        },
        confirmButton = { },
    )
}
