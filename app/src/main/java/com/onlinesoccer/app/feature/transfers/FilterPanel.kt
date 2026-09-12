package com.onlinesoccer.app.feature.transfers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlinesoccer.app.data.model.TransferFilter

/**
 * Gemeinsames Filter-Panel für „Transferliste" und „Transfermarkt": aufklappbare Liste
 * aller Filter-Auswahlen plus „Anzeigen"-Button (entspricht dem Website-Formular).
 */
@Composable
internal fun TransferFilterPanel(
    filter: List<TransferFilter>,
    wahl: Map<String, String>,
    onFilterWaehlen: (String, String) -> Unit,
    onAnzeigen: () -> Unit,
    anzeigenLabel: String = "Anzeigen",
) {
    var offen by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Filter",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(onClick = { offen = !offen }) {
                    Icon(
                        if (offen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (offen) "Filter ausblenden" else "Filter anzeigen",
                    )
                }
            }

            if (offen) {
                filter.forEach { kategorie ->
                    FilterDropdown(
                        kategorie = kategorie,
                        aktiverWert = wahl[kategorie.name] ?: "0",
                        onWaehlen = onFilterWaehlen,
                    )
                }
                Spacer(Modifier.height(6.dp))
                FilledTonalButton(
                    onClick = onAnzeigen,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(anzeigenLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    kategorie: TransferFilter,
    aktiverWert: String,
    onWaehlen: (String, String) -> Unit,
) {
    var offen by remember { mutableStateOf(false) }
    val aktiveOption = kategorie.option(aktiverWert)

    Column(Modifier.fillMaxWidth()) {
        Text(
            kategorie.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        ExposedDropdownMenuBox(
            expanded = offen,
            onExpandedChange = { offen = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = aktiveOption?.label ?: kategorie.label,
                onValueChange = {},
                readOnly = true,
                label = { Text(kategorie.label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = offen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
                kategorie.optionen.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.label) },
                        onClick = {
                            offen = false
                            onWaehlen(kategorie.name, opt.wert)
                        },
                    )
                }
            }
        }
    }
}