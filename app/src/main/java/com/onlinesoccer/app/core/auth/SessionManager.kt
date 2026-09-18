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
        _state.value = if (verifySession()) AuthUiState.SignedIn else AuthUiState.SignedOut
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
                client.newCall(request).execute().use { response ->
                    val bytes = response.body?.bytes()
                    if (bytes != null && LoginErrorTexts.matches(bytes)) {
                        httpError = "Username oder Passwort ist falsch."
                    } else if (!response.isSuccessful) {
                        httpError = "Serverfehler (HTTP ${response.code})"
                    } else {
                        httpOk = true
                    }
                }
                if (httpOk && verifySession()) {
                    LoginResult.Success
                } else {
                    LoginResult.Failure(httpError ?: "Login fehlgeschlagen – bitte erneut versuchen.")
                }
            } catch (e: IOException) {
                LoginResult.Failure("Netzwerkfehler: ${e.message ?: "keine Verbindung"}")
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
    suspend fun guestLogin(): Boolean {
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

        val httpOk = withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { it.isSuccessful }
            } catch (e: IOException) {
                false
            }
        }

        if (httpOk && verifyDemoSession()) {
            _state.value = AuthUiState.SignedInDemo
            return true
        }
        _state.value = AuthUiState.SignedOut
        return false
    }

    /** Prüft per Hauptseite, ob eine persönliche Session existiert. */
    private suspend fun verifySession(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(OsApi.MAIN).build()
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: return@withContext false
                SessionGuard.isPersonalView(bytes)
            }
        } catch (e: IOException) {
            false
        }
    }

    /** Prüft per Hauptseite, ob die Gast-Session die Demo-Ansicht liefert. */
    private suspend fun verifyDemoSession(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(OsApi.MAIN).build()
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: return@withContext false
                SessionGuard.isDemoView(bytes)
            }
        } catch (e: IOException) {
            false
        }
    }

    private object LoginErrorTexts {
        private val TEXT = "Der Username oder das Passwort ist falsch."

        fun matches(bytes: ByteArray): Boolean =
            HtmlTools.serverText(bytes).contains(TEXT)
    }
}