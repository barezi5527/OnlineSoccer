package com.onlinesoccer.app.feature.dashboard

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.DashboardData

@Composable
fun DashboardScreen(
    onLiveClick: () -> Unit = {},
    onBerichtClick: (String?) -> Unit = {},
    onZugabgabeClick: () -> Unit = {},
    onServerBereicheClick: () -> Unit = {},
    onZatReportClick: (zat: Int?, saison: Int?) -> Unit = { _, _ -> },
    refreshTrigger: Int = 0,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var jugendHinweisGesehen by remember { mutableStateOf(false) }
    val hinweis = (uiState as? DashboardUiState.Ready)?.data?.jugendHinweis

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) viewModel.refresh()
    }

    if (hinweis != null && !jugendHinweisGesehen) {
        AlertDialog(
            onDismissRequest = { jugendHinweisGesehen = true },
            title = { Text("Jugendspieler", fontWeight = FontWeight.Bold) },
            text = { Text(hinweis) },
            confirmButton = {
                TextButton(onClick = { jugendHinweisGesehen = true }) { Text("Verstanden") }
            },
        )
    }

    when (val state = uiState) {
        is DashboardUiState.Loading -> LoadingState()
        is DashboardUiState.Error -> ErrorState(state.message, viewModel::refresh)
        is DashboardUiState.Ready -> DashboardContent(
            state.data,
            viewModel::refresh,
            onLiveClick,
            onBerichtClick,
            onZugabgabeClick,
            onServerBereicheClick,
            onZatReportClick,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.padding(start = 4.dp))
            Text("Erneut versuchen")
        }
    }
}

@Composable
private fun DashboardContent(
    data: DashboardData,
    onRefresh: () -> Unit,
    onLiveClick: () -> Unit,
    onBerichtClick: (String?) -> Unit,
    onZugabgabeClick: () -> Unit,
    onServerBereicheClick: () -> Unit,
    onZatReportClick: (zat: Int?, saison: Int?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TeamHeaderCard(data)

        data.forumUrl?.let { ForumCard(it) }

        if (data.zugabgabeStatus != null) {
            ZugabgabeStatusCard(data, onZugabgabeClick)
        }

        data.naechstesSpiel?.let { next ->
            MatchCard(
                title = "Nächstes Spiel",
                match = next,
                zat = data.zat,
                zatDatum = data.zatDatum,
                onClick = onLiveClick,
            )
        }

        data.letztesSpiel?.let { prev ->
            MatchCard(
                title = "Letztes Spiel",
                match = prev,
                onClick = { onBerichtClick(prev.berichtUrl) },
            )
        }

        data.letztesSpiel?.let { prev ->
            ZatReportCard(
                zat = prev.gepaartZat,
                onClick = { onZatReportClick(prev.gepaartZat, prev.saison) },
            )
        }

        SummaryCard(data.rows)

        ServerBereicheCard(onServerBereicheClick)

        FilledTonalButton(
            onClick = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.padding(start = 4.dp))
            Text("Aktualisieren")
        }
    }
}

@Composable
private fun TeamHeaderCard(data: DashboardData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamLogo(data.teamLogoUrl)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = data.teamName ?: "Dein Team",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    data.liga?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamLogo(url: String?) {
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, url) {
        value = url?.let {
            withContext(Dispatchers.IO) {
                runCatching { URL(it).openStream().use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() } }.getOrNull()
            }
        }
    }
    if (image != null) {
        Image(image!!, contentDescription = "Vereinslogo", modifier = Modifier.height(72.dp).width(72.dp), contentScale = ContentScale.Fit)
    } else {
        Spacer(Modifier.height(72.dp).width(72.dp))
    }
}

@Composable
private fun ForumCard(url: String) {
    val context = LocalContext.current
    Card(
        Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: ActivityNotFoundException) {
                    // No browser is available; keep the dashboard usable.
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Zum Länderforum", fontWeight = FontWeight.Bold)
                Text("Forum im Browser öffnen", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Forum im Browser öffnen")
        }
    }
}

@Composable
private fun ServerBereicheCard(onServerBereicheClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onServerBereicheClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Weitere Online-Soccer-Bereiche", fontWeight = FontWeight.Bold)
                Text(
                    "Anmeldung, Bewerbung, Optionen & Internes",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ZugabgabeStatusCard(data: DashboardData, onZugabgabeClick: () -> Unit) {
    val gueltig = data.zugabgabeStatus.equals("Gültig", ignoreCase = true)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onZugabgabeClick),
        colors = CardDefaults.cardColors(
            containerColor = if (gueltig) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (gueltig) "✓ Zugabgabe gültig" else "✗ Zugabgabe ungültig",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "ZAT ${data.zat ?: "–"}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MatchCard(
    title: String,
    match: DashboardData.MatchInfo,
    zat: String? = null,
    zatDatum: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickableMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Card(modifier = Modifier.fillMaxWidth().then(clickableMod)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = (match.art ?: "Spiel").replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${if (match.heim) "Heim gegen" else "Auswärts bei"} ${match.gegner ?: "–"}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (zat != null || zatDatum != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (zat != null) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = "ZAT $zat",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        if (zatDatum != null) {
                            if (zat != null) Spacer(Modifier.width(8.dp))
                            Text(
                                text = zatDatum,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (onClick != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (title == "Nächstes Spiel") "Matchcenter öffnen" else "Spielbericht öffnen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Persönliche ZAT-Zusammenfassung (`zar.php`) – unter „Letztes Spiel“ auf dem Dashboard. */
@Composable
private fun ZatReportCard(
    zat: Int?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("ZAT-Report", fontWeight = FontWeight.Bold)
                Text(
                    "Einnahmen & Trainingserfolge (ZAT ${zat ?: "–"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(rows: List<DashboardData.LabeledValue>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = row.value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
