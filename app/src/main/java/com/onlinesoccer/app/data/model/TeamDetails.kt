package com.onlinesoccer.app.data.model

/** Eintrag im Teaminformationen-Menü (dynamisch von der Website geparst). */
data class TeamInfoMenuEintrag(
    val label: String,
    val path: String,
    val showteamS: String? = null,
    val tabellenplatzTeamId: Long? = null,
)

/** Zeile des Saisonplans (`showteam.php?s=6`). */
data class SaisonplanEintrag(
    val zat: String,
    val spielart: String,
    val gegner: String,
    val ergebnis: String,
    val berichtUrl: String? = null,
    val gegnerTeamId: Long? = null,
)

/** Saisonplan inkl. verfügbarer Saisons (aus dem `<select name="saison">` der Website). */
data class SaisonplanDaten(
    val saisons: List<Int> = emptyList(),
    val gewaehlteSaison: Int? = null,
    val eintraege: List<SaisonplanEintrag> = emptyList(),
)

/** Zeile der Vereinshistorie (`showteam.php?s=7`). */
data class VereinshistorieEintrag(
    val saison: String,
    val zat: String,
    val spielerAnzahl: String,
    val avgSkill: String,
    val avgOpti: String,
    val avgAlter: String,
    val avgMW: String,
    val sumMW: String,
    val avgGehalt: String,
    val sumGehalt: String,
    val manager: String,
)

/** Ein Transfer-Eintrag (`showteam.php?s=8`). */
data class TransferhistorieBlock(
    val datum: String,
    val team1: String,
    val team2: String,
    val team1Id: Long? = null,
    val team2Id: Long? = null,
    val details: List<String>,
)

/** Zeile der Leihtabelle (`showteam.php?s=9`). */
data class LeihhistorieEintrag(
    val datum: String,
    val spieler: String,
    val spielerPid: Long? = null,
    val von: String,
    val zu: String,
    val zahlung: String,
    val dauer: String,
)

/** Zeile der Saisonhistorie (`showteam.php?s=10`). */
data class SaisonhistorieEintrag(
    val saison: String,
    val liga: String,
    val tabelle: String,
    val pokal: String,
    val ose: String,
    val osc: String,
)

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
    val position: SpielerPosition = SpielerPosition.AMATEUR,
    val gehalt: String? = null,
    val laufzeit: String? = null,
    val marktwert: String? = null,
    val geburtstag: String? = null,
)

/** Stärken-Einzelwerte (`showteam.php?s=2`): 18 Skill-Werte je Spieler. */
data class StaerkeZeile(
    val pid: Long,
    val name: String,
    val position: SpielerPosition = SpielerPosition.AMATEUR,
    val werte: Map<String, String> = emptyMap(),
)

/** Statistik je Spieler (`showteam.php?s=3` Saison / `s=4` Gesamt). */
data class StatistikZeile(
    val pid: Long,
    val name: String,
    val position: SpielerPosition = SpielerPosition.AMATEUR,
    val werte: Map<String, String> = emptyMap(),
)

/** Teaminfo/Stadion (`showteam.php?s=5`). */
data class Teaminfo(
    val zeilen: List<Pair<String, String>> = emptyList(),
    /**
     * Daraus abgeleitete Stadion-Kennzahlen (Sitz-/Stehplätze, überdachte Anteile,
     * Fassungsvermögen) für den Stadionplan; `null`, wenn die Seite keine Plätze liefert.
     */
    val stadionPlan: StadionPlanDaten? = null,
    val stadionname: String? = null,
)

/**
 * Trainer/Manager eines Vereins. Nur ein aktiver Trainer (mit PM-Empfänger-ID)
 * ist per Private Nachricht erreichbar; ein freies Team hat keinen Trainer.
 */
sealed interface TeamTrainer {
    /** Der Verein hat einen aktiven Trainer (per PN erreichbar). */
    data class Besetzt(val name: String, val id: Long) : TeamTrainer

    /** Der Verein hat keinen aktiven Trainer („Team ist frei“). */
    data object Frei : TeamTrainer

    /** Trainer-Situation unbekannt (z. B. eigener Verein); nichts anzeigen. */
    data object Unbekannt : TeamTrainer
}

/** Kader eines fremden Vereins (`st.php?c=<id>`). */
data class FremdesTeam(
    val teamId: Long,
    val name: String = "",
    val liga: String = "",
    val kader: List<KaderSpieler> = emptyList(),
    val trainer: TeamTrainer = TeamTrainer.Unbekannt,
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
    /** Trainer des zugehörigen Vereins (nur bei fremden Spielern ausgefüllt). */
    val trainer: TeamTrainer = TeamTrainer.Unbekannt,
) {
    val sonderFaehigkeiten: List<SonderFaehigkeit> get() = kader?.sonderFaehigkeiten.orEmpty()
    val skill: Double get() = kader?.skill ?: (staerken["Skillschnitt"]?.toDoubleOrNull() ?: 0.0)
    val opti: Double get() = kader?.opti ?: (staerken["Opt. Skill"]?.toDoubleOrNull() ?: 0.0)
    val fit: Int get() = kader?.fit ?: 0
    val mor: Int get() = kader?.mor ?: 0
}