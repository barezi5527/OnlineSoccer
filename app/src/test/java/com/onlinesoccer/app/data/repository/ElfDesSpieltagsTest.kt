package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.BerichtAufstellung
import com.onlinesoccer.app.data.model.BerichtEinstellungen
import com.onlinesoccer.app.data.model.BerichtEreignis
import com.onlinesoccer.app.data.model.BerichtEreignisTyp
import com.onlinesoccer.app.data.model.BerichtSpieler
import com.onlinesoccer.app.data.model.ElfErgebnis
import com.onlinesoccer.app.data.model.ElfSpieler
import com.onlinesoccer.app.data.model.SpielBericht
import com.onlinesoccer.app.data.model.SpielerPosition
import kotlin.math.round
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ElfDesSpieltagsTest {

    private fun kandidat(
        name: String,
        verein: String,
        position: SpielerPosition,
        startelf: Boolean = true,
        minuten: Int? = null,
        tore: Int = 0,
        vorlagen: Int = 0,
        gelbeKarten: Int = 0,
        roteKarten: Int = 0,
        sieg: Boolean = false,
        unentschieden: Boolean = false,
        niederlage: Boolean = false,
        gegenTore: Int = 0,
        hatErgebnis: Boolean = true,
        elfmeter: Boolean = false,
        spielerId: Long? = null,
        zweikaempfe: Int = 0,
        zweikampfQuote: Double = 0.0,
        schuesse: Int = 0,
        aufsTor: Int = 0,
        auffaelligkeit: Int = 0,
        gehalteneBalle: Int? = null,
        berichtNote: Double? = null,
    ) = ElfKandidat(
        name = name,
        verein = verein,
        teamId = 1,
        position = position,
        spielerId = spielerId,
        startelf = startelf,
        minuten = minuten,
        tore = tore,
        vorlagen = vorlagen,
        gelbeKarten = gelbeKarten,
        roteKarten = roteKarten,
        sieg = sieg,
        unentschieden = unentschieden,
        niederlage = niederlage,
        gegenTore = gegenTore,
        hatErgebnis = hatErgebnis,
        elfmeter = elfmeter,
        zweikaempfe = zweikaempfe,
        zweikampfQuote = zweikampfQuote,
        schuesse = schuesse,
        aufsTor = aufsTor,
        auffaelligkeit = auffaelligkeit,
        gehalteneBalle = gehalteneBalle,
        berichtNote = berichtNote,
    )

    private fun note(kandidat: ElfKandidat): Double = ElfBewertung.bewerten(kandidat).gesamt

    // ---------- ElfBewertung ----------

    @Test
    fun bewertung_stuermerZweiToreUndSieg() {
        val kandidat = kandidat("A", "V", SpielerPosition.STU, tore = 2, sieg = true)
        assertEquals(6.9, note(kandidat), 0.001)
    }

    @Test
    fun bewertung_torwartZuNullBekommtZuNullBonus() {
        val torwart = kandidat("TW", "V", SpielerPosition.TOR, sieg = true, gegenTore = 0)
        assertEquals(6.1, note(torwart), 0.001)
    }

    @Test
    fun bewertung_mittelfeldToreWerdenStaerkerGewichtet() {
        val mit = kandidat("MT", "V", SpielerPosition.MIT, sieg = true, tore = 1)
        val stu = kandidat("ST", "V", SpielerPosition.STU, sieg = true, tore = 1)
        assertTrue(note(mit) > note(stu))
    }

    @Test
    fun bewertung_kartenUndNiederlageMindern() {
        val kandidat = kandidat(
            "X", "V", SpielerPosition.ABW, niederlage = true, gelbeKarten = 1, gegenTore = 2,
        )
        assertEquals(5.1, note(kandidat), 0.001)
    }

    @Test
    fun bewertung_bleibtImKickertypischenRahmen() {
        // Schwächste denkbare Note: Niederlage + 4 Gelbe + 3 Rote.
        val min = kandidat(
            "MIN", "V", SpielerPosition.STU, niederlage = true,
            gelbeKarten = 4, roteKarten = 3,
        )
        assertEquals(1.5, note(min), 0.001)

        // Stärkste theoretisch erreichbare Note: alle Kategorie-Caps ausgeschöpft.
        // Roh 10,1 → geklemmt auf 10,0; der 9,95-Gate ist erfüllt (keine Untergrenze).
        val max = kandidat(
            "MAX", "V", SpielerPosition.ABW,
            tore = 2, vorlagen = 2, elfmeter = true, sieg = true, gegenTore = 0,
            schuesse = 7, aufsTor = 6, auffaelligkeit = 6,
            zweikaempfe = 30, zweikampfQuote = 70.0, berichtNote = 1.0,
        )
        assertEquals(10.0, note(max), 0.001)
        assertTrue(note(max) <= 10.0)
    }

    @Test
    fun bewertung_nichtEingesetzterBankSpielerWirdAusgeschlossen() {
        val eingesetzter = kandidat("A", "V", SpielerPosition.STU)
        val bank = kandidat("B", "V", SpielerPosition.STU, startelf = false)

        assertTrue(ElfBewertung.hatEinsatz(eingesetzter))
        assertFalse(ElfBewertung.hatEinsatz(bank))

        // Nicht eingesetzte Bank-Spieler tauchen in der Elf nicht auf.
        val elf = ElfAuswahl.erstelleElf(listOf(eingesetzter, bank)).spieler
        assertTrue(elf.any { it.name == "A" })
        assertFalse(elf.any { it.name == "B" })
    }

    @Test
    fun bewertung_formatiertMitDeutschemKomma() {
        assertEquals("8,6", ElfBewertung.formatiere(8.6))
        assertEquals("7,0", ElfBewertung.formatiere(7.0))
    }

    // ---------- Bewertungsformel: Basis + K1 + K2 + K3 + K4 + K5 − Karten ----------

    @Test
    fun formel_stuermerDreiToreUndVorlageMitSieg() {
        val kandidat = kandidat("A", "V", SpielerPosition.STU, tore = 3, vorlagen = 1, sieg = true)
        assertEquals(7.7, note(kandidat), 0.001)
    }

    @Test
    fun formel_spielerEinTorUndDreiVorlagenMitSieg() {
        val kandidat = kandidat("B", "V", SpielerPosition.STU, tore = 1, vorlagen = 3, sieg = true)
        assertEquals(7.4, note(kandidat), 0.001)
    }

    @Test
    fun formel_torStufenBisHattrick() {
        // K1: 1 Tor positionsabhängig (STU 0,5), 2 Tore gedeckelt (1,0), 3 Tore
        // bricht den Deckel (1,5), 4+ Tore gedeckelt bei 2,0 (positionsunabhängig).
        assertEquals(5.5 + 0.5, note(kandidat("C", "V", SpielerPosition.STU, tore = 1)), 0.001)
        assertEquals(5.5 + 1.0, note(kandidat("C", "V", SpielerPosition.STU, tore = 2)), 0.001)
        assertEquals(5.5 + 1.5, note(kandidat("C", "V", SpielerPosition.STU, tore = 3)), 0.001)
        assertEquals(5.5 + 2.0, note(kandidat("C", "V", SpielerPosition.STU, tore = 4)), 0.001)
    }

    @Test
    fun formel_vorlagenZaehlenBisZurViertenVorlage() {
        // K1-Vorlagen: +0,35 je, unter der 4. Vorlage noch nicht gedeckelt
        // (max. 1,4): 1=0,35 · 2=0,7 · 3=1,05 · 4=1,4 · 5=1,4 (Deckel).
        assertEquals(5.9, note(kandidat("D", "V", SpielerPosition.STU, vorlagen = 1)), 0.001)
        assertEquals(6.2, note(kandidat("D", "V", SpielerPosition.STU, vorlagen = 2)), 0.001)
        assertEquals(6.6, note(kandidat("D", "V", SpielerPosition.STU, vorlagen = 3)), 0.001)
        assertEquals(6.9, note(kandidat("D", "V", SpielerPosition.STU, vorlagen = 4)), 0.001)
        assertEquals(6.9, note(kandidat("D", "V", SpielerPosition.STU, vorlagen = 5)), 0.001)
    }

    @Test
    fun formel_torBonusHaengtVonDerPositionAb() {
        assertEquals(5.5 + 0.5, note(kandidat("ST", "V", SpielerPosition.STU, tore = 1)), 0.001)
        assertEquals(5.5 + 0.6, note(kandidat("MIT", "V", SpielerPosition.MIT, tore = 1)), 0.001)
        assertEquals(5.5 + 0.6, note(kandidat("OMI", "V", SpielerPosition.OMI, tore = 1)), 0.001)
        assertEquals(5.5 + 0.6, note(kandidat("DMI", "V", SpielerPosition.DMI, tore = 1)), 0.001)
        // Gegen-1-Tor, damit der Zu-Null-Bonus (ABW/TW) den Torbonus nicht überlagert.
        assertEquals(5.5 + 0.8, note(kandidat("ABW", "V", SpielerPosition.ABW, tore = 1, gegenTore = 1)), 0.001)
        assertEquals(5.5 + 1.0, note(kandidat("TW", "V", SpielerPosition.TOR, tore = 1, gegenTore = 1)), 0.001)
    }

    @Test
    fun formel_siegUnentschiedenNiederlage() {
        assertEquals(5.9, note(kandidat("S", "V", SpielerPosition.STU, sieg = true)), 0.001)
        assertEquals(5.6, note(kandidat("U", "V", SpielerPosition.STU, unentschieden = true)), 0.001)
        assertEquals(5.3, note(kandidat("N", "V", SpielerPosition.STU, niederlage = true)), 0.001)
    }

    @Test
    fun formel_zuNullNurFuerTorwartUndAbwehr() {
        assertEquals(5.8, note(kandidat("TW", "V", SpielerPosition.TOR, gegenTore = 0)), 0.001)
        assertEquals(5.7, note(kandidat("ABW", "V", SpielerPosition.ABW, gegenTore = 0)), 0.001)
        // Mittelfeld und Sturm erhalten keinen Zu-Null-Bonus.
        assertEquals(5.5, note(kandidat("MIT", "V", SpielerPosition.MIT, gegenTore = 0)), 0.001)
        assertEquals(5.5, note(kandidat("ST", "V", SpielerPosition.STU, gegenTore = 0)), 0.001)
    }

    @Test
    fun formel_fehlendesErgebnisGibtWederBonusNochMalus() {
        // Auch mit gesetztem Flag: ohne Endstand im Bericht gibt es keinen Bonus.
        assertEquals(5.5, note(kandidat("S", "V", SpielerPosition.STU, sieg = true, hatErgebnis = false)), 0.001)
        assertEquals(5.5, note(kandidat("U", "V", SpielerPosition.STU, unentschieden = true, hatErgebnis = false)), 0.001)
        assertEquals(5.5, note(kandidat("N", "V", SpielerPosition.STU, niederlage = true, hatErgebnis = false)), 0.001)
        // Auch kein Zu-Null-Bonus ohne Endstand.
        assertEquals(5.5, note(kandidat("TW", "V", SpielerPosition.TOR, gegenTore = 0, hatErgebnis = false)), 0.001)
    }

    @Test
    fun formel_gelbeKarteMindert() {
        assertEquals(5.3, note(kandidat("G", "V", SpielerPosition.STU, gelbeKarten = 1)), 0.001)
        assertEquals(5.1, note(kandidat("G2", "V", SpielerPosition.STU, gelbeKarten = 2)), 0.001)
        // Karten-Malus gilt unabhängig von der Basis (auch für Einwechsler).
        assertEquals(3.8, note(kandidat("G3", "V", SpielerPosition.STU, startelf = false, gelbeKarten = 1)), 0.001)
    }

    @Test
    fun formel_rotEreignisMindertMitEinemPunktOhneDoppelGelb() {
        // Ein Rot-Ereignis (Gelb-Rot bzw. Rote Karte) ist exakt −1,0 – dafür
        // wird kein zusätzlicher −0,2-Gelb-Abzug vorgenommen.
        assertEquals(4.5, note(kandidat("R", "V", SpielerPosition.STU, roteKarten = 1)), 0.001)
        // Zwei unabhängige Rot-Ereignisse addieren sich.
        assertEquals(3.5, note(kandidat("R2", "V", SpielerPosition.STU, roteKarten = 2)), 0.001)
    }

    @Test
    fun formel_rotUndGelbWerdenNurAlsGetrennteEreignisseAddiert() {
        // Ein separates Gelb (anderes Foul) + ein Rot-Ereignis zählen getrennt.
        assertEquals(4.3, note(kandidat("RG", "V", SpielerPosition.STU, gelbeKarten = 1, roteKarten = 1)), 0.001)
    }

    @Test
    fun formel_zehnBleibtAussergewoehnlicherSonderfall() {
        // Selbst bei maximalem Ausschöpfen fast aller Caps bleibt die Note unter
        // 10,0 – reale Berichte liefern keine Bericht-Note (K5 = 0), und eine
        // 10,0 erfordert eine ungerundete Rohnote von mindestens 9,95.
        val monster = kandidat(
            "MONSTER", "V", SpielerPosition.ABW,
            tore = 2, vorlagen = 2, elfmeter = true, sieg = true, gegenTore = 0,
            schuesse = 7, aufsTor = 6, auffaelligkeit = 6,
            zweikaempfe = 30, zweikampfQuote = 70.0, berichtNote = null,
        )
        assertEquals(9.9, note(monster), 0.001)
        assertTrue(note(monster) < 10.0)
    }

    @Test
    fun formel_extremLeistungWirdUeberDieDeckelGedeckelt() {
        // 10 Tore geben 2,0 (4+-Stufe), 5 Vorlagen 0,7 → K1-Cap 2,0: 5,5+2,0+0,35 = 7,85 → 7,9.
        val mega = kandidat("MEGA", "V", SpielerPosition.STU, tore = 10, vorlagen = 5, sieg = true)
        assertEquals(7.9, note(mega), 0.001)
    }

    @Test
    fun stresstest_kanterspielBleibtImKickertypischenRahmenUndOrdnetKorrekt() {
        // Simulierter Kantersieg 7:0 mit mehreren herausragenden Einzelleistungen:
        // Die Deckel müssen beide >9,5-Spieler-Paare verhindern, die Reihenfolge
        // darf nicht kippen und ohne Bericht-Note darf niemand eine 10,0 erreichen.
        val torjäger = kandidat(
            "TORJÄGER", "KAN", SpielerPosition.STU,
            tore = 4, vorlagen = 1, schuesse = 9, aufsTor = 6, auffaelligkeit = 6,
            zweikaempfe = 22, zweikampfQuote = 62.0, sieg = true, gegenTore = 0,
        )
        val assistMaschine = kandidat(
            "ASSIST", "KAN", SpielerPosition.MIT,
            vorlagen = 4, schuesse = 3, aufsTor = 2, auffaelligkeit = 5,
            zweikaempfe = 30, zweikampfQuote = 58.0, sieg = true, gegenTore = 0,
        )
        val soliderStürmer = kandidat(
            "SOLIDE", "KAN", SpielerPosition.STU,
            tore = 1, vorlagen = 1, schuesse = 3, aufsTor = 2, auffaelligkeit = 2,
            zweikaempfe = 8, zweikampfQuote = 50.0, sieg = true, gegenTore = 0,
        )
        val aufräumer = kandidat(
            "AUFRÄUMER", "KAN", SpielerPosition.ABW,
            vorlagen = 1, auffaelligkeit = 3, zweikaempfe = 14, zweikampfQuote = 68.0,
            sieg = true, gegenTore = 0,
        )
        val keeper = kandidat(
            "KEEPER", "KAN", SpielerPosition.TOR,
            gehalteneBalle = 6, sieg = true, gegenTore = 0,
        )

        // Einzelwerte: Torjäger 9,6, Assist 8,7, Solide 7,7, Aufräumer 7,2, Keeper 6,6.
        assertEquals(9.6, note(torjäger), 0.001)
        assertEquals(8.7, note(assistMaschine), 0.001)
        assertEquals(7.7, note(soliderStürmer), 0.001)
        assertEquals(7.2, note(aufräumer), 0.001)
        assertEquals(6.6, note(keeper), 0.001)

        // Reihenfolge muss der Leistung folgen: Torjäger vor Assist vor Solide
        // vor Aufräumer vor Keeper – trotz Kanterspiel.
        val noten = listOf(
            note(torjäger), note(assistMaschine), note(soliderStürmer),
            note(aufräumer), note(keeper),
        )
        assertEquals(noten.sortedDescending(), noten)

        // Selbst im Kanterspiel bleibt die ganze Elf unter 9,95 → keine 10,0,
        // solange der Bericht keine Note (K5) liefert.
        val elf = ElfAuswahl.erstelleElf(
            listOf(torjäger, assistMaschine, soliderStürmer, aufräumer, keeper)
        ).spieler
        assertTrue("Top-Spieler muss an der Spitze stehen", elf.first().name == "TORJÄGER")
        assertTrue("Elf bleibt unter der 10,0-Schwelle", elf.maxOf { it.bewertung } < 9.95)
    }

    @Test
    fun formel_rundungsUndFormatBeispiele() {
        // Rundungsbeispiele der Vorgabe (eine Nachkommastelle).
        assertEquals("9,7", ElfBewertung.formatiere(9.65))
        assertEquals("9,8", ElfBewertung.formatiere(9.79))
        assertEquals("9,8", ElfBewertung.formatiere(9.80))
        assertEquals("10,0", ElfBewertung.formatiere(9.95))
    }

    @Test
    fun formel_reproduzierbarBeiIdentischenDaten() {
        val a = kandidat("X", "V", SpielerPosition.STU, tore = 2, vorlagen = 1, sieg = true, gelbeKarten = 1)
        val b = kandidat("X", "V", SpielerPosition.STU, tore = 2, vorlagen = 1, sieg = true, gelbeKarten = 1)

        assertEquals(note(a), note(b), 0.001)
        assertEquals(ElfBewertung.bewerten(a).zeilen, ElfBewertung.bewerten(b).zeilen)
    }

    @Test
    fun formel_elfKannGanzOhneZehnExistieren() {
        val kandidaten = buildList {
            add(kandidat("TW", "TW", SpielerPosition.TOR, sieg = true))
            (1..4).forEach { add(kandidat("ABW$it", "ABW", SpielerPosition.ABW, sieg = true)) }
            (1..3).forEach { add(kandidat("MIT$it", "MIT", SpielerPosition.MIT, sieg = true)) }
            (1..3).forEach { add(kandidat("STU$it", "STU", SpielerPosition.STU, tore = if (it <= 1) 2 else 1, sieg = true)) }
        }
        val elf = ElfAuswahl.erstelleElf(kandidaten).spieler

        assertEquals(11, elf.size)
        assertTrue(elf.none { it.bewertung == 10.0 })
    }

    // ---------- Transparenz der Bewertung ----------

    @Test
    fun bewertung_liefertNachvollziehbareZeilen() {
        val kandidat = kandidat("STERN", "FC", SpielerPosition.STU, tore = 2, sieg = true)
        val result = ElfBewertung.bewerten(kandidat)

        val zeilen = result.zeilen.associate { it.kriterium to it.beitrag }
        assertEquals(5.5, zeilen["Grundbewertung"] ?: 0.0, 0.001)
        assertEquals(1.0, zeilen["Direkter Impact"] ?: 0.0, 0.001)
        assertEquals(0.35, zeilen["Ergebnis & Teambonus"] ?: 0.0, 0.001)

        // Keine versteckten Faktoren – weder Einsatzzeit noch alte Einzelzeilen.
        assertNull(zeilen["Einsatzzeit"])
        assertNull(zeilen["Tore"])
        assertNull(zeilen["Vorlagen"])

        // Die Summe der sichtbaren Einzelbestandteile ist die ungerundete
        // Rohbewertung; die Endnote ist deren Rundung auf eine Nachkommastelle.
        val summe = result.zeilen.sumOf { it.beitrag }
        assertEquals(6.85, summe, 0.00001)
        assertEquals(round((summe + 1e-9) * 10.0) / 10.0, result.gesamt, 0.00001)
        assertEquals(6.9, result.gesamt, 0.001)
    }

    @Test
    fun bewertung_fehlendeKriterienErzeugenKeineZeilen() {
        val kandidat = kandidat("RUHIG", "FC", SpielerPosition.ABW)
        val zeilen = ElfBewertung.bewerten(kandidat).zeilen

        assertFalse(zeilen.any { it.kriterium == "Direkter Impact" })
        assertFalse(zeilen.any { it.kriterium == "Effizienz" })
        assertFalse(zeilen.any { it.kriterium == "Zweikämpfe" })
        assertFalse(zeilen.any { it.kriterium == "Bericht-Note" })
        assertFalse(zeilen.any { it.kriterium == "Karten" })
        assertTrue(zeilen.any { it.kriterium == "Grundbewertung" })
    }

    @Test
    fun bewertung_elfmeterErhoehtDenDirektenImpact() {
        val kandidat = kandidat("ELFM", "FC", SpielerPosition.STU, tore = 1, vorlagen = 1, elfmeter = true, sieg = true)
        val ohneElfmeter = kandidat("ELFM", "FC", SpielerPosition.STU, tore = 1, vorlagen = 1, sieg = true)

        // Ein (im Bericht erwähnter) verwandelter Elfmeter gibt +0,2 in K1.
        assertEquals(6.9, note(kandidat), 0.001)
        assertEquals(6.7, note(ohneElfmeter), 0.001)
        assertTrue(note(kandidat) > note(ohneElfmeter))
    }

    @Test
    fun bewertung_zweikaempfeUndEffizienzSindKriterien() {
        val mitStats = kandidat(
            "MACHER", "FC", SpielerPosition.STU,
            sieg = true,
            zweikaempfe = 10, zweikampfQuote = 50.0,
            schuesse = 6, aufsTor = 4, auffaelligkeit = 5, gehalteneBalle = 3,
        )
        val ohneStats = kandidat("MACHER", "FC", SpielerPosition.STU, sieg = true)

        val zeilen = ElfBewertung.bewerten(mitStats).zeilen.associate { it.kriterium to it.beitrag }
        // K2: Quote 66,7 % → +0,4 · aufs Tor 4 → +0,2 · 5 Nennungen → +0,2 = 0,8.
        assertEquals(0.8, zeilen["Effizienz"] ?: 0.0, 0.001)
        // K3: 10 gewonnene ZK × 0,03 = 0,3 · Quote 50 % → +0,15 = 0,45.
        assertEquals(0.45, zeilen["Zweikämpfe"] ?: 0.0, 0.001)

        // Ohne Statistik bleiben K2/K3 neutral – die Note fällt niedriger aus.
        assertTrue(note(mitStats) > note(ohneStats))
        assertEquals(7.1, note(mitStats), 0.001)
        assertEquals(5.9, note(ohneStats), 0.001)
    }

    @Test
    fun formel_dokumentiertesBeispielAStuermerDesSpieltags() {
        // Referenzrechnung aus der Gewichtungs-Spezifikation (Beispiel A):
        // Sieg, 2 Tore, 1 Vorlage, 5 Schüsse/3 aufs Tor (60 %), 3 Nennungen,
        // 10 ZK/63 %, Bericht-Note 2,0 → K1 1,35 · K2 0,60 · K3 0,50 · K4 0,35 ·
        // K5 0,25 → 5,5 + 3,05 = 8,6.
        val kandidat = kandidat(
            "STUERMER", "FC", SpielerPosition.STU,
            tore = 2, vorlagen = 1, sieg = true,
            schuesse = 5, aufsTor = 3, auffaelligkeit = 3,
            zweikaempfe = 10, zweikampfQuote = 63.0,
            berichtNote = 2.0,
        )
        val zeilen = ElfBewertung.bewerten(kandidat).zeilen.associate { it.kriterium to it.beitrag }
        assertEquals(1.35, zeilen["Direkter Impact"] ?: 0.0, 0.001)
        assertEquals(0.6, zeilen["Effizienz"] ?: 0.0, 0.001)
        assertEquals(0.5, zeilen["Zweikämpfe"] ?: 0.0, 0.001)
        assertEquals(0.35, zeilen["Ergebnis & Teambonus"] ?: 0.0, 0.001)
        assertEquals(0.25, zeilen["Bericht-Note"] ?: 0.0, 0.001)
        assertEquals(8.6, note(kandidat), 0.001)
    }

    @Test
    fun bewertung_zweikampfDeckelBegrenztDenK3Beitrag() {
        // 2.000 ZK @ 100 %: Quantität deckelt bei 0,6, Quote bei 0,3 → K3 = 0,9 (K3-Budget).
        val monster = kandidat(
            "ZK-MONSTER", "FC", SpielerPosition.MIT,
            zweikaempfe = 2000, zweikampfQuote = 100.0,
        )
        val zeilen = ElfBewertung.bewerten(monster).zeilen.associate { it.kriterium to it.beitrag }
        assertEquals(0.9, zeilen["Zweikämpfe"] ?: 0.0, 0.001)
        assertEquals(6.4, note(monster), 0.001)
    }

    @Test
    fun bewertung_ohneZweikampfStatistikKeinK3Bonus() {
        val kandidat = kandidat("OHNE-ZK", "FC", SpielerPosition.MIT, zweikaempfe = 0, zweikampfQuote = 70.0)
        val zeilen = ElfBewertung.bewerten(kandidat).zeilen
        assertFalse(zeilen.any { it.kriterium == "Zweikämpfe" })
        assertEquals(5.5, note(kandidat), 0.001)
    }

    @Test
    fun bewertung_berichtNoteWirktAufK5() {
        // Stürmer-Sieg als Basis: 5,5 + 0,35 = 5,85.
        assertEquals(6.1, note(kandidat("N1", "FC", SpielerPosition.STU, sieg = true, berichtNote = 2.0)), 0.001)
        assertEquals(5.7, note(kandidat("N2", "FC", SpielerPosition.STU, sieg = true, berichtNote = 6.0)), 0.001)
        // 4,5 ergibt +0,0 und erzeugt keine Bericht-Note-Zeile.
        val neutral = ElfBewertung.bewerten(kandidat("N3", "FC", SpielerPosition.STU, sieg = true, berichtNote = 4.5))
        assertEquals(5.9, neutral.gesamt, 0.001)
        assertFalse(neutral.zeilen.any { it.kriterium == "Bericht-Note" })
        // Ohne Bericht-Note bleibt K5 neutral.
        val ohne = ElfBewertung.bewerten(kandidat("N4", "FC", SpielerPosition.STU, sieg = true, berichtNote = null))
        assertEquals(5.9, ohne.gesamt, 0.001)
        assertFalse(ohne.zeilen.any { it.kriterium == "Bericht-Note" })
    }

    @Test
    fun bewertung_torwartParadenZaehlenStattZweikaempfe() {
        val torwart = kandidat(
            "PARADE", "FC", SpielerPosition.TOR,
            sieg = true, gegenTore = 0, gehalteneBalle = 5,
        )
        val zeilen = ElfBewertung.bewerten(torwart).zeilen.associate { it.kriterium to it.beitrag }

        // Gehaltene Bälle ersetzen beim Torwart die Zweikämpfe (5–6 → +0,5).
        assertEquals(0.5, zeilen["Paraden"] ?: 0.0, 0.001)
        assertNull(zeilen["Zweikämpfe"])
        // Sieg + Zu-Null = 0,65 → Cap 0,55 in K4.
        assertEquals(0.55, zeilen["Ergebnis & Teambonus"] ?: 0.0, 0.001)
        assertEquals(6.6, note(torwart), 0.001)
    }

    // ---------- Einsatzzeit ----------

    @Test
    fun bewertung_einwechslerHatFesteNiedrigereBasis() {
        // Einwechsler: Basis 4,0 (fix), Zuschläge unskaliert; Starter: Basis 5,5.
        val einwechsler = kandidat(
            "SUB", "V", SpielerPosition.STU,
            startelf = false, minuten = 30, tore = 1, sieg = true,
        )
        val volles = kandidat("VOLL", "V", SpielerPosition.STU, tore = 1, sieg = true)

        assertEquals(6.4, note(volles), 0.001)
        assertEquals(4.9, note(einwechsler), 0.001)
        // Der Unterschied liegt exakt in der Basis (1,5) – kein Minuten-Faktor.
        assertEquals(note(volles) - note(einwechsler), 1.5, 0.001)
    }

    @Test
    fun bewertung_grundbewertungIstFuerEinwechslerKennzeichnend() {
        val starter = ElfBewertung.bewerten(kandidat("START", "V", SpielerPosition.STU))
        val einwechsler = ElfBewertung.bewerten(kandidat("JOKER", "V", SpielerPosition.STU, startelf = false))

        assertEquals("Grundbewertung", starter.zeilen.first().kriterium)
        assertEquals("Grundbewertung (Einwechslung)", einwechsler.zeilen.first().kriterium)
        assertEquals(5.5, starter.zeilen.first().beitrag, 0.001)
        assertEquals(4.0, einwechsler.zeilen.first().beitrag, 0.001)
    }

    @Test
    fun bewertung_nieEingewechselterBankSpielerWirdGeringBewertet() {
        val bank = kandidat("BANK", "V", SpielerPosition.STU, startelf = false)
        val starter = kandidat("START", "V", SpielerPosition.STU, sieg = true)
        assertTrue(note(bank) < note(starter))
        assertTrue(note(bank) <= 4.0)
    }

    // ---------- Keine Normalisierung / 10 ist Ausnahme ----------

    @Test
    fun bewertung_keineKuenstlicheNormalisierungAufZehn() {
        // Der beste Spieler eines Spieltags bleibt bei seiner echten Note –
        // auch wenn sonst niemand höher liegt, wird nichts auf 10 angehoben.
        val kandidaten = buildList {
            (1..10).forEach {
                add(kandidat("GUT$it", "V", SpielerPosition.STU, tore = if (it <= 2) 1 else 0, sieg = true))
            }
        }
        val bestaetigt = kandidaten.sortedByDescending { note(it) }.first()
        assertEquals(note(bestaetigt), ElfAuswahl.erstelleElf(kandidaten).spieler.maxOf { it.bewertung }, 0.001)
        assertTrue(note(bestaetigt) < 9.0)
    }

    @Test
    fun bewertung_zehnErfordertAussergewoehnlicheLeistung() {
        // Ein Tor im Sieg reicht nie für 10.
        val einTor = kandidat("A", "V", SpielerPosition.STU, tore = 1, sieg = true)
        assertTrue(note(einTor) < 10.0)

        // Auch ein Hattrick (3 Tore + Vorlage + Sieg) bleibt mit 7,7 unter 10,0.
        val hattrick = kandidat("B", "V", SpielerPosition.STU, tore = 3, vorlagen = 1, sieg = true)
        assertEquals(7.7, note(hattrick), 0.001)
        assertTrue(note(hattrick) < 10.0)
    }

    @Test
    fun bewertung_unterlegenerSpielerKannHochBewertetWerden() {
        // Zwei Tore + Vorlage trotz Niederlage – individuelle Leistung dominiert.
        val kandidat = kandidat("VERLIERER", "V", SpielerPosition.STU, tore = 2, vorlagen = 1, niederlage = true)
        assertEquals(6.7, note(kandidat), 0.001)
    }

    // ---------- ElfAuswahl ----------

    @Test
    fun auswahl_bildetGuenstigsteVerfuegbareFormation() {
        val kandidaten = buildList {
            (1..1).forEach { add(kandidat("TW$it", "TW", SpielerPosition.TOR)) }
            (1..6).forEach { add(kandidat("ABW$it", "ABW", SpielerPosition.ABW)) }
            (1..5).forEach { add(kandidat("MIT$it", "MIT", SpielerPosition.MIT)) }
            (1..4).forEach { add(kandidat("STU$it", "STU", SpielerPosition.STU)) }
        }
        val aufstellung = ElfAuswahl.erstelleElf(kandidaten)

        assertEquals(11, aufstellung.spieler.size)
        assertTrue(aufstellung.vollstaendig)
        assertNotNull(aufstellung.formation)
        // Die Formation ist eine der erlaubten klassischen Formationen.
        assertTrue(ElfAuswahl.FORMATIONEN.any { it.label == aufstellung.formation })
        // Die gewählten Anzahlen entsprechen exakt der Formation. Das Mittelfeld
        // wird auf die Formationslinien (OMI/MIT/DMI) verteilt – die Summe zählt.
        val formation = ElfAuswahl.FORMATIONEN.first { it.label == aufstellung.formation }
        assertEquals(1, aufstellung.spieler.count { it.position == SpielerPosition.TOR })
        assertEquals(formation.abwehr, aufstellung.spieler.count { it.position == SpielerPosition.ABW })
        assertEquals(
            formation.mittelfeld,
            aufstellung.spieler.count {
                it.position == SpielerPosition.OMI ||
                    it.position == SpielerPosition.MIT ||
                    it.position == SpielerPosition.DMI
            },
        )
        assertEquals(formation.omi, aufstellung.spieler.count { it.position == SpielerPosition.OMI })
        assertEquals(formation.dmi, aufstellung.spieler.count { it.position == SpielerPosition.DMI })
        assertEquals(formation.sturm, aufstellung.spieler.count { it.position == SpielerPosition.STU })
    }

    @Test
    fun auswahl_waehltDreiFuenfZweiWennNurDreiAbwehrVerfuegbar() {
        // Nur 3 Abwehrspieler: 4-3-3 ist nicht füllbar, 3-5-2 gewinnt.
        val kandidaten = buildList {
            (1..1).forEach { add(kandidat("TW$it", "TW", SpielerPosition.TOR)) }
            (1..3).forEach { add(kandidat("ABW$it", "ABW", SpielerPosition.ABW)) }
            (1..5).forEach { add(kandidat("MIT$it", "MIT", SpielerPosition.MIT)) }
            (1..2).forEach { add(kandidat("STU$it", "STU", SpielerPosition.STU)) }
        }
        val (elf, formation, vollstaendig) = ElfAuswahl.erstelleElf(kandidaten)

        assertEquals("3-5-2", formation)
        assertTrue(vollstaendig)
        assertEquals(11, elf.size)
        assertEquals(3, elf.count { it.position == SpielerPosition.ABW })
        assertEquals(5, elf.count { it.position == SpielerPosition.MIT })
        assertEquals(2, elf.count { it.position == SpielerPosition.STU })
    }

    @Test
    fun auswahl_uebernimmtBewertungsEingabenInSpieler() {
        val kandidaten = buildList {
            (1..1).forEach { add(kandidat("TW", "TW", SpielerPosition.TOR, sieg = true, gegenTore = 0)) }
            (1..4).forEach { add(kandidat("ABW$it", "ABW", SpielerPosition.ABW)) }
            (1..3).forEach { add(kandidat("MIT$it", "MIT", SpielerPosition.MIT)) }
            add(kandidat("STERN", "FC", SpielerPosition.STU, tore = 2, sieg = true, spielerId = 4711))
            (1..2).forEach { add(kandidat("STU$it", "STU", SpielerPosition.STU)) }
        }
        val (elf, _, _) = ElfAuswahl.erstelleElf(kandidaten)
        val stern = elf.first { it.name == "STERN" }

        assertEquals(2, stern.tore)
        assertTrue(stern.sieg)
        assertEquals(4711L, stern.spielerId)
        assertEquals(note(kandidaten.first { it.name == "STERN" }), stern.bewertung, 0.001)
    }

    @Test
    fun auswahl_bewertungBleibtNachAuswahlUnveraendert() {
        val kandidaten = buildList {
            add(kandidat("TW", "V", SpielerPosition.TOR))
            (1..4).forEach { add(kandidat("ABW$it", "V", SpielerPosition.ABW, sieg = true)) }
            (1..3).forEach { add(kandidat("MIT$it", "V", SpielerPosition.MIT)) }
            add(kandidat("BESTER", "V", SpielerPosition.STU, tore = 1, sieg = true))
            add(kandidat("STU2", "V", SpielerPosition.STU))
            add(kandidat("STU3", "V", SpielerPosition.STU))
        }
        val (elf, _, _) = ElfAuswahl.erstelleElf(kandidaten)
        val bester = elf.first { it.name == "BESTER" }
        assertEquals(note(kandidaten.first { it.name == "BESTER" }), bester.bewertung, 0.001)
    }

    @Test
    fun auswahl_nimmtBesserenProPosition() {
        // Bestbewerteter Spieler insgesamt ist ein Stürmer, die Abwehr hält
        // trotzdem ihre 4 eigenen Plätze; der schwächste Abwehrspieler fällt raus.
        val kandidaten = buildList {
            add(kandidat("TW", "V", SpielerPosition.TOR))
            (1..4).forEach { add(kandidat("ABW$it", "V", SpielerPosition.ABW, sieg = true)) }
            add(kandidat("SCHWACH_ABW", "V", SpielerPosition.ABW, startelf = false))
            (1..3).forEach { add(kandidat("MIT$it", "V", SpielerPosition.MIT)) }
            add(kandidat("STERN", "V", SpielerPosition.STU, tore = 2, sieg = true))
            add(kandidat("STU2", "V", SpielerPosition.STU))
            add(kandidat("STU3", "V", SpielerPosition.STU))
        }
        val (elf, _, vollstaendig) = ElfAuswahl.erstelleElf(kandidaten)

        assertTrue(vollstaendig)
        assertTrue(elf.any { it.name == "STERN" })
        assertFalse(elf.any { it.name == "SCHWACH_ABW" })
        assertEquals(4, elf.count { it.position == SpielerPosition.ABW })
    }

    @Test
    fun auswahl_keineLueckenNurDurchFallback() {
        val kandidaten = buildList {
            (1..1).forEach { add(kandidat("TW", "TW", SpielerPosition.TOR)) }
            (1..2).forEach { add(kandidat("ABW$it", "ABW", SpielerPosition.ABW)) }
            (1..5).forEach { add(kandidat("MIT$it", "MIT", SpielerPosition.MIT)) }
            (1..3).forEach { add(kandidat("STU$it", "STU", SpielerPosition.STU)) }
        }
        val (elf, _, vollstaendig) = ElfAuswahl.erstelleElf(kandidaten)

        assertEquals(11, elf.size)
        assertFalse(vollstaendig)
    }

    @Test
    fun auswahl_wenigerKandidatenErgibtKurzeListe() {
        val (elf, _, vollstaendig) = ElfAuswahl.erstelleElf(
            listOf(kandidat("NUR", "V", SpielerPosition.STU)),
        )
        assertFalse(vollstaendig)
        assertEquals(1, elf.size)
        assertEquals("NUR", elf.first().name)
    }

    @Test
    fun auswahl_gleicherSpielerNurEinmal() {
        val doppelt = kandidat("DOPPELT", "V", SpielerPosition.STU)
        val (elf, _, _) = ElfAuswahl.erstelleElf(
            buildList {
                (1..1).forEach { add(kandidat("TW", "TW", SpielerPosition.TOR)) }
                (1..4).forEach { add(kandidat("ABW$it", "ABW", SpielerPosition.ABW)) }
                (1..3).forEach { add(kandidat("MIT$it", "MIT", SpielerPosition.MIT)) }
                add(doppelt)
                add(doppelt.copy(startelf = false))
            },
        )
        assertEquals(1, elf.count { it.name == "DOPPELT" })
    }

    // ---------- Spieler des Spieltags ----------

    @Test
    fun ergebnis_spielerDesSpieltagsIstHoehstbewertet() {
        val spieler = listOf(
            ElfSpieler("B", "V", SpielerPosition.STU, 8.4),
            ElfSpieler("A", "V", SpielerPosition.STU, 9.1),
            ElfSpieler("C", "V", SpielerPosition.TOR, 7.2),
        )
        val ergebnis = ElfErgebnis(
            land = "D", liga = "1", saison = 24, spieltag = 1, spieler = spieler,
        )
        assertEquals("A", ergebnis.spielerDesSpieltags?.name)
        assertEquals(9.1, ergebnis.spielerDesSpieltags?.bewertung ?: 0.0, 0.001)
    }

    @Test
    fun ergebnis_besterSpieltagOhneZehnIsPlausibel() {
        // Ein typischer Spieltag: Bester 8,6 – wie vom Nutzer gefordert plausibel.
        val spieler = listOf(
            ElfSpieler("Spieler1", "V", SpielerPosition.STU, 8.6),
            ElfSpieler("Spieler2", "V", SpielerPosition.MIT, 8.3),
            ElfSpieler("Spieler3", "V", SpielerPosition.ABW, 8.1),
            ElfSpieler("Spieler4", "V", SpielerPosition.ABW, 7.9),
            ElfSpieler("Spieler5", "V", SpielerPosition.STU, 7.8),
            ElfSpieler("Spieler6", "V", SpielerPosition.TOR, 7.6),
        )
        val ergebnis = ElfErgebnis(
            land = "D", liga = "1", saison = 24, spieltag = 1, spieler = spieler,
        )
        assertEquals(8.6, ergebnis.spielerDesSpieltags?.bewertung ?: 0.0, 0.001)
    }

    // ---------- ElfCache ----------

    @Test
    fun cache_schluesselTrenntNutzerUndLiga() {
        val a = ElfCache.schluessel(teamId = 1, ligaId = 3, landId = 2, saison = 24, zat = 5)
        val b = ElfCache.schluessel(teamId = 2, ligaId = 3, landId = 2, saison = 24, zat = 5)
        val c = ElfCache.schluessel(teamId = 1, ligaId = 3, landId = 2, saison = 25, zat = 5)
        val d = ElfCache.schluessel(teamId = 1, ligaId = 3, landId = 2, saison = 24, zat = 5)
        assertNotEquals(a, b)
        assertNotEquals(a, c)
        assertEquals(a, d)
    }

    @Test
    fun cache_wechselDerLigaVerwendetNichtErgebnisDerVorherigenLiga() {
        // „Italien · Liga 1 · Spieltag 2" (landId=4, ligaId=1) …
        val italienLiga1Spieltag2 = ElfCache.schluessel(teamId = 7, ligaId = 1, landId = 4, saison = 24, zat = 2)
        // … und „Italien · Liga 2 · Spieltag 2" (ligaId=2) dürfen niemals denselben
        // Cache-Eintrag verwenden – sonst würde beim Ligawechsel die Elf der
        // vorherigen Liga aus dem Cache gezeigt.
        val italienLiga2Spieltag2 = ElfCache.schluessel(teamId = 7, ligaId = 2, landId = 4, saison = 24, zat = 2)
        assertNotEquals(italienLiga1Spieltag2, italienLiga2Spieltag2)
    }

    @Test
    fun cache_schluesselTrenntLigenLaenderUndSpieltage() {
        // Gleicher Nutzer, gleiches Land, gleiche Saison, gleicher Spieltag –
        // nur die Liga wechselt: getrennte Schlüssel.
        val liga1 = ElfCache.schluessel(teamId = 7, ligaId = 1, landId = 4, saison = 24, zat = 2)
        val liga2 = ElfCache.schluessel(teamId = 7, ligaId = 2, landId = 4, saison = 24, zat = 2)
        assertNotEquals(liga1, liga2)

        // Auch Land- und Spieltagwechsel trennen; identische Kombination ist identisch.
        assertNotEquals(liga1, ElfCache.schluessel(teamId = 7, ligaId = 1, landId = 5, saison = 24, zat = 2))
        assertNotEquals(liga1, ElfCache.schluessel(teamId = 7, ligaId = 1, landId = 4, saison = 24, zat = 3))
        assertNotEquals(liga1, ElfCache.schluessel(teamId = 7, ligaId = 1, landId = 4, saison = 25, zat = 2))
        assertEquals(liga1, ElfCache.schluessel(teamId = 7, ligaId = 1, landId = 4, saison = 24, zat = 2))
    }

    @Test
    fun ermittlung_funktioniertFuerZweiVerschiedeneLigenUnabhaengig() {
        // „Italien · Liga 1 · Spieltag 2"
        val italienLiga1 = elfFuerKombination(
            land = "Italien",
            liga = "Liga 1",
            saison = 24,
            spieltag = 2,
            vereinPraefix = "IT1",
        )
        // „Italien · Liga 2 · Spieltag 2"
        val italienLiga2 = elfFuerKombination(
            land = "Italien",
            liga = "Liga 2",
            saison = 24,
            spieltag = 2,
            vereinPraefix = "IT2",
        )

        // Beide Kombinationen werden eigenständig berechnet und sind gültig.
        assertEquals("Italien", italienLiga1.land)
        assertEquals("Liga 1", italienLiga1.liga)
        assertEquals(2, italienLiga1.spieltag)
        assertEquals("Italien", italienLiga2.land)
        assertEquals("Liga 2", italienLiga2.liga)
        assertEquals(2, italienLiga2.spieltag)
        assertTrue(italienLiga1.spieler.isNotEmpty())
        assertTrue(italienLiga2.spieler.isNotEmpty())
        assertEquals(11, italienLiga1.spieler.size)
        assertEquals(11, italienLiga2.spieler.size)
        assertTrue(italienLiga1.vollstaendig)
        assertTrue(italienLiga2.vollstaendig)

        // Die Elf der Liga 2 ist nicht die Elf der Liga 1 (keine Vermischung).
        val namenLiga1 = italienLiga1.spieler.map { it.name }.toSet()
        val namenLiga2 = italienLiga2.spieler.map { it.name }.toSet()
        assertNotEquals(namenLiga1, namenLiga2)

        // Gleiche Kombination ist reproduzierbar (deterministisch, cachebar).
        val wiederholt = elfFuerKombination(
            land = "Italien",
            liga = "Liga 1",
            saison = 24,
            spieltag = 2,
            vereinPraefix = "IT1",
        )
        assertEquals(italienLiga1, wiederholt)
    }

    // ---------- ElfDesSpieltagsTest-Helfer ----------

    /** Baut die Elf ausschließlich aus den Berichten einer Land+Liga+Spieltag-Kombination. */
    private fun elfFuerKombination(
        land: String,
        liga: String,
        saison: Int,
        spieltag: Int,
        vereinPraefix: String,
    ): ElfErgebnis {
        val kandidaten = buildList {
            repeat(3) { i ->
                val heimId = (1000 + i).toLong()
                val gastId = (2000 + i).toLong()
                addAll(
                    ElfAuswertung.kandidatenAusBericht(
                        SpielBericht(
                            saison = saison,
                            zat = spieltag,
                            ergebnis = if (i % 2 == 0) "2:1" else "1:0",
                            heim = "$vereinPraefix Alpha $i",
                            gast = "$vereinPraefix Beta $i",
                            heimId = heimId,
                            gastId = gastId,
                            heimAufstellung = BerichtAufstellung(
                                spieler = listOf(
                                    BerichtSpieler("$vereinPraefix Keeper $i", "T", "Torwart", 15),
                                    BerichtSpieler("$vereinPraefix Abwehr A $i", "C", "Abwehr", 10),
                                    BerichtSpieler("$vereinPraefix Abwehr B $i", "D", "Abwehr", 11),
                                ),
                            ),
                            gastAufstellung = BerichtAufstellung(
                                spieler = listOf(
                                    BerichtSpieler("$vereinPraefix Stuermer $i", "A", "Sturm", 0),
                                    BerichtSpieler("$vereinPraefix Mittelfeld $i", "B", "Mittelfeld", 4),
                                ),
                            ),
                            ereignisse = listOf(
                                BerichtEreignis(
                                    "10",
                                    "Neuer Spielstand: 1:0 ($vereinPraefix Stuermer $i)",
                                    BerichtEreignisTyp.TOR,
                                ),
                                BerichtEreignis(
                                    "40",
                                    "Neuer Spielstand: 2:0 ($vereinPraefix Mittelfeld $i)",
                                    BerichtEreignisTyp.TOR,
                                ),
                                BerichtEreignis(
                                    "70",
                                    "Neuer Spielstand: 2:1 ($vereinPraefix Beta $i)",
                                    BerichtEreignisTyp.TOR,
                                ),
                            ),
                            url = "test-$land-$liga-$spieltag",
                        ),
                    ),
                )
            }
        }
        val aufstellung = ElfAuswahl.erstelleElf(kandidaten)
        return ElfErgebnis(
            land = land,
            liga = liga,
            saison = saison,
            spieltag = spieltag,
            spieler = aufstellung.spieler,
            formation = aufstellung.formation,
            begegnungen = 3,
            berichteErfolgreich = 3,
            vollstaendig = aufstellung.vollstaendig,
        )
    }

    @Test
    fun cache_roundtripErhaeltErgebnis() {
        val ergebnis = ElfErgebnis(
            land = "Deutschland",
            liga = "1. Liga",
            saison = 24,
            spieltag = 7,
            spieler = listOf(
                ElfSpieler(
                    name = "Max Muster",
                    verein = "FC Test",
                    position = SpielerPosition.STU,
                    bewertung = 8.6,
                    spielerId = 4711,
                    startelf = true,
                    minuten = 90,
                    tore = 2,
                    vorlagen = 1,
                    sieg = true,
                    zweikaempfe = 12,
                    zweikampfQuote = 58.33,
                    schuesse = 7,
                    aufsTor = 5,
                    auffaelligkeit = 4,
                    berichtNote = 2.5,
                ),
                ElfSpieler(
                    name = "Tom Tormann",
                    verein = "FC Test",
                    position = SpielerPosition.TOR,
                    bewertung = 7.4,
                    startelf = true,
                    gegenTore = 0,
                    sieg = true,
                    gehalteneBalle = 6,
                ),
            ),
            formation = "4-3-3",
            begegnungen = 9,
            berichteErfolgreich = 9,
            vollstaendig = true,
        )
        val gelesen = ElfCache.deserialisieren(ElfCache.serialisieren(ergebnis))

        assertEquals(ergebnis, gelesen)
    }

    @Test
    fun cache_kaputteDatenErgebenNull() {
        assertEquals(null, ElfCache.deserialisieren("Müll"))
        assertEquals(null, ElfCache.deserialisieren(""))
    }

    @Test
    fun cache_alteVersionWirdVerworfen() {
        // v1-Einträge (4 Felder je Spieler) sind mit der neuen Bewertung ungültig.
        val alt = "ElfDesSpieltags|v1\u241FDeutschland\u241F1. Liga\u241F24\u241F7\u241F9\u241F9\u241Ftrue" +
            "\u241EMax\u241FFC Test\u241ESTU\u241E8.6"
        assertEquals(null, ElfCache.deserialisieren(alt))

        // Auch v3 (17 Felder, ohne Formation) und v4 (23 Felder, ohne teamId)
        // werden zugunsten von v7 (26 Felder je Spieler) verworfen.
        val dritte = "ElfDesSpieltags|v3\u241FDeutschland\u241F1. Liga\u241F24\u241F7\u241F9\u241F9\u241Ftrue" +
            listOf("Max", "FC Test", "STU", "7.7", "", "true", "90", "2", "1", "0", "0", "true", "false", "false", "0", "true", "false")
                .joinToString("\u241F", "\u241E")
        assertEquals(null, ElfCache.deserialisieren(dritte))

        // v4 (23 Felder) ohne teamId ist ebenfalls zu verwerfen.
        val vierte = "ElfDesSpieltags|v4\u241FDeutschland\u241F1. Liga\u241F24\u241F7\u241F9\u241F9\u241Ftrue" +
            listOf("Max", "FC Test", "STU", "7.7", "5", "true", "90", "2", "1", "0", "0", "true", "false", "false", "0", "true", "false", "0", "0.0", "0", "0", "0", null)
                .joinToString("\u241F", "\u241E")
        assertEquals(null, ElfCache.deserialisieren(vierte))

        // v6 (25 Felder) ohne berichtNote wird zugunsten von v7 verworfen.
        val sechste = "ElfDesSpieltags|v6\u241FDeutschland\u241F1. Liga\u241F24\u241F7\u241F9\u241F9\u241Ftrue" +
            listOf(
                "Max", "FC Test", "STU", "7.7", "5", "4711", "true", "90", "2", "1", "0", "0",
                "true", "false", "false", "0", "true", "false", "12", "58.33", "7", "5", "4", null, "true",
            ).joinToString("\u241F", "\u241E")
        assertEquals(null, ElfCache.deserialisieren(sechste))
    }

    // ---------- ElfAuswertung ----------

    @Test
    fun auswertung_uebernimmtRealeBerichtsdaten() {
        val bericht = SpielBericht(
            saison = 24,
            zat = 5,
            heim = "FC Alpha",
            gast = "KSV Beta",
            heimId = 10,
            gastId = 20,
            ergebnis = "2:1",
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(
                    BerichtSpieler("Aaron Muller", "A", "Sturm", 0),
                    BerichtSpieler("Ben Klinger", "B", "Mittelfeld", 4),
                    BerichtSpieler("Cedric Hofer", "C", "Abwehr", 10),
                    BerichtSpieler("Tom Mayer", "T", "Torwart", 15),
                ),
                einstellungen = BerichtEinstellungen(),
            ),
            gastAufstellung = BerichtAufstellung(
                spieler = listOf(
                    BerichtSpieler("Dieter Roth", "A", "Sturm", 1),
                    BerichtSpieler("Emil Kraft", "B", "Mittelfeld", 5),
                    BerichtSpieler("Frank Bauer", "C", "Abwehr", 11),
                    BerichtSpieler("Tim Werner", "T", "Torwart", 15),
                ),
                einstellungen = BerichtEinstellungen(),
            ),
            ereignisse = listOf(
                BerichtEreignis("2", "TOR Neuer Spielstand: 1:0 (Aaron Muller)", BerichtEreignisTyp.TOR),
                BerichtEreignis("33", "TOR Neuer Spielstand: 2:0 (Ben Klinger)", BerichtEreignisTyp.TOR),
                BerichtEreignis("70", "TOR Neuer Spielstand: 2:1 (Dieter Roth)", BerichtEreignisTyp.TOR),
                BerichtEreignis("65", "Gelbe Karte für Frank Bauer", BerichtEreignisTyp.GELBE_KARTE),
                BerichtEreignis("85", "Gelbe Karte", BerichtEreignisTyp.GELBE_KARTE),
            ),
            url = "test",
        )

        val kandidaten = ElfAuswertung.kandidatenAusBericht(bericht)
        val heim = kandidaten.filter { it.teamId == 10L }
        val gast = kandidaten.filter { it.teamId == 20L }

        assertEquals(4, heim.size)
        assertEquals(4, gast.size)

        val aaron = kandidaten.first { it.name == "Aaron Muller" }
        assertTrue(aaron.startelf)
        assertEquals(SpielerPosition.STU, aaron.position)
        assertEquals(1, aaron.tore)
        assertTrue(aaron.sieg)
        assertFalse(aaron.niederlage)
        assertEquals(1, aaron.gegenTore)

        val ben = kandidaten.first { it.name == "Ben Klinger" }
        assertEquals(1, ben.tore)
        assertEquals(SpielerPosition.MIT, ben.position)

        val dieter = kandidaten.first { it.name == "Dieter Roth" }
        assertTrue(dieter.niederlage)
        assertEquals(1, dieter.tore)

        val frank = kandidaten.first { it.name == "Frank Bauer" }
        assertEquals(1, frank.gelbeKarten)
        assertEquals(0, frank.roteKarten)

        // Unbekannter Spieler in einem Tor-Ereignis wird nicht zugeordnet.
        assertTrue(kandidaten.none { it.tore > 1 })
    }

    @Test
    fun auswertung_erkenntKapitaenNurBeiEindeutigemHinweis() {
        val bericht = SpielBericht(
            saison = 24, zat = 5, ergebnis = "2:0",
            heim = "FC Alpha", gast = "KSV Beta", heimId = 1, gastId = 2,
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(
                    BerichtSpieler("Max Müller", "A", "Sturm", 0),
                    BerichtSpieler("Tom Mayer", "B", "Mittelfeld", 4),
                    BerichtSpieler("Jan Schulz", "T", "Torwart", 15),
                ),
            ),
            gastAufstellung = BerichtAufstellung(spieler = emptyList()),
            ereignisse = emptyList(),
            url = "test",
            rohtext = "Aufstellung: Max Müller (C) … Tom Mayer (Kapitän) …",
        )
        val kandidaten = ElfAuswertung.kandidatenAusBericht(bericht)
        assertTrue(kandidaten.first { it.name == "Max Müller" }.kapitän)
        assertTrue(kandidaten.first { it.name == "Tom Mayer" }.kapitän)
        assertFalse(kandidaten.first { it.name == "Jan Schulz" }.kapitän)
    }

    @Test
    fun auswertung_teamIdsAusBerichtUrlBeiStatischemBericht() {
        // Statische Berichte tragen Team-IDs nur im Dateinamen (`<heimId>-<gastId>.html`).
        val bericht = SpielBericht(
            saison = 24,
            zat = 7,
            heim = "FC Alpha",
            gast = "KSV Beta",
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(
                    BerichtSpieler("Aaron Muller", "A", "Sturm", 0),
                ),
            ),
            gastAufstellung = BerichtAufstellung(
                spieler = listOf(
                    BerichtSpieler("Dieter Roth", "A", "Sturm", 1),
                ),
            ),
            ereignisse = emptyList(),
            url = "https://os.ongapo.com/rep/saison/24/7/42-17.html",
        )

        val kandidaten = ElfAuswertung.kandidatenAusBericht(bericht)

        assertEquals(42L, kandidaten.first { it.name == "Aaron Muller" }.teamId)
        assertEquals(17L, kandidaten.first { it.name == "Dieter Roth" }.teamId)
    }

    @Test
    fun auswertung_beruecksichtigtEinsatzminutenAusWechseln() {
        val bericht = SpielBericht(
            saison = 24, zat = 5, ergebnis = "1:1",
            heim = "FC Alpha", gast = "KSV Beta", heimId = 1, gastId = 2,
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(
                    BerichtSpieler("Start Spieler", "A", "Sturm", 0),
                    BerichtSpieler("Joker Ben", "U", "Sturm", null),
                ),
            ),
            gastAufstellung = BerichtAufstellung(spieler = emptyList()),
            ereignisse = listOf(
                BerichtEreignis("60", "Einwechslung: Joker Ben", BerichtEreignisTyp.SONSTIGES),
                BerichtEreignis("75", "Auswechslung: Start Spieler", BerichtEreignisTyp.SONSTIGES),
            ),
            url = "test",
        )

        val kandidaten = ElfAuswertung.kandidatenAusBericht(bericht)
        val starter = kandidaten.first { it.name == "Start Spieler" }
        val joker = kandidaten.first { it.name == "Joker Ben" }

        assertEquals(75, starter.minuten)
        assertTrue(starter.startelf)
        assertEquals(30, joker.minuten) // 90 - 60
        assertFalse(joker.startelf)
    }

    @Test
    fun auswertung_erkenntElfmeterNurBeiBeleg() {
        val bericht = SpielBericht(
            saison = 24, zat = 5, ergebnis = "1:0",
            heim = "FC Alpha", gast = "KSV Beta", heimId = 1, gastId = 2,
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(BerichtSpieler("Aaron Muller", "A", "Sturm", 0)),
            ),
            gastAufstellung = BerichtAufstellung(spieler = emptyList()),
            ereignisse = listOf(
                BerichtEreignis(
                    "60",
                    "Elfmeter verwandelt – TOR Neuer Spielstand: 1:0 (Aaron Muller)",
                    BerichtEreignisTyp.TOR,
                ),
            ),
            url = "test",
        )

        val aaron = ElfAuswertung.kandidatenAusBericht(bericht).first { it.name == "Aaron Muller" }
        assertTrue(aaron.elfmeter)
        assertEquals(1, aaron.tore)
    }

    @Test
    fun auswertung_uebernimmtSpielerIdAusBericht() {
        val bericht = SpielBericht(
            saison = 24, zat = 5, ergebnis = "0:0",
            heim = "FC Alpha", gast = "KSV Beta", heimId = 1, gastId = 2,
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(
                    BerichtSpieler("Aaron Muller", "A", "Sturm", 0, spielerId = 12345),
                ),
            ),
            gastAufstellung = BerichtAufstellung(spieler = emptyList()),
            url = "test",
        )

        val aaron = ElfAuswertung.kandidatenAusBericht(bericht).first { it.name == "Aaron Muller" }
        assertEquals(12345L, aaron.spielerId)
    }

    @Test
    fun auswertung_torUndVorlageAusSpielstandsklammer() {
        // Echte Server-Form: „Neuer Spielstand: X:Y (Torschütze, Vorlage)".
        val bericht = SpielBericht(
            saison = 24, zat = 5, ergebnis = "2:1",
            heim = "FC Alpha", gast = "KSV Beta", heimId = 1, gastId = 2,
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(
                    BerichtSpieler("Aaron Muller", "A", "Sturm", 0),
                    BerichtSpieler("Ben Klinger", "B", "Mittelfeld", 4),
                ),
            ),
            gastAufstellung = BerichtAufstellung(spieler = emptyList()),
            ereignisse = listOf(
                BerichtEreignis("10", "Neuer Spielstand: 1:0 (Aaron Muller, Ben Klinger)", BerichtEreignisTyp.TOR),
                BerichtEreignis("33", "Neuer Spielstand: 2:1 (Aaron Muller)", BerichtEreignisTyp.TOR),
            ),
            url = "test",
        )

        val kandidaten = ElfAuswertung.kandidatenAusBericht(bericht)
        val aaron = kandidaten.first { it.name == "Aaron Muller" }
        val ben = kandidaten.first { it.name == "Ben Klinger" }

        assertEquals(2, aaron.tore)
        assertEquals(1, ben.vorlagen)
    }

    @Test
    fun auswertung_eingewechseltUeberKommtFuer() {
        // Echte Server-Form: „… wechselt: A kommt für B".
        val bericht = SpielBericht(
            saison = 24, zat = 5, ergebnis = "1:1",
            heim = "FC Alpha", gast = "KSV Beta", heimId = 1, gastId = 2,
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(
                    BerichtSpieler("Start Spieler", "A", "Sturm", 0),
                    BerichtSpieler("Joker Ben", "U", "Sturm", null),
                ),
            ),
            gastAufstellung = BerichtAufstellung(spieler = emptyList()),
            ereignisse = listOf(
                BerichtEreignis("60", "Heimteam wechselt: Joker Ben kommt für Start Spieler", BerichtEreignisTyp.SONSTIGES),
            ),
            url = "test",
        )

        val kandidaten = ElfAuswertung.kandidatenAusBericht(bericht)
        val starter = kandidaten.first { it.name == "Start Spieler" }
        val joker = kandidaten.first { it.name == "Joker Ben" }

        assertEquals(60, starter.minuten) // Startelf-Spieler wurde in der 60. Minute ausgewechselt
        assertEquals(30, joker.minuten)   // 90 - 60
    }

    @Test
    fun auswertung_mehrereWechselImSelbenEintrag() {
        val bericht = SpielBericht(
            saison = 24, zat = 5, ergebnis = "0:0",
            heim = "FC Alpha", gast = "KSV Beta", heimId = 1, gastId = 2,
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(
                    BerichtSpieler("A Spieler", "A", "Sturm", 0),
                    BerichtSpieler("B Spieler", "B", "Mittelfeld", 4),
                    BerichtSpieler("Joker Eins", "U", "Sturm", null),
                    BerichtSpieler("Joker Zwei", "V", "Mittelfeld", null),
                ),
            ),
            gastAufstellung = BerichtAufstellung(spieler = emptyList()),
            ereignisse = listOf(
                BerichtEreignis(
                    "70",
                    "Heimteam wechselt: Joker Eins kommt für A Spieler, Joker Zwei kommt für B Spieler",
                    BerichtEreignisTyp.SONSTIGES,
                ),
            ),
            url = "test",
        )

        val kandidaten = ElfAuswertung.kandidatenAusBericht(bericht)
        assertEquals(20, kandidaten.first { it.name == "Joker Eins" }.minuten)
        assertEquals(20, kandidaten.first { it.name == "Joker Zwei" }.minuten)
        assertEquals(70, kandidaten.first { it.name == "A Spieler" }.minuten)
        assertEquals(70, kandidaten.first { it.name == "B Spieler" }.minuten)
    }

    @Test
    fun auswertung_karteUeberKassiertDafuer() {
        // Echte Server-Form: „FREISTOSS Frank Bauer kassiert dafür die gelbe Karte".
        val bericht = SpielBericht(
            saison = 24, zat = 5, ergebnis = "0:0",
            heim = "FC Alpha", gast = "KSV Beta", heimId = 1, gastId = 2,
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(
                    BerichtSpieler("Frank Bauer", "C", "Abwehr", 10),
                    BerichtSpieler("Aaron Muller", "A", "Sturm", 0),
                ),
            ),
            gastAufstellung = BerichtAufstellung(spieler = emptyList()),
            ereignisse = listOf(
                BerichtEreignis("65", "FREISTOSS Frank Bauer kassiert dafür die gelbe Karte", BerichtEreignisTyp.GELBE_KARTE),
            ),
            url = "test",
        )

        val frank = ElfAuswertung.kandidatenAusBericht(bericht).first { it.name == "Frank Bauer" }
        assertEquals(1, frank.gelbeKarten)
        assertEquals(0, frank.roteKarten)
        assertEquals(0, ElfAuswertung.kandidatenAusBericht(bericht).first { it.name == "Aaron Muller" }.gelbeKarten)
    }

    @Test
    fun auswertung_stichprobeRealerSpielbericht() {
        // Die Tore/Vorlagen aus dem real getesteten Bericht (1077-1075, Saison 24, ZAT 2).
        val kandidaten = ElfAuswertung.kandidatenAusBericht(
            SpielBericht(
                saison = 24, zat = 2, ergebnis = "1:4",
                heim = "Newcastle Glory", gast = "FC Moor",
                heimId = 1077, gastId = 1075,
                heimAufstellung = BerichtAufstellung(spieler = listOf(BerichtSpieler("Anthony Colt", "A", "Sturm", 0))),
                gastAufstellung = BerichtAufstellung(
                    spieler = listOf(
                        BerichtSpieler("Ebulfez Mammadov", "B", "Mittelfeld", 4),
                        BerichtSpieler("David Evian", "C", "Mittelfeld", 5),
                        BerichtSpieler("Brane Simovic", "D", "Sturm", 0),
                        BerichtSpieler("Daryll Trumble", "E", "Mittelfeld", 6),
                    ),
                ),
                ereignisse = listOf(
                    BerichtEreignis("8", "Neuer Spielstand: 0:1 (Ebulfez Mammadov, David Evian)", BerichtEreignisTyp.TOR),
                    BerichtEreignis("22", "Neuer Spielstand: 0:2 (Ebulfez Mammadov, Daryll Trumble)", BerichtEreignisTyp.TOR),
                    BerichtEreignis("44", "Neuer Spielstand: 0:3 (Brane Simovic, Ebulfez Mammadov)", BerichtEreignisTyp.TOR),
                    BerichtEreignis("78", "Neuer Spielstand: 0:4 (Ebulfez Mammadov, Daryll Trumble)", BerichtEreignisTyp.TOR),
                    BerichtEreignis("88", "Neuer Spielstand: 1:4 (Anthony Colt, Dashamir Gecaj)", BerichtEreignisTyp.TOR),
                ),
                url = "test",
            ),
        )

        assertEquals(3, kandidaten.first { it.name == "Ebulfez Mammadov" }.tore)
        assertEquals(1, kandidaten.first { it.name == "Ebulfez Mammadov" }.vorlagen)
        assertEquals(1, kandidaten.first { it.name == "Brane Simovic" }.tore)
        assertEquals(1, kandidaten.first { it.name == "Anthony Colt" }.tore)
        assertEquals(2, kandidaten.first { it.name == "Daryll Trumble" }.vorlagen)
        assertEquals(1, kandidaten.first { it.name == "David Evian" }.vorlagen)
    }

    @Test
    fun auswertung_uebernimmtSpielerstatistikUndAuffaelligkeit() {
        val bericht = SpielBericht(
            saison = 24, zat = 5, ergebnis = "1:0",
            heim = "FC Alpha", gast = "KSV Beta", heimId = 1, gastId = 2,
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(
                    BerichtSpieler("Aaron Muller", "A", "Sturm", 0),
                    BerichtSpieler("Tom Mayer", "T", "Torwart", 15),
                ),
            ),
            gastAufstellung = BerichtAufstellung(spieler = emptyList()),
            heimSpielerStatistik = mapOf(
                "aaron muller" to com.onlinesoccer.app.data.model.BerichtSpielerStatistik(
                    zweikaempfe = 8, zweikampfQuote = 62.5, schuesse = 5, aufsTor = 3,
                ),
            ),
            ereignisse = listOf(
                BerichtEreignis("10", "Neuer Spielstand: 1:0 (Aaron Muller)", BerichtEreignisTyp.TOR),
            ),
            url = "test",
        )

        val aaron = ElfAuswertung.kandidatenAusBericht(bericht).first { it.name == "Aaron Muller" }
        assertEquals(8, aaron.zweikaempfe)
        assertEquals(62.5, aaron.zweikampfQuote, 0.001)
        assertEquals(5, aaron.schuesse)
        assertEquals(3, aaron.aufsTor)
        assertEquals(1, aaron.auffaelligkeit)
    }

    @Test
    fun auswertung_gehalteneBalleFuerTorwartAusStatistik() {
        // Heim-Torwart: Gast feuert 3 Schüsse aufs Tor, 1 Gegentor → 2 Paraden.
        val bericht = SpielBericht(
            saison = 24, zat = 5, ergebnis = "1:1",
            heim = "FC Alpha", gast = "KSV Beta", heimId = 1, gastId = 2,
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(
                    BerichtSpieler("Ein Tor", "A", "Sturm", 0),
                    BerichtSpieler("Tom Mayer", "T", "Torwart", 15),
                ),
            ),
            gastAufstellung = BerichtAufstellung(
                spieler = listOf(BerichtSpieler("Gast Stuermer", "A", "Sturm", 1)),
            ),
            gastSpielerStatistik = mapOf(
                "gast stuermer" to com.onlinesoccer.app.data.model.BerichtSpielerStatistik(schuesse = 4, aufsTor = 3),
            ),
            url = "test",
        )

        val heimKeeper = ElfAuswertung.kandidatenAusBericht(bericht).first { it.name == "Tom Mayer" }
        assertEquals(SpielerPosition.TOR, heimKeeper.position)
        assertEquals(2, heimKeeper.gehalteneBalle)

        // Ohne Spielerstatistik bleibt der Wert neutral (null statt 0-Paraden).
        val ohneStats = SpielBericht(
            saison = 24, zat = 5, ergebnis = "0:0",
            heim = "FC Alpha", gast = "KSV Beta", heimId = 1, gastId = 2,
            heimAufstellung = BerichtAufstellung(spieler = listOf(BerichtSpieler("TW", "T", "Torwart", 15))),
            gastAufstellung = BerichtAufstellung(spieler = emptyList()),
            url = "test",
        )
        val keeperOhneStats = ElfAuswertung.kandidatenAusBericht(ohneStats).first()
        assertNull(keeperOhneStats.gehalteneBalle)
        assertEquals(0, keeperOhneStats.auffaelligkeit)
    }

    @Test
    fun auswertung_uebernimmtBerichtNoteAusSpielerstatistik() {
        val bericht = SpielBericht(
            saison = 24, zat = 5, ergebnis = "1:0",
            heim = "FC Alpha", gast = "KSV Beta", heimId = 1, gastId = 2,
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(BerichtSpieler("Aaron Muller", "A", "Sturm", 0)),
            ),
            gastAufstellung = BerichtAufstellung(spieler = emptyList()),
            heimSpielerStatistik = mapOf(
                "aaron muller" to com.onlinesoccer.app.data.model.BerichtSpielerStatistik(note = "3.5"),
            ),
            url = "test",
        )

        val aaron = ElfAuswertung.kandidatenAusBericht(bericht).first { it.name == "Aaron Muller" }
        assertEquals(3.5, aaron.berichtNote ?: 0.0, 0.001)

        // Komma-Schreibweise wird ebenso akzeptiert; leere/unbekannte Note bleibt null.
        val mitKomma = ElfAuswertung.kandidatenAusBericht(
            bericht.copy(
                heimSpielerStatistik = mapOf(
                    "aaron muller" to com.onlinesoccer.app.data.model.BerichtSpielerStatistik(note = "2,5"),
                ),
            ),
        ).first()
        assertEquals(2.5, mitKomma.berichtNote ?: 0.0, 0.001)

        val ohneNote = ElfAuswertung.kandidatenAusBericht(
            bericht.copy(heimSpielerStatistik = mapOf("aaron muller" to com.onlinesoccer.app.data.model.BerichtSpielerStatistik(note = ""))),
        ).first()
        assertNull(ohneNote.berichtNote)
    }

    @Test
    fun auswertung_minutenBleibenNeutralOhneWechselhinweise() {
        val bericht = SpielBericht(
            saison = 24, zat = 5, ergebnis = "1:0",
            heim = "FC Alpha", gast = "KSV Beta", heimId = 1, gastId = 2,
            heimAufstellung = BerichtAufstellung(
                spieler = listOf(BerichtSpieler("Norm Spieler", "A", "Sturm", 0)),
            ),
            gastAufstellung = BerichtAufstellung(spieler = emptyList()),
            url = "test",
        )

        val spieler = ElfAuswertung.kandidatenAusBericht(bericht).first()
        assertNull(spieler.minuten)
    }

    @Test
    fun kandidatVon_RekonstruiertSpielerFuerBewertung() {
        val spieler = ElfSpieler(
            name = "Max", verein = "FC", position = SpielerPosition.STU,
            bewertung = 6.9, spielerId = 5, tore = 2, sieg = true,
        )
        val original = ElfBewertung.kandidatVon(spieler)
        assertEquals("Max", original.name)
        assertEquals(2, original.tore)
        assertTrue(original.sieg)
        assertEquals(5L, original.spielerId)
        // Transparente Neuberechnung liefert dieselbe Note.
        assertEquals(spieler.bewertung, ElfBewertung.bewerten(original).gesamt, 0.001)
    }
}