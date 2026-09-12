package com.onlinesoccer.app.feature.team.friendly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.Freundschaftsdaten
import com.onlinesoccer.app.data.repository.FreundschaftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class FreundschaftUiState(
    val ladend: Boolean = false,
    val daten: Freundschaftsdaten? = null,
    val aktion: Boolean = false,
    val fehler: String? = null,
    val meldung: String? = null,
)

@HiltViewModel
class FreundschaftViewModel @Inject constructor(
    private val repository: FreundschaftRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FreundschaftUiState())
    val uiState: StateFlow<FreundschaftUiState> = _uiState.asStateFlow()
    private val requestMutex = Mutex()

    init { lade() }

    fun lade() = viewModelScope.launch {
        requestMutex.withLock {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                FreundschaftUiState(daten = repository.lade())
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Freundschaftsspiele konnten nicht geladen werden.")
            }
        }
    }

    fun blindeEinladung(zat: String, doppelt: Boolean) = ausfuehren { repository.blindeEinladung(zat, doppelt) }
    fun reserviere(zats: List<String>) = ausfuehren { repository.reserviere(zats) }
    fun loescheReservierungen() = ausfuehren { repository.loescheReservierungen() }
    fun storniere(id: String) = ausfuehren { repository.storniere(id) }
    fun zeigeTeams(land: String, liga: String) = ausfuehren { repository.zeigeTeams(land, liga) }

    private fun ausfuehren(action: suspend () -> Freundschaftsdaten) = viewModelScope.launch {
        requestMutex.withLock {
            _uiState.value = _uiState.value.copy(aktion = true, fehler = null, meldung = null)
            _uiState.value = try {
                FreundschaftUiState(daten = action(), meldung = "✓ Aktualisiert")
            } catch (e: Exception) {
                _uiState.value.copy(aktion = false, fehler = e.message ?: "Aktion fehlgeschlagen.")
            }
        }
    }
}
