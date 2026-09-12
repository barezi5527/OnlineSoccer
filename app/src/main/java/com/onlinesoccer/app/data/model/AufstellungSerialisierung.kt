package com.onlinesoccer.app.data.model

import java.io.IOException

/**
 * Serialisiert eine Aufstellung in das Beta-JSON der Website (Phase-2 §5.2).
 *
 * Der Server erwartet (Eigenentwicklung, Za-Beta-Speicherformat in `za_beta.html`):
 *
 * ```
 * [ [<pid_tor>,0,0],
 *   [<pid_feld1>,<links 1..11>,<oben 1..15>], ... (10 Feldspieler, 1-basiert!),
 *   [<pid_ersatz1>,0,-1], ... (6 Ersatz, Bankindex i -> -i) ]
 * ```
 */
object AufstellungSerialisierung {

    fun toBetaJson(aufstellung: Aufstellung): String {
        val feld = aufstellung.feldspieler
        val ersatz = aufstellung.bank

        if (aufstellung.torwart == null || feld.size != 10 || ersatz.size != 6) {
            throw IOException("Ungültige Aufstellung für den Server (TW/Feld/Bank inkonsistent).")
        }

        return buildString {
            append("[")
            append("[${aufstellung.torwart!!.pid},0,0]")
            feld.forEach { s ->
                // App-Grid ist 0-basiert (zeile 0..14, spalte 0..10); der Server
                // rechnet 1-basiert (links 1..11, oben 1..15).
                val slot = s.slot as AufstellungSlot.Feld
                append(",[${s.pid},${slot.spalte + 1},${slot.zeile + 1}]")
            }
            // Ersatzbank: App-Index 0..5 (U..Z, 0 = Ersatztorwart auf U) => Server-Index.
            // Die Beta-Seite nummeriert den Ersatztorwart (ersatz_goal) mit -5, die
            // Feld-Ersatzplätze aufsteigend 0..-4 – genau gespiegelt zur App-Nummerierung
            // (App 0 = Torwart => -5, App 5 = Z => 0).
            ersatz.forEach { s ->
                val slot = s.slot as AufstellungSlot.Ersatz
                append(",[${s.pid},${slot.index - ERSATZBANK_BUCHSTABEN.lastIndex},-1]")
            }
            append("]")
        }
    }
}