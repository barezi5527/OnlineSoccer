package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.PmDetail
import com.onlinesoccer.app.data.model.PmNachricht
import com.onlinesoccer.app.data.model.PmAntwortFormular
import com.onlinesoccer.app.data.model.PmEmpfaengerVorschlag
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * PM-Posteingang via `/osneu/pm` (REST-ähnlich, Phase-2 §13).
 *
 * Struktur ist serverseitig nur grob dokumentiert – Parser defensiv gehalten.
 */
@Singleton
class PmRepository @Inject constructor(
    private val client: OkHttpClient,
) {

    suspend fun liste(): List<PmNachricht> = withContext(Dispatchers.IO) {
        val html = get("osneu/pm")
            ?: throw IOException("Nachrichten konnten nicht geladen werden.")
        parseListe(html)
    }

    /** Anzahl ungelesener Nachrichten im Posteingang (für den Briefumschlag-Badge). */
    suspend fun ungeleseneAnzahl(): Int = withContext(Dispatchers.IO) {
        val html = get("osneu/pm") ?: return@withContext 0
        parseListe(html).count { it.ausgehend.not() && it.gelesen.not() }
    }

    suspend fun lesen(pmId: Long): PmDetail = withContext(Dispatchers.IO) {
        val html = get("osneu/pm/read/$pmId")
            ?: throw IOException("Nachricht konnte nicht geladen werden.")
        parseDetail(html, pmId)
    }

    suspend fun antwortFormular(pmId: Long): PmAntwortFormular = withContext(Dispatchers.IO) {
        val html = get("osneu/pm", listOf("action" to "reply", "pn_id" to pmId.toString()))
            ?: throw IOException("Antwortformular konnte nicht geladen werden.")
        parseAntwortFormular(html)
    }

    suspend fun antworten(formular: PmAntwortFormular): Unit = withContext(Dispatchers.IO) {
        val url = OsApi.BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments("osneu/pm")
            .addQueryParameter("action", "writeNew")
            .build()
        val body = FormBody.Builder()
            .add("pn_empfaenger", formular.empfaenger)
            .add("pn_empfaenger_id", formular.empfaengerId)
            .add("pn_betreff", formular.betreff)
            .add("pn_transfer_id", formular.transferId)
            .add("pn_text", formular.text)
            .build()
        val antwort = post(url, body)
            ?: throw IOException("Nachricht konnte nicht gesendet werden.")
        pruefeAktionAntwort(antwort, "Nachricht konnte nicht gesendet werden.")
    }

    suspend fun loeschen(pmId: Long): Unit = withContext(Dispatchers.IO) {
        val antwort = get("osneu/pm/delete/$pmId")
            ?: throw IOException("Nachricht konnte nicht gelöscht werden.")
        pruefeAktionAntwort(antwort, "Nachricht konnte nicht gelöscht werden.")
    }

    /** Empfänger-Autovervollständigung für eine neue Nachricht (`osneu/ajax/findUser`). */
    suspend fun empfaengerSuchen(keyword: String): List<PmEmpfaengerVorschlag> = withContext(Dispatchers.IO) {
        val url = OsApi.BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments("osneu/ajax/findUser")
            .build()
        val body = FormBody.Builder().add("keyword", keyword).build()
        val antwort = post(url, body) ?: return@withContext emptyList()
        parseEmpfaengerVorschlaege(antwort)
    }

    private suspend fun get(path: String, parameter: List<Pair<String, String>> = emptyList()): String? = runCatching {
        val url = OsApi.BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments(path)
            .apply { parameter.forEach { (name, value) -> addQueryParameter(name, value) } }
            .build()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful || body.isBlank()) return@use null
            if (SessionGuard.isPureLoginView(body.toByteArray())) null else body
        }
    }.getOrNull()

    private suspend fun post(url: HttpUrl, body: FormBody): String? = runCatching {
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { response ->
            val antwort = response.body?.string().orEmpty()
            if (!response.isSuccessful || antwort.isBlank()) return@use null
            if (SessionGuard.isPureLoginView(antwort.toByteArray())) null else antwort
        }
    }.getOrNull()

    /** Posteingang/Postausgang: `div.pmrow`-Zeilen mit `input[name=pmid]` (Phase-2, pm.js). */
    internal fun parseListe(html: String): List<PmNachricht> {
        val doc = Jsoup.parse(html)
        val result = LinkedHashMap<Long, PmNachricht>()

        fun rowParse(row: Element, postausgang: Boolean) {
            val id = row.selectFirst("input[type=hidden][name=pmid]")?.attr("value")?.toLongOrNull()
                ?: return
            val zellen = row.select("div.pmcell")
            val betreff = zellen.getOrNull(0)?.text()?.trim()
            val partner = zellen.getOrNull(1)?.text()?.trim()
            val datum = zellen.getOrNull(4)?.text()?.trim()
            val gelesen = !row.classNames().contains("pmunread")
            if (postausgang) {
                result.putIfAbsent(
                    id,
                    PmNachricht(id, sender = null, empfänger = partner, betreff = betreff, datum = datum, gelesen = true, ausgehend = true),
                )
            } else {
                result.putIfAbsent(
                    id,
                    PmNachricht(id, sender = partner, empfänger = null, betreff = betreff, datum = datum, gelesen = gelesen),
                )
            }
        }

        doc.select("#tab_inbox div.pmrow").forEach { rowParse(it, postausgang = false) }
        doc.select("#tab_outbox div.pmrow").forEach { rowParse(it, postausgang = true) }

        return result.values.toList()
    }

    /** Lesen: Nachrichtentext aus `read/<id>` extrahieren (`.pmtext` bzw. größter Textblock). */
    internal fun parseDetail(html: String, pmId: Long): PmDetail {
        val doc = Jsoup.parse(html)

        val body = doc.selectFirst(".pmtext, .nachricht, [class*=message]")?.wholeText()?.trim()
            ?: grossterTextBlock(doc)?.wholeText()?.trim()
            ?: doc.body()?.wholeText()?.trim()

        val betreff = doc.selectFirst(".pmheader, .betreff, [class*=subject]")?.text()?.takeIf { it.isNotBlank() }
            ?: doc.title().takeIf { it.isNotBlank() }

        val sender = doc.selectFirst("[class*=sender], [class*=von]")?.text()?.trim()

        val nachricht = PmNachricht(
            id = pmId,
            sender = sender,
            empfänger = null,
            betreff = betreff,
            datum = null,
            gelesen = true,
        )
        return PmDetail(nachricht = nachricht, body = body?.takeIf { it.isNotBlank() })
    }

    internal fun parseAntwortFormular(html: String): PmAntwortFormular {
        val doc = Jsoup.parse(html)
        fun value(name: String): String = doc.selectFirst("[name=$name]")?.attr("value").orEmpty()
        val text = doc.selectFirst("textarea[name=pn_text]")?.wholeText().orEmpty()
        return PmAntwortFormular(
            empfaenger = value("pn_empfaenger"),
            empfaengerId = value("pn_empfaenger_id"),
            betreff = value("pn_betreff"),
            text = text,
            transferId = value("pn_transfer_id").ifBlank { "0" },
        )
    }

    /** Vorschläge aus `osneu/ajax/findUser` – Server liefert `<li>` mit `input[name=userID]` und `<b>Name</b>` (live verifiziert), Fallback auf `userID|<name>`-Zeilen. */
    internal fun parseEmpfaengerVorschlaege(html: String): List<PmEmpfaengerVorschlag> {
        val doc = Jsoup.parse(html, "UTF-8")
        val ergebnis = LinkedHashMap<Long, String>()
        doc.select("li").forEach { li ->
            val id = li.selectFirst("input[name=userID]")?.attr("value")?.toLongOrNull() ?: return@forEach
            val name = li.selectFirst("b")?.text()?.trim()?.takeIf { it.isNotBlank() } ?: return@forEach
            ergebnis.putIfAbsent(id, name)
        }
        if (ergebnis.isEmpty()) {
            val eintraege = doc.select("div, p").mapNotNull { element ->
                element.text().trim().takeIf { it.isNotBlank() }
            }.ifEmpty {
                html.lineSequence().filter { it.isNotBlank() }.toList()
            }
            eintraege.forEach { zeile ->
                val treffer = Regex("(\\d+)[|](.*)").find(zeile) ?: return@forEach
                val id = treffer.groupValues[1].toLongOrNull() ?: return@forEach
                val name = treffer.groupValues[2].trim().removeSuffix(",").trim()
                if (name.isNotBlank()) ergebnis.putIfAbsent(id, name)
            }
        }
        return ergebnis.map { (id, name) -> PmEmpfaengerVorschlag(id, name) }
    }

    private fun pruefeAktionAntwort(body: String, fehlermeldung: String) {
        if (body.isBlank() || SessionGuard.isPureLoginView(body.toByteArray())) {
            throw IOException(fehlermeldung)
        }
        val doc = Jsoup.parse(body)
        val fehler = doc.select(".error, .errorbox, .errortext, .warning").firstOrNull()?.text()
            ?.takeIf { it.isNotBlank() }
        if (fehler != null) throw IOException(fehler)
    }

    private fun grossterTextBlock(doc: org.jsoup.nodes.Document): Element? {
        return doc.body()?.select("div, p")?.maxByOrNull { it.text().length }
    }
}
