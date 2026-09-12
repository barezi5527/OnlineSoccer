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
)

@HiltViewModel
class InternationaleViewModel @Inject constructor(
    private val repository: InternationaleRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InternationaleUiState())
    val uiState: StateFlow<InternationaleUiState> = _uiState.asStateFlow()
    private var ladeJob: Job? = null

    fun lade(path: String, filter: InternationaleFilter = InternationaleFilter()) {
        ladeJob?.cancel()
        ladeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null, ansicht = null)
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
}
