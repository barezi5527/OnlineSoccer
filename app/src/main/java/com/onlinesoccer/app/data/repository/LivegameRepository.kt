package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.LiveEreignis
import com.onlinesoccer.app.data.model.LivegameData
import com.onlinesoccer.app.data.model.StatistikWerte
import com.onlinesoccer.app.data.model.TaktikWerte
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Livegame-Datenquelle (JSON-Endpunkt, Phase-2 §15.1).
 *
 * `data.php?action=gamedata&teamid=<Heim-ID>&zat=<ZAT>` liefert Live-Aktionen,
 * Aufstellungen, Taktik und Statistiken als JSON.
 */
@Singleton
class LivegameRepository @Inject constructor(
    private val client: OkHttpClient,
) {

    suspend fun ladeSpiel(teamId: Int, zat: Int): LivegameData = withContext(Dispatchers.IO) {
        val url = OsApi.BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments("livegame/php/data.php")
            .addQueryParameter("action", "gamedata")
            .addQueryParameter("teamid", teamId.toString())
            .addQueryParameter("zat", zat.toString())
            .build()
        val request = Request.Builder().url(url).build()
        val body = try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use ""
                response.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            throw IOException("Livegame konnte nicht geladen werden: ${e.message}", e)
        }
        if (SessionGuard.isLoginView(body.toByteArray()) || body.isBlank()) {
            throw IOException("Keine Livegame-Daten verfügbar.")
        }
        parse(body)
    }

    /** Defensiver JSON-Parser (verzeiht unbekannte Felder). */
    internal fun parse(json: String): LivegameData {
        val root = JSONObject(json)

        fun intWert(obj: JSONObject?, key: String, fallback: Int = 0): Int {
            if (obj == null || !obj.has(key)) return fallback
            val v = obj.opt(key) ?: return fallback
            return when (v) {
                is Number -> v.toInt()
                else -> v.toString().toIntOrNull() ?: fallback
            }
        }

        val game = root.optJSONObject("game")
        val heimObj = game?.optJSONObject("home")
        val gastObj = game?.optJSONObject("away")

        val heimName = heimObj?.optString("name")?.takeIf { it.isNotBlank() && it != "null" }
        val gastName = gastObj?.optString("name")?.takeIf { it.isNotBlank() && it != "null" }

        val heimTore = intWert(game, "homeGoals") or intWert(heimObj, "goals")
        val gastTore = intWert(game, "awayGoals") or intWert(gastObj, "goals")

        val actions = root.optJSONArray("actions")
        val ereignisse = if (actions != null) {
            buildList {
                for (i in 0 until actions.length()) {
                    val a = actions.optJSONObject(i) ?: continue
                    val minute = intWert(a, "minute", -1)
                    val text = a.optString("text").takeIf { it.isNotBlank() && it != "null" }
                        ?: a.optString("event").takeIf { it.isNotBlank() && it != "null" }
                    if (text != null) add(LiveEreignis(minute, text))
                }
            }
        } else emptyList()

        fun taktik(seite: String?): TaktikWerte? {
            val t = root.optJSONObject("tactics")?.optJSONObject(seite) ?: return null
            return TaktikWerte(
                commitment = intWert(t, "commitment"),
                hardness = intWert(t, "hardness"),
                playtype = intWert(t, "playtype"),
                defence = intWert(t, "defence"),
                midfield = intWert(t, "midfield"),
                offence = intWert(t, "offence"),
            )
        }

        fun statistik(seite: String?): StatistikWerte? {
            val s = root.optJSONObject("gamestatistics")?.optJSONObject(seite) ?: return null
            return StatistikWerte(
                goals = intWert(s, "goals"),
                offside = intWert(s, "offside"),
                corners = intWert(s, "corners"),
                fouls = intWert(s, "fouls"),
                penalties = intWert(s, "penalties"),
                possession = intWert(s, "posession"),
            )
        }

        return LivegameData(
            heimName = heimName,
            gastName = gastName,
            heimTore = heimTore,
            gastTore = gastTore,
            zat = intWert(game, "zat", 0).takeIf { it > 0 },
            spieltyp = root.optString("gametype").takeIf { it.isNotBlank() && it != "null" },
            datum = root.optString("date").takeIf { it.isNotBlank() && it != "null" },
            stadion = root.optString("stadium").takeIf { it.isNotBlank() && it != "null" },
            played = game?.optBoolean("played") ?: false,
            ereignisse = ereignisse,
            heimTaktik = taktik("home"),
            gastTaktik = taktik("away"),
            heimStatistik = statistik("home"),
            gastStatistik = statistik("away"),
        )
    }

    private infix fun Int.or(fallback: Int): Int = if (this != 0) this else fallback
}