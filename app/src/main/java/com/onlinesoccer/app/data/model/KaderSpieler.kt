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
    /** Spalte „Verl.“ wie auf der Website: verbleibende Verletzung in ZAT; „0“ bedeutet nicht verletzt. */
    val verletzung: String = "",
) {
    /** Gesperrt: Spalte „Sperre“ enthält eine aktive Sperre (z. B. „1L“, „3P 1I“); „0“ bedeutet keine. */
    val gesperrt: Boolean get() = sperre.isNotBlank() && sperre != "0"

    /**
     * Verletzt: Spalte „Verl.“ enthält eine Verletzung. FIT ist kein Ersatz –
     * auch fitlose, nicht verletzte Spieler kommen vor.
     */
    val verletzt: Boolean get() = verletzung.isNotBlank() && verletzung != "0"

    /**
     * Verletzung als Kürzel, analog zur Sperre: „1V“ = 1 ZAT verletzt (Sperre
     * dagegen „1L“ = 1 Ligaspiel, „1P“ = 1 Pokalspiel, „1I“ = 1 internationales
     * Spiel). Die Spalte „Verl.“ enthält nur die Zahl, das „V“ ergänzt die App.
     * Werte ohne Zahl bleiben unverändert.
     */
    val verletzungsKuerzel: String
        get() = verletzung.toIntOrNull()?.takeIf { it > 0 }?.let { "${it}V" } ?: verletzung
}