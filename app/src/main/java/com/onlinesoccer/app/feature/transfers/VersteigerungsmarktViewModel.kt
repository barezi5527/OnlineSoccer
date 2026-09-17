package com.onlinesoccer.app.feature.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.VersteigerungsmarktEintrag
import com.onlinesoccer.app.data.model.VersteigerungsmarktErgebnis
import com.onlinesoccer.app.data.model.VmGebotInformation
import com.onlinesoccer.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VersteigerungsmarktUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val ergebnis: VersteigerungsmarktErgebnis = VersteigerungsmarktErgebnis(),
    val wahl: Map<String, String> = emptyMap(),
)

/** Zustand des „Gebot abgeben"-Dialogs im Versteigerungsmarkt (Gebote werden erst nach Bestätigung gesendet). */
sealed interface VmBietDialogState {
    data object Verborgen : VmBietDialogState
    data object Laedt : VmBietDialogState
    data class Bereit(val info: VmGebotInformation?) : VmBietDialogState
    data class Ergebnis(val erfolg: Boolean, val meldung: String) : VmBietDialogState
}

/** Native Darstellung des „Versteigerungsmarkts" (`viewvm.php`): Filter + Anzeigen + Gebote. */
@HiltViewModel
class VersteigerungsmarktViewModel @Inject constructor(
    private val repository: ServerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VersteigerungsmarktUiState())
    val uiState: StateFlow<VersteigerungsmarktUiState> = _uiState.asStateFlow()

    private val _bietDialog = MutableStateFlow<VmBietDialogState>(VmBietDialogState.Verborgen)
    val bietDialog: StateFlow<VmBietDialogState> = _bietDialog.asStateFlow()

    init {
        ladeFormular()
    }

    /** Lädt nur die Filteroptionen des Markts (GET, Formular-Seite). */
    fun ladeFormular() {
        _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
        viewModelScope.launch {
            _uiState.value = try {
                val ergebnis = repository.versteigerungsmarktFormular()
                _uiState.value.copy(ladend = false, ergebnis = ergebnis)
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Versteigerungsmarkt konnte nicht geladen werden.")
            }
        }
    }

    fun filterWaehlen(name: String, wert: String) {
        _uiState.value = _uiState.value.copy(wahl = _uiState.value.wahl + (name to wert))
    }

    /** Zeigt die Spieler an (`sshow` = reine Listenansicht, sendet kein Gebot). */
    fun anzeigen() {
        val filter = _uiState.value.wahl
        _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
        viewModelScope.launch {
            _uiState.value = try {
                val ergebnis = repository.versteigerungsmarkt(filter)
                _uiState.value.copy(ladend = false, ergebnis = ergebnis)
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Versteigerungsmarkt konnte nicht geladen werden.")
            }
        }
    }

    /** Öffnet den „Gebot abgeben"-Dialog: lädt die Gebots-Vorschau (`vmgebot.php`, nur lesend). */
    fun onBietenKlick(eintrag: VersteigerungsmarktEintrag) {
        if (_bietDialog.value != VmBietDialogState.Verborgen) return
        _bietDialog.value = VmBietDialogState.Laedt
        viewModelScope.launch {
            try {
                val info = repository.vmGebotInfo(eintrag.spielerId)
                _bietDialog.value = if (info?.submitName.isNullOrBlank() && info?.betragName.isNullOrBlank()) {
                    VmBietDialogState.Ergebnis(
                        erfolg = false,
                        meldung = "Der Versteigerungsmarkt erlaubt für diesen Spieler kein Gebot.",
                    )
                } else {
                    VmBietDialogState.Bereit(info)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bietDialog.value = VmBietDialogState.Ergebnis(
                    erfolg = false,
                    meldung = e.message ?: "Gebots-Informationen konnten nicht geladen werden.",
                )
            }
        }
    }

    /** Sendet das Gebot erst, nachdem der Nutzer im Dialog bestätigt hat. */
    fun vmGebotAbgeben(betrag: String?) {
        val aktuell = _bietDialog.value as? VmBietDialogState.Bereit ?: return
        val pid = aktuell.info?.spielerId ?: return
        _bietDialog.value = VmBietDialogState.Laedt
        viewModelScope.launch {
            try {
                val ergebnis = repository.vmGebotAbgeben(pid, betrag)
                _bietDialog.value = VmBietDialogState.Ergebnis(erfolg = ergebnis.erfolg, meldung = ergebnis.meldung)
                anzeigen()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bietDialog.value = VmBietDialogState.Ergebnis(
                    erfolg = false,
                    meldung = e.message ?: "Gebot konnte nicht gesendet werden.",
                )
            }
        }
    }

    fun bietDialogSchliessen() {
        _bietDialog.value = VmBietDialogState.Verborgen
    }
}