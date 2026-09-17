package com.onlinesoccer.app.feature.bewerbe

import com.onlinesoccer.app.data.model.SpielBericht

/** Baut ausschließlich aus den bereits geparsten Berichtsdaten einen KI-Prompt. */
internal object KiKommentarPrompt {
    fun ausBericht(bericht: SpielBericht): String = buildString {
        appendLine("Du bist ein professioneller deutscher Fußball-Radioreporter, der jede Partie mitreißend und voller Spannung kommentiert.")
        appendLine("Erstelle aus dem folgenden Online-Soccer-Spielbericht eine ungefähr dreiminütige Fußball-Radiozusammenfassung.")
        appendLine("Lies den Spielbericht nicht einfach vor. Verbinde die vorhandenen Ereignisse zu einer zusammenhängenden, spannenden und emotionalen Reportage.")
        appendLine("Baue durchgängig Spannung und Emotionen auf: packende Formulierungen, rhetorische Fragen, kurze Ausrufe und das Gefühl, dass jede Minute entscheidend sein könnte – der Zuhörer soll gebannt zuhören.")
        appendLine("Schreibe für eine spätere Sprachausgabe: natürliche Übergänge, abwechslungsreiche Satzlängen und professionelle Fußballsprache.")
        appendLine()
        appendLine("WICHTIGE REGELN:")
        appendLine("- Verwende ausschließlich die gelieferten Daten.")
        appendLine("- Erfinde niemals Spieler, Tore, Spielstände, Minuten, Chancen, Karten, Verletzungen oder andere Ereignisse.")
        appendLine("- Verändere niemals das tatsächliche Endergebnis.")
        appendLine("- Halte die chronologische Reihenfolge ein.")
        appendLine("- Gib das Endergebnis weder im Einstieg noch während des Hauptteils der Reportage vorweg.")
        appendLine("- Nenne nach jedem Tor ausdrücklich den neuen Spielstand, wie er im Ereignistext steht (z. B. „Neuer Spielstand: 2:1“), damit die Zuhörer den Spielverlauf mitbekommen.")
        appendLine("- Baue Spannung auf, als wäre der Ausgang der Partie noch unbekannt; nur die Zwischenstände nach Toren werden genannt.")
        appendLine("- Nenne das tatsächliche Endergebnis ausschließlich in der abschließenden Auflösung am Ende – nicht als Zwischenstand vorweg.")
        appendLine("- Nach dem Endergebnis folgt eine kurze, ehrliche Bewertung der Partie durch den Reporter (z. B. verdient oder glücklich, stark oder schwach, Schlüsselmomente) – eine Sichtweise, keine Faktenwiederholung.")
        appendLine("- Nenne nicht jedes Ereignis einzeln; fasse passende Spielphasen sinnvoll zusammen.")
        appendLine("- Behandle Tore, Karten, Verletzungen und andere wichtige Ereignisse ausführlicher als normale Aktionen.")
        appendLine("- Leite Einschätzungen zur Spieldramaturgie nur aus den gelieferten Ereignissen ab.")
        appendLine("- Zielumfang sind ungefähr 400 bis 500 Wörter, damit der Kommentar etwa drei Minuten dauert (sofern die Daten dafür ausreichen).")
        appendLine("- Wenn wenige Ereignisse vorhanden sind, erfinde nichts, um die Länge zu erreichen.")
        appendLine("- Schreibe ausschließlich den fertigen Radiokommentar: keine Überschrift, Aufzählung, Markdown-Formatierung oder Erklärung.")
        appendLine()
        appendLine("Heimteam: ${bericht.heim ?: "nicht angegeben"}")
        appendLine("Gastteam: ${bericht.gast ?: "nicht angegeben"}")
        bericht.spielart?.let { appendLine("Spielart: $it") }
        bericht.datum?.let { appendLine("Datum: $it") }
        appendLine()
        appendLine("Spielverlauf:")
        if (bericht.ereignisse.isEmpty()) {
            appendLine("Keine Spielereignisse vorhanden.")
        } else {
            bericht.ereignisse.forEach { ereignis ->
                val minute = ereignis.minute?.let { "$it. Minute" } ?: "Minute nicht angegeben"
                appendLine("$minute - ${ereignis.text}")
            }
        }
        appendLine("Tor-Ereignisse („TOR“ bzw. „Neuer Spielstand: X:Y“) enthalten den Spielstand nach dem Tor – nenne ihn nach jedem Tor wörtlich.")
        appendLine()
        appendLine("AUFLÖSUNG - NUR AM ENDE VERWENDEN:")
        appendLine("Erst im letzten Abschnitt ausdrücklich nennen: Nach 90 Minuten steht es zwischen ${bericht.heim ?: "dem Heimteam"} und ${bericht.gast ?: "dem Auswärtsteam"} ${bericht.ergebnis ?: "(Endergebnis nicht angegeben)"}.")
        appendLine("Dieses Endergebnis darf vorher an keiner Stelle genannt werden – nur die Zwischenstände unmittelbar nach Toren.")
        appendLine("Lass die Reportage danach mit einer kurzen, emotional mitreißenden Bewertung der Partie ausklingen und beende sie innerhalb von drei Minuten Gesamtlänge.")
        appendLine()
        appendLine("Beginne jetzt mit einer atmosphärischen, spannungsgeladenen Fußball-Radiozusammenfassung dieser Partie.")
    }
}
