package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.AktionFeldTyp
import java.io.File
import org.junit.Assert.assertEquals
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
        assertEquals("0 Spieler", betreuung?.infoZeilen?.first()?.second)
        assertEquals("6 Trainerplätze", 6, betreuung?.infoZeilen?.size)

        val spieler = ansicht.abschnitte.first { it.titel == "Spieler-Training" }.zeilen
        assertTrue("Spieler vorhanden", spieler.size >= 20)
        val vincez = spieler.first { it.ueberschrift == "Lars Vincez" }
        assertEquals("Torwart · 33 Jahre · Opti 89.37", vincez.untertitel)
        assertEquals("---", vincez.werte.toMap()["Trainer"])
        assertEquals("---", vincez.werte.toMap()["trainierter Skill"])
        assertEquals("0.00 %", vincez.werte.toMap()["Chance"])
        assertTrue("Trainings-Schlüssel gesetzt", vincez.aktionSchluessel?.toIntOrNull() != null)
        assertEquals("Training bearbeiten", vincez.aktionTitel)
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

        val form = ansicht.abschnitte.first { it.titel == "Ins A-Team berufen" }.aktionen.single()
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