package com.onlinesoccer.app.feature.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.core.state.TEAMWECHSEL_VERWORFEN
import com.onlinesoccer.app.core.state.TeamGeneration
import com.onlinesoccer.app.core.state.schreibvorgangErlaubt
import com.onlinesoccer.app.data.model.TransferStatus
import com.onlinesoccer.app.data.model.TransferStatusErgebnis
import com.onlinesoccer.app.data.model.TransferStatusZeile
import com.onlinesoccer.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransferStatusUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val ergebnis: TransferStatusErgebnis = TransferStatusErgebnis(),
    val speichernd: Boolean = false,
)

/** Dialog des „Status ändern"-Flows – gesendet wird erst nach ausdrücklicher Bestätigung. */
sealed interface StatusDialogState {
    data object Verborgen : StatusDialogState

    /** Nutzer hat im ⋮-Menü einen neuen Status gewählt; es fehlt noch die Bestätigung. */
    data class Bestaetigung(val zeile: TransferStatusZeile, val neuerStatus: TransferStatus) : StatusDialogState

    /** Antwort des Servers nach dem Speichern. */
    data class Ergebnis(val erfolg: Boolean, val meldung: String) : StatusDialogState
}

/** Native „Transferstatus"-Übersicht (`tstatus.php`): Status der eigenen Spieler. */
@HiltViewModel
class TransferStatusViewModel @Inject constructor(
    private val repository: ServerRepository,
    private val teamGeneration: TeamGeneration,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferStatusUiState())
    val uiState: StateFlow<TransferStatusUiState> = _uiState.asStateFlow()

    private val _dialog = MutableStateFlow<StatusDialogState>(StatusDialogState.Verborgen)
    val dialog: StateFlow<StatusDialogState> = _dialog.asStateFlow()

    /**
     * Generation, unter der die Statusliste geladen wurde.
     *
     * `tstatus.php` schreibt **die ganze Liste** der eigenen Spieler zurück — und der
     * POST enthält genau diese Liste. Nach einem Teamwechsel wäre das eine Liste von
     * Team-1-Spielern, die dem Server als Team-2-Liste ginge. Deshalb der Gate-Vorab
     * (Plan T35).
     */
    private var geladeneGeneration = teamGeneration.current()

    private fun gate(): String? {
        if (schreibvorgangErlaubt(geladeneGeneration, teamGeneration.current())) return null
        return TEAMWECHSEL_VERWORFEN
    }

    init {
        lade()
    }

    fun lade() {
        _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
        viewModelScope.launch {
            geladeneGeneration = teamGeneration.current()
            _uiState.value = try {
                val ergebnis = repository.transferStatus()
                _uiState.value.copy(ladend = false, ergebnis = ergebnis)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Transferstatus konnte nicht geladen werden.",
                )
            }
        }
    }

    /** Auswahl im ⋮-Menü: fordert für den Spieler eine Bestätigung des neuen Status an. */
    fun statusWaehlen(spielerId: Long, neuerStatus: TransferStatus) {
        if (_dialog.value != StatusDialogState.Verborgen || _uiState.value.speichernd) return
        val zeile = _uiState.value.ergebnis.zeilen.firstOrNull { it.spielerId == spielerId } ?: return
        if (zeile.status == neuerStatus) return
        _dialog.value = StatusDialogState.Bestaetigung(zeile, neuerStatus)
    }

    /** Bestätigung im Dialog: übernimmt den Status für genau diesen Spieler und sendet ihn. */
    fun bestaetigen() {
        val aktuell = _dialog.value
        if (aktuell !is StatusDialogState.Bestaetigung) return
        _dialog.value = StatusDialogState.Verborgen

        val zeilen = _uiState.value.ergebnis.zeilen.map { zeile ->
            if (zeile.spielerId == aktuell.zeile.spielerId) zeile.mitStatus(aktuell.neuerStatus) else zeile
        }
        _uiState.value = _uiState.value.copy(speichernd = true)
        viewModelScope.launch {
            gate()?.let { abgelehnt ->
                _uiState.value = _uiState.value.copy(speichernd = false)
                _dialog.value = StatusDialogState.Ergebnis(false, abgelehnt)
                return@launch
            }
            try {
                val antwort = repository.transferStatusSpeichern(zeilen)
                afterSpeichern(antwort.erfolg, antwort.meldung)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                afterSpeichern(false, e.message ?: "Transferstatus konnte nicht gespeichert werden.")
            }
        }
    }

    private fun afterSpeichern(erfolg: Boolean, meldung: String) {
        _uiState.value = _uiState.value.copy(speichernd = false)
        _dialog.value = StatusDialogState.Ergebnis(erfolg, meldung)
        if (erfolg) lade()
    }

    fun dialogSchliessen() {
        if (_uiState.value.speichernd) return
        _dialog.value = StatusDialogState.Verborgen
    }
}