package com.onlinesoccer.app.data.model

/**
 * Sonderfähigkeit eines Spielers (Spalte „S“ der Kaderübersicht `showteam.php?s=0`).
 *
 * Kürzel und Einschränkungen entsprechen der Website, die Attribute zeigen,
 * welche Werte von der Sonderfähigkeit gestärkt werden.
 */
enum class SonderFaehigkeit(
    val kuerzel: String,
    val bezeichnung: String,
    val positionsHinweis: String?,
    val attribute: List<String>,
) {
    ELFMETERKILLER("E", "Elfmeterkiller", "Nur Torhüter", listOf("Reflexe", "Spiel auf der Linie", "Fangsicherheit")),
    LIBERO("L", "Libero", "Nur Abwehr und Defensives Mittelfeld", listOf("Deckung", "Übersicht", "Zweikampf")),
    SPIELMACHER("S", "Spielmacher", "Nur Feldspieler", listOf("Übersicht", "Passgenauigkeit", "Ballkontrolle")),
    FREISTOSSSPEZIALIST("F", "Freistoß-Spezialist", "Nur Feldspieler", listOf("Schussgenauigkeit", "Übersicht", "Ballkontrolle")),
    TORINSTINKT("T", "Torinstinkt", "Nur Feldspieler", listOf("Schussgenauigkeit", "Kopfball", "Geschwindigkeit")),
    FLANKENGOTT("G", "Flankengott", "Nur Feldspieler", listOf("Passgenauigkeit", "Geschwindigkeit", "Ballkontrolle")),
    KAPITAEN("K", "Kapitän", null, listOf("Führungsqualität", "Erfahrung", "Einstellung")),
    PFERDELUNGE("P", "Pferdelunge", "Nur Feldspieler", listOf("Ausdauer", "Geschwindigkeit", "Zuverlässigkeit")),
    ;

    companion object {
        fun vonKuerzel(kuerzel: String): SonderFaehigkeit? =
            entries.firstOrNull { it.kuerzel.equals(kuerzel, ignoreCase = true) }
    }
}