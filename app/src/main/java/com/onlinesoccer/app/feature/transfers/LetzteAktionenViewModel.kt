package com.onlinesoccer.app.feature.transfers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.LetzteAktionenArt
import com.onlinesoccer.app.data.model.LetzteAktionenErgebnis
import com.onlinesoccer.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LetzteAktionenUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val ergebnis: LetzteAktionenErgebnis = LetzteAktionenErgebnis(),
)

/**
 * Native Darstellung der „Letzten …"-Seiten unter „Transfers"
 * (`Letzte Transfers`, `Letzte Leihen`, `Letzte VM-Käufe`, `Letzte TM-Käufe`,
 * `Letzte Schnelltransfers`). Die Art kommt über das Routen-Argument „art".
 */
@HiltViewModel
class LetzteAktionenViewModel @Inject constructor(
    private val repository: ServerRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val art: LetzteAktionenArt = savedStateHandle.get<String>("art")
        ?.let(LetzteAktionenArt::vonRouteId)
        ?: LetzteAktionenArt.TRANSFERS

    private val _uiState = MutableStateFlow(LetzteAktionenUiState())
    val uiState: StateFlow<LetzteAktionenUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
        viewModelScope.launch {
            _uiState.value = try {
                val ergebnis = repository.letzteAktionen(art)
                _uiState.value.copy(ladend = false, ergebnis = ergebnis)
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "${art.titel} konnten nicht geladen werden.",
                )
            }
        }
    }
}