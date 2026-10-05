package com.onlinesoccer.app.core.storage

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Speichert das Schließen des Jugendhinweises bis zur nächsten Anmeldung. */
@Singleton
class JugendHinweisStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun istAusgeblendet(teamId: Long?, hinweis: String?): Boolean {
        val key = key(teamId)
        val gespeichert = prefs.getString(key, null)
        val ausgeblendet = jugendHinweisSollAusgeblendet(hinweis, gespeichert)
        if (!ausgeblendet && gespeichert != null) prefs.edit { remove(key) }
        return ausgeblendet
    }

    fun ausblenden(teamId: Long?, hinweis: String?) {
        if (!hinweis.isNullOrBlank()) prefs.edit { putString(key(teamId), hinweis) }
    }

    /** Neue Anmeldung ist eine neue Hinweissitzung. */
    fun zuruecksetzen() {
        prefs.edit { clear() }
    }

    private fun key(teamId: Long?): String = "jugend_hinweis_${teamId?.takeIf { it > 0L } ?: 0L}"

    private companion object {
        const val PREFS_NAME = "dashboard_hinweise"
    }
}

/** Gleiche aktive Warnung bleibt geschlossen; eine geänderte oder fehlende wird wieder gezeigt. */
internal fun jugendHinweisSollAusgeblendet(aktuellerHinweis: String?, gespeicherterHinweis: String?): Boolean =
    !aktuellerHinweis.isNullOrBlank() && aktuellerHinweis == gespeicherterHinweis
