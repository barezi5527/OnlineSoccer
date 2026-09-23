package com.onlinesoccer.app.feature.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.TeamInfoMenuEintrag
import com.onlinesoccer.app.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeaminformationenUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val menuEintraege: List<TeamInfoMenuEintrag> = emptyList(),
    /** Verein, dessen Teaminformationen geladen werden; `null` = eigener Verein. */
    val teamId: Long? = null,
)

/** Lädt die Unterpunkte des Bereichs „Teaminformationen" dynamisch von der Website. */
@HiltViewModel
class TeaminformationenViewModel @Inject constructor(
    private val repository: TeamRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeaminformationenUiState())
    val uiState: StateFlow<TeaminformationenUiState> = _uiState.asStateFlow()

    fun lade(teamId: Long? = null) {
        if (!_uiState.value.menuEintraege.isEmpty() && _uiState.value.teamId == teamId) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null, teamId = teamId)
            _uiState.value = try {
                _uiState.value.copy(
                    ladend = false,
                    menuEintraege = repository.ladeTeaminformationenMenu(teamId),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Teaminformationen konnten nicht geladen werden.",
                )
            }
        }
    }
}