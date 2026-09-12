package com.onlinesoccer.app.feature.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.ManagerlisteDaten
import com.onlinesoccer.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManagerlisteUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val daten: ManagerlisteDaten = ManagerlisteDaten(),
    val landId: String = "0",
    val ligaId: String = "0",
)

@HiltViewModel
class ManagerlisteViewModel @Inject constructor(
    private val repository: ServerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManagerlisteUiState())
    val uiState: StateFlow<ManagerlisteUiState> = _uiState.asStateFlow()

    init {
        lade("0", "0")
    }

    fun landWaehlen(landId: String) {
        lade(landId, _uiState.value.ligaId)
    }

    fun ligaWaehlen(ligaId: String) {
        lade(_uiState.value.landId, ligaId)
    }

    private fun lade(landId: String, ligaId: String) {
        _uiState.value = _uiState.value.copy(ladend = true, fehler = null, landId = landId, ligaId = ligaId)
        viewModelScope.launch {
            _uiState.value = try {
                val daten = repository.managerliste(landId, ligaId)
                ManagerlisteUiState(ladend = false, daten = daten, landId = landId, ligaId = ligaId)
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Managerliste konnte nicht geladen werden.",
                )
            }
        }
    }
}