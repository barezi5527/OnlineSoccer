package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.Aufstellung
import com.onlinesoccer.app.data.model.AufstellungSlot
import com.onlinesoccer.app.data.model.AufstellungSpieler
import com.onlinesoccer.app.data.model.SpielerPosition
import com.onlinesoccer.app.data.model.ZugabgabeElementTyp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressionstests für die klassische Zugabgabe (p=1 Aktionen, p=2 Einstellungen)
 * gegen echte Server-Dumps (app/src/test/resources/dumps/{za0,za1,za2,za1_i1,za1_i6}.html).
 */
class ZugabgabeRepositoryParseTest {

    private val repo = ZugabgabeRepository(okhttp3.OkHttpClient())

    private fun dump(name: String): String =
        File("src/test/resources/dumps/$name.html").readText()

    @Test
    fun kopfinfo_liestZatUndStatus() {
        val info = repo.parseKopfinfo(dump("za1"))
        assertNotNull(info)
        assertEquals(3, info.zat)
        assertEquals("Ungültig", info.status)
        assertTrue(!info.gueltig)
    }

    @Test
    fun aktionen_optionenListe() {
        val formular = repo.parseFormular(dump("za1_i1"), ZugabgabeElementTyp.EINWECHSLUNG)
        assertNotNull(formular)
        assertEquals(ZugabgabeElementTyp.EINWECHSLUNG, formular.typ)
        assertTrue("Spielerliste vorhanden", formular.spieler.isNotEmpty())
        assertTrue("Einwechselspieler-ID bekannt", formular.spieler.any { it.id == "111260" })
        assertTrue("Minuten 1..90", formular.minuten.size >= 90)
        assertTrue("Abhängigkeit vorhanden", formular.abhaengigkeiten.any { it.id == "0" && it.label.contains("Immer") })
        assertTrue("Zeilen A..O/Spalten 1..11", formular.positionsZeilen.any { it.id == "A" } && formular.positionsSpalten.any { it.id == "11" })
        assertTrue("Sonderplatz vorhanden (Torwart/Karten)", formular.positionsSonder.any { it.id == "T" })
    }

    @Test
    fun einsatz_werteStufen() {
        val formular = repo.parseFormular(dump("za1_i2"), ZugabgabeElementTyp.EINSATZ)
        assertTrue("Einsatz 50..150%", formular.werte.any { it.label == "100%" })
        assertTrue("standard 100% vorausgewählt", formular.werte.any { it.id == "3" })
    }

    @Test
    fun haerte_werteHaeufig() {
        val formular = repo.parseFormular(dump("za1_i3"), ZugabgabeElementTyp.HAERTE)
        assertTrue("Härte-Stufen", formular.werte.map { it.label }.containsAll(listOf("Fairplay", "Vorsichtig", "Normal", "Hart", "Brutal")))
    }

    @Test
    fun spielweise_werteStufen() {
        val formular = repo.parseFormular(dump("za1_i4"), ZugabgabeElementTyp.SPIELWEISE)
        assertTrue("Spielweise-Stufen", formular.werte.any { it.label == "Normal" })
    }

    @Test
    fun manndeckung_gegenspielerUndEigenerSpieler() {
        val formular = repo.parseFormular(dump("za1_i6"), ZugabgabeElementTyp.MANNDECKUNG)
        assertTrue("Gegner-Kader vorhanden (Position wird übernommen)", formular.gegenspieler.any { it.id == "-1" })
        assertTrue("Eigene Spieler für Manndecker", formular.spieler.isNotEmpty())
    }

    @Test
    fun einstellungen_spielerAuswahlOhneMinute() {
        val formular = repo.parseFormular(dump("za2_i8"), ZugabgabeElementTyp.KAPITAEN)
        assertTrue("Kapitän = reine Spielerauswahl", formular.typ.istSpielerAuswahl)
        assertTrue("Spieler-IDs", formular.spieler.any { it.id == "111260" })
        assertTrue("keine Minute", formular.minuten.isEmpty())
    }

    @Test
    fun taktikAbwehr_werteKatalog() {
        val formular = repo.parseFormular(dump("za2_i16"), ZugabgabeElementTyp.TAKTIK_ABWEHR)
        assertTrue("Abseitsfalle enthalten", formular.werte.any { it.label == "Abseitsfalle" })
        assertTrue("Minute als Feld vorhanden", formular.minuten.isNotEmpty())
    }

    @Test
    fun aktionenseite_ohneExistingElemente() {
        val seite = repo.parseElementeSeite(dump("za1"), 1)
        assertNotNull(seite.kopfinfo)
        assertEquals(3, seite.kopfinfo!!.zat)
        assertTrue("keine bestehenden Aktionen erwartet (Testkonto leer)", seite.elemente.isEmpty())
    }

    @Test
    fun aktionenseite_liestDelzaeRadioEintraege() {
        val seite = repo.parseElementeSeite(dump("za1_existing"), 1)
        assertEquals(listOf("8365", "8366"), seite.elemente.map { it.relaId })
        assertEquals("Immer : Einsatz einstellen auf 50% in der 1. Minute", seite.elemente[0].text)
        assertEquals("Gewählte Aktion löschen", seite.loeschLabel)
    }

    @Test
    fun einstellungsseite_liestDelzaeRadioEintraege() {
        val seite = repo.parseElementeSeite(dump("za2_existing"), 2)
        assertEquals(listOf("8364", "8361"), seite.elemente.map { it.relaId })
        assertEquals("Kapitän : Dino Scerri", seite.elemente[0].text)
        assertEquals("Gewählte Einstellung löschen", seite.loeschLabel)
    }

    @Test
    fun aufstellung_taktikenAuswahlEnthaeltEigeneUndStandard() {
        val aufstellung = repo.parseKlassisch(dump("za0"))
        assertNotNull(aufstellung.status)
        assertTrue("raster1-Optionen müssen vorhanden sein", aufstellung.taktiken.isNotEmpty())
        assertTrue(
            "Standard-Taktik 4-4-2 (id 1) erwartet",
            aufstellung.taktiken.any { it.id == "1" && it.label.contains("4-4-2") },
        )
        assertTrue(
            "Eigene Taktik mit langer id erwartet (z.B. 93110)",
            aufstellung.taktiken.any { it.id.length >= 5 },
        )
        assertTrue(
            "Passende Standard-Taktiken (3-4-3) erwartet",
            aufstellung.taktiken.any { it.label.contains("3-4-3") },
        )
    }

    @Test
    fun aufstellung_raSlotsOptionenUndZatAuswahl() {
        val aufstellung = repo.parseKlassisch(dump("za0"))
        assertTrue("Kaderspieler vorhanden", aufstellung.spieler.isNotEmpty())
        assertTrue("34 Kaderspieler im ra[]", aufstellung.spieler.size >= 30)
        assertTrue(
            "Slot-Optionen A–L/T/U–Z enthalten",
            aufstellung.kaderSlots.map { it.id }.containsAll(listOf("A", "L", "T", "U", "Z")),
        )
        assertTrue("Slot-Optionen enthalten leere (nicht aufgestellt)", aufstellung.kaderSlots.any { it.id.isBlank() })
        assertTrue("ZAT-Auswahl (lauf) vorhanden", aufstellung.zatOptionen.any { it.id == "1" })
        assertTrue("ZAT-Auswahl enthält höhere ZATs", aufstellung.zatOptionen.any { it.id == "72" })
        assertTrue("Noch keine Slot-Zuordnung (Testdump leer)", aufstellung.spieler.all { it.raSlot == null })
        assertEquals("0.00", aufstellung.aufstellungsWerte?.optiSkill)
        assertEquals("0.00", aufstellung.aufstellungsWerte?.skillSchnitt)
        assertEquals("0.00", aufstellung.aufstellungsWerte?.fitness)
        assertEquals("0.00", aufstellung.aufstellungsWerte?.moral)
    }

    @Test
    fun raster_liestBuchstabenInKlassischenZellen() {
        val raster = repo.parseRasterLetters(dump("za_raster"))
        fun zelle(b: String): Pair<Int, Int>? = raster[b]?.let { it.zeile to it.spalte }
        // (zeile, spalte) 0-basiert; Zeile 0 = Sturm (O)..14 = Abwehr (A)
        assertEquals(2 to 3, zelle("A"))  // M, Spalte 4
        assertEquals(2 to 7, zelle("B"))  // M, Spalte 8
        assertEquals(3 to 2, zelle("C"))  // L, Spalte 3
        assertEquals(3 to 8, zelle("D"))  // L, Spalte 9
        assertEquals(7 to 5, zelle("E"))  // H, Spalte 6
        assertEquals(9 to 1, zelle("F"))  // F, Spalte 2
        assertEquals(9 to 9, zelle("G"))  // F, Spalte 10
        assertEquals(11 to 5, zelle("H")) // D, Spalte 6
        assertEquals(12 to 2, zelle("K")) // C, Spalte 3
        assertEquals(12 to 8, zelle("L")) // C, Spalte 9
        assertEquals("T nicht in Feldzellen (gehört zum Torwart)", 10, raster.size)
    }

    @Test
    fun betaSpielerlisteKommtNurAusDemZugabgabeRaster() {
        val aufstellung = repo.parseBeta(
            """
            <div class="player tor" id="player_999"><p class="name">Falscher Bereich</p></div>
            <div id="sortable">
              <div class="player stu" id="player_123">
                <div class="number">9</div>
                <p class="name"><a>Spielberechtigt</a></p>
                <p class="stats"><span class="alter">22</span><span class="skill">80.0</span><span class="opti">90.0</span><span class="fit">99</span><span class="mor">88</span></p>
              </div>
            </div>
            <script>moveFieldClone('#player_123', 1, 2);</script>
            """.trimIndent(),
        )

        assertEquals(listOf(123L), aufstellung?.spieler?.map { it.pid })
    }

    @Test
    fun betaOhneSortableSpielerLiefertKeineAuswahl() {
        // Login- oder leere Seite ohne spielerberechtigte Liste (kein #sortable > div.player).
        val aufstellung = repo.parseBeta(
            """
            <html><body><div class="player stu" id="player_999">
              <p class="name">Nicht im Raster</p>
              <p class="stats"><span class="alter">22</span><span class="skill">80.0</span></p>
            </div></body></html>
            """.trimIndent(),
        )
        assertNull(aufstellung)
    }

    private fun kaderSpieler(pid: Long, name: String = "S$pid"): AufstellungSpieler =
        AufstellungSpieler(
            pid = pid,
            name = name,
            nummer = "",
            alter = 25,
            skill = 80.0,
            opti = 90.0,
            fit = 99,
            mor = 88,
            position = SpielerPosition.STU,
            slot = null,
        )

    private fun klassischAufstellung(vararg pids: Long): Aufstellung {
        val spieler = pids.map { kaderSpieler(it) }
        return Aufstellung(
            zat = 1,
            spielart = "Liga",
            gegner = "Gegner",
            status = "Ungültig",
            spieler = spieler,
            taktiken = emptyList(),
            kaderSlots = emptyList(),
        )
    }

    @Test
    fun mergeKlassischeRaListeIstEinzigeBerechtigungsquelle() {
        // Beta-Liste enthält zusätzlichen Spieler 200 (z.B. Rotsperre Tomás Simao).
        // Klassische ra[]-Liste (Website filtert gesperrte/verletzte) enthält NUR 100 und 101.
        val beta = Aufstellung(zat = null, spielart = null, gegner = null, status = null,
            spieler = listOf(kaderSpieler(100), kaderSpieler(101), kaderSpieler(200)))
        val klassisch = klassischAufstellung(100, 101)

        val ergebnis = repo.mergeBetaMitKlassisch(beta, klassisch, emptyMap())

        assertEquals("Nur die klassische ra[]-Liste liefert spielberechtigte Spieler", listOf(100L, 101L), ergebnis.spieler.map { it.pid })
        assertTrue("PID 200 steht nur in der Beta-Liste (nicht spielberechtigt) und darf nicht auswählbar sein", ergebnis.spieler.none { it.pid == 200L })
        // Zusatzdaten aus der klassischen Seite werden übernommen.
        assertEquals(1, ergebnis.zat)
        assertEquals("Liga", ergebnis.spielart)
        assertEquals("Gegner", ergebnis.gegner)
        assertEquals("Ungültig", ergebnis.status)
    }

    @Test
    fun mergeBetaReichertKlassischeSpielerMitDetailsAn() {
        // Beta liefert detaillierte Werte (Skill/Fit/Mor/Nummer), klassisch die Berechtigung.
        val betaSpieler = AufstellungSpieler(
            pid = 100, name = "Detail", nummer = "7", alter = 26,
            skill = 88.5, opti = 91.0, fit = 77, mor = 66,
            position = SpielerPosition.MIT, slot = null,
        )
        val beta = Aufstellung(zat = null, spielart = null, gegner = null, status = null, spieler = listOf(betaSpieler))
        val klassisch = klassischAufstellung(100)

        val ergebnis = repo.mergeBetaMitKlassisch(beta, klassisch, emptyMap())

        val spieler = ergebnis.spieler.single()
        assertEquals("Skill kommt aus der Beta-Detail-Seite", 88.5, spieler.skill, 0.001)
        assertEquals("Fit kommt aus der Beta-Detail-Seite", 77, spieler.fit)
        assertEquals("Mor kommt aus der Beta-Detail-Seite", 66, spieler.mor)
        assertEquals("Nummer kommt aus der Beta-Detail-Seite", "7", spieler.nummer)
        assertEquals("Position kommt aus der Beta-Detail-Seite", SpielerPosition.MIT, spieler.position)
    }

    @Test
    fun mergeOhneBetaListeNutztKlassischeRaListe() {
        // Kein Beta-Dump verfügbar: die klassische ra[]-Liste ist trotzdem eine
        // gültige Berechtigungsquelle (Website filtert dort gesperrte/verletzte).
        val klassisch = klassischAufstellung(100, 101, 200)

        val ergebnis = repo.mergeBetaMitKlassisch(beta = null, klassisch = klassisch, raster = emptyMap())

        assertEquals(listOf(100L, 101L, 200L), ergebnis.spieler.map { it.pid })
        assertEquals("ZAT bleibt fürs Anzeigen erhalten", 1, ergebnis.zat)
    }

    @Test
    fun mergeTorwartSlotWirdAusRaSlotTAbgeleitet() {
        // Klassische Seite setzt einen Spieler auf "T" (Torwart).
        // Ohne Beta-Slot muss der Torwart dennoch als Torwart markiert sein,
        // damit das Spielfeld den Torwart immer anzeigt (Regression zu #Taktik-Torwart).
        val beta = Aufstellung(
            zat = null, spielart = null, gegner = null, status = null,
            spieler = listOf(kaderSpieler(100).copy(slot = null)),
        )
        val klassisch = Aufstellung(
            zat = 1, spielart = "Liga", gegner = "Gegner", status = "Ungültig",
            spieler = listOf(kaderSpieler(100).copy(raSlot = "T")),
            taktiken = emptyList(), kaderSlots = emptyList(),
        )

        val ergebnis = repo.mergeBetaMitKlassisch(beta, klassisch, emptyMap())

        val torwart = ergebnis.torwart
        assertNotNull("Torwart muss immer gesetzt sein wenn raSlot = T", torwart)
        assertEquals(100L, torwart!!.pid)
        assertEquals(AufstellungSlot.Torwart, torwart.slot)
        assertEquals("T", torwart.raSlot)
    }

    @Test
    fun betaTorwartWirdAlsFeld00Erkannt() {
        // Das Beta-JSON speichert den Torwart als [pid, 0, 0]. Beim Laden ruft
        // der Server ggf. moveFieldClone('#player_X', 0, 0) auf – Koordinaten
        // (0,0) liegen außerhalb des gültigen Felds (Zeilen 1–15, Spalten 1–11)
        // und müssen als Torwart erkannt werden.
        val beta = repo.parseBeta(
            """
            <div id="sortable">
              <div class="player tor" id="player_100">
                <div class="number">1</div>
                <p class="name"><a>Torwart</a></p>
                <p class="stats"><span class="alter">30</span><span class="skill">85.0</span><span class="opti">90.0</span><span class="fit">95</span><span class="mor">80</span></p>
              </div>
            </div>
            <script>moveFieldClone('#player_100', 0, 0);</script>
            """.trimIndent(),
        )
        assertNotNull(beta)
        assertEquals(AufstellungSlot.Torwart, beta!!.spieler.single().slot)
    }

    @Test
    fun betaTorwartWirdAlsOneDrop7Erkannt() {
        // Der aktive Torwart wird in #goal (oneDrop-Index 7) platziert:
        // moveOneDropClone('#player_X', 7). Der Parser muss nr >= 6 als Torwart
        // erkennen (Website: nr 5+6 = torhelper).
        val beta = repo.parseBeta(
            """
            <div id="sortable">
              <div class="player tor" id="player_100">
                <div class="number">1</div>
                <p class="name"><a>Torwart</a></p>
                <p class="stats"><span class="alter">30</span><span class="skill">85.0</span><span class="opti">90.0</span><span class="fit">95</span><span class="mor">80</span></p>
              </div>
            </div>
            <script>moveOneDropClone('#player_100', 7);</script>
            """.trimIndent(),
        )
        assertNotNull(beta)
        assertEquals(AufstellungSlot.Torwart, beta!!.spieler.single().slot)
    }

    @Test
    fun betaErsatztorwartErhaeltBankIndex0() {
        // Beta-Seite: oneDrop-Index 5 = #ersatz_goal (Ersatztorwart, gelb). Der Parser
        // spiegelt die Beta-Nummerierung gegen die klassischen Bank-Buchstaben, damit
        // der Ersatztorwart auf dem App-Index 0 (Buchstabe U) liegt statt auf 5.
        val beta = repo.parseBeta(
            """
            <div id="sortable">
              <div class="player tor" id="player_100"><div class="number">1</div><p class="name"><a>Aktiv</a></p><p class="stats"><span class="alter">30</span><span class="skill">85.0</span></p></div>
              <div class="player tor" id="player_120"><div class="number">12</div><p class="name"><a>Ersatz</a></p><p class="stats"><span class="alter">29</span><span class="skill">70.0</span></p></div>
            </div>
            <script>
              moveOneDropClone('#player_100', 6);
              moveOneDropClone('#player_120', 5);
            </script>
            """.trimIndent(),
        )
        assertNotNull(beta)
        assertEquals(AufstellungSlot.Torwart, beta!!.spieler.first { it.pid == 100L }.slot)
        assertEquals("Ersatztorwart (#ersatz_goal, nr 5) = Bank-Index 0 (U)", AufstellungSlot.Ersatz(0), beta.spieler.first { it.pid == 120L }.slot)
    }

    @Test
    fun betaErsatzFeldspielerWirdGespiegeltZugeordnet() {
        // Die Beta-Reihenfolge ist gegen die klassischen Buchstaben gespiegelt:
        // oberster Feld-Ersatz (nr 0) => Z (Index 5), unterster (nr 4) => V (Index 1).
        val beta = repo.parseBeta(
            """
            <div id="sortable">
              <div class="player abw" id="player_100"><div class="number">5</div><p class="name"><a>Oben</a></p><p class="stats"><span class="alter">28</span><span class="skill">70.0</span></p></div>
              <div class="player stu" id="player_200"><div class="number">9</div><p class="name"><a>Unten</a></p><p class="stats"><span class="alter">27</span><span class="skill">69.0</span></p></div>
            </div>
            <script>
              moveOneDropClone('#player_100', 0);
              moveOneDropClone('#player_200', 4);
            </script>
            """.trimIndent(),
        )
        assertNotNull(beta)
        assertEquals("Oberster Feld-Ersatz (nr 0) = Z (Index 5)", AufstellungSlot.Ersatz(5), beta!!.spieler.first { it.pid == 100L }.slot)
        assertEquals("Unterster Feld-Ersatz (nr 4) = V (Index 1)", AufstellungSlot.Ersatz(1), beta.spieler.first { it.pid == 200L }.slot)
    }

    @Test
    fun mergeTorwartFallbackAusBetaPosition() {
        // Wenn weder klassisches raSlot="T" noch der Beta-Slot den Torhüter
        // identifiziert (z.B. leerer raSlot UND Beta-Parser ohne Treffer),
        // muss der Merge den Torhüter anhand der Beta-Positions-Klasse (TOR)
        // als Fallback erkennen.
        val beta = Aufstellung(
            zat = null, spielart = null, gegner = null, status = null,
            spieler = listOf(
                kaderSpieler(100).copy(position = SpielerPosition.TOR, slot = null),
                kaderSpieler(200).copy(position = SpielerPosition.ABW, slot = AufstellungSlot.Feld(3, 2)),
            ),
        )
        val klassisch = Aufstellung(
            zat = 1, spielart = "Liga", gegner = "Gegner", status = "Ungültig",
            spieler = listOf(
                kaderSpieler(100).copy(raSlot = null),
                kaderSpieler(200).copy(raSlot = "A"),
            ),
            taktiken = emptyList(), kaderSlots = emptyList(),
        )

        val ergebnis = repo.mergeBetaMitKlassisch(beta, klassisch, emptyMap())

        val torwart = ergebnis.torwart
        assertNotNull("Torhüter muss per Fallback aus Beta-Position TOR erkannt werden", torwart)
        assertEquals(100L, torwart!!.pid)
        assertEquals(AufstellungSlot.Torwart, torwart.slot)
    }

    @Test
    fun mergeTorwartTZelleGewinntImmerGegenBetaSlot() {
        // Die T-Zelle ist die verbindliche Sonderstellung des Torwarts. Auch wenn
        // die Beta-Seite einen (Feld- oder Ersatz-)Slot für den Torwart liefert,
        // darf dieser die fixe T-Zuordnung nicht überschreiben – das Spielfeld
        // zeigt den Torwart dann immer auf A6 (Regression zu #Taktik-Torwart).
        val beta = Aufstellung(
            zat = null, spielart = null, gegner = null, status = null,
            spieler = listOf(kaderSpieler(100).copy(slot = AufstellungSlot.Feld(zeile = 3, spalte = 5))),
        )
        val klassisch = Aufstellung(
            zat = 1, spielart = "Liga", gegner = "Gegner", status = "Ungültig",
            spieler = listOf(kaderSpieler(100).copy(raSlot = "T")),
            taktiken = emptyList(), kaderSlots = emptyList(),
        )

        val ergebnis = repo.mergeBetaMitKlassisch(beta, klassisch, emptyMap())

        assertEquals("Klassische T-Zelle überschreibt Beta-Feld-Slot", AufstellungSlot.Torwart, ergebnis.spieler.first().slot)
    }

    @Test
    fun mergeOhneKlassischeListeLiefertKeineAuswahl() {
        // Nur die Beta-Seite wird geladen (klassische Seite z.B. gescheitert):
        // ohne Berechtigungsliste dürfen keine Spieler auswählbar werden.
        val beta = Aufstellung(zat = null, spielart = null, gegner = null, status = null,
            spieler = listOf(kaderSpieler(100)))

        val ergebnis = repo.mergeBetaMitKlassisch(beta, klassisch = null, raster = emptyMap())

        assertTrue("Ohne klassische Berechtigungsliste keine Spieler wählbar", ergebnis.spieler.isEmpty())
    }

    @Test
    fun checkza_parstAlleDreiAbschnitteUndErfolg() {
        val ergebnis = repo.parseCheckzaErgebnis(dump("za_checkza_valid"))

        assertTrue("Gesamtstatus erfolgreich", ergebnis.gueltig)
        assertTrue(ergebnis.gesamtStatus.startsWith("\u2713"))

        assertEquals("15 Aufstellungseintraege (inkl. Spaltenkopf + 14 Spieler)", 15, ergebnis.aufstellung.size)
        assertTrue("Kopfzeile Platz/Spieler erkannt", ergebnis.aufstellung.any { it.text.contains("Platz") })
        assertTrue("Torwart Eintrag", ergebnis.aufstellung.any { it.text.contains("Mueller, Thomas") })

        assertEquals("2 Aktionen", 2, ergebnis.aktionen.size)
        assertTrue("Einsatz-Aktion", ergebnis.aktionen[0].text.contains("Einsatz"))
        assertTrue("Haerte-Aktion", ergebnis.aktionen[1].text.contains("H\u00e4rte"))

        assertEquals("4 Einstellungen", 4, ergebnis.einstellungen.size)
        assertTrue("Kapitän", ergebnis.einstellungen.any { it.text.contains("Kapit") })
        assertTrue("Spielmacher", ergebnis.einstellungen.any { it.text.contains("Spielmacher") })
    }

    @Test
    fun checkza_keineGueltigeZugabgabe() {
        val ergebnis = repo.parseCheckzaErgebnis(dump("za_checkza"))

        assertTrue("nicht gueltig", !ergebnis.gueltig)
        assertTrue("Fehlerstatus", ergebnis.gesamtStatus.contains("g\u00fcltige Zugabgabe"))
        assertTrue(
            "Aufstellung bleibt sichtbar (inkl. mojibake U+FFFD statt \u00fc)",
            ergebnis.aufstellung.any {
                it.text.replace('\uFFFD', '\u00FC').contains("Keine g\u00fcltige Zugabgabe")
            },
        )
        assertTrue("keine Abschnitte 2/3 im dem-Dump vorhanden", ergebnis.aktionen.isEmpty() && ergebnis.einstellungen.isEmpty())
    }

    @Test
    fun checkza_leereAntwortLiefertKeinGueltigesErgebnis() {
        val ergebnis = repo.parseCheckzaErgebnis("")
        assertTrue("leere Antwort = nicht gueltig", !ergebnis.gueltig)
        assertTrue("Fallback-Status fuer leere Antwort", ergebnis.gesamtStatus.isNotBlank())
    }

    @Test
    fun checkza_loginErforderlich() {
        val ergebnis = repo.parseCheckzaErgebnis(
            "<html><body><div>Du ben\u00f6tigst ein Team um diese Seite verwenden zu k\u00f6nnen!</div></body></html>",
        )
        assertTrue("kein Team = nicht gueltig", !ergebnis.gueltig)
        assertTrue("Abschnitte leer", ergebnis.aufstellung.isEmpty() && ergebnis.aktionen.isEmpty() && ergebnis.einstellungen.isEmpty())
    }

    @Test
    fun mergeBankBuchstabenUVZBildenErsatzbank() {
        // Klassische ra[]-Buchstaben U-Z müssen auch dann als Ersatzbank erkannt werden,
        // wenn die Beta-Seite keine Mover liefert (sonst bliebe die Bank unsichtbar).
        val beta = Aufstellung(
            zat = null, spielart = null, gegner = null, status = null,
            spieler = listOf(
                kaderSpieler(100).copy(slot = null),
                kaderSpieler(101).copy(slot = null),
            ),
        )
        val klassisch = Aufstellung(
            zat = 1, spielart = "Liga", gegner = "Gegner", status = "Ungültig",
            spieler = listOf(
                kaderSpieler(100).copy(raSlot = "U"),
                kaderSpieler(200).copy(raSlot = "Z"),
            ),
            taktiken = emptyList(), kaderSlots = emptyList(),
        )

        val ergebnis = repo.mergeBetaMitKlassisch(beta, klassisch, emptyMap())

        assertEquals("U = Ersatz(0) (Ersatztorwart)", AufstellungSlot.Ersatz(0), ergebnis.spieler.first { it.pid == 100L }.slot)
        assertEquals("Z = Ersatz(5) (Feld-Ersatzspieler)", AufstellungSlot.Ersatz(5), ergebnis.spieler.first { it.pid == 200L }.slot)
        assertEquals("Ersatzbank aus klassischen Buchstaben belegt", 2, ergebnis.bank.size)
    }

    @Test
    fun mergeBankBuchstabeWinsGegenBetaSlot() {
        // Wie bei der T-Zelle ist der klassische Bank-Buchstabe verbindlich:
        // ein abweichender Beta-Slot darf die Zuordnung nicht überschreiben.
        val beta = Aufstellung(
            zat = null, spielart = null, gegner = null, status = null,
            spieler = listOf(kaderSpieler(100).copy(slot = AufstellungSlot.Ersatz(3))),
        )
        val klassisch = Aufstellung(
            zat = 1, spielart = "Liga", gegner = "Gegner", status = "Ungültig",
            spieler = listOf(kaderSpieler(100).copy(raSlot = "U")),
            taktiken = emptyList(), kaderSlots = emptyList(),
        )

        val ergebnis = repo.mergeBetaMitKlassisch(beta, klassisch, emptyMap())

        assertEquals("Klassischer Bank-Buchstabe U gewinnt gegen Beta-Slot", AufstellungSlot.Ersatz(0), ergebnis.spieler.first().slot)
    }

    @Test
    fun mergeFeldBuchstabeGewinntGegenBetaErsatzSlot() {
        // Wie bei T- und Bank-Buchstaben ist auch ein klassischer Feld-Buchstabe
        // (A-L) verbindlich: Ein Beta-Ersatz-Slot darf einen Feldspieler nicht auf
        // die Bank "wandern" lassen (sonst verwaisst der Feldplatz).
        val beta = Aufstellung(
            zat = null, spielart = null, gegner = null, status = null,
            spieler = listOf(kaderSpieler(100).copy(slot = AufstellungSlot.Ersatz(3))),
        )
        val klassisch = Aufstellung(
            zat = 1, spielart = "Liga", gegner = "Gegner", status = "Ungültig",
            spieler = listOf(kaderSpieler(100).copy(raSlot = "C")),
            taktiken = emptyList(), kaderSlots = emptyList(),
        )

        val ergebnis = repo.mergeBetaMitKlassisch(
            beta, klassisch,
            raster = mapOf("C" to ZugabgabeRepository.RasterZelle(3, 2)),
        )

        assertEquals("Klassischer Feld-Buchstabe C bleibt auf dem Feld", AufstellungSlot.Feld(3, 2), ergebnis.spieler.first().slot)
        assertTrue("Kein Bankplatz durch Beta-Slot", ergebnis.bank.isEmpty())
    }

    @Test
    fun mergeTorwartFallbackSetztRaSlotTDamitKeineBankGeisterEntstehen() {
        // Frueher behielt der per Fallback promotete Torwart seinen Bank-Buchstaben
        // (z.B. "U" des Ersatztorwarts): Er wurde dann gleichzeitig als Bank-Besatz
        // (via raSlot) angezeigt, obwohl die Bank den Platz nicht mehr belegt hat.
        val beta = Aufstellung(
            zat = null, spielart = null, gegner = null, status = null,
            spieler = listOf(kaderSpieler(100).copy(position = SpielerPosition.TOR, slot = null)),
        )
        val klassisch = Aufstellung(
            zat = 1, spielart = "Liga", gegner = "Gegner", status = "Ungültig",
            spieler = listOf(kaderSpieler(100).copy(raSlot = "U")),
            taktiken = emptyList(), kaderSlots = emptyList(),
        )

        val ergebnis = repo.mergeBetaMitKlassisch(beta, klassisch, emptyMap())

        val torwart = ergebnis.torwart
        assertNotNull("Torhüter per Fallback erkannt", torwart)
        assertEquals("Promoteter Torhüter erhält raSlot T statt U", "T", torwart!!.raSlot)
        assertTrue("Bankplatz U ist danach frei", ergebnis.bank.isEmpty())
        assertNull("Kein Spieler mehr auf Bank-Slot 0", ergebnis.spielerAufBankSlot(0))
    }

    @Test
    fun mergeDoppelteErsatzIndizesWerdenDedupliziert() {
        // Wenn Klassisch einen Bank-Buchstaben hat und Beta denselben Platz per
        // Slot belegt, entstuenden zwei "Besitzer" eines Platzes. Der Inhaber des
        // klassischen Buchstabens gewinnt, der andere verliert seinen Slot.
        val beta = Aufstellung(
            zat = null, spielart = null, gegner = null, status = null,
            spieler = listOf(
                kaderSpieler(100).copy(slot = null),
                kaderSpieler(200).copy(slot = AufstellungSlot.Ersatz(1)),
            ),
        )
        val klassisch = Aufstellung(
            zat = 1, spielart = "Liga", gegner = "Gegner", status = "Ungültig",
            spieler = listOf(
                kaderSpieler(100).copy(raSlot = "V"),
                kaderSpieler(200).copy(raSlot = null),
            ),
            taktiken = emptyList(), kaderSlots = emptyList(),
        )

        val ergebnis = repo.mergeBetaMitKlassisch(beta, klassisch, emptyMap())

        val besitzerV = ergebnis.spieler.first { it.pid == 100L }
        val betaDummy = ergebnis.spieler.first { it.pid == 200L }
        assertEquals("Klassischer V-Inhaber bleibt am Platz", AufstellungSlot.Ersatz(1), besitzerV.slot)
        assertEquals("V-Inhaber behält Buchstabe", "V", besitzerV.raSlot)
        assertNull("Doppeltbelegung W (Index 1) entfernt", betaDummy.slot)
        assertNull("Buchstabe der Doppeltbelegung entfernt", betaDummy.raSlot)
        assertEquals("Ersatzbank zählt nur einen Besatz", 1, ergebnis.bank.size)
    }

    @Test
    fun mergeGesamterDumpEnthaeltAlleKlassischSpielberechtigten() {
        // Gegen echte Server-Dumps: alle in der klassischen ra[]-Liste stehenden
        // Spieler müssen in der Auswahl sein – unabhängig von der Beta-Vollständigkeit.
        val beta = repo.parseBeta(dump("za_beta"))
        val klassisch = repo.parseKlassisch(dump("za0"))

        val ergebnis = repo.mergeBetaMitKlassisch(beta, klassisch, emptyMap())

        assertEquals(
            "Die klassische ra[]-Liste bestimmt die spielberechtigte Auswahl",
            klassisch.spieler.map { it.pid }.sorted(),
            ergebnis.spieler.map { it.pid }.sorted(),
        )
    }
}
