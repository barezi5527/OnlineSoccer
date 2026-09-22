package com.onlinesoccer.app.feature.team

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlinesoccer.app.data.model.TeamTrainer

/** Zustand des „Nachricht an Trainer“-Dialogs (Senden-Status). */
data class TrainerNachrichtState(
    val sendend: Boolean = false,
    val meldung: String? = null,
    val fehler: String? = null,
)

/** Trainer-Zeile für die Kopfzeile (Kader / Spielerkarte). */
@Composable
fun TrainerHeaderZeile(
    trainer: TeamTrainer,
    onKlick: (TeamTrainer.Besetzt) -> Unit,
) {
    when (trainer) {
        is TeamTrainer.Besetzt -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onKlick(trainer) },
        ) {
            Icon(
                Icons.Filled.MailOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Trainer: ${trainer.name}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        TeamTrainer.Frei -> Text(
            "Team ist frei",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TeamTrainer.Unbekannt -> Unit
    }
}

/** Popup zum Schreiben einer Private-Nachricht an den Trainer eines Vereins. */
@Composable
fun TrainerNachrichtDialog(
    trainer: TeamTrainer.Besetzt,
    zustand: TrainerNachrichtState,
    onDismiss: () -> Unit,
    onSenden: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val fertig = zustand.meldung != null

    AlertDialog(
        onDismissRequest = { if (!zustand.sendend) onDismiss() },
        title = { Text("Nachricht an ${trainer.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (fertig) {
                    Text(
                        zustand.meldung!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Nachricht") },
                        minLines = 6,
                        maxLines = 10,
                        enabled = !zustand.sendend,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    zustand.fehler?.let { fehler ->
                        Text(
                            fehler,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        "Die Nachricht wird als Private Nachricht (PN) verschickt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (fertig) {
                TextButton(onClick = onDismiss) { Text("Schließen") }
            } else {
                Button(
                    onClick = { onSenden(text.trim()) },
                    enabled = text.isNotBlank() && !zustand.sendend,
                ) {
                    Text(if (zustand.sendend) "Sende …" else "Senden")
                }
            }
        },
        dismissButton = {
            if (!fertig) {
                TextButton(onClick = onDismiss, enabled = !zustand.sendend) { Text("Abbrechen") }
            }
        },
    )
}