package com.onlinesoccer.app.feature.bewerbe

import com.onlinesoccer.app.data.model.InternationaleSpiel

/** Qualifikations-Hin- und Rückspiel, gruppiert nach den beiden Team-IDs. */
internal data class InternationalePaarung(
    val hinspiel: InternationaleSpiel,
    val rueckspiel: InternationaleSpiel?,
)

/**
 * Die Website liefert Hin- und Rückspiel als einzelne Einträge im selben Paarungsblock.
 * Die ID-Sortierung gruppiert beide Heim-/Gast-Richtungen, ohne Gruppenspiele derselben
 * Teams versehentlich zusammenzufassen (diese Funktion wird nur für Qualifikationen genutzt).
 */
internal fun qualifikationsPaarungen(spiele: List<InternationaleSpiel>): List<InternationalePaarung> {
    val gruppen = linkedMapOf<String, MutableList<InternationaleSpiel>>()
    spiele.forEachIndexed { index, spiel ->
        val ids = listOfNotNull(spiel.heimTeamId, spiel.gastTeamId).sorted()
        val schluessel = if (ids.size == 2 && ids[0] != ids[1]) {
            "id:${ids[0]}:${ids[1]}"
        } else {
            val namen = listOf(spiel.heim, spiel.gast).sortedBy { it.lowercase() }
            "name:${namen.joinToString("|") { it.lowercase() }}:$index"
        }
        gruppen.getOrPut(schluessel) { mutableListOf() } += spiel
    }
    return gruppen.values.map { spieleDerPaarung ->
        InternationalePaarung(
            hinspiel = spieleDerPaarung.first(),
            rueckspiel = spieleDerPaarung.getOrNull(1),
        )
    }
}
