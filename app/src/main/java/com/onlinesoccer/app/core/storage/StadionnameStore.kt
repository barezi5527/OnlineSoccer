package com.onlinesoccer.app.core.storage

import android.content.Context
import com.onlinesoccer.app.data.model.StadionnameLogik
import com.onlinesoccer.app.data.model.StadionnameOverride
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class StadionnameStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun lesen(teamId: Long): StadionnameOverride? = withContext(Dispatchers.IO) {
        if (teamId <= 0) return@withContext null
        val name = prefs.getString(nameKey(teamId), null)?.let(StadionnameLogik::normalisiere)
        if (name.isNullOrBlank() || !StadionnameLogik.istGueltig(name)) return@withContext null
        val aktivSeit = prefs.getLong(aktivKey(teamId), 0L)
        if (aktivSeit <= 0L) return@withContext null
        StadionnameOverride(teamId = teamId, name = name, aktivSeitMillis = aktivSeit)
    }

    suspend fun speichern(
        teamId: Long,
        name: String,
        aktivSeitMillis: Long = System.currentTimeMillis(),
    ): Boolean = withContext(Dispatchers.IO) {
        val normalisiert = StadionnameLogik.normalisiere(name)
        if (teamId <= 0 || !StadionnameLogik.istGueltig(normalisiert) || aktivSeitMillis <= 0L) {
            return@withContext false
        }
        prefs.edit()
            .putString(nameKey(teamId), normalisiert)
            .putLong(aktivKey(teamId), aktivSeitMillis)
            .commit()
    }

    suspend fun loeschen(teamId: Long): Boolean = withContext(Dispatchers.IO) {
        if (teamId <= 0) return@withContext false
        prefs.edit()
            .remove(nameKey(teamId))
            .remove(aktivKey(teamId))
            .commit()
    }

    private fun nameKey(teamId: Long): String = "$VERSION_NAME$teamId"
    private fun aktivKey(teamId: Long): String = "$VERSION_AKTIV$teamId"

    private companion object {
        const val PREFS_NAME = "stadionname"
        const val VERSION_NAME = "v1_name_"
        const val VERSION_AKTIV = "v1_aktiv_"
    }
}
