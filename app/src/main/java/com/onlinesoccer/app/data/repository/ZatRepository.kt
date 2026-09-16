package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.HtmlTools
import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.LigaSpiel
import com.onlinesoccer.app.data.model.ZuzuDaten
import com.onlinesoccer.app.data.model.ZuzuPreise
import com.onlinesoccer.app.data.model.ZuzuSpieler
import com.onlinesoccer.app.data.model.ZatErgebnisse
import com.onlinesoccer.app.data.model.ZatLiga
import com.onlinesoccer.app.data.model.ZatReport
import com.onlinesoccer.app.data.model.ZatReportEinnahme
import com.onlinesoccer.app.data.model.ZatReportTraining
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/** ZAT-Bereich: ZAT-Ergebnisse (`zer.php`) und Zugabgabe-Zusatz (`zuzu.php`). */
@Singleton
class ZatRepository @Inject constructor(
    private val client: OkHttpClient,
) {

    suspend fun ladeZatErgebnisse(): ZatErgebnisse? = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/zer.php?erganzeigen=1") ?: return@withContext null
        parseZatErgebnisse(html)
    }

    internal fun parseZatErgebnisse(html: String): ZatErgebnisse {
        val doc = Jsoup.parse(html)
        val zat = HtmlTools.ueberschriften(doc).firstNotNullOfOrNull { it.toIntOrNull() }
        val ligen = mutableListOf<ZatLiga>()
        doc.select("table").forEach { tabelle ->
            val ligaName = ligaName(tabelle) ?: return@forEach
            val rows = tabelle.select("tr").mapNotNull { tr ->
                val zellen = tr.select("td").map { it.text().trim() }.filter { it.isNotEmpty() }
                if (zellen.size < 2) return@mapNotNull null
                // erste zwei Zellen = Heim/Gast, Rest = Ergebnis
                val heim = zellen[0]; val gast = zellen[1]
                val ergebnis = zellen.drop(2).firstOrNull { it.contains(":") || it.contains("-") }
                val (tH, tG) = ergebnis?.let { split(it) } ?: (null to null)
                val sid = tr.selectFirst("td a[href*='bericht.php']")?.attr("href")
                    ?.let { Regex("[?&]s=([\\d,]+)").find(it)?.groupValues?.get(1) }
                LigaSpiel(
                    zat = zat ?: 0,
                    heim = heim,
                    gast = gast,
                    toreHeim = tH,
                    toreGast = tG,
                    gespielt = tH != null,
                    berichtSid = sid,
                )
            }
            if (rows.isNotEmpty()) ligen += ZatLiga(name = ligaName, spiele = rows.distinctBy { it.heim to it.gast })
        }
        return ZatErgebnisse(zat = zat, zusammengefasst = ligen)
    }

    /**
     * Ladet den persönlichen ZAT-Report (`zar.php`) für einen ZAT.
     * Ohne ZAT/Saison holt die Seite ihren Standardwert (aktueller ZAT).
     */
    suspend fun ladeZatReport(zat: Int?, saison: Int?): ZatReport? = withContext(Dispatchers.IO) {
        val basis = Request.Builder()
            .url("${OsApi.BASE_URL}/zar.php")
        val request = if (zat != null || saison != null) {
            val form = FormBody.Builder()
                .add("zat", zat?.toString().orEmpty())
                .add("saison", saison?.toString().orEmpty())
                .add("ansehen", "ansehen")
                .build()
            basis.post(form).build()
        } else {
            basis.build()
        }
        val html = try {
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: return@withContext null
                if (SessionGuard.isPureLoginView(bytes)) return@withContext null
                bytes.toString(Charsets.UTF_8)
            }
        } catch (e: IOException) {
            null
        }
        if (html == null) return@withContext null
        parseZatReport(html)
    }

    internal fun parseZatReport(html: String): ZatReport {
        val doc = Jsoup.parse(html)

        fun selectedWert(name: String): Int? =
            doc.selectFirst("select[name=$name] option[selected]")?.attr("value")?.toIntOrNull()

        val einnahmen = mutableListOf<ZatReportEinnahme>()
        val trainingserfolge = mutableListOf<ZatReportTraining>()

        doc.select("h3").forEach { h3 ->
            when {
                h3.text().startsWith("1.") || h3.text().contains("Einnahmen") -> {
                    h3.nextElementSibling()?.takeIf { it.tagName() == "table" }?.select("tr")?.forEach { tr ->
                        val zellen = tr.select("td")
                        if (zellen.size >= 2) {
                            val label = zellen[0].text().trim()
                            val wert = zellen[1].text().trim()
                            if (label.isNotEmpty() && wert.isNotEmpty()) {
                                einnahmen += ZatReportEinnahme(label = label, wert = wert)
                            }
                        }
                    }
                }
                h3.text().startsWith("2.") || h3.text().contains("Trainingserfolge") -> {
                    h3.nextElementSibling()?.takeIf { it.tagName() == "table" }?.select("tr")?.forEach { tr ->
                        val zellen = tr.select("td")
                        val nameZelle = zellen.firstOrNull() ?: return@forEach
                        val name = nameZelle.selectFirst("a")?.text()?.trim()
                            ?: nameZelle.ownText().trim()
                        if (name.isEmpty()) return@forEach
                        val link = nameZelle.selectFirst("a[href*='spielerinfo']")
                        val pid = link?.attr("href")
                            ?.let { Regex("(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() }
                        val position = nameZelle.selectFirst(
                            "[class=TOR],[class=ABW],[class=DMI],[class=MIT],[class=OMI],[class=STU]",
                        )?.attr("class")
                        trainingserfolge += ZatReportTraining(
                            name = name,
                            pid = pid,
                            position = position,
                            beschreibung = zellen.getOrNull(1)?.text()?.trim().orEmpty(),
                            wert = zellen.getOrNull(2)?.text()?.trim()?.takeIf { it.startsWith("(") && it.endsWith(")") },
                        )
                    }
                }
            }
        }

        return ZatReport(
            zat = selectedWert("zat"),
            saison = selectedWert("saison"),
            einnahmen = einnahmen,
            trainingserfolge = trainingserfolge,
        )
    }

    /** Zugabgabe-Zusatz (`zuzu.php`): Eintrittspreise + Physio-Liste (lesend). */
    suspend fun ladeZuzu(): ZuzuDaten? = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/zuzu.php") ?: return@withContext null
        runCatching { parseZuzu(html) }.getOrNull()
    }

    internal fun parseZuzu(html: String): ZuzuDaten {
        val doc = Jsoup.parse(html)

        fun inputWert(name: String): String? =
            doc.selectFirst("input[name=$name]")?.attr("value")?.takeIf { it.isNotBlank() }

        val preise = ZuzuPreise(
            liga = inputWert("liga"),
            pokal = inputWert("pokal"),
            international = inputWert("int"),
        )

        // Physio-Liste: Das Formular mit dem „Zum Physio schicken“-Button (`pspeichern`)
        // enthält die Spieler-Tabelle (# | Spieler | FIT | Physio | Kosten).
        val physioForm = doc.select("form").firstOrNull { it.selectFirst("input[name=pspeichern]") != null }
        val spieler = physioForm?.select("table tr")?.mapNotNull { tr ->
            val zellen = tr.select("td")
            if (zellen.size < 5) return@mapNotNull null
            val nummer = zellen[0].text().trim().toIntOrNull() ?: return@mapNotNull null
            val link = zellen[1].selectFirst("a[href*='spielerinfo']")
            val pid = link?.attr("href")
                ?.let { Regex("(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() }
                ?: return@mapNotNull null
            val name = link?.text()?.trim() ?: zellen[1].text().trim()
            val fit = zellen[2].text().trim().toIntOrNull() ?: 0
            ZuzuSpieler(
                nummer = nummer,
                pid = pid,
                name = name,
                fit = fit,
                kosten = zellen[4].text().trim(),
            )
        }.orEmpty()

        return ZuzuDaten(preise = preise, spieler = spieler)
    }

    /** Speichert die Eintrittspreise (Heimspiele) von `zuzu.php`. Reale Schreibaktion. */
    suspend fun zuzuPreiseSpeichern(preise: ZuzuPreise): ZuzuDaten = withContext(Dispatchers.IO) {
        val form = FormBody.Builder()
            .add("liga", preise.liga.orEmpty())
            .add("pokal", preise.pokal.orEmpty())
            .add("int", preise.international.orEmpty())
            .add("speichern", "Speichern")
            .build()
        val html = postZuzu(form)
        parseZuzu(html)
    }

    /** Schickt markierte Spieler zum Physio (`zuzu.php`). Reale Schreibaktion. */
    suspend fun zuzuPhysioSchicken(pids: List<Long>): ZuzuDaten = withContext(Dispatchers.IO) {
        val form = FormBody.Builder().apply {
            pids.distinct().forEach { pid -> add("physio[$pid]", "on") }
            add("pspeichern", "Zum Physio schicken")
        }.build()
        val html = postZuzu(form)
        parseZuzu(html)
    }

    private fun postZuzu(form: FormBody): String {
        val request = Request.Builder()
            .url("${OsApi.BASE_URL}/zuzu.php")
            .post(form)
            .build()
        val html = try {
            client.newCall(request).execute().use { it.body?.string().orEmpty() }
        } catch (e: IOException) {
            throw IOException("Netzwerkfehler beim Speichern der Zugabgabe-Zusätze.", e)
        }
        if (SessionGuard.isPureLoginView(html.toByteArray())) {
            throw IOException("Sitzung abgelaufen - bitte neu anmelden.")
        }
        val fehlertext = Jsoup.parse(html).select(".error, .errorbox, .errortext")
            .firstOrNull()?.text()?.takeIf { it.isNotBlank() }
        fehlertext?.let { throw IOException(it) }
        return html
    }

    private fun ligaName(tabelle: Element): String? {
        var prev = tabelle.previousElementSibling()
        var depth = 0
        while (prev != null && depth < 4) {
            val text = prev.ownText().trim()
            if (text.isNotEmpty() && text.length in 3..60) return text
            prev = prev.previousElementSibling()
            depth++
        }
        return null
    }

    private fun split(text: String): Pair<Int?, Int?> {
        val norm = text.replace("–", "-").replace(" ", "").replace(".", ":")
        val match = Regex("(\\d+)[:\\-](\\d+)").find(norm) ?: return null to null
        return match.groupValues[1].toIntOrNull() to match.groupValues[2].toIntOrNull()
    }

    private fun safeGet(url: String): String? {
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: return null
                if (SessionGuard.isPureLoginView(bytes)) return null
                bytes.toString(Charsets.UTF_8)
            }
        } catch (e: IOException) {
            null
        }
    }
}