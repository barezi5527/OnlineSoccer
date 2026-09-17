package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.core.auth.AuthUiState
import com.onlinesoccer.app.core.auth.SessionManager
import com.onlinesoccer.app.core.network.HtmlTools
import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.core.network.SessionGuard
import com.onlinesoccer.app.data.model.AktionFeld
import com.onlinesoccer.app.data.model.AktionFeldTyp
import com.onlinesoccer.app.data.model.AktionForm
import com.onlinesoccer.app.data.model.AktionsButton
import com.onlinesoccer.app.data.model.AktionsOption
import com.onlinesoccer.app.data.model.AktionZeile
import com.onlinesoccer.app.data.model.FremdesTeam
import com.onlinesoccer.app.data.model.KaderSpieler
import com.onlinesoccer.app.data.model.LeihhistorieEintrag
import com.onlinesoccer.app.data.model.SaisonhistorieEintrag
import com.onlinesoccer.app.data.model.SaisonplanDaten
import com.onlinesoccer.app.data.model.SaisonplanEintrag
import com.onlinesoccer.app.data.model.SeitenAnsicht
import com.onlinesoccer.app.data.model.SonderFaehigkeit
import com.onlinesoccer.app.data.model.SpielerKarte
import com.onlinesoccer.app.data.model.SpielerPosition
import com.onlinesoccer.app.data.model.StaerkeZeile
import com.onlinesoccer.app.data.model.StatistikZeile
import com.onlinesoccer.app.data.model.Teaminfo
import com.onlinesoccer.app.data.model.TeamInfoMenuEintrag
import com.onlinesoccer.app.data.model.TransferhistorieBlock
import com.onlinesoccer.app.data.model.UebersichtAbschnitt
import com.onlinesoccer.app.data.model.UebersichtZeile
import com.onlinesoccer.app.data.model.VereinshistorieEintrag
import com.onlinesoccer.app.data.model.VertragZeile
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
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Mannschaft/Kader-Datenquelle (`showteam.php`s=0)).
 *
 * Liefert den kompletten Kader (Nr., Name, Alter, Position, Skill, Opti, Fitness,
 * Moral). Fallback auf die Beta-Zugabgabeseite, falls die Teamübersicht nicht
 * geparst werden kann.
 */
@Singleton
class TeamRepository @Inject constructor(
    private val client: OkHttpClient,
    private val zugabgabeRepository: ZugabgabeRepository,
    private val sessionManager: SessionManager? = null,
) {

    suspend fun ladeKader(): List<KaderSpieler> = withContext(Dispatchers.IO) {
        val squadHtml = safeGet("${OsApi.BASE_URL}/showteam.php?s=0")
        val ausKader = squadHtml?.let {
            runCatching { parseKader(it) }.getOrNull()
        }
        if (!ausKader.isNullOrEmpty()) {
            return@withContext ausKader
        }
        zugabgabeRepository.ladeAufstellung().spieler.map { s ->
            KaderSpieler(
                pid = s.pid,
                name = s.name,
                nummer = s.nummer,
                alter = s.alter,
                position = s.position,
                skill = s.skill,
                opti = s.opti,
                fit = s.fit,
                mor = s.mor,
            )
        }
    }

    /** Parsiert die Teamübersicht (`showteam.php?s=0`). Spalten sind header-basiert. */
    internal fun parseKader(html: String): List<KaderSpieler> {
        val doc = Jsoup.parse(html)
        val tabelle = doc.selectFirst("table#team")
            ?: doc.select("table").maxByOrNull { it.select("tr").size }
            ?: return emptyList()
        val rows = tabelle.select("tr")
        val kopf = rows.firstOrNull { it.select("td,th").size >= 5 } ?: return emptyList()
        val header = kopf.select("td,th").map { it.text().trim() }

        fun spalte(label: String): Int = header.indexOfFirst { it.contains(label, ignoreCase = true) }
        val iNr = spalte("Nr.")
        val iAlter = spalte("Alter")
        val iPos = spalte("Pos")
        val iMor = spalte("MOR")
        val iFit = spalte("FIT")
        val iSkill = spalte("Skillschnitt")
        val iOpti = spalte("Opt.Skill")
        val iSonder = header.indexOfFirst { it.trim().equals("S", ignoreCase = true) }
        val iSperre = spalte("Sperre")

        return rows.drop(1).mapNotNull { tr ->
            val nameEl = tr.selectFirst("td a[href*='sp.php']") ?: return@mapNotNull null
            val pid = nameEl.attr("href")
                .let { Regex("[?&]s=(\\d+)").find(it)?.groupValues?.get(1) }
                ?.toLongOrNull() ?: return@mapNotNull null
            val zellen = tr.select("td")
            val position = positionFromText(zellen.getOrNull(iPos)?.text().orEmpty())
                ?: positionFromClass(nameEl.parent()?.className().orEmpty())
            KaderSpieler(
                pid = pid,
                name = nameEl.text().orEmpty(),
                nummer = zellen.getOrNull(iNr)?.text().orEmpty(),
                alter = zellen.getOrNull(iAlter)?.text()?.toIntOrNull()?.takeIf { it in 15..60 },
                position = position,
                skill = zellen.getOrNull(iSkill)?.text()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0,
                opti = zellen.getOrNull(iOpti)?.text()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0,
                fit = zellen.getOrNull(iFit)?.text()?.parsePct() ?: 0,
                mor = zellen.getOrNull(iMor)?.text()?.parsePct() ?: 0,
                sonderFaehigkeiten = if (iSonder >= 0) {
                    zellen.getOrNull(iSonder)?.select("abbr")?.mapNotNull { abbr ->
                        val kuerzel = abbr.text().trim()
                        SonderFaehigkeit.vonKuerzel(kuerzel)
                            ?: SonderFaehigkeit.vonKuerzel(abbr.attr("title").trim())
                    }.orEmpty()
                } else {
                    emptyList()
                },
                sperre = zellen.getOrNull(iSperre)?.text()?.trim().orEmpty(),
            )
        }
    }

    private fun String.parsePct(): Int {
        val ziffer = replace("%", "").trim().toIntOrNull() ?: return 0
        return ziffer.coerceIn(0, 100)
    }

    /** Positionscode aus dem Text der „Pos“-Spalte (z. B. „TOR“, „ABW“, „STU“). */
    private fun positionFromText(text: String): SpielerPosition? = when (text.trim().uppercase()) {
        "TW", "TOR" -> SpielerPosition.TOR
        "ABW" -> SpielerPosition.ABW
        "DMI" -> SpielerPosition.DMI
        "MIT", "MF" -> SpielerPosition.MIT
        "OMI" -> SpielerPosition.OMI
        "STU", "ST" -> SpielerPosition.STU
        else -> null
    }

    /** Erste Zelle einer Zeile, die einen Positionscode enthält (Fallback-Spalte „Pos“). */
    private fun positionAusZellen(zellen: List<String>): SpielerPosition? =
        zellen.firstNotNullOfOrNull { positionFromText(it) }

    private fun positionFromClass(classes: String): SpielerPosition = when {
        classes.contains("tor", ignoreCase = true) -> SpielerPosition.TOR
        classes.contains("abw", ignoreCase = true) -> SpielerPosition.ABW
        classes.contains("dmi", ignoreCase = true) -> SpielerPosition.DMI
        classes.contains("mit", ignoreCase = true) -> SpielerPosition.MIT
        classes.contains("omi", ignoreCase = true) -> SpielerPosition.OMI
        classes.contains("stu", ignoreCase = true) -> SpielerPosition.STU
        else -> SpielerPosition.AMATEUR
    }

    suspend fun ladeVertraege(): List<VertragZeile> = withContext(Dispatchers.IO) {
        safeGet("${OsApi.BASE_URL}/showteam.php?s=1")?.let { parseVertraege(it) }.orEmpty()
    }

    suspend fun ladeStaerken(): List<StaerkeZeile> = withContext(Dispatchers.IO) {
        safeGet("${OsApi.BASE_URL}/showteam.php?s=2")?.let { parseStaerken(it) }.orEmpty()
    }

    suspend fun ladeStatistik(gesamt: Boolean): List<StatistikZeile> = withContext(Dispatchers.IO) {
        safeGet("${OsApi.BASE_URL}/showteam.php?s=${if (gesamt) 4 else 3}")
            ?.let { parseStatistik(it) }.orEmpty()
    }

    suspend fun ladeTeaminfo(): Teaminfo = withContext(Dispatchers.IO) {
        safeGet("${OsApi.BASE_URL}/showteam.php?s=5")?.let { parseTeaminfo(it) } ?: Teaminfo()
    }

    /**
     * Generische Seite des Team-Bereichs, z. B. `ju.php`, `ka.php`, `osneu/stadion`.
     * Es werden ausschließlich Seiten auf [OsApi.BASE_URL] geladen; gesperrte
     * Seiten (Login-/Rechte-Hinweise) werden abgewiesen. Bekannte Team-Seiten
     * werden mit einem eigenen Parser in übersichtliche Abschnitte zerlegt,
     * alle übrigen Seiten bleiben generisch (Absätze + Tabellen).
     */
    suspend fun ladeSeite(path: String): SeitenAnsicht? = withContext(Dispatchers.IO) {
        val url = when {
            path.startsWith(OsApi.BASE_URL) -> path
            path.startsWith("http") -> null
            else -> "${OsApi.BASE_URL}/$path"
        } ?: return@withContext null
        safeGet(url)?.let { html ->
            if (HtmlTools.sperrHinweis(html) != null) null
            else parseTeamSeite(path, html)
        }
    }

    /**
     * Führt ein auf der Seite angebotenes Formular aus (POST wie im Browser:
     * Felder in Reihenfolge + Absende-Button). Liefert `true` bei Erfolg.
     */
    suspend fun fuehreAktionAus(ziel: String, felder: List<Pair<String, String>>): Boolean =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder().apply { felder.forEach { (name, wert) -> add(name, wert) } }.build()
            try {
                client.newCall(Request.Builder().url(ziel).post(body).build()).execute().use { response ->
                    val bytes = response.body?.bytes() ?: return@withContext false
                    !SessionGuard.isLoginView(bytes)
                }
            } catch (e: IOException) {
                false
            }
        }

    /** Lädt ein separates Formular (z. B. das Scouting-Gebot `juscout.php?g=<id>`). */
    suspend fun ladeAktionFormular(ziel: String): AktionForm? = withContext(Dispatchers.IO) {
        safeGet(ziel)?.let { parseGebotFormular(it) }
    }

    private fun seiteUrl(pfad: String): String =
        if (pfad.startsWith("http")) pfad else "${OsApi.BASE_URL}/$pfad"

    private enum class TeamSeite {
        JUGEND, JUGENDSCOUT, TRAINING, TRAINER, VERTRAEGE, STADION, KONTO, STEUER, TEAM;

        companion object {
            fun vonPfad(pfad: String): TeamSeite? = when {
                pfad.endsWith("ju.php") -> JUGEND
                pfad.endsWith("juscout.php") -> JUGENDSCOUT
                pfad.endsWith("training.php") -> TRAINING
                pfad.endsWith("trainer.php") -> TRAINER
                pfad.endsWith("vt.php") -> VERTRAEGE
                pfad.endsWith("osneu/stadion") -> STADION
                pfad.endsWith("ka.php") -> KONTO
                pfad.endsWith("steuer.php") -> STEUER
                pfad.contains("showteam.php") -> TEAM
                else -> null
            }
        }
    }

    private fun parseTeamSeite(pfad: String, html: String): SeitenAnsicht {
        val ziel = seiteUrl(pfad)
        return when (TeamSeite.vonPfad(pfad)) {
            TeamSeite.JUGEND -> parseJugend(html, ziel)
            TeamSeite.JUGENDSCOUT -> parseJugendscouting(html, ziel)
            TeamSeite.TRAINING -> parseTraining(html, ziel)
            TeamSeite.TRAINER -> parseTrainer(html, ziel)
            TeamSeite.VERTRAEGE -> parseVertraegeVerlaengern(html, ziel)
            TeamSeite.STADION -> parseStadion(html, ziel)
            TeamSeite.KONTO -> parseKonto(html)
            TeamSeite.STEUER -> parseSteuer(html)
            TeamSeite.TEAM -> parseTeamUebersicht(html)
            null -> HtmlTools.seitenAnsicht(html)
        }
    }

    /** Erzeugt ein [AktionFeld] von einem `<select>` (inkl. aktuellem Wert). */
    private fun selectFeld(select: Element): AktionFeld = AktionFeld(
        name = select.attr("name"),
        typ = AktionFeldTyp.AUSWAHL,
        label = select.attr("name"),
        optionen = select.select("option").map { AktionsOption(it.attr("value").ifEmpty { it.text() }, it.text()) },
        standard = select.select("option[selected]").firstOrNull()?.let { it.attr("value").ifEmpty { it.text() } }
            ?: select.select("option").firstOrNull()?.attr("value").orEmpty(),
    )

    /** Jugendteam (`ju.php`): Gruppen je Jahrgang plus „ins A-Team berufen“-Auswahl. */
    internal fun parseJugend(html: String, ziel: String = ""): SeitenAnsicht {
        val doc = Jsoup.parse(html)
        val abschnitte = mutableListOf<UebersichtAbschnitt>()
        val punkte = bitteBeachten(html)
        if (punkte.isNotEmpty()) {
            abschnitte += UebersichtAbschnitt(titel = "Bitte beachten", punkte = punkte)
        }
        val tabelle = doc.select("table").maxByOrNull { it.select("tr").size }
            ?: return SeitenAnsicht(titel = TITEL_JUGEND, abschnitte = abschnitte)
        var gruppe = "Jugendteam"
        val zeilen = mutableListOf<UebersichtZeile>()
        val ziehmich = mutableListOf<AktionsOption>()
        fun flushGruppe() {
            if (zeilen.isNotEmpty()) {
                abschnitte += UebersichtAbschnitt(titel = gruppe, zeilen = zeilen.toList())
                zeilen.clear()
            }
        }
        tabelle.select("tr").forEach { tr ->
            val gruppenKopf = tr.selectFirst("td b")?.text()?.trim()
            if (gruppenKopf != null && gruppenKopf.startsWith("Jahrgang")) {
                flushGruppe()
                gruppe = gruppenKopf
                return@forEach
            }
            val zellen = tr.select("td")
            if (zellen.size < 8) return@forEach
            val alter = zellen[0].text().trim()
            if (alter.toIntOrNull() == null) return@forEach
            tr.selectFirst("input[name=ziehmich]")?.attr("value")
                ?.takeIf { it.isNotBlank() }
                ?.let { pid -> ziehmich += AktionsOption(pid, "$alter Jahre · ${zellen[3].text().trim()}") }
            val pid = tr.selectFirst("input[name=ziehmich]")?.attr("value")
            zeilen += UebersichtZeile(
                ueberschrift = "$alter Jahre · Geb. ${zellen[1].text().trim()}",
                untertitel = zellen[3].text().trim(),
                werte = buildList {
                    add("Skill" to zellen[5].text().trim())
                    add("Talent" to zellen[6].text().trim())
                    zellen[4].text().trim().takeIf { it.isNotBlank() }?.let { add("U" to it) }
                    zellen.getOrNull(8)?.text()?.trim()?.takeIf { it.isNotBlank() }?.let { add("Aufwertung" to it) }
                },
                aktionSchluessel = pid,
                aktionTitel = "Ins A-Team berufen",
            )
        }
        flushGruppe()
        if (ziel.isNotBlank() && ziehmich.isNotEmpty()) {
            abschnitte += UebersichtAbschnitt(
                titel = "Ins A-Team berufen",
                aktionen = listOf(
                    AktionForm(
                        ziel = ziel,
                        felder = listOf(
                            AktionFeld(
                                name = "ziehmich",
                                typ = AktionFeldTyp.RADIO,
                                label = "Spieler auswählen",
                                optionen = ziehmich,
                            ),
                        ),
                        buttons = listOf(AktionsButton("ziehen", "Markierten Spieler ins A-Team berufen")),
                    ),
                ),
            )
        }
        return SeitenAnsicht(titel = TITEL_JUGEND, abschnitte = abschnitte)
    }

    /** Skandiert die Hinweisabsätze unter „Bitte beachten:“ (vor der ersten Tabelle). */
    private fun bitteBeachten(html: String): List<String> {
        val start = html.indexOf("Bitte beachten:")
        if (start < 0) return emptyList()
        val ende = html.indexOf("<table", start)
        val segment = if (ende > start) html.substring(start, ende) else html.substring(start)
        return segment
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .let { org.jsoup.parser.Parser.unescapeEntities(it, false) }
            .lines()
            .map { it.trim().replace("\\s+".toRegex(), " ") }
            .filter { it.isNotEmpty() && !it.equals("Bitte beachten:", true) }
    }

    /** Jugendscouting (`juscout.php`): Angebote plus „Gebot abgeben“ (öffnet Gebot-Formular). */
    internal fun parseJugendscouting(html: String, ziel: String = ""): SeitenAnsicht {
        val doc = Jsoup.parse(html)
        val tabelle = doc.select("table").maxByOrNull { it.select("tr").size }
            ?: return SeitenAnsicht(titel = TITEL_JUGENDSCOUT)
        val zeilen = mutableListOf<UebersichtZeile>()
        val gebote = mutableListOf<AktionZeile>()
        tabelle.select("tr").forEach { tr ->
            val zellen = tr.select("td")
            if (zellen.size < 11) return@forEach
            val alter = zellen[0].text().trim()
            if (alter.toIntOrNull() == null) return@forEach
            val name = "$alter Jahre · Geb. ${zellen[1].text().trim()}"
            zeilen += UebersichtZeile(
                ueberschrift = name,
                untertitel = zellen[3].text().trim(),
                werte = listOf(
                    "Skill" to zellen[2].text().trim(),
                    "Gebotsfrist" to zellen[5].text().trim(),
                    "Mindestgebot" to zellen[6].text().trim(),
                    "Hoch" to zellen[7].text().trim(),
                    "Mittel" to zellen[8].text().trim(),
                    "Gering" to zellen[9].text().trim(),
                ),
            )
            val snr = Regex("jubieten\\((\\d+)\\)").find(zellen[10].html())
                ?.groupValues?.get(1) ?: return@forEach
            zeilen[zeilen.lastIndex] = zeilen.last().copy(
                aktionSchluessel = snr,
                aktionTitel = "Gebot abgeben",
            )
            gebote += AktionZeile(
                schluessel = snr,
                bezeichnung = name,
                ziel = if (ziel.isNotBlank()) "$ziel?g=$snr" else "",
                button = AktionsButton(null, "Gebot abgeben"),
            )
        }
        val abschnitte = mutableListOf(
            UebersichtAbschnitt(
                titel = "Scouting-Angebote",
                zeilen = zeilen,
                aktionen = if (ziel.isNotBlank() && gebote.isNotEmpty()) {
                    listOf(
                        AktionForm(
                            ziel = ziel,
                            titel = "Gebote",
                            zeilen = gebote,
                        ),
                    )
                } else emptyList(),
            ),
        )
        return SeitenAnsicht(titel = TITEL_JUGENDSCOUT, abschnitte = abschnitte)
    }

    /** Training (`training.php`): Trainer-Betreuung, Trainingsspeicher und Spieler-Trainingseinstellungen. */
    internal fun parseTraining(html: String, ziel: String = ""): SeitenAnsicht {
        val doc = Jsoup.parse(html)
        val abschnitte = mutableListOf<UebersichtAbschnitt>()
        val tabellen = doc.select("table")
        val betreuung = tabellen.firstOrNull()?.select("td")?.mapNotNull { z ->
            val m = Regex("^Trainer (\\d+): (\\d+) Spieler$").find(z.text().trim())
            m?.let { "Trainer ${it.groupValues[1]}" to "${it.groupValues[2]} Spieler" }
        }.orEmpty()
        if (betreuung.isNotEmpty()) {
            abschnitte += UebersichtAbschnitt(titel = "Trainer-Betreuung", infoZeilen = betreuung)
        }

        val speicherFormulare = mutableListOf<AktionForm>()
        doc.select("form").forEach { form ->
            when {
                form.selectFirst("select[name=trainingload]") != null -> speicherFormulare += AktionForm(
                    ziel = ziel,
                    titel = "Training laden",
                    felder = listOf(
                        selectFeld(form.selectFirst("select[name=trainingload]")!!).copy(label = "Bestehende Einstellung"),
                    ),
                    buttons = listOf(AktionsButton(null, "laden")),
                )
                form.selectFirst("select[name=trainingsaveas]") != null -> speicherFormulare += AktionForm(
                    ziel = ziel,
                    titel = "Training speichern unter",
                    felder = listOf(
                        selectFeld(form.selectFirst("select[name=trainingsaveas]")!!).copy(label = "Einstellung überschreiben"),
                    ),
                    buttons = listOf(AktionsButton(null, "Speichern als")),
                )
                form.selectFirst("input[name=trainingsave]") != null -> speicherFormulare += AktionForm(
                    ziel = ziel,
                    titel = "Neue Training-Speicherung",
                    felder = listOf(
                        AktionFeld(
                            name = "trainingsave",
                            typ = AktionFeldTyp.TEXT,
                            label = "Name der neuen Einstellung",
                            standard = "",
                        ),
                    ),
                    buttons = listOf(AktionsButton(null, "Neue Speicherung anlegen")),
                )
                form.selectFirst("select[name=trainingsdelete]") != null -> speicherFormulare += AktionForm(
                    ziel = ziel,
                    titel = "Training löschen",
                    felder = listOf(
                        selectFeld(form.selectFirst("select[name=trainingsdelete]")!!).copy(label = "Zu löschende Einstellung"),
                    ),
                    buttons = listOf(AktionsButton(null, "Löschen")),
                )
            }
        }
        if (speicherFormulare.isNotEmpty()) {
            val warnung = doc.select("td").firstOrNull { it.text().contains("nicht übernommen") }?.text()?.trim()
            abschnitte += UebersichtAbschnitt(
                titel = "Trainingsspeicher",
                punkte = listOfNotNull(warnung),
                aktionen = speicherFormulare,
            )
        }

        val tabelle = tabellen.maxByOrNull { it.select("tr").size }
            ?: return SeitenAnsicht(titel = TITEL_TRAINING, abschnitte = abschnitte)
        val zeilen = mutableListOf<UebersichtZeile>()
        val trainingsZeilen = mutableListOf<AktionZeile>()
        val hauptForm = doc.select("form").firstOrNull { it.selectFirst("input[name=trainingspeichern]") != null }
        tabelle.select("tr").forEach { tr ->
            val nameEl = tr.selectFirst("td a")
            val name = nameEl?.text()?.trim() ?: return@forEach
            val zellen = tr.select("td")
            val alter = zellen.getOrNull(2)?.text()?.trim().orEmpty()
            val opti = zellen.getOrNull(3)?.text()?.trim().orEmpty()
            val trainer = tr.selectFirst("select")?.selectFirst("option[selected]")?.text()?.trim()
                ?: tr.selectFirst("select")?.selectFirst("option")?.text()?.trim().orEmpty()
            val skillSelect = tr.select("select").getOrNull(1)
            val skill = skillSelect?.selectFirst("option[selected]")?.text()?.trim()
                ?: skillSelect?.selectFirst("option")?.text()?.trim().orEmpty()
            val chance = zellen.getOrNull(7)?.text()?.trim().orEmpty()
            val pid = Regex("spielerinfo\\((\\d+)\\)").find(nameEl.outerHtml())?.groupValues?.get(1) ?: return@forEach
            val klasse = nameEl.parent()?.className().orEmpty()
            val positionName = listOf("tor", "abw", "dmi", "mit", "omi", "stu")
                .firstOrNull { klasse.contains(it, ignoreCase = true) }
                ?.let { positionsName(positionFromClass(klasse)) }
            val untertitel = buildList {
                positionName?.let(::add)
                if (alter.isNotBlank()) add("$alter Jahre")
                if (opti.isNotBlank()) add("Opti $opti")
            }.joinToString(" · ")
            zeilen += UebersichtZeile(
                ueberschrift = name,
                untertitel = untertitel.ifBlank { null },
                werte = listOf(
                    "Trainer" to trainer,
                    "trainierter Skill" to skill,
                    "Chance" to chance,
                ),
                aktionSchluessel = pid,
                aktionTitel = "Training bearbeiten",
            )
            val feld1 = hauptForm?.selectFirst("select[name=tr1$pid]")
            val feld2 = hauptForm?.selectFirst("select[name=tr2$pid]")
            if (feld1 != null || feld2 != null) {
                trainingsZeilen += AktionZeile(
                    schluessel = pid,
                    bezeichnung = name,
                    felder = listOfNotNull(
                        feld1?.let { selectFeld(it).copy(label = "Trainer") },
                        feld2?.let { selectFeld(it).copy(label = "Trainierter Skill") },
                    ),
                )
            }
        }
        if (ziel.isNotBlank() && trainingsZeilen.isNotEmpty()) {
            abschnitte += UebersichtAbschnitt(
                titel = "Spieler-Training",
                zeilen = zeilen,
                aktionen = listOf(
                    AktionForm(
                        ziel = ziel,
                        titel = "Trainings-Einstellungen",
                        zeilen = trainingsZeilen,
                        buttons = listOf(
                            AktionsButton("trainingspeichern", "Trainingseinstellung speichern"),
                        ),
                    ),
                ),
            )
        } else {
            abschnitte += UebersichtAbschnitt(titel = "Spieler-Training", zeilen = zeilen)
        }
        return SeitenAnsicht(titel = TITEL_TRAINING, abschnitte = abschnitte)
    }

    /** Trainer (`trainer.php`): Trainerstab samt Monatsgehältern und Einstell-Formularen. */
    internal fun parseTrainer(html: String, ziel: String = ""): SeitenAnsicht {
        val doc = Jsoup.parse(html)
        val summe = Regex("Summe.*?([\\d.]+)\\s*Euro").find(doc.text())?.groupValues?.get(1)
        val abschnitte = mutableListOf<UebersichtAbschnitt>()
        if (summe != null) {
            abschnitte += UebersichtAbschnitt(
                titel = "Übersicht des Trainerstabes",
                infoZeilen = listOf("Trainermonatsgehälter gesamt" to "$summe Euro"),
            )
        }
        val tabelle = doc.select("table").maxByOrNull { it.select("tr").size }
            ?: return SeitenAnsicht(titel = TITEL_TRAINER, abschnitte = abschnitte)
        val zeilen = tabelle.select("tr").mapNotNull { tr ->
            val zellen = tr.select("td")
            if (zellen.size < 4) return@mapNotNull null
            val nr = zellen[0].text().trim()
            if (nr.toIntOrNull() == null) return@mapNotNull null
            UebersichtZeile(
                ueberschrift = "Trainer $nr",
                untertitel = zellen[1].text().trim(),
                werte = listOf(
                    "Gehalt" to zellen[2].text().trim(),
                    "Vertrag" to "${zellen[3].text().trim()} ZAT",
                ),
                aktionSchluessel = nr,
                aktionTitel = "Trainer einstellen",
            )
        }
        val forms = doc.select("input[name=trainer]").indices.mapNotNull { idx ->
            val nr = doc.select("input[name=trainer]")[idx].attr("value")
            val skill = doc.select("select[name=skill]").getOrNull(idx) ?: return@mapNotNull null
            val dauer = doc.select("select[name=dauer]").getOrNull(idx) ?: return@mapNotNull null
            AktionForm(
                ziel = ziel,
                titel = "Trainer $nr einstellen",
                felder = listOf(
                    AktionFeld(name = "trainer", typ = AktionFeldTyp.VERSTECKT, standard = nr),
                    selectFeld(skill).copy(label = "Skill"),
                    selectFeld(dauer).copy(label = "Vertragslaufzeit"),
                ),
                buttons = listOf(AktionsButton("einstellen", "einstellen als Trainer $nr")),
            )
        }
        abschnitte += UebersichtAbschnitt(
            titel = "Trainerstab",
            zeilen = zeilen,
            aktionen = if (ziel.isNotBlank()) forms else emptyList(),
        )
        return SeitenAnsicht(titel = TITEL_TRAINER, abschnitte = abschnitte)
    }

    /** Verträge verlängern (`vt.php`): Verlängerungsangebote je Spieler, nur lesend. */
    internal fun parseVertraegeVerlaengern(html: String, ziel: String = ""): SeitenAnsicht {
        val doc = Jsoup.parse(html)
        val tabelle = doc.select("table").maxByOrNull { it.select("tr").size }
            ?: return SeitenAnsicht(titel = TITEL_VT)
        val kopf = tabelle.select("tr").firstOrNull()?.select("td,th")?.map { it.text().trim() }.orEmpty()
        val monate = buildList {
            for (i in 0 until kopf.size - 1) {
                val zahl = kopf[i].toIntOrNull()
                if (zahl != null && kopf[i + 1].equals("Monate", ignoreCase = true)) add(zahl to i + 1)
            }
        }
        val zeilen = tabelle.select("tr").drop(1).mapNotNull { tr ->
            val nameEl = tr.selectFirst("td a") ?: return@mapNotNull null
            val name = nameEl.text().trim()
            val zellen = tr.select("td")
            val pid = Regex("spielerinfo\\((\\d+)\\)").find(nameEl.outerHtml())
                ?.groupValues?.get(1) ?: return@mapNotNull null
            UebersichtZeile(
                ueberschrift = name,
                untertitel = "${zellen.getOrNull(1)?.text()?.trim().orEmpty()} Jahre · ${zellen.getOrNull(2)?.text()?.trim().orEmpty()}",
                werte = buildList {
                    zellen.getOrNull(3)?.text()?.trim()?.takeIf { it.isNotBlank() }?.let { add("Gehalt" to it) }
                    zellen.getOrNull(4)?.text()?.trim()?.takeIf { it.isNotBlank() }?.let { add("Laufzeit" to "$it Monate") }
                    zellen.getOrNull(5)?.text()?.trim()?.takeIf { it.isNotBlank() }?.let { add("Skillschnitt" to it) }
                    zellen.getOrNull(6)?.text()?.trim()?.takeIf { it.isNotBlank() }?.let { add("Opt. Skill" to it) }
                    monate.forEach { (anzahl, wertIndex) ->
                        zellen.getOrNull(wertIndex)?.text()?.trim()?.takeIf { it.isNotBlank() }
                            ?.let { add("$anzahl Monate" to it) }
                    }
                },
                aktionSchluessel = pid,
                aktionTitel = "Vertrag verlängern",
            )
        }
        val gebote = mutableListOf<AktionZeile>()
        tabelle.select("tr").drop(1).forEach { tr ->
            val nameEl = tr.selectFirst("td a") ?: return@forEach
            val pid = Regex("spielerinfo\\((\\d+)\\)").find(nameEl.outerHtml())
                ?.groupValues?.get(1) ?: return@forEach
            val radios = tr.select("input[type=radio]").filter { it.attr("name").startsWith("gehalt[") }
            if (radios.isEmpty()) return@forEach
            gebote += AktionZeile(
                schluessel = pid,
                bezeichnung = nameEl.text().trim(),
                felder = listOf(
                    AktionFeld(
                        name = "gehalt[$pid]",
                        typ = AktionFeldTyp.RADIO,
                        label = "Laufzeit wählen",
                        optionen = radios.mapNotNull { radio ->
                            val monate = when (radio.attr("value")) {
                                "1" -> "24"
                                "2" -> "36"
                                "3" -> "48"
                                "4" -> "60"
                                else -> null
                            } ?: return@mapNotNull null
                            AktionsOption(radio.attr("value"), "$monate Monate")
                        },
                        standard = radios.firstOrNull { it.hasAttr("checked") }?.attr("value").orEmpty(),
                    ),
                ),
            )
        }
        return SeitenAnsicht(
            titel = TITEL_VT,
            abschnitte = listOf(
                UebersichtAbschnitt(
                    titel = "Verlängerungsangebote (Gehalt / Monat)",
                    zeilen = zeilen,
                    aktionen = if (ziel.isNotBlank() && gebote.isNotEmpty()) {
                        listOf(
                            AktionForm(
                                ziel = ziel,
                                titel = "Verträge verlängern",
                                felder = listOf(
                                    AktionFeld(name = "update", typ = AktionFeldTyp.VERSTECKT, standard = "1"),
                                ),
                                zeilen = gebote,
                                buttons = listOf(AktionsButton("vertragsauswahl", "Verträge verlängern")),
                            ),
                        )
                    } else emptyList(),
                ),
            ),
        )
    }

    /** Stadionausbau (`osneu/stadion`): aktueller Zustand + Ausbaumöglichkeiten + Ausbau-Formular. */
    internal fun parseStadion(html: String, ziel: String = ""): SeitenAnsicht {
        val doc = Jsoup.parse(html)
        val abschnitte = mutableListOf<UebersichtAbschnitt>()

        val laufenderAusbau = doc.selectFirst("p.tor")
        if (laufenderAusbau != null) {
            val headline = laufenderAusbau.text().trim().removeSuffix(":")
            val details = mutableListOf<String>()
            if (headline.isNotEmpty()) details += headline
            var sibling = laufenderAusbau.nextElementSibling()
            while (sibling != null && sibling.tagName() == "p") {
                val text = sibling.text().trim()
                if (text.isNotEmpty()) details += text
                sibling = sibling.nextElementSibling()
            }
            abschnitte += UebersichtAbschnitt(titel = TITEL_LAUFENDER_AUSBAU, punkte = details)
        }

        val zustand = doc.selectFirst("table")?.select("tr")?.mapNotNull { tr ->
            val zellen = tr.select("td").map { it.text().replace('\u00a0', ' ').trim() }.filter { it.isNotEmpty() }
            if (zellen.size < 2) return@mapNotNull null
            val pairs = mutableListOf<Pair<String, String>>()
            var i = 0
            while (i + 1 < zellen.size) {
                val label = zellen[i].removeSuffix(":").trim()
                if (label.isNotEmpty()) pairs += label to zellen[i + 1]
                i += 2
            }
            pairs
        }.orEmpty().flatten()
        val aufbereitet = zustand.mapIndexed { idx, (label, wert) ->
            when {
                label == "davon überdacht" && zustand.getOrNull(idx - 1)?.first == "Sitzplätze" ->
                    "davon überdacht (Sitz)" to wert
                label == "davon überdacht" && zustand.getOrNull(idx - 1)?.first == "Stehplätze" ->
                    "davon überdacht (Steh)" to wert
                else -> label to wert
            }
        }
        if (aufbereitet.isNotEmpty()) {
            abschnitte += UebersichtAbschnitt(titel = "Aktueller Stadion-Zustand", infoZeilen = aufbereitet)
        }

        val tabellen = doc.select("table")
        val optionen = tabellen.getOrNull(1)?.select("tr")?.mapNotNull { tr ->
            val zellen = tr.select("td").map { it.text().trim() }
            if (zellen.size < 3) return@mapNotNull null
            val m = Regex("^(.+?)\\s*\\(([\\d.]+)\\s+pro Platz\\):?\\s*$").find(zellen[0])
            val label = m?.groupValues?.get(1) ?: zellen[0].removeSuffix(":").trim()
            val kosten = m?.let { "${it.groupValues[2]} pro Platz" } ?: ""
            val max = Regex("max\\.\\s*([\\d.]+)").find(zellen[2])?.groupValues?.get(1)
            if (label.isBlank()) return@mapNotNull null
            UebersichtZeile(
                ueberschrift = label,
                untertitel = kosten,
                werte = max?.let { listOf("Maximum" to it) }.orEmpty(),
            )
        }.orEmpty()
        if (optionen.isNotEmpty()) {
            val form = doc.selectFirst("form[action]") ?.let { form ->
                val ziel = form.attr("action").let { aktion ->
                    when {
                        aktion.startsWith("http") -> aktion
                        aktion.startsWith("/") -> "${OsApi.BASE_URL}$aktion"
                        else -> "$ziel/$aktion"
                    }
                }
                AktionForm(
                    ziel = ziel,
                    titel = "Ausbau beantragen",
                    felder = buildList {
                        add(AktionFeld(name = "action", typ = AktionFeldTyp.VERSTECKT, standard = "doAngebot"))
                        form.select("input[type=number]").forEach { input ->
                            val anzahl = Regex("^(\\d[\\.\\d]*)([\\.,]\\d+)?$").find(
                                input.attr("value"),
                            )?.groupValues?.get(1)
                            add(
                                AktionFeld(
                                    name = input.attr("name"),
                                    typ = AktionFeldTyp.NUMMER,
                                    label = "Anzahl Plätze",
                                    standard = anzahl ?: "",
                                    min = input.attr("min").toIntOrNull(),
                                    max = input.attr("max").toIntOrNull(),
                                ),
                            )
                        }
                        form.select("select").forEach { select ->
                            add(selectFeld(select).copy(label = if (select.attr("name") == "heizung") "Rasenheizung" else "Anzeigetafel"))
                        }
                    },
                    buttons = listOf(AktionsButton(null, "Angebote einholen")),
                )
            }
            abschnitte += UebersichtAbschnitt(
                titel = "Ausbaumöglichkeiten",
                zeilen = optionen,
                aktionen = if (ziel.isNotBlank() && form != null) listOf(form) else emptyList(),
            )
        }
        return SeitenAnsicht(titel = TITEL_STADION, abschnitte = abschnitte)
    }

    /** Scouting-Gebot (`juscout.php?g=<id>`): Formular mit Ersatzspieler + Geldbetrag. */
    internal fun parseGebotFormular(html: String): AktionForm {
        val doc = Jsoup.parse(html)
        val form = doc.selectFirst("form[action]") ?: return AktionForm(ziel = "")
        val ziel = form.attr("action").let { aktion ->
            when {
                aktion.startsWith("http") -> aktion
                aktion.startsWith("/") -> "${OsApi.BASE_URL}$aktion"
                else -> "${OsApi.BASE_URL}/$aktion"
            }
        }
        val exchange = form.selectFirst("select[name=exchange]")
        val geld = form.selectFirst("input[name=Geld]")
        return AktionForm(
            ziel = ziel,
            titel = "Gebot abgeben",
            felder = listOfNotNull(
                exchange?.let(::selectFeld)?.copy(label = "Ersatzspieler"),
                geld?.let {
                    AktionFeld(
                        name = "Geld",
                        typ = AktionFeldTyp.NUMMER,
                        label = "Gebot (EUR)",
                        standard = it.attr("value").ifEmpty { "0" },
                    )
                },
            ),
            buttons = listOf(AktionsButton("Gebot", "Gebot abgeben")),
        )
    }

    /** Kontoauszug (`ka.php`): Buchungen, nur lesend. */
    internal fun parseKonto(html: String): SeitenAnsicht {
        val doc = Jsoup.parse(html)
        val tabelle = doc.select("table").maxByOrNull { it.select("tr").size }
            ?: return SeitenAnsicht(titel = TITEL_KONTO)
        val zeilen = tabelle.select("tr").drop(1).mapNotNull { tr ->
            val zellen = tr.select("td").map { it.text().trim() }
            if (zellen.size < 5 || zellen[0].isBlank()) return@mapNotNull null
            UebersichtZeile(
                ueberschrift = if (zellen[3].isNotBlank()) zellen[3] else "Buchung",
                untertitel = zellen[0],
                werte = listOfNotNull(
                    zellen[1].takeIf { it.isNotBlank() }?.let { "Eingang" to it },
                    zellen[2].takeIf { it.isNotBlank() }?.let { "Ausgang" to it },
                    "Kontostand" to zellen[4],
                ),
            )
        }
        return SeitenAnsicht(
            titel = TITEL_KONTO,
            abschnitte = listOf(UebersichtAbschnitt(titel = "Kontobewegungen", zeilen = zeilen)),
        )
    }

    /** Steuerübersicht (`steuer.php`): Bargeld/Gesamt-Blöcke als Einzelwerte. */
    internal fun parseSteuer(html: String): SeitenAnsicht {
        val doc = Jsoup.parse(html)
        val abschnitte = mutableListOf<UebersichtAbschnitt>()
        val tabelle = doc.selectFirst("table") ?: return SeitenAnsicht(titel = TITEL_STEUER)
        var block: String? = null
        val zeilen = mutableListOf<Pair<String, String>>()
        fun flushBlock() {
            if (block != null && zeilen.isNotEmpty()) {
                abschnitte += UebersichtAbschnitt(titel = block, infoZeilen = zeilen.toList())
                zeilen.clear()
            }
        }
        tabelle.select("tr").forEach { tr ->
            val b = tr.selectFirst("td b")?.text()?.trim()
            if (b != null) {
                flushBlock()
                block = b
                return@forEach
            }
            val zellen = tr.select("td").map { it.text().trim() }
            if (zellen.size >= 2 && zellen[0].isNotBlank() && zellen[1].isNotBlank()) {
                zeilen += zellen[0].removeSuffix(":").trim() to zellen[1]
            }
        }
        flushBlock()
        return SeitenAnsicht(titel = TITEL_STEUER, abschnitte = abschnitte)
    }

    /** Teamübersicht (`showteam.php?s=0`): Kader-Zeilen als Karten (gleiche Daten wie der Kader-Tab). */
    internal fun parseTeamUebersicht(html: String): SeitenAnsicht {
        val kader = parseKader(html)
        val zeilen = kader.map { spieler ->
            UebersichtZeile(
                ueberschrift = spieler.name,
                untertitel = "${positionsName(spieler.position)} · ${spieler.alter?.let { "$it Jahre" } ?: "Alter k. A."} · Nr. ${spieler.nummer}",
                werte = listOf(
                    "Skill" to spieler.skill.skillText(),
                    "Opti" to spieler.opti.skillText(),
                    "Fit" to spieler.fit.toString(),
                    "Mor" to spieler.mor.toString(),
                ),
            )
        }
        return SeitenAnsicht(
            titel = TITEL_TEAM,
            abschnitte = listOf(UebersichtAbschnitt(titel = "Kader", zeilen = zeilen)),
        )
    }

    private fun Double.skillText(): String = ((this * 10).toInt() / 10.0).toString().replace('.', ',')

    private fun positionsName(position: SpielerPosition): String = when (position) {
        SpielerPosition.TOR -> "Torwart"
        SpielerPosition.ABW -> "Abwehr"
        SpielerPosition.DMI -> "Def. Mittelfeld"
        SpielerPosition.MIT -> "Mittelfeld"
        SpielerPosition.OMI -> "Off. Mittelfeld"
        SpielerPosition.STU -> "Sturm"
        SpielerPosition.AMATEUR -> "Amateur"
    }

    /** Spielerkarte: Kaderzeile + Vertrag + Stärken + Statistik (parallel geladen). */
    suspend fun spielerKarte(pid: Long): SpielerKarte = withContext(Dispatchers.IO) {
        val kader = async { ladeKader().firstOrNull { it.pid == pid } }
        val vertrag = async { ladeVertraege().firstOrNull { it.pid == pid } }
        val staerken = async { ladeStaerken().firstOrNull { it.pid == pid } }
        val statSaison = async { ladeStatistik(gesamt = false).firstOrNull { it.pid == pid } }
        val statGesamt = async { ladeStatistik(gesamt = true).firstOrNull { it.pid == pid } }
        val profil = async { safeGet("${OsApi.BASE_URL}/sp.php?s=$pid") }

        val k = kader.await()
        val profRoh = profil.await().orEmpty()
        val sprofil = parseSpielerProfil(profRoh)
        SpielerKarte(
            pid = pid,
            name = k?.name ?: sprofil.name ?: vertrag.await()?.name ?: staerken.await()?.name ?: "Spieler",
            nummer = k?.nummer.orEmpty(),
            alter = k?.alter ?: sprofil.alter,
            position = k?.position ?: sprofil.position ?: SpielerPosition.AMATEUR,
            kader = k,
            vertrag = vertrag.await() ?: sprofil.vertrag?.copy(name = sprofil.name.orEmpty()),
            staerken = staerken.await()?.werte.orEmpty().ifEmpty { sprofil.staerken },
            statistikSaison = statSaison.await()?.werte.orEmpty().ifEmpty { sprofil.statistikSaison },
            statistikGesamt = statGesamt.await()?.werte.orEmpty().ifEmpty { sprofil.statistikGesamt },
            profilRohtext = parseProfilRohtext(profRoh),
        )
    }

    /** Kader eines fremden Vereins laden (`st.php?c=<id>`); `null` bei Login-/Lade-Fehler. */
    suspend fun ladeFremdenKader(teamId: Long): FremdesTeam? = withContext(Dispatchers.IO) {
        val html = safeGet("${OsApi.BASE_URL}/st.php?c=$teamId") ?: return@withContext null
        val team = parseFremdesTeam(html, teamId)
        if (team.name.isBlank()) null else team
    }

    /** Spielerkarte eines fremden Spielers direkt aus `sp.php` (ohne eigenen Kader). */
    suspend fun spielerKarteFremd(pid: Long, teamId: Long): SpielerKarte = withContext(Dispatchers.IO) {
        val team = async { ladeFremdenKader(teamId) }
        val profil = async { safeGet("${OsApi.BASE_URL}/sp.php?s=$pid") }
        val k = team.await()?.kader?.firstOrNull { it.pid == pid }
        val sprofil = parseSpielerProfil(profil.await().orEmpty())
        SpielerKarte(
            pid = pid,
            name = sprofil.name ?: k?.name ?: "Spieler",
            nummer = k?.nummer.orEmpty(),
            alter = sprofil.alter ?: k?.alter,
            position = sprofil.position ?: k?.position ?: SpielerPosition.AMATEUR,
            kader = k,
            vertrag = sprofil.vertrag?.copy(name = sprofil.name.orEmpty()),
            staerken = sprofil.staerken,
            statistikSaison = sprofil.statistikSaison,
            statistikGesamt = sprofil.statistikGesamt,
        )
    }

    /** Parsiert `st.php?c=<id>`: Vereinsname + Kader (Tabellenstruktur wie `showteam.php?s=0`). */
    internal fun parseFremdesTeam(html: String, teamId: Long): FremdesTeam {
        val kopf = Jsoup.parse(html).select("b").firstOrNull()?.ownText()?.trim().orEmpty()
        val name = kopf.substringBefore(" - ").trim()
        val liga = kopf.substringAfter(" - ").trim()
        return FremdesTeam(
            teamId = teamId,
            name = name,
            liga = liga,
            kader = if (name.isBlank()) emptyList() else parseKader(html),
        )
    }

    internal fun parseVertraege(html: String): List<VertragZeile> {
        val tabelle = com.onlinesoccer.app.core.network.HtmlTools.hauptTabelle(html) ?: return emptyList()
        if (tabelle.header.isEmpty()) return emptyList()
        return tabelle.zeilen.mapNotNull { zeile ->
            val pid = zeile.pid ?: return@mapNotNull null
            VertragZeile(
                pid = pid,
                name = zeile.name ?: "",
                position = zeile.position ?: positionAusZellen(zeile.zellen) ?: SpielerPosition.AMATEUR,
                gehalt = tabelle.wert(zeile, "gehalt"),
                laufzeit = tabelle.wert(zeile, "lauf") ?: tabelle.wert(zeile, "vertrag"),
                marktwert = tabelle.wert(zeile, "marktwert")
                    ?: tabelle.wert(zeile, "spielerwert")
                    ?: tabelle.wert(zeile, "mw"),
                geburtstag = tabelle.wert(zeile, "geburtstag") ?: tabelle.wert(zeile, "geb"),
            )
        }
    }

    internal fun parseStaerken(html: String): List<StaerkeZeile> {
        val tabelle = com.onlinesoccer.app.core.network.HtmlTools.hauptTabelle(html) ?: return emptyList()
        if (tabelle.header.isEmpty()) return emptyList()
        return tabelle.zeilen.mapNotNull { zeile ->
            val pid = zeile.pid ?: return@mapNotNull null
            val werte = buildMap {
                for (ih in tabelle.header.indices) {
                    val kopf = tabelle.header[ih]
                    if (kopf.isBlank() || kopf in STAERKE_META) continue
                    val wert = zeile.zellen.getOrNull(ih)?.takeIf { it.isNotBlank() } ?: continue
                    put(kopf, wert)
                }
            }
            StaerkeZeile(
                pid = pid,
                name = zeile.name ?: "",
                position = zeile.position ?: positionAusZellen(zeile.zellen) ?: SpielerPosition.AMATEUR,
                werte = werte,
            )
        }
    }

    internal fun parseStatistik(html: String): List<StatistikZeile> {
        val doc = Jsoup.parse(html)
        val tabelle = doc.select("table").maxByOrNull { it.select("tr").size } ?: return emptyList()
        val rows = tabelle.select("tr").filter { it.select("td,th").isNotEmpty() }
        if (rows.size < 3) return emptyList()

        // Statistik (`s=3`/`s=4`) hat eine zweizeilige Kopfzeile: Gruppen („Spiele“,
        // „Tore“, …) mit colspan über den Unter-Spalten LI/LP/IP/FS. In der App werden
        // nur die kombinierte Spaltennamen gezeigt (z. B. „Tore/LI“); die Meta-Spalten
        // Name/Land/U werden ausgelassen, da der Spielername separat angezeigt wird.
        val kopfZweizeilig = statistikKopfzeilen(rows[0], rows[1])
        val kopf = kopfZweizeilig ?: rows[0].select("td,th").map { it.text().trim() }

        return rows.drop(if (kopfZweizeilig != null) 2 else 1).mapNotNull { tr ->
            val zellen = tr.select("td").map { it.text().trim() }
            if (zellen.size < 2) return@mapNotNull null
            val link = tr.selectFirst("td a[href*='sp.php'], td a[href*='st.php']")
            val pid = link?.attr("href")?.let {
                Regex("[?&]s=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull()
            } ?: return@mapNotNull null
            val name = link?.text() ?: zellen.firstOrNull().orEmpty()
            val position = if (link != null) {
                val ausKlasse = positionFromClass(link.parent()?.className().orEmpty())
                if (ausKlasse != SpielerPosition.AMATEUR) ausKlasse
                else positionAusZellen(zellen) ?: SpielerPosition.AMATEUR
            } else {
                SpielerPosition.AMATEUR
            }
            val werte = buildMap {
                for (ih in kopf.indices) {
                    val spaltenname = kopf[ih]
                    if (spaltenname.isBlank()) continue
                    val wert = zellen.getOrNull(ih)?.takeIf { it.isNotBlank() } ?: continue
                    put(spaltenname, wert)
                }
            }
            StatistikZeile(pid = pid, name = name, position = position, werte = werte)
        }
    }

    /** Kombiniert Zeile 1 (Gruppen, `colspan`) + Zeile 2 (Unter-Spalten) zu Spaltentiteln. */
    private fun statistikKopfzeilen(gruppenZeile: Element, spaltenZeile: Element): List<String>? {
        val gruppenZellen = gruppenZeile.select("td,th")
        val spaltenZellen = spaltenZeile.select("td,th")
        val istZweizeilig = gruppenZellen.any { (it.attr("colspan").toIntOrNull() ?: 1) > 1 } &&
            spaltenZellen.size > gruppenZellen.size
        if (!istZweizeilig) return null

        fun expand(zellen: org.jsoup.select.Elements): List<String> = zellen.flatMap { zelle ->
            val span = zelle.attr("colspan").toIntOrNull() ?: 1
            List(span) { zelle.text().trim() }
        }
        val gruppen = expand(gruppenZellen)
        val spalten = expand(spaltenZellen)
        val meta = setOf("name", "land", "u")
        return (0 until maxOf(gruppen.size, spalten.size)).map { i ->
            val gruppe = gruppen.getOrElse(i) { "" }
            val spalte = spalten.getOrElse(i) { "" }
            when {
                gruppe.lowercase() in meta || spalte.lowercase() in meta -> ""
                gruppe.isBlank() && spalte.isBlank() -> ""
                gruppe.isBlank() -> spalte
                spalte.isBlank() -> gruppe
                else -> "$gruppe/$spalte"
            }
        }
    }

    /** Nur diese Felder der Teaminfo/Stadion-Seite (`s=5`) werden angezeigt. */
    private val teaminfoErlaubt = setOf(
        "teamname", "stadiongrösse", "stadionname", "sitzplätze", "davon überdacht",
        "stehplätze", "anzeigetafel", "rasenheizung", "summe marktwert",
        "schnitt marktwert", "summe gehalt", "schnitt gehalt",
    )

    internal fun parseTeaminfo(html: String): Teaminfo {
        val doc = Jsoup.parse(html)
        val zeilen = mutableListOf<Pair<String, String>>()
        // Jede Zeile hat bis zu vier Zellen: Label, Wert, Label, Wert. Die Zellen werden
        // paarweise (Label->Wert) kombiniert statt des bisherigen Zusammenklebens.
        doc.select("table").forEach { tabelle ->
            tabelle.select("tr").forEach { tr ->
                val zellen = tr.select("td,th").map { it.text().trim() }.filter { it.isNotEmpty() }
                var i = 0
                while (i < zellen.size) {
                    val label = zellen[i].removeSuffix(":").trim()
                    val wert = zellen.getOrNull(i + 1)?.trim().orEmpty()
                    if (label.isNotEmpty() && wert.isNotEmpty() &&
                        label.lowercase() in teaminfoErlaubt
                    ) {
                        zeilen += label to wert
                    }
                    i += 2
                }
            }
        }
        return Teaminfo(zeilen)
    }

    /**
     * Menüstruktur der „Teaminformationen" (`showteam.php`): die Unterpunkte im
     * Kopfbereich werden dynamisch aus den `a[hspace=20]`-Links gelesen, sodass
     * Änderungen der Website ohne Code-Anpassung übernommen werden.
     */
    internal fun parseTeaminformationenMenu(html: String): List<TeamInfoMenuEintrag> {
        val doc = Jsoup.parse(html)
        val links = doc.select("a[hspace=20]")
        return links.mapNotNull { a ->
            val href = a.attr("href").trim()
            val label = a.text().trim()
            if (href.isEmpty() || label.isEmpty()) return@mapNotNull null
            val showteamS = if (href.startsWith("showteam.php", ignoreCase = true)) {
                Regex("[?&]s=(\\d+)").find(href)?.groupValues?.get(1)
            } else {
                null
            }
            val tabellenplatzTeamId = if (href.contains("tabellenplatz", ignoreCase = true)) {
                Regex("tabellenplatz\\((\\d+)\\)").find(href)?.groupValues?.get(1)?.toLongOrNull()
            } else {
                null
            }
            TeamInfoMenuEintrag(
                label = label,
                path = href,
                showteamS = showteamS,
                tabellenplatzTeamId = tabellenplatzTeamId,
            )
        // „Teamübersicht" (s=0) ist als „Mannschaft" in den Team-Bereich verschoben und
        // „Tabellenplätze" (tabellenplatz(#teamId)) wird nicht mehr im Menü angezeigt.
        }.filterNot { it.showteamS == "0" || it.tabellenplatzTeamId != null }
        .distinctBy { it.label }
    }

    suspend fun ladeTeaminformationenMenu(): List<TeamInfoMenuEintrag> = withContext(Dispatchers.IO) {
        safeGet("${OsApi.BASE_URL}/showteam.php?s=0")?.let { parseTeaminformationenMenu(it) }.orEmpty()
    }

    /** Saisonplan (`showteam.php?s=6`), optional für eine gewählte Saison. */
    suspend fun ladeSaisonplan(saison: Int? = null): SaisonplanDaten = withContext(Dispatchers.IO) {
        val url = if (saison != null) {
            "${OsApi.BASE_URL}/showteam.php?s=6&saison=$saison"
        } else {
            "${OsApi.BASE_URL}/showteam.php?s=6"
        }
        safeGet(url)?.let { parseSaisonplan(it) } ?: SaisonplanDaten()
    }

    internal fun parseSaisonplan(html: String): SaisonplanDaten {
        val doc = Jsoup.parse(html)
        val saisons = doc.select("select[name='saison'] option")
            .mapNotNull { it.attr("value").toIntOrNull() }
            .distinct()
            .sortedDescending()
        val gewaehlte = doc.select("select[name='saison'] option[selected]")
            .firstOrNull()?.attr("value")?.toIntOrNull()
            ?: saisons.firstOrNull()
        val tabelle = doc.select("table").firstOrNull { t ->
            val kopf = t.select("tr").firstOrNull()?.select("td,th")?.map { it.text().trim() }.orEmpty()
            kopf.any { it.equals("Gegner", true) } && kopf.any { it.equals("ZAT", true) }
        } ?: doc.select("table").maxByOrNull { it.select("tr").size }
            ?: return SaisonplanDaten(saisons, gewaehlte, emptyList())
        val kopf = tabelle.select("tr").firstOrNull()?.select("td,th")?.map { it.text().trim() }.orEmpty()
        val iZat = kopf.indexOfFirst { it.equals("ZAT", true) }
        val iArt = kopf.indexOfFirst { it.equals("Spielart", true) }
        val iGegner = kopf.indexOfFirst { it.equals("Gegner", true) }
        val iErgebnis = kopf.indexOfFirst { it.equals("Ergebnis", true) }
        val iBericht = kopf.indexOfFirst { it.equals("Bericht", true) }
        val eintraege = tabelle.select("tr").drop(1).mapNotNull { tr ->
            val zellen = tr.select("td")
            val zat = zellen.getOrNull(iZat)?.text()?.trim().orEmpty()
            if (zat.toIntOrNull() == null) return@mapNotNull null
            val gegner = zellen.getOrNull(iGegner)?.text()?.trim().orEmpty()
            if (gegner.isEmpty()) return@mapNotNull null
            val gegnerTeamId = zellen.getOrNull(iGegner)?.selectFirst("a[href*='st.php']")
                ?.attr("href")?.let { Regex("[?&]c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() }
            val berichtUrl = when {
                iBericht < 0 -> null
                else -> zellen.getOrNull(iBericht)?.selectFirst("a")?.attr("href")?.let { href ->
                    if (href.startsWith("javascript:os_bericht")) {
                        Regex("os_bericht\\(([^)]+)\\)").find(href)?.groupValues?.get(1)
                    } else {
                        null
                    }
                }
            }
            SaisonplanEintrag(
                zat = zat,
                spielart = zellen.getOrNull(iArt)?.text()?.trim().orEmpty(),
                gegner = gegner,
                ergebnis = zellen.getOrNull(iErgebnis)?.text()?.trim().orEmpty(),
                berichtUrl = berichtUrl,
                gegnerTeamId = gegnerTeamId,
            )
        }
        return SaisonplanDaten(saisons, gewaehlte, eintraege)
    }

    /** Vereinshistorie (`showteam.php?s=7`): Spielerentwicklung je ZAT. */
    suspend fun ladeVereinshistorie(): List<VereinshistorieEintrag> = withContext(Dispatchers.IO) {
        safeGet("${OsApi.BASE_URL}/showteam.php?s=7")?.let { parseVereinshistorie(it) }.orEmpty()
    }

    internal fun parseVereinshistorie(html: String): List<VereinshistorieEintrag> {
        val doc = Jsoup.parse(html)
        val tabelle = doc.select("table").firstOrNull { it.text().contains("∑Spieler") }
            ?: doc.select("table").maxByOrNull { it.select("tr").size }
            ?: return emptyList()
        val zeilen = mutableListOf<VereinshistorieEintrag>()
        var header = tabelle.select("tr").firstOrNull()?.select("td,th")?.map { it.text().trim() }.orEmpty()
        if (header.size < 5) {
            val erste = doc.select("tr").firstOrNull { it.select("td,th").size >= 8 }
                ?.select("td,th")?.map { it.text().trim() }.orEmpty()
            if (erste.isNotEmpty()) header = erste
        }
        fun spalte(label: String) = header.indexOfFirst { it.contains(label, ignoreCase = true) }
        val iSaison = spalte("Saison")
        val iZat = spalte("ZAT")
        val iSp = spalte("Spieler")
        val iSkill = spalte("Skill")
        val iOpti = spalte("Opti")
        val iAlter = spalte("Alter")
        val iAvgMw = header.indexOfFirst { it.contains("MW") && !it.contains("∑") && it.contains("∅") }
            .takeIf { it >= 0 }
        val iSumMw = header.indexOfFirst { it.contains("∑MW") || it.contains("Summe MW") }
            .takeIf { it >= 0 }
        val iAvgGehalt = header.indexOfFirst { it.contains("Gehalt") && !it.contains("∑") && it.contains("∅") }
            .takeIf { it >= 0 }
        val iSumGehalt = header.indexOfFirst { it.contains("∑Gehalt") || it.contains("Summe Gehalt") }
            .takeIf { it >= 0 }
        val iManager = spalte("Manager")

        tabelle.select("tr").drop(1).forEach { tr ->
            val zellen = tr.select("td").map { it.text().trim() }
            if (zellen.size < 4) return@forEach
            val saison = zellen.getOrNull(iSaison)?.takeIf { it.isNotBlank() }
                ?: return@forEach
            if (saison == header.firstOrNull() && zellen.all { it.isBlank() }) return@forEach
            zeilen += VereinshistorieEintrag(
                saison = saison,
                zat = zellen.getOrNull(iZat).orEmpty(),
                spielerAnzahl = zellen.getOrNull(iSp).orEmpty(),
                avgSkill = zellen.getOrNull(iSkill).orEmpty(),
                avgOpti = zellen.getOrNull(iOpti).orEmpty(),
                avgAlter = zellen.getOrNull(iAlter).orEmpty(),
                avgMW = iAvgMw?.let { zellen.getOrNull(it) }.orEmpty(),
                sumMW = iSumMw?.let { zellen.getOrNull(it) }.orEmpty(),
                avgGehalt = iAvgGehalt?.let { zellen.getOrNull(it) }.orEmpty(),
                sumGehalt = iSumGehalt?.let { zellen.getOrNull(it) }.orEmpty(),
                manager = zellen.getOrNull(iManager).orEmpty(),
            )
        }
        return zeilen
    }

    /** Transferhistorie (`showteam.php?s=8`): Blöcke je Transfer/VM-Kauf. */
    suspend fun ladeTransferhistorie(): List<TransferhistorieBlock> = withContext(Dispatchers.IO) {
        safeGet("${OsApi.BASE_URL}/showteam.php?s=8")?.let { parseTransferhistorie(it) }.orEmpty()
    }

    internal fun parseTransferhistorie(html: String): List<TransferhistorieBlock> {
        val doc = Jsoup.parse(html)
        return doc.select("table > tbody > tr").mapNotNull { tr ->
            val inner = tr.selectFirst("table[width='100%']") ?: return@mapNotNull null
            val zellen = inner.select("td")
            if (zellen.size < 4) return@mapNotNull null
            val datum = zellen.getOrNull(0)?.text()?.trim().orEmpty()
            val team1 = zellen.getOrNull(1)?.text()?.trim().orEmpty()
            val team2 = zellen.getOrNull(2)?.text()?.trim().orEmpty()
            if (datum.isEmpty() && team1.isEmpty()) return@mapNotNull null
            val team1Id = zellen.getOrNull(1)?.selectFirst("a[href*='st.php']")
                ?.attr("href")?.let { Regex("[?&]c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() }
                ?.takeIf { it > 0 }
            val team2Id = zellen.getOrNull(2)?.selectFirst("a[href*='st.php']")
                ?.attr("href")?.let { Regex("[?&]c=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() }
                ?.takeIf { it > 0 }
            val details = zellen.drop(3).mapNotNull { td ->
                val text = td.text().trim().replace("\u00a0", " ")
                text.takeIf { it.isNotBlank() && it != "&nbsp;" }
            }
            TransferhistorieBlock(
                datum = datum,
                team1 = team1,
                team2 = team2,
                team1Id = team1Id,
                team2Id = team2Id,
                details = details,
            )
        }
    }

    /** Leihhistorie (`showteam.php?s=9`): Tabelle aller Leihen. */
    suspend fun ladeLeihhistorie(): List<LeihhistorieEintrag> = withContext(Dispatchers.IO) {
        safeGet("${OsApi.BASE_URL}/showteam.php?s=9")?.let { parseLeihhistorie(it) }.orEmpty()
    }

    internal fun parseLeihhistorie(html: String): List<LeihhistorieEintrag> {
        val doc = Jsoup.parse(html)
        val tabelle = doc.select("table#leihe").firstOrNull()
            ?: doc.select("table").maxByOrNull { it.select("tr").size }
            ?: return emptyList()
        val kopfTds = tabelle.select("tr").firstOrNull()?.select("td,th")?.toList().orEmpty()

        // Spaltenindex im Body anhand kumulativer colspan (Header „Zahlung/Abrechnung" spannt 2 Spalten).
        fun spaltenIndex(label: String): Int {
            var index = 0
            for (td in kopfTds) {
                if (td.text().trim().contains(label, ignoreCase = true)) return index
                index += td.attr("colspan").takeIf { it.isNotBlank() }?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            }
            return -1
        }

        val iDatum = spaltenIndex("Datum")
        val iSpieler = spaltenIndex("Spieler")
        val iVon = spaltenIndex("Von")
        val iZu = spaltenIndex("Zu")
        val iZahlung = spaltenIndex("Zahlung")
        val iDauer = spaltenIndex("Dauer")
        return tabelle.select("tr").drop(1).mapNotNull { tr ->
            val zellen = tr.select("td").map { it.text().trim() }
            if (zellen.size < 5) return@mapNotNull null
            val datum = zellen.getOrNull(iDatum).orEmpty()
            val spieler = zellen.getOrNull(iSpieler).orEmpty()
            if (datum.isEmpty() && spieler.isEmpty()) return@mapNotNull null
            val pid = tr.selectFirst("a[href*='sp.php']")?.attr("href")
                ?.let { Regex("[?&]s=(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull() }
            val zahlung = if (zellen.getOrNull(iZahlung).isNullOrBlank()) {
                ""
            } else {
                val betrag = zellen.getOrNull(iZahlung).orEmpty()
                val euro = zellen.getOrNull(iZahlung + 1)?.equals("Euro", true) == true
                if (euro) "$betrag Euro" else betrag
            }
            LeihhistorieEintrag(
                datum = datum,
                spieler = spieler,
                spielerPid = pid,
                von = zellen.getOrNull(iVon).orEmpty(),
                zu = zellen.getOrNull(iZu).orEmpty(),
                zahlung = zahlung,
                dauer = zellen.getOrNull(iDauer).orEmpty(),
            )
        }
    }

    /** Saisonhistorie (`showteam.php?s=10`): Liga/Tabelle/Pokal/OSE/OSC je Saison. */
    suspend fun ladeSaisonhistorie(): List<SaisonhistorieEintrag> = withContext(Dispatchers.IO) {
        safeGet("${OsApi.BASE_URL}/showteam.php?s=10")?.let { parseSaisonhistorie(it) }.orEmpty()
    }

    internal fun parseSaisonhistorie(html: String): List<SaisonhistorieEintrag> {
        val doc = Jsoup.parse(html)
        val tabelle = doc.select("table").firstOrNull { t ->
            val kopf = t.select("tr").firstOrNull()?.select("td,th")?.map { it.text().trim() }.orEmpty()
            kopf.any { it.equals("Saison", true) } && kopf.any { it.equals("Liga", true) }
        } ?: doc.select("table").maxByOrNull { it.select("tr").size }
            ?: return emptyList()
        val kopf = tabelle.select("tr").firstOrNull()?.select("td,th")?.map { it.text().trim() }.orEmpty()
        fun spalte(label: String) = kopf.indexOfFirst { it.contains(label, ignoreCase = true) }
        val iSaison = spalte("Saison")
        val iLiga = spalte("Liga")
        val iTabelle = spalte("Tabelle")
        val iPokal = spalte("Pokal")
        val iOse = spalte("OSE")
        val iOsc = spalte("OSC")
        return tabelle.select("tr").drop(1).mapNotNull { tr ->
            val zellen = tr.select("td").map { it.text().trim() }
            if (zellen.size < 4) return@mapNotNull null
            val saison = zellen.getOrNull(iSaison).orEmpty()
            if (saison.isEmpty()) return@mapNotNull null
            SaisonhistorieEintrag(
                saison = saison,
                liga = zellen.getOrNull(iLiga).orEmpty(),
                tabelle = zellen.getOrNull(iTabelle).orEmpty(),
                pokal = zellen.getOrNull(iPokal).orEmpty(),
                ose = zellen.getOrNull(iOse).orEmpty(),
                osc = zellen.getOrNull(iOsc).orEmpty(),
            )
        }
    }

    /** Tabellenplatz-Bild (`tabellenplatz.php?t=<teamId>`) als Byte-Array. */
    suspend fun ladeTabellenplatzBild(teamId: Long): ByteArray? = withContext(Dispatchers.IO) {
        val url = "${OsApi.BASE_URL}/tabellenplatz.php?t=$teamId"
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                response.body?.bytes()
            }
        } catch (e: IOException) {
            null
        }
    }

    internal data class SpielerProfil(
        val name: String?,
        val alter: Int?,
        val position: SpielerPosition?,
        val vertrag: VertragZeile?,
        val staerken: Map<String, String> = emptyMap(),
        val statistikSaison: Map<String, String> = emptyMap(),
        val statistikGesamt: Map<String, String> = emptyMap(),
    )

    /** Spielerprofil aus `sp.php`: Kopf-Daten, Stärken (`td.stats`), Statistik. */
    internal fun parseSpielerProfil(html: String): SpielerProfil {
        val doc = Jsoup.parse(html)

        val felder = mutableMapOf<String, String>()
        doc.select("td.stat").forEach { td ->
            val text = td.text().trim()
            if (!text.endsWith(":")) return@forEach
            val wert = td.nextElementSibling()?.text()?.trim().orEmpty()
            if (wert.isNotEmpty()) felder[text.removeSuffix(":").trim()] = wert
        }

        val staerken = buildMap {
            doc.select("td.stats").forEach { td ->
                val text = td.text().trim()
                val idx = text.lastIndexOf(" :")
                if (idx > 0) {
                    val label = text.substring(0, idx).trim()
                    val wert = text.substring(idx + 2).trim()
                    if (label.isNotEmpty() && wert.isNotEmpty()) put(label, wert)
                }
            }
        }

        var statSaison = emptyMap<String, String>()
        var statGesamt = emptyMap<String, String>()
        doc.select("tr").forEach { tr ->
            val zellen = tr.select("td.stat").map { it.text().trim() }
            zellen.forEachIndexed { i, cell ->
                val saison = Regex("^(.+?)\\s+Saison\\s*:\\s*$").find(cell)
                val karriere = Regex("^(.+?)\\s+Karriere\\s*:\\s*$").find(cell)
                if (saison == null && karriere == null) return@forEachIndexed
                val metric = saison?.groupValues?.get(1) ?: karriere!!.groupValues.get(1)
                val werte = zellen.drop(i + 1)
                val eintraege = listOf("LI", "LP", "IP", "FS").mapIndexedNotNull { j, col ->
                    val w = werte.getOrNull(j)?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
                    "$metric ($col)" to w
                }.toMap()
                if (saison != null) statSaison += eintraege else statGesamt += eintraege
            }
        }

        return SpielerProfil(
            name = felder["Name"],
            alter = felder["Alter"]?.toIntOrNull(),
            position = felder["Stammposition"]?.let { positionFromText(it) },
            vertrag = VertragZeile(
                pid = -1,
                name = felder["Name"].orEmpty(),
                gehalt = felder["Monatsgehalt"],
                laufzeit = felder["Vertragslaufzeit"],
                marktwert = felder["Marktwert"],
                geburtstag = felder["Geburtstag"],
            ),
            staerken = staerken,
            statistikSaison = statSaison,
            statistikGesamt = statGesamt,
        )
    }

    private fun parseProfilRohtext(html: String): List<String> {
        if (html.isBlank()) return emptyList()
        return runCatching {
            Jsoup.parse(html).body()?.text()
                ?.lineSequence()?.map { it.trim() }?.filter { it.isNotEmpty() }?.toList().orEmpty()
        }.getOrElse { emptyList() }
    }

    private fun safeGet(url: String): String? {
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: return null
                // Im Demo-Modus ist die Demo-Ansicht („DemoTeam") die gewünschte Antwort –
                // nur echte Login-Formulare blocken. In der persönlichen Sitzung bleibt die
                // strikte Prüfung (Login ODER Demo-Marker), damit eine abgelaufene Session
                // nicht als eigene Sitzung durchgeht.
                val imDemo = sessionManager?.state?.value == AuthUiState.SignedInDemo
                if (if (imDemo) SessionGuard.isPureLoginView(bytes) else SessionGuard.isLoginView(bytes)) return null
                bytes.toString(Charsets.UTF_8)
            }
        } catch (e: IOException) {
            null
        }
    }

    private companion object {
        const val TITEL_JUGEND = "Jugendteam"
        const val TITEL_JUGENDSCOUT = "Jugendscouting"
        const val TITEL_TRAINING = "Training"
        const val TITEL_TRAINER = "Trainer"
        const val TITEL_VT = "Verträge verlängern"
        const val TITEL_STADION = "Stadionausbau"
        const val TITEL_LAUFENDER_AUSBAU = "Laufender Stadionausbau"
        const val TITEL_KONTO = "Kontoauszug"
        const val TITEL_STEUER = "Steuerübersicht"
        const val TITEL_TEAM = "Teamübersicht"

        /** Nicht-Skill-Spalten der Stärken-Tabelle (`showteam.php?s=2`), die nicht als Wert angezeigt werden. */
        val STAERKE_META = setOf("#", "Name", "Land", "U")
    }
}