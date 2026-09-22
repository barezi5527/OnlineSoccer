package com.onlinesoccer.app.ui

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.onlinesoccer.app.core.auth.AuthUiState
import com.onlinesoccer.app.feature.auth.login.LoginScreen
import com.onlinesoccer.app.feature.bewerbe.BewerbeScreen
import com.onlinesoccer.app.feature.bewerbe.InternationaleScreen
import com.onlinesoccer.app.feature.bewerbe.SpielberichtScreen
import com.onlinesoccer.app.feature.dashboard.DashboardScreen
import com.onlinesoccer.app.feature.pm.PmScreen
import com.onlinesoccer.app.feature.server.FreieTeamsScreen
import com.onlinesoccer.app.feature.server.ManagerSucheScreen
import com.onlinesoccer.app.feature.server.ManagerlisteScreen
import com.onlinesoccer.app.feature.server.ServerBereicheScreen
import com.onlinesoccer.app.feature.server.ZweitteamsScreen
import com.onlinesoccer.app.feature.statistik.FairplayScreen
import com.onlinesoccer.app.feature.statistik.SpielerscoutScreen
import com.onlinesoccer.app.feature.statistik.SpielersucheScreen
import com.onlinesoccer.app.feature.statistik.SpielstatistikenScreen
import com.onlinesoccer.app.feature.statistik.SpielervergleichScreen
import com.onlinesoccer.app.feature.statistik.StatistikBereich
import com.onlinesoccer.app.feature.statistik.StatistikenScreen
import com.onlinesoccer.app.feature.statistik.TabellenstatistikenScreen
import com.onlinesoccer.app.feature.statistik.TopscorerScreen
import com.onlinesoccer.app.feature.statistik.TopspielerScreen
import com.onlinesoccer.app.feature.statistik.TopteamsScreen
import com.onlinesoccer.app.feature.spiele.SpieleScreen
import com.onlinesoccer.app.feature.team.SeiteScreen
import com.onlinesoccer.app.feature.team.SpielerkarteScreen
import com.onlinesoccer.app.feature.team.VereinBereichScreen
import com.onlinesoccer.app.feature.team.VereinScreen
import com.onlinesoccer.app.data.model.LetzteAktionenArt
import com.onlinesoccer.app.data.model.TeamInfoMenuEintrag
import com.onlinesoccer.app.feature.transfers.EigeneGeboteScreen
import com.onlinesoccer.app.feature.transfers.LeihUebersichtScreen
import com.onlinesoccer.app.feature.transfers.LetzteAktionenScreen
import com.onlinesoccer.app.feature.transfers.TransferBereich
import com.onlinesoccer.app.feature.transfers.TransferListeScreen
import com.onlinesoccer.app.feature.transfers.TransferMarktScreen
import com.onlinesoccer.app.feature.transfers.TransferStatusScreen
import com.onlinesoccer.app.feature.transfers.TransfersScreen
import com.onlinesoccer.app.feature.transfers.VersteigerungsmarktScreen
import com.onlinesoccer.app.feature.transfers.VmSetzenScreen
import com.onlinesoccer.app.feature.zat.ZatScreen
import com.onlinesoccer.app.feature.zat.ZatReportScreen
import com.onlinesoccer.app.feature.team.TeaminformationenContentScreen
import com.onlinesoccer.app.feature.team.TeaminformationenScreen
import com.onlinesoccer.app.feature.team.TeamScreen
import kotlin.math.roundToInt

object Routes {
    const val DASHBOARD = "dashboard"
    const val TEAM = "team"
    const val ZAT = "zat"
    const val BEWERBE = "bewerbe"
    const val INTERNATIONALE_BEWERBE = "internationale-bewerbe"
    const val NACHRICHTEN = "nachrichten"
    const val SPIELER = "spieler/{pid}?teamId={teamId}&quelle={quelle}"

    /** Herkunft einer Spielerkarte: aus dem Transfermarkt geöffnet (zeigt „Bieten"). */
    const val SPIELER_QUELLE_TM = "tm"
    const val VEREIN = "verein/{teamId}"
    const val VEREIN_BEREICH = "verein-bereich"
    const val TEAMINFO = "teaminfo"
    const val TEAMINFO_CONTENT = "teaminfo-content/{eintragId}?teamId={teamId}&label={label}"
    const val BERICHT = "bericht?sid={sid}&url={url}"
    const val ZAT_REPORT = "zat-report?zat={zat}&saison={saison}"
    const val SEITE = "seite/{path}"
    const val SERVER_SEITE = "server-seite/{path}"
    const val SERVER = "server"
    const val FREIE_TEAMS = "freie-teams"
    const val FREIE_ZWEITTEAMS = "freie-zweitteams"
    const val MANAGERLISTE = "managerliste"
    const val MANAGERSUCHE = "managersuche"
    const val TRANSFERS = "transfers"
    const val TRANSFER_LISTE = "transferliste"
    const val TRANSFER_MARKT = "transfermarkt"
    const val VERSTEIGERUNGSMARKT = "versteigerungsmarkt"
    const val VM_SETZEN = "vm-setzen"
    const val EIGENE_GEBOTE = "eigene-gebote"
    const val LEIH_UEBERSICHT = "leih-uebersicht"
    const val TRANSFER_STATUS = "transfer-status"
    const val LETZTE_AKTIONEN = "letzte-aktionen/{art}"
    const val SPIELE = "matchcenter"
    const val STATISTIKEN = "statistik"
    const val STATISTIK_TOPSCORER = "statistik/topscorer"
    const val STATISTIK_TOP_SPIELER = "statistik/top-spieler"
    const val STATISTIK_TOP_TEAMS = "statistik/top-teams"
    const val STATISTIK_FAIRPLAY = "statistik/fairplay"
    const val STATISTIK_SPIELERSCOUT = "statistik/spielerscout"
    const val STATISTIK_SPIELERSUCHE = "statistik/spielersuche"
    const val STATISTIK_SPIELERVERGLEICH = "statistik/spielervergleich"
    const val STATISTIK_SPIELSTATISTIKEN = "statistik/spielstatistiken"
    const val STATISTIK_TABELLENSTATISTIKEN = "statistik/tabellenstatistiken"
}

@Composable
fun AppRoot(
    viewModel: AppViewModel = viewModel(),
    themeDark: Boolean = false,
    themeFollowsSystem: Boolean = false,
    onThemeCycle: () -> Unit = {},
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val vertragsWarnung by viewModel.vertragsWarnung.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        when (authState) {
            AuthUiState.Restoring -> Box(Modifier.fillMaxSize())
            AuthUiState.SignedOut -> LoginScreen(viewModel = viewModel)
            AuthUiState.SignedIn -> MainScaffold(
                onLogout = viewModel::logout,
                themeDark = themeDark,
                themeFollowsSystem = themeFollowsSystem,
                onThemeCycle = onThemeCycle,
            )
            AuthUiState.SignedInDemo -> MainScaffold(
                demo = true,
                onLogout = viewModel::logout,
                themeDark = themeDark,
                themeFollowsSystem = themeFollowsSystem,
                onThemeCycle = onThemeCycle,
            )
        }
        if (authState == AuthUiState.SignedIn && vertragsWarnung.isNotEmpty()) {
            VertragsWarnungDialog(
                vertraege = vertragsWarnung,
                onDismiss = viewModel::dismissVertragsWarnung,
            )
        }
    }
}

/** Ziel-Route eines Transfer-Menüeintrags. */
private fun routeFuerBereich(bereich: TransferBereich): String = when (bereich) {
    TransferBereich.TRANSFERLISTE -> Routes.TRANSFER_LISTE
    TransferBereich.TRANSFERMARKT -> Routes.TRANSFER_MARKT
    TransferBereich.VERSTEIGERUNGSMARKT -> Routes.VERSTEIGERUNGSMARKT
    TransferBereich.VM_SETZEN -> Routes.VM_SETZEN
    TransferBereich.EIGENE_GEBOTE -> Routes.EIGENE_GEBOTE
    TransferBereich.LEIH_UEBERSICHT -> Routes.LEIH_UEBERSICHT
    TransferBereich.TRANSFERSTATUS -> Routes.TRANSFER_STATUS
    TransferBereich.LETZTE_TRANSFERS -> Routes.LETZTE_AKTIONEN.replace("{art}", LetzteAktionenArt.TRANSFERS.routeId)
    TransferBereich.LETZTE_LEIHEN -> Routes.LETZTE_AKTIONEN.replace("{art}", LetzteAktionenArt.LEIHEN.routeId)
    TransferBereich.LETZTE_VM -> Routes.LETZTE_AKTIONEN.replace("{art}", LetzteAktionenArt.VM.routeId)
    TransferBereich.LETZTE_TM -> Routes.LETZTE_AKTIONEN.replace("{art}", LetzteAktionenArt.TM.routeId)
    TransferBereich.LETZTE_BLITZ -> Routes.LETZTE_AKTIONEN.replace("{art}", LetzteAktionenArt.BLITZ.routeId)
}

/** Ziel-Route eines Statistiken-Eintrags. */
private fun statistikRoute(bereich: StatistikBereich): String? = when (bereich) {
    StatistikBereich.TOPTSCORER -> Routes.STATISTIK_TOPSCORER
    StatistikBereich.TOP_SPIELER -> Routes.STATISTIK_TOP_SPIELER
    StatistikBereich.TOP_TEAMS -> Routes.STATISTIK_TOP_TEAMS
    StatistikBereich.FAIRPLAY -> Routes.STATISTIK_FAIRPLAY
    StatistikBereich.SPIELERSCOUT -> Routes.STATISTIK_SPIELERSCOUT
    StatistikBereich.SPIELERSUCHE -> Routes.STATISTIK_SPIELERSUCHE
    StatistikBereich.SPIELERVERGLEICH -> Routes.STATISTIK_SPIELERVERGLEICH
    StatistikBereich.SPIELSTATISTIKEN -> Routes.STATISTIK_SPIELSTATISTIKEN
    StatistikBereich.TABELLENSTATISTIKEN -> Routes.STATISTIK_TABELLENSTATISTIKEN
}

private fun titelFuer(route: String?, demo: Boolean = false, art: String? = null, label: String? = null): String {
    val basis = when (route) {
        Routes.DASHBOARD -> "Dashboard"
        Routes.TEAM -> "Team"
        Routes.ZAT -> "Zugabgabe / ZAT"
        Routes.BEWERBE -> "Nationale Bewerbe"
        Routes.INTERNATIONALE_BEWERBE -> "Intern. Bewerbe"
        Routes.NACHRICHTEN -> "Nachrichten"
        Routes.SPIELE -> "Matchcenter"
        Routes.SPIELER -> "Spielerkarte"
        Routes.VEREIN -> "Verein"
        Routes.VEREIN_BEREICH -> "Verein"
        Routes.TEAMINFO -> "Teaminformationen"
        Routes.TEAMINFO_CONTENT -> label?.takeIf { it.isNotBlank() }?.let { Uri.decode(it) } ?: "Teaminformationen"
        Routes.BERICHT -> "Spielbericht"
        Routes.ZAT_REPORT -> "ZAT-Report"
        Routes.SEITE -> "Team"
        Routes.SERVER_SEITE -> "Weitere Bereiche"
        Routes.SERVER -> "Weitere Bereiche"
        Routes.FREIE_TEAMS -> "Freie Teams"
        Routes.FREIE_ZWEITTEAMS -> "Freie Zweitteams"
        Routes.MANAGERLISTE -> "Team-/Managerliste"
        Routes.MANAGERSUCHE -> "Team-/Managersuche"
        Routes.TRANSFERS -> "Transfers"
        Routes.TRANSFER_LISTE -> "Transferliste"
        Routes.TRANSFER_MARKT -> "Transfermarkt"
        Routes.VERSTEIGERUNGSMARKT -> "Versteigerungsmarkt"
        Routes.VM_SETZEN -> "Auf den VM setzen"
        Routes.EIGENE_GEBOTE -> "Eigene Gebote"
        Routes.LEIH_UEBERSICHT -> "Leihspieler Übersicht"
        Routes.TRANSFER_STATUS -> "Transferstatus"
        Routes.LETZTE_AKTIONEN -> LetzteAktionenArt.vonRouteId(art.orEmpty())?.titel ?: "Letzte Transfers"
        Routes.STATISTIKEN -> "Statistiken"
        Routes.STATISTIK_TOPSCORER -> "Topscorer"
        Routes.STATISTIK_TOP_SPIELER -> "Top-Spieler"
        Routes.STATISTIK_TOP_TEAMS -> "Top-Teams"
        Routes.STATISTIK_FAIRPLAY -> "Fairplay"
        Routes.STATISTIK_SPIELERSCOUT -> "Spielerscout"
        Routes.STATISTIK_SPIELERSUCHE -> "Spielersuche"
        Routes.STATISTIK_SPIELERVERGLEICH -> "Spielervergleich"
        Routes.STATISTIK_SPIELSTATISTIKEN -> "Spielstatistiken"
        Routes.STATISTIK_TABELLENSTATISTIKEN -> "Tabellenstatistiken"
        else -> "Online Soccer"
    }
    return if (demo) "$basis · Demo" else basis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    demo: Boolean = false,
    onLogout: () -> Unit,
    themeDark: Boolean,
    themeFollowsSystem: Boolean,
    onThemeCycle: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var dashboardRefreshTrigger by remember { mutableStateOf(0) }

    fun parseSid(sid: String?): String? = sid?.takeIf { it.isNotBlank() }

    fun buildSpielerRoute(pid: Long, teamId: Long?, quelle: String = ""): String =
        Routes.SPIELER
            .replace("{pid}", pid.toString())
            .replace("{teamId}", teamId?.toString().orEmpty())
            .replace("{quelle}", quelle)

    /** Route zu einem Teaminformationen-Unterpunkt; `null`, wenn kein bekannter Typ vorliegt. */
    fun buildTeaminfoContentRoute(eintrag: TeamInfoMenuEintrag): String? {
        val id = eintrag.showteamS?.let { "s$it" }
            ?: eintrag.tabellenplatzTeamId?.let { "tp" }
            ?: return null
        return Routes.TEAMINFO_CONTENT
            .replace("{eintragId}", id)
            .replace("{teamId}", eintrag.tabellenplatzTeamId?.toString() ?: "0")
            .replace("{label}", Uri.encode(eintrag.label))
    }

    /** Baue Bericht-Route aus Sid und/oder statischer URL; `sid` hat Vorrang. */
    fun buildBerichtRoute(sid: String?, url: String?): String? {
        val s = parseSid(sid)
        return Routes.BERICHT
            .replace("{sid}", Uri.encode(s.orEmpty()))
            .replace("{url}", Uri.encode(url.orEmpty()))
            .let { if (s == null && url.isNullOrBlank()) null else it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        titelFuer(
                            currentRoute,
                            demo,
                            art = backStackEntry?.arguments?.getString("art"),
                            label = backStackEntry?.arguments?.getString("label"),
                        ),
                    )
                },
                actions = {
                    IconButton(onClick = onThemeCycle) {
                        Icon(
                            imageVector = when {
                                themeFollowsSystem -> Icons.Filled.BrightnessAuto
                                themeDark -> Icons.Filled.DarkMode
                                else -> Icons.Filled.LightMode
                            },
                            contentDescription = "Theme wechseln",
                        )
                    }
                    if (currentRoute != Routes.NACHRICHTEN) {
                        IconButton(onClick = {
                            navController.navigate(Routes.NACHRICHTEN) {
                                launchSingleTop = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.MailOutline,
                                contentDescription = "Nachrichten",
                            )
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Abmelden",
                        )
                    }
                },
            )
        },
        bottomBar = {
            ResponsiveNavigationBar(
                currentRoute = currentRoute,
                onSelectTab = { tab ->
                    if (tab.route == Routes.DASHBOARD) dashboardRefreshTrigger++
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    refreshTrigger = dashboardRefreshTrigger,
                    onLiveClick = {
                        navController.navigate(Routes.SPIELE) { launchSingleTop = true }
                    },
                     onBerichtClick = { url ->
                         buildBerichtRoute(null, url)?.let { route ->
                             navController.navigate(route) { launchSingleTop = true }
                         }
                    },
                    onZatReportClick = { zat, saison ->
                        navController.navigate(
                            Routes.ZAT_REPORT
                                .replace("{zat}", zat?.toString().orEmpty())
                                .replace("{saison}", saison?.toString().orEmpty()),
                        ) { launchSingleTop = true }
                    },
                    onZugabgabeClick = {
                        navController.navigate(Routes.ZAT) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    },
                    onServerBereicheClick = {
                        navController.navigate(Routes.SERVER) { launchSingleTop = true }
                    },
                )
            }
            composable(Routes.SERVER) {
                ServerBereicheScreen(
                    demo = demo,
                    onClose = { navController.popBackStack() },
                    onFreieTeams = {
                        navController.navigate(Routes.FREIE_TEAMS) { launchSingleTop = true }
                    },
                    onFreieZweitteams = {
                        navController.navigate(Routes.FREIE_ZWEITTEAMS) { launchSingleTop = true }
                    },
                    onManagerliste = {
                        navController.navigate(Routes.MANAGERLISTE) { launchSingleTop = true }
                    },
                    onManagersuche = {
                        navController.navigate(Routes.MANAGERSUCHE) { launchSingleTop = true }
                    },
                    onSeite = { path ->
                        navController.navigate(
                            Routes.SERVER_SEITE.replace("{path}", Uri.encode(path)),
                        ) { launchSingleTop = true }
                    },
                    onAnmeldung = onLogout,
                )
            }
            composable(Routes.FREIE_TEAMS) {
                FreieTeamsScreen(
                    onClose = { navController.popBackStack() },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.FREIE_ZWEITTEAMS) {
                ZweitteamsScreen(
                    onClose = { navController.popBackStack() },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.MANAGERLISTE) {
                ManagerlisteScreen(
                    onClose = { navController.popBackStack() },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.MANAGERSUCHE) {
                ManagerSucheScreen(
                    onClose = { navController.popBackStack() },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.TEAM) {
                TeamScreen(
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                    onSeiteClick = { path ->
                        navController.navigate(Routes.SEITE.replace("{path}", Uri.encode(path))) {
                            launchSingleTop = true
                        }
                    },
                    onTeaminformationenClick = {
                        navController.navigate(Routes.TEAMINFO) { launchSingleTop = true }
                    },
                )
            }
            composable(Routes.TEAMINFO) {
                TeaminformationenScreen(
                    onClose = { navController.popBackStack() },
                    onEintragClick = { eintrag ->
                        val route = buildTeaminfoContentRoute(eintrag)
                        if (route != null) {
                            navController.navigate(route) { launchSingleTop = true }
                        } else {
                            navController.navigate(
                                Routes.SEITE.replace("{path}", Uri.encode(eintrag.path)),
                            ) { launchSingleTop = true }
                        }
                    },
                )
            }
            composable(
                Routes.TEAMINFO_CONTENT,
                arguments = listOf(
                    navArgument("eintragId") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                    navArgument("teamId") { type = androidx.navigation.NavType.LongType; defaultValue = 0L },
                    navArgument("label") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                TeaminformationenContentScreen(
                    eintragId = entry.arguments?.getString("eintragId"),
                    teamId = (entry.arguments?.getLong("teamId") ?: 0L).takeIf { it > 0 },
                    titel = Uri.decode(entry.arguments?.getString("label").orEmpty())
                        .ifBlank { "Teaminformationen" },
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                    onBerichtClick = { sid ->
                        buildBerichtRoute(sid, null)?.let { route ->
                            navController.navigate(route) { launchSingleTop = true }
                        }
                    },
                )
            }
            composable(Routes.VEREIN_BEREICH) {
                VereinBereichScreen(
                    onSeiteClick = { path ->
                        navController.navigate(Routes.SEITE.replace("{path}", Uri.encode(path))) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.ZAT) {
                ZatScreen(
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.TRANSFERS) {
                TransfersScreen(
                    onEintrag = { bereich ->
                        val ziel = routeFuerBereich(bereich)
                        navController.navigate(ziel) { launchSingleTop = true }
                    },
                )
            }
            composable(Routes.TRANSFER_LISTE) {
                TransferListeScreen(
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.TRANSFER_MARKT) {
                TransferMarktScreen(
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null, if (demo) "" else Routes.SPIELER_QUELLE_TM)) {
                            launchSingleTop = true
                        }
                    },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.VERSTEIGERUNGSMARKT) {
                VersteigerungsmarktScreen(
                    demo = demo,
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.VM_SETZEN) {
                VmSetzenScreen(
                    demo = demo,
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.EIGENE_GEBOTE) {
                EigeneGeboteScreen(
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.LEIH_UEBERSICHT) {
                LeihUebersichtScreen(
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.TRANSFER_STATUS) {
                TransferStatusScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Routes.LETZTE_AKTIONEN,
                arguments = listOf(navArgument("art") { type = androidx.navigation.NavType.StringType }),
            ) {
                LetzteAktionenScreen(
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.BEWERBE) {
                BewerbeScreen(
                    demo = demo,
                    onSpielbericht = { sid, url ->
                        val route = buildBerichtRoute(sid, url) ?: return@BewerbeScreen
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                    onSpielerKarte = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.INTERNATIONALE_BEWERBE) {
                InternationaleScreen(
                    onSpielbericht = { sid, url ->
                        val route = buildBerichtRoute(sid, url) ?: return@InternationaleScreen
                        navController.navigate(route) { launchSingleTop = true }
                    },
                )
            }
            composable(Routes.SPIELE) { SpieleScreen() }
            composable(Routes.NACHRICHTEN) {
                PmScreen(onClose = { navController.popBackStack() })
            }
            composable(
                Routes.SPIELER,
                arguments = listOf(
                    navArgument("pid") { type = androidx.navigation.NavType.LongType; defaultValue = 0L },
                    navArgument("teamId") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                    navArgument("quelle") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                ),
            ) {
                SpielerkarteScreen(onClose = { navController.popBackStack() })
            }
            composable(
                Routes.VEREIN,
                arguments = listOf(navArgument("teamId") { type = androidx.navigation.NavType.LongType }),
            ) { entry ->
                val teamId = entry.arguments?.getLong("teamId") ?: 0L
                VereinScreen(
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, teamId)) {
                            launchSingleTop = true
                        }
                    },
                    onClose = { navController.popBackStack() },
                )
            }
            composable(
                Routes.BERICHT,
                arguments = listOf(
                    navArgument("sid") { defaultValue = "" },
                    navArgument("url") { defaultValue = "" },
                ),
            ) {
                SpielberichtScreen(onClose = { navController.popBackStack() })
            }
            composable(
                Routes.ZAT_REPORT,
                arguments = listOf(
                    navArgument("zat") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                    navArgument("saison") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                ),
            ) {
                ZatReportScreen(
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.STATISTIKEN) {
                StatistikenScreen(
                    onClose = { navController.popBackStack() },
                    onEintrag = { bereich ->
                        val route = statistikRoute(bereich)
                        if (route != null) {
                            navController.navigate(route) { launchSingleTop = true }
                        }
                    },
                )
            }
            composable(Routes.STATISTIK_TOPSCORER) {
                TopscorerScreen(
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.STATISTIK_TOP_SPIELER) {
                TopspielerScreen(
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.STATISTIK_TOP_TEAMS) {
                TopteamsScreen(
                    onClose = { navController.popBackStack() },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.STATISTIK_FAIRPLAY) {
                FairplayScreen(
                    onClose = { navController.popBackStack() },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.STATISTIK_SPIELERSCOUT) {
                SpielerscoutScreen(
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.STATISTIK_SPIELERSUCHE) {
                SpielersucheScreen(
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.STATISTIK_SPIELERVERGLEICH) {
                SpielervergleichScreen(
                    onClose = { navController.popBackStack() },
                    onSpielerClick = { pid ->
                        navController.navigate(buildSpielerRoute(pid, null)) {
                            launchSingleTop = true
                        }
                    },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.STATISTIK_SPIELSTATISTIKEN) {
                SpielstatistikenScreen(
                    onClose = { navController.popBackStack() },
                )
            }
            composable(Routes.STATISTIK_TABELLENSTATISTIKEN) {
                TabellenstatistikenScreen(
                    onClose = { navController.popBackStack() },
                    onTeamClick = { teamId ->
                        navController.navigate(Routes.VEREIN.replace("{teamId}", teamId.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.SEITE) {
                SeiteScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.SERVER_SEITE) {
                SeiteScreen(onClose = { navController.popBackStack() })
            }
        }
    }
}

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(Routes.DASHBOARD, "Dashboard", Icons.Filled.Home),
    BottomTab(Routes.TEAM, "Team", Icons.Filled.Groups),
    BottomTab(Routes.ZAT, "ZAT", Icons.Filled.SportsSoccer),
    BottomTab(Routes.BEWERBE, "Nationale\nBewerbe", Icons.Filled.EmojiEvents),
    BottomTab(Routes.INTERNATIONALE_BEWERBE, "Intern.\nBewerbe", Icons.Filled.Public),
    BottomTab(Routes.TRANSFERS, "Transfers", Icons.Filled.SwapHoriz),
    BottomTab(Routes.STATISTIKEN, "Statistiken", Icons.Filled.BarChart),
    BottomTab(Routes.VEREIN_BEREICH, "Verein", Icons.Filled.AccountBalance),
)

/**
 * Responsive untere Navigationsleiste.
 *
 * Smartphone-Hochformat: höchstens 5 Menüpunkte gleichzeitig.
 * Smartphone-Querformat: höchstens 6 Menüpunkte gleichzeitig.
 * Passt nicht alles in die Breite, wird die Leiste horizontal wischbar,
 * sodass jeder Bereich erreichbar bleibt. Bei großer Breite (Tablet/Desktop)
 * bleibt die bestehende gleichmäßige Darstellung erhalten.
 */
@Composable
private fun ResponsiveNavigationBar(
    currentRoute: String?,
    onSelectTab: (BottomTab) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bruttoBreiteOk = widthDp >= 600
    // Tablet/Desktop: alle Bereiche gleichmäßig, wie bisher.
    if (bruttoBreiteOk) {
        NavigationBar {
            bottomTabs.forEach { tab ->
                BottomTabItem(tab, tab.route == currentRoute) { onSelectTab(tab) }
            }
        }
        return
    }

    val maxGleichzeitig = if (isLandscape) 6 else 5
    val minBreiteProEintrag = 72
    val passend = maxOf(1, widthDp / minBreiteProEintrag)
    val sichtbar = minOf(bottomTabs.size, maxGleichzeitig, passend)
    val brauchtScroll = sichtbar < bottomTabs.size
    val eintragsBreite = widthDp.dp / sichtbar

    NavigationBar {
        if (!brauchtScroll) {
            bottomTabs.forEach { tab ->
                BottomTabItem(tab, tab.route == currentRoute) { onSelectTab(tab) }
            }
            return@NavigationBar
        }

        val scrollState = rememberScrollState()
        val ausgewaehlt = bottomTabs.indexOfFirst { it.route == currentRoute }
        val densityPx = LocalDensity.current.density
        LaunchedEffect(ausgewaehlt, widthDp) {
            if (ausgewaehlt >= 0) {
                val eintragDp = widthDp / sichtbar.toFloat()
                val ziel = (eintragDp * ausgewaehlt - (widthDp - eintragDp) / 2.0f)
                    .coerceAtLeast(0f) * densityPx
                scrollState.animateScrollTo(ziel.roundToInt())
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
        ) {
            bottomTabs.forEachIndexed { index, tab ->
                Row(Modifier.width(eintragsBreite)) {
                    BottomTabItem(tab, tab.route == currentRoute) { onSelectTab(tab) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomTabItem(
    tab: BottomTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(tab.icon, contentDescription = null) },
        label = { Text(tab.label) },
    )
}
