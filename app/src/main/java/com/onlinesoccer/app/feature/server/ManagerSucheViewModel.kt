package com.onlinesoccer.app.feature.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.ManagerSucheDaten
import com.onlinesoccer.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManagerSucheUiState(
    val managerText: String = "",
    val teamText: String = "",
    val operator: String = "and",
    val ladend: Boolean = false,
    val fehler: String? = null,
    val daten: ManagerSucheDaten = ManagerSucheDaten(),
    val gesucht: Boolean = false,
)

@HiltViewModel
class ManagerSucheViewModel @Inject constructor(
    private val repository: ServerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManagerSucheUiState())
    val uiState: StateFlow<ManagerSucheUiState> = _uiState.asStateFlow()

    fun managerTextAendern(text: String) {
        _uiState.value = _uiState.value.copy(managerText = text)
    }

    fun teamTextAendern(text: String) {
        _uiState.value = _uiState.value.copy(teamText = text)
    }

    fun operatorWaehlen(operator: String) {
        _uiState.value = _uiState.value.copy(operator = operator)
    }

    fun suchen() {
        val state = _uiState.value
        val manager = state.managerText.trim()
        val team = state.teamText.trim()
        if (manager.isEmpty() && team.isEmpty()) return
        _uiState.value = state.copy(ladend = true, fehler = null)
        viewModelScope.launch {
            _uiState.value = try {
                val daten = repository.managersuche(manager, state.operator, team)
                state.copy(
                    ladend = false,
                    daten = daten,
                    gesucht = true,
                    managerText = manager,
                    teamText = team,
                )
            } catch (e: Exception) {
                state.copy(ladend = false, fehler = e.message ?: "Suche konnte nicht ausgeführt werden.")
            }
        }
    }
}