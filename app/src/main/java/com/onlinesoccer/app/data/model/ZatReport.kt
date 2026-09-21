package com.onlinesoccer.app.data.model

/**
 * Persönlicher ZAT-Report (`zar.php`): Einnahmen/Ausgaben und Trainingserfolge
 * der eigenen Mannschaft für einen bestimmten ZAT.
 */
data class ZatReport(
    val zat: Int?,
    val saison: Int?,
    val einnahmen: List<ZatReportEinnahme>,
    val trainingserfolge: List<ZatReportTraining>,
    /** Im Formular der Seite wählbare ZATs (für die gewählte Saison). */
    val zats: List<Int> = emptyList(),
    /** Im Formular der Seite wählbare Saisons. */
    val saisons: List<Int> = emptyList(),
)

/** Eine Zeile im Abschnitt „1. Einnahmen / Ausgaben“ (inkl. Gesamtsumme). */
data class ZatReportEinnahme(
    val label: String,
    val wert: String,
)

/** Eine Zeile im Abschnitt „2. Trainingserfolge“. */
data class ZatReportTraining(
    val name: String,
    val pid: Long?,
    val position: String?,
    val beschreibung: String,
    val wert: String?,
)