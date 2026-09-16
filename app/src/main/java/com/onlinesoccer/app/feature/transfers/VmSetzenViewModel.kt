package com.onlinesoccer.app.feature.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.VmSetzenErgebnis
import com.onlinesoccer.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VmSetzenUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val ergebnis: VmSetzenErgebnis = VmSetzenErgebnis(),
)

/** „Auf den VM setzen" (`vmsetzen.php`) – rein lesend, es wird nie ein Formular abgesendet. */
@HiltViewModel
class VmSetzenViewModel @Inject constructor(
    private val repository: ServerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VmSetzenUiState())
    val uiState: StateFlow<VmSetzenUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
        viewModelScope.launch {
            _uiState.value = try {
                val ergebnis = repository.vmsetzen()
                _uiState.value.copy(ladend = false, ergebnis = ergebnis)
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Auf den VM setzen konnte nicht geladen werden.")
            }
        }
    }
}