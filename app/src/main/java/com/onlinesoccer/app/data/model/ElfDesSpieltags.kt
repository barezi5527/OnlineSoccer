package com.onlinesoccer.app.data.model

/**
 * „Elf des Spieltags": Datenmodell der lokalen Auswertung.
 *
 * Die Liga wird ausschließlich dynamisch aus dem Kontext des angemeldeten
 * Benutzers (Server-Standard `ls.php`) ermittelt – es ist keine Liga fest im
 * Code hinterlegt. Alle Spieler stammen aus den echten Spielberichten des
 * Spieltags, es werden keine Werte erfunden.
 */
data class ElfKontext(
    /** Team-ID des angemeldeten Benutzers (Cache-Unterteilung je Benutzer). */
    val teamId: Long? = null,
    val landId: Int = 0,
    val ligaId: Int = 0,
    val saison: Int = 0,
    val landLabel: String = "",
    val ligaLabel: String = "",
    val saisonLabel: String? = null,
    /** Standard-Spieltag (letzter/aktueller abgeschlossener des Servers). */
    val zat: Int = 0,
    val zatOptionen: List<Int> = emptyList(),
) {
    val gueltig: Boolean get() = ligaId > 0 && landId > 0 && zat > 0
}

/**
 * Ein Spieler der Elf des Spieltags – aus den realen Spielberichten.
 *
 * Neben der Bewertung werden die tatsächlich im Bericht gefundenen Einzelwerte
 * mitgeführt. Daraus lässt sich die Bewertung jederzeit transparent und exakt
 * nachvollziehbar aus [ElfBewertung.bewerten] neu berechnen (Bewertungsdetails),
 * ohne sie im Cache speichern zu müssen. Nicht im Bericht vorhandene Werte
 * bleiben neutral (0 / null) und werden nie geschätzt.
 */
data class ElfSpieler(
    val name: String,
    val verein: String,
    val position: SpielerPosition,
    val bewertung: Double,
    /** Team-ID des Vereins des Spielers (Wappenquelle), falls der Bericht sie trägt. */
    val teamId: Long? = null,
    /** Spieler-ID des Berichts, falls die Seite sie verlinkt (sonst null). */
    val spielerId: Long? = null,
    /** Stand der Spieler in der Startelf (Startspieler im Bericht). */
    val startelf: Boolean = true,
    /**
     * Kapitän der Mannschaft („C“-Binde) – nur wenn der Bericht dies eindeutig
     * nennt (z. B. „Name (C)“, „Kapitän: Name“). Ohne Hinweis bleibt false;
     * es wird nichts erfunden.
     */
    val kapitän: Boolean = false,
    /**
     * Einsatzminuten, nur wenn der Bericht Ein-/Auswechslungen nennt
     * (sonst null → Bewertung schätzt nicht, sondern wertet neutral).
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
}

/**
 * Eine Zeile der transparenten Bewertungsaufschlüsselung – ein Kriterium und
 * sein effektiver Beitrag zur Gesamtnote. Beitrag 0 wird nicht angezeigt.
 */
data class BewertungsZeile(
    val kriterium: String,
    val beitrag: Double,
)

/**
 * Transparente Bewertung eines Spielers: Gesamtnote (1,0 … 10,0) plus die
 * nachvollziehbare Aufschlüsselung nach Kriterien.
 */
data class SpielerBewertung(
    val gesamt: Double,
    val zeilen: List<BewertungsZeile>,
)

/** Ergebnis der Bewertung eines Spieltags (bewertet + gecacht). */
data class ElfErgebnis(
    val land: String,
    val liga: String,
    val saison: Int,
    val spieltag: Int,
    val spieler: List<ElfSpieler> = emptyList(),
    /** Gewählte Formation (z. B. „4-3-3"); null beim Lücken-Fallback. */
    val formation: String? = null,
    /** Anzahl aller Begegnungen des Spieltags in der Liga. */
    val begegnungen: Int = 0,
    /** Anzahl erfolgreich geladener/geprüfter Spielberichte. */
    val berichteErfolgreich: Int = 0,
    /** True, wenn alle Begegnungen geladen und bewertet werden konnten. */
    val vollstaendig: Boolean = false,
) {
    val elfVollstaendig: Boolean get() = spieler.size == 11

    /**
     * Spieler des Spieltags: der höchstbewertete Spieler. Die Bewertung wird
     * dabei nie künstlich angehoben – auch ein bester Wert von 8,6 bleibt 8,6.
     */
    val spielerDesSpieltags: ElfSpieler?
        get() = spieler.maxByOrNull { it.bewertung }
}