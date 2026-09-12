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
        appendLine("- Nenne das Endergebnis weder im Einstieg noch während des Hauptteils der Reportage.")
        appendLine("- Wiederhole keine vollständigen Zwischen- oder Endspielstände aus den Ereignistexten, außer wenn sie zwingend nötig sind.")
        appendLine("- Baue Spannung auf, als wäre der Ausgang der Partie noch unbekannt.")
        appendLine("- Nenne das tatsächliche Endergebnis ausschließlich in der abschließenden Auflösung am Ende.")
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
        appendLine()
        appendLine("AUFLÖSUNG - NUR AM ENDE VERWENDEN:")
        appendLine("Erst im letzten Abschnitt ausdrücklich nennen: Nach 90 Minuten steht es zwischen ${bericht.heim ?: "dem Heimteam"} und ${bericht.gast ?: "dem Auswärtsteam"} ${bericht.ergebnis ?: "(Endergebnis nicht angegeben)"}.")
        appendLine("Dieses Ergebnis darf vorher an keiner Stelle genannt oder vorweggenommen werden.")
        appendLine("Lass die Reportage danach mit einer kurzen, emotional mitreißenden Bewertung der Partie ausklingen und beende sie innerhalb von drei Minuten Gesamtlänge.")
        appendLine()
        appendLine("Beginne jetzt mit einer atmosphärischen, spannungsgeladenen Fußball-Radiozusammenfassung dieser Partie.")
    }
}
