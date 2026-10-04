package com.onlinesoccer.app.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Verschlüsselte Ablage der Session-Tokens.
 *
 * `lc` ist wie ein Passwort zu behandeln: Der Server stellt darauf still eine neue
 * `os`-SID aus (stiller Relogin). Niemals loggen oder im Klartext speichern.
 *
 * Der `by lazy`-Block ist teuer (MasterKey anlegen, Datei entschlüsseln) und würde beim
 * ersten Zugriff vom Main-Thread laufen, also vor dem ersten Frame blockieren. Deshalb
 * muss [waermeAuf] laufen, bevor ein Main-Thread auf [lc], [os] oder [lastEmail] zugreift.
 */
@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Persistente Sitzungskennung (2 Jahre Laufzeit). */
    var lc: String?
        get() = prefs.getString(KEY_LC, null)
        set(value) {
            prefs.edit().putString(KEY_LC, value).apply()
        }

    /** Flüchtige Sitzungskennung (wird bei jedem stillen Relogin erneuert). */
    var os: String?
        get() = prefs.getString(KEY_OS, null)
        set(value) {
            prefs.edit().putString(KEY_OS, value).apply()
        }

    /** Zuletzt verwendeter Login (für vorbefülltes Feld). */
    private var lastEmail: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) {
            prefs.edit().putString(KEY_EMAIL, value).apply()
        }

    fun hasSession(): Boolean = !lc.isNullOrEmpty()

    fun clearSession() {
        prefs.edit().remove(KEY_LC).remove(KEY_OS).apply()
    }

    /**
     * Erzeugt [prefs] einmalig im Hintergrund.
     *
     * Einzige Voraussetzung dafür, dass [lc], [os] und [lastEmail] danach auch vom
     * Main-Thread sicher sind: ab diesem Zeitpunkt sind alle Werte im Speicher.
     */
    suspend fun waermeAuf() {
        withContext(Dispatchers.IO) { prefs }
    }

    /** Liest den zuletzt verwendeten Login im Hintergrund. */
    suspend fun lastEmailLaden(): String? = withContext(Dispatchers.IO) { lastEmail }

    /** Speichert den zuletzt verwendeten Login im Hintergrund. */
    suspend fun speichereLastEmail(email: String) {
        withContext(Dispatchers.IO) { lastEmail = email }
    }

    private companion object {
        const val FILE_NAME = "os_session_secure"
        const val KEY_LC = "cookie_lc"
        const val KEY_OS = "cookie_os"
        const val KEY_EMAIL = "last_email"
    }
}