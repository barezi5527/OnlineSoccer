package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.InternationaleAnsicht
import com.onlinesoccer.app.data.model.InternationaleFilter
import com.onlinesoccer.app.data.model.InternationaleOption
import com.onlinesoccer.app.data.model.InternationaleSpiel
import com.onlinesoccer.app.core.network.HtmlTools
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

@Singleton
class InternationaleRepository @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun lade(path: String, filter: InternationaleFilter = InternationaleFilter()): InternationaleAnsicht =
        withContext(Dispatchers.IO) {
            val url = "${OsApi.BASE_URL}$path"
            val ersteAntwort = safeGet(url)
                ?: throw IOException("Internationale Bewerbe konnten nicht geladen werden.")
            val ersteAnsicht = parse(ersteAntwort)
            if (!filter.istLeer()) {
                return@withContext safePost(url, filter)?.let(::parse) ?: ersteAnsicht
            }
            if (!ersteAnsicht.hatFilter) return@withContext ersteAnsicht

            // Die Wettbewerbsseiten liefern die Spiele erst nach dem Formular-POST.
            val standardFilter = ersteAnsicht.filter.copy(
                ergebnisse = ersteAnsicht.filter.ergebnisse
                    ?.takeUnless { it == "0" }
                    ?: ersteAnsicht.ergebnisOptionen.firstOrNull { it.wert == "1" }?.wert,
            )
            safePost(url, standardFilter)?.let(::parse) ?: ersteAnsicht
        }

    internal fun parse(html: String): InternationaleAnsicht {
        val doc = Jsoup.parse(html)
        val filter = InternationaleFilter(
            saison = selectedValue(doc, "season"),
            runde = selectedValue(doc, "runde"),
            gruppe = selectedValue(doc, "gruppe"),
            ergebnisse = selectedValue(doc, "ergebnisse"),
        )
        val spiele = doc.select("#international a[href*='st.php']")
            .mapNotNull { it.parent()?.parent() }
            .mapNotNull { parent ->
                val teams = parent.select("a[href*='st.php']")
                if (teams.size < 2) return@mapNotNull null
                val berichtLink = parent.selectFirst(
                    "a[href*='rep/saison'], a[href*='os_bericht'], a[onclick*='os_bericht']",
                )
                val bericht = berichtLink?.let { link ->
                    val href = link.attr("href")
                    when {
                        href.contains("os_bericht") -> osBerichtUrl(href)
                        href.contains("rep/saison") -> absoluteUrl(href)
                        else -> osBerichtUrl(link.attr("onclick"))
                    }
                }
                val ergebnis = parent.select("span").map { it.text().trim() }
                    .firstOrNull { Regex("\\d+\\s*:\\s*\\d+").containsMatchIn(it) }
                InternationaleSpiel(teams[0].text().trim(), teams[1].text().trim(), ergebnis, bericht)
            }
            .distinctBy { it.heim to it.gast }

        val tabellen = doc.selectFirst("table.osranking")?.let(::parseOsRanking)
            ?.let(::listOf)
            ?: HtmlTools.tabellen(doc).map { tabelle ->
                com.onlinesoccer.app.data.model.WertTabelle(
                    header = tabelle.header,
                    zeilen = tabelle.zeilen.map { it.zellen },
                )
            }

        return InternationaleAnsicht(
            titel = doc.select("h1,h2,h3").firstOrNull()?.text()?.trim(),
            saisonen = options(doc, "season"),
            runden = options(doc, "runde"),
            gruppen = options(doc, "gruppe"),
            ergebnisOptionen = options(doc, "ergebnisse"),
            filter = filter,
            abschnitte = doc.select("p, li").map { it.text().trim() }
                .filter { it.isNotEmpty() && it.length < 300 && it != "Konferenz ansehen" }
                .distinct()
                .take(120),
            tabellen = tabellen,
            spiele = spiele,
        )
    }

    private fun parseOsRanking(table: org.jsoup.nodes.Element): com.onlinesoccer.app.data.model.WertTabelle {
        val rows = table.select("tr")
        val istClubRanking = rows.drop(1).any { it.select("a[href*='st.php?c=']").isNotEmpty() }
        val header = if (istClubRanking) {
            listOf("Platz", "Summe", "Land", "Club", "Saison 23", "Saison 22", "Saison 21")
        } else {
            listOf("Platz", "Summe", "Land", "Saison 23", "Saison 22", "Saison 21")
        }
        val zeilen = rows.drop(1).map { row ->
            val zellen = row.select("td").map { it.text().trim() }
            if (istClubRanking) {
                listOf(
                    zellen.getOrElse(0) { "" },
                    summe3(zellen.getOrElse(9) { "" }),
                    flagCode(row),
                    zellen.getOrElse(2) { "" },
                    zellen.getOrElse(3) { "" },
                    zellen.getOrElse(5) { "" },
                    zellen.getOrElse(7) { "" },
                )
            } else {
                listOf(
                    zellen.getOrElse(0) { "" },
                    zellen.getOrElse(6) { "" },
                    zellen.getOrElse(2) { "" },
                    zellen.getOrElse(3) { "" },
                    zellen.getOrElse(4) { "" },
                    zellen.getOrElse(5) { "" },
                )
            }
        }
        return com.onlinesoccer.app.data.model.WertTabelle(
            header = header,
            zeilen = zeilen,
        )
    }

    /** Club-Summe auf 3 Nachkommastellen runden (Server liefert 7+ Stellen). */
    private fun summe3(text: String): String {
        val wert = text.replace(",", ".").toDoubleOrNull() ?: return text
        return String.format(java.util.Locale.US, "%.3f", wert)
    }

    private fun flagCode(row: org.jsoup.nodes.Element): String =
        row.selectFirst("td img[src*='flaggen/']")?.attr("src")
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.')
            ?.uppercase()
            .orEmpty()

    private fun options(doc: org.jsoup.nodes.Document, name: String): List<InternationaleOption> =
        doc.select("select[name=$name] option").map {
            InternationaleOption(it.attr("value"), it.text().trim())
        }

    private fun selectedValue(doc: org.jsoup.nodes.Document, name: String): String? =
        doc.select("select[name=$name] option[selected]").firstOrNull()?.attr("value")

    private fun safeGet(url: String): String? = request(Request.Builder().url(url).get().build())

    private fun safePost(url: String, filter: InternationaleFilter): String? {
        val form = FormBody.Builder()
        filter.saison?.let { form.add("season", it) }
        filter.runde?.let { form.add("runde", it) }
        filter.gruppe?.let { form.add("gruppe", it) }
        filter.ergebnisse?.let { form.add("ergebnisse", it) }
        return request(Request.Builder().url(url).post(form.build()).build())
    }

    private fun request(request: Request): String? = try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            if (SessionGuard.isPureLoginView(bytes)) return null
            bytes.toString(Charsets.UTF_8)
        }
    } catch (_: IOException) {
        null
    }

    private fun absoluteUrl(href: String): String = when {
        href.startsWith("http") -> href
        href.startsWith("/") -> "${OsApi.BASE_URL}$href"
        else -> "${OsApi.BASE_URL}/$href"
    }

    private fun osBerichtUrl(onClick: String): String? {
        val match = Regex("os_bericht\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)")
            .find(onClick) ?: return null
        val (heim, gast, zat, saison) = match.destructured
        return "${OsApi.BASE_URL}/rep/saison/$saison/$zat/$heim-$gast.html"
    }

    private fun InternationaleFilter.istLeer(): Boolean =
        saison == null && runde == null && gruppe == null && ergebnisse == null

    private val InternationaleAnsicht.hatFilter: Boolean
        get() = saisonen.isNotEmpty() || runden.isNotEmpty() || gruppen.isNotEmpty() || ergebnisOptionen.isNotEmpty()
}
