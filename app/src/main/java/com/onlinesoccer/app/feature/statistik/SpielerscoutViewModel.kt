package com.onlinesoccer.app.feature.statistik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.SpielerscoutDaten
import com.onlinesoccer.app.data.repository.StatistikRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpielerscoutUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val daten: SpielerscoutDaten = SpielerscoutDaten(),
)

/** Spielerscout: die interessantesten Spieler in fünf Kategorien. */
@HiltViewModel
class SpielerscoutViewModel @Inject constructor(
    private val repository: StatistikRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpielerscoutUiState())
    val uiState: StateFlow<SpielerscoutUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                _uiState.value.copy(ladend = false, daten = repository.spielerscout())
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Spielerscout konnte nicht geladen werden.",
                )
            }
        }
    }
}