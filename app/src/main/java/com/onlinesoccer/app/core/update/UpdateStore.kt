package com.onlinesoccer.app.core.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)

    fun darfPruefen(now: Long): Boolean =
        now - prefs.getLong(KEY_LETZTE_PRUEFUNG, 0L) >= PRUEF_INTERVALL_MS

    fun pruefungGespeichert(now: Long) {
        prefs.edit().putLong(KEY_LETZTE_PRUEFUNG, now).apply()
    }

    fun istGelesen(version: String): Boolean = prefs.getString(KEY_GELESENE_VERSION, null) == version

    fun alsGelesenMarkieren(version: String) {
        prefs.edit().putString(KEY_GELESENE_VERSION, version).apply()
    }

    fun verfuegbaresUpdate(): UpdateInfo? {
        val version = prefs.getString(KEY_UPDATE_VERSION, null) ?: return null
        val url = prefs.getString(KEY_UPDATE_URL, null) ?: return null
        return UpdateInfo(version, url)
    }

    fun updateSpeichern(update: UpdateInfo?) {
        prefs.edit().apply {
            if (update == null) {
                remove(KEY_UPDATE_VERSION)
                remove(KEY_UPDATE_URL)
            } else {
                putString(KEY_UPDATE_VERSION, update.version)
                putString(KEY_UPDATE_URL, update.releaseUrl)
            }
        }.apply()
    }

    private companion object {
        const val KEY_LETZTE_PRUEFUNG = "letzte_pruefung"
        const val KEY_GELESENE_VERSION = "gelesene_version"
        const val KEY_UPDATE_VERSION = "update_version"
        const val KEY_UPDATE_URL = "update_url"
        const val PRUEF_INTERVALL_MS = 7 * 24 * 60 * 60 * 1000L
    }
}
