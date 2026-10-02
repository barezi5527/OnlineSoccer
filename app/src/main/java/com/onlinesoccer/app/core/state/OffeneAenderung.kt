package com.onlinesoccer.app.core.state

import jakarta.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Registriert, in welchen Bildschirmen eine **ungespeicherte** Änderung offen ist.
 *
 * Zweck: Der 1|2-Button darf einen Teamwechsel nicht still verwerfen. Solange
 * eine Aufstellung oder Taktik bearbeitet, aber nicht gespeichert wurde, muss
 * der Nutzer den Wechsel ausdrücklich bestätigen — sonst gehen seine Änderungen
 * verloren, weil T26 den Backstack samt ViewModel zerstört.
 *
 * Warum ein Singleton und nicht ein Feld im `TeamwechselUiState`: die betroffenen
 * ViewModels leben an ihrem `NavBackStackEntry` (`hiltViewModel()`), die TopAppBar
 * sieht sie nicht. Ein Singleton ist die einzige Stelle, die beide Seiten kennen.
 *
 * Die Einträge werden in `onCleared()` der ViewModels wieder entfernt — sonst
 * bliebe der Button nach T26 dauerhaft gesperrt.
 */
@Singleton
class OffeneAenderung @Inject constructor() {
    private val _offen = MutableStateFlow<Set<String>>(emptySet())
    val offen: StateFlow<Set<String>> = _offen.asStateFlow()

    fun oeffnen(bereich: String) {
        _offen.value = _offen.value + bereich
    }

    fun schliessen(bereich: String) {
        _offen.value = _offen.value - bereich
    }

    fun istOffen(bereich: String): Boolean = bereich in _offen.value
}

/** Schlüssel der Bildschirme mit möglicher offener Änderung. */
object AenderungBereich {
    const val ZUGABABE = "Zugababe"
    const val TAKTIK = "Taktik"
}

/**
 * Text für den Bestätigungsdialog. Rein, ohne Netz — dadurch in T36 testbar.
 */
internal fun bestaetigungstext(bereiche: Set<String>): String? {
    if (bereiche.isEmpty()) return null
    val lesbar = bereiche.sorted().joinToString(" und ")
    return "$lesbar offen — wirklich wechseln? Nicht gespeicherte Änderungen gehen verloren."
}
