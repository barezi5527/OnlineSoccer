package com.onlinesoccer.app.feature.team

import com.onlinesoccer.app.data.model.SeitenAnsicht
import com.onlinesoccer.app.data.model.UebersichtZeile

internal fun laufzeitZahl(wert: String?): Int =
    wert?.trim()?.removeSuffix(" Monate")?.trim()?.toIntOrNull() ?: Int.MAX_VALUE

private fun UebersichtZeile.laufzeit(): String? =
    werte.firstOrNull { it.first.equals("Laufzeit", ignoreCase = true) }?.second

private fun vertragsZeilenComparator(sortierung: VertragsSortierung): Comparator<UebersichtZeile> =
    when (sortierung) {
        VertragsSortierung.LAUFZEIT -> compareBy<UebersichtZeile> { laufzeitZahl(it.laufzeit()) }
            .thenBy { it.ueberschrift.lowercase() }
        VertragsSortierung.NAME -> compareBy<UebersichtZeile> { it.ueberschrift.lowercase() }
        VertragsSortierung.POSITION -> compareBy<UebersichtZeile> { it.position?.rang ?: Int.MAX_VALUE }
            .thenBy { laufzeitZahl(it.laufzeit()) }
            .thenBy { it.ueberschrift.lowercase() }
    }

internal fun sortierteVerlaengerungen(
    ansicht: SeitenAnsicht,
    sortierung: VertragsSortierung,
): SeitenAnsicht {
    if (ansicht.titel != "Verträge verlängern") return ansicht
    val comparator = vertragsZeilenComparator(sortierung)
    return ansicht.copy(
        abschnitte = ansicht.abschnitte.map { abschnitt ->
            abschnitt.copy(zeilen = abschnitt.zeilen.sortedWith(comparator))
        },
    )
}
