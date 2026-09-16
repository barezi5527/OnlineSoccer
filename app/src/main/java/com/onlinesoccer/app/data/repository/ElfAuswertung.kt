package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.BerichtEreignis
import com.onlinesoccer.app.data.model.BerichtEreignisTyp
import com.onlinesoccer.app.data.model.BerichtSpielerStatistik
import com.onlinesoccer.app.data.model.SpielBericht
import com.onlinesoccer.app.data.model.SpielerPosition

/**
 * Überführt einen realen Spielbericht in bewertbare Kandidaten.
 *
 * Es werden ausschließlich im Bericht vorhandene Daten genutzt:
 *  - Einsatz: Startelf (T + A..L) vs. Bank
 *  - Einsatzminuten: nur aus ausdrücklichen Ein-/Auswechslungs-Hinweisen im
 *    Ticker („Einwechslung …"/„Auswechslung …"); fehlt der Hinweis, bleibt
 *    die Zeit unbekannt (null) und wird in der Bewertung nicht geraten.
 *  - Tore: „Neuer Spielstand: X:Y (Torschütze)"-Ereignisse – nur wenn der
 *    Torschütze einem gelisteten Spieler zugeordnet werden kann
 *  - Karten: Gelb/Rot-Ereignisse; nur wenn genau ein gelisteter Spieler im
 *    Text vorkommt (sonst keine falsche Zuordnung)
 *  - Vorlagen: nur bei ausdrücklichem Vorlage-Hinweis im Text, gleiche Regel
 *  - Elfmeter: nur wenn ein Tor-Ereignis des Spielers Elfmeter erwähnt
 *  - Ergebnis/Gegentore je Team aus dem Endstand
 * Nicht zuordenbare Namen und fehlende Daten bleiben neutral (0 / null).
 */
object ElfAuswertung {

    private val spielstandRegex = Regex(
        "Neuer\\s+Spielstand:\\s*(\\d+)\\s*:\\s*(\\d+)\\s*\\(([^()]*)\\)",
        RegexOption.IGNORE_CASE,
    )

    private val elfmeterRegex = Regex("elfmeter|11[- ]?meter|strafstoß|penalty", RegexOption.IGNORE_CASE)

    /**
     * Haupt-Wechselformat der realen Berichte: „… wechselt: A kommt für B" –
     * optional mit mehreren Wechseln im selben Ticker-Eintrag.
     */
    private val kommtFuerRegex = Regex(
        "([^,;]{1,80}?)\\s+kommt\\s+für\\s+([^,;]{1,80})",
        RegexOption.IGNORE_CASE,
    )

    private val einwechslungRegex = Regex("einwechslung|eingewechselt|für\\s+ihn\\s+kommt", RegexOption.IGNORE_CASE)
    private val auswechslungRegex = Regex("auswechslung|ausgewechselt|wird\\s+ersetzt", RegexOption.IGNORE_CASE)

    /** Name kurz vor „… kassiert dafür die … Karte" (reale Berichte). */
    private val kassiertRegex = Regex(
        "([^,;.!?]{0,80}?)\\s+kassiert\\s+dafür\\s+die\\s+(?:gelb[- ]?rote|zweite\\s+gelbe|rote|gelbe)\\s+karte",
        RegexOption.IGNORE_CASE,
    )

    fun kandidatenAusBericht(bericht: SpielBericht): List<ElfKandidat> {
        val heimAufstellung = bericht.heimAufstellung
        val gastAufstellung = bericht.gastAufstellung

        // Statische Berichte (`rep/.../<heimId>-<gastId>.html`) tragen die Team-IDs
        // im Dateinamen; die `os_bericht(...)`-IDs sind dort oft null.
        val heimId = bericht.heimId ?: teamIdAusUrl(bericht.url, heim = true)
        val gastId = bericht.gastId ?: teamIdAusUrl(bericht.url, heim = false)

        val bekannteNamen = buildSet {
            heimAufstellung?.spieler?.forEach { add(it.name) }
            gastAufstellung?.spieler?.forEach { add(it.name) }
        }

        val (performHeim, performGast) = ergebnisAufteilung(bericht.ergebnis)
        val (toreProName, vorlagenProName) = torUndVorlagen(bericht.ereignisse, bekannteNamen)
        val kartenProName = kartenSpieler(bericht.ereignisse, bekannteNamen)
        val elfmeterProName = elfmeterSpieler(bericht.ereignisse, bekannteNamen)
        val minutenProName = einsatzMinuten(bericht.ereignisse, bekannteNamen)
        val auffaelligkeitProName = auffaelligkeit(bericht.ereignisse, bekannteNamen)
        val kapitänProName = kapitän(bericht.rohtext.orEmpty(), bekannteNamen)

        // Gehaltene Bälle je Torhüter: Schüsse aufs Tor des Gegners minus Gegentore.
// Nur wenn die Spielerstatistik des Gegners vorliegt und der Endstand bekannt ist.
val gegnerAufsTorHeim = bericht.gastSpielerStatistik.values.sumOf { it.aufsTor }
val gegnerAufsTorGast = bericht.heimSpielerStatistik.values.sumOf { it.aufsTor }

fun gehaltene(opponentenStatistik: Map<String, BerichtSpielerStatistik>, gegnerAufsTor: Int, perform: Performanz?): Int? {
    if (perform == null || opponentenStatistik.isEmpty()) return null
    return (gegnerAufsTor - perform.gegenTore).coerceAtLeast(0)
}

        val kandidaten = mutableListOf<ElfKandidat>()

        heimAufstellung?.spieler?.forEach { spieler ->
            val position = positionVon(spieler.position, spieler.feldzeile) ?: return@forEach
            val nameKey = spieler.name.lowercase()
            val karten = kartenProName[nameKey] ?: (0 to 0)
            val stat = bericht.heimSpielerStatistik[nameKey]
            kandidaten += ElfKandidat(
                name = spieler.name,
                verein = bericht.heim.orEmpty(),
                teamId = heimId,
                position = position,
                spielerId = spieler.spielerId,
                startelf = heimAufstellung.startspieler.any { it.name == spieler.name },
                kapitän = spieler.name.lowercase() in kapitänProName,
                minuten = minutenProName[nameKey],
                tore = toreProName[nameKey] ?: 0,
                vorlagen = vorlagenProName[nameKey] ?: 0,
                gelbeKarten = karten.first,
                roteKarten = karten.second,
                sieg = performHeim?.sieg == true,
                unentschieden = performHeim?.unentschieden == true,
                niederlage = performHeim?.niederlage == true,
                gegenTore = performHeim?.gegenTore ?: 0,
                hatErgebnis = performHeim != null,
                elfmeter = elfmeterProName[nameKey] ?: false,
                zweikaempfe = stat?.zweikaempfe ?: 0,
                zweikampfQuote = stat?.zweikampfQuote ?: 0.0,
                schuesse = stat?.schuesse ?: 0,
                aufsTor = stat?.aufsTor ?: 0,
                auffaelligkeit = auffaelligkeitProName[nameKey] ?: 0,
                gehalteneBalle = if (position == SpielerPosition.TOR) {
                    gehaltene(bericht.gastSpielerStatistik, gegnerAufsTorHeim, performHeim)
                } else {
                    null
                },
            )
        }

        gastAufstellung?.spieler?.forEach { spieler ->
            val position = positionVon(spieler.position, spieler.feldzeile) ?: return@forEach
            val nameKey = spieler.name.lowercase()
            val karten = kartenProName[nameKey] ?: (0 to 0)
            val stat = bericht.gastSpielerStatistik[nameKey]
            kandidaten += ElfKandidat(
                name = spieler.name,
                verein = bericht.gast.orEmpty(),
                teamId = gastId,
                position = position,
                spielerId = spieler.spielerId,
                startelf = gastAufstellung.startspieler.any { it.name == spieler.name },
                kapitän = spieler.name.lowercase() in kapitänProName,
                minuten = minutenProName[nameKey],
                tore = toreProName[nameKey] ?: 0,
                vorlagen = vorlagenProName[nameKey] ?: 0,
                gelbeKarten = karten.first,
                roteKarten = karten.second,
                sieg = performGast?.sieg == true,
                unentschieden = performGast?.unentschieden == true,
                niederlage = performGast?.niederlage == true,
                gegenTore = performGast?.gegenTore ?: 0,
                hatErgebnis = performGast != null,
                elfmeter = elfmeterProName[nameKey] ?: false,
                zweikaempfe = stat?.zweikaempfe ?: 0,
                zweikampfQuote = stat?.zweikampfQuote ?: 0.0,
                schuesse = stat?.schuesse ?: 0,
                aufsTor = stat?.aufsTor ?: 0,
                auffaelligkeit = auffaelligkeitProName[nameKey] ?: 0,
                gehalteneBalle = if (position == SpielerPosition.TOR) {
                    gehaltene(bericht.heimSpielerStatistik, gegnerAufsTorGast, performGast)
                } else {
                    null
                },
            )
        }

        return kandidaten
    }

    /**
     * Team-ID aus dem statischen Berichtspfad `.../<heimId>-<gastId>.html`.
     * Liefert nur dann IDs, wenn der Pfad exakt diesem Aufbau folgt.
     */
    internal fun teamIdAusUrl(url: String, heim: Boolean): Long? {
        val m = Regex("/(\\d+)-(\\d+)\\.html$").find(url) ?: return null
        return m.groupValues[if (heim) 1 else 2].toLongOrNull()
    }

    private data class Performanz(val sieg: Boolean, val unentschieden: Boolean, val niederlage: Boolean, val gegenTore: Int)

    /** Endstand je Team: Sieg/Unentschieden/Niederlage und Gegentore. */
    private fun ergebnisAufteilung(ergebnis: String?): Pair<Performanz?, Performanz?> {
        val match = ergebnis
            ?.takeIf { it.isNotBlank() }
            ?.let { Regex("(\\d+)\\s*[:\\-]\\s*(\\d+)").find(it.trim()) }
            ?: return null to null
        val heim = match.groupValues[1].toIntOrNull() ?: return null to null
        val gast = match.groupValues[2].toIntOrNull() ?: return null to null

        fun performanz(tore: Int, gegnerTore: Int): Performanz {
            val sieg = tore > gegnerTore
            val unentschieden = tore == gegnerTore
            return Performanz(sieg, unentschieden, niederlage = !sieg && !unentschieden, gegenTore = gegnerTore)
        }
        return performanz(heim, gast) to performanz(gast, heim)
    }

    /**
     * Tore **und** Vorlagen je Spielername. Ein Tor-Ereignis gibt den Torschützen
     * in der Klammer an („Neuer Spielstand: X:Y (Torschütze)"); nennt der Bericht
     * im selben Klammerzusatz zusätzlich den Vorlagengeber („… (Torschütze,
     * Vorlage)"), wird diesem der Assist gutgeschrieben. Nur wirklich gelistete
     * Namen werden berücksichtigt – nichts wird geraten.
     */
    private fun torUndVorlagen(
        ereignisse: List<BerichtEreignis>,
        bekannteNamen: Set<String>,
    ): Pair<Map<String, Int>, Map<String, Int>> {
        val tore = mutableMapOf<String, Int>()
        val vorlagen = mutableMapOf<String, Int>()
        ereignisse.forEach { ereignis ->
            spielstandRegex.findAll(ereignis.text).forEach { m ->
                val namen = m.groupValues[3].trim()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                namen.firstOrNull { it in bekannteNamen }?.let { torschuetze ->
                    val schluessel = torschuetze.lowercase()
                    tore[schluessel] = (tore[schluessel] ?: 0) + 1
                }
                namen.drop(1).firstOrNull { it in bekannteNamen }?.let { vorlagengeber ->
                    val schluessel = vorlagengeber.lowercase()
                    vorlagen[schluessel] = (vorlagen[schluessel] ?: 0) + 1
                }
            }
        }
        return tore to vorlagen
    }

    /**
     * Karten je Spielername. Bevorzugt wird der Spieler direkt vor „… kassiert
     * dafür die … Karte" (reale Berichte); alternativ „… Karte für Name".
     * Mehrdeutige Hinweise ohne erkennbaren Spieler bleiben unzugeordnet.
     */
    private fun kartenSpieler(ereignisse: List<BerichtEreignis>, bekannteNamen: Set<String>): Map<String, Pair<Int, Int>> {
        val ergebnis = mutableMapOf<String, MutablePair>()
        ereignisse.forEach { ereignis ->
            val rot = ereignis.typ == BerichtEreignisTyp.ROTE_KARTE
            val gelb = ereignis.typ == BerichtEreignisTyp.GELBE_KARTE
            if (!rot && !gelb) return@forEach
            val name = spielerDerKarte(ereignis.text, bekannteNamen) ?: return@forEach
            val paar = ergebnis.getOrPut(name.lowercase()) { MutablePair() }
            if (rot) paar.rote++ else paar.gelbe++
        }
        return ergebnis.mapValues { it.value.gelbe to it.value.rote }
    }

    private fun spielerDerKarte(text: String, bekannteNamen: Set<String>): String? {
        // Bevorzugt: Name steht unmittelbar vor „kassiert dafür die … Karte".
        kassiertRegex.find(text)?.let { m ->
            val vorher = m.groupValues[1]
            return bekannteNamen
                .map { it to vorher.lastIndexOf(it) }
                .filter { it.second >= 0 }
                .maxByOrNull { it.second }
                ?.first
        }
        // Alternativ: „… Karte für Name" und dann der erste gelistete Name dahinter.
        Regex("karte\\s+für\\s+", RegexOption.IGNORE_CASE).find(text)?.let { m ->
            val danach = text.substring(m.range.last + 1)
            return bekannteNamen.filter { danach.startsWith(it, ignoreCase = true) }.maxByOrNull { it.length }
        }
        // Zuletzt: genau ein gelisteter Name im Hinweis.
        return bekannteNamen.filter { istGenannt(text, it) }.singleOrNull()
    }

    /**
     * Spieler, die nachweislich einen Elfmeter verwandelt haben: Ein Tor-
     * Ereignis, das Elfmeter/Strafstoß/Penalty erwähnt und genau einen
     * bekannten Spieler nennt.
     */
    private fun elfmeterSpieler(ereignisse: List<BerichtEreignis>, bekannteNamen: Set<String>): Map<String, Boolean> {
        val ergebnis = mutableMapOf<String, Boolean>()
        ereignisse.forEach { ereignis ->
            if (!elfmeterRegex.containsMatchIn(ereignis.text)) return@forEach
            val gefundene = bekannteNamen.filter { istGenannt(ereignis.text, it) }
            if (gefundene.size != 1) return@forEach
            ergebnis[gefundene.first().lowercase()] = true
        }
        return ergebnis
    }

    /**
     * Einsatzminuten je Spielername – ausschließlich aus ausdrücklichen Wechsel-
     * Hinweisen des Tickers. Die realen Berichte nennen meist „… wechselt:
     * A kommt für B" – typischerweise auch mehrere Wechsel im selben Eintrag.
     * Klassische Hinweise („Einwechslung: X", „Auswechslung: X", „X wird ersetzt")
     * werden ebenso verstanden. Fehlt ein Hinweis, bleibt der Wert null (keine
     * Schätzung).
     */
    private fun einsatzMinuten(ereignisse: List<BerichtEreignis>, bekannteNamen: Set<String>): Map<String, Int?> {
        val ein = mutableMapOf<String, Int>()
        val aus = mutableMapOf<String, Int>()
        ereignisse.forEach { ereignis ->
            val minute = ereignis.minute?.toIntOrNull() ?: return@forEach

            // Hauptformat: „A kommt für B" – Attributeingang und -ausgang getrennt.
            var attribuiert = false
            kommtFuerRegex.findAll(ereignis.text).forEach { m ->
                val einName = bekannteNamen.filter { istGenannt(m.groupValues[1], it) }.maxByOrNull { it.length }
                val ausName = bekannteNamen.filter { istGenannt(m.groupValues[2], it) }.maxByOrNull { it.length }
                if (einName != null) {
                    val key = einName.lowercase()
                    ein[key] = minOf(ein[key] ?: Int.MAX_VALUE, minute)
                    attribuiert = true
                }
                if (ausName != null) {
                    val key = ausName.lowercase()
                    aus[key] = maxOf(aus[key] ?: 0, minute)
                    attribuiert = true
                }
            }
            if (attribuiert) return@forEach

            // Fallback: einseitige Hinweise („Einwechslung: X", „X wird ersetzt").
            val eingewechselt = einwechslungRegex.containsMatchIn(ereignis.text)
            val ausgewechselt = auswechslungRegex.containsMatchIn(ereignis.text)
            if (!eingewechselt && !ausgewechselt) return@forEach
            val gefundene = bekannteNamen.filter { istGenannt(ereignis.text, it) }
            if (gefundene.size != 1) return@forEach
            val schluessel = gefundene.first().lowercase()
            if (eingewechselt) {
                ein[schluessel] = minOf(ein[schluessel] ?: Int.MAX_VALUE, minute)
            }
            if (ausgewechselt) {
                aus[schluessel] = maxOf(aus[schluessel] ?: 0, minute)
            }
        }

        return bekannteNamen.associate { name ->
            val nameKey = name.lowercase()
            val inMinute = ein[nameKey]
            val outMinute = aus[nameKey]
            val minuten = when {
                inMinute == null && outMinute == null -> null
                inMinute == null -> outMinute // Startelf-Spieler ausgewechselt: spielte bis dahin
                else -> (outMinute ?: 90) - inMinute
            }
            nameKey to minuten?.coerceAtLeast(1)
        }
    }

    /**
     * Auffälligkeit je Spieler: Anzahl der Ticker-Ereignisse, in denen der Name
     * erwähnt wird (Tore, Vorlagen, Wechsel, Karten …) – „wie auffällig man im
     * Spielbericht auftaucht". Nicht erwähnte Spieler bleiben bei 0.
     */
    private fun auffaelligkeit(ereignisse: List<BerichtEreignis>, bekannteNamen: Set<String>): Map<String, Int> {
        val zaehler = mutableMapOf<String, Int>()
        ereignisse.forEach { ereignis ->
            bekannteNamen.forEach { name ->
                if (istGenannt(ereignis.text, name)) {
                    val key = name.lowercase()
                    zaehler[key] = (zaehler[key] ?: 0) + 1
                }
            }
        }
        return zaehler
    }

    /** Prüft, ob ein Spielername als Wort im Text vorkommt (Groß-/Kleinschreibung egal). */
    private fun istGenannt(text: String, name: String): Boolean {
        if (name.isBlank() || name.length < 2) return false
        return text.contains(name, ignoreCase = true)
    }

    /**
     * Kapitäne aus dem Berichtstext – nur eindeutige Hinweise, sonst leer:
     * „Name (C)“, „Name (Kapitän)“ oder „Kapitän: Name“. Fehlt ein solcher
     * Hinweis im Bericht, bleibt der Spieler ohne Binde; es wird nichts
     * erfunden.
     */
    private fun kapitän(rohtext: String, bekannteNamen: Set<String>): Set<String> {
        if (rohtext.isBlank()) return emptySet()
        return bekannteNamen.filter { name ->
            val n = Regex.escape(name)
            Regex("$n\\s*\\(c\\)", RegexOption.IGNORE_CASE).containsMatchIn(rohtext) ||
                Regex("$n\\s*\\(kapitä?n\\)", RegexOption.IGNORE_CASE).containsMatchIn(rohtext) ||
                Regex("kapitä?n\\s*:?\\s*$n\\b", RegexOption.IGNORE_CASE).containsMatchIn(rohtext)
        }.mapTo(mutableSetOf()) { it.lowercase() }
    }

    private class MutablePair(var gelbe: Int = 0, var rote: Int = 0)

    /** Position aus der Rolle im Berichtsraster, Fallback über die Feldzeile. */
    private fun positionVon(position: String?, feldzeile: Int?): SpielerPosition? {
        when (position?.trim()?.lowercase()) {
            "torwart", "tw", "torspieler" -> return SpielerPosition.TOR
            "abwehr", "abwehrspieler" -> return SpielerPosition.ABW
            "mittelfeld", "mittelfeldspieler" -> return SpielerPosition.MIT
            "sturm", "stürmer", "stu", "st" -> return SpielerPosition.STU
        }
        // Fallback über die Raster-Zeile (0 oben … 14 unten / nahe Torwart).
        return when {
            feldzeile == null -> null
            feldzeile <= 3 -> SpielerPosition.STU
            feldzeile <= 8 -> SpielerPosition.MIT
            else -> SpielerPosition.ABW
        }
    }
}