package com.onlinesoccer.app.feature.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.FreieTeamsDaten
import com.onlinesoccer.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FreieTeamsUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val daten: FreieTeamsDaten = FreieTeamsDaten(),
    val gewaehltesLand: String = "0",
)

@HiltViewModel
class FreieTeamsViewModel @Inject constructor(
    private val repository: ServerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreieTeamsUiState())
    val uiState: StateFlow<FreieTeamsUiState> = _uiState.asStateFlow()

    init {
        lade("0")
    }

    fun lade(landId: String) {
        _uiState.value = _uiState.value.copy(ladend = true, fehler = null, gewaehltesLand = landId)
        viewModelScope.launch {
            _uiState.value = try {
                val daten = repository.freieTeams(landId)
                FreieTeamsUiState(ladend = false, daten = daten, gewaehltesLand = landId)
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Freie Teams konnten nicht geladen werden.",
                )
            }
        }
    }
}