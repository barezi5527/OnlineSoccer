package com.onlinesoccer.app.data.model

/** Ein Spieler des eigenen Kaders (Teamübersicht `showteam.php?s=0`). */
data class KaderSpieler(
    val pid: Long,
    val name: String,
    val nummer: String,
    val alter: Int?,
    val position: SpielerPosition,
    val skill: Double,
    val opti: Double,
    val fit: Int,
    val mor: Int,
    val sonderFaehigkeiten: List<SonderFaehigkeit> = emptyList(),
    /** Spalte „Sperre“ wie auf der Website: Kürzel („1L“, „2P“, „2I“ …); „0“ bedeutet keine Sperre. */
    val sperre: String = "",
) {
    /** Gesperrt: Spalte „Sperre“ enthält eine aktive Sperre (z. B. „1L“, „3P 1I“); „0“ bedeutet keine. */
    val gesperrt: Boolean get() = sperre.isNotBlank() && sperre != "0"
}