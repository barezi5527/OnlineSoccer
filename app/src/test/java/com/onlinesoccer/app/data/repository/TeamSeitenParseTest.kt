package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.AktionFeld
import com.onlinesoccer.app.data.model.AktionFeldTyp
import com.onlinesoccer.app.data.model.AktionsOption
import com.onlinesoccer.app.data.model.UebersichtZeile
import com.onlinesoccer.app.feature.team.SKILLWERT_PREFIX
import com.onlinesoccer.app.feature.team.istTrainingFormular
import com.onlinesoccer.app.feature.team.skillWertFeld
import com.onlinesoccer.app.feature.team.skillWerte
import com.onlinesoccer.app.feature.team.skillWerteAusIndex
import com.onlinesoccer.app.feature.team.trainingFormularFelder
import com.onlinesoccer.app.feature.team.zeilenAktionen
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressionstests für die nur lesenden Team-Übersichtsseiten der horizontalen
 * Menüleiste (Jugendteam … Teamübersicht) gegen echte Server-Dumps.
 */
class TeamSeitenParseTest {

    private val repo = TeamRepository(okhttp3.OkHttpClient(), ZugabgabeRepository(okhttp3.OkHttpClient()))

    private fun dump(name: String): String =
        File("src/test/resources/dumps/$name.html").readText()

    @Test
    fun jugend_liefertHinweiseUndJahrgangsgruppen() {
        val ansicht = repo.parseJugend(dump("ju"))

        assertEquals("Jugendteam", ansicht.titel)
        val hinweise = ansicht.abschnitte.firstOrNull { it.titel == "Bitte beachten" }
        assertTrue("Hinweise vorhanden", hinweise?.punkte?.size ?: 0 >= 5)
        val punkte = hinweise!!.punkte
        assertTrue("Hinweise beginnen mit der ?-Jugendspieler Regel", punkte.first().startsWith("Jugendspieler mit einem"))
        assertTrue("Hinweise enthalten die Zieh-Frist", punkte.any { it.contains("vor ZAT 72") })

        val jahrgang18 = ansicht.abschnitte.firstOrNull { it.titel == "Jahrgang Saison 18" }
        assertEquals("Jahrgang Saison 18 hat 3 Spieler", 3, jahrgang18?.zeilen?.size)
        assertTrue(
            "Keine Karte aus der Tabellenkopfzeile",
            ansicht.abschnitte.none { a -> a.zeilen.any { it.ueberschrift == "Alter Jahre · Geb. Geb." } },
        )
        val erster = jahrgang18?.zeilen?.first()
        assertEquals("18 Jahre · Geb. 12", erster?.ueberschrift)
        assertEquals("GER", erster?.untertitel)
        val werte = erster?.werte?.toMap().orEmpty()
        assertEquals("28.41", werte["Skill"])
        assertEquals("normal", werte["Talent"])
        assertTrue("Schlüssel für Zieh-Aktion gesetzt", erster?.aktionSchluessel?.toIntOrNull() != null)
        assertEquals("Ins A-Team berufen", erster?.aktionTitel)
    }

    @Test
    fun jugendscouting_liestAngeboteOhneGebotAktion() {
        val ansicht = repo.parseJugendscouting(dump("juscout"))

        assertEquals("Jugendscouting", ansicht.titel)
        val zeilen = ansicht.abschnitte.first().zeilen
        assertEquals("5 Angebote", 5, zeilen.size)
        val erster = zeilen.first()
        assertEquals("15 Jahre · Geb. 12", erster.ueberschrift)
        assertEquals("USA", erster.untertitel)
        val werte = erster.werte.toMap()
        assertEquals("25.24", werte["Skill"])
        assertEquals("18.09.2026", werte["Gebotsfrist"])
        assertEquals("3.000.000", werte["Mindestgebot"])
        assertEquals("Hoch: 25.76%", werte["Hoch"])
        assertEquals("Mittel: 64.11%", werte["Mittel"])
        assertEquals("Gering: 10.13%", werte["Gering"])
        assertTrue("Keine Gebot-Aktion", !werte.containsKey("Gebot"))
        assertEquals("286064", zeilen.first().aktionSchluessel)
        assertEquals("Gebot abgeben", zeilen.first().aktionTitel)
    }

    @Test
    fun training_liestBetreuungUndSpielerwerte() {
        val ansicht = repo.parseTraining(dump("training"))

        assertEquals("Training", ansicht.titel)
        val betreuung = ansicht.abschnitte.firstOrNull { it.titel == "Trainer-Betreuung" }
        assertEquals("Trainer 1", betreuung?.infoZeilen?.first()?.first)
        assertEquals("0", betreuung?.infoZeilen?.first()?.second)
        assertEquals("6 Trainerplätze", 6, betreuung?.infoZeilen?.size)
        assertTrue(
            "Ohne überzähligen Trainer keine Warnung (versteckte Serverzeile wird ignoriert)",
            betreuung?.punkte.isNullOrEmpty(),
        )

        val spieler = ansicht.abschnitte.first { it.titel == "Spieler-Training" }.zeilen
        assertTrue("Spieler vorhanden", spieler.size >= 20)
        val vincez = spieler.first { it.ueberschrift == "Lars Vincez" }
        assertEquals("Torwart · 33 Jahre · Opti 89.37", vincez.untertitel)
        assertEquals("T 1 60", vincez.werte.toMap()["Trainer"])
        assertEquals("STB", vincez.werte.toMap()["trainierter Skill"])
        assertEquals("58", vincez.werte.toMap()["Skillwert"])
        assertEquals("12.50 %", vincez.werte.toMap()["Chance"])
        assertTrue("Trainings-Schlüssel gesetzt", vincez.aktionSchluessel?.toIntOrNull() != null)
        assertEquals("Training bearbeiten", vincez.aktionTitel)

        val kehrer = spieler.first { it.ueberschrift == "Heiko Kehrer" }
        assertEquals("", kehrer.werte.toMap()["Skillwert"])
    }

    @Test
    fun training_warnungBeiMehrAlsFuenfBetreutenSpielern() {
        val html = """
            <html><body>
            <table border="0" width="405">
            <tr><td>Trainer 1: 6 Spieler</td><td>Trainer 2: 5 Spieler</td><td>Trainer 3: 0 Spieler</td></tr>
            <tr><td>Trainer 4: 0 Spieler</td><td>Trainer 5: 0 Spieler</td><td>Trainer 6: 0 Spieler</td></tr>
            <tr style="display:none"><td class="STU" colspan="3">Ein oder mehr Trainer betreuen mehr als 5 Spieler! Bitte reduziere diese Anzahl, ansonsten werden nur die ersten 5 Spieler von diesem Trainer trainiert!</td></tr>
            </table>
            </body></html>
        """.trimIndent()

        val betreuung = repo.parseTraining(html).abschnitte
            .firstOrNull { it.titel == "Trainer-Betreuung" }

        assertEquals("Trainer 1", betreuung?.infoZeilen?.first()?.first)
        assertEquals("6", betreuung?.infoZeilen?.first()?.second)
        assertTrue(
            "Warnung sichtbar, sobald ein Trainer mehr als 5 Spieler betreut",
            betreuung?.punkte?.any { it.contains("mehr als 5 Spieler") } == true,
        )
    }

    @Test
    fun trainer_liefertStabUndSumme() {
        val ansicht = repo.parseTrainer(dump("trainer"))

        assertEquals("Trainer", ansicht.titel)
        val summe = ansicht.abschnitte.firstOrNull { it.titel == "Übersicht des Trainerstabes" }
        assertEquals("60.000 Euro", summe?.infoZeilen?.first()?.second)

        val stab = ansicht.abschnitte.first { it.titel == "Trainerstab" }.zeilen
        assertEquals("6 Trainer", 6, stab.size)
        val erster = stab.first()
        assertEquals("Trainer 1", erster.ueberschrift)
        assertEquals("Trainer 01", erster.untertitel)
        assertEquals("10.000", erster.werte.toMap()["Gehalt"])
        assertEquals("0 ZAT", erster.werte.toMap()["Vertrag"])
        assertEquals("1", erster.aktionSchluessel)
        assertEquals("Trainer einstellen", erster.aktionTitel)
    }

    @Test
    fun vertraege_liestVerlaengerungsangebote() {
        val ansicht = repo.parseVertraegeVerlaengern(dump("vt"))

        assertEquals("Verträge verlängern", ansicht.titel)
        val zeilen = ansicht.abschnitte.first().zeilen
        assertTrue("Spieler vorhanden", zeilen.size >= 20)
        val kehrer = zeilen.first { it.ueberschrift == "Heiko Kehrer" }
        assertEquals("31 Jahre · GER", kehrer.untertitel)
        val werte = kehrer.werte.toMap()
        assertEquals("5.934", werte["Gehalt"])
        assertEquals("12 Monate", werte["Laufzeit"])
        assertEquals("36.65", werte["Skillschnitt"])
        assertEquals("55.22", werte["Opt. Skill"])
        assertEquals("5.550", werte["24 Monate"])
        assertEquals("4.935", werte["36 Monate"])
        assertEquals("4.187", werte["48 Monate"])
        assertEquals("3.553", werte["60 Monate"])
        assertEquals("115904", zeilen.first { it.ueberschrift == "Flavio Zurlinden" }.aktionSchluessel)
        assertEquals("Vertrag verlängern", zeilen.first { it.ueberschrift == "Flavio Zurlinden" }.aktionTitel)
    }

    @Test
    fun stadion_liefertZustandUndAusbauoptionen() {
        val ansicht = repo.parseStadion(dump("stadion"))

        assertEquals("Stadionausbau", ansicht.titel)
        val zustand = ansicht.abschnitte.first { it.titel == "Aktueller Stadion-Zustand" }.infoZeilen.toMap()
        assertEquals("90.500", zustand["Fassungsvermögen"])
        assertEquals("90.500", zustand["Sitzplätze"])
        assertEquals("90.500", zustand["davon überdacht (Sitz)"])
        assertEquals("0", zustand["Stehplätze"])
        assertEquals("0", zustand["davon überdacht (Steh)"])
        assertEquals("Multimediawürfel", zustand["Anzeigetafel"])
        assertEquals("installiert", zustand["Rasenheizung"])
        assertEquals("10.982.835", zustand["Kontostand"])

        val plan = ansicht.abschnitte.first { it.titel == "Aktueller Stadion-Zustand" }.stadionPlan
        assertTrue("Stadionplan-Daten vorhanden", plan != null)
        assertEquals(90_500, plan?.sitzplaetze)
        assertEquals(0, plan?.stehplaetze)
        assertEquals(90_500, plan?.sitzUeberdacht)
        assertEquals(0, plan?.stehUeberdacht)
        assertEquals(90_500, plan?.kapazitaet)

        val optionen = ansicht.abschnitte.first { it.titel == "Ausbaumöglichkeiten" }.zeilen
        assertEquals("8 Ausbauoptionen", 8, optionen.size)
        val steh = optionen.first()
        assertEquals("Stehplätze bauen", steh.ueberschrift)
        assertEquals("2.859 pro Platz", steh.untertitel)
        assertEquals("10.000", steh.werte.toMap()["Maximum"])
    }

    @Test
    fun stadion_liestAusstattungAlsPremiumDaten() {
        val ansicht = repo.parseStadion(dump("stadion_ausbau"))

        val zustand = ansicht.abschnitte.first { it.titel == "Aktueller Stadion-Zustand" }
        val plan = zustand.stadionPlan
        assertNotNull("Stadionplan vorhanden", plan)
        assertEquals("Multimediawürfel als Anzeigetafel", "Multimediawürfel", plan!!.anzeigetafel)
        assertTrue("Rasenheizung installiert", plan.rasenheizung)
        assertTrue("Premium-Ausstattung erkannt", plan.hatPremiumAusstattung)
    }

    @Test
    fun stadion_einfacheAnzeigetafelKeinPremium() {
        // Einfache „Anzeigetafel" + Rasenheizung zählt nicht als Premium.
        val ansicht = repo.parseStadion(dump("stadion_mit_bereichen"))

        val zustand = ansicht.abschnitte.firstOrNull { it.titel == "Aktueller Stadion-Zustand" }
        val plan = zustand?.stadionPlan
        assertNotNull("Stadionplan vorhanden", plan)
        assertEquals("Einfache Anzeigetafel bleibt bestehen", "Anzeigetafel", plan!!.anzeigetafel)
        assertTrue("Rasenheizung installiert", plan.rasenheizung)
        assertFalse("Einfache Anzeigetafel → kein Premium", plan.hatPremiumAusstattung)
    }

    @Test
    fun stadion_mitLaufendemAusbau_zeigtHinweisAlsErstenAbschnitt() {
        val ansicht = repo.parseStadion(dump("stadion_ausbau"))

        assertEquals("Stadionausbau", ansicht.titel)
        val ausbau = ansicht.abschnitte.first()
        assertEquals("Laufender Stadionausbau", ausbau.titel)
        assertTrue("Baufirma als Merkpunkt", ausbau.punkte.any { it.contains("Flott & Teuer") })
        assertTrue("Neu gebaute Plätze", ausbau.punkte.any { it.startsWith("1000 überdachte Sitzplätze") })
        assertTrue("Reduzierte Stadionkapazität", ausbau.punkte.any { it.startsWith("Während des Ausbaus reduziert sich") })
        assertTrue("Zustand weiterhin vorhanden", ansicht.abschnitte.any { it.titel == "Aktueller Stadion-Zustand" })
        assertTrue("Ausbaumöglichkeiten weiterhin vorhanden", ansicht.abschnitte.any { it.titel == "Ausbaumöglichkeiten" })
    }

    @Test
    fun stadion_ohneLaufendenAusbau_zeigtKeinenHinweisAbschnitt() {
        val ansicht = repo.parseStadion(dump("stadion"))

        assertTrue(
            "Ohne laufenden Ausbau kein Hinweis-Abschnitt",
            ansicht.abschnitte.none { it.titel == "Laufender Stadionausbau" },
        )
    }

    @Test
    fun konto_liefertBuchungen() {
        val ansicht = repo.parseKonto(dump("ka"))

        assertEquals("Kontoauszug", ansicht.titel)
        val zeilen = ansicht.abschnitte.first().zeilen
        assertTrue("Buchungen vorhanden", zeilen.isNotEmpty())
        val zat3 = zeilen.first()
        assertEquals("Abrechnung ZAT 3", zat3.ueberschrift)
        assertEquals("08.09.2026", zat3.untertitel)
        assertEquals("238.500", zat3.werte.toMap()["Eingang"])
        assertEquals(null, zat3.werte.toMap()["Ausgang"])
        assertEquals("10.982.835", zat3.werte.toMap()["Kontostand"])

        val transfer = zeilen.first { it.ueberschrift == "Schnelltransfer von Abel Eorlson" }
        assertEquals("1.214.361", transfer.werte.toMap()["Eingang"])
        assertEquals("7.686.417", transfer.werte.toMap()["Kontostand"])
    }

    @Test
    fun steuer_liefertBargeldUndGesamtBloecke() {
        val ansicht = repo.parseSteuer(dump("steuer"))

        assertEquals("Steuerübersicht", ansicht.titel)
        val bargeld = ansicht.abschnitte.first { it.titel == "Bargeld" }.infoZeilen.toMap()
        assertEquals("6.472.056", bargeld["Saisonstart"])
        assertEquals("10.982.835", bargeld["Derzeit"])
        assertEquals("4.510.779", bargeld["Zwischensumme"])

        val gesamt = ansicht.abschnitte.first { it.titel == "Gesamt" }.infoZeilen.toMap()
        assertEquals("0", gesamt["Transfers"])
        assertEquals("0", gesamt["Transfermarkt"])
        assertEquals("0", gesamt["Versteigerungsmarkt"])
        assertEquals("4.510.779", gesamt["Bargeld"])
        assertEquals("4.510.779", gesamt["Endergebnis"])
        assertEquals("225.539", gesamt["Steuern"])
    }

    @Test
    fun teamUebersicht_bautKaderkarten() {
        val ansicht = repo.parseTeamUebersicht(dump("showteam_s0"))

        assertEquals("Teamübersicht", ansicht.titel)
        val zeilen = ansicht.abschnitte.first().zeilen
        assertTrue("Kaderkarten vorhanden", zeilen.size >= 20)
        val vincez = zeilen.first { it.ueberschrift == "Lars Vincez" }
        assertEquals("Torwart · 33 Jahre · Nr. 1", vincez.untertitel)
        assertEquals("61,4", vincez.werte.toMap()["Skill"])
        assertEquals("89,3", vincez.werte.toMap()["Opti"])
        assertEquals("97", vincez.werte.toMap()["Fit"])
        assertEquals("89", vincez.werte.toMap()["Mor"])
    }

    @Test
    fun jugend_bautZieheInsATeamAktion() {
        val ansicht = repo.parseJugend(dump("ju"), "https://os.ongapo.com/ju.php")

        // Dasselbe Formular wird von mehreren Jahrgangsgruppen geteilt.
        val form = ansicht.abschnitte.flatMap { it.aktionen }.distinct().single()
        assertEquals("https://os.ongapo.com/ju.php", form.ziel)
        val feld = form.felder.single()
        assertEquals("ziehmich", feld.name)
        assertEquals(AktionFeldTyp.RADIO, feld.typ)
        assertTrue("Radiogruppe hat Spieleroptionen", feld.optionen.size >= 5)
        assertTrue("Optionen sind Spieler-IDs", feld.optionen.all { it.wert.toIntOrNull() != null })
        val button = form.buttons.single()
        assertEquals("ziehen", button.name)
        assertTrue(button.text.contains("A-Team"))
    }

    /**
     * Jeder ziehbare Jugendspieler braucht sein eigenes ⋮-Menü. `SeiteScreen`
     * verknüpft Zeile und Formular nur innerhalb desselben Abschnitts – lag das
     * Berufungsformular in einem eigenen Abschnitt, bekam keine Zeile eine Aktion.
     */
    @Test
    fun jugend_berufungLiegtInDerJahrgangsgruppeDesSpielers() {
        val ansicht = repo.parseJugend(dump("ju"), "https://os.ongapo.com/ju.php")

        val jahrgang18 = ansicht.abschnitte.first { it.titel == "Jahrgang Saison 18" }
        assertEquals(1, jahrgang18.aktionen.size)
        jahrgang18.zeilen.forEach { zeile ->
            val aktionen = zeilenAktionen(zeile, jahrgang18.aktionen)
            assertEquals(
                "Zeile „${zeile.ueberschrift}“ ohne Berufungsaktion",
                listOf("Ins A-Team berufen"),
                aktionen.map { it.titel },
            )
        }
        // Das Formular gehört dem Jahrgang, nicht einer Einzelzeile: es liefert
        // keine Zeilen-Zuordnung und damit keinen Vollformular-Abschnitt.
        assertTrue(jahrgang18.aktionen.single().zeilen.isEmpty())
    }

    /** Zu junge Spieler haben kein `ziehmich`-Feld – dort darf keine Aktion erscheinen. */
    @Test
    fun jugend_ohneZiehbarenSpielerLiefertKeineAktion() {
        val ansicht = repo.parseJugend(dump("ju"), "https://os.ongapo.com/ju.php")

        val jahrgang20 = ansicht.abschnitte.first { it.titel == "Jahrgang Saison 20" }
        assertTrue(jahrgang20.zeilen.isNotEmpty())
        assertTrue(jahrgang20.zeilen.none { !it.aktionSchluessel.isNullOrBlank() })
        assertTrue("Zu junge Jahrgänge ohne Berufungsformular", jahrgang20.aktionen.isEmpty())
        jahrgang20.zeilen.forEach { zeile ->
            assertTrue(zeilenAktionen(zeile, jahrgang20.aktionen).isEmpty())
        }
    }

    /**
     * Ohne eigenes Formular gibt es auch keine zweite Radioliste am Seitenende –
     * die Berufung läuft ausschließlich über das ⋮-Menü der Zeile.
     */
    @Test
    fun jugend_zeigtKeineZusaetzlicheRadioliste() {
        val ansicht = repo.parseJugend(dump("ju"), "https://os.ongapo.com/ju.php")

        assertFalse(
            "Abschnitt mit reiner Radioliste vorhanden",
            ansicht.abschnitte.any { it.titel == "Ins A-Team berufen" },
        )
        assertTrue(
            "Jede Radiogruppe gehört zu einer Gruppe mit Spielern",
            ansicht.abschnitte.all { abschnitt ->
                abschnitt.aktionen.all { form ->
                    form.felder.none { it.typ == AktionFeldTyp.RADIO } || abschnitt.zeilen.isNotEmpty()
                }
            },
        )
    }

    /** Alle vier Unterreiter sind vorhanden; Teamübersicht (page 1) ist aktiv. */
    @Test
    fun jugend_liefertUnterreiterMitAktiverTeamuebersicht() {
        val ansicht = repo.parseJugend(dump("ju"), "https://os.ongapo.com/ju.php")

        assertEquals(
            listOf("Teamübersicht", "Spielereinzelwerte", "Opt. Skill", "Optionen"),
            ansicht.tabs.map { it.label },
        )
        assertEquals(listOf("ju.php", "ju.php?page=2", "ju.php?page=3", "ju.php?page=4"), ansicht.tabs.map { it.path })
        assertEquals(ansicht.tabs.first { it.path == "ju.php" }.aktiv, true)
    }

    @Test
    fun jugendEinzelwerte_liefertSkillsAlsChips() {
        val ansicht = repo.parseJugendEinzelwerte(dump("ju_einzelwerte"))

        val jahrgang18 = ansicht.abschnitte.first { it.titel == "Jahrgang Saison 18" }
        assertEquals(3, jahrgang18.zeilen.size)
        val erster = jahrgang18.zeilen.first()
        assertEquals("18 Jahre · GER", erster.ueberschrift)
        val werte = erster.werte.toMap()
        assertEquals("30", werte["SCH"])
        assertEquals("12", werte["BAK"])
        assertEquals("61", werte["EIN"])
        assertTrue("17 Einzelskills als Chips", werte.size == 17)
        // Keine Berufungsaktion auf der Einzelwerte-Seite.
        assertTrue(ansicht.abschnitte.all { it.aktionen.isEmpty() })
        assertTrue(ansicht.tabs.first { it.path == "ju.php?page=2" }.aktiv)
    }

    @Test
    fun jugendOptSkill_liefertSkillUndPositionen() {
        val ansicht = repo.parseJugendOptSkill(dump("ju_opt_skill"))

        val jahrgang19 = ansicht.abschnitte.first { it.titel == "Jahrgang Saison 19" }
        val uzb = jahrgang19.zeilen.first { it.ueberschrift == "18 Jahre · UZB" }
        val werte = uzb.werte.toMap()
        assertEquals("30.47", werte["Skill"])
        assertEquals("43.78", werte["TOR"])
        // Spieler ohne optimierten Positionswert führen diese Position nicht.
        val jahrgang18 = ansicht.abschnitte.first { it.titel == "Jahrgang Saison 18" }
        val ger = jahrgang18.zeilen.first { it.ueberschrift == "18 Jahre · GER" }
        assertEquals("28.53", ger.werte.toMap()["Skill"])
        assertFalse(ger.werte.toMap().containsKey("TOR"))
        assertTrue(ansicht.tabs.first { it.path == "ju.php?page=3" }.aktiv)
    }

    @Test
    fun jugendOptionen_liefertFoerderungUndSchranke() {
        val ansicht = repo.parseJugendOptionen(dump("ju_optionen"), "https://os.ongapo.com/ju.php?page=4")

        assertEquals(listOf("Allgemeine Optionen", "Jugendschranke"), ansicht.abschnitte.map { it.titel })

        val foerderung = ansicht.abschnitte.first { it.titel == "Allgemeine Optionen" }.aktionen.single()
        assertEquals("https://os.ongapo.com/ju.php?page=4", foerderung.ziel)
        val select = foerderung.felder.single()
        assertEquals("foerderung", select.name)
        assertEquals(AktionFeldTyp.AUSWAHL, select.typ)
        assertEquals("500", select.standard)
        assertEquals("10000", select.optionen.last().wert)

        val schranke = ansicht.abschnitte.first { it.titel == "Jugendschranke" }.aktionen.single()
        val radio = schranke.felder.single()
        assertEquals("schranke", radio.name)
        assertEquals(AktionFeldTyp.RADIO, radio.typ)
        assertEquals(listOf("1", "2", "0"), radio.optionen.map { it.wert })
        assertEquals("0", radio.standard)
        // Absende-Buttons tragen keinen Namen – es wird nur der Feldwert übertragen.
        assertTrue(foerderung.buttons.single().name == null)
        assertTrue(schranke.buttons.single().name == null)
        assertTrue(ansicht.tabs.first { it.path == "ju.php?page=4" }.aktiv)
    }

    /** `ju.php?page=2` muss weiterhin als Jugend-Seite erkannt werden (Query-Strip). */
    @Test
    fun jugend_mitQueryWirdAlsJugendSeiteErkannt() {
        val ansicht = repo.parseJugendSeite("ju.php?page=3", dump("ju_opt_skill"), "https://os.ongapo.com/ju.php?page=3")

        assertEquals(
            listOf("Teamübersicht", "Spielereinzelwerte", "Opt. Skill", "Optionen"),
            ansicht.tabs.map { it.label },
        )
        assertTrue("Opt. Skill ist aktiv", ansicht.tabs.first { it.path == "ju.php?page=3" }.aktiv)
        assertTrue("Opt-Skill-Zeilen vorhanden", ansicht.abschnitte.isNotEmpty())
    }

    @Test
    fun jugendscouting_bautGebotZeilenMitZiel() {
        val ansicht = repo.parseJugendscouting(dump("juscout"), "https://os.ongapo.com/juscout.php")

        val form = ansicht.abschnitte.first().aktionen.single()
        assertEquals(5, form.zeilen.size)
        val erste = form.zeilen.first()
        assertEquals("https://os.ongapo.com/juscout.php?g=286064", erste.ziel)
        assertEquals("Gebot abgeben", erste.button?.text)
    }

    @Test
    fun gebotFormular_liestScoutingPopup() {
        val form = repo.parseGebotFormular(dump("juscout_g"))

        assertEquals("https://os.ongapo.com/juscout.php?g=286065", form.ziel)
        val ersatz = form.felder.first { it.name == "exchange" }
        assertEquals(AktionFeldTyp.AUSWAHL, ersatz.typ)
        assertTrue("Ersatzspieler wählbar", ersatz.optionen.isNotEmpty())
        val geld = form.felder.first { it.name == "Geld" }
        assertEquals(AktionFeldTyp.NUMMER, geld.typ)
        assertEquals("Gebot", form.buttons.single().name)
    }

    @Test
    fun training_bautSpeicherformulareUndTrainingsformular() {
        val ansicht = repo.parseTraining(dump("training"), "https://os.ongapo.com/training.php")

        val speicher = ansicht.abschnitte.first { it.titel == "Trainingsspeicher" }.aktionen
        assertEquals("4 Speicherfunktionen", 4, speicher.size)
        val speicherAbschnitt = ansicht.abschnitte.first { it.titel == "Trainingsspeicher" }
        assertTrue(
            "Speicher-Warnung vorhanden",
            speicherAbschnitt.punkte.any { it.contains("nicht übernommen") },
        )
        assertEquals(
            listOf("Training laden", "Training speichern unter", "Neue Training-Speicherung", "Training löschen"),
            speicher.map { it.titel },
        )
        assertEquals("Bestehende Einstellung", speicher.first().felder.single().label)

        val form = ansicht.abschnitte.first { it.titel == "Spieler-Training" }.aktionen.single()
        assertTrue("Zeilenselekte je Spieler", form.zeilen.isNotEmpty())
        val erste = form.zeilen.first()
        val trainer = erste.felder.first { it.name.startsWith("tr1") }
        assertEquals(AktionFeldTyp.AUSWAHL, trainer.typ)
        assertEquals("Trainer", trainer.label)
        val skill = erste.felder.first { it.name.startsWith("tr2") }
        assertEquals(AktionFeldTyp.AUSWAHL, skill.typ)
        assertEquals("Trainierter Skill", skill.label)
        assertEquals(listOf("trainingspeichern"), form.buttons.map { it.name })
    }

    /**
     * Regression: Wird nur die geänderte Zeile gesendet, entfernt der Server das
     * Training aller übrigen Spieler. Der POST muss daher alle `tr1`/`tr2`-Felder
     * enthalten – auch die unveränderten.
     */
    @Test
    fun training_formularSendetAlleSpielerfelder() {
        val ansicht = repo.parseTraining(dump("training"), "https://os.ongapo.com/training.php")
        val form = ansicht.abschnitte.first { it.titel == "Spieler-Training" }.aktionen.single()
        assertTrue(istTrainingFormular(form))

        val werte = form.zeilen.flatMap { it.felder }.associate { it.name to it.standard }.toMutableMap()
        val erster = form.zeilen.first()
        val anderer = form.zeilen[1]
        werte[erster.felder.first { it.name.startsWith("tr1") }.name] = "0"
        werte[anderer.felder.first { it.name.startsWith("tr1") }.name] = "2"

        val felder = trainingFormularFelder(form, werte)

        assertEquals("Zwei Auswahlfelder je Spieler plus Speichern-Button", form.zeilen.size * 2 + 1, felder.size)
        form.zeilen.forEach { zeile ->
            zeile.felder.forEach { feld ->
                assertTrue(
                    "Feld ${feld.name} fehlt im POST",
                    felder.any { it.first == feld.name },
                )
            }
        }
        assertTrue(
            "Unveränderte Spielerwahl bleibt im POST",
            felder.any { it.first == anderer.felder.first { feld -> feld.name.startsWith("tr1") }.name && it.second == "2" },
        )
        assertEquals("trainingspeichern" to "Trainingseinstellung speichern", felder.last())
    }

    @Test
    fun training_skillwerteAusEinzelwertenWerdenAngezeigt() {
        val feld = skillWertFeld("97944")
        assertEquals(SKILLWERT_PREFIX + "97944", feld)

        // Der s=2-String steht in Spaltenreihenfolge (inkl. FUQ/ERF/WID/SEL/DIS/EIN).
        val werte = skillWerteAusIndex(
            "SCH 60 BAK 74 KOB 68 ZWK 55 DEC 71 GES 73 FUQ 1 ERF 2 AGG 82 " +
                "PAS 67 AUS 79 UEB 68 WID 3 SEL 4 DIS 5 ZUV 84 EIN 6",
            listOf("SCH", "BAK", "KOB", "ZWK", "DEC", "GES", "FUQ", "ERF", "AGG", "PAS", "AUS", "UEB", "WID", "SEL", "DIS", "ZUV", "EIN"),
        )
        assertEquals("60", werte["SCH"])
        assertEquals("55", werte["ZWK"])
        assertEquals("82", werte["AGG"])
        assertEquals("84", werte["ZUV"])
    }

    /**
     * Regression: `AuswahlFeld` schlägt die Zusatzanzeige über `option.wert` nach.
     * Gleichzeitig ist der Optionswert die **Position** des Skills in der
     * `s=2`-Spaltenreihenfolge (Torwart und Feldspieler nutzen dieselben Spalten).
     * Belegt an Bonciucov: Option `STS` (Wert 2) → Spalte 2 = `BAK` = 46 = die
     * Skillspalte der Trainingsseite.
     */
    @Test
    fun training_skillwerteWerdenProOptionswertZugeordnet() {
        val zeile = UebersichtZeile(
            ueberschrift = "Evghenii Bonciucov",
            untertitel = "Torwart · 23 Jahre · Opti 73.33",
            werte = listOf(
                skillWertFeld("149061") to
                    "SCH 26 BAK 46 KOB 73 ZWK 76 DEC 74 GES 85 FUQ 8 ERF 18 AGG 29 " +
                    "PAS 12 AUS 23 UEB 30 WID 30 SEL 86 DIS 82 ZUV 23 EIN 27",
            ),
        )
        val feld = AktionFeld(
            name = "tr2149061",
            label = "Trainierter Skill",
            optionen = listOf(
                AktionsOption("0", "---"),
                AktionsOption("1", "ABS"),
                AktionsOption("2", "STS"),
                AktionsOption("3", "FAN"),
                AktionsOption("4", "STB"),
                AktionsOption("7", "AGG"),
                AktionsOption("11", "ZUV"),
            ),
        )

        val werte = skillWerte(zeile, feld)

        // Im Drilldown steht nur der Wert; das Kürzel kommt aus dem Options-Label.
        assertEquals("26", werte["1"])
        assertEquals("46", werte["2"])
        assertEquals("76", werte["4"])
        assertEquals("29", werte["7"])
        assertEquals("23", werte["11"])
        assertFalse("Unbekannte Option bleibt ohne Zusatz", werte.containsKey("0"))
    }

    @Test
    fun trainer_bautEinstellFormulare() {
        val ansicht = repo.parseTrainer(dump("trainer"), "https://os.ongapo.com/trainer.php")

        val stab = ansicht.abschnitte.first { it.titel == "Trainerstab" }
        assertEquals("6 Formulare", 6, stab.aktionen.size)
        val erste = stab.aktionen.first()
        assertEquals("1", erste.felder.first { it.name == "trainer" }.standard)
        assertEquals("einstellen als Trainer 1", erste.buttons.single().text)
        val skill = erste.felder.first { it.name == "skill" }
        assertEquals(AktionFeldTyp.AUSWAHL, skill.typ)
        assertTrue("Skillwerte vorhanden", skill.optionen.size >= 10)
        val dauer = erste.felder.first { it.name == "dauer" }
        assertEquals("96", dauer.optionen.last().wert)
        assertEquals("6", dauer.standard)
    }

    @Test
    fun trainer_parstFeuernUndEinstellenAusJeweiligerZeile() {
        val ansicht = repo.parseTrainer(dump("trainer_mit_feuern"), "https://os.ongapo.com/trainer.php")

        val stab = ansicht.abschnitte.first { it.titel == "Trainerstab" }
        assertEquals(listOf("Feuern", "Trainer einstellen"), stab.zeilen.map { it.aktionTitel })
        assertEquals(2, stab.aktionen.size)

        val feuern = stab.aktionen.first()
        assertEquals("1", feuern.felder.single().standard)
        assertEquals("feuern", feuern.buttons.single().name)
        assertEquals("Trainer 1 feuern", feuern.buttons.single().text)

        val einstellen = stab.aktionen.last()
        assertEquals("2", einstellen.felder.first().standard)
        assertEquals("6", einstellen.felder.first { it.name == "dauer" }.standard)
    }

    @Test
    fun trainer_liestServerKostenhinweisBeimEinstellen() {
        val vorschau = repo.parseTrainerAktionVorschau(
            dump("trainer_einstell_bestaetigung"),
            listOf("trainer" to "2", "skill" to "62.5", "dauer" to "6", "einstellen" to "einstellen als Trainer 2"),
        )

        assertEquals(
            "Ein Trainer mit dem Skill Trainer 02 würde dich über 6 ZATs 38.307 Euro pro Abrechnung kosten.",
            vorschau?.meldung,
        )
        assertEquals(
            listOf("dauer" to "6", "skill" to "62.5", "trainer" to "2", "te" to "einstellen"),
            vorschau?.felder,
        )
        assertEquals(
            null,
            repo.parseTrainerAktionVorschau(
                dump("trainer"),
                listOf("trainer" to "2", "skill" to "62.5", "dauer" to "6", "einstellen" to "einstellen als Trainer 2"),
            ),
        )
    }

    @Test
    fun trainer_liestServerAbfindungUndFeuerbestaetigung() {
        val vorschau = repo.parseTrainerAktionVorschau(
            dump("trainer_feuer_bestaetigung"),
            listOf("trainer" to "2", "feuern" to "Trainer 2 feuern"),
        )

        assertEquals("Es kostet dich 12.345 Euro den Trainer zu feuern.", vorschau?.meldung)
        assertEquals(listOf("trainer" to "2", "tf" to "Feuern!"), vorschau?.felder)
    }

    @Test
    fun vertraege_bautVerlaengerungsFormular() {
        val ansicht = repo.parseVertraegeVerlaengern(dump("vt"), "https://os.ongapo.com/vt.php")

        val form = ansicht.abschnitte.first().aktionen.single()
        assertEquals("https://os.ongapo.com/vt.php", form.ziel)
        assertEquals("1", form.felder.first { it.name == "update" }.standard)
        assertTrue("Verlängerbare Spieler vorhanden", form.zeilen.isNotEmpty())
        val zurlinden = form.zeilen.first { it.bezeichnung == "Flavio Zurlinden" }
        val radio = zurlinden.felder.single()
        assertEquals("gehalt[115904]", radio.name)
        assertEquals(AktionFeldTyp.RADIO, radio.typ)
        assertEquals(
            listOf("24 Monate", "36 Monate"),
            radio.optionen.map { it.label },
        )
        assertEquals("vertragsauswahl", form.buttons.single().name)
        assertTrue("Nicht verlängerbare Spieler haben kein Formular", form.zeilen.none { it.schluessel == "104203" })
    }

    @Test
    fun stadion_mitBereichen_liestTribuenen() {
        val ansicht = repo.parseStadion(dump("stadion_mit_bereichen"))

        val plan = ansicht.abschnitte.first { it.titel == "Aktueller Stadion-Zustand" }.stadionPlan
        assertTrue("Stadionplan-Daten vorhanden", plan != null)
        assertEquals("Fassungsvermögen vom Server übernommen", 45_000, plan?.fassungsvermoegen)
        assertEquals(37_000, plan?.sitzplaetze)
        assertEquals(8_000, plan?.stehplaetze)

        val bereiche = plan?.bereiche.orEmpty()
        assertEquals("4 Tribünen erkannt", 4, bereiche.size)
        val nord = bereiche.first { it.name == "Nordtribüne" }
        assertEquals(15_000, nord.sitzplaetze)
        val sued = bereiche.first { it.name == "Südtribüne" }
        assertEquals(10_000, sued.sitzplaetze)
        val ost = bereiche.first { it.name == "Osttribüne" }
        assertEquals(8_000, ost.sitzplaetze)
        val west = bereiche.first { it.name == "Westtribüne" }
        assertEquals(12_000, west.sitzplaetze)
    }

    @Test
    fun stadion_ohneBereiche_liefertLeereBereichsliste() {
        val ansicht = repo.parseStadion(dump("stadion"))

        assertEquals(0, ansicht.abschnitte.first { it.titel == "Aktueller Stadion-Zustand" }.stadionPlan?.bereiche?.size)
        assertEquals(90_500, ansicht.abschnitte.first { it.titel == "Aktueller Stadion-Zustand" }.stadionPlan?.fassungsvermoegen)
    }

    @Test
    fun stadion_bautAusbauFormular() {
        val ansicht = repo.parseStadion(dump("stadion"), "https://os.ongapo.com/osneu/stadion")

        val form = ansicht.abschnitte.first { it.titel == "Ausbaumöglichkeiten" }.aktionen.single()
        assertEquals("https://os.ongapo.com/osneu/stadion", form.ziel)
        assertEquals("doAngebot", form.felder.first { it.name == "action" }.standard)
        val listNamen = listOf("NeuSteh", "NeuSitz", "NeuUSteh", "NeuUSitz", "USteh", "USitz", "StehZuSitz", "UStehZuSitz")
        assertEquals("Alle Umbau-Felder vorhanden", listNamen, form.felder.filter { it.typ == AktionFeldTyp.NUMMER }.map { it.name })
        assertTrue(form.felder.any { it.name == "tafel" })
        assertTrue(form.felder.any { it.name == "heizung" })
        assertEquals("Angebote einholen", form.buttons.single().text)
    }
}
