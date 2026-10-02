package com.onlinesoccer.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/**
 * Rückfrage vor einem Teamwechsel, solange eine ungespeicherte Änderung offen ist.
 *
 * **Warum es diesen Dialog braucht:** T26 setzt nach einem bestätigten Wechsel den
 * gesamten Backstack zurück, damit kein ViewModel Daten des alten Teams behält. Damit
 * stirbt auch der ViewModel des Zugababe-/Taktik-Screens — und dessen ungespeicherte
 * Änderung ist weg, ohne dass der Nutzer etwas davon bemerkt. Ein `disabled` auf dem
 * 1|2-Button wäre ebenfalls möglich, erklärt aber nicht, **warum** gesperrt ist und
 * lässt den Nutzer raten (Plan T33/T34).
 *
 * Der Text kommt aus `bestaetigungstext(bereiche)` und wird hier nur angezeigt: die
 * Entscheidung trifft `wechselAktion()`, damit sie ohne UI testbar bleibt.
 *
 * [onAbbrechen] ist auch [AlertDialog.onDismissRequest] — ein Tippen außerhalb des
 * Dialogs ist ein „Nein", denn ein unabsichtliches Wegtippen soll keine Änderung
 * verwerfen.
 */
@Composable
fun TeamwechselBestaetigungDialog(
    text: String,
    onWechseln: () -> Unit,
    onAbbrechen: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onAbbrechen,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                "Ungespeicherte Änderungen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = { Text(text) },
        dismissButton = {
            TextButton(onClick = onAbbrechen) {
                Text("Abbrechen")
            }
        },
        confirmButton = {
            // ⚠️ Kein `enabled = false` o. Ä.: Wer den Dialog sieht, hat eine offene
            // Änderung bestätigt bekommen — der Button muss durchdrückbar sein, sonst
            // wäre der Weg aus der Sackgasse nur "Abbrechen" und damit der Wechsel
            // faktisch gesperrt. Die Gefahr ist im Text benannt.
            Button(
                onClick = onWechseln,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Trotzdem wechseln")
            }
        },
    )
}