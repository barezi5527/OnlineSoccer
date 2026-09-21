package com.onlinesoccer.app.feature.statistik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.FairplayZeile
import com.onlinesoccer.app.data.repository.StatistikRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FairplayUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val zeilen: List<FairplayZeile> = emptyList(),
)

/** Fairplaytabelle aller Erstligisten (`fpt.php`). */
@HiltViewModel
class FairplayViewModel @Inject constructor(
    private val repository: StatistikRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FairplayUiState())
    val uiState: StateFlow<FairplayUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                _uiState.value.copy(ladend = false, zeilen = repository.fairplay())
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Fairplaytabelle konnte nicht geladen werden.",
                )
            }
        }
    }
}