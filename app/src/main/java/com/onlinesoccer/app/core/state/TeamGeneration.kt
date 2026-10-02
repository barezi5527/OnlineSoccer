package com.onlinesoccer.app.core.state

import jakarta.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Zaehlt die bestaetigten Teamwechsel — das ist die **Schreibsperre-Generation**
 * fuer alle ViewModels, die Daten des jeweils aktiven Teams geladen haben.
 *
 * Grund (Plan Block 6): ein ViewModel laeuft an seinem `NavBackStackEntry` und
 * kann einen Teamwechsel theoretically ueberleben, wenn er den Reset verpasst
 * (Hintergrund-Coroutine, laufender Request). Seine Spieler-PIDs gehoeren dann
 * zum **vorherigen** Team; ein POST wuerde Team-1-Inhalt in Team 2 schreiben.
 * Das ist Datenbeschaedigung auf einem fremden Spielerserver, kein Kosmetikfehler.
 *
 * Deshalb haelt jeder Writer die Generation, unter der er geladen hat, und
 * vergleicht sie unmittelbar vor dem POST. Ein Teamwechsel **erhoeht** die
 * Generation, ein Wechsel **zurueck** auf Team 1 ebenfalls — die Sperre ist
 * damit richtungsunabhaengig und braucht keinen Zaehler ueber die Logins hinweg.
 */
@Singleton
class TeamGeneration @Inject constructor() {
    private val _value = MutableStateFlow(0L)
    val value: StateFlow<Long> = _value.asStateFlow()

    /** Ein bestaetigter Teamwechsel. Ein **fehlgeschlagener** erhoeht nichts. */
    fun increment() {
        _value.update { it + 1 }
    }

    fun current(): Long = _value.value
}

/** Meldung, wenn das Gate einen Schreibvorgang ablehnt. */
const val TEAMWECHSEL_VERWORFEN =
    "Team wurde gewechselt – bitte neu laden und erneut speichern."

/**
 * Gate vor jedem Schreibvorgang. `true` = erlaubt.
 *
 * Bewusst eine **reine** Funktion ohne Netz und ohne Android: genau so ist sie in
 * T36 ohne Mocking testbar. Gleichheit ist das einzige Kriterium — ein „gleiche
 * Generation ja, hoehere nein"-Vergleich waere ebenfalls korrekt, aber die
 * Generation kann nur durch `increment()` wachsen, also ist Gleichheit äquivalent
 * und einfacher zu lesen.
 */
internal fun schreibvorgangErlaubt(geladeneGeneration: Long, aktuelleGeneration: Long): Boolean =
    geladeneGeneration == aktuelleGeneration
