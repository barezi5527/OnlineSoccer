package com.onlinesoccer.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.core.auth.AuthUiState
import com.onlinesoccer.app.core.auth.LoginResult
import com.onlinesoccer.app.core.auth.SessionManager
import com.onlinesoccer.app.core.storage.TokenStorage
import com.onlinesoccer.app.data.model.VertragZeile
import com.onlinesoccer.app.data.repository.DashboardRepository
import com.onlinesoccer.app.data.repository.PmRepository
import com.onlinesoccer.app.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val tokenStorage: TokenStorage,
    private val teamRepository: TeamRepository,
    private val pmRepository: PmRepository,
    private val dashboardRepository: DashboardRepository,
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

    init {
        viewModelScope.launch {
            sessionManager.restore()
            if (sessionManager.state.value == AuthUiState.SignedIn) {
                pruefeVertragslaufzeiten()
                ladeTeamIds()
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
     */
    private suspend fun beobachteAnmeldung() {
        sessionManager.state.collect { zustand ->
            when (zustand) {
                AuthUiState.SignedIn -> ladeTeamIds()
                AuthUiState.SignedOut, AuthUiState.SignedInDemo -> _zweitTeamId.value = null
                AuthUiState.Restoring -> Unit
            }
        }
    }

    private suspend fun ladeTeamIds() {
        _zweitTeamId.value = runCatching { teamRepository.ladeTeamIds()?.zweitTeamId }.getOrNull()
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
        _zweitTeamId.value = null
        viewModelScope.launch {
            dashboardRepository.invalidate()
            sessionManager.logout()
        }
    }
}
