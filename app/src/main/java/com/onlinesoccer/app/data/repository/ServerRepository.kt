package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.HtmlTools
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.EigeneGeboteErgebnis
import com.onlinesoccer.app.data.model.EigeneGeboteZeile
import com.onlinesoccer.app.data.model.FreieTeamZeile
import com.onlinesoccer.app.data.model.FreieTeamsDaten
import com.onlinesoccer.app.data.model.GebotInformation
import com.onlinesoccer.app.data.model.GebotsErgebnis
import com.onlinesoccer.app.data.model.LaenderOption
import com.onlinesoccer.app.data.model.LeihUebersichtErgebnis
import com.onlinesoccer.app.data.model.LeihUebersichtZeile
import com.onlinesoccer.app.data.model.LetzteAktionenArt
import com.onlinesoccer.app.data.model.LetzteAktionenErgebnis
import com.onlinesoccer.app.data.model.LetzteAktionenZeile
import com.onlinesoccer.app.data.model.ManagerSucheDaten
import com.onlinesoccer.app.data.model.ManagerSucheZeile
import com.onlinesoccer.app.data.model.ManagerZeile
import com.onlinesoccer.app.data.model.ManagerlisteDaten
import com.onlinesoccer.app.data.model.TransferFilter
import com.onlinesoccer.app.data.model.TransferListeErgebnis
import com.onlinesoccer.app.data.model.TransferListeZeile
import com.onlinesoccer.app.data.model.TransferMarktEintrag
import com.onlinesoccer.app.data.model.TransferMarktErgebnis
import com.onlinesoccer.app.data.model.TransferDetail
import com.onlinesoccer.app.data.model.TransferOption
import com.onlinesoccer.app.data.model.TransferStatus
import com.onlinesoccer.app.data.model.TransferStatusAntwort
import com.onlinesoccer.app.data.model.TransferStatusErgebnis
import com.onlinesoccer.app.data.model.TransferStatusZeile
import com.onlinesoccer.app.data.model.VersteigerungsmarktEintrag
import com.onlinesoccer.app.data.model.VersteigerungsmarktErgebnis
import com.onlinesoccer.app.data.model.VmGebotInformation
import com.onlinesoccer.app.data.model.VmSetzenAntwort
import com.onlinesoccer.app.data.model.VmSetzenEintrag
import com.onlinesoccer.app.data.model.VmSetzenErgebnis
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Öffentliche Listen der Website: „Freie Teams" (`osneu/freieteams`) und
 * „Team-/Managerliste" (`osneu/managerliste`). Nur lesende GET-/Formular-Aufrufe,
 * die auch ohne Anmeldung funktionieren.
 */
@Singleton
class ServerRepository @Inject constructor(
    private val client: OkHttpClient,
) {

    /**
     * Lädt „Freie Teams". Ohne Länderwahl (id 0) nur der Formular-Aufruf;
     * mit gewähltem Land ein POST `action=loadTeamListe` (serverseitiges Formular-
     * Verhalten, entspricht dem „Anzeigen"-Button) und die passenden Zeilen.
     */
    suspend fun freieTeams(landId: String): FreieTeamsDaten = withContext(Dispatchers.IO) {
        val url = "${OsApi.BASE_URL}/osneu/freieteams"
        val html = if (landId.isBlank() || landId == "0") {
            safeGet(url)
        } else {
            val body = FormBody.Builder()
                .add("action", "loadTeamListe")
                .add("comboLand", landId)
                .build()
            post(url, body)
        } ?: return@withContext FreieTeamsDaten()
        parseFreieTeams(html)
    }

    /** Lädt „Freie Zweitteams" (`osneu/fzt`): gleich aufgebaut wie „Freie Teams",
     * aber Formularfeld `land` ohne versteckte `action` (entspricht dem Website-Button).
     */
    suspend fun freieZweitteams(landId: String): FreieTeamsDaten = withContext(Dispatchers.IO) {
        val url = "${OsApi.BASE_URL}/osneu/fzt"
        val html = if (landId.isBlank() || landId == "0") {
            safeGet(url)
        } else {
            val body = FormBody.Builder()
                .add("land", landId)
                .add("anzeigen", "Anzeigen")
                .build()
            post(url, body)
        } ?: return@withContext FreieTeamsDaten()
        parseFreieZweitteams(html)
    }

    /** Lädt die „Team-/Managerliste" mit optionalen Land-/Liga-Filtern (GET). */
    suspend fun managerliste(landId: String, ligaId: String): ManagerlisteDaten = withContext(Dispatchers.IO) {
        val params = buildList {
            if (landId.isNotBlank() && landId != "0") add("comboLand=$landId")
            if (ligaId.isNotBlank() && ligaId != "0") add("comboLiga=$ligaId")
        }
        val url = "${OsApi.BASE_URL}/osneu/managerliste" + if (params.isEmpty()) "" else "?${params.joinToString("&")}"
        val html = safeGet(url) ?: return@withContext ManagerlisteDaten()
        parseManagerliste(html)
    }

    /** Führt eine „Team-/Managersuche" aus (`osneu/managersuche`): POST mit Manager-
     * und Team-Text sowie Operator (`and`/`or`), Ergebnis sind Manager-/Verein-Zeilen.
     */
    suspend fun managersuche(manager: String, operator: String, team: String): ManagerSucheDaten =
        withContext(Dispatchers.IO) {
            val url = "${OsApi.BASE_URL}/osneu/managersuche"
            val body = FormBody.Builder()
                .add("manager", manager)
                .add("operator", operator)
                .add("team", team)
                .add("Suchen", "Suchen")
                .build()
            val html = post(url, body) ?: return@withContext ManagerSucheDaten()
            parseManagerSuche(html)
        }

    private val TRANSFERLISTE_FILTER = listOf("alter", "skill", "opti", "abloese", "position", "tstatus", "tdetail", "sortierung", "tinfo", "proSeite")
    private val TRANSFERMARKT_FILTER = listOf("alter", "skill", "marktwert", "position", "sortierung")
    private val VERSTEIGERUNGSMARKT_FILTER = TRANSFERMARKT_FILTER

    /** Lädt die „Transferliste" (`osneu/transferliste`) mit den gewählten Filtern (GET,
     * alle Filterfelder werden gesendet, wert 0 = keine Einschränkung) und optionaler Seite.
     * Ohne gewählte Filter (`filter` leer) erscheint die Formular-Seite (nur Optionen). */
    suspend fun transferliste(filter: Map<String, String>, seite: Int): TransferListeErgebnis =
        withContext(Dispatchers.IO) {
            val query = buildList {
                add("gesucht=1")
                TRANSFERLISTE_FILTER.forEach { name -> add("$name=${filter[name] ?: "0"}") }
                if (seite > 1) add("seite=$seite")
            }.joinToString("&")
            val url = "${OsApi.BASE_URL}/osneu/transferliste?$query"
            val html = safeGet(url) ?: return@withContext TransferListeErgebnis()
            parseTransferliste(html, seite)
        }

    /** Lädt die Filteroptionen des „Transfermarkts" (`tm.php`) per GET (Formular-Seite). */
    suspend fun transfermarktFormular(): TransferMarktErgebnis = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/tm.php") ?: return@withContext TransferMarktErgebnis()
        parseTransfermarkt(html)
    }

    /** Lädt den „Transfermarkt" (`tm.php`) mit Filtern per POST (`sshow` = Spieler anzeigen). */
    suspend fun transfermarkt(filter: Map<String, String>): TransferMarktErgebnis =
        withContext(Dispatchers.IO) {
            val url = "${OsApi.BASE_URL}/tm.php"
            val body = FormBody.Builder().apply {
                TRANSFERMARKT_FILTER.forEach { name -> add(name, filter[name] ?: "0") }
                add("sshow", "Spieler anzeigen")
            }.build()
            val html = post(url, body) ?: return@withContext TransferMarktErgebnis()
            parseTransfermarkt(html)
        }

    /** Lädt die Filteroptionen des „Versteigerungsmarkts" (`viewvm.php`) per GET (Formular-Seite). */
    suspend fun versteigerungsmarktFormular(): VersteigerungsmarktErgebnis = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/viewvm.php") ?: return@withContext VersteigerungsmarktErgebnis()
        parseVersteigerungsmarkt(html)
    }

    /** Lädt den „Versteigerungsmarkt" (`viewvm.php`) mit Filtern per POST (`sshow` – nur Anzeigen, kein Gebot). */
    suspend fun versteigerungsmarkt(filter: Map<String, String>): VersteigerungsmarktErgebnis =
        withContext(Dispatchers.IO) {
            val url = "${OsApi.BASE_URL}/viewvm.php"
            val body = FormBody.Builder().apply {
                VERSTEIGERUNGSMARKT_FILTER.forEach { name -> add(name, filter[name] ?: "0") }
                add("sshow", "Spieler anzeigen")
            }.build()
            val html = post(url, body) ?: return@withContext VersteigerungsmarktErgebnis()
            parseVersteigerungsmarkt(html)
        }

    /** Lädt „Auf den VM setzen" (`vmsetzen.php`) nur lesend: eigene, auf den VM
     *  setzbare Spieler samt Startpreis-Staffeln. Es wird nie ein Formular abgesendet. */
    suspend fun vmsetzen(): VmSetzenErgebnis = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/vmsetzen.php") ?: return@withContext VmSetzenErgebnis()
        parseVmSetzen(html)
    }

    internal fun parseTransferliste(html: String, seite: Int = 1): TransferListeErgebnis {
        val doc = Jsoup.parse(html)
        val filter = TRANSFERLISTE_FILTER.mapNotNull { filterKategorie(doc, it) }
        val zeilen = doc.select("table tr").mapNotNull { tr ->
            val link = tr.selectFirst("td a[href*='sp.php']") ?: return@mapNotNull null
            val zellen = tr.select("td")
            if (zellen.size < 8) return@mapNotNull null
            val landZelle = zellen.getOrNull(5)
            val land = landZelle?.selectFirst("img")?.attr("src")
                ?.let { Regex("flaggen/(\\w+)\\.gif").find(it)?.groupValues?.get(1) }
                ?: landZelle?.text()?.trim().orEmpty()
            val teamLink = zellen.getOrNull(8)?.selectFirst("a[href*='st.php']")
            TransferListeZeile(
                spielerId = Regex("s=(\\d+)").find(link.attr("href"))?.groupValues?.get(1)?.toLongOrNull() ?: 0L,
                name = link.text().trim(),
                status = zellen.getOrNull(0)?.text()?.trim().orEmpty(),
                details = zellen.getOrNull(1)?.text()?.trim().orEmpty(),
                alter = zellen.getOrNull(3)?.text()?.trim().orEmpty(),
                position = zellen.getOrNull(4)?.text()?.trim().orEmpty(),
                land = land,
                skill = zellen.getOrNull(6)?.text()?.trim().orEmpty(),
                optSkill = zellen.getOrNull(7)?.text()?.trim().orEmpty(),
                teamId = teamLink?.attr("href")?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                team = teamLink?.text()?.trim().orEmpty(),
                abloese = zellen.getOrNull(9)?.text()?.trim().orEmpty(),
                datum = zellen.getOrNull(10)?.text()?.trim().orEmpty(),
            )
        }
        val pagination = Regex("Seite (\\d+) von (\\d+) \\((\\d+) Treffer\\)").find(html)
        val hinweis = doc.select("p").firstOrNull()?.text()?.trim()
        return TransferListeErgebnis(
            filter = filter,
            hinweis = hinweis,
            zeilen = zeilen,
            seite = pagination?.groupValues?.get(1)?.toIntOrNull() ?: seite,
            gesamtSeiten = pagination?.groupValues?.get(2)?.toIntOrNull() ?: 0,
            treffer = pagination?.groupValues?.get(3)?.toIntOrNull() ?: zeilen.size,
            gesucht = pagination != null || zeilen.isNotEmpty(),
        )
    }

    internal fun parseTransfermarkt(html: String): TransferMarktErgebnis {
        val doc = Jsoup.parse(html)
        val filter = TRANSFERMARKT_FILTER.mapNotNull { filterKategorie(doc, it) }
        val eintraege = doc.select("table#tm tr").mapNotNull { tr ->
            val link = tr.selectFirst("td a[href*='spielerinfo']") ?: return@mapNotNull null
            val zellen = tr.select("td")
            val spielerId = Regex("spielerinfo\\((\\d+)\\)").find(link.attr("href"))
                ?.groupValues?.get(1)?.toLongOrNull() ?: return@mapNotNull null
            val bieterLink = zellen.getOrNull(7)?.selectFirst("a[href*='teaminfo']")
            TransferMarktEintrag(
                spielerId = spielerId,
                name = link.text().trim(),
                alter = zellen.getOrNull(1)?.text()?.trim().orEmpty(),
                position = zellen.getOrNull(2)?.text()?.trim().orEmpty(),
                land = zellen.getOrNull(3)?.text()?.trim().orEmpty(),
                skill = zellen.getOrNull(4)?.text()?.trim().orEmpty(),
                optSkill = zellen.getOrNull(5)?.text()?.trim().orEmpty(),
                gebot = zellen.getOrNull(6)?.text()?.trim().orEmpty(),
                bieter = bieterLink?.text()?.trim().orEmpty(),
                bieterTeamId = bieterLink?.attr("href")
                    ?.let { Regex("teaminfo\\((\\d+)\\)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                gehalt = zellen.getOrNull(8)?.text()?.trim().orEmpty(),
                dauer = zellen.getOrNull(9)?.text()?.trim().orEmpty(),
                anzahl = zellen.getOrNull(10)?.text()?.trim().orEmpty(),
            )
        }
        val hinweis = doc.select("b").firstOrNull { it.text().contains("Transfermarkt") }?.text()?.trim()
        return TransferMarktErgebnis(
            filter = filter,
            hinweis = hinweis,
            eintraege = eintraege,
            gesucht = eintraege.isNotEmpty(),
        )
    }

    internal fun parseVersteigerungsmarkt(html: String): VersteigerungsmarktErgebnis {
        val doc = Jsoup.parse(html)
        val filter = VERSTEIGERUNGSMARKT_FILTER.mapNotNull { filterKategorie(doc, it) }
        val eintraege = doc.select("table#tm tr").mapNotNull { tr ->
            val link = tr.selectFirst("td a[href*='spielerinfo']") ?: return@mapNotNull null
            val zellen = tr.select("td")
            val spielerId = Regex("spielerinfo\\((\\d+)\\)").find(link.attr("href"))
                ?.groupValues?.get(1)?.toLongOrNull() ?: return@mapNotNull null
            val bieterLink = zellen.getOrNull(8)?.selectFirst("a[href*='teaminfo']")
            VersteigerungsmarktEintrag(
                spielerId = spielerId,
                name = link.text().trim(),
                alter = zellen.getOrNull(1)?.text()?.trim().orEmpty(),
                position = zellen.getOrNull(2)?.text()?.trim().orEmpty(),
                land = zellen.getOrNull(3)?.text()?.trim().orEmpty(),
                skill = zellen.getOrNull(4)?.text()?.trim().orEmpty(),
                optSkill = zellen.getOrNull(5)?.text()?.trim().orEmpty(),
                gebot = zellen.getOrNull(6)?.text()?.trim().orEmpty(),
                prozentMw = zellen.getOrNull(7)?.text()?.trim().orEmpty(),
                bieter = bieterLink?.text()?.trim().orEmpty(),
                bieterTeamId = bieterLink?.attr("href")
                    ?.let { Regex("teaminfo\\((\\d+)\\)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                gehalt = zellen.getOrNull(9)?.text()?.trim().orEmpty(),
                dauer = zellen.getOrNull(10)?.text()?.trim().orEmpty(),
                anzahl = zellen.getOrNull(11)?.text()?.trim().orEmpty(),
            )
        }
        val hinweis = doc.select("b").firstOrNull { it.text().contains("Versteigerungsmarkt") }?.text()?.trim()
        return VersteigerungsmarktErgebnis(
            filter = filter,
            hinweis = hinweis,
            eintraege = eintraege,
            gesucht = eintraege.isNotEmpty(),
        )
    }

    internal fun parseVmSetzen(html: String): VmSetzenErgebnis {
        val doc = Jsoup.parse(html)
        val eintraege = doc.select("table tr").mapNotNull { tr ->
            val link = tr.selectFirst("td a[href*='spielerinfo']") ?: return@mapNotNull null
            val pidInput = tr.previousElementSibling()
            val spielerId = if (pidInput != null && pidInput.tagName() == "input" && pidInput.attr("name") == "vmsetzen") {
                pidInput.attr("value").toLongOrNull()
            } else {
                null
            } ?: return@mapNotNull null
            val zellen = tr.select("td")
            val startpreis = tr.selectFirst("select[name=startpreis]")
            val startpreise = startpreis?.select("option")?.mapNotNull { opt ->
                opt.attr("value").toIntOrNull()?.let { TransferOption(it.toString(), opt.text().trim()) }
            }.orEmpty()
            VmSetzenEintrag(
                spielerId = spielerId,
                name = link.text().trim(),
                alter = zellen.getOrNull(1)?.text()?.trim().orEmpty(),
                land = zellen.getOrNull(2)?.text()?.trim().orEmpty(),
                u = zellen.getOrNull(3)?.text()?.trim().orEmpty(),
                skill = zellen.getOrNull(4)?.text()?.trim().orEmpty(),
                opti = zellen.getOrNull(5)?.text()?.trim().orEmpty(),
                marktwert = zellen.getOrNull(6)?.text()?.trim().orEmpty(),
                gebuehr = zellen.getOrNull(7)?.text()?.trim().orEmpty(),
                startpreise = startpreise,
            )
        }
        return VmSetzenErgebnis(eintraege = eintraege)
    }

    /** Lädt „Eigene Gebote" (`viewtm.php`) mit der Summe über alle Gebote. */
    suspend fun eigeneGebote(): EigeneGeboteErgebnis = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/viewtm.php") ?: return@withContext EigeneGeboteErgebnis()
        parseEigeneGebote(html)
    }

    /** Lädt die „Leihspieler Übersicht" (`viewleih.php`: verliehene und geliehene Spieler). */
    suspend fun leihUebersicht(): LeihUebersichtErgebnis = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/viewleih.php") ?: return@withContext LeihUebersichtErgebnis()
        parseLeihUebersicht(html)
    }

    /** Lädt eine „Letzten …"-Seite (`osneu/lasttrans`, `lastleih`, `lastvm`, `lasttm`, `lastblitz`). */
    suspend fun letzteAktionen(art: LetzteAktionenArt): LetzteAktionenErgebnis =
        withContext(Dispatchers.IO) {
            val html = safeGet("${OsApi.BASE_URL}/${art.pfad}") ?: return@withContext LetzteAktionenErgebnis()
            parseLetzteAktionen(html, art)
        }

    /** Lädt die Gebots-Vorschau samt Absende-Formular für einen TM-Spieler (`gebot.php?s=<pid>`, nur lesend). */
    suspend fun gebotInfo(pid: Long): GebotInformation? = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/gebot.php?s=$pid") ?: return@withContext null
        parseGebotInfo(html, pid)
    }

    /**
     * Gibt ein Transfermarkt-Gebot für einen TM-Spieler ab. Entspricht exakt dem
     * Website-Formular: `POST gebot.php?s=<pid>` mit dem Submit-Feld aus der Seite
     * (`name="Gebot"`, Wert „Gebot abgeben als <Team>").
     */
    suspend fun gebotAbgeben(pid: Long): GebotsErgebnis = withContext(Dispatchers.IO) {
        val info = gebotInfo(pid) ?: return@withContext GebotsErgebnis(erfolg = false, meldung = "Keine Gebots-Informationen geladen.")
        val name = info.submitName.ifBlank { "Gebot" }
        val wert = info.submitValue.ifBlank { "Gebot abgeben" }
        val body = FormBody.Builder().add(name, wert).build()
        val html = post("${OsApi.BASE_URL}/gebot.php?s=$pid", body)
            ?: return@withContext GebotsErgebnis(
                erfolg = false,
                meldung = "Die Antwort des Servers konnte nicht gelesen werden. Bitte Status unter „Eigene Gebote“ prüfen, ob das Gebot angekommen ist.",
            )
        parseGebotErgebnis(html)
    }

    /** Lädt die Gebots-Vorschau samt Absende-Formular für einen VM-Spieler (`vmgebot.php?s=<pid>`, nur lesend). */
    suspend fun vmGebotInfo(pid: Long): VmGebotInformation? = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/vmgebot.php?s=$pid") ?: return@withContext null
        parseVmGebotInfo(html, pid)
    }

    /**
     * Gibt ein Versteigerungsmarkt-Gebot für einen VM-Spieler ab. Entspricht dem
     * Website-Formular: `POST vmgebot.php?s=<pid>` mit dem Submit-Feld aus der Seite
     * und – falls vorhanden – dem Betragsfeld (z. B. `Geld`). Ohne Betragsfeld wird
     * nur der Submit-Button gesendet (entspricht dem Klick auf „Gebot abgeben").
     */
    suspend fun vmGebotAbgeben(pid: Long, betrag: String? = null): GebotsErgebnis = withContext(Dispatchers.IO) {
        val info = vmGebotInfo(pid) ?: return@withContext GebotsErgebnis(erfolg = false, meldung = "Keine Gebots-Informationen geladen.")
        val submitName = info.submitName.ifBlank { "Gebot" }
        val submitValue = info.submitValue.ifBlank { "Gebot abgeben" }
        val betragFeld = info.betragName.takeIf { it.isNotBlank() }
        val body = FormBody.Builder().apply {
            if (betragFeld != null) {
                add(betragFeld, betrag?.takeIf { it.isNotBlank() } ?: info.betragWert)
            }
            add(submitName, submitValue)
        }.build()
        val html = post("${OsApi.BASE_URL}/vmgebot.php?s=$pid", body)
            ?: return@withContext GebotsErgebnis(
                erfolg = false,
                meldung = "Die Antwort des Servers konnte nicht gelesen werden. Bitte Status unter „Eigene Gebote“ prüfen, ob das Gebot angekommen ist.",
            )
        parseGebotErgebnis(html)
    }

    /**
     * Setzt einen eigenen Spieler auf den Versteigerungsmarkt. Entspricht exakt dem
     * Website-Formular: `POST vmsetzen.php` mit Hidden `vmsetzen=<pid>` und gewähltem
     * `startpreis` (Submit-Button besitzt keinen Namen und wird daher nicht gesendet).
     * Die App sendet erst, nachdem der Nutzer im Dialog bestätigt hat.
     */
    suspend fun vmSetzen(spielerId: Long, startpreis: String): VmSetzenAntwort = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("vmsetzen", spielerId.toString())
            .add("startpreis", startpreis)
            .build()
        val html = post("${OsApi.BASE_URL}/vmsetzen.php", body)
            ?: return@withContext VmSetzenAntwort(
                erfolg = false,
                meldung = "Die Antwort des Servers konnte nicht gelesen werden. Bitte prüfen, ob der Spieler auf dem Versteigerungsmarkt gelistet ist.",
            )
        parseVmSetzenErgebnis(html, spielerId)
    }

    /** Lädt die „Transferstatus"-Übersicht (`tstatus.php`): alle eigenen Spieler mit Status, Mindestablöse, Infotext und Details. */
    suspend fun transferStatus(): TransferStatusErgebnis = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/tstatus.php") ?: return@withContext TransferStatusErgebnis()
        parseTransferStatus(html)
    }

    /**
     * Speichert den Transferstatus aller Kaderspieler – entspricht exakt dem
     * Website-Formular (`POST tstatus.php`). Liefert der Server wegen einer
     * Änderung eine Kontrollabfrage (`doit=Durchführen`), wird sie automatisch
     * mit denselben Formularwerten bestätigt (zweiter POST wie im Browser).
     * Die App sendet erst, nachdem der Nutzer die Änderung im Dialog bestätigt hat.
     */
    suspend fun transferStatusSpeichern(zeilen: List<TransferStatusZeile>): TransferStatusAntwort =
        withContext(Dispatchers.IO) {
            val url = "${OsApi.BASE_URL}/tstatus.php"
            val html = post(url, transferStatusBody(zeilen))
                ?: return@withContext TransferStatusAntwort(
                    erfolg = false,
                    meldung = "Antwort des Servers nicht lesbar. Bitte Status anschließend unter „Transferstatus“ prüfen.",
                )
            val endHtml = if (istKontrollabfrage(html)) {
                post(url, kontrollabfrageBody(html))
            } else {
                html
            } ?: return@withContext TransferStatusAntwort(
                erfolg = false,
                meldung = "Antwort der Bestätigung nicht lesbar. Bitte Status anschließend prüfen.",
            )
            parseTransferStatusAntwort(endHtml, zeilen)
        }

    /**
     * Erster POST: alle Spieler des Formulars plus den Website-Button
     * „Einstellungen speichern". Checkbox-Details werden wie im Browser nur
     * bei gesetzter Checkbox gesendet.
     */
    private fun transferStatusBody(zeilen: List<TransferStatusZeile>): FormBody =
        FormBody.Builder().apply {
            zeilen.forEach { zeile ->
                add("tstatus[${zeile.spielerId}]", zeile.status.wert)
                add("tmindest[${zeile.spielerId}]", zeile.mindestabloese.ifBlank { "0" })
                add("ttext[${zeile.spielerId}]", zeile.transfertext)
                zeile.details.forEach { detail ->
                    add("tdetails[${zeile.spielerId}][${detail.kuerzel}]", "1")
                }
            }
            add("switch", "Einstellungen speichern")
        }.build()

    /**
     * Zweiter POST: die Kontrollabfrage wird mit ihren Hidden-Feldern und dem
     * Bestätigungs-Button `doit=Durchführen` abgesendet (wie im Browser).
     */
    private fun kontrollabfrageBody(html: String): FormBody =
        Jsoup.parse(html).select("input[type=hidden]").let { inputs ->
            FormBody.Builder().apply {
                inputs.forEach { input ->
                    val name = input.attr("name")
                    if (name.isNotBlank()) add(name, input.attr("value"))
                }
                add("doit", "Durchführen")
            }.build()
        }

    /** True, wenn die Server-Antwort eine Kontrollabfrage (Submit `doit`) ist. */
    internal fun istKontrollabfrage(html: String): Boolean =
        Jsoup.parse(html).selectFirst("input[type=submit][name=doit]") != null

    /**
     * Parser für `tstatus.php`: je Kaderspieler eine Tabellenzeile mit Name,
     * MOR, FIT, Skillschnitt, Opt.Skill sowie Formularfeldern
     * `tstatus[pid]` (Radio), `tmindest[pid]`, `ttext[pid]`,
     * `tdetails[pid][V/K/T/L]` (Checkbox).
     */
    internal fun parseTransferStatus(html: String): TransferStatusErgebnis {
        val doc = Jsoup.parse(html)
        val zeilen = doc.select("tr").mapNotNull { tr ->
            val link = tr.selectFirst("td a[href*='spielerinfo']") ?: return@mapNotNull null
            val pid = Regex("spielerinfo\\((\\d+)\\)").find(link.attr("href"))
                ?.groupValues?.get(1)?.toLongOrNull() ?: return@mapNotNull null
            val inputs = tr.select("input")
            val radios = inputs.filter { it.attr("name") == "tstatus[$pid]" }
            val status = radios.firstOrNull { it.hasAttr("checked") }?.attr("value")
                ?.let { TransferStatus.vonWert(it) } ?: TransferStatus.N
            val details = buildSet {
                TransferDetail.entries.forEach { detail ->
                    val checkbox = inputs.firstOrNull { it.attr("name") == "tdetails[$pid][${detail.kuerzel}]" }
                    if (checkbox?.hasAttr("checked") == true) add(detail)
                }
            }
            val zellen = tr.select("td")
            TransferStatusZeile(
                spielerId = pid,
                name = link.text().trim(),
                mor = zellen.getOrNull(1)?.text()?.trim().orEmpty(),
                fit = zellen.getOrNull(2)?.text()?.trim().orEmpty(),
                skillSchnitt = zellen.getOrNull(3)?.text()?.trim().orEmpty(),
                optSkill = zellen.getOrNull(4)?.text()?.trim().orEmpty(),
                status = status,
                mindestabloese = inputs.firstOrNull { it.attr("name") == "tmindest[$pid]" }?.attr("value").orEmpty(),
                transfertext = inputs.firstOrNull { it.attr("name") == "ttext[$pid]" }?.attr("value").orEmpty(),
                details = details,
            )
        }
        return TransferStatusErgebnis(zeilen = zeilen)
    }

    /**
     * Deutet die Antwort nach der Bestätigung. Erfolg wird anhand der in der
     * Antwort gelesenen Statuswerte geprüft: Entspricht der Spielerstatus der
     * beabsichtigten Änderung, gilt der Speichervorgang als übernommen.
     */
    internal fun parseTransferStatusAntwort(
        html: String,
        erwartet: Collection<TransferStatusZeile>,
    ): TransferStatusAntwort {
        if (html.isBlank()) {
            return TransferStatusAntwort(false, "Keine Antwort vom Server. Bitte Status anschließend prüfen.")
        }
        val text = Jsoup.parse(html).body()?.text().orEmpty()
        val fehlerSatz = text.lineSequence()
            .firstOrNull { satz ->
                satz.isNotBlank() && REAKTION_FEHLER.any { marker -> satz.contains(marker, ignoreCase = true) }
            }
        if (fehlerSatz != null) return TransferStatusAntwort(false, fehlerSatz.trim())

        val geaendert = parseTransferStatus(html)
        if (geaendert.zeilen.isNotEmpty()) {
            val abweichend = erwartet.firstOrNull { erwarteteZeile ->
                geaendert.zeilen.firstOrNull { it.spielerId == erwarteteZeile.spielerId }?.status != erwarteteZeile.status
            }
            return if (abweichend == null) {
                TransferStatusAntwort(true, "Transferstatus gespeichert.")
            } else {
                TransferStatusAntwort(false, "Der Server hat die Änderung nicht vollständig übernommen. Bitte Status anschließend prüfen.")
            }
        }
        if (istKontrollabfrage(html)) {
            return TransferStatusAntwort(false, "Der Server verlangt eine weitere Bestätigung – bitte erneut versuchen.")
        }
        return TransferStatusAntwort(false, "Antwort des Servers nicht eindeutig. Bitte Status anschließend prüfen.")
    }

    private companion object {
        /** Hinweise auf eine fehlgeschlagene Übernahme in Server-Antworten. */
        val REAKTION_FEHLER = listOf(
            "Fehler", "nicht möglich", "ungültig", "gesperrt", "konnte nicht", "abgelaufen", "beendet",
        )
    }

    /**
     * Parser für `gebot.php`: Kennzahlen in `label:`-Kopfzeilen, das zentrierte
     * `<b>Gebot: <betrag></b>` sowie das Absende-Formular (`submit name/value`).
     */
    internal fun parseGebotInfo(html: String, pid: Long = 0L): GebotInformation {
        val doc = Jsoup.parse(html)
        val felder = gebotsFelder(doc)
        val gebotZeile = doc.select("b").firstOrNull { it.text().startsWith("Gebot:") }?.text()?.trim() ?: ""
        val submit = doc.select("form input[type=submit]").firstOrNull()
        val hoechstgebot = felder["Höchstgebot"]?.takeIf { it.isNotBlank() }
            ?: felder["Mindestgebot"]?.takeIf { it.isNotBlank() }
            ?: gebotZeile.removePrefix("Gebot:").trim()
        return GebotInformation(
            spielerId = pid,
            name = felder["Name"].orEmpty(),
            alter = felder["Alter"].orEmpty(),
            nationalitaet = felder["Nationalität"].orEmpty(),
            position = felder["Stammposition"].orEmpty(),
            marktwert = felder["Marktwert"].orEmpty(),
            angeboteBis = felder["Angebote bis"].orEmpty(),
            hoechstgebot = hoechstgebot,
            gehalt = felder["Gehalt"].orEmpty(),
            bieter = felder["Bieter"].orEmpty(),
            submitName = submit?.attr("name").orEmpty(),
            submitValue = submit?.attr("value").orEmpty(),
        )
    }

    /**
     * Parser für `vmgebot.php`: Kennzahlen, das zentrierte `<b>Gebot: <betrag></b>`,
     * das Absende-Formular (`submit name/value`) sowie ein optionales Betragsfeld
     * (`input type=text`, z. B. `Geld`). Der Seitenaufbau entspricht der Dokumentation
     * (Analyse Phase 2, `vmgebot.php?s=<snr>`); fehlende Felder werden leer erfasst.
     */
    internal fun parseVmGebotInfo(html: String, pid: Long = 0L): VmGebotInformation {
        val doc = Jsoup.parse(html)
        val felder = gebotsFelder(doc)
        val gebotZeile = doc.select("b").firstOrNull { it.text().startsWith("Gebot:") }?.text()?.trim() ?: ""
        val submit = doc.select("form input[type=submit]").firstOrNull()
        val betragInput = doc.select("form input[type=text]").firstOrNull()
        val hoechstgebot = felder["Höchstgebot"]?.takeIf { it.isNotBlank() }
            ?: felder["Mindestgebot"]?.takeIf { it.isNotBlank() }
            ?: gebotZeile.removePrefix("Gebot:").trim()
        return VmGebotInformation(
            spielerId = pid,
            name = felder["Name"].orEmpty(),
            alter = felder["Alter"].orEmpty(),
            nationalitaet = felder["Nationalität"].orEmpty(),
            position = felder["Stammposition"].orEmpty(),
            marktwert = felder["Marktwert"].orEmpty(),
            angeboteBis = felder["Angebote bis"].orEmpty(),
            hoechstgebot = hoechstgebot,
            gehalt = felder["Gehalt"].orEmpty(),
            bieter = felder["Bieter"].orEmpty(),
            betragName = betragInput?.attr("name").orEmpty(),
            betragWert = betragInput?.attr("value").orEmpty(),
            submitName = submit?.attr("name").orEmpty(),
            submitValue = submit?.attr("value").orEmpty(),
        )
    }

    /**
     * Antwort auf `POST vmsetzen.php` deuten: Nach dem Absenden rendert der Server die
     * Spielerliste erneut. Erfolg liegt vor, wenn der gesetzte Spieler nicht mehr in der
     * Liste auftaucht und die Seite keine Fehlermeldung enthält. Fehler werden über die
     * bereits bekannten Fehler-Marker erkannt.
     */
    internal fun parseVmSetzenErgebnis(html: String, spielerId: Long): VmSetzenAntwort {
        if (html.isBlank()) {
            return VmSetzenAntwort(false, "Keine Antwort vom Server. Bitte prüfen, ob der Spieler auf dem Versteigerungsmarkt gelistet ist.")
        }
        val text = Jsoup.parse(html).body()?.text().orEmpty()
        val fehlerSatz = text.lineSequence()
            .firstOrNull { satz ->
                satz.isNotBlank() && REAKTION_FEHLER.any { marker -> satz.contains(marker, ignoreCase = true) }
            }
        if (fehlerSatz != null) return VmSetzenAntwort(false, fehlerSatz.trim())
        val nochVorhanden = parseVmSetzen(html).eintraege.any { it.spielerId == spielerId }
        return if (!nochVorhanden) {
            VmSetzenAntwort(true, "Spieler wurde auf den Versteigerungsmarkt gesetzt.")
        } else {
            VmSetzenAntwort(false, "Der Server hat den Spieler nicht auf den Versteigerungsmarkt gesetzt. Bitte prüfen und ggf. erneut versuchen.")
        }
    }

    /** Liest `Label:`→Wert-Paare aus den `<tr>`-Zeilen der Gebots-Formulare (`gebot.php`, `vmgebot.php`). */
    private fun gebotsFelder(doc: Document): Map<String, String> {
        val felder = mutableMapOf<String, String>()
        doc.select("tr").forEach { tr ->
            val tds = tr.select("td")
            var i = 0
            while (i + 1 < tds.size) {
                val label = tds[i].text().trim()
                if (label.endsWith(":") && label.length <= 40) {
                    felder[label.removeSuffix(":").trim()] = tds[i + 1].text().trim()
                    i += 2
                } else {
                    i += 1
                }
            }
        }
        return felder
    }

    /**
     * Antwort auf `POST gebot.php` deuten. Erfolg wird NUR bei einem klaren Erfolgs-Signal
     * gemeldet (`erfolgMarker`), Fehler über `fehlerMarker`. Ist nichts Eindeutiges erkennbar
     * (z. B. umgeleitete Login-Seite oder unerwartete Meldung), gilt das Gebot als unklar und
     * endet „fail-closed" mit dem Hinweis, den Status unter „Eigene Gebote" zu prüfen.
     */
    internal fun parseGebotErgebnis(html: String): GebotsErgebnis {
        if (html.isBlank()) return GebotsErgebnis(false, "Keine Antwort vom Server. Bitte Status unter „Eigene Gebote“ prüfen.")
        val text = Jsoup.parse(html).body()?.text().orEmpty()
        val fehlerMarker = listOf(
            "Sperre", "gesperrt", "ungültig", "nicht möglich", "konnte nicht", "Fehler",
            "kein Guthaben", "nicht freigeschaltet", "abgelaufen", "beendet",
        )
        val erfolgMarker = listOf(
            "erfolgreich", "abgegeben", "gesendet", "Gebot wurde", "geboten",
        )
        val fehlerSatz = text.lineSequence()
            .firstOrNull { satz -> satz.isNotBlank() && fehlerMarker.any { marker -> satz.contains(marker, ignoreCase = true) } }
        if (fehlerSatz != null) return GebotsErgebnis(false, fehlerSatz.trim())
        val erfolgSatz = text.lineSequence()
            .firstOrNull { satz -> satz.isNotBlank() && erfolgMarker.any { marker -> satz.contains(marker, ignoreCase = true) } }
        if (erfolgSatz != null) return GebotsErgebnis(true, erfolgSatz.trim())
        return GebotsErgebnis(
            false,
            "Antwort des Servers nicht eindeutig. Bitte Status unter „Eigene Gebote“ prüfen.",
        )
    }

    /**
     * Parser für „Eigene Gebote" (`viewtm.php`): Tabelle
     * `Name|Alter|Land|U|Skillschnitt|Opt. Skill|Laufzeit|Gehalt|Gebot|Transfertag` plus
     * Summen-Zeile („Die Summe der Gebote beträgt: X Euro").
     */
    internal fun parseEigeneGebote(html: String): EigeneGeboteErgebnis {
        val doc = Jsoup.parse(html)
        val zeilen = doc.select("table table tr").mapNotNull { tr ->
            val link = tr.selectFirst("td a[href*='spielerinfo']") ?: return@mapNotNull null
            val zellen = tr.select("td")
            if (zellen.size < 10) return@mapNotNull null
            val positionsZelle = zellen[3]
            EigeneGeboteZeile(
                spielerId = Regex("spielerinfo\\((\\d+)\\)").find(link.attr("href"))
                    ?.groupValues?.get(1)?.toLongOrNull() ?: return@mapNotNull null,
                name = link.text().trim(),
                alter = zellen[1].text().trim(),
                land = zellen[2].text().trim(),
                position = positionsZelle.text().trim().ifBlank { positionsZelle.attr("class").uppercase() },
                skill = zellen[4].text().trim(),
                optSkill = zellen[5].text().trim(),
                laufzeit = zellen[6].text().trim(),
                gehalt = zellen[7].text().trim(),
                gebot = zellen[8].text().trim(),
                transfertag = zellen[9].text().trim(),
            )
        }
        val summe = Regex("Summe der Gebote betr.{0,10}:?\\s*([\\d.]+)\\s*Euro").find(html)
            ?.groupValues?.get(1).orEmpty()
        return EigeneGeboteErgebnis(zeilen = zeilen, summe = summe)
    }

    /**
     * Parser für die „Leihspieler Übersicht" (`viewleih.php`): zwei Abschnitte
     * (verliehene / geliehene Spieler) mit je einer Tabelle
     * `Name|Alter|Land|U|Skillschnitt|Opt. Skill|Leihdauer|Gehalt|Leihgebühr|Leihclub`.
     */
    internal fun parseLeihUebersicht(html: String): LeihUebersichtErgebnis {
        val doc = Jsoup.parse(html)
        val abschnitte = doc.select("b").mapNotNull { b ->
            when {
                b.text().contains("verliehenen Spieler") -> "verliehen"
                b.text().contains("geliehenen Spieler") -> "geliehen"
                else -> null
            }
        }
        val tabellen = doc.select("table table").mapNotNull { table ->
            val kopf = table.selectFirst("tr")?.text()?.trim().orEmpty()
            if (!kopf.contains("Name") || !kopf.contains("Leihclub")) return@mapNotNull null
            table.select("tr").mapNotNull { tr ->
                val link = tr.selectFirst("td a[href*='spielerinfo']") ?: return@mapNotNull null
                val zellen = tr.select("td")
                if (zellen.size < 10) return@mapNotNull null
                val positionsZelle = zellen[3]
                val clubLink = zellen[9].selectFirst("a[href*='teaminfo']")
                LeihUebersichtZeile(
                    spielerId = Regex("spielerinfo\\((\\d+)\\)").find(link.attr("href"))
                        ?.groupValues?.get(1)?.toLongOrNull() ?: return@mapNotNull null,
                    name = link.text().trim(),
                    alter = zellen[1].text().trim(),
                    land = zellen[2].text().trim(),
                    position = positionsZelle.text().trim().ifBlank { positionsZelle.attr("class").uppercase() },
                    skill = zellen[4].text().trim(),
                    optSkill = zellen[5].text().trim(),
                    dauer = zellen[6].text().trim(),
                    gehalt = zellen[7].text().trim(),
                    leihgebuehr = zellen[8].text().trim(),
                    leihclub = clubLink?.text()?.trim().orEmpty(),
                    leihclubId = clubLink?.attr("href")
                        ?.let { Regex("teaminfo\\((\\d+)\\)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                )
            }
        }
        fun abschnitt(art: String): List<LeihUebersichtZeile> {
            val index = abschnitte.indexOf(art)
            return if (index >= 0 && index < tabellen.size) tabellen[index] else emptyList()
        }
        return LeihUebersichtErgebnis(verliehen = abschnitt("verliehen"), geliehen = abschnitt("geliehen"))
    }

    /**
     * Parser für die „Letzten …"-Seiten. `Letzte Transfers` werden als Blöcke (Datum,
     * Verein-von, Verein-zu, ein oder mehrere Spieler mit Betrag je Seite, Zahlungsdetails)
     * in einzelne Zeilen aufgelöst; die Tabellen der übrigen Seiten werden über ihre
     * (colspan-fähige) Kopfzeile zugeordnet.
     */
    internal fun parseLetzteAktionen(html: String, art: LetzteAktionenArt): LetzteAktionenErgebnis =
        if (art == LetzteAktionenArt.TRANSFERS) {
            parseLetzteTransfers(html)
        } else {
            parseLetzteTransfersTabelle(html)
        }

    private fun parseLetzteTransfers(html: String): LetzteAktionenErgebnis {
        val doc = Jsoup.parse(html)
        val zeilen = doc.select("table[width='100%']").flatMap { block ->
            val stLinks = block.select("td a[href*='st.php']")
            val datum = block.select("td").firstOrNull()?.text()?.trim()
                ?.let { Regex("(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2})").find(it)?.groupValues?.get(1) }.orEmpty()
            val anmerkung = block.select("td").mapNotNull { td ->
                val text = td.text().trim()
                if (text.contains("Euro") && !text.contains(" : ")) text else null
            }.distinct().joinToString(" · ")
            val platzierungen = block.select("a[href*='sp.php']").mapNotNull { link ->
                val name = link.text().trim()
                val spielerZelle = link.parent() ?: return@mapNotNull null
                val voll = spielerZelle.text().trim()
                val betrag = voll.removePrefix(name).removePrefix(" :").trim()
                Triple(link, name, betrag)
            }
            if (platzierungen.isEmpty()) {
                listOf(
                    LetzteAktionenZeile(
                        spielerId = 0L,
                        spieler = "",
                        datum = datum,
                        von = stLinks.firstOrNull()?.text()?.trim().orEmpty(),
                        vonId = stLinks.firstOrNull()?.attr("href")
                            ?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                        zu = stLinks.getOrNull(1)?.text()?.trim().orEmpty(),
                        zuId = stLinks.getOrNull(1)?.attr("href")
                            ?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                        anmerkung = anmerkung,
                    )
                )
            } else {
                platzierungen.map { (link, name, betrag) ->
                    LetzteAktionenZeile(
                        spielerId = Regex("s=(\\d+)").find(link.attr("href"))
                            ?.groupValues?.get(1)?.toLongOrNull() ?: 0L,
                        spieler = name,
                        datum = datum,
                        position = link.parent()?.attr("class")?.uppercase().orEmpty(),
                        von = stLinks.firstOrNull()?.text()?.trim().orEmpty(),
                        vonId = stLinks.firstOrNull()?.attr("href")
                            ?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                        zu = stLinks.getOrNull(1)?.text()?.trim().orEmpty(),
                        zuId = stLinks.getOrNull(1)?.attr("href")
                            ?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                        betrag = betrag,
                        anmerkung = anmerkung,
                    )
                }
            }
        }
        return LetzteAktionenErgebnis(zeilen = zeilen)
    }

    private fun parseLetzteTransfersTabelle(html: String): LetzteAktionenErgebnis {
        val doc = Jsoup.parse(html)
        val rows = doc.select("table tr")
        val kopf = rows.firstOrNull()?.select("th,td")?.flatMap { zelle ->
            val span = zelle.attr("colspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
            List(span) { zelle.text().trim() }
        }.orEmpty()
        if (kopf.isEmpty()) return LetzteAktionenErgebnis()
        val zeilen = rows.drop(1).mapNotNull { tr ->
            val link = tr.selectFirst("td a[href*='sp.php']") ?: return@mapNotNull null
            val zellen = tr.select("td")
            if (zellen.size < kopf.size) return@mapNotNull null
            var datum = ""
            var spieler = ""
            var von = ""
            var vonId: Long? = null
            var zu = ""
            var zuId: Long? = null
            var team = ""
            var teamId: Long? = null
            var ziel = ""
            var betrag = ""
            var dauer = ""
            kopf.forEachIndexed { index, name ->
                val zelle = zellen[index]
                when (name) {
                    "Datum" -> datum = zelle.text().trim()
                    "Spieler" -> spieler = link.text().trim()
                    "Von" -> {
                        von = zelle.text().trim()
                        vonId = zelle.selectFirst("a[href*='st.php']")?.attr("href")
                            ?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() }
                    }
                    "Zu" -> {
                        zu = zelle.text().trim()
                        zuId = zelle.selectFirst("a[href*='st.php']")?.attr("href")
                            ?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() }
                    }
                    "Team" -> {
                        team = zelle.text().trim()
                        teamId = zelle.selectFirst("a[href*='st.php']")?.attr("href")
                            ?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() }
                    }
                    "Ziel" -> ziel = zelle.text().trim()
                    "Zahlung / Abrechnung", "Zahlung", "Preis" -> if (betrag.isEmpty()) {
                        betrag = zelle.text().trim()
                    }
                    "Dauer" -> dauer = zelle.text().trim()
                }
            }
            LetzteAktionenZeile(
                spielerId = Regex("s=(\\d+)").find(link.attr("href"))
                    ?.groupValues?.get(1)?.toLongOrNull() ?: 0L,
                spieler = spieler,
                datum = datum,
                von = von,
                vonId = vonId,
                zu = zu,
                zuId = zuId,
                team = team,
                teamId = teamId,
                ziel = ziel,
                betrag = betrag,
                dauer = dauer,
            )
        }
        return LetzteAktionenErgebnis(zeilen = zeilen)
    }

    /** Optionen eines `select[name=…]` inklusive Platzhalter (wert 0 = „Alle"). */
    private fun filterKategorie(doc: Document, name: String): TransferFilter? {
        val select = doc.selectFirst("select[name=$name]") ?: return null
        val optionen = select.select("option").mapNotNull { opt ->
            val wert = opt.attr("value")
            val label = opt.text().trim()
            if (wert.isBlank() || label.isBlank()) null else TransferOption(wert, label)
        }
        if (optionen.isEmpty()) return null
        return TransferFilter(name, optionen)
    }

    internal fun parseFreieTeams(html: String): FreieTeamsDaten =
        parseTeamVerzeichnis(html, "comboLand", Regex(".*Teams? frei.*"))

    internal fun parseFreieZweitteams(html: String): FreieTeamsDaten =
        parseTeamVerzeichnis(html, "land", Regex(".*Zweitteams zur Bewerbung freigegeben.*"))

    /** Gemeinsamer Parser für die Verein-Listen „Freie Teams" und „Freie Zweitteams". */
    private fun parseTeamVerzeichnis(html: String, selectName: String, anzahlRegex: Regex): FreieTeamsDaten {
        val doc = Jsoup.parse(html)
        val anzahl = doc.select("p").firstOrNull { anzahlRegex.matches(it.text()) }?.text()?.trim()
        val laender = optionen(doc, selectName)
        val zeilen = doc.select("table tr").mapNotNull { tr ->
            val link = tr.selectFirst("td a[href*='st.php']") ?: return@mapNotNull null
            val zellen = tr.select("td")
            val teamId = Regex("c=(\\d+)").find(link.attr("href"))?.groupValues?.get(1)?.toLongOrNull()
            val verein = link.text().trim()
            if (verein.isBlank()) return@mapNotNull null
            FreieTeamZeile(
                teamId = teamId,
                verein = verein,
                land = zellen.getOrNull(1)?.text()?.trim().orEmpty(),
                liga = zellen.getOrNull(2)?.text()?.trim().orEmpty(),
            )
        }
        return FreieTeamsDaten(anzahlText = anzahl, laender = laender, zeilen = zeilen)
    }

    internal fun parseManagerliste(html: String): ManagerlisteDaten {
        val doc = Jsoup.parse(html)
        val laender = optionen(doc, "comboLand")
        val ligas = optionen(doc, "comboLiga")
        val zeilen = doc.select("table.managerliste tr").mapNotNull { tr ->
            val zellen = tr.select("td")
            if (zellen.size < 7) return@mapNotNull null
            val managerLink = zellen[0].selectFirst("a")
            val teamLink = zellen[1].selectFirst("a")
            val manager = managerLink?.text() ?: zellen[0].text().trim()
            val team = teamLink?.text() ?: zellen[1].text().trim()
            if (manager.isBlank() && team.isBlank()) return@mapNotNull null
            ManagerZeile(
                managerId = managerLink?.attr("href")
                    ?.let { Regex("receiver_id=(-?\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                manager = manager,
                teamId = teamLink?.attr("href")
                    ?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                team = team,
                nmr5 = zellen[2].text().trim(),
                nmr20 = zellen[3].text().trim(),
                nmrGesamt = zellen[4].text().trim(),
                wechselsperre = zellen[5].text().trim(),
                zugabgabe = zellen[6].text().trim(),
            )
        }
        return ManagerlisteDaten(laender = laender, ligas = ligas, zeilen = zeilen)
    }

    /** Parser für die „Team-/Managersuche": Zeilen mit Manager-Link (`receiver_id`) +
     * Vereins-Link (`st.php`); „Kein Treffer"-Zeilen ohne Manager-Link werden ignoriert. */
    internal fun parseManagerSuche(html: String): ManagerSucheDaten {
        val doc = Jsoup.parse(html)
        val zeilen = doc.select("tr").mapNotNull { tr ->
            val managerLink = tr.selectFirst("td a[href*='receiver_id=']") ?: return@mapNotNull null
            val vereinLink = tr.selectFirst("td a[href*='st.php']")
            if (managerLink.text().trim().isBlank()) return@mapNotNull null
            ManagerSucheZeile(
                managerId = Regex("receiver_id=(-?\\d+)")
                    .find(managerLink.attr("href"))?.groupValues?.get(1)?.toLongOrNull(),
                manager = managerLink.text().trim(),
                teamId = vereinLink?.attr("href")
                    ?.let { Regex("c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() },
                team = vereinLink?.text()?.trim().orEmpty(),
            )
        }
        return ManagerSucheDaten(zeilen = zeilen)
    }

    /** Optionen eines `select[name=…]` (ohne den leeren Platzhalter, Wert 0). */
    private fun optionen(doc: Document, name: String): List<LaenderOption> =
        doc.select("select[name=$name] > option").mapNotNull { opt ->
            val id = opt.attr("value")
            val label = opt.text().trim()
            if (id.isBlank() || id == "0" || label.isBlank()) {
                null
            } else {
                LaenderOption(id = id, label = label)
            }
        }

    private fun safeGet(url: String): String? {
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                val bytes = response.body?.bytes() ?: return null
                if (SessionGuard.isLoginView(bytes)) null else HtmlTools.serverText(bytes)
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun post(url: String, body: FormBody): String? {
        return try {
            client.newCall(Request.Builder().url(url).post(body).build()).execute().use { response ->
                val bytes = response.body?.bytes() ?: return null
                if (SessionGuard.isLoginView(bytes)) null else HtmlTools.serverText(bytes)
            }
        } catch (e: IOException) {
            null
        }
    }
}
