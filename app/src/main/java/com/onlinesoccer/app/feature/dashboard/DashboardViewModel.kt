package com.onlinesoccer.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.core.storage.JugendHinweisStore
import com.onlinesoccer.app.data.model.DashboardData
import com.onlinesoccer.app.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Ready(
        val data: DashboardData,
        val jugendHinweisAusgeblendet: Boolean = false,
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val jugendHinweisStore: JugendHinweisStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        laden()
    }

    /** Erzwungener Abruf (Aktualisieren-Schalter, Tab-Tipp): umgeht den Cache. */
    fun refresh() {
        laden(forceRefresh = true)
    }

    fun jugendHinweisSchliessen() {
        val bereit = _uiState.value as? DashboardUiState.Ready ?: return
        val hinweis = bereit.data.jugendHinweis ?: return
        jugendHinweisStore.ausblenden(bereit.data.teamId, hinweis)
        _uiState.value = bereit.copy(jugendHinweisAusgeblendet = true)
    }

    private fun laden(forceRefresh: Boolean = false) {
        _uiState.value = DashboardUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val data = repository.fetchDashboard(forceRefresh = forceRefresh)
                DashboardUiState.Ready(
                    data = data,
                    jugendHinweisAusgeblendet = jugendHinweisStore.istAusgeblendet(
                        data.teamId,
                        data.jugendHinweis,
                    ),
                )
            } catch (e: Exception) {
                DashboardUiState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }
}
