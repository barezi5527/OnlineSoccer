package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.DashboardData
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

@Singleton
class DashboardRepository @Inject constructor(
    private val client: OkHttpClient,
) {

    suspend fun fetchDashboard(): DashboardData = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(OsApi.MAIN).build()
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: throw IOException("Leere Antwort")
                if (!SessionGuard.isPersonalView(bytes) && !SessionGuard.isDemoView(bytes)) {
                    throw IOException("Session abgelaufen – bitte neu anmelden")
                }
                val html = bytes.toString(Charsets.UTF_8)
                parse(html)
            }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Dashboard konnte nicht geladen werden: ${e.message}", e)
        }
    }

    internal fun parse(html: String): DashboardData = Jsoup.parse(html).let { doc ->

        val buero = doc.selectFirst("td:contains(Willkommen im Managerbüro)")
        val bueroSegmente = buero
            ?.html()
            ?.split(Regex("(?i)<br\\s*/?>"))
            ?.map { Jsoup.parseBodyFragment(it).text().trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val parsedTeamName = bueroSegmente
            .firstOrNull { it.startsWith("Willkommen") }
            ?.substringAfter("von ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        // Eigener Teamname: bevorzugt aus der Begrüßungszelle. Das erste
        // javascript:teaminfo(...)-Link auf haupt.php gehört meist zum Gegner.
        val teamName = parsedTeamName
            ?: doc.selectFirst("a[href^=javascript:teaminfo]")?.text()?.trim()
                ?.takeIf { it.isNotBlank() }
        // Liga & Land (z. B. "2. Liga B Deutschland"): Zeile nach der Begrüßung.
        val liga = bueroSegmente.drop(1).firstOrNull { line ->
            line.contains("Liga", ignoreCase = true)
        } ?: bueroSegmente.getOrNull(1)

        val wappen = doc.selectFirst("img[src*=images/wappen]")?.attr("src")
        val teamId = wappen?.let { Regex("(\\d+)\\.").find(it)?.groupValues?.get(1)?.toIntOrNull() }
        val teamLogoUrl = wappen?.let { if (it.startsWith("http")) it else "${OsApi.BASE_URL}/${it.trimStart('/')}" }
        val forumUrl = doc.select("a").firstOrNull {
            val text = it.text().trim()
            text.contains("Länderforum", ignoreCase = true) ||
                text.contains("Laenderforum", ignoreCase = true) ||
                it.attr("href").contains("forum", ignoreCase = true)
        }?.attr("href")?.let { if (it.startsWith("http")) it else "${OsApi.BASE_URL}/${it.trimStart('/')}" }

        val zatText = doc.selectFirst("b:contains(Der nächste ZAT)")?.text()
        val zat = Regex("ZAT\\s+(\\d+)").find(zatText.orEmpty())?.groupValues?.get(1)
        val zatDatum = zatText?.substringAfter("liegt auf ")?.trim()

        fun stat(label: String): String? {
            val statsRow = doc.selectFirst("td:matchesOwn(^Logins\\s*$)")?.parent() ?: return null
            val labels = statsRow.children().map { it.ownText().trim() }
            val valuesRow = statsRow.nextElementSibling() ?: return null
            val idx = labels.indexOf(label)
            if (idx < 0) return null
            val cell = valuesRow.children().getOrNull(idx) ?: return null
            return cell.selectFirst("a")?.text() ?: cell.ownText().trim().ifEmpty { null }
        }

        val logins = stat("Logins")
        val zugabgabeStatus = stat("Zugabgabe")
        val kontostand = stat("Kontostand")
        val pmNeu = stat("PMs")
        val fssEinladungen = stat("FSS-Einladungen")

        fun matchRow(marker: String): DashboardData.MatchInfo? {
            val labelCell = doc.selectFirst("b:contains($marker)") ?: return null
            val cells = labelCell.closest("tr")?.select("td") ?: return null
            val artCell = cells.getOrNull(1)?.ownText()?.trim().orEmpty()
            val third = cells.getOrNull(2) ?: return null
            val gegner = third.selectFirst("a[href*=teaminfo]")?.text()?.trim()
            val gegnerId = third.selectFirst("a[href*=teaminfo]")
                ?.attr("href")
                ?.let { Regex("teaminfo\\((\\d+)\\)").find(it)?.groupValues?.get(1)?.toIntOrNull() }
            val berichtHref = third.selectFirst("a[href*=os_bericht]")?.attr("href")
            val berichtUrl = berichtHref?.let { parseBerichtUrl(it) }
            val gepaartZat = third.selectFirst("a[href*=spielpreview]")
                ?.attr("href")
                ?.let { Regex("spielpreview\\(\\d+,\\d+,(\\d+)\\)").find(it)?.groupValues?.get(1)?.toIntOrNull() }
                ?: berichtHref?.let { parseBerichtZat(it) }
            return DashboardData.MatchInfo(
                art = artCell,
                heim = artCell.contains("Heim"),
                gegner = gegner,
                gegnerId = gegnerId,
                berichtUrl = berichtUrl,
                gepaartZat = gepaartZat,
            )
        }

        val letztesSpiel = matchRow("Dein letztes Spiel")
        val naechstesSpiel = matchRow("Dein nächstes Spiel")

        val rows = listOfNotNull(
            zugabgabeStatus?.let { DashboardData.LabeledValue("Zugabgabe", it) },
            logins?.let { DashboardData.LabeledValue("Logins", it) },
            kontostand?.let { DashboardData.LabeledValue("Kontostand", it) },
            (zat != null || zatDatum != null)?.let {
                DashboardData.LabeledValue("ZAT", listOfNotNull(zat, zatDatum).joinToString("  •  "))
            },
            pmNeu?.let { DashboardData.LabeledValue("Frische PMs", it) },
            fssEinladungen?.let { DashboardData.LabeledValue("FSS-Einladungen", it) },
        )

        DashboardData(
            teamId = teamId,
            teamName = teamName,
            liga = liga,
            logins = logins,
            kontostand = kontostand,
            zugabgabeStatus = zugabgabeStatus,
            pmNeu = pmNeu,
            fssEinladungen = fssEinladungen,
            zat = zat,
            zatDatum = zatDatum,
            letztesSpiel = letztesSpiel,
            naechstesSpiel = naechstesSpiel,
            rows = rows,
            teamLogoUrl = teamLogoUrl,
            forumUrl = forumUrl,
        )
    }

    internal fun parseBerichtUrl(href: String): String? {
        val args = Regex("os_bericht\\((\\d+),(\\d+),(\\d+),(\\d+)\\)")
            .find(href)?.destructured ?: return null
        val (heim, gast, zat, saison) = args
        return "${OsApi.BASE_URL}/rep/saison/$saison/$zat/$heim-$gast.html"
    }

    private fun parseBerichtZat(href: String): Int? =
        Regex("os_bericht\\(\\d+,\\d+,(\\d+),\\d+\\)").find(href)
            ?.groupValues?.get(1)?.toIntOrNull()
}
