package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.HtmlTools
import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.core.network.ToggleClient
import com.onlinesoccer.app.data.model.TeamwechselErgebnis
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * Teamwechsel zwischen Haupt- und Zweitteam (`haupt.php?changetosecond=true`).
 *
 * Der Endpunkt ist ein reiner `isset()`-Toggle in der PHP-Session: **jeder**
 * Abruf schaltet um, es gibt kein Ziel und kein `false`. Daraus folgen drei
 * Regeln, die dieser Baustein bewusst einhält:
 *
 * 1. **Genau ein Schreibvorgang pro Nutzergeste.** Kein Retry, kein
 *    Nachfassen, kein zweiter Versuch — OkHttps Retry-Verhalten ist auf dem
 *    injizierten Client abgeschaltet ([ToggleClient]).
 * 2. **Single-Flight statt `Mutex`.** Ein `Mutex` serialisiert nur: ein
 *    Doppelklick ließe den zweiten Request warten, und der schaltet
 *    **zurück**. Stattdessen bricht der Guard den zweiten Aufruf sofort ab.
 * 3. **Der Erfolg wird nicht behauptet, sondern geprüft.** Diese Klasse
 *    liefert nur das rohe HTML weiter; die Identität des neuen Teams liest
 *    [aktivesTeamAusHtml] daraus.
 */
@Singleton
class TeamwechselRepository @Inject constructor(
    @ToggleClient private val client: OkHttpClient,
) {

    private val laeuft = AtomicBoolean(false)

    /**
     * Führt den Toggle genau einmal aus und reicht die Antwort durch.
     *
     * `null` = Fehler, Abbruch oder bereits laufender Toggle — es wurde dann
     * **nichts** bestätigt. Der HTML-Text wird bewusst nicht hier geparst: die
     * Identität des neuen Teams liest der Aufrufer mit [aktivesTeamAusHtml] und
     * bewertet sie über [werteAus]. Ein Fehler wird nie durch einen zweiten
     * Versuch kompensiert: genau das wäre ein Rück-Toggle.
     */
    suspend fun teamwechselDurchfuehren(): String? {
        if (!laeuft.compareAndSet(false, true)) return null
        return try {
            holeToggleHtml()
        } finally {
            laeuft.set(false)
        }
    }

    /**
     * Holt die Antwort von `haupt.php` **nach** dem Toggle. Das ist zugleich
     * die einzige Verifikationsquelle für den neuen Teamzustand.
     *
     * Der Aufrufer bestimmt den Zeitpunkt; [teamwechselDurchfuehren] ruft ihn
     * unmittelbar auf.
     */
    suspend fun holeToggleHtml(): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(OsApi.TEAMWECHSEL).build()
        try {
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: return@withContext null
                // Abgelaufene Session ⇒ keine verwertbare Antwort. Ohne diese
                // Prüfung würde die Login-Ansicht als „Wechsel ok" gelesen.
                if (!SessionGuard.isPersonalView(bytes)) return@withContext null
                HtmlTools.serverText(bytes)
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Liest Identität (Team-ID + Name) des **jetzt aktiven** Teams aus einer
     * `haupt.php`-Antwort. Reine Funktion, kein Netz — dadurch direkt testbar.
     *
     * **Bewusst nicht** `DashboardData.teamId` als Quelle: der 30-Sekunden-Cache
     * macht den Vorzustand systematisch falsch, und der Button soll den Server-
     * Befund zeigen, nicht einen zwischengespeicherten Wert.
     *
     * @return `teamId` bis `null` und Name bis `null` (unbekannt, z. B. ohne Wappen).
     */
    internal fun aktivesTeamAusHtml(html: String): Pair<Long?, String?> {
        val doc = Jsoup.parse(html)
        // Begrüßungszelle: "Willkommen im Managerbüro von <Teamname>".
        val buero = doc.selectFirst("td:contains(Willkommen im Managerbüro)") ?: return null to null
        val name = buero.selectFirst("b")?.text()
            ?.substringAfter("von ", "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        // Team-ID aus dem Wappen. Die Begrüßungszelle enthält selbst kein
        // Wappen, deshalb wird die **umgebende Tabellenzeile** herangezogen
        // (live verifiziert: links + rechts steht je das eigene Wappen) — nicht
        // das erste Wappen im ganzen Dokument, das könnte ein Fremdverein sein.
        val teamId = buero.closest("tr")
            ?.selectFirst("img[src*=images/wappen]")
            ?.attr("src")
            ?.let { Regex("""(\d+)\.""").find(it)?.groupValues?.get(1)?.toLongOrNull() }
        return teamId to name
    }

    /**
     * Wertet Vorher/Nachher aus. Rein, `internal`, **ohne Netz** — dadurch in
     * T11 komplett testbar.
     *
     * @param vorher Identität vor dem Toggle, @param nachher danach (je `Long?` zu `Name?`).
     */
    internal fun werteAus(
        vorher: Pair<Long?, String?>?,
        nachher: Pair<Long?, String?>?,
    ): TeamwechselErgebnis = when {
        // Kein verwertbarer Vorzustand (z. B. Cache verjagt): der Serverbefund
        // allein trägt die Meldung.
        vorher == null || nachher == null ->
            TeamwechselErgebnis.Fehler("Teamwechsel nicht bestätigt – bitte erneut versuchen.")
        // ID-Vergleich ist der belastbare Nachweis.
        vorher.first != null && nachher.first != null ->
            if (vorher.first != nachher.first) {
                TeamwechselErgebnis.Erfolgreich(nachher.first, nachher.second)
            } else {
                TeamwechselErgebnis.Unveraendert
            }
        // Ohne IDs bleibt der Vergleich der Namen. Gleicher oder unbekannter
        // Name ⇒ kein Beleg für einen Wechsel, also **kein** Erfolgsreport.
        vorher.second != null && nachher.second != null ->
            if (vorher.second != nachher.second) {
                TeamwechselErgebnis.Erfolgreich(null, nachher.second)
            } else {
                TeamwechselErgebnis.Unveraendert
            }
        else -> TeamwechselErgebnis.Fehler("Teamwechsel nicht bestätigt – bitte erneut versuchen.")
    }
}