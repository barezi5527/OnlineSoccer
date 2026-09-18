package com.onlinesoccer.app.data.model

/**
 * Gemeinsame Spielereignis-Klassifikation – eine Quelle für alle Quellen
 * (Spielbericht-HTML, Livegame-JSON). Ordnet einen Ereignistext einem
 * [BerichtEreignisTyp] zu, damit der „Spielverlauf" überall identisch
 * eingefärbt/gekennzeichnet wird.
 */

/** Spielstand-Zeile eines Tores, z. B. „TOR Neuer Spielstand: 1:0 (Name)". */
val torSpielstandRegex = Regex("TOR\\s+Neuer Spielstand:\\s*(\\d+):(\\d+)\\s*\\(([^)]+)\\)")

/** Nur das großgeschriebene „TOR" zählt als Tor – „…knapp vorbei am Tor" ist keins. */
private val torWortRegex = Regex("\\bTOR\\b")

/** Ein neuer Spielstand entsteht ausschließlich durch ein Tor. */
private val neuerSpielstandRegex = Regex("Neuer Spielstand:\\s*\\d+:\\s*\\d+")

private val roteKarteRegex =
    Regex("gelb[- ]rote?\\s+karte|zweite gelbe|rote?\\s+karte|platzverweis|des feldes verwiesen", RegexOption.IGNORE_CASE)
private val gelbeKarteRegex = Regex("gelbe?\\s+karte", RegexOption.IGNORE_CASE)
private val verletzungRegex = Regex("verletz", RegexOption.IGNORE_CASE)
private val elfmeterRegex = Regex("elfmeter|11[- ]?meter|strafstoß|penalty", RegexOption.IGNORE_CASE)

/** Ordnet den Ereignistext einem [BerichtEreignisTyp] zu. */
fun klassifiziereEreignis(text: String): BerichtEreignisTyp = when {
    torSpielstandRegex.containsMatchIn(text) ||
        torWortRegex.containsMatchIn(text) ||
        neuerSpielstandRegex.containsMatchIn(text) -> BerichtEreignisTyp.TOR
    roteKarteRegex.containsMatchIn(text) -> BerichtEreignisTyp.ROTE_KARTE
    gelbeKarteRegex.containsMatchIn(text) -> BerichtEreignisTyp.GELBE_KARTE
    verletzungRegex.containsMatchIn(text) -> BerichtEreignisTyp.VERLETZUNG
    elfmeterRegex.containsMatchIn(text) -> BerichtEreignisTyp.ELFMETER
    else -> BerichtEreignisTyp.SONSTIGES
}