package com.onlinesoccer.app.feature.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.EigeneGeboteErgebnis
import com.onlinesoccer.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EigeneGeboteUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val ergebnis: EigeneGeboteErgebnis = EigeneGeboteErgebnis(),
)

/** Native Darstellung der „Eigenen Gebote" (`viewtm.php`). */
@HiltViewModel
class EigeneGeboteViewModel @Inject constructor(
    private val repository: ServerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EigeneGeboteUiState())
    val uiState: StateFlow<EigeneGeboteUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
        viewModelScope.launch {
            _uiState.value = try {
                val ergebnis = repository.eigeneGebote()
                _uiState.value.copy(ladend = false, ergebnis = ergebnis)
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Eigene Gebote konnten nicht geladen werden.",
                )
            }
        }
    }
}