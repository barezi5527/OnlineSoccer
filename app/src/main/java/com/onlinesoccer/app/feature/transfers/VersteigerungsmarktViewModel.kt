package com.onlinesoccer.app.feature.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.VersteigerungsmarktErgebnis
import com.onlinesoccer.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

/** Native Darstellung des „Versteigerungsmarkts" (`viewvm.php`): Filter + Anzeigen (kein Gebot). */
@HiltViewModel
class VersteigerungsmarktViewModel @Inject constructor(
    private val repository: ServerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VersteigerungsmarktUiState())
    val uiState: StateFlow<VersteigerungsmarktUiState> = _uiState.asStateFlow()

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
}