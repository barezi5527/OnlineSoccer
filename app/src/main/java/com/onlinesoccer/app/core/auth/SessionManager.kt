package com.onlinesoccer.app.core.auth

import com.onlinesoccer.app.core.network.HtmlTools
import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.OsCookieStore
import com.onlinesoccer.app.core.network.SessionGuard
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

enum class AuthUiState {
    /** Erster Start / Session-Wiederherstellung läuft. */
    Restoring,

    /** Keine gültige Session – Login-Screen anzeigen. */
    SignedOut,

    /** Angemeldet – Haupt-Navigation anzeigen. */
    SignedIn,

    /**
     * Als Gast im Demo-Modus: anonyme Session mit Demo-Daten (wie der
     * „Gast"-Button auf der Website). Reicht bis App-/Server-Session-Ende.
     */
    SignedInDemo,
}

sealed interface LoginResult {
    data object Success : LoginResult
    data class Failure(val message: String) : LoginResult

    /**
     * Die Website ist nicht erreichbar: Timeout/Verbindungsfehler oder
     * HTTP 5xx – z. B. weil ein ZAT-Spieltag läuft oder eine Störung vorliegt.
     */
    data object ServerUnavailable : LoginResult
}

/**
 * Besitzt die Sitzung: Login, stiller Relogin über `lc`, Logout.
 *
 * Authentifiziert allein anhand der Serverantwort-Inhalte (Phase-2-SessionGuard).
 * Alle blockierenden Netzwerkaufrufe laufen auf [Dispatchers.IO].
 */
@Singleton
class SessionManager @Inject constructor(
    private val client: OkHttpClient,
    private val cookieStore: OsCookieStore,
) {

    private val _state = MutableStateFlow(AuthUiState.Restoring)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    /** Beim App-Start: Vorhandenes `lc` versuchen still zu verwerten. */
    suspend fun restore() {
        if (!cookieStore.hasSession()) {
            _state.value = AuthUiState.SignedOut
            return
        }
        _state.value = AuthUiState.Restoring
        _state.value = if (verifySession() == VerifyResult.Ok) {
            AuthUiState.SignedIn
        } else {
            AuthUiState.SignedOut
        }
    }

    suspend fun login(email: String, password: String): LoginResult {
        _state.value = AuthUiState.Restoring
        val body = FormBody.Builder()
            .add("action", "os_login")
            .add("loginemail", email)
            .add("passwort", password)
            .add("imageField.x", "10")
            .add("imageField.y", "10")
            .build()
        val request = Request.Builder()
            .url(OsApi.LOGIN)
            .post(body)
            .build()

        val result: LoginResult = withContext(Dispatchers.IO) {
            try {
                var httpOk = false
                var httpError: String? = null
                var serverDown = false
                client.newCall(request).execute().use { response ->
                    when {
                        response.code >= 500 -> serverDown = true
                        LoginErrorTexts.matches(response.body?.bytes() ?: ByteArray(0)) ->
                            httpError = "Username oder Passwort ist falsch."
                        !response.isSuccessful -> httpError = "Serverfehler (HTTP ${response.code})"
                        else -> httpOk = true
                    }
                }
                when {
                    serverDown -> LoginResult.ServerUnavailable
                    !httpOk ->
                        LoginResult.Failure(
                            httpError ?: "Login fehlgeschlagen – bitte erneut versuchen.",
                        )
                    else -> when (verifySession()) {
                        VerifyResult.Ok -> LoginResult.Success
                        VerifyResult.Network -> LoginResult.ServerUnavailable
                        VerifyResult.None ->
                            LoginResult.Failure("Login fehlgeschlagen – bitte erneut versuchen.")
                    }
                }
            } catch (e: IOException) {
                LoginResult.ServerUnavailable
            }
        }

        _state.value = if (result is LoginResult.Success) AuthUiState.SignedIn else AuthUiState.SignedOut
        return result
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(OsApi.INDEX).build()).execute().close()
            }
        }
        cookieStore.clear()
        _state.value = AuthUiState.SignedOut
    }

    /**
     * Gastzugang wie auf der Website: leerer Login-POST über den „Gast"-Button
     * (`imageField2`) erzeugt eine anonyme Demo-Session (kein `lc`-Token).
     * Danach zeigt der Server überall Demo-Daten („DemoTeam").
     */
    suspend fun guestLogin(): LoginResult {
        _state.value = AuthUiState.Restoring
        val body = FormBody.Builder()
            .add("action", "os_login")
            .add("loginemail", "")
            .add("passwort", "")
            .add("imageField2.x", "10")
            .add("imageField2.y", "10")
            .build()
        val request = Request.Builder()
            .url(OsApi.LOGIN)
            .post(body)
            .build()

        val result: LoginResult = withContext(Dispatchers.IO) {
            try {
                val vorPruefung = client.newCall(request).execute().use { response ->
                    when {
                        response.code >= 500 -> LoginResult.ServerUnavailable
                        !response.isSuccessful ->
                            LoginResult.Failure("Serverfehler (HTTP ${response.code})")
                        else -> null
                    }
                }
                vorPruefung ?: when (verifyDemoSession()) {
                    VerifyResult.Ok -> LoginResult.Success
                    VerifyResult.Network -> LoginResult.ServerUnavailable
                    VerifyResult.None ->
                        LoginResult.Failure("Demo-Zugang konnte nicht hergestellt werden.")
                }
            } catch (e: IOException) {
                LoginResult.ServerUnavailable
            }
        }

        _state.value = if (result is LoginResult.Success) {
            AuthUiState.SignedInDemo
        } else {
            AuthUiState.SignedOut
        }
        return result
    }

    /** Prüft per Hauptseite, ob eine persönliche Session existiert. */
    private suspend fun verifySession(): VerifyResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(OsApi.MAIN).build()
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: return@withContext VerifyResult.None
                if (response.code >= 500) {
                    VerifyResult.Network
                } else if (SessionGuard.isPersonalView(bytes)) {
                    VerifyResult.Ok
                } else {
                    VerifyResult.None
                }
            }
        } catch (e: IOException) {
            VerifyResult.Network
        }
    }

    /** Prüft per Hauptseite, ob die Gast-Session die Demo-Ansicht liefert. */
    private suspend fun verifyDemoSession(): VerifyResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(OsApi.MAIN).build()
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: return@withContext VerifyResult.None
                if (response.code >= 500) {
                    VerifyResult.Network
                } else if (SessionGuard.isDemoView(bytes)) {
                    VerifyResult.Ok
                } else {
                    VerifyResult.None
                }
            }
        } catch (e: IOException) {
            VerifyResult.Network
        }
    }

    /**
     * Ergebnis einer Server-Prüfung: [Ok] = gewünschte Sitzung aktiv,
     * [None] = nicht aktiv, [Network] = Website nicht erreichbar (5xx/Timeout).
     */
    private sealed interface VerifyResult {
        data object Ok : VerifyResult
        data object None : VerifyResult
        data object Network : VerifyResult
    }

    private object LoginErrorTexts {
        private val TEXT = "Der Username oder das Passwort ist falsch."

        fun matches(bytes: ByteArray): Boolean =
            HtmlTools.serverText(bytes).contains(TEXT)
    }
}