package com.onlinesoccer.app.feature.bewerbe

import com.onlinesoccer.app.data.model.WertTabelle
import java.text.Normalizer

/** Filtert das Club-Ranking lokal und behält Rang, Team-IDs und Zeilenklassen bei. */
internal fun clubRankingGefiltert(tabelle: WertTabelle, suchtext: String): WertTabelle {
    val clubSpalte = tabelle.header.indexOfFirst { it.equals("Club", ignoreCase = true) }
    if (suchtext.isBlank() || clubSpalte < 0) return tabelle

    val suche = normalisiereVereinsname(suchtext)
    val treffer = tabelle.zeilen.indices.filter { index ->
        normalisiereVereinsname(tabelle.zeilen[index].getOrNull(clubSpalte).orEmpty()).contains(suche)
    }
    return tabelle.copy(
        zeilen = treffer.map { tabelle.zeilen[it] },
        teamIds = if (tabelle.teamIds.isEmpty()) emptyList() else treffer.map { tabelle.teamIds.getOrNull(it) },
        zeilenKlasse = if (tabelle.zeilenKlasse.isEmpty()) {
            emptyList()
        } else {
            treffer.map { tabelle.zeilenKlasse.getOrNull(it) }
        },
    )
}

private fun normalisiereVereinsname(text: String): String =
    Normalizer.normalize(text.lowercase().replace("ß", "ss"), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
