package com.onlinesoccer.app.data.model

data class InternationaleOption(
    val wert: String,
    val label: String,
)

data class InternationaleFilter(
    val saison: String? = null,
    val runde: String? = null,
    val gruppe: String? = null,
    val ergebnisse: String? = null,
)

data class InternationaleAnsicht(
    val titel: String? = null,
    val saisonen: List<InternationaleOption> = emptyList(),
    val runden: List<InternationaleOption> = emptyList(),
    val gruppen: List<InternationaleOption> = emptyList(),
    val ergebnisOptionen: List<InternationaleOption> = emptyList(),
    val filter: InternationaleFilter = InternationaleFilter(),
    val abschnitte: List<String> = emptyList(),
    val tabellen: List<WertTabelle> = emptyList(),
    val spiele: List<InternationaleSpiel> = emptyList(),
)

data class InternationaleSpiel(
    val heim: String,
    val gast: String,
    val ergebnis: String? = null,
    val berichtUrl: String? = null,
)
