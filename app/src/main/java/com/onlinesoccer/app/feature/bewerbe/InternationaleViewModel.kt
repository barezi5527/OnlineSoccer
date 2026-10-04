package com.onlinesoccer.app.feature.bewerbe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.InternationaleAnsicht
import com.onlinesoccer.app.data.model.InternationaleFilter
import com.onlinesoccer.app.data.repository.InternationaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InternationaleUiState(
    val ladend: Boolean = false,
    val fehler: String? = null,
    val ansicht: InternationaleAnsicht? = null,
    /** Name der zuletzt geladenen Ansicht; macht Rückkehr aus dem Kader erkennbar. */
    val ansichtKey: String? = null,
) {
    /**
     * Beim Zurückkehren aus dem Mannschaftskader wird der Bildschirm neu
     * zusammengesetzt, das ViewModel lebt aber weiter. Solange dieselbe Ansicht
     * geladen ist, darf sie nicht neu geladen werden – sonst gingen gewählte
     * Tabelle, ZAT und Saison verloren.
     */
    fun istGeladen(key: String): Boolean = ansichtKey == key

    /**
     * Hebt die Zuordnung der geladenen Ansicht auf, damit das erneute Öffnen wieder
     * frisch lädt. Die bereits geladenen Daten bleiben erhalten.
     */
    fun zurUebersicht(): InternationaleUiState = copy(ansichtKey = null)
}

@HiltViewModel
class InternationaleViewModel @Inject constructor(
    private val repository: InternationaleRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InternationaleUiState())
    val uiState: StateFlow<InternationaleUiState> = _uiState.asStateFlow()
    private var ladeJob: Job? = null

    fun lade(key: String, path: String, filter: InternationaleFilter = InternationaleFilter()) {
        ladeJob?.cancel()
        ladeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null, ansicht = null, ansichtKey = key)
            _uiState.value = try {
                _uiState.value.copy(ladend = false, ansicht = repository.lade(path, filter))
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Internationale Bewerbe konnten nicht geladen werden.",
                )
            }
        }
    }

    /** siehe [InternationaleUiState.istGeladen]. */
    fun istAnsichtGeladen(key: String): Boolean = _uiState.value.istGeladen(key)

    /** siehe [InternationaleUiState.zurUebersicht]. */
    fun verwerfeAnsicht() {
        _uiState.value = _uiState.value.zurUebersicht()
    }
}
