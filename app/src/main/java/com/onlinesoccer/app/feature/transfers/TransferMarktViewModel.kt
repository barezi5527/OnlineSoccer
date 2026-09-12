package com.onlinesoccer.app.feature.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.TransferMarktErgebnis
import com.onlinesoccer.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransferMarktUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val ergebnis: TransferMarktErgebnis = TransferMarktErgebnis(),
    val wahl: Map<String, String> = emptyMap(),
)

/** Native Darstellung des „Transfermarkts" (`tm.php`): Filter wählen + anzeigen. */
@HiltViewModel
class TransferMarktViewModel @Inject constructor(
    private val repository: ServerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferMarktUiState())
    val uiState: StateFlow<TransferMarktUiState> = _uiState.asStateFlow()

    init {
        ladeFormular()
    }

    /** Lädt nur die Filteroptionen des Märkts (GET, Formular-Seite). */
    fun ladeFormular() {
        _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
        viewModelScope.launch {
            _uiState.value = try {
                val ergebnis = repository.transfermarktFormular()
                _uiState.value.copy(ladend = false, ergebnis = ergebnis)
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Transfermarkt konnte nicht geladen werden.")
            }
        }
    }

    fun filterWaehlen(name: String, wert: String) {
        _uiState.value = _uiState.value.copy(wahl = _uiState.value.wahl + (name to wert))
    }

    fun anzeigen() {
        val filter = _uiState.value.wahl
        _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
        viewModelScope.launch {
            _uiState.value = try {
                val ergebnis = repository.transfermarkt(filter)
                _uiState.value.copy(ladend = false, ergebnis = ergebnis)
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Transfermarkt konnte nicht geladen werden.")
            }
        }
    }
}