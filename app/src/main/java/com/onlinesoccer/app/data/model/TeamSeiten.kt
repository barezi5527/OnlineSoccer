package com.onlinesoccer.app.data.model

/** Ein Karten-Eintrag einer Übersichtsseite (z. B. ein Jugendspieler oder eine Buchung). */
data class UebersichtZeile(
    val ueberschrift: String,
    val untertitel: String? = null,
    val werte: List<Pair<String, String>> = emptyList(),
    /** Schlüssel zur Zuordnung zeilenübergreifender Aktionen (z. B. Spieler-ID). */
    val aktionSchluessel: String? = null,
    /** Text der verfügbaren Aktion im ⋮-Menü der Karte. */
    val aktionTitel: String? = null,
)

/** Ein Abschnitt einer Übersichtsseite (Titel + Einzelwerte/Merkpunkte/Karten + evtl. Formulare). */
data class UebersichtAbschnitt(
    val titel: String? = null,
    val infoZeilen: List<Pair<String, String>> = emptyList(),
    val punkte: List<String> = emptyList(),
    val zeilen: List<UebersichtZeile> = emptyList(),
    val aktionen: List<AktionForm> = emptyList(),
    /** Optionaler Stadionplan (wird in der Anzeige „Stadionausbau“ gezeichnet). */
    val stadionPlan: StadionPlanDaten? = null,
)

/**
 * Ein Formular der Website, das in der App nachgebaut wird (z. B. „Markierten
 * Spieler ins A-Team berufen", „Verträge verlängern", Stadion ausbauen).
 * Der Nutzer füllt es aus und sendet es per POST an [ziel].
 */
data class AktionForm(
    val ziel: String,
    val felder: List<AktionFeld> = emptyList(),
    val zeilen: List<AktionZeile> = emptyList(),
    val buttons: List<AktionsButton> = emptyList(),
    val titel: String? = null,
)

/** Eine Datenzeile des Formulars mit eigenen Eingabefeldern (z. B. `gehalt[pid]`-Radios). */
data class AktionZeile(
    val schluessel: String,
    val bezeichnung: String,
    val felder: List<AktionFeld> = emptyList(),
    /** Wenn gesetzt, öffnet die Zeile statt Inline-Feldern ein separates Formular (z. B. Gebot). */
    val ziel: String? = null,
    val button: AktionsButton? = null,
)

/** Eingabefeld eines Formulars (Select, Zahl, Text, verstecktes Pflichtfeld oder Radiogruppe). */
data class AktionFeld(
    val name: String,
    val typ: AktionFeldTyp = AktionFeldTyp.AUSWAHL,
    val label: String = "",
    val optionen: List<AktionsOption> = emptyList(),
    val standard: String = optionen.firstOrNull()?.wert ?: "",
    val min: Int? = null,
    val max: Int? = null,
)

enum class AktionFeldTyp {
    /** Dropdown-/Listenauswahl (select). */
    AUSWAHL,
    /** Radiogruppe (eine Auswahl je Gruppe, z. B. ziehmich / gehalt[pid]). */
    RADIO,
    /** Numerische Eingabe (input type=number bzw. Geldbetrag). */
    NUMMER,
    /** Freitext (input type=text). */
    TEXT,
    /** Unsichtbares Pflichtfeld (hidden input): wird immer mitgesendet. */
    VERSTECKT,
}

data class AktionsOption(val wert: String, val label: String)

/** Absende-Button eines Formulars; Name/Wert werden wie im Browser mitgesendet. */
data class AktionsButton(
    val name: String?,
    val text: String,
    /** Sicherheitsabfrage vor dem Absenden (Standard: an für verändernde Aktionen). */
    val bestaetigung: Boolean = true,
)