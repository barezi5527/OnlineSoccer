package com.onlinesoccer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Wiederverwendbare Sub-Navigationsleiste eines Hubs (wie der Website-Header). */
@Composable
fun HubTabs(
    tabs: List<Pair<String, Any>>,
    selected: Any,
    onSelect: (Any) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val vertical = if (compact) 2.dp else 6.dp
    val topPadding = if (compact) 2.dp else 4.dp
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = vertical),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.padding(top = topPadding),
    ) {
        items(tabs) { (label, value) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                },
            )
        }
    }
}