package com.onlinesoccer.app.feature.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.TransferListeErgebnis
import com.onlinesoccer.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransferListeUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val ergebnis: TransferListeErgebnis = TransferListeErgebnis(),
    val wahl: Map<String, String> = emptyMap(),
    val seite: Int = 1,
)

/** Native Transferliste (`osneu/transferliste`): Filter wählen, „Anzeigen", blättern. */
@HiltViewModel
class TransferListeViewModel @Inject constructor(
    private val repository: ServerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferListeUiState())
    val uiState: StateFlow<TransferListeUiState> = _uiState.asStateFlow()

    init {
        ladeFormular()
    }

    /** Lädt die Transferliste mit den aktuell gewählten Filtern (leer = Seite 1 aller Spieler). */
    fun ladeFormular() {
        ladend(1)
        viewModelScope.launch {
            _uiState.value = try {
                val form = repository.transferliste(emptyMap(), 1)
                _uiState.value.copy(ladend = false, ergebnis = form)
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Transferliste konnte nicht geladen werden.")
            }
        }
    }

    fun filterWaehlen(name: String, wert: String) {
        _uiState.value = _uiState.value.copy(wahl = _uiState.value.wahl + (name to wert))
    }

    fun anzeigen() = suchen(1)

    fun seiteWaehlen(seite: Int) = suchen(seite)

    private fun ladend(seite: Int) {
        _uiState.value = _uiState.value.copy(ladend = true, fehler = null, seite = seite)
    }

    private fun suchen(seite: Int) {
        val filter = _uiState.value.wahl
        ladend(seite)
        viewModelScope.launch {
            _uiState.value = try {
                val ergebnis = repository.transferliste(filter, seite)
                _uiState.value.copy(ladend = false, ergebnis = ergebnis)
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Transferliste konnte nicht geladen werden.")
            }
        }
    }
}