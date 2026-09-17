package com.onlinesoccer.app.feature.bewerbe

import com.onlinesoccer.app.data.model.BerichtEreignisTyp
import com.onlinesoccer.app.data.model.SpielBericht

/**
 * Erzeugt die Aussage des Heim- oder Gasttrainers zur Pressekonferenz direkt
 * aus dem geparsten Spielbericht. Deterministisch und offline – wie die
 * „Elf des Spieltags" stammt jede Aussage ausschließlich aus den Berichtsdaten.
 */
internal object KiPressekonferenz {

    /** Perspektive, aus der der Trainer spricht. */
    enum class Trainer { HEIM, GAST }

    /** Überschrift und kurzer Stellungnahmetext eines Trainers. */
    data class Aussage(
        val trainer: Trainer,
        val teamName: String,
        val ueberschrift: String,
        val text: String,
    ) {
        /** Kompletter Forums-Beitrag: Überschrift, Leerzeile, Text. */
        fun ganz(): String = buildString {
            appendLine(ueberschrift)
            appendLine()
            append(text)
        }
    }

    fun aussage(bericht: SpielBericht, trainer: Trainer = Trainer.HEIM): Aussage {
        val tore = bericht.ergebnis
            ?.split(":", limit = 2)
            ?.map { it.trim().toIntOrNull() }

        val heimTore = tore?.getOrNull(0)
        val gastTore = tore?.getOrNull(1)
        val eigeneTore = if (trainer == Trainer.HEIM) heimTore else gastTore
        val gegnerTore = if (trainer == Trainer.HEIM) gastTore else heimTore

        val ausgang = when {
            eigeneTore != null && gegnerTore != null && eigeneTore > gegnerTore -> Ausgang.SIEG
            eigeneTore != null && gegnerTore != null && eigeneTore < gegnerTore -> Ausgang.NIEDERLAGE
            eigeneTore != null && gegnerTore != null -> Ausgang.UNENTSCHIEDEN
            else -> Ausgang.UNBESTIMMT
        }

        val eigen = if (trainer == Trainer.HEIM) bericht.heim ?: "unser Team" else bericht.gast ?: "unser Team"
        val gegner = if (trainer == Trainer.HEIM) bericht.gast ?: "der Gegner" else bericht.heim ?: "der Gegner"

        return Aussage(
            trainer = trainer,
            teamName = eigen,
            ueberschrift = ueberschrift(ausgang),
            text = buildString {
                append(stellungnahme(ausgang, eigen, gegner))
                ballbesitz(bericht, trainer)?.let { append(" ").append(it) }
                besonderes(bericht)?.let { append(" ").append(it) }
            },
        )
    }

    private enum class Ausgang { SIEG, UNENTSCHIEDEN, NIEDERLAGE, UNBESTIMMT }

    private fun ueberschrift(ausgang: Ausgang): String = when (ausgang) {
        Ausgang.SIEG -> "„Wir haben an uns geglaubt und uns belohnt!“"
        Ausgang.UNENTSCHIEDEN -> "„Ein Punkt, der sich wie ein kleiner Sieg anfühlt!“"
        Ausgang.NIEDERLAGE -> "„Heute hat am Ende die Cleverness gefehlt“"
        Ausgang.UNBESTIMMT -> "„Mein Blick auf das Spiel“"
    }

    private fun stellungnahme(ausgang: Ausgang, eigen: String, gegner: String): String = when (ausgang) {
        Ausgang.SIEG -> buildString {
            append("Was für ein Auftritt von $eigen! Den Sieg gegen $gegner haben wir uns ")
            append("hart erkämpft, und er ist auch verdient. Die Mannschaft hat über 90 Minuten ")
            append("an sich geglaubt, Leidenschaft und unbedingten Willen gezeigt. ")
            append("Ich bin mächtig stolz auf diese Truppe!")
        }
        Ausgang.UNENTSCHIEDEN -> buildString {
            append("Ein Punkt, aber ein verdienter! Gegen $gegner haben wir alles abverlangt ")
            append("und einen Kampf geliefert, der sich gewaschen hat. Am Ende fehlte das ")
            append("Quäntchen Glück im Abschluss, doch mit diesem Kampfgeist können wir ")
            append("zufrieden in die Kabine gehen.")
        }
        Ausgang.NIEDERLAGE -> buildString {
            append("Das Ergebnis tut richtig weh, weil die Mannschaft heute viel investiert hat. ")
            append("Gegen $gegner haben wir uns nie aufgegeben, aber am Ende fehlten uns die ")
            append("Cleverness und das nötige Glück. Jetzt heißt es: Ärmel hochkrempeln, ")
            append("aufarbeiten und schnell wieder angreifen.")
        }
        Ausgang.UNBESTIMMT -> buildString {
            append("Es war ein intensives Spiel, in dem sich beide Mannschaften nichts geschenkt haben. ")
            append("Ich bin mit der Moral meiner Spieler zufrieden – alles Weitere sehen wir uns genau an.")
        }
    }

    /** Floskeln aus der Ballbesitz-Statistik (Format „heim : gast“). */
    private fun ballbesitz(bericht: SpielBericht, trainer: Trainer): String? {
        val wert = bericht.statistik?.ballbesitz ?: return null
        val heim = wert.substringBefore(":").trim().toIntOrNull() ?: return null
        val gast = wert.substringAfter(":").trim().toIntOrNull() ?: return null
        val eigen = if (trainer == Trainer.HEIM) heim else gast
        val fremd = if (trainer == Trainer.HEIM) gast else heim
        return when {
            eigen - fremd >= 10 -> "Ganz klar: Wir hatten das Spiel über weite Strecken unter Kontrolle."
            fremd - eigen >= 10 -> "Ehrlich gesagt hatten die Gegner zu viel Ballbesitz – das müssen wir dringend besser machen."
            else -> null
        }
    }

    /** Kurzer Verweis auf besondere Ereignisse des Spiels. */
    private fun besonderes(bericht: SpielBericht): String? {
        val roteKarten = bericht.ereignisse.count { it.typ == BerichtEreignisTyp.ROTE_KARTE }
        val verletzungen = bericht.ereignisse.count { it.typ == BerichtEreignisTyp.VERLETZUNG }
        val elfmeter = bericht.ereignisse.count { it.typ == BerichtEreignisTyp.ELFMETER }

        val saetze = mutableListOf<String>()
        if (roteKarten > 0) {
            saetze += if (roteKarten == 1) "Die Rote Karte hat die Partie zusätzlich aufgeheizt."
            else "Die roten Karten haben das Spiel unnötig aufgeheizt."
        }
        if (verletzungen > 0) {
            saetze += "Die Verletzungsunterbrechungen haben uns aus dem Rhythmus gebracht."
        }
        if (elfmeter > 0) {
            saetze += "Vom Elfmeterpunkt aus ging es heute hoch her."
        }
        return saetze.joinToString(" ").ifBlank { null }
    }
}