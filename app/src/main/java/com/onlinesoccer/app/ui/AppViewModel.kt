package com.onlinesoccer.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.core.auth.AuthUiState
import com.onlinesoccer.app.core.auth.LoginResult
import com.onlinesoccer.app.core.auth.SessionManager
import com.onlinesoccer.app.core.storage.TokenStorage
import com.onlinesoccer.app.data.model.TeamIds
import com.onlinesoccer.app.data.model.TeamwechselErgebnis
import com.onlinesoccer.app.data.model.VertragZeile
import com.onlinesoccer.app.data.repository.DashboardRepository
import com.onlinesoccer.app.data.repository.PmRepository
import com.onlinesoccer.app.data.repository.TeamRepository
import com.onlinesoccer.app.data.repository.TeamwechselRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Höchste Restlaufzeit (in ZAT), ab der beim Login gewarnt wird. */
internal const val VERTRAGS_WARNUNG_ZAT = 2

/** Abstand, in dem der Briefumschlag-Badge nach ungelesenen PMs fragt. */
internal const val UNGELESENE_POLL_INTERVALL_MS = 60_000L

/**
 * Spieler, deren Vertrag höchstens [schwelle] ZAT Restlaufzeit hat.
 * Nicht-numerische oder fehlende Laufzeitangaben werden ignoriert.
 */
internal fun vertraegeKurzVorAuslauf(
    vertraege: List<VertragZeile>,
    schwelle: Int = VERTRAGS_WARNUNG_ZAT,
): List<VertragZeile> = vertraege.filter { zeile ->
    zeile.laufzeit?.trim()?.toIntOrNull()?.let { it <= schwelle } == true
}

/**
 * Zustand des 1|2-Buttons.
 *
 * [aktiverIndex] ist **immer** aus dem Serverbefund abgeleitet und wird nie
 * persistiert: der Serverstand ist die Wahrheit, und er überlebt einen
 * Kaltstart — nach einem Neustart kann der Button daher „2" zeigen.
 */
internal data class TeamwechselUiState(
    /** Der Server kennt einen `changetosecond`-Anker, d. h. es gibt ein Zweitteam. */
    val wechselMoeglich: Boolean = false,
    /** Anzeigename des Zielteams (aus „Zu X wechseln"). */
    val zweitTeamName: String? = null,
    /** Team-ID des gerade aktiven Teams, aus dem Serverbefund. */
    val teamId: Long? = null,
    /** Anzeigename des gerade aktiven Teams, aus dem Dashboard-Befund. */
    val teamName: String? = null,
/**
     * Team-ID des Zweitteams.
     *
     * Aus `showteam.php?s=0` ermittelt und dabei **rollenkorrekt** umgerechnet
     * (siehe `ladeTeamIds()`): unabhängig davon, ob gerade das Haupt- oder das
     * Zweitteam aktiv ist, steht hier dasselbe Team. Deshalb ist der Wert auch
     * nach einem Kaltstart mit aktivem Team 2 gültig.
     */
    val zweitTeamId: Long? = null,
    /** Ein Wechsel läuft gerade — der Button ist dann deaktiviert. */
    val laeuft: Boolean = false,
    /** Kurzmeldung für die Snackbar (Erfolg nennt den Teamnamen aus dem Refetch). */
    val meldung: String? = null,
) {
    /**
     * 1 = Hauptteam, 2 = Zweitteam.
     *
     * Default 1: solange kein Zweitteam bekannt ist, gibt es nur „1" — ohne
     * Zweitteam wird der Button gar nicht gerendert.
     */
    val aktiverIndex: Int get() = if (zweitTeamId != null && teamId == zweitTeamId) 2 else 1
}

/**
 * Kurzmeldung zum Teamwechsel für die Snackbar.
 *
 * ⚠️ Erfolg nennt den Teamnamen aus dem **Refetch** ([TeamwechselErgebnis.Erfolgreich]),
 * nicht den Zweitteam-Namen aus dem `changetosecond`-Link: der benennt nach dem
 * Wechsel das eben **verlassene** Team.
 */
internal fun teamwechselMeldung(ergebnis: TeamwechselErgebnis): String = when (ergebnis) {
    is TeamwechselErgebnis.Erfolgreich ->
        ergebnis.teamName?.let { "Jetzt aktiv: $it" } ?: "Teamwechsel erfolgreich."
    TeamwechselErgebnis.Unveraendert ->
        "Der Server meldet weiterhin dasselbe Team – es wurde nicht gewechselt."
    is TeamwechselErgebnis.Fehler -> ergebnis.text
}

/**
 * Bestimmt aus dem Befund von `showteam.php?s=0` die ID des **festen**
 * Zweitteams — also des Teams, das im 1|2-Button konsequent „2" bleibt.
 *
 * `showteam.php` ist rollenrelativ: die Seite zeigt das gerade aktive Team, und
 * der Partner-Anker heißt entsprechend „Mein Zweitteam" oder „Mein Hauptteam".
 * Das Zweitteam ist deshalb der Partner — außer der Server nennt ihn
 * „Mein Hauptteam", dann ist das eigene Team das Zweitteam.
 *
 * Ohne diese Umrechnung zeigte der Button nach einem Kaltstart auf Team 2
 * dauerhaft „1". Ein Kaltstart ist dabei **kein** Ruhezustand: greift das
 * Cookie noch, wird nicht neu angemeldet und der Server bleibt auf Team 2.
 */
internal fun zweitTeamIdAus(ids: TeamIds?): Long? = when {
    ids == null -> null
    ids.teamId == null || ids.partnerTeamId == null -> null
    ids.teamId == ids.partnerTeamId -> null
    ids.partnerIstHauptteam -> ids.teamId
    else -> ids.partnerTeamId
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val tokenStorage: TokenStorage,
    private val teamRepository: TeamRepository,
    private val pmRepository: PmRepository,
    private val dashboardRepository: DashboardRepository,
    private val teamwechselRepository: TeamwechselRepository,
) : ViewModel() {

    val authState: StateFlow<AuthUiState> = sessionManager.state

    private val _email = MutableStateFlow(tokenStorage.lastEmail.orEmpty())
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _loggingIn = MutableStateFlow(false)
    val loggingIn: StateFlow<Boolean> = _loggingIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    /** True, wenn die Website nicht erreichbar war (ZAT-Spieltag/Störung). */
    private val _serverUnavailable = MutableStateFlow(false)
    val serverUnavailable: StateFlow<Boolean> = _serverUnavailable.asStateFlow()

    /** Spieler mit höchstens [VERTRAGS_WARNUNG_ZAT] ZAT Restlaufzeit (Popup beim Login). */
    private val _vertragsWarnung = MutableStateFlow<List<VertragZeile>>(emptyList())
    val vertragsWarnung: StateFlow<List<VertragZeile>> = _vertragsWarnung.asStateFlow()

    private val _ungeleseneNachrichten = MutableStateFlow(0)
    val ungeleseneNachrichten: StateFlow<Int> = _ungeleseneNachrichten.asStateFlow()

    /**
     * ID des Zweitteams, `null` wenn der Account keins besitzt.
     *
     * Wird **einmalig** nach dem Anmelden geladen und bewusst nicht
     * persistiert: ein stiller Re-Login erzeugt eine neue PHP-Session, in der
     * der Server auf das Hauptteam zurücksetzt — ein gespeicherter Wert wäre
     * nach jedem Kaltstart systematisch falsch.
     */
    private val _zweitTeamId = MutableStateFlow<Long?>(null)
    val zweitTeamId: StateFlow<Long?> = _zweitTeamId.asStateFlow()

    /** Zustand des 1|2-Buttons inkl. Serverbefund. */
    private val _teamwechsel = MutableStateFlow(TeamwechselUiState())
    internal val teamwechsel: StateFlow<TeamwechselUiState> = _teamwechsel.asStateFlow()

    private val _teamwechselAusgefuehrt = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Einmal-Signal „der Wechsel ist bestätigt" für die Navigation der UI.
     *
     * Bewusst **kein** State-Feld: `teamId` ändert sich auch beim Anmelden, ein
     * beobachteter State würde dort eine unnötige Navigation auslösen.
     *
     * `MutableSharedFlow` mit `replay = 0` und **nicht** als Kanal: wer gerade
     * nicht zuhört, soll das Signal nicht später bekommen — sonst würde es nach
     * einer Rotation noch einmal eine Navigation auslösen. Solange die TopAppBar
     * sichtbar ist (der Button liegt dort), ist ein Empfänger immer da.
     */
    val teamwechselAusgefuehrt: SharedFlow<Unit> = _teamwechselAusgefuehrt.asSharedFlow()

    init {
        viewModelScope.launch {
            sessionManager.restore()
            if (sessionManager.state.value == AuthUiState.SignedIn) {
                pruefeVertragslaufzeiten()
            } else {
                dashboardRepository.invalidate()
            }
            starteUngelesenePolling()
            beobachteAnmeldung()
        }
    }

    /**
     * Lädt die Team-IDs bei jedem Übergang in den Angemeldet-Zustand, aber
     * **nicht** bei jedem Screenwechsel. Gast-/Demo-Sitzungen haben kein
     * Zweitteam, dort bleibt der Wert `null`.
     *
     * Deckt auch den Kaltstart ab: [kotlinx.coroutines.flow.StateFlow.collect]
     * liefert den aktuellen Zustand sofort mit — deshalb wird `ladeTeamIds()`
     * hier **nicht** zusätzlich im `init` aufgerufen.
     */
    private suspend fun beobachteAnmeldung() {
        sessionManager.state.collect { zustand ->
            when (zustand) {
                AuthUiState.SignedIn -> ladeTeamIds()
                AuthUiState.SignedOut, AuthUiState.SignedInDemo -> teamwechselZuruecksetzen()
                AuthUiState.Restoring -> Unit
            }
        }
    }

    private suspend fun ladeTeamIds() {
        val ids = runCatching { teamRepository.ladeTeamIds() }.getOrNull()
        val zweitId = zweitTeamIdAus(ids)
        _zweitTeamId.value = zweitId
        _teamwechsel.value = _teamwechsel.value.copy(
            zweitTeamId = zweitId,
            wechselMoeglich = zweitId != null,
            // Das eigene Team der Seite ist das gerade aktive Team — und damit
            // der 1:1-Befund, den `wechsleTeam()` für den Vergleich vor und nach
            // dem Toggle braucht. Das gilt auch für einen Kaltstart, bei dem der
            // Server bereits auf Team 2 steht.
            teamId = ids?.teamId,
            teamName = null,
        )
    }

    /** Gast-/Abmeldezustand: kein Zweitteam, der 1|2-Button bleibt unsichtbar. */
    private fun teamwechselZuruecksetzen() {
        _zweitTeamId.value = null
        _teamwechsel.value = TeamwechselUiState()
    }

    /**
     * Meldung quittieren, sobald die Snackbar sie angezeigt hat.
     *
     * Ohne das würde dieselbe Meldung bei jeder Rekombination — und nach einer
     * Rotation erneut — wieder erscheinen. Das Leeren ist zugleich der Grund,
     * warum zwei gleichlautende Meldungen hintereinander trotzdem wieder
     * erscheinen: der Wert läuft zwischendurch auf `null`.
     */
    fun teamwechselMeldungQuittiert() {
        if (_teamwechsel.value.meldung != null) {
            _teamwechsel.value = _teamwechsel.value.copy(meldung = null)
        }
    }

    /**
     * Wechselt zwischen Haupt- und Zweitteam — **ein** Klick, **ein**
     * Schreibvorgang, **ein** Refetch.
     *
     * Kein Optimismus: der Button zeigt den neuen Wert erst, wenn der Server
     * ihn bestätigt hat. Der Backstack-Reset und das Neu-Binden der
     * ViewModel-Caches passieren in der UI (Plan T26/T27), dieser Pfad liefert
     * nur den Serverstand.
     */
    fun wechsleTeam() {
        // Regel 6: `laeuft` wird vor dem `launch` gesetzt — zwei Klicks in
        // derselben Frame passieren sonst beide die Prüfung. Zusätzlich
        // dedupliziert `TeamwechselRepository` den Toggle selbst.
        if (_teamwechsel.value.laeuft) return
        _teamwechsel.value = _teamwechsel.value.copy(laeuft = true, meldung = null)
        viewModelScope.launch {
            try {
                fuehreWechselAus()
            } catch (e: CancellationException) {
                // Kein Fehler: ein App-Kill mitten im Toggle ist harmlos, der
                // nächste Start liest den Server. Nur die Sperre aufräumen.
                _teamwechsel.value = _teamwechsel.value.copy(laeuft = false)
                throw e
            }
        }
    }

    private suspend fun fuehreWechselAus() {
        val stand = _teamwechsel.value
        // 1) Vorzustand aus dem letzten Serverbefund — nie persistiert (Regel 1).
        val vorher = stand.teamId?.let { it to stand.teamName }

        // 2) Genau ein Schreibvorgang (Regel 3). `null` = Fehler/Abbruch/laufend;
        //    es wird **nicht** nachgefasst, das wäre ein Rück-Toggle.
        val antwort = teamwechselRepository.teamwechselDurchfuehren()

        // 3) Cache leeren, bevor irgendjemand neu befüllt: der Toggle lief außerhalb
        //    von `DashboardRepository`, sonst könnte hier der Team-1-Stand
        //    liegen bleiben. `invalidate()` und `fetchDashboard` teilen denselben
        //    Mutex — ein laufender Fetch kann also nicht dazwischen einschieben.
        dashboardRepository.invalidate()

        // 4) Genau ein Refetch, unabhängig vom Toggle-Ergebnis: der gemeinsame
        //    Cache soll in jedem Fall den Stand des aktiven Teams zeigen.
        val refreshed = try {
            dashboardRepository.fetchDashboard(forceRefresh = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        // 5) Nachzustand = Server-Wahrheit. Der Refetch ist die belastbarste
        //    Quelle, die Toggle-Antwort die unabhängige zweite — sie greift, wenn
        //    der Refetch ausfiel. ⚠️ Nicht `refreshed.teamwechsel.zweitTeamName`:
        //    das ist das Team, in das man jetzt wechseln *könnte*.
        val nachher = refreshed?.teamId?.toLong()?.let { it to refreshed.teamName }
            ?: antwort?.let { teamwechselRepository.aktivesTeamAusHtml(it) }

        // 6) Bewertung: „Erfolg" heißt, der Server meldet jetzt ein anderes Team.
        val ergebnis = teamwechselRepository.werteAus(vorher, nachher)

        // Auf den aktuellen Stand schreiben, nicht auf [stand]: zwischen Toggle
        // und Refetch suspendiert der Pfad, da kann ein Logout dazwischenliegen.
        val neu = _teamwechsel.value
        _teamwechsel.value = neu.copy(
            laeuft = false,
            // Unbekannter Befund ⇒ Vorwert behalten statt auf null zu setzen.
            teamId = nachher?.first ?: neu.teamId,
            teamName = nachher?.second ?: neu.teamName,
            wechselMoeglich = refreshed?.teamwechsel?.wechselMoeglich ?: neu.wechselMoeglich,
            zweitTeamName = refreshed?.teamwechsel?.zweitTeamName ?: neu.zweitTeamName,
            meldung = teamwechselMeldung(ergebnis),
        )

        // Erst **nach** dem Zustandsschreiben: die UI navigiert daraufhin neu
        // auf, und der neue ViewModel liest den bereits aktualisierten Cache.
        // Nur ein bestätigter Wechsel löst die Navigation aus — bei einem Fehler
        // bleibt der Nutzer, wo er ist.
        if (ergebnis is TeamwechselErgebnis.Erfolgreich) {
            _teamwechselAusgefuehrt.tryEmit(Unit)
        }
    }

    /** Pollt periodisch die Anzahl ungelesener PMs für den Briefumschlag-Badge. */
    private fun starteUngelesenePolling() {
        viewModelScope.launch {
            while (isActive) {
                if (sessionManager.state.value == AuthUiState.SignedIn) {
                    aktualisiereUngelesene()
                }
                delay(UNGELESENE_POLL_INTERVALL_MS)
            }
        }
    }

    fun aktualisiereUngelesene() {
        viewModelScope.launch {
            val anzahl = runCatching { pmRepository.ungeleseneAnzahl() }.getOrDefault(_ungeleseneNachrichten.value)
            _ungeleseneNachrichten.value = anzahl
        }
    }

    /** Lädt die Vertragstabelle und warnt, sobald ein Vertrag höchstens 2 ZAT läuft. */
    private fun pruefeVertragslaufzeiten() {
        viewModelScope.launch {
            val kurz = runCatching {
                vertraegeKurzVorAuslauf(teamRepository.ladeVertraege())
            }.getOrDefault(emptyList())
            _vertragsWarnung.value = kurz
        }
    }

    fun dismissVertragsWarnung() {
        _vertragsWarnung.value = emptyList()
    }

    fun onEmailChange(value: String) {
        _email.value = value
    }

    fun onPasswordChange(value: String) {
        _password.value = value
    }

    fun login() {
        if (_loggingIn.value) return
        val email = _email.value.trim()
        val password = _password.value
        if (email.isEmpty() || password.isEmpty()) {
            _loginError.value = "Bitte Mail und Passwort eingeben."
            _serverUnavailable.value = false
            return
        }
        viewModelScope.launch {
            _loggingIn.value = true
            _loginError.value = null
            _serverUnavailable.value = false
            dashboardRepository.invalidate()
            tokenStorage.lastEmail = email
            when (val result = sessionManager.login(email, password)) {
                is LoginResult.Success -> {
                    pruefeVertragslaufzeiten()
                    aktualisiereUngelesene()
                }
                is LoginResult.Failure -> _loginError.value = result.message
                LoginResult.ServerUnavailable -> _serverUnavailable.value = true
            }
            _loggingIn.value = false
        }
    }

    fun guestLogin() {
        if (_loggingIn.value) return
        viewModelScope.launch {
            _loggingIn.value = true
            _loginError.value = null
            _serverUnavailable.value = false
            dashboardRepository.invalidate()
            when (val result = sessionManager.guestLogin()) {
                LoginResult.Success -> Unit
                is LoginResult.Failure -> _loginError.value = result.message
                LoginResult.ServerUnavailable -> _serverUnavailable.value = true
            }
            _loggingIn.value = false
        }
    }

    fun logout() {
        _vertragsWarnung.value = emptyList()
        _ungeleseneNachrichten.value = 0
        // Nicht persistiert (Regel 1) — der Wert gehört zur alten Anmeldung.
        teamwechselZuruecksetzen()
        viewModelScope.launch {
            dashboardRepository.invalidate()
            sessionManager.logout()
        }
    }
}
