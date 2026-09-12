package com.onlinesoccer.app.data.model

/** Eine generische Tabellenansicht (Header + Datenzeilen) für die Team-/Liga-Tabellen. */
data class WertTabelle(
    val header: List<String> = emptyList(),
    val zeilen: List<List<String>> = emptyList(),
) {
    fun spalte(label: String): Int =
        header.indexOfFirst { it.contains(label, ignoreCase = true) }

    fun wert(zeile: List<String>, label: String): String? =
        spalte(label).takeIf { it >= 0 }?.let { zeile.getOrNull(it) }
            ?.takeIf { it.isNotBlank() }
}

/** Eine Zeile der Teamübersicht (Verträge, `showteam.php?s=1`) je Spieler. */
data class VertragZeile(
    val pid: Long,
    val name: String,
    val gehalt: String? = null,
    val laufzeit: String? = null,
    val marktwert: String? = null,
    val geburtstag: String? = null,
)

/** Stärken-Einzelwerte (`showteam.php?s=2`): 18 Skill-Werte je Spieler. */
data class StaerkeZeile(
    val pid: Long,
    val name: String,
    val werte: Map<String, String> = emptyMap(),
)

/** Statistik je Spieler (`showteam.php?s=3` Saison / `s=4` Gesamt). */
data class StatistikZeile(
    val pid: Long,
    val name: String,
    val werte: Map<String, String> = emptyMap(),
)

/** Teaminfo/Stadion (`showteam.php?s=5`). */
data class Teaminfo(
    val zeilen: List<Pair<String, String>> = emptyList(),
)

/** Kader eines fremden Vereins (`st.php?c=<id>`). */
data class FremdesTeam(
    val teamId: Long,
    val name: String = "",
    val liga: String = "",
    val kader: List<KaderSpieler> = emptyList(),
)

/** Zusammengeführte Spielerkarte (Spielerprofil). */
data class SpielerKarte(
    val pid: Long,
    val name: String,
    val nummer: String,
    val alter: Int?,
    val position: SpielerPosition,
    val kader: KaderSpieler? = null,
    val vertrag: VertragZeile? = null,
    val staerken: Map<String, String> = emptyMap(),
    val statistikSaison: Map<String, String> = emptyMap(),
    val statistikGesamt: Map<String, String> = emptyMap(),
    val profilRohtext: List<String> = emptyList(),
) {
    val sonderFaehigkeiten: List<SonderFaehigkeit> get() = kader?.sonderFaehigkeiten.orEmpty()
    val skill: Double get() = kader?.skill ?: (staerken["Skillschnitt"]?.toDoubleOrNull() ?: 0.0)
    val opti: Double get() = kader?.opti ?: (staerken["Opt. Skill"]?.toDoubleOrNull() ?: 0.0)
    val fit: Int get() = kader?.fit ?: 0
    val mor: Int get() = kader?.mor ?: 0
}