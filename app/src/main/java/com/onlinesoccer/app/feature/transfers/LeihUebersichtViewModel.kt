package com.onlinesoccer.app.feature.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.LeihUebersichtErgebnis
import com.onlinesoccer.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LeihUebersichtUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val ergebnis: LeihUebersichtErgebnis = LeihUebersichtErgebnis(),
)

/** Native Darstellung der „Leihspieler Übersicht" (`viewleih.php`). */
@HiltViewModel
class LeihUebersichtViewModel @Inject constructor(
    private val repository: ServerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeihUebersichtUiState())
    val uiState: StateFlow<LeihUebersichtUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
        viewModelScope.launch {
            _uiState.value = try {
                val ergebnis = repository.leihUebersicht()
                _uiState.value.copy(ladend = false, ergebnis = ergebnis)
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Leihspieler Übersicht konnte nicht geladen werden.",
                )
            }
        }
    }
}