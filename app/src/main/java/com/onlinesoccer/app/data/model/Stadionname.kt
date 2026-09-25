package com.onlinesoccer.app.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

data class StadionnameOverride(
    val teamId: Long,
    val name: String,
    val aktivSeitMillis: Long,
)

object StadionnameLogik {
    const val MAX_LAENGE = 60

    private val datumsformat = DateTimeFormatter.ofPattern("dd.MM.uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    fun normalisiere(eingabe: String): String = eingabe.trim()

    fun fehler(eingabe: String): String? {
        val name = normalisiere(eingabe)
        return when {
            name.isEmpty() -> null
            name.length > MAX_LAENGE -> "Der Name darf höchstens $MAX_LAENGE Zeichen haben."
            name.any { it.isISOControl() || it == '\u2028' || it == '\u2029' } ->
                "Der Name darf keine Zeilenumbrüche oder Steuerzeichen enthalten."
            else -> null
        }
    }

    fun istGueltig(name: String): Boolean =
        normalisiere(name).isNotEmpty() && fehler(name) == null

    fun anzeigename(
        bericht: SpielBericht,
        teamId: Long?,
        gespeichert: StadionnameOverride?,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String? {
        if (teamId == null || teamId <= 0 || gespeichert?.teamId != teamId) return null
        if (bericht.heimId != teamId || bericht.saison <= 0 || bericht.zat <= 0) return null
        if (gespeichert.aktivSeitMillis <= 0 || !istGueltig(gespeichert.name)) return null

        val berichtsDatum = runCatching { LocalDate.parse(bericht.datum?.trim(), datumsformat) }.getOrNull()
            ?: return null
        val aktivierungsDatum = runCatching {
            Instant.ofEpochMilli(gespeichert.aktivSeitMillis).atZone(zoneId).toLocalDate()
        }.getOrNull() ?: return null

        return gespeichert.name.takeIf { berichtsDatum.isAfter(aktivierungsDatum) }
    }
}
