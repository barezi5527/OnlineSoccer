package com.onlinesoccer.app.feature.zugabgabe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.Aufstellung
import com.onlinesoccer.app.data.model.AufstellungSlot
import com.onlinesoccer.app.data.model.ERSATZBANK_BUCHSTABEN
import com.onlinesoccer.app.data.model.tauschePlaetze
import com.onlinesoccer.app.data.repository.ZugabgabeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ZugabgabeUiState(
    val ladende: Boolean = false,
    val aufstellung: Aufstellung? = null,
    val speichernd: Boolean = false,
    val taktikLadend: Boolean = false,
    val zatLadend: Boolean = false,
    val kaderSpeichernd: Boolean = false,
    val message: String? = null,
    val fehler: String? = null,
)

@HiltViewModel
class ZugabgabeViewModel @Inject constructor(
    private val repository: ZugabgabeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ZugabgabeUiState())
    val uiState: StateFlow<ZugabgabeUiState> = _uiState.asStateFlow()

    init {
        ladeAufstellung()
    }

    /** Setzt den im Header angezeigten Zugabgabe-Status direkt nach einem Checkza-Lauf. */
    fun aktualisiereStatus(status: String) {
        val aufstellung = _uiState.value.aufstellung ?: return
        _uiState.value = _uiState.value.copy(aufstellung = aufstellung.copy(status = status))
    }

    fun ladeAufstellung() {
        viewModelScope.launch {
            _uiState.value = ZugabgabeUiState(ladende = true)
            _uiState.value = try {
                val aufstellung = repository.ladeAufstellung()
                ZugabgabeUiState(
                    aufstellung = aufstellung,
                    message = if (aufstellung.spieler.isEmpty()) "Keine spielberechtigten Spieler gefunden." else null,
                )
            } catch (e: Exception) {
                ZugabgabeUiState(fehler = e.message ?: "Aktuelle ZAT-Daten konnten nicht geladen werden.")
            }
        }
    }

    /** Setzt einen Website-Spieler auf eine konkrete Beta-Rasterzelle. */
    fun setzeAufFeld(pid: Long, zeile: Int, spalte: Int) {
        val aufstellung = _uiState.value.aufstellung ?: return
        if (aufstellung.rasterPositionen.isNotEmpty() &&
            aufstellung.rasterPositionen.values.none { it.zeile == zeile && it.spalte == spalte }
        ) return
        val ziel = AufstellungSlot.Feld(zeile, spalte)
        val raSlot = aufstellung.rasterPositionen.entries
            .firstOrNull { it.value.zeile == zeile && it.value.spalte == spalte }
            ?.key
        val besetzt = aufstellung.spieler.firstOrNull { it.slot == ziel }
            ?: aufstellung.spieler.firstOrNull { raSlot != null && it.raSlot == raSlot }
        if (besetzt != null && besetzt.pid != pid) {
            // Ziel ist belegt: Spieler tauschen statt verdrängen (kein „verschwindender“ Spieler).
            val neueSpieler = aufstellung.spieler.tauschePlaetze(pid, besetzt.pid)
            _uiState.value = _uiState.value.copy(
                aufstellung = aufstellung.copy(spieler = neueSpieler),
                message = null,
            )
            return
        }
        val neueSpieler = aufstellung.spieler.map { spieler ->
            when {
                spieler.pid == pid -> spieler.copy(slot = ziel, raSlot = raSlot)
                spieler.slot == ziel || (raSlot != null && spieler.raSlot == raSlot) -> spieler.copy(slot = null, raSlot = null)
                else -> spieler
            }
        }
        _uiState.value = _uiState.value.copy(
            aufstellung = aufstellung.copy(spieler = neueSpieler),
            message = null,
        )
    }

    fun setzeAufTorwart(pid: Long) {
        val aufstellung = _uiState.value.aufstellung ?: return
        val vorhandenerTor = aufstellung.torwart
        if (vorhandenerTor != null && vorhandenerTor.pid != pid) {
            val neueSpieler = aufstellung.spieler.tauschePlaetze(pid, vorhandenerTor.pid)
            _uiState.value = _uiState.value.copy(
                aufstellung = aufstellung.copy(spieler = neueSpieler),
                message = null,
            )
            return
        }
        val neueSpieler = aufstellung.spieler.map { spieler ->
            when {
                spieler.pid == pid -> spieler.copy(slot = AufstellungSlot.Torwart, raSlot = "T")
                spieler.slot == AufstellungSlot.Torwart -> spieler.copy(slot = null, raSlot = null)
                else -> spieler
            }
        }
        _uiState.value = _uiState.value.copy(
            aufstellung = aufstellung.copy(spieler = neueSpieler),
            message = null,
        )
    }

    /** Platziert den markierten Spieler auf einem Ersatzbank-Platz (U=0 … Z=5). */
    fun setzeAufBank(pid: Long, index: Int) {
        if (index !in ERSATZBANK_BUCHSTABEN.indices) return
        val aufstellung = _uiState.value.aufstellung ?: return
        val buchstabe = ERSATZBANK_BUCHSTABEN[index].toString()
        val ziel = AufstellungSlot.Ersatz(index)
        val besetzt = aufstellung.spieler.firstOrNull { it.slot == ziel }
            ?: aufstellung.spieler.firstOrNull { it.raSlot == buchstabe }
        if (besetzt != null && besetzt.pid != pid) {
            // Ziel ist belegt: tauschen statt verdrängen.
            val neueSpieler = aufstellung.spieler.tauschePlaetze(pid, besetzt.pid)
            _uiState.value = _uiState.value.copy(
                aufstellung = aufstellung.copy(spieler = neueSpieler),
            )
            return
        }
        val neueSpieler = aufstellung.spieler.map { spieler ->
            when {
                spieler.pid == pid -> spieler.copy(slot = ziel, raSlot = buchstabe)
                spieler.slot == ziel || spieler.raSlot == buchstabe -> spieler.copy(slot = null, raSlot = null)
                else -> spieler
            }
        }
        _uiState.value = _uiState.value.copy(
            aufstellung = aufstellung.copy(spieler = neueSpieler),
        )
    }

    /** Entfernt einen Spieler lokal aus einer Position; gespeichert wird erst beim Submit. */
    fun entferneVonPosition(pid: Long) {
        val aufstellung = _uiState.value.aufstellung ?: return
        val neueSpieler = aufstellung.spieler.map { spieler ->
            if (spieler.pid == pid) spieler.copy(slot = null, raSlot = null) else spieler
        }
        _uiState.value = _uiState.value.copy(
            aufstellung = aufstellung.copy(spieler = neueSpieler),
            message = null,
        )
    }

    /** Bestätigte Speicherung der (bearbeiteten) Aufstellung als Zugabgabe. */
    fun speichere() {
        val aufstellung = _uiState.value.aufstellung ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(speichernd = true, message = null)
            _uiState.value = try {
                val meldung = repository.speichereBeta(aufstellung)
                _uiState.value.copy(speichernd = false, message = meldung)
            } catch (e: Exception) {
                _uiState.value.copy(
                    speichernd = false,
                    message = e.message ?: "Speichern fehlgeschlagen.",
                )
            }
        }
    }

    /** Wendet die gewählte Formation (Taktikauswahl) an und lädt neu. Nur aus bestätigter Nutzerabsicht. */
    fun wendeTaktikAn(taktikId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(taktikLadend = true, message = null)
            _uiState.value = try {
                val aufstellung = repository.wendeTaktikAn(taktikId)
                _uiState.value.copy(
                    taktikLadend = false,
                    aufstellung = aufstellung,
                    message = null,
                )
            } catch (e: Exception) {
                _uiState.value.copy(
                    taktikLadend = false,
                    message = e.message ?: "Taktik konnte nicht geladen werden.",
                )
            }
        }
    }

    /** „Laden aus ZAT": übernimmt alle Einstellungen des gewählten ZAT. Nur aus bestätigter Nutzerabsicht. */
    fun wendeZatAn(zatId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(zatLadend = true, message = null)
            _uiState.value = try {
                val aufstellung = repository.wendeZatAn(zatId)
                _uiState.value.copy(
                    zatLadend = false,
                    aufstellung = aufstellung,
                    message = "Einstellungen vom ZAT übernommen.",
                )
            } catch (e: Exception) {
                _uiState.value.copy(
                    zatLadend = false,
                    message = e.message ?: "ZAT konnte nicht geladen werden.",
                )
            }
        }
    }

    /** Setzt den Raster-Slot (ra[<pid>]) eines Kaderspielers lokal. */
    fun setzeKaderSlot(pid: Long, slotId: String) {
        val aufstellung = _uiState.value.aufstellung ?: return
        if (slotId.isBlank()) {
            entferneVonPosition(pid)
            return
        }
        val neuerSlot = when {
            slotId == "T" -> AufstellungSlot.Torwart
            slotId in ERSATZBANK_BUCHSTABEN -> AufstellungSlot.Ersatz(ERSATZBANK_BUCHSTABEN.indexOf(slotId))
            else -> aufstellung.rasterPositionen[slotId]?.let { AufstellungSlot.Feld(it.zeile, it.spalte) }
        }
        val besetzt = aufstellung.spieler.firstOrNull { sp ->
            (neuerSlot != null && sp.slot == neuerSlot) || sp.raSlot == slotId
        }
        if (besetzt != null && besetzt.pid != pid) {
            // Belegt: tauschen. Der neue Spieler erhaelt zwingend den gewaehlten Buchstaben,
            // der bisherige Besitzer rutscht (mit seinen eigenen Werten) an die alte Position.
            val quelle = aufstellung.spieler.firstOrNull { it.pid == pid } ?: return
            val neueSpieler = aufstellung.spieler.map { sp ->
                when (sp.pid) {
                    pid -> sp.copy(slot = neuerSlot, raSlot = slotId)
                    besetzt.pid -> sp.copy(slot = quelle.slot, raSlot = quelle.raSlot)
                    else -> sp
                }
            }
            _uiState.value = _uiState.value.copy(
                aufstellung = aufstellung.copy(spieler = neueSpieler),
            )
            return
        }
        val neueSpieler = aufstellung.spieler.map { spieler ->
            if (spieler.pid == pid) spieler.copy(raSlot = slotId, slot = neuerSlot) else spieler
        }
        _uiState.value = _uiState.value.copy(
            aufstellung = aufstellung.copy(spieler = neueSpieler),
        )
    }

    /** Bestätigte Speicherung der Kader-Zuordnung (klassischer ra[]-Weg). */
    fun speichereKader() {
        val aufstellung = _uiState.value.aufstellung ?: return
        val slots = aufstellung.spieler
            .filter { it.raSlot != null }
            .associate { it.pid to (it.raSlot ?: "") }
        if (slots.isEmpty()) {
            _uiState.value = _uiState.value.copy(message = "Noch keine Zuordnung vorhanden.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(kaderSpeichernd = true, message = null)
            _uiState.value = try {
                val neueAufstellung = repository.speichereKaderAufstellung(slots)
                _uiState.value.copy(
                    kaderSpeichernd = false,
                    aufstellung = neueAufstellung,
                    message = "✓ Aufstellung gespeichert",
                )
            } catch (e: Exception) {
                _uiState.value.copy(
                    kaderSpeichernd = false,
                    message = e.message ?: "Aufstellung konnte nicht gespeichert werden.",
                )
            }
        }
    }

    /** Löscht die klassische Kader-Aufstellung nach bestätigter Nutzeraktion. */
    fun loescheKader() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(kaderSpeichernd = true, message = null)
            _uiState.value = try {
                val neueAufstellung = repository.loescheKaderAufstellung()
                _uiState.value.copy(
                    kaderSpeichernd = false,
                    aufstellung = neueAufstellung,
                    message = "✓ Aufstellung gelöscht",
                )
            } catch (e: Exception) {
                _uiState.value.copy(
                    kaderSpeichernd = false,
                    message = e.message ?: "Aufstellung konnte nicht gelöscht werden.",
                )
            }
        }
    }
}
