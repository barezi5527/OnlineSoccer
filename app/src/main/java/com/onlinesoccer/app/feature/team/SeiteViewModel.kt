package com.onlinesoccer.app.feature.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.AktionForm
import com.onlinesoccer.app.data.model.SeitenAnsicht
import com.onlinesoccer.app.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SeiteUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val seite: SeitenAnsicht? = null,
    val sendend: Boolean = false,
    val aktioFehler: String? = null,
    /** In einer Zwischenansicht geladenes Formular (z. B. Scouting-Gebot). */
    val dialogFormular: AktionForm? = null,
)

@HiltViewModel
class SeiteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TeamRepository,
) : ViewModel() {

    private val path: String = savedStateHandle["path"] ?: ""

    private val _uiState = MutableStateFlow(SeiteUiState())
    val uiState: StateFlow<SeiteUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = SeiteUiState(ladend = true)
            _uiState.value = try {
                val ansicht = repository.ladeSeite(path)
                if (ansicht == null) {
                    SeiteUiState(
                        ladend = false,
                        fehler = "Seite nicht verfügbar – bitte anmelden und erneut versuchen.",
                    )
                } else {
                    SeiteUiState(ladend = false, seite = ansicht)
                }
            } catch (e: Exception) {
                SeiteUiState(ladend = false, fehler = e.message ?: "Seite konnte nicht geladen werden.")
            }
        }
    }

    /** Sendet ein ausgefülltes Formular und lädt die Seite danach frisch. */
    fun sendeAktion(ziel: String, felder: List<Pair<String, String>>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sendend = true, aktioFehler = null)
            val ok = repository.fuehreAktionAus(ziel, felder)
            if (ok) {
                _uiState.value = _uiState.value.copy(sendend = false, dialogFormular = null)
                lade()
            } else {
                _uiState.value = _uiState.value.copy(
                    sendend = false,
                    aktioFehler = "Aktion fehlgeschlagen – bitte anmelden und erneut versuchen.",
                )
            }
        }
    }

    /** Öffnet ein separates Formular (z. B. das Scouting-Gebot einer Angebotszeile). */
    fun ladeFormular(ziel: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(dialogFormular = repository.ladeAktionFormular(ziel))
        }
    }

    fun schliesseDialog() {
        _uiState.value = _uiState.value.copy(dialogFormular = null)
    }
}