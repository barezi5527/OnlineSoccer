package com.onlinesoccer.app.feature.bewerbe

import com.onlinesoccer.app.data.model.BerichtEreignisTyp
import com.onlinesoccer.app.data.model.SpielBericht
import com.onlinesoccer.app.data.repository.ElfAuswertung

/**
 * Erzeugt die Aussage des Heim- oder Gasttrainers zur Pressekonferenz direkt
 * aus dem geparsten Spielbericht. Deterministisch und offline – wie die
 * „Elf des Spieltags" stammt jede Aussage ausschließlich aus den Berichtsdaten.
 *
 * Heim- und Gasttrainer erhalten bewusst unterschiedliche Aussagen: eigene
 * Überschriften, eine trainerabhängige Stellungnahme sowie teambezogene
 * Einordnung von Ballbesitz und besonderen Ereignissen (Karten, Verletzungen,
 * Elfmeter).
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
            ueberschrift = ueberschrift(ausgang, trainer),
            text = buildString {
                append(stellungnahme(ausgang, trainer, eigen, gegner))
                ballbesitz(bericht, trainer)?.let { append(" ").append(it) }
                besonderes(bericht, trainer)?.let { append(" ").append(it) }
            },
        )
    }

    private enum class Ausgang { SIEG, UNENTSCHIEDEN, NIEDERLAGE, UNBESTIMMT }

    private fun ueberschrift(ausgang: Ausgang, trainer: Trainer): String = when (ausgang) {
        Ausgang.SIEG -> if (trainer == Trainer.HEIM)
            "„Ein Heimsieg zum Feiern!“"
        else
            "„Drei Punkte aus der Fremde – Wahnsinn!“"
        Ausgang.UNENTSCHIEDEN -> if (trainer == Trainer.HEIM)
            "„Zu Hause mehr erhofft, den Punkt aber mitgenommen.“"
        else
            "„Ein Auswärtspunkt, der sich wie ein Sieg anfühlt.“"
        Ausgang.NIEDERLAGE -> if (trainer == Trainer.HEIM)
            "„Eine Heimniederlage, die doppelt weh tut.“"
        else
            "„Auswärts zu harmlos – das müssen wir ändern.“"
        Ausgang.UNBESTIMMT -> if (trainer == Trainer.HEIM)
            "„Mein Blick auf unser Heimspiel.“"
        else
            "„Mein Blick auf unser Auswärtsspiel.“"
    }

    private fun stellungnahme(ausgang: Ausgang, trainer: Trainer, eigen: String, gegner: String): String = when (ausgang) {
        Ausgang.SIEG -> if (trainer == Trainer.HEIM) buildString {
            append("Was für ein Auftritt von $eigen! Vor eigenem Publikum haben wir $gegner ")
            append("über weite Strecken beherrscht und uns den Sieg redlich verdient. ")
            append("Die Mannschaft hat von der ersten Minute an an sich geglaubt – ich bin mächtig stolz auf diese Truppe!")
        } else buildString {
            append("Was für eine reife Leistung von $eigen! Bei $gegner so cool zu bleiben und ")
            append("die Punkte mitzunehmen, verlangt besonderen Respekt. ")
            append("Wir haben unsere Chancen eiskalt genutzt und hinten kaum etwas zugelassen – ich bin stolz auf jeden Einzelnen!")
        }
        Ausgang.UNENTSCHIEDEN -> if (trainer == Trainer.HEIM) buildString {
            append("Ein Punkt gegen $gegner, mit dem $eigen am Ende leben kann. ")
            append("Zu Hause wollten wir mehr und hatten auch die besseren Momente, doch das letzte Quäntchen hat gefehlt. ")
            append("Mit dem Kampfgeist meiner Mannschaft bin ich zufrieden.")
        } else buildString {
            append("$eigen hat sich bei $gegner einen Punkt erkämpft, und der ist für uns Gold wert. ")
            append("Auswärts haben wir wenig zugelassen und unsere wenigen Chancen konsequent genutzt. ")
            append("Mit diesem Auftritt können wir sehr zufrieden die Rückreise antreten.")
        }
        Ausgang.NIEDERLAGE -> if (trainer == Trainer.HEIM) buildString {
            append("Das Ergebnis gegen $gegner tut richtig weh, weil $eigen heute viel investiert hat. ")
            append("Vor eigenem Publikum fehlten uns am Ende Cleverness und Konsequenz. ")
            append("Jetzt gilt es, die richtigen Schlüsse zu ziehen und schnell wieder anzugreifen.")
        } else buildString {
            append("$eigen hat sich bei $gegner nie aufgegeben, aber am Ende wurden wir für unsere Fehler bestraft. ")
            append("Auswärts haben wir zu selten unser eigenes Spiel durchgesetzt. ")
            append("Wir müssen das aufarbeiten und schnell wieder in die Spur kommen.")
        }
        Ausgang.UNBESTIMMT -> if (trainer == Trainer.HEIM) buildString {
            append("Es war ein intensives Heimspiel, in dem $eigen und $gegner sich nichts geschenkt haben. ")
            append("Ich bin mit der Moral meiner Spieler zufrieden – wir nehmen die positiven Ansätze mit.")
        } else buildString {
            append("$eigen hat bei $gegner eine ordentliche Vorstellung abgeliefert. ")
            append("Auch wenn nicht alles rund lief, stimmt die Moral – darauf bauen wir auf.")
        }
    }

    /** Floskeln aus der Ballbesitz-Statistik (Format „heim : gast“). */
    private fun ballbesitz(bericht: SpielBericht, trainer: Trainer): String? {
        val wert = bericht.statistik?.ballbesitz ?: return null
        val heim = wert.substringBefore(":").trim().removeSuffix("%").toIntOrNull() ?: return null
        val gast = wert.substringAfter(":").trim().removeSuffix("%").toIntOrNull() ?: return null
        val eigen = if (trainer == Trainer.HEIM) heim else gast
        val fremd = if (trainer == Trainer.HEIM) gast else heim
        return when {
            eigen - fremd >= 10 -> "Ganz klar: Wir hatten das Spiel über weite Strecken unter Kontrolle."
            fremd - eigen >= 10 -> "Ehrlich gesagt hatten die Gegner zu viel Ballbesitz – das müssen wir dringend besser machen."
            else -> null
        }
    }

    /** Teambezogener Verweis auf besondere Ereignisse des Spiels. */
    private fun besonderes(bericht: SpielBericht, trainer: Trainer): String? {
        val heimNamen = bericht.heimAufstellung?.spieler.orEmpty()
            .mapTo(mutableSetOf()) { it.name.lowercase() }
        val gastNamen = bericht.gastAufstellung?.spieler.orEmpty()
            .mapTo(mutableSetOf()) { it.name.lowercase() }
        val bekannteNamen = (bericht.heimAufstellung?.spieler.orEmpty().map { it.name } +
            bericht.gastAufstellung?.spieler.orEmpty().map { it.name }).toSet()

        fun seite(name: String): Trainer? = when {
            name.lowercase() in heimNamen -> Trainer.HEIM
            name.lowercase() in gastNamen -> Trainer.GAST
            else -> null
        }

        val roteKarten = ElfAuswertung.kartenEreignisse(bericht.ereignisse, bekannteNamen)
            .filter { it.third == BerichtEreignisTyp.ROTE_KARTE }
            .mapNotNull { seite(it.second) }
        val verletzungen = ElfAuswertung.verletzteSpieler(bericht.ereignisse, bekannteNamen)
            .values.mapNotNull { seite(it) }
        val elfmeter = bericht.ereignisse
            .filter { it.typ == BerichtEreignisTyp.ELFMETER }
            .mapNotNull { ereignis ->
                bekannteNamen
                    .filter { name -> ereignis.text.contains(name, ignoreCase = true) }
                    .mapNotNull { seite(it) }
                    .toSet()
                    .singleOrNull()
            }

        val eigeneRote = roteKarten.count { it == trainer }
        val gegnerRote = roteKarten.count { it != trainer }
        val eigeneVerletzungen = verletzungen.count { it == trainer }
        val gegnerVerletzungen = verletzungen.count { it != trainer }
        val eigeneElfmeter = elfmeter.count { it == trainer }
        val gegnerElfmeter = elfmeter.count { it != trainer }

        val saetze = mutableListOf<String>()
        if (eigeneRote == 1) saetze += "Die Rote Karte gegen uns hat uns unnötig geschwächt."
        else if (eigeneRote > 1) saetze += "Die Roten Karten gegen uns haben uns das Leben selbst schwer gemacht."
        if (gegnerRote == 1) saetze += "Die Rote Karte gegen den Gegner hat uns in die Karten gespielt."
        else if (gegnerRote > 1) saetze += "Die Roten Karten beim Gegner haben das Spiel zu unseren Gunsten gedreht."
        if (eigeneVerletzungen > 0) saetze += "Die Verletzungsunterbrechungen in unseren Reihen haben uns aus dem Rhythmus gebracht."
        if (gegnerVerletzungen > 0) saetze += "Die Verletzungsunterbrechungen beim Gegner haben das Tempo aus dem Spiel genommen."
        if (eigeneElfmeter > 0) saetze += "Vom Elfmeterpunkt aus haben wir heute unsere Chance gesucht."
        if (gegnerElfmeter > 0) saetze += "Vom Elfmeterpunkt aus wurde der Gegner heute belohnt."

        return saetze.joinToString(" ").ifBlank { null }
    }
}
