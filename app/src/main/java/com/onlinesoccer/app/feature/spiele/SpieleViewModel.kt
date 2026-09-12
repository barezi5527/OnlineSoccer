package com.onlinesoccer.app.feature.spiele

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.DashboardData
import com.onlinesoccer.app.data.model.LivegameData
import com.onlinesoccer.app.data.repository.DashboardRepository
import com.onlinesoccer.app.data.repository.LivegameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpieleUiState(
    val ladende: Boolean = false,
    val dashboard: DashboardData? = null,
    val livegame: LivegameData? = null,
    val liveLadend: Boolean = false,
    val fehler: String? = null,
)

@HiltViewModel
class SpieleViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val livegameRepository: LivegameRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpieleUiState())
    val uiState: StateFlow<SpieleUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = SpieleUiState(ladende = true)
            _uiState.value = try {
                val dashboard = dashboardRepository.fetchDashboard()
                val state = SpieleUiState(ladende = false, dashboard = dashboard)
                state.copy(livegame = ladeLivegame(state))
            } catch (e: Exception) {
                SpieleUiState(
                    ladende = false,
                    fehler = e.message ?: "Spiele konnten nicht geladen werden.",
                )
            }
        }
    }

    private suspend fun ladeLivegame(state: SpieleUiState): LivegameData? {
        val dashboard = state.dashboard ?: return null
        val next = dashboard.naechstesSpiel ?: return null
        val zat = dashboard.zat?.toIntOrNull() ?: next.gepaartZat ?: return null
        val heimId = (if (next.heim) dashboard.teamId else next.gegnerId) ?: return null
        return runCatching { livegameRepository.ladeSpiel(heimId, zat) }.getOrNull()
    }
}