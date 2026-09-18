package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.HtmlTools
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.Aufstellung
import com.onlinesoccer.app.data.model.AufstellungSerialisierung
import com.onlinesoccer.app.data.model.AufstellungSlot
import com.onlinesoccer.app.data.model.AufstellungSpieler
import com.onlinesoccer.app.data.model.AufstellungsWerte
import com.onlinesoccer.app.data.model.AuswahlOption
import com.onlinesoccer.app.data.model.CheckzaEintrag
import com.onlinesoccer.app.data.model.CheckzaErgebnis
import com.onlinesoccer.app.data.model.ERSATZBANK_BUCHSTABEN
import com.onlinesoccer.app.data.model.ERSATZTORWART_BANK_INDEX
import com.onlinesoccer.app.data.model.RasterPosition
import com.onlinesoccer.app.data.model.SpielerPosition
import com.onlinesoccer.app.data.model.ZugabgabeElementEintrag
import com.onlinesoccer.app.data.model.ZugabgabeElementTyp
import com.onlinesoccer.app.data.model.ZugabgabeElementeSeite
import com.onlinesoccer.app.data.model.ZugabgabeFormular
import com.onlinesoccer.app.data.model.ZugabgabeKopfinfo
import java.net.URLEncoder
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * Zugabgabe-Datenquelle (Lesen + spaeter Speichern).
 *
 * WICHTIG (Spieltags-Schutz): Waehrend der Entwicklung erfolgen hier ausschliesslich
 * LESENDE GET-Aufrufe. Das Abgeben (POST zugabgabe_beta.php beziehungsweise
 * GET aufspeichern klassisch) wird erst im End-to-End-Test aktiviert und nur dann,
 * wenn der Nutzer kein laufendes Zugabgabe-Fenster mehr hat.
 */
@Singleton
class ZugabgabeRepository @Inject constructor(
    private val client: OkHttpClient,
) {

    /** Liest beide Zugabgabe-Seiten (parallel) und liefert die geparste Aufstellung. */
    suspend fun ladeAufstellung(): Aufstellung = withContext(Dispatchers.IO) {
        val (klassischHtml, betaHtml) = coroutineScope {
            val klassischDeferred = async { safeGet("${OsApi.BASE_URL}/zugabgabe.php") }
            val betaDeferred = async { safeGet("${OsApi.BASE_URL}/zugabgabe_beta.php") }
            klassischDeferred.await() to betaDeferred.await()
        }

        if (klassischHtml == null && betaHtml == null) {
            throw IOException("Aktuelle ZAT-Daten konnten nicht abgerufen werden.")
        }

        val klassisch = klassischHtml?.let { runCatching { parseKlassisch(it) }.getOrNull() }

        val raster = klassischHtml?.let { runCatching { parseRasterLetters(it) }.getOrNull() } ?: emptyMap()
        val beta = betaHtml?.let { runCatching { parseBeta(it) }.getOrNull() }

        mergeBetaMitKlassisch(beta, klassisch, raster)
    }

    /**
     * Erzwingt die "spielberechtigte Website-Liste" als einzige Auswahlquelle.
     *
     * Die Website filtert die spielberechtigten Spieler in der KLASSISCHEN
     * `ra[]`-Liste von `zugabgabe.php` selbst (gesperrte/verletzte Spieler
     * werden dort nicht aufgelistet). Die Beta-Seite (`zugabgabe_beta.php`)
     * zeigt dagegen den kompletten Kader inklusive NICHT spielberechtigter
     * Spieler und ist daher keine verlaessliche Auswahlquelle.
     *
     * - Basis der Auswahl ist immer die klassische `ra[]`-Liste.
     * - Die Beta-Liste liefert nur Detaildaten (Nummer, Skill, Fit, Mor,
     *   Position, Raster-Slots) ZUSAETZLICH fuer die berechtigten Spieler.
     * - Ein Spieler, der nur in der Beta-Liste steht (z.B. Rotsperre), wird
     *   NICHT auswaehlbar.
     * - Fehlt die klassische Liste ganz (Parse-Fehler), sind aus
     *   Sicherheitsgruenden keine Spieler waehlbar (keine verlaessliche
     *   Berechtigungsquelle).
     */
    internal fun mergeBetaMitKlassisch(
        beta: Aufstellung?,
        klassisch: Aufstellung?,
        raster: Map<String, RasterZelle>,
    ): Aufstellung {
        val feldBuchstaben = "ABCDEFGHIJKL"
        val bankBuchstaben = "UVWXYZ"
        val basisKopfdaten = klassisch ?: beta
        val betaSpieler = beta?.spieler?.associateBy { it.pid }.orEmpty()

        val spieler = if (klassisch != null) {
            // Die klassische ra[]-Liste bestimmt, wer spielberechtigt ist.
            klassisch.spieler.map { k ->
                val b = betaSpieler[k.pid]
                val ra = k.raSlot
                val pos = if (ra != null && ra.length == 1 && ra[0] in feldBuchstaben) raster[ra] else null
                val slot = when {
                    // Der Torwart steht auf der Website fest in der T-Zelle (Sonderzeile
                    // unterhalb des Feldrasters = Spielfeld-Position A6). Diese Sonderstellung
                    // ist verbindlich und darf nie durch einen Beta-Slot ueberschrieben werden.
                    ra == "T" -> AufstellungSlot.Torwart
                    // Ersatzbank U-Z: die klassische ra[]-Zuordnung ist verbindlich und liefert
                    // einen Ersatz-Slot auch dann, wenn die Beta-Seite keine Mover liefert
                    // (sonst bliebe die Ersatzbank unsichtbar).
                    ra != null && ra.length == 1 && ra[0] in bankBuchstaben ->
                        AufstellungSlot.Ersatz(bankBuchstaben.indexOf(ra[0]))
                    // Feld A-L: auch hier ist die klassische ra[]-Zuordnung verbindlich, damit
                    // ein Beta-Ersatz-Slot einen Feldspieler nicht auf die Bank "wandern" laesst.
                    pos != null -> AufstellungSlot.Feld(pos.zeile, pos.spalte)
                    b?.slot != null -> b.slot
                    else -> null
                }
                k.copy(
                    nummer = b?.nummer ?: k.nummer,
                    alter = b?.alter ?: k.alter,
                    skill = b?.skill ?: k.skill,
                    opti = b?.opti ?: k.opti,
                    fit = b?.fit ?: k.fit,
                    mor = b?.mor ?: k.mor,
                    position = b?.position ?: k.position,
                    raSlot = ra,
                    slot = slot,
                )
            }
        } else {
            // Keine verlaessliche Berechtigungsliste (klassische Seite fehlt):
            // aus Sicherheitsgruenden keine Spieler auswaehlbar machen.
            emptyList()
        }

        // Fallback: Wenn nach dem Merge kein Spieler als Torwart markiert ist
        // (weder klassisches raSlot=="T" noch ein Beta-Slot greift), dann den
        // Torhueter anhand der Beta-Positions-Klasse (class="tor") ermitteln.
        // Das passiert z.B., wenn die Website nach einem Formation-Laden den
        // Torhueter nur ueber moveFieldClone(player, 0, 0) platziert und die
        // klassische ra[]-Seite kein "T" im Torwart-Select zeigt.
        val mitTorwart = if (spieler.none { it.slot == AufstellungSlot.Torwart }) {
            val torKandidat = spieler.firstOrNull { k ->
                betaSpieler[k.pid]?.position == SpielerPosition.TOR && k.slot != AufstellungSlot.Torwart
            }
            if (torKandidat != null) {
                spieler.map {
                    if (it.pid == torKandidat.pid) it.copy(
                        slot = AufstellungSlot.Torwart,
                        // Beim Promoten zwingend auf "T" setzen - ein stehenbleibender
                        // Bank-Buchstabe (z.B. "U" des Ersatztorwarts) wuerde sonst den
                        // Ersatztorwart weiter als Bank-Besatz anzeigen und die Bank falsch zaehlen.
                        raSlot = "T",
                    ) else it
                }
            } else {
                spieler
            }
        } else {
            spieler
        }

        // Doppelte Ersatzbank-Slots nach dem Merge sind unzulaessig (sonst zaehlt
        // die Bank falsch und das Speichern schlaegt fehl); der Spieler mit dem
        // klassischen Platz-Buchstaben gewinnt, alle weiteren verlieren ihren Slot.
        val konsistent = dedupliziereErsatzbank(mitTorwart)

        return Aufstellung(
            zat = basisKopfdaten?.zat,
            spielart = basisKopfdaten?.spielart,
            gegner = basisKopfdaten?.gegner,
            status = basisKopfdaten?.status,
            spieler = konsistent,
            taktiken = klassisch?.taktiken.orEmpty().ifEmpty { beta?.taktiken.orEmpty() },
            kaderSlots = klassisch?.kaderSlots.orEmpty().ifEmpty { beta?.kaderSlots.orEmpty() },
            rasterPositionen = raster.mapValues { (_, position) ->
                RasterPosition(position.zeile, position.spalte)
            },
            aufstellungsWerte = klassisch?.aufstellungsWerte ?: beta?.aufstellungsWerte,
            zatOptionen = klassisch?.zatOptionen.orEmpty().ifEmpty { beta?.zatOptionen.orEmpty() },
        )
    }

    /**
     * Entfernt doppelte Ersatzbank-Besetzungen: Jeder Platz U-Z darf nur von einem
     * Spieler gehalten werden (sonst zaehlt die Bank falsch und das Speichern des
     * Beta-JSON wuerde fehlschlagen). Der Spieler, dessen klassischer Buchstabe zum
     * Platz passt, gewinnt; alle weiteren Belegungen des Platzes verlieren ihren Slot.
     */
    private fun dedupliziereErsatzbank(spieler: List<AufstellungSpieler>): List<AufstellungSpieler> {
        val holder = LongArray(ERSATZBANK_BUCHSTABEN.length) { -1L }
        ERSATZBANK_BUCHSTABEN.forEachIndexed { index, buchstabe ->
            holder[index] = spieler
                .firstOrNull {
                    (it.slot as? AufstellungSlot.Ersatz)?.index == index && it.raSlot == buchstabe.toString()
                }
                ?.pid
                ?: spieler.firstOrNull { (it.slot as? AufstellungSlot.Ersatz)?.index == index }?.pid
                ?: -1L
        }
        return spieler.map { s ->
            val index = (s.slot as? AufstellungSlot.Ersatz)?.index ?: return@map s
            if (index in holder.indices && holder[index] == s.pid) s else s.copy(slot = null, raSlot = null)
        }
    }

    /**
     * Wendet eine Formation an (klassische Taktikauswahl `raster1`): GET
     * `zugabgabe.php?p=0&raster1=<id>&raster=Laden` und laedt danach die Aufstellung neu.
     * Reale Spielaktion - nur aus bestaetigter Nutzerabsicht aufrufen.
     */
    suspend fun wendeTaktikAn(taktikId: String): Aufstellung = withContext(Dispatchers.IO) {
        val url = "${OsApi.BASE_URL}/zugabgabe.php?p=0&raster1=${encode(taktikId)}&raster=${encode("Laden")}"
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { }
        } catch (e: IOException) {
            throw IOException("Netzwerkfehler beim Laden der Taktik.", e)
        }
        ladeAufstellung()
    }

    private fun safeGet(url: String): String? {
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: return null
                if (SessionGuard.isLoginView(bytes)) return null
                HtmlTools.serverText(bytes)
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Speichert die aktuelle Aufstellung ueber den Beta-Weg (`aufstellung` JSON,
     * Phase-2 5.2). Echte Spielaktion - nur aus bestaetigter Nutzerabsicht aufrufen.
     */
    suspend fun speichereBeta(aufstellung: Aufstellung): String = withContext(Dispatchers.IO) {
        val json = AufstellungSerialisierung.toBetaJson(aufstellung)
        val form = FormBody.Builder().add("aufstellung", json).build()
        val request = Request.Builder()
            .url("${OsApi.BASE_URL}/zugabgabe_beta.php")
            .post(form)
            .build()
        val html = try {
            client.newCall(request).execute().use { it.body?.string().orEmpty() }
        } catch (e: IOException) {
            throw IOException("Netzwerkfehler beim Speichern.", e)
        }
        if (SessionGuard.isLoginView(html.toByteArray())) {
            throw IOException("Sitzung abgelaufen - bitte neu anmelden.")
        }
        if (html.contains("erfolgreich gespeichert", ignoreCase = true) ||
            html.contains("Zugabgabe erfolgreich", ignoreCase = true)
        ) {
            "\u2713 Zugabgabe erfolgreich gespeichert"
        } else {
            // Fehlertext der Seite uebernehmen, falls vorhanden
            val fehler = Jsoup.parse(html).select(".error, .errorbox, .errortext")
                .firstOrNull()?.text()?.takeIf { it.isNotBlank() }
            "Zeichnung gesendet. Serverantwort ohne Best\u00e4tigung" + (fehler?.let { ": $it" } ?: " - bitte pr\u00fcfen.")
        }
    }

    /**
     * Liest die Formular-Optionen einer Aktionen-/Einstellungen-Seite
     * `zugabgabe.php?p=<gruppe>&item=<id>`. Nur lesend.
     */
    suspend fun ladeFormular(typ: ZugabgabeElementTyp): ZugabgabeFormular? = withContext(Dispatchers.IO) {
        safeGet("${OsApi.BASE_URL}/zugabgabe.php?p=${typ.gruppe}&item=${typ.id}")
            ?.let { runCatching { parseFormular(it, typ) }.getOrNull() }
    }

    /**
     * Leist Aktionen/Einstellungen der Seite `zugabgabe.php?p=<gruppe>` (ohne item) aus:
     * Kopfinfo + bereits angelegte Elemente (bestehende Aktionen bzw. Einstellungen).
     */
    suspend fun ladeElemente(gruppe: Int): ZugabgabeElementeSeite = withContext(Dispatchers.IO) {
        safeGet("${OsApi.BASE_URL}/zugabgabe.php?p=$gruppe")
            ?.let { runCatching { parseElementeSeite(it, gruppe) }.getOrNull() }
            ?: ZugabgabeElementeSeite(kopfinfo = null, elemente = emptyList(), loeschLabel = null)
    }

    /**
     * Legt eine Aktion/Einstellung serverseitig an (GET `?p=x&itemcreate=<id>&...&anlegen=...`).
     * Reale Schreibaktion - nur aus bestaetigter Nutzerabsicht aufrufen.
     */
    suspend fun elementSpeichern(typ: ZugabgabeElementTyp, werte: Map<String, String>): String = withContext(Dispatchers.IO) {
        val params = buildList {
            add("p=${typ.gruppe}")
            add("itemcreate=${typ.id}")
            werte.forEach { (k, v) -> if (v.isNotBlank()) add("$k=${encode(v)}") }
            add("anlegen=${encode(if (typ.istEinstellung) "   Neue Einstellung festlegen   " else "   Neue Aktion anlegen   ")}")
        }
        val url = "${OsApi.BASE_URL}/zugabgabe.php?${params.joinToString("&")}"
        val html = try {
            client.newCall(Request.Builder().url(url).build()).execute()
                .use { it.body?.string().orEmpty() }
        } catch (e: IOException) {
            throw IOException("Netzwerkfehler beim Speichern.", e)
        }
        if (SessionGuard.isLoginView(html.toByteArray())) {
            throw IOException("Sitzung abgelaufen - bitte neu anmelden.")
        }
        val doc = Jsoup.parse(html)
        val fehlertext = doc.select(".error, .errorbox, .errortext").firstOrNull()?.text()
            ?.takeIf { it.isNotBlank() }
        if (html.contains("schon vorhanden", ignoreCase = true) ||
            html.contains("nicht m\u00f6glich", ignoreCase = true) ||
            html.contains("nicht moeglich", ignoreCase = true)
        ) {
            fehlertext?.let { throw IOException(it) }
            throw IOException("${typ.label} konnte nicht angelegt werden.")
        }
        fehlertext?.let { throw IOException(it) }
        "\u2713 ${typ.label} angelegt"
    }

    /** Loescht ausgewaehlte Aktionen/Einstellungen der Seite ueber `delzae=<id>`. */
    suspend fun elementeLoeschen(gruppe: Int, auswahl: List<String>, loeschLabel: String?): String = withContext(Dispatchers.IO) {
        if (auswahl.isEmpty()) return@withContext "Keine Elemente ausgew\u00e4hlt."
        val label = loeschLabel ?: if (gruppe == 1) "Gew\u00e4hlte Aktion l\u00f6schen" else "Gew\u00e4hlte Einstellung l\u00f6schen"
        var html = ""
        auswahl.forEach { id ->
            val params = listOf(
                "p=$gruppe",
                "delzae=${encode(id)}",
                "delete=${encode(label)}",
            )
            val url = "${OsApi.BASE_URL}/zugabgabe.php?${params.joinToString("&")}"
            html = try {
                client.newCall(Request.Builder().url(url).build()).execute()
                    .use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("Serverfehler beim L\u00f6schen (HTTP ${response.code}).")
                        }
                        response.body?.string().orEmpty().ifBlank {
                            throw IOException("Leere Serverantwort beim L\u00f6schen.")
                        }
                    }
            } catch (e: IOException) {
                throw IOException("Netzwerkfehler beim L\u00f6schen.", e)
            }
            if (SessionGuard.isLoginView(html.toByteArray())) {
                throw IOException("Sitzung abgelaufen - bitte neu anmelden.")
            }
            Jsoup.parse(html).select(".error, .errorbox, .errortext").firstOrNull()?.text()
                ?.takeIf { it.isNotBlank() }
                ?.let { throw IOException(it) }
        }
        val rest = parseElementeSeite(html, gruppe).elemente
        val nichtGeloescht = rest.map { it.relaId }.intersect(auswahl.toSet())
        if (nichtGeloescht.isNotEmpty()) {
            throw IOException("Nicht alle ausgew\u00e4hlten Elemente konnten gel\u00f6scht werden.")
        }
        if (rest.isEmpty()) {
            "\u2713 ${auswahl.size} Element(e) gel\u00f6scht."
        } else {
            "\u2713 Element(e) gel\u00f6scht (${rest.size} verbleiben)."
        }
    }

    /** "Zugabgabe speichern": ruft checkza.php auf und liefert die Server-Bestaetigung. */
    suspend fun zugabgabeSpeichern(): String = withContext(Dispatchers.IO) {
        val ergebnis = checkzaErgebnis()
        ergebnis.gesamtStatus
    }

    /** Ruft checkza.php auf und liefert ein strukturiertes CheckzaErgebnis. */
    suspend fun checkzaErgebnis(): CheckzaErgebnis = withContext(Dispatchers.IO) {
        val html = try {
            client.newCall(Request.Builder().url("${OsApi.BASE_URL}/checkza.php").build()).execute()
                .use { it.body?.string().orEmpty() }
        } catch (e: IOException) {
            throw IOException("Netzwerkfehler beim Speichern der Zugabgabe.", e)
        }
        if (SessionGuard.isLoginView(html.toByteArray())) {
            throw IOException("Sitzung abgelaufen - bitte neu anmelden.")
        }
        parseCheckzaErgebnis(html)
    }

    /** Parst die checkza.php-HTML-Antwort in ein strukturiertes CheckzaErgebnis. */
    internal fun parseCheckzaErgebnis(html: String): CheckzaErgebnis {
        val doc = Jsoup.parse(html)
        val body = doc.body() ?: return CheckzaErgebnis(
            aufstellung = emptyList(),
            aktionen = emptyList(),
            einstellungen = emptyList(),
            gesamtStatus = html.trim().ifBlank { "Antwort leer." },
            gueltig = false,
        )

        val abschnitte = parseAbschnitte(body)

        val gueltig = html.contains("erfolgreich gespeichert", ignoreCase = true)
        val gesamtStatus = when {
            gueltig -> "\u2713 Zugabgabe erfolgreich gespeichert"
            istKeineGueltigeZugabgabe(html) -> "\u2717 Noch keine g\u00fcltige Zugabgabe vorhanden."
            html.contains("Zugabgabe wird gecheckt", ignoreCase = true) -> {
                val text = body.text()
                "Zugabgabe wird gecheckt\u2026 " + text.takeLast(80).trim()
            }
            else -> body.text().trim().ifBlank { "Zugabgabe gespeichert." }
        }

        return CheckzaErgebnis(
            aufstellung = abschnitte["1"] ?: emptyList(),
            aktionen = abschnitte["2"] ?: emptyList(),
            einstellungen = abschnitte["3"] ?: emptyList(),
            gesamtStatus = gesamtStatus,
            gueltig = gueltig,
        )
    }

    /** Erkennt "Keine g\u00fcltige Zugabgabe" inkl. mojibake (U+FFFD statt \u00fc). */
    private fun istKeineGueltigeZugabgabe(text: String): Boolean {
        val normalisiert = text.replace('\uFFFD', '\u00FC')
        return "keine g\u00fcltige zugabgabe" in normalisiert.lowercase()
            || "keine g\u00fcltige zugabgabe" in text.lowercase()
    }

    /**
     * Parst die drei nummerierten Abschnitte (1. Aufstellung, 2. Aktionen, 3. Einstellungen)
     * aus dem checkza.php-Body.
     */
    private fun parseAbschnitte(body: org.jsoup.nodes.Element): Map<String, List<CheckzaEintrag>> {
        val ergebnis = HashMap<String, List<CheckzaEintrag>>()

        val sectionHeaders = body.select("b").filter { el ->
            val text = el.text().trim()
            text.matches(Regex("^\\d+\\..*"))
        }

        for (header in sectionHeaders) {
            val headerText = header.text().trim()
            val nummer = headerText.substringBefore(".").trim()
            if (nummer !in listOf("1", "2", "3")) continue

            val eintraege = parseSectionTable(header)
            ergebnis[nummer] = eintraege
        }

        return ergebnis
    }

    /**
     * Parst die Tabelle nach einem Abschnitts-Header.
     * Die Tabelle kann <td class="STU"> fuer gueltige Eintraege oder Fehlermeldungen enthalten.
     */
    private fun parseSectionTable(header: org.jsoup.nodes.Element): List<CheckzaEintrag> {
        val eintraege = mutableListOf<CheckzaEintrag>()

        var sibling = header.nextElementSibling()
        while (sibling != null) {
            if (sibling.tagName() == "b") break
            if (sibling.tagName() == "table") {
                val zeilen = sibling.select("tr")
                for (zeile in zeilen) {
                    val zellen = zeile.select("td")
                    if (zellen.isEmpty()) continue

                    val zellenText = zellen.joinToString(" : ") { it.text().trim() }
                    if (zellenText.isBlank()) continue

                    val istGueltig = when {
                        istKeineGueltigeZugabgabe(zellenText) -> false
                        zellenText.contains("Keine Aktionen", ignoreCase = true) -> false
                        zellenText.contains("Keine Einstellungen", ignoreCase = true) -> false
                        zellenText.contains("Fehler", ignoreCase = true) -> false
                        zellenText.contains("ung\u00fcltig", ignoreCase = true) -> false
                        zellenText.contains("ung\u00fcltig".replace('\u00FC', '\uFFFD'), ignoreCase = true) -> false
                        else -> true
                    }

                    eintraege.add(CheckzaEintrag(text = zellenText, gueltig = istGueltig))
                }
                if (eintraege.isNotEmpty()) break
            }
            sibling = sibling.nextElementSibling()
        }

        return eintraege
    }

    /** Verarbeitet `?p=<gruppe>` (ohne item): Kopfinfo + bestehende Elemente. */
    internal fun parseElementeSeite(html: String, gruppe: Int): ZugabgabeElementeSeite {
        val doc = Jsoup.parse(html)
        val kopfinfo = parseKopfinfo(html)
        val loeschLabel = doc.select("input[name=delete]").firstOrNull()?.attr("value")?.trim()
        // Bestehende Eintraege werden vom klassischen Editor als Radio-Buttons
        // `delzae=<id>` ausgegeben.
        val elemente = doc.select("input[type=radio][name=delzae]").mapNotNull { input ->
            val zeile = input.closest("tr")
            val text = zeile?.select("td")?.lastOrNull()?.text()?.trim()
            val id = input.attr("value").trim()
            if (id.isNotEmpty() && !text.isNullOrEmpty()) {
                ZugabgabeElementEintrag(relaId = id, text = text)
            } else {
                null
            }
        }
        return ZugabgabeElementeSeite(kopfinfo = kopfinfo, elemente = elemente, loeschLabel = loeschLabel)
    }

    internal fun parseFormular(html: String, typ: ZugabgabeElementTyp): ZugabgabeFormular {
        val doc = Jsoup.parse(html)
        fun optionen(name: String): List<AuswahlOption> =
            doc.select("select[name=$name] > option").mapNotNull { opt ->
                val label = opt.text().trim()
                AuswahlOption(opt.attr("value"), label)
            }.filter { it.id.isNotEmpty() || it.label.isNotEmpty() }

        fun rein(select: org.jsoup.nodes.Element?): List<AuswahlOption> =
            select?.select("option")?.mapNotNull { opt ->
                val label = opt.text().trim()
                AuswahlOption(opt.attr("value"), label)
            }?.filter { it.id.isNotEmpty() || it.label.isNotEmpty() } ?: emptyList()

        val P1 = doc.selectFirst("select[name=P1]")
        val spieler = (rein(doc.selectFirst("select[name=zao_einspieler]"))
            .ifEmpty { rein(doc.selectFirst("select[name=zao_spieler]")) }
            .ifEmpty { rein(doc.selectFirst("select[name=spieler_id]")) })
        val saveButton = doc.select("input[name=anlegen]").firstOrNull()?.attr("value")?.trim()
            ?: "   Neue Aktion anlegen   "

        return ZugabgabeFormular(
            typ = typ,
            kopfinfo = parseKopfinfo(html),
            spieler = spieler,
            gegenspieler = if (typ == ZugabgabeElementTyp.MANNDECKUNG) rein(P1) else emptyList(),
            minuten = rein(doc.selectFirst("select[name=zao_minute]")),
            abhaengigkeiten = rein(doc.selectFirst("select[name=zao_abhaengigkeit]")),
            positionsZeilen = if (typ.brauchtPosition) rein(P1) else emptyList(),
            positionsSpalten = if (typ.brauchtPosition) rein(doc.selectFirst("select[name=P2]")) else emptyList(),
            positionsSonder = rein(doc.selectFirst("select[name=P3]")),
            werte = if (!typ.brauchtPosition && typ != ZugabgabeElementTyp.MANNDECKUNG) rein(P1) else emptyList(),
            saveButton = saveButton,
        )
    }

    /** Kopfinfo (ZAT, Termin, Spiel, Status) aus der Zugabgabe-Seite. */
    internal fun parseKopfinfo(html: String): ZugabgabeKopfinfo {
        val doc = Jsoup.parse(html)
        val zaText = doc.select("td").mapNotNull { it.ownText() }
            .firstOrNull { it.startsWith("ZA f") }
        val zat = zaText?.let { Regex("ZAT (\\d+)").find(it)?.groupValues?.get(1)?.toInt() }
        val termin = zaText?.let { Regex("Termin: ([^S]*)Spiel").find(it)?.groupValues?.get(1)?.trim() }
        val spielTd = doc.select("td").firstOrNull { it.select("a[href*='teaminfo']").isNotEmpty() }
        val spiel = spielTd?.ownText()?.substringBefore(":")?.trim()?.takeIf { it.isNotEmpty() }
        val status = doc.select("td").mapNotNull { it.ownText() }
            .firstOrNull { it.contains("Zugabgabe:") }?.substringAfter("Zugabgabe:")?.trim()

        return ZugabgabeKopfinfo(zat = zat, termin = termin, spiel = spiel, status = status)
    }

    private fun encode(wert: String): String = URLEncoder.encode(wert, Charsets.UTF_8.name())

    /** Beta-Seite: Spieler-Daten + aktuelle Aufstellung aus loadAufstellung(). */
    internal fun parseBeta(html: String): Aufstellung? {
        val doc = Jsoup.parse(html)
        // Nur diese Website-Liste enthaelt die aktuell spielberechtigten Spieler.
        val spielerElemente = doc.select("#sortable > div.player")
        if (spielerElemente.isEmpty()) return null

        val spieler = spielerElemente.map { div ->
            val pid = div.id().removePrefix("player_").toLongOrNull() ?: return@map null
            val nummer = div.selectFirst(".number")?.text().orEmpty()
            val name = div.selectFirst("p.name a")?.text()
                ?: div.selectFirst("p.name")?.text()
                ?: "Unbekannt"
            val stats = div.selectFirst(".stats")
            val alter = stats?.selectFirst(".alter")?.text()?.toIntOrNull()?.takeIf { it in 15..60 }
            val skill = stats?.selectFirst(".skill")?.text()?.toDoubleOrNull() ?: 0.0
            val opti = stats?.selectFirst(".opti")?.text()?.toDoubleOrNull() ?: 0.0
            val fit = stats?.selectFirst(".fit")?.text()?.toIntOrNull() ?: 0
            val mor = stats?.selectFirst(".mor")?.text()?.toIntOrNull() ?: 0
            val position = positionFromClass(div.className())
            AufstellungSpieler(
                pid = pid,
                name = name,
                nummer = nummer,
                alter = alter,
                skill = skill,
                opti = opti,
                fit = fit,
                mor = mor,
                position = position,
                slot = null,
            )
        }.filterNotNull()

        val slots = parseSlotsFromJs(doc)
        val mitSlot = spieler.map { s -> s.copy(slot = slots[s.pid]) }
        var tor = mitSlot.firstOrNull { it.slot == AufstellungSlot.Torwart }
        if (tor == null) {
            // Ersatztorwart (Bank-Index 0 = Buchstabe U) als aktiv uebernehmen,
            // falls Tor nicht besetzt waere
            tor = mitSlot.firstOrNull {
                it.slot is AufstellungSlot.Ersatz &&
                    (it.slot as AufstellungSlot.Ersatz).index == ERSATZTORWART_BANK_INDEX
            }
        }

        return Aufstellung(
            zat = null,
            spielart = null,
            gegner = null,
            status = null,
            spieler = mitSlot,
        )
    }

    private fun parseSlotsFromJs(doc: org.jsoup.nodes.Document): Map<Long, AufstellungSlot> {
        val script = doc.select("script").joinToString("\n") { it.html() }
        val slots = HashMap<Long, AufstellungSlot>()

        regex("moveOneDropClone\\([^']*'#player_(\\d+)'[^,]*, *(\\d+)\\)").findAll(script)
            .forEach { m ->
                val pid = m.groupValues[1].toLong()
                val nr = m.groupValues[2].toInt()
                // oneDrop-Index der Beta-Seite: 0..4 = Feld-Ersatzspieler (Trikots oben→unten),
                // 5 = Ersatztorwart (#ersatz_goal, ohne 5 = gelb), >= 6 = aktiver Torwart (#goal).
                // Die Beta-Nummerierung ist gegen die klassischen Bank-Buchstaben gespiegelt:
                // Ersatztorwart (nr 5) erhaelt Index 0 (U), oberster Feld-Ersatz (nr 0) Index 5 (Z).
                slots[pid] = when {
                    nr >= 6 -> AufstellungSlot.Torwart
                    else -> AufstellungSlot.Ersatz(ERSATZBANK_BUCHSTABEN.lastIndex - nr)
                }
            }

        regex("moveFieldClone\\([^']*'#player_(\\d+)'[^,]*, *(\\d+), *(\\d+)\\)").findAll(script)
            .forEach { m ->
                val pid = m.groupValues[1].toLong()
                val zeile = m.groupValues[2].toInt()
                val spalte = m.groupValues[3].toInt()
                // Das Beta-JSON speichert den Torwart als [pid, 0, 0] - KO-System (0,0).
                // Beim Laden ruft der Server ggf. moveFieldClone(player, 0, 0) auf.
                // Koordinaten (0,0) sind ausserhalb des gueltigen Spielfelds (Zeilen 1-15,
                // Spalten 1-11) und kennzeichnen eindeutig den Torwart.
                slots[pid] = if (zeile == 0 && spalte == 0) AufstellungSlot.Torwart
                else AufstellungSlot.Feld(zeile = zeile, spalte = spalte)
            }
        return slots
    }

    private fun regex(pattern: String) = Regex(pattern, RegexOption.IGNORE_CASE)

    /** Klassische Formationstabelle: Buchstabe -> Zelle (0-basiert). Zeile 0 = Sturm (O). */
    internal fun parseRasterLetters(html: String): Map<String, RasterZelle> {
        val doc = Jsoup.parse(html)
        val zeilen = "ONMLKJIHGFEDCBA"
        val ergebnis = HashMap<String, RasterZelle>()
        val tabelle = doc.select("table").filter { it.text().contains("Ersatzbank") }.firstOrNull()
            ?: return ergebnis
        tabelle.select("tr").forEach { tr ->
            val tds = tr.select("td")
            if (tds.isEmpty()) return@forEach
            val label = tds[0].text().trim()
            val zi = zeilen.indexOf(label)
            if (zi >= 0 && tds.size >= 12) {
                for (i in 1..11) {
                    val b = tds[i].selectFirst("b")?.text()?.trim()
                    if (b != null && b.length == 1 && b[0] in 'A'..'Z') {
                        ergebnis[b] = RasterZelle(zi, i - 1)
                    }
                }
            }
        }
        return ergebnis
    }

    internal data class RasterZelle(val zeile: Int, val spalte: Int)

    private fun positionFromClass(classes: String): SpielerPosition = when {
        "tor" in classes -> SpielerPosition.TOR
        "abw" in classes -> SpielerPosition.ABW
        "dmi" in classes -> SpielerPosition.DMI
        "mit" in classes -> SpielerPosition.MIT
        "omi" in classes -> SpielerPosition.OMI
        "stu" in classes -> SpielerPosition.STU
        else -> SpielerPosition.AMATEUR
    }

    /** Klassische Seite: Kopfinformationen + ra[]-Feldpositionen als Fallback/Referenz. */
    internal fun parseKlassisch(html: String): Aufstellung {
        val doc = Jsoup.parse(html)

        val kopfinfo = doc.select("td").mapNotNull { it.ownText() }.firstOrNull { it.startsWith("ZA f") }
        val zat = kopfinfo?.let { Regex("ZAT (\\d+)").find(it)?.groupValues?.get(1)?.toInt() }
        val spielzeile = doc.selectFirst("td.TOR")?.text()
            ?.let { Regex("((?:Liga|Friendly|Pokal)[^:]*):\\s*(.+)").find(it) }
        val spielart = spielzeile?.groupValues?.get(1)
        val gegner = spielzeile?.groupValues?.get(2)
        val status = doc.select("td").mapNotNull { it.ownText() }
            .firstOrNull { it.contains("Zugabgabe:") }?.substringAfter("Zugabgabe:")?.trim()

        fun zusammenfassung(label: String): String? = doc.select("tr").firstNotNullOfOrNull { tr ->
            val zellen = tr.children().filter { it.tagName() == "td" }
            if (zellen.size == 2 && zellen[0].text().trim().equals(label, ignoreCase = true)) {
                zellen[1].text().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
        }

        val aufstellungsWerte = AufstellungsWerte(
            optiSkill = zusammenfassung("Opt. Skill"),
            skillSchnitt = zusammenfassung("Skillschnitt"),
            fitness = zusammenfassung("Fitness"),
            moral = zusammenfassung("Moral"),
        ).takeUnless { it.istLeer }

        val taktiken = doc.select("select[name=raster1] > option").mapNotNull { opt ->
            val id = opt.attr("value")
            val label = opt.text().trim()
            if (id.isBlank() || label.isBlank()) null else AuswahlOption(id, label)
        }

        val zatOptionen = doc.select("select[name=lauf] > option").mapNotNull { opt ->
            val id = opt.attr("value")
            val label = opt.text().trim()
            if (id.isBlank() || label.isBlank()) null else AuswahlOption(id, label)
        }

        fun raOptionen(firstRa: org.jsoup.nodes.Element?): List<AuswahlOption> =
            firstRa?.select("option")?.mapNotNull { opt ->
                val id = opt.attr("value")
                val label = opt.text().trim()
                if (label.isBlank()) null else AuswahlOption(id, label)
            } ?: emptyList()

        val kaderSlots = doc.select("select[name]").firstOrNull { it.attr("name").startsWith("ra[") }
            ?.let { raOptionen(it) } ?: emptyList()

        val spieler = doc.select("tr").mapNotNull { tr ->
            val select = tr.select("select[name]").firstOrNull {
                it.attr("name").startsWith("ra[")
            } ?: return@mapNotNull null
            val pid = select.attr("name").removePrefix("ra[").removeSuffix("]").toLongOrNull()
                ?: return@mapNotNull null
            val name = tr.selectFirst("td a[href^=javascript:spielerinfo]")?.text()
                ?: return@mapNotNull null
            val raSlot = select.select("option[selected]").firstOrNull()?.attr("value")
                ?.takeIf { it.isNotBlank() }
            AufstellungSpieler(
                pid = pid,
                name = name,
                nummer = "",
                alter = null,
                skill = 0.0,
                opti = 0.0,
                fit = 0,
                mor = 0,
                position = SpielerPosition.AMATEUR,
                slot = null,
                raSlot = raSlot,
            )
        }
        return Aufstellung(
            zat = zat,
            spielart = spielart,
            gegner = gegner,
            status = status,
            spieler = spieler,
            aufstellungsWerte = aufstellungsWerte,
            taktiken = taktiken,
            kaderSlots = kaderSlots,
            zatOptionen = zatOptionen,
        )
    }

    /**
     * "Laden aus ZAT": GET `zugabgabe.php?p=0&lauf=<ZAT-ID>` uebernimmt alle
     * Einstellungen des gewaehlten ZAT in die aktuelle Zugabgabe. Reale Schreibaktion -
     * nur aus bestaetigter Nutzerabsicht aufrufen. Danach wird neu geladen.
     */
    suspend fun wendeZatAn(zatId: String): Aufstellung = withContext(Dispatchers.IO) {
        val url = "${OsApi.BASE_URL}/zugabgabe.php?p=0&lauf=${encode(zatId)}"
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { }
        } catch (e: IOException) {
            throw IOException("Netzwerkfehler beim Laden aus ZAT.", e)
        }
        ladeAufstellung()
    }

    /**
     * Speichert die Kader-Zuordnung ueber den klassischen Weg (`aufspeichern`) mit
     * allen `ra[<pid>]=<slot>`-Werten. Reale Schreibaktion - nur aus bestaetigter
     * Nutzerabsicht aufrufen. Danach wird neu geladen.
     */
    suspend fun speichereKaderAufstellung(slots: Map<Long, String>): Aufstellung = withContext(Dispatchers.IO) {
        val raParams = slots.entries.joinToString("&") { (pid, slot) ->
            "ra%5B$pid%5D=${encode(slot)}"
        }
        val url = "${OsApi.BASE_URL}/zugabgabe.php?p=0&$raParams&aufspeichern=${encode("Aufstellung speichern")}"
        val html = try {
            client.newCall(Request.Builder().url(url).build()).execute()
                .use { it.body?.string().orEmpty() }
        } catch (e: IOException) {
            throw IOException("Netzwerkfehler beim Speichern der Aufstellung.", e)
        }
        if (SessionGuard.isLoginView(html.toByteArray())) {
            throw IOException("Sitzung abgelaufen - bitte neu anmelden.")
        }
        val fehler = Jsoup.parse(html).select(".error, .errorbox, .errortext").firstOrNull()?.text()
            ?.takeIf { it.isNotBlank() }
        fehler?.let { throw IOException(it) }
        ladeAufstellung()
    }

    /** Loescht die klassische Kader-Aufstellung ueber das Website-Formular. */
    suspend fun loescheKaderAufstellung(): Aufstellung = withContext(Dispatchers.IO) {
        val html = try {
            client.newCall(
                Request.Builder()
                    .url("${OsApi.BASE_URL}/zugabgabe.php?p=0&del_za=1")
                    .build(),
            ).execute().use { it.body?.string().orEmpty() }
        } catch (e: IOException) {
            throw IOException("Netzwerkfehler beim L\u00f6schen der Aufstellung.", e)
        }
        if (SessionGuard.isLoginView(html.toByteArray())) {
            throw IOException("Sitzung abgelaufen - bitte neu anmelden.")
        }
        Jsoup.parse(html).select(".error, .errorbox, .errortext").firstOrNull()?.text()
            ?.takeIf { it.isNotBlank() }
            ?.let { throw IOException(it) }
        ladeAufstellung()
    }
}
