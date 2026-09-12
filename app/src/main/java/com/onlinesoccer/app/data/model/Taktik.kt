package com.onlinesoccer.app.data.model

/**
 * Taktik des Taktik-Editors (`taktiken.php`).
 *
 * Die Website nutzt `<input type="checkbox" name="taktik[]" value="<Zeile><Spalte>">`
 * mit Werten wie `O1` bis `A11`. Zeilen O–A: O oben (Sturm), A unten (Abwehr),
 * jeweils 11 Spalten. Der Torwart (fixe gelbe `T`-Zelle) ist kein Taktik-Feld.
 *
 * Zusätzlich liest die Seite zwei Auswahllisten: `raster1` (Standardtaktiken)
 * und `raster2` (eigene Taktiken) – beides als [AuswahlOption] mit Id + Label.
 */
data class Taktik(
    val codes: Set<String>,
    val speichername: String? = null,
    val standardTaktiken: List<AuswahlOption> = emptyList(),
    val eigeneTaktiken: List<AuswahlOption> = emptyList(),
) {
    val istLeer: Boolean get() = codes.isEmpty()
}