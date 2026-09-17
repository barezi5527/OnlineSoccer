package com.onlinesoccer.app.feature.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.VmSetzenEintrag
import com.onlinesoccer.app.data.model.VmSetzenErgebnis
import com.onlinesoccer.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VmSetzenUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val ergebnis: VmSetzenErgebnis = VmSetzenErgebnis(),
)

/** Zustand des „Auf den VM setzen"-Dialogs (Absenden erst nach Bestätigung). */
sealed interface VmSetzenDialogState {
    data object Verborgen : VmSetzenDialogState
    data class Bereit(val eintrag: VmSetzenEintrag) : VmSetzenDialogState
    data object Sende : VmSetzenDialogState
    data class Ergebnis(val erfolg: Boolean, val meldung: String) : VmSetzenDialogState
}

/** „Auf den VM setzen" (`vmsetzen.php`): eigene Spieler lesen und mit Bestätigung auf den VM setzen. */
@HiltViewModel
class VmSetzenViewModel @Inject constructor(
    private val repository: ServerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VmSetzenUiState())
    val uiState: StateFlow<VmSetzenUiState> = _uiState.asStateFlow()

    private val _dialog = MutableStateFlow<VmSetzenDialogState>(VmSetzenDialogState.Verborgen)
    val dialog: StateFlow<VmSetzenDialogState> = _dialog.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
        viewModelScope.launch {
            _uiState.value = try {
                val ergebnis = repository.vmsetzen()
                _uiState.value.copy(ladend = false, ergebnis = ergebnis)
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Auf den VM setzen konnte nicht geladen werden.")
            }
        }
    }

    /** Öffnet den „Auf den VM setzen"-Dialog für einen Spieler (Startpreis wählen + bestätigen). */
    fun aufVmSetzenKlick(eintrag: VmSetzenEintrag) {
        if (_dialog.value != VmSetzenDialogState.Verborgen) return
        _dialog.value = VmSetzenDialogState.Bereit(eintrag)
    }

    /** Setzt den Spieler erst nach Bestätigung des Dialogs auf den VM (`POST vmsetzen.php`). */
    fun aufVmSetzen(startpreis: String) {
        val aktuell = _dialog.value as? VmSetzenDialogState.Bereit ?: return
        val pid = aktuell.eintrag.spielerId
        _dialog.value = VmSetzenDialogState.Sende
        viewModelScope.launch {
            try {
                val ergebnis = repository.vmSetzen(pid, startpreis)
                _dialog.value = VmSetzenDialogState.Ergebnis(erfolg = ergebnis.erfolg, meldung = ergebnis.meldung)
                if (ergebnis.erfolg) lade()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _dialog.value = VmSetzenDialogState.Ergebnis(
                    erfolg = false,
                    meldung = e.message ?: "Spieler konnte nicht auf den Versteigerungsmarkt gesetzt werden.",
                )
            }
        }
    }

    fun dialogSchliessen() {
        _dialog.value = VmSetzenDialogState.Verborgen
    }
}