package com.onlinesoccer.app.feature.statistik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.Spielstatistiken
import com.onlinesoccer.app.data.repository.StatistikRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpielstatistikenUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val daten: Spielstatistiken = Spielstatistiken(),
)

/** Spielstatistiken (`osneu/statistics`). */
@HiltViewModel
class SpielstatistikenViewModel @Inject constructor(
    private val repository: StatistikRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpielstatistikenUiState())
    val uiState: StateFlow<SpielstatistikenUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                _uiState.value.copy(ladend = false, daten = repository.spielstatistiken())
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Spielstatistiken konnten nicht geladen werden.",
                )
            }
        }
    }
}