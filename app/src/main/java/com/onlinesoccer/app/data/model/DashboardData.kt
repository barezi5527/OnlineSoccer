package com.onlinesoccer.app.data.model

/** Persönliche Kernwerte vom Dashboard der Hauptseite. */
data class DashboardData(
    val teamId: Int?,
    val teamName: String?,
    val liga: String?,
    val logins: String?,
    val kontostand: String?,
    val zugabgabeStatus: String?,
    val pmNeu: String?,
    val fssEinladungen: String?,
    val zat: String?,
    val zatDatum: String?,
    val letztesSpiel: MatchInfo?,
    val naechstesSpiel: MatchInfo?,
    val rows: List<LabeledValue>,
    val teamLogoUrl: String? = null,
    val forumUrl: String? = null,
) {
    data class LabeledValue(val label: String, val value: String)

    data class MatchInfo(
        val art: String?,
        val heim: Boolean,
        val gegner: String?,
        val gegnerId: Int?,
        val berichtUrl: String? = null,
        val gepaartZat: Int?,
    )
}
