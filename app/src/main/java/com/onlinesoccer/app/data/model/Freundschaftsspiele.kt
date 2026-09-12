package com.onlinesoccer.app.data.model

data class FreundschaftOption(val value: String, val label: String)

data class FreundschaftSpiel(
    val id: String? = null,
    val zat: String? = null,
    val gegner: String,
    val details: String = "",
    val stornierbar: Boolean = false,
)

data class Freundschaftsdaten(
    val spiele: List<FreundschaftSpiel> = emptyList(),
    val reservierteZats: List<FreundschaftOption> = emptyList(),
    val reservierbareZats: List<FreundschaftOption> = emptyList(),
    val blindZats: List<FreundschaftOption> = emptyList(),
    val laender: List<FreundschaftOption> = emptyList(),
    val ligen: List<FreundschaftOption> = emptyList(),
    val teams: List<FreundschaftOption> = emptyList(),
    val meldungen: List<String> = emptyList(),
)
