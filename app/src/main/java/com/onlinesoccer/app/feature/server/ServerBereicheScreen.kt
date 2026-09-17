package com.onlinesoccer.app.feature.server

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Öffnungsziel eines [ServerItem]. */
enum class ServerAktion {
    /** Eigener Screen (native Liste). */
    NATIV,

    /** Allgemeiner Nur-Lese-Reader (`seite/{path}`). */
    READER,

    /** Im externen Browser öffnen. */
    BROWSER,

    /** Zur App-Anmeldung (Demo-Sitzung beenden → Login-Screen). */
    ANMELDUNG,

    /** Nur Hinweistext, keine Aktion. */
    HINWEIS,
}

/** Ein Einstiegspunkt eines Website-Menüs. */
data class ServerItem(
    val label: String,
    val untertitel: String? = null,
    val aktion: ServerAktion,
    val path: String? = null,
    val url: String? = null,
    val erfordertLogin: Boolean = false,
)

/** Eine Sektion (entspricht einer Website-Gruppe) mit Unterpunkten. */
data class ServerBereich(
    val titel: String,
    val items: List<ServerItem>,
)

/** Statische Abbildung der Website-Menüs (Pfade/URLs live verifiziert). */
object ServerMenu {

    private const val BASE = "https://os.ongapo.com"

    val bereiche: List<ServerBereich> = listOf(
        ServerBereich(
            titel = "Entdecken & Finden",
            items = listOf(
                ServerItem("Freie Teams", "Vereine ohne Manager, mit Land-Filter", ServerAktion.NATIV),
                ServerItem("Freie Zweitteams", "Zur Bewerbung freigegebene Zweitteams", ServerAktion.NATIV),
                ServerItem("Gesperrte Teams", "Zwangsabgestiegene oder gesperrte Vereine", ServerAktion.READER, path = "osneu/gesperrteteams"),
                ServerItem("Team/Managerliste", "Manager mit NMR, Wechselsperre, Zugabgabe", ServerAktion.NATIV),
                ServerItem("Team/Managersuche", "Suche nach Manager oder Team", ServerAktion.NATIV),
            ),
        ),
        ServerBereich(
            titel = "Konto & Bewerbung",
            items = listOf(
                ServerItem("Passwort verloren?", "Neues Passwort für ein Team anfordern", ServerAktion.READER, path = "osneu/lostpw"),
                ServerItem("Bewerbung", "Bewerbung um ein freies Team", ServerAktion.BROWSER, url = "$BASE/bewerbung.php", erfordertLogin = true),
                ServerItem("Zweitteam übernehmen", "Ein freies Zweitteam übernehmen", ServerAktion.BROWSER, url = "$BASE/zweitteam.php", erfordertLogin = true),
                ServerItem("Anmeldung", "Anmeldung erfolgt direkt über das App-Login", ServerAktion.ANMELDUNG),
                ServerItem("Abmeldung", "Abmeldung vom Spiel ist nur per E-Mail an das OS-Team möglich", ServerAktion.HINWEIS),
            ),
        ),
        ServerBereich(
            titel = "Einstellungen & Hilfe",
            items = listOf(
                ServerItem("Benachrichtigungen", "E-Mail-Benachrichtigungen verwalten", ServerAktion.BROWSER, url = "$BASE/osneu/benachrichtigungen", erfordertLogin = true),
                ServerItem("Passwort ändern", "Eigenes Passwort ändern", ServerAktion.BROWSER, url = "$BASE/osneu/passwort", erfordertLogin = true),
                ServerItem("Zusatzfeatures", "Zusatzfunktionen buchen und deaktivieren", ServerAktion.BROWSER, url = "$BASE/zfeatures.php", erfordertLogin = true),
                ServerItem("Support", "Kontakt zum OS-Team", ServerAktion.BROWSER, url = "$BASE/support.php", erfordertLogin = true),
            ),
        ),
    )
}

/**
 * Übersicht der Website-Menüs „Anmeldung/Bewerbung" und „Optionen/Internes".
 * Nur lesend: eigene native Listen (Freie Teams, Freie Zweitteams, Team-/Managerliste,
 * Team-/Managersuche), generischer Reader für Info-/Suchseiten sowie
 * „Im Browser öffnen" (der Browser erhält nie die App-Session).
 */
@Composable
fun ServerBereicheScreen(
    demo: Boolean = false,
    onClose: () -> Unit = {},
    onFreieTeams: () -> Unit = {},
    onFreieZweitteams: () -> Unit = {},
    onManagerliste: () -> Unit = {},
    onManagersuche: () -> Unit = {},
    onSeite: (String) -> Unit = {},
    onAnmeldung: () -> Unit = {},
) {
    val context = LocalContext.current

    fun oeffnenBrowser(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            // Kein Browser verfügbar; Menü bleibt bedienbar.
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                }
                Column(Modifier.padding(start = 4.dp)) {
                    Text(
                        "Weitere Online-Soccer-Bereiche",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Menüs der Website: Anmeldung/Bewerbung und Optionen/Internes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(ServerMenu.bereiche) { bereich ->
            BereichCard(
                bereich = bereich,
                demo = demo,
                onFreieTeams = onFreieTeams,
                onFreieZweitteams = onFreieZweitteams,
                onManagerliste = onManagerliste,
                onManagersuche = onManagersuche,
                onSeite = onSeite,
                onBrowser = ::oeffnenBrowser,
                onAnmeldung = onAnmeldung,
            )
        }

        item {
            Text(
                "Eingaben (Bewerbung, Passwort, Benachrichtigungen …) sind im Browser möglich – "
                    + "der Browser verwendet dafür deine eigenen Login-Daten. Die App sendet keine Schreibaktionen.",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BereichCard(
    bereich: ServerBereich,
    demo: Boolean,
    onFreieTeams: () -> Unit,
    onFreieZweitteams: () -> Unit,
    onManagerliste: () -> Unit,
    onManagersuche: () -> Unit,
    onSeite: (String) -> Unit,
    onBrowser: (String) -> Unit,
    onAnmeldung: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                bereich.titel,
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            bereich.items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                ItemZeile(item, demo, onFreieTeams, onFreieZweitteams, onManagerliste, onManagersuche, onSeite, onBrowser, onAnmeldung)
            }
        }
    }
}

@Composable
private fun ItemZeile(
    item: ServerItem,
    demo: Boolean,
    onFreieTeams: () -> Unit,
    onFreieZweitteams: () -> Unit,
    onManagerliste: () -> Unit,
    onManagersuche: () -> Unit,
    onSeite: (String) -> Unit,
    onBrowser: (String) -> Unit,
    onAnmeldung: () -> Unit,
) {
    val gesperrt = item.erfordertLogin && demo
    val klickbar = item.aktion != ServerAktion.HINWEIS && !gesperrt && !(item.aktion == ServerAktion.ANMELDUNG && !demo)

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = klickbar) {
                when (item.aktion) {
                    ServerAktion.NATIV -> when (item.label) {
                        "Freie Teams" -> onFreieTeams()
                        "Freie Zweitteams" -> onFreieZweitteams()
                        "Team/Managerliste" -> onManagerliste()
                        "Team/Managersuche" -> onManagersuche()
                    }
                    ServerAktion.READER -> item.path?.let(onSeite)
                    ServerAktion.BROWSER -> item.url?.let(onBrowser)
                    ServerAktion.ANMELDUNG -> if (demo) onAnmeldung()
                    ServerAktion.HINWEIS -> Unit
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val hinweis = when {
                gesperrt -> "Anmeldung nötig ($item.untertitel)"
                item.aktion == ServerAktion.HINWEIS -> item.untertitel.orEmpty()
                else -> item.untertitel.orEmpty()
            }
            if (hinweis.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    hinweis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        EndIcon(item.aktion, gesperrt, demo)
    }
}

@Composable
private fun EndIcon(aktion: ServerAktion, gesperrt: Boolean, demo: Boolean) {
    val anmeldungGespert = aktion == ServerAktion.ANMELDUNG && !demo
    val icon: ImageVector
    val desc: String
    when {
        gesperrt || anmeldungGespert -> {
            icon = if (anmeldungGespert) Icons.Filled.ChevronRight else Icons.Filled.Lock
            desc = if (anmeldungGespert) "" else "Anmeldung nötig"
        }
        aktion == ServerAktion.BROWSER -> {
            icon = Icons.AutoMirrored.Filled.OpenInNew
            desc = "Im Browser öffnen"
        }
        aktion == ServerAktion.HINWEIS -> {
            icon = Icons.Filled.ChevronRight
            desc = ""
        }
        else -> {
            icon = Icons.Filled.ChevronRight
            desc = "Öffnen"
        }
    }
    Icon(
        icon,
        contentDescription = desc,
        tint = if (gesperrt || anmeldungGespert) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    )
}