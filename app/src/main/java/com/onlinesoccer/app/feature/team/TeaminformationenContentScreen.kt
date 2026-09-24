package com.onlinesoccer.app.feature.team

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onlinesoccer.app.data.model.LeihhistorieEintrag
import com.onlinesoccer.app.data.model.SaisonhistorieEintrag
import com.onlinesoccer.app.data.model.SaisonplanDaten
import com.onlinesoccer.app.data.model.TransferhistorieBlock
import com.onlinesoccer.app.data.model.VereinshistorieEintrag

/** Lädt und zeigt die Inhalte eines einzelnen Teaminformationen-Unterpunkts nativ an. */
@Composable
fun TeaminformationenContentScreen(
    eintragId: String?,
    teamId: Long?,
    titel: String,
    onClose: () -> Unit,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
    onBerichtClick: (String) -> Unit = {},
    viewModel: TeamViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(eintragId, teamId) {
        when (eintragId) {
            "s0" -> if (uiState.kader.isEmpty()) viewModel.ladeKader(teamId)
            "s1" -> if (uiState.vertraege == null) viewModel.ladeVertraege(teamId)
            "s2" -> if (uiState.staerken == null) viewModel.ladeStaerken(teamId)
            "s3" -> viewModel.ladeStatistik(gesamt = false, teamId = teamId)
            "s4" -> viewModel.ladeStatistik(gesamt = true, teamId = teamId)
            "s5" -> if (uiState.teaminfo == null) viewModel.ladeTeaminfo(teamId)
            "s6" -> viewModel.ladeSaisonplan(uiState.saisonplan?.gewaehlteSaison, teamId)
            "s7" -> if (uiState.vereinshistorie == null) viewModel.ladeVereinshistorie(teamId)
            "s8" -> if (uiState.transferhistorie == null) viewModel.ladeTransferhistorie(teamId)
            "s9" -> if (uiState.leihhistorie == null) viewModel.ladeLeihhistorie(teamId)
            "s10" -> if (uiState.saisonhistorie == null) viewModel.ladeSaisonhistorie(teamId)
            "tp" -> if (teamId != null) viewModel.ladeTabellenplatzBild(teamId)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TeaminfoHeader(titel, onClose)

        when {
            uiState.ladend && !hatInhalt(eintragId, uiState) -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.fehler != null && !hatInhalt(eintragId, uiState) -> {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(uiState.fehler.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = { neuLaden(eintragId, teamId, viewModel) }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.padding(start = 4.dp))
                        Text("Erneut versuchen")
                    }
                }
            }
            else -> when (eintragId) {
                "s0" -> KaderAnsicht(uiState, viewModel, onSpielerClick)
                "s1" -> VertraegeAnsicht(uiState.vertraege.orEmpty(), onSpielerClick)
                "s2" -> StaerkenAnsicht(uiState.staerken.orEmpty())
                "s3" -> StatistikAnsicht(
                    uiState.statistik.orEmpty(),
                    gesamt = false,
                    onGesamt = { gesamt -> viewModel.ladeStatistik(gesamt, teamId) },
                )
                "s4" -> StatistikAnsicht(
                    uiState.statistik.orEmpty(),
                    gesamt = true,
                    onGesamt = { gesamt -> viewModel.ladeStatistik(gesamt, teamId) },
                )
                "s5" -> TeaminfoAnsicht(uiState.teaminfo, uiState.saison)
                "s6" -> SaisonplanAnsicht(
                    uiState.saisonplan,
                    onSaison = { saison -> viewModel.ladeSaisonplan(saison, teamId) },
                    onBerichtClick,
                )
                "s7" -> VereinshistorieAnsicht(uiState.vereinshistorie)
                "s8" -> TransferhistorieAnsicht(uiState.transferhistorie, onTeamClick)
                "s9" -> LeihhistorieAnsicht(uiState.leihhistorie, onSpielerClick, onTeamClick)
                "s10" -> SaisonhistorieAnsicht(uiState.saisonhistorie)
                "tp" -> TabellenplatzAnsicht(uiState.tabellenplatzBild)
                else -> Text("Unbekannter Eintrag.", Modifier.padding(24.dp))
            }
        }
    }
}

private fun hatInhalt(eintragId: String?, uiState: TeamUiState): Boolean = when (eintragId) {
    "s0" -> uiState.kader.isNotEmpty()
    "s1" -> uiState.vertraege != null
    "s2" -> uiState.staerken != null
    "s3", "s4" -> uiState.statistik != null
    "s5" -> uiState.teaminfo != null
    "s6" -> uiState.saisonplan != null
    "s7" -> uiState.vereinshistorie != null
    "s8" -> uiState.transferhistorie != null
    "s9" -> uiState.leihhistorie != null
    "s10" -> uiState.saisonhistorie != null
    "tp" -> uiState.tabellenplatzBild != null
    else -> true
}

private fun neuLaden(eintragId: String?, teamId: Long?, viewModel: TeamViewModel) = when (eintragId) {
    "s0" -> viewModel.ladeKader(teamId)
    "s1" -> viewModel.ladeVertraege(teamId)
    "s2" -> viewModel.ladeStaerken(teamId)
    "s3" -> viewModel.ladeStatistik(gesamt = false, teamId = teamId)
    "s4" -> viewModel.ladeStatistik(gesamt = true, teamId = teamId)
    "s5" -> viewModel.ladeTeaminfo(teamId)
    "s6" -> viewModel.ladeSaisonplan(viewModel.uiState.value.saisonplan?.gewaehlteSaison, teamId)
    "s7" -> viewModel.ladeVereinshistorie(teamId)
    "s8" -> viewModel.ladeTransferhistorie(teamId)
    "s9" -> viewModel.ladeLeihhistorie(teamId)
    "s10" -> viewModel.ladeSaisonhistorie(teamId)
    "tp" -> if (teamId != null) viewModel.ladeTabellenplatzBild(teamId) else Unit
    else -> Unit
}

@Composable
private fun TeaminfoHeader(titel: String, onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
        }
        Text(
            titel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Saisonplan (`s=6`) mit Saison-Auswahl. */
@Composable
private fun SaisonplanAnsicht(
    daten: SaisonplanDaten?,
    onSaison: (Int?) -> Unit,
    onBerichtClick: (String) -> Unit = {},
) {
    if (daten == null) {
        Text("Kein Saisonplan gefunden.", Modifier.padding(24.dp))
        return
    }
    Column(Modifier.fillMaxSize()) {
        if (daten.saisons.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp),
            ) {
                item {
                    FilterChip(
                        selected = daten.gewaehlteSaison == null,
                        onClick = { onSaison(null) },
                        label = { Text("Aktuelle Saison") },
                    )
                }
                items(daten.saisons) { saison ->
                    val gewaehlt = saison == daten.gewaehlteSaison
                    FilterChip(
                        selected = gewaehlt,
                        onClick = { onSaison(if (gewaehlt) null else saison) },
                        label = { Text("Saison $saison") },
                    )
                }
            }
        }
        if (daten.eintraege.isEmpty()) {
            Text(
                "Keine Spiele in dieser Saison.",
                Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(daten.eintraege) { spiel ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                spiel.gegner,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "ZAT ${spiel.zat} · ${spiel.spielart}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (spiel.ergebnis.isNotBlank()) {
                            Text(
                                spiel.ergebnis,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                        if (spiel.berichtUrl != null) {
                            Text(
                                "Bericht",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onBerichtClick(spiel.berichtUrl!!) }
                                    .padding(4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vereinshistorie (`s=7`): Tabelle analog zu den übrigen Statistiktabellen –
 * fixierte Kopfzeile, angepinnte Saison-Spalte, breitenangepasste Spalten,
 * Zebra-Zeilen. Eine angetippte Zeile wird zeilenübergreifend markiert.
 */
@Composable
private fun VereinshistorieAnsicht(eintraege: List<VereinshistorieEintrag>?) {
    if (eintraege.isNullOrEmpty()) {
        Text("Keine Vereinshistorie gefunden.", Modifier.padding(24.dp))
        return
    }
    val kopfzeilen = listOf(
        "Saison", "ZAT", "∑Spieler", "∅Skill", "∅Opti", "∅Alter",
        "∅MW", "∑MW", "∅Gehalt", "∑Gehalt", "Manager",
    )
    val saisonSpalte = eintraege.map { it.saison }
    val werteJeSpalte: List<List<String>> = listOf(
        eintraege.map { it.zat },
        eintraege.map { it.spielerAnzahl },
        eintraege.map { it.avgSkill },
        eintraege.map { it.avgOpti },
        eintraege.map { it.avgAlter },
        eintraege.map { it.avgMW },
        eintraege.map { it.sumMW },
        eintraege.map { it.avgGehalt },
        eintraege.map { it.sumGehalt },
        eintraege.map { it.manager },
    )
    // Breiten passend zum Zahlenumfang: Kennzahlen schmal, MW/Gehalt und Manager breiter.
    val spaltenBreiten = listOf(
        52.dp, 76.dp, 64.dp, 64.dp, 64.dp,
        88.dp, 96.dp, 88.dp, 96.dp, 128.dp,
    )
    val saisonBreite = 68.dp
    val zeilenHoehe = 40.dp

    var markierteZeile by rememberSaveable { mutableStateOf<Int?>(null) }
    val vertScroll = rememberScrollState()
    val horizScroll = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        Text(
            "${eintraege.size} Einträge · Zeile antippen zum Markieren",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
            VereinshistorieKopfZelle("Saison", saisonBreite)
            Row(Modifier.horizontalScroll(horizScroll)) {
                kopfzeilen.drop(1).forEachIndexed { idx, kopf ->
                    VereinshistorieKopfZelle(kopf, spaltenBreiten.getOrElse(idx) { 76.dp })
                }
            }
        }

        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .width(saisonBreite)
                    .verticalScroll(vertScroll),
            ) {
                saisonSpalte.forEachIndexed { zeile, wert ->
                    val markiert = zeile == markierteZeile
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(zeilenHoehe)
                            .background(vereinshistorieZeilenFarbe(zeile, markiert))
                            .clickable { markierteZeile = if (markiert) null else zeile },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (markiert) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .width(3.dp)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                        Text(
                            wert,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (markiert) FontWeight.Bold else FontWeight.Normal,
                            color = if (markiert) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.horizontalScroll(horizScroll)) {
                    Column(Modifier.verticalScroll(vertScroll)) {
                        eintraege.indices.forEach { zeile ->
                            val markiert = zeile == markierteZeile
                            Row(
                                Modifier
                                    .height(zeilenHoehe)
                                    .background(vereinshistorieZeilenFarbe(zeile, markiert))
                                    .clickable { markierteZeile = if (markiert) null else zeile },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                werteJeSpalte.forEachIndexed { idx, spaltenWerte ->
                                    Box(
                                        Modifier
                                            .width(spaltenBreiten.getOrElse(idx) { 76.dp })
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            spaltenWerte.getOrElse(zeile) { "" },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (markiert) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (markiert) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VereinshistorieKopfZelle(text: String, breite: Dp) {
    Box(
        Modifier
            .width(breite)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun vereinshistorieZeilenFarbe(zeile: Int, markiert: Boolean): Color = when {
    markiert -> MaterialTheme.colorScheme.primaryContainer
    zeile % 2 == 1 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    else -> Color.Transparent
}

/** Saisonhistorie (`s=10`). */
@Composable
private fun SaisonhistorieAnsicht(eintraege: List<SaisonhistorieEintrag>?) {
    if (eintraege.isNullOrEmpty()) {
        Text("Keine Saisonhistorie gefunden.", Modifier.padding(24.dp))
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(eintraege) { e ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Saison ${e.saison}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    InfoZeilen(
                        listOf(
                            "Liga" to e.liga,
                            "Tabelle" to e.tabelle,
                            "Pokal" to e.pokal,
                            "OSE" to e.ose,
                            "OSC" to e.osc,
                        ),
                    )
                }
            }
        }
    }
}

/** Transferhistorie (`s=8`): ein Block je Transfer/VM-Kauf. */
@Composable
private fun TransferhistorieAnsicht(
    bloecke: List<TransferhistorieBlock>?,
    onTeamClick: (Long) -> Unit,
) {
    if (bloecke.isNullOrEmpty()) {
        Text("Keine Transferhistorie gefunden.", Modifier.padding(24.dp))
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(bloecke) { block ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        block.datum,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TeamLink(block.team1, block.team1Id, onTeamClick, Modifier.weight(1f))
                        Text(
                            "\u2192",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        TeamLink(block.team2, block.team2Id, onTeamClick, Modifier.weight(1f))
                    }
                    block.details.forEach { detail ->
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamLink(
    name: String,
    teamId: Long?,
    onTeamClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (teamId != null && name.isNotBlank()) {
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.clickable { onTeamClick(teamId) },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            modifier = modifier,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Leihhistorie (`s=9`). */
@Composable
private fun LeihhistorieAnsicht(
    eintraege: List<LeihhistorieEintrag>?,
    onSpielerClick: (Long) -> Unit,
    onTeamClick: (Long) -> Unit,
) {
    if (eintraege.isNullOrEmpty()) {
        Text("Keine Leihhistorie gefunden.", Modifier.padding(24.dp))
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(eintraege) { e ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (e.spielerPid != null) {
                            Text(
                                e.spieler,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onSpielerClick(e.spielerPid) },
                            )
                        } else {
                            Text(
                                e.spieler,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Text(
                            e.datum,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "Von: ${e.von.ifBlank { "–" }}  ·  Zu: ${e.zu.ifBlank { "–" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (e.zahlung.isNotBlank() || e.dauer.isNotBlank()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                e.zahlung.ifBlank { "–" },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                            if (e.dauer.isNotBlank()) {
                                Text(
                                    e.dauer,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tabellenplätze (`tp`): Bild, horizontal & vertikal scrollbar. */
@Composable
private fun TabellenplatzAnsicht(pngBytes: ByteArray?) {
    if (pngBytes == null || pngBytes.isEmpty()) {
        Text("Kein Tabellenplatz-Bild gefunden.", Modifier.padding(24.dp))
        return
    }
    val bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
    if (bitmap == null) {
        Text("Das Tabellenplatz-Bild konnte nicht gelesen werden.", Modifier.padding(24.dp))
        return
    }
    Box(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState()),
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Tabellenplatz",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InfoZeilen(zeilen: List<Pair<String, String>>) {
    zeilen.forEach { (label, wert) ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.35f),
            )
            Text(
                wert.ifBlank { "–" },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.65f),
                textAlign = TextAlign.End,
            )
        }
    }
}