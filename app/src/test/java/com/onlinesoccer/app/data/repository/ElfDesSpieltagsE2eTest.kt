package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.ElfErgebnis
import com.onlinesoccer.app.data.model.SpielBericht
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * End-to-End-Test gegen den echten Online-Soccer-Server (Gast-Session, keine
 * Zugangsdaten). Er treibt die Produktions-Repositories an:
 *
 *   ls.php (Spieltag) -> statische Spielberichte -> ElfAuswertung -> ElfAuswahl
 *   -> ElfErgebnis -> ElfCache-Round-Trip.
 *
 * Bei nicht erreichbarem Server wird der Test übersprungen (Assume), nicht rot.
 */
class ElfDesSpieltagsE2eTest {

    private class MerkCookieJar : CookieJar {
        val cookies = mutableMapOf<String, List<Cookie>>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            cookies[url.host] ?: emptyList()
    }

    private class ZaehlenInterceptor(val treffer: MutableList<String>) : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            treffer += chain.request().url.toString()
            return chain.proceed(chain.request())
        }
    }

    private fun baueClient(zaehler: MutableList<String> = mutableListOf()): OkHttpClient =
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(MerkCookieJar())
            .addInterceptor(ZaehlenInterceptor(zaehler))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", OsApi.USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    private fun gastLogin(client: OkHttpClient): Boolean {
        val body = FormBody.Builder()
            .add("action", "os_login")
            .add("loginemail", "")
            .add("passwort", "")
            .add("imageField2.x", "10")
            .add("imageField2.y", "10")
            .build()
        val request = Request.Builder().url(OsApi.LOGIN).post(body).build()
        client.newCall(request).execute().use { response ->
            return response.isSuccessful
        }
    }

    @Test
    fun endToEndGegenRealenServer() = runBlocking {
        val zaehler = mutableListOf<String>()
        val client = baueClient(zaehler)
        assertTrue(gastLogin(client))

        val bewerbeRepository = BewerbeRepository(client)
        val berichtRepository = BerichtRepository(client)

        // Spieltag England (land=1), Liga 1, Saison 24, Spieltag 1 – real abrufbar.
        val geladener = bewerbeRepository.ladeSpieltag(zat = 1, liga = 1, land = 1, saison = 24)
        assumeTrue("Server nicht erreichbar oder Spieltag nicht abrufbar", geladener != null)
        val spieltag = geladener!!

        assertEquals(1, spieltag.liga)
        assertEquals(1, spieltag.land)
        assertEquals(24, spieltag.saison)
        assertEquals(1, spieltag.zat)

        val spiele = spieltag.spiele.filter { it.gespielt && it.berichtUrl != null }
        val zufriedenstellendBerichte = spieltag.spiele.filter { it.gespielt }
        assertTrue("Keine gespielten Begegnungen gefunden", zufriedenstellendBerichte.isNotEmpty())

        // Die reale Stichprobe: England Saison 24 Spieltag 1 hat 10 Begegnungen.
        assertEquals(zufriedenstellendBerichte.size, spiele.size)

        val kandidaten = mutableListOf<ElfKandidat>()
        var erfolgreich = 0
        val berichte = mutableListOf<SpielBericht>()

        spiele.forEach { spiel ->
            val bericht = spiel.berichtUrl?.let { berichtRepository.ladeBericht(sid = null, url = it) }
            assertNotNull("Bericht nicht geladen: ${spiel.heim} - ${spiel.gast}", bericht)
            if (bericht != null) {
                erfolgreich++
                berichte += bericht
                kandidaten += ElfAuswertung.kandidatenAusBericht(bericht)
            }
        }
        assertEquals(spiele.size, erfolgreich)

        // Stichprobe aus dem real getesteten Bericht (Newcastle Glory – FC Moor:
        // Ebulfez Mammadov 3 Tore). Der statische Bericht enthält keinen
        // os_bericht()-Anker, daher Zuordnung über die Vereinsnamen.
        val newcastleBericht = berichte.first { it.heim == "Newcastle Glory" && it.gast == "FC Moor" }
        val mannschaft = ElfAuswertung.kandidatenAusBericht(newcastleBericht)
        val mammadov = mannschaft.firstOrNull { "ebulfez mammadov" == it.name.lowercase() }
        assertNotNull("Torschützenliste fehlt im 1077-1075-Bericht", mammadov)
        assertEquals(3, mammadov!!.tore)
        // Die Spielerstatistik-Tabelle liefert Zweikämpfe & Schüsse mit echten Werten.
        assertTrue("Spielerstatistik fehlt (Mammadov)", mammadov.schuesse > 0)

        val aufstellung = ElfAuswahl.erstelleElf(kandidaten)
        val elf = aufstellung.spieler
        val elfVollstaendig = aufstellung.vollstaendig
        val formation = aufstellung.formation
        val ergebnis = ElfErgebnis(
            land = spieltag.landOptionen.firstOrNull { it.wert == spieltag.land }?.label ?: "England",
            liga = spieltag.ligaOptionen.firstOrNull { it.wert == spieltag.liga }?.label ?: "Liga",
            saison = spieltag.saison,
            spieltag = spieltag.zat,
            spieler = elf,
            formation = formation,
            begegnungen = spiele.size,
            berichteErfolgreich = erfolgreich,
            vollstaendig = spiele.isNotEmpty() && erfolgreich == spiele.size && elfVollstaendig,
        )

        assertEquals(11, ergebnis.spieler.size)
        assertEquals(1, ergebnis.spieler.count { it.position == com.onlinesoccer.app.data.model.SpielerPosition.TOR })
        // Die Elf entspricht exakt einer erlaubten, dynamisch gewählten Formation.
        assertNotNull("Keine Formation gewählt", formation)
        val gewaehlteFormation = ElfAuswahl.FORMATIONEN.firstOrNull { it.label == formation }
        assertNotNull("Formation '$formation' nicht erlaubt", gewaehlteFormation)
        val f = gewaehlteFormation!!
        assertEquals(f.abwehr, ergebnis.spieler.count { it.position == com.onlinesoccer.app.data.model.SpielerPosition.ABW })
        // Das Mittelfeld wird auf die Formationslinien OMI/MIT/DMI verteilt.
        assertEquals(
            f.mittelfeld,
            ergebnis.spieler.count {
                it.position == com.onlinesoccer.app.data.model.SpielerPosition.OMI ||
                    it.position == com.onlinesoccer.app.data.model.SpielerPosition.MIT ||
                    it.position == com.onlinesoccer.app.data.model.SpielerPosition.DMI
            },
        )
        assertEquals(f.sturm, ergebnis.spieler.count { it.position == com.onlinesoccer.app.data.model.SpielerPosition.STU })

        // Transparenz: die gecachten Bewertungen sind exakt aus den Kandidaten berechenbar.
        ergebnis.spieler.forEach { spieler ->
            val neu = ElfBewertung.bewerten(ElfBewertung.kandidatVon(spieler)).gesamt
            assertEquals(spieler.bewertung, neu, 0.001)
        }

        // Vereinswappen: Für jeden Elf-Spieler der realen Liga muss eine Team-ID
        // (vom Bericht oder aus der Berichts-URL) vorliegen – sonst zeigt die UI
        // nur den Wappen-Platzhalter.
        ergebnis.spieler.forEach { spieler ->
            assertNotNull("Keine Team-ID für ${spieler.name} (${spieler.verein})", spieler.teamId)
        }

        // Cache-Round-Trip (v7, 26 Felder): Deserialisierung == Original.
        val wiederhergestellt = ElfCache.deserialisieren(ElfCache.serialisieren(ergebnis))
        assertNotNull("Cache-Round-Trip fehlgeschlagen", wiederhergestellt)
        assertEquals(ergebnis, wiederhergestellt)
        assertTrue("Cache-Prefix v7 fehlt", ElfCache.serialisieren(ergebnis).startsWith("ElfDesSpieltags|v7"))

        // ---- Dokumentations-Report ----
        println("=".repeat(78))
        println("ELF DES SPIELTAGS – E2E gegen https://os.ongapo.com")
        println("=".repeat(78))
        println("Liga   : ${ergebnis.liga} (${ergebnis.land})")
        println("Saison : ${ergebnis.saison}   Spieltag: ${ergebnis.spieltag}")
        println("Begegnungen: ${ergebnis.begegnungen}   Berichte erfolgreich: ${ergebnis.berichteErfolgreich}   Vollständig: ${ergebnis.vollstaendig}")
        println("Formation : ${ergebnis.formation ?: "– (Fallback)"}")
        println()
        println("Spieltabelle:")
        spiele.zip(berichte).forEach { (spiel, bericht) ->
            val max = ElfAuswertung.kandidatenAusBericht(bericht)
            val top = max.maxByOrNull { ElfBewertung.bewerten(it).gesamt }
            val maxNote = top?.let { "${ElfBewertung.formatiere(ElfBewertung.bewerten(it).gesamt)} (${it.name})" } ?: "-"
            println("  ${spiel.heim} ${spiel.toreHeim}:${spiel.toreGast} ${spiel.gast}  | ${max.size} Kandidaten | Bestnote $maxNote")
        }
        println()

        println("Spieler gesamt: ${kandidaten.size}   → Elf:")
        ergebnis.spieler
            .sortedByDescending { it.bewertung }
            .forEachIndexed { i, spieler ->
                val bewertung = ElfBewertung.bewerten(ElfBewertung.kandidatVon(spieler))
                val zeilen = bewertung.zeilen.joinToString("; ") { "${it.kriterium} ${ElfBewertung.formatiereBeitrag(it.beitrag)}" }
                println(
                    "  ${i + 1}. ${spieler.name} (${spieler.verein}, ${spieler.position}, " +
                        "${if (spieler.startelf) "Startelf" else "Bank"}) " +
                        "- Note ${ElfBewertung.formatiere(spieler.bewertung)} " +
                        "[T ${spieler.tore} | V ${spieler.vorlagen} | Gelb ${spieler.gelbeKarten} | Rot ${spieler.roteKarten} " +
                        "| Min ${spieler.minuten ?: "-"}]"
                )
                println("      → $zeilen")
            }
        println()
        val zehnBewertungen = ergebnis.spieler.count { it.bewertung == 10.0 }
        println("10,0-Bewertungen: $zehnBewertungen von ${ergebnis.spieler.size} Elf-Spielern (keine Normalisierung)")
        ergebnis.spielerDesSpieltags?.let { bester ->
            println("Spieler des Spieltags: ${bester.name} (${bester.verein}) – ${ElfBewertung.formatiere(bester.bewertung)}")
        }
        println("HTTP-Aufrufe gesamt (Login + ls.php + 10 Berichte): ${zaehler.size}")
        println("=".repeat(78))
    }
}