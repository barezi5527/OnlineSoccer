package com.onlinesoccer.app.feature.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.FremdesTeam
import com.onlinesoccer.app.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VereinUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val team: FremdesTeam? = null,
)

@HiltViewModel
class VereinViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TeamRepository,
) : ViewModel() {

    private val teamId: Long = (savedStateHandle["teamId"] as? Long)
        ?: (savedStateHandle["teamId"] as? String)?.toLongOrNull()
        ?: 0L

    private val _uiState = MutableStateFlow(VereinUiState())
    val uiState: StateFlow<VereinUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = VereinUiState(ladend = true)
            _uiState.value = try {
                val team = repository.ladeFremdenKader(teamId)
                if (team == null || team.kader.isEmpty()) {
                    VereinUiState(
                        ladend = false,
                        fehler = "Verein konnte nicht geladen werden.",
                    )
                } else {
                    VereinUiState(ladend = false, team = team)
                }
            } catch (e: Exception) {
                VereinUiState(
                    ladend = false,
                    fehler = e.message ?: "Verein konnte nicht geladen werden.",
                )
            }
        }
    }
}