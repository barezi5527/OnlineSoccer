package com.onlinesoccer.app.feature.zat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.CheckzaErgebnis
import com.onlinesoccer.app.data.model.ZatErgebnisse
import com.onlinesoccer.app.data.repository.ZatRepository
import com.onlinesoccer.app.data.repository.ZugabgabeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ZatUiState(
    val ladend: Boolean = false,
    val fehler: String? = null,
    val ergebnisse: ZatErgebnisse? = null,
    val speichernd: Boolean = false,
    val speicherErfolgreich: Boolean = false,
    val meldung: String? = null,
    val checkzaErgebnis: CheckzaErgebnis? = null,
)

@HiltViewModel
class ZatViewModel @Inject constructor(
    private val repository: ZatRepository,
    private val zugabgabeRepository: ZugabgabeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ZatUiState())
    val uiState: StateFlow<ZatUiState> = _uiState.asStateFlow()

    fun ladeErgebnisse() {
        if (_uiState.value.ergebnisse != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                _uiState.value.copy(ladend = false, ergebnisse = repository.ladeZatErgebnisse())
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "ZAT-Ergebnisse konnten nicht geladen werden.")
            }
        }
    }

    /** Endgueltiges "Zugabgabe speichern" (checkza.php). Reale Schreibaktion. */
    fun zugabgabeSpeichern() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                speichernd = true,
                speicherErfolgreich = false,
                fehler = null,
                meldung = null,
                checkzaErgebnis = null,
            )
            try {
                val ergebnis = zugabgabeRepository.checkzaErgebnis()
                _uiState.value = _uiState.value.copy(
                    speichernd = false,
                    speicherErfolgreich = ergebnis.gueltig,
                    meldung = ergebnis.gesamtStatus,
                    checkzaErgebnis = ergebnis,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    speichernd = false,
                    speicherErfolgreich = false,
                    fehler = e.message ?: "Zugabgabe konnte nicht gespeichert werden.",
                )
            }
        }
    }

    fun schliesseCheckzaErgebnis() {
        _uiState.value = _uiState.value.copy(checkzaErgebnis = null)
    }
}
