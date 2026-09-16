package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.BewertungsZeile
import com.onlinesoccer.app.data.model.ElfSpieler
import com.onlinesoccer.app.data.model.SpielerBewertung
import com.onlinesoccer.app.data.model.SpielerPosition
import java.util.Locale
import kotlin.math.round

/**
 * Kandidat für die Elf des Spieltags, gewonnen aus einem echten Spielbericht.
 *
 * Es werden ausschließlich im Bericht tatsächlich vorhandene Daten verwendet:
 * Einsatz (Startelf/Bank), Einsatzminuten (sofern der Bericht Ein-/Auswechsel
 * nennt), Tore (aus „Neuer Spielstand … (X)"), Karten, verwandelte Elfmeter,
 * Team-Ergebnis und Gegentore. Fehlende Werte bleiben neutral (0 / null) und
 * fließen nicht als Positiv-Wert in die Bewertung ein.
 */
data class ElfKandidat(
    val name: String,
    val verein: String,
    val teamId: Long? = null,
    val position: SpielerPosition,
    /** Spieler-ID des Berichts, falls die Seite sie verlinkt (sonst null). */
    val spielerId: Long? = null,
    /** Spieler stand in der Startelf des Spielberichts. */
    val startelf: Boolean,
    /**
     * Kapitän der Mannschaft („C“-Binde) – nur wenn der Bericht dies eindeutig
     * nennt (z. B. „Name (C)“, „Kapitän: Name“). Ohne Hinweis bleibt false;
     * es wird nichts erfunden.
     */
    val kapitän: Boolean = false,
    /**
     * Einsatzminuten, nur wenn der Bericht Ein-/Auswechslungen nennt
     * (sonst null → Bewertung schätzt die Spielzeit nicht, sondern wertet
     * über die Startelf-Info neutral).
     */
    val minuten: Int? = null,
    val tore: Int = 0,
    val vorlagen: Int = 0,
    val gelbeKarten: Int = 0,
    val roteKarten: Int = 0,
    val sieg: Boolean = false,
    val unentschieden: Boolean = false,
    val niederlage: Boolean = false,
    /** Gegentore des eigenen Teams in diesem Spiel. */
    val gegenTore: Int = 0,
    /**
     * Ob der Endstand des Spiels im Bericht stand (false, wenn unbekannt).
     * Nur bei bekanntem Ergebnis zählen Zu-Null-Spiel und Gegentore; bei
     * fehlendem Ergebnis wird nichts angenommen.
     */
    val hatErgebnis: Boolean = true,
    /** Im Bericht ausdrücklich genannter verwandelter Elfmeter (sonst false). */
    val elfmeter: Boolean = false,
    /** Zweikämpfe / gewonnene Zweikämpfe laut Spielerstatistik (0 = nicht berichtet). */
    val zweikaempfe: Int = 0,
    /** Zweikampfquote in Prozent (ZK-%), 0…100. */
    val zweikampfQuote: Double = 0.0,
    /** Schüsse insgesamt laut Spielerstatistik. */
    val schuesse: Int = 0,
    /** Schüsse aufs Tor laut Spielerstatistik. */
    val aufsTor: Int = 0,
    /** Auffälligkeit: Anzahl Ticker-Erwähnungen des Namens im Bericht. */
    val auffaelligkeit: Int = 0,
    /**
     * Gehaltene Bälle des Torhüters (Schüsse aufs Tor des Gegners minus
     * Gegentore) – nur wenn Spielerstatistik und Endstand vorliegen; sonst null.
     */
    val gehalteneBalle: Int? = null,
) {
    /** Kein eigener Gegentreffer (Zu-Null-Spiel) – nur bei bekanntem Ergebnis. */
    val zuNull: Boolean get() = hatErgebnis && gegenTore == 0

    /** Ob für den Spieler positive oder negative Einzelereignisse vorliegen. */
    internal val hatEreignisse: Boolean
        get() = tore > 0 || vorlagen > 0 || gelbeKarten > 0 || roteKarten > 0
}

/**
 * Zentrale, positionsabhängige Bewertungslogik der Elf des Spieltags.
 *
 * Die Note entsteht ausschließlich aus der in den Spielberichten tatsächlich
 * vorhandenen Information nach der Formel
 *
 *   5,5 Grundbewertung + positive Aktionen + Ergebnis/Spielkontext − negative Aktionen
 *
 * Es gibt **keine** künstlichen Zusatzgrößen: Zweikämpfe, Schüsse, Passquoten,
 * xG, Paraden, Auffälligkeit, Elfmeter-Treffer oder geschätzte Spielanteile
 * fließen bewusst nicht ein – nur Tore, Vorlagen, Ergebnis, Zu-Null-Spiel und
 * Karten sind bewerbar (alle Werte stammen aus dem realen Spielbericht).
 *
 * Die Einsatzminuten sind **kein** Bonus: Ein kurzer Einsatz (z. B. 20 Minuten
 * mit Tor) erhält exakt denselben Torbonus wie eine volle Partie, aber nie
 * einen künstlichen „Vollspielbonus". Die Ermittlung der Einsatzzeit selbst
 * ([effektiveMinuten]) bleibt unverändert bestehen und entscheidet nur noch,
 * ob ein Spieler überhaupt als eingesetzt gilt ([hatEinsatz]) – nicht
 * eingesetzte Bank-Spieler werden von der Elf ausgeschlossen.
 *
 * Aufschlüsselung (transparent, in dieser Reihenfolge):
 *
 *  – Grundbewertung  5,5
 *  – Tore            STU +1,0 · MIT/OMI +1,2 · ABW +1,5 · TW +2,0 (je Tor)
 *  – Vorlagen        +0,6 je Vorlage
 *  – Ergebnis        nur bei bekanntem Endstand: Sieg +0,3 · Unentschieden +0,1 · Niederlage −0,2
 *  – Zu null         nur bei bekanntem Endstand: TW +0,5 · ABW +0,3
 *  – Karten          je Gelb −0,2 · je Rot-Ereignis −1,0 (kein Doppel-Abzug)
 *
 * Die Rohbewertung wird auf eine Nachkommastelle gerundet und auf maximal 10,0
 * begrenzt. Eine 10,0 darf nur entstehen, wenn die **ungerundete** Rohbewertung
 * mindestens 9,8 beträgt – es findet **keine** Normalisierung oder
 * Hochskalierung statt. Auch eine Elf ganz ohne 10,0 ist ausdrücklich in
 * Ordnung. Fehlende Daten (z. B. kein Endstand) erzeugen weder Bonus noch Malus.
 */
object ElfBewertung {

    /** Effektive Einsatzzeit in Minuten (Feldkonstante). */
    private const val VOLLE_SPIELZEIT = 90

    /**
     * Zeilennamen der transparenten Aufschlüsselung – identisch zur Anzeige,
     * damit Grundbewertung/Tore/… direkt mit den Dialogzeilen korrespondieren.
     */
    private const val GRUND = "Grundbewertung"
    private const val TORE = "Tore"
    private const val VORLAGEN = "Vorlagen"
    private const val ERGEBNIS = "Ergebnis"
    private const val ZU_NULL = "Zu null"
    private const val KARTEN = "Karten"

    /** Grundbewertung – identisch für jeden eingesetzten Spieler. */
    private const val GRUNDBEWERTUNG = 5.5

    /** Torbonus je Tor, abhängig von der Position. */
    private const val TOR_STU = 1.0
    private const val TOR_MIT = 1.2
    private const val TOR_ABW = 1.5
    private const val TOR_TW = 2.0

    /** Vorlagen-Bonus je Vorlage. */
    private const val VORLAGE_BONUS = 0.6

    /** Ergebnis-Bonus/-Malus – nur bei tatsächlich vorhandenem Endstand. */
    private const val SIEG_BONUS = 0.3
    private const val UNENTSCHIEDEN_BONUS = 0.1
    private const val NIEDERLAGE_MALUS = 0.2

    /** Zu-Null-Bonus – nur bei tatsächlich vorhandenem Endstand. */
    private const val ZU_NULL_TW = 0.5
    private const val ZU_NULL_ABW = 0.3

    /** Karten-Malus (positiver Betrag). */
    const val GELBE_KARTE_MALUS = 0.2
    /**
     * Malus je Rot-Ereignis. Der Parser wertet „Gelb-Rot"/„zweite Gelbe" ebenso
     * wie die „direkte Rote Karte" als EIN Rot-Ereignis (`roteKarten`), ohne die
     * Spielart zu unterscheiden. Der Malus orientiert sich daher an der üblichen
     * Form Gelb-Rot (Vorgabe −1,0); die strengere Variante „direkte Rote Karte"
     * (−2,0) ließe sich ohne erfundene Schätzung nicht sicher abgrenzen und wird
     * bewusst nicht angesetzt. Ein Rot-Ereignis wird nie zusätzlich als Gelb
     * gewertet (kein Doppel-Abzug, siehe [kartenMalus]).
     */
    const val ROTE_KARTE_MALUS = 1.0

    /** Eine 10,0 setzt eine ungerundete Rohbewertung von mindestens 9,8 voraus. */
    private const val ZEHN_UNTERGRENZE = 9.8
    private const val MAX_NOTE = 10.0

    /**
     * Bewertet einen Kandidaten und liefert die nachvollziehbare Note samt
     * Aufschlüsselung in exakt dieser Reihenfolge: Grundbewertung, Tore,
     * Vorlagen, Ergebnis, Zu null, Karten. Die Summe der sichtbaren Beiträge
     * entspricht der Rohbewertung, aus der die (gerundete) Endnote entsteht.
     */
    fun bewerten(kandidat: ElfKandidat): SpielerBewertung {
        val zeilen = mutableListOf<BewertungsZeile>()
        var summe = 0.0

        // 1. Grundbewertung – immer.
        zeilen += BewertungsZeile(GRUND, GRUNDBEWERTUNG)
        summe += GRUNDBEWERTUNG

        // 2. Tore – positionsabhängig, je Tor aufsummiert.
        val tore = kandidat.tore * torBonus(kandidat.position)
        if (tore != 0.0) {
            zeilen += BewertungsZeile(TORE, tore)
            summe += tore
        }

        // 3. Vorlagen – je Vorlage.
        val vorlagen = kandidat.vorlagen * VORLAGE_BONUS
        if (vorlagen != 0.0) {
            zeilen += BewertungsZeile(VORLAGEN, vorlagen)
            summe += vorlagen
        }

        // 4. Ergebnis – nur bei tatsächlich vorhandenem Endstand.
        val ergebnis = ergebnisBonus(kandidat)
        if (ergebnis != 0.0) {
            zeilen += BewertungsZeile(ERGEBNIS, ergebnis)
            summe += ergebnis
        }

        // 5. Zu null – nur bei tatsächlich vorhandenem Endstand (Torwart/Abwehr).
        val zuNull = zuNullBonus(kandidat)
        if (zuNull != 0.0) {
            zeilen += BewertungsZeile(ZU_NULL, zuNull)
            summe += zuNull
        }

        // 6. Karten – je Gelb −0,2, je Rot-Ereignis −1,0 (kein Doppel-Abzug).
        val karten = kartenMalus(kandidat)
        if (karten != 0.0) {
            zeilen += BewertungsZeile(KARTEN, karten)
            summe += karten
        }

        val gesamt = endnote(summe)
        return SpielerBewertung(gesamt = gesamt, zeilen = zeilen)
    }

    /**
     * DMI/OMI gelten als Mittelfeld – exakt dieselbe Zuordnung wie bei der
     * Formation (siehe [ElfAuswahl]).
     */
    private fun kanonisch(position: SpielerPosition): SpielerPosition = when (position) {
        SpielerPosition.DMI, SpielerPosition.OMI -> SpielerPosition.MIT
        else -> position
    }

    /**
     * Effektive Einsatzminuten. Wenn der Bericht keine Ein-/Auswechslung nennt
     * ([minuten] == null), wird nicht geraten: Startelfspieler werden mit
     * voller Partie gewertet, Einwechselspieler ohne Ereignis mit 0 Minuten
     * (nicht eingesetzt), Einwechselspieler **mit** Ereignis neutral kurz.
     * Diese Ermittlung bleibt unverändert bestehen; die Minuten fließen als
     * **kein** Bonus in die Bewertung ein – sie entscheiden nur, ob ein
     * Spieler als eingesetzt gilt ([hatEinsatz]).
     */
    private fun effektiveMinuten(kandidat: ElfKandidat): Int = when {
        kandidat.minuten != null -> kandidat.minuten
        kandidat.startelf -> VOLLE_SPIELZEIT
        kandidat.hatEreignisse -> 30
        else -> 0
    }

    /** Ob der Spieler nachweislich eingesetzt war (Einsatzzeit > 0 Minuten). */
    internal fun hatEinsatz(kandidat: ElfKandidat): Boolean = effektiveMinuten(kandidat) > 0

    /** Torbonus je Tor, abhängig von der kanonischen Position. */
    private fun torBonus(position: SpielerPosition): Double = when (kanonisch(position)) {
        SpielerPosition.TOR -> TOR_TW
        SpielerPosition.ABW -> TOR_ABW
        SpielerPosition.MIT -> TOR_MIT
        else -> TOR_STU
    }

    /** Ergebnis-Bonus/-Malus – ausschließlich bei tatsächlich vorhandenem Endstand. */
    private fun ergebnisBonus(kandidat: ElfKandidat): Double {
        if (!kandidat.hatErgebnis) return 0.0
        return when {
            kandidat.sieg -> SIEG_BONUS
            kandidat.unentschieden -> UNENTSCHIEDEN_BONUS
            kandidat.niederlage -> -NIEDERLAGE_MALUS
            else -> 0.0
        }
    }

    /**
     * Zu-Null-Bonus (Torwart +0,5, Abwehr +0,3) – ausschließlich bei tatsächlich
     * vorhandenem Endstand und 0 Gegentoren des eigenen Teams.
     */
    private fun zuNullBonus(kandidat: ElfKandidat): Double {
        if (!kandidat.hatErgebnis || !kandidat.zuNull) return 0.0
        return when (kanonisch(kandidat.position)) {
            SpielerPosition.TOR -> ZU_NULL_TW
            SpielerPosition.ABW -> ZU_NULL_ABW
            else -> 0.0
        }
    }

    /**
     * Karten-Malus: je Gelb −0,2, je Rot-Ereignis −1,0. Gelb- und Rot-Ereignisse
     * sind getrennte Zähler; ein einzelnes Rot-Ereignis wird nie zusätzlich als
     * Gelb-Karte abgezogen (kein Doppel-Abzug für dasselbe Ereignis).
     */
    private fun kartenMalus(kandidat: ElfKandidat): Double =
        -(kandidat.gelbeKarten * GELBE_KARTE_MALUS + kandidat.roteKarten * ROTE_KARTE_MALUS)

    /**
     * Endnote: Rohbewertung auf eine Nachkommastelle gerundet und auf die Skala
     * 1,0 … 10,0 geklemmt. Eine 10,0 verlangt eine ungerundete Rohbewertung von
     * mindestens 9,8 – darunter wird niemals auf 10,0 aufgerundet.
     */
    private fun endnote(roh: Double): Double {
        val geklemmt = roh.coerceIn(1.0, MAX_NOTE)
        val gerundet = runde(geklemmt)
        return if (gerundet >= MAX_NOTE && geklemmt < ZEHN_UNTERGRENZE) {
            9.9
        } else {
            gerundet.coerceAtMost(MAX_NOTE)
        }
    }

    /** Rundung auf eine Nachkommastelle (halbe Werte werden aufgerundet). */
    private fun runde(wert: Double): Double = round((wert + 1e-9) * 10.0) / 10.0

    /** Aufbereitung einer Bewertung (eine Nachkommastelle, Komma). */
    fun formatiere(bewertung: Double): String =
        String.format(Locale.GERMANY, "%.1f", bewertung)

    /** Aufbereitung eines Kriteriumsbeitrags: „+1,5“, „-0,2“, „0,0“. */
    fun formatiereBeitrag(beitrag: Double): String {
        val wert = String.format(Locale.GERMANY, "%.1f", kotlin.math.abs(beitrag))
        return when {
            beitrag > 0.0 -> "+$wert"
            beitrag < 0.0 -> "-$wert"
            else -> "0,0"
        }
    }

    /**
     * Rekonstruiert den Kandidaten aus einem [ElfSpieler], damit die gespeicherte
     * (gecachte) Elf dieselbe transparente Bewertungsaufschlüsselung zeigt wie
     * die gerade geladene. Die Note bleibt dadurch jederzeit nachvollziehbar.
     */
    fun kandidatVon(spieler: ElfSpieler): ElfKandidat = ElfKandidat(
        name = spieler.name,
        verein = spieler.verein,
        teamId = spieler.teamId,
        spielerId = spieler.spielerId,
        position = spieler.position,
        startelf = spieler.startelf,
        kapitän = spieler.kapitän,
        minuten = spieler.minuten,
        tore = spieler.tore,
        vorlagen = spieler.vorlagen,
        gelbeKarten = spieler.gelbeKarten,
        roteKarten = spieler.roteKarten,
        sieg = spieler.sieg,
        unentschieden = spieler.unentschieden,
        niederlage = spieler.niederlage,
        gegenTore = spieler.gegenTore,
        hatErgebnis = spieler.hatErgebnis,
        elfmeter = spieler.elfmeter,
        zweikaempfe = spieler.zweikaempfe,
        zweikampfQuote = spieler.zweikampfQuote,
        schuesse = spieler.schuesse,
        aufsTor = spieler.aufsTor,
        auffaelligkeit = spieler.auffaelligkeit,
        gehalteneBalle = spieler.gehalteneBalle,
    )
}

/**
 * Auswahl der Elf mit **dynamischer Formation**: Es werden klassische Formationen
 * (Torwart + Abwehr/Mittelfeld/Sturm) durchgegangen; gewählt wird die Formation,
 * deren bestbewertete Spieler insgesamt die höchste Summe ergeben. Enthält das
 * Kandidatenfeld keine Formation vollständig füllbar (zu wenige Spieler einer
 * Gruppe), wird als Fallback die bestbewerteten elf Spieler genommen
 * ([ElfAufstellung.vollstaendig] == false).
 *
 * **Nicht eingesetzte** Spieler (keine Einsatzzeit im Bericht, `hatEinsatz` ==
 * false) werden von der Elf ausgeschlossen.
 *
 * Es werden je Position die bestbewerteten verfügbaren Spieler gewählt – nicht
 * die elf höchsten Gesamtwerte eines Teams. Die Bewertung selbst wird durch die
 * Auswahl NICHT verändert – ein Spieler mit 8,8 bleibt bei 8,8.
 */
object ElfAuswahl {

    /**
     * Eine klassische Formation: 1 Torwart plus Abwehrlinie, Mittelfeldlinien
     * (DMI = defensives, MIT = zentrales, OMI = offensives Mittelfeld) und
     * Sturm. Die Berichte unterscheiden nur Torwart/Abwehr/Mittelfeld/Sturm;
     * die Mittelfeld-Unterteilungen entstehen rein aus der gewählten Formation
     * für die taktische Darstellung und ändern die Bewertung nicht (DMI/OMI
     * werden in der Bewertung wie MIT behandelt).
     */
    data class Formation(
        val label: String,
        val abwehr: Int,
        val dmi: Int = 0,
        val mit: Int = 0,
        val omi: Int = 0,
        val sturm: Int,
    ) {
        /** Gesamte Mittelfeldstärke (DMI + MIT + OMI). */
        val mittelfeld: Int get() = dmi + mit + omi
    }

    /**
     * Erlaubte klassische Formationen der Bundesliga in bevorzugter Reihenfolge
     * (bei gleicher Gesamtnote gewinnt die frühere): modernes 4-2-3-1 zuerst,
     * dann 4-3-3, 4-4-2, die Raute, 4-1-4-1 sowie die breiten 3er-/5er-Ketten.
     */
    val FORMATIONEN: List<Formation> = listOf(
        Formation("4-2-3-1", abwehr = 4, dmi = 2, omi = 3, sturm = 1),
        Formation("4-3-3", abwehr = 4, mit = 3, sturm = 3),
        Formation("4-4-2", abwehr = 4, mit = 4, sturm = 2),
        Formation("4-4-2 Raute", abwehr = 4, dmi = 1, mit = 2, omi = 1, sturm = 2),
        Formation("4-1-4-1", abwehr = 4, dmi = 1, mit = 4, sturm = 1),
        Formation("3-5-2", abwehr = 3, mit = 5, sturm = 2),
        Formation("3-4-3", abwehr = 3, mit = 4, sturm = 3),
        Formation("4-5-1", abwehr = 4, mit = 5, sturm = 1),
        Formation("5-3-2", abwehr = 5, mit = 3, sturm = 2),
        Formation("5-4-1", abwehr = 5, mit = 4, sturm = 1),
    )

    /** Feldzeile im Spielfeld-Diagramm (0 = Sturm oben … 5 = Torwart unten). */
    fun feldZeile(position: SpielerPosition): Int = when (position) {
        SpielerPosition.STU -> 0
        SpielerPosition.OMI -> 1
        SpielerPosition.MIT -> 2
        SpielerPosition.DMI -> 3
        SpielerPosition.ABW -> 4
        SpielerPosition.TOR -> 5
        else -> 2
    }

    /**
     * Mittelfeld-Rollen einer Formation in Feldreihenfolge (oben → unten):
     * zuerst OMI (offensiv), dann MIT, zuletzt DMI (defensiv). Zusammen mit der
     * Auswahlreihenfolge (bestbewertete zuerst) landen die stärksten Mittelfeld-
     * spieler in der offensivsten Linie – rein darstellend, ohne Bewertungs-
     * wirkung.
     */
    private fun mittelfeldRollen(formation: Formation): List<SpielerPosition> = buildList {
        repeat(formation.omi) { add(SpielerPosition.OMI) }
        repeat(formation.mit) { add(SpielerPosition.MIT) }
        repeat(formation.dmi) { add(SpielerPosition.DMI) }
    }

    /**
     * Das Auswahlergebnis: die maximal elf gewählten Spieler, das Label der
     * gewählten Formation (null im Lücken-Fallback) und ob eine vollständige
     * Formation (ohne Fallback) gebildet werden konnte.
     */
    data class ElfAufstellung(
        val spieler: List<ElfSpieler>,
        val formation: String?,
        val vollstaendig: Boolean,
    )

    /**
     * Erstellt die Elf (max. 11) aus den Kandidaten. Es wird die Formation mit
     * der höchsten Gesamtsumme der bestbewerteten Spieler je Position gewählt.
     */
    fun erstelleElf(kandidaten: List<ElfKandidat>): ElfAufstellung {
        val bewertet = kandidaten
            .filter { it.name.isNotBlank() && ElfBewertung.hatEinsatz(it) }
            .distinctBy { it.teamId to it.name.lowercase() }
            .map { it.copy(position = kanonischePosition(it.position)) }
            .sortedByDescending { ElfBewertung.bewerten(it).gesamt }

        fun ziehe(position: SpielerPosition, anzahl: Int): List<ElfKandidat> {
            val gewaehlt = mutableListOf<ElfKandidat>()
            val namen = mutableSetOf<String>()
            for (kandidat in bewertet) {
                if (gewaehlt.size >= anzahl) break
                if (kandidat.position != position) continue
                if (kandidat.name in namen) continue
                gewaehlt += kandidat
                namen += kandidat.name
            }
            return gewaehlt
        }

        var besteFormation: Formation? = null
        var besteSpieler: List<ElfKandidat> = emptyList()
        var besteSumme = Double.NEGATIVE_INFINITY

        FORMATIONEN.forEach { formation ->
            val tor = ziehe(SpielerPosition.TOR, 1)
            val abwehr = ziehe(SpielerPosition.ABW, formation.abwehr)
            val mittelfeld = ziehe(SpielerPosition.MIT, formation.mittelfeld)
            val sturm = ziehe(SpielerPosition.STU, formation.sturm)
            if (tor.size < 1 || abwehr.size < formation.abwehr ||
                mittelfeld.size < formation.mittelfeld || sturm.size < formation.sturm
            ) {
                return@forEach
            }
            val auswahl = tor + abwehr + mittelfeld + sturm
            val summe = auswahl.sumOf { ElfBewertung.bewerten(it).gesamt }
            if (summe > besteSumme) {
                besteFormation = formation
                besteSpieler = auswahl
                besteSumme = summe
            }
        }

        // Lücken-Fallback: bestbewertete restliche Spieler (ohne Formation).
        val gewaehlt = if (besteFormation != null) {
            besteSpieler
        } else {
            val fallback = mutableListOf<ElfKandidat>()
            val namen = mutableSetOf<String>()
            for (kandidat in bewertet) {
                if (fallback.size >= 11) break
                if (kandidat.name in namen) continue
                fallback += kandidat
                namen += kandidat.name
            }
            fallback
        }

        // Die gewählte Formation bestimmt die Mittelfeld-Unterteilung: Die
        // bestbewerteten Mittelfeldspieler (Auswahlreihenfolge) bekommen die
        // Rollen OMI → MIT → DMI (offensiv → defensiv). Reine Darstellung –
        // die Bewertung bleibt unverändert (DMI/OMI werden wie MIT bewertet).
        val rollen = besteFormation?.let { mittelfeldRollen(it) }.orEmpty().toMutableList()
        val spieler = gewaehlt.take(11).map { kandidat ->
            val position = if (kandidat.position == SpielerPosition.MIT && rollen.isNotEmpty()) {
                rollen.removeAt(0)
            } else {
                kandidat.position
            }
            ElfSpieler(
                name = kandidat.name,
                verein = kandidat.verein,
                position = position,
                bewertung = ElfBewertung.bewerten(kandidat).gesamt,
                teamId = kandidat.teamId,
                spielerId = kandidat.spielerId,
                startelf = kandidat.startelf,
                kapitän = kandidat.kapitän,
                minuten = kandidat.minuten,
                tore = kandidat.tore,
                vorlagen = kandidat.vorlagen,
                gelbeKarten = kandidat.gelbeKarten,
                roteKarten = kandidat.roteKarten,
                sieg = kandidat.sieg,
                unentschieden = kandidat.unentschieden,
                niederlage = kandidat.niederlage,
                gegenTore = kandidat.gegenTore,
                hatErgebnis = kandidat.hatErgebnis,
                elfmeter = kandidat.elfmeter,
                zweikaempfe = kandidat.zweikaempfe,
                zweikampfQuote = kandidat.zweikampfQuote,
                schuesse = kandidat.schuesse,
                aufsTor = kandidat.aufsTor,
                auffaelligkeit = kandidat.auffaelligkeit,
                gehalteneBalle = kandidat.gehalteneBalle,
            )
        }
        return ElfAufstellung(
            spieler = spieler,
            formation = besteFormation?.label,
            vollstaendig = besteFormation != null,
        )
    }

    private fun kanonischePosition(position: SpielerPosition): SpielerPosition = when (position) {
        SpielerPosition.DMI, SpielerPosition.OMI -> SpielerPosition.MIT
        else -> position
    }
}