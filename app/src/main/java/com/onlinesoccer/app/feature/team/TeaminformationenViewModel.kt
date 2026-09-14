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
)

/** Lädt die Unterpunkte des Bereichs „Teaminformationen" dynamisch von der Website. */
@HiltViewModel
class TeaminformationenViewModel @Inject constructor(
    private val repository: TeamRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeaminformationenUiState())
    val uiState: StateFlow<TeaminformationenUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                _uiState.value.copy(
                    ladend = false,
                    menuEintraege = repository.ladeTeaminformationenMenu(),
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