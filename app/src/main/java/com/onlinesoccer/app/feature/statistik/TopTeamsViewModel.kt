package com.onlinesoccer.app.feature.statistik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.LaenderOption
import com.onlinesoccer.app.data.model.TopTeamZeile
import com.onlinesoccer.app.data.repository.StatistikRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TopTeamsUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val land: String = "0",
    val liga: String = "0",
    val statistik: String = "2",
    val anzeige: String = "1",
    val laender: List<LaenderOption> = emptyList(),
    val ligas: List<LaenderOption> = emptyList(),
    val statistiken: List<LaenderOption> = emptyList(),
    val anzeigen: List<LaenderOption> = emptyList(),
    val zeilen: List<TopTeamZeile> = emptyList(),
)

/** „Top-Teams" (`osneu/statteam`): Wertvollste Teams nach Land/Liga/Statistik/Anzeige. */
@HiltViewModel
class TopTeamsViewModel @Inject constructor(
    private val repository: StatistikRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopTeamsUiState())
    val uiState: StateFlow<TopTeamsUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        val s = _uiState.value
        lade(s.land, s.liga, s.statistik, s.anzeige)
    }

    fun landWaehlen(id: String) = lade(id, _uiState.value.liga, _uiState.value.statistik, _uiState.value.anzeige)

    fun ligaWaehlen(id: String) = lade(_uiState.value.land, id, _uiState.value.statistik, _uiState.value.anzeige)

    fun statistikWaehlen(id: String) = lade(_uiState.value.land, _uiState.value.liga, id, _uiState.value.anzeige)

    fun anzeigeWaehlen(id: String) = lade(_uiState.value.land, _uiState.value.liga, _uiState.value.statistik, id)

    private fun lade(land: String, liga: String, statistik: String, anzeige: String) {
        _uiState.value = _uiState.value.copy(
            ladend = true,
            fehler = null,
            land = land,
            liga = liga,
            statistik = statistik,
            anzeige = anzeige,
        )
        viewModelScope.launch {
            _uiState.value = try {
                val daten = repository.topteams(land, liga, statistik, anzeige)
                _uiState.value.copy(
                    ladend = false,
                    laender = daten.laender.ifEmpty { _uiState.value.laender },
                    ligas = daten.ligas.ifEmpty { _uiState.value.ligas },
                    statistiken = daten.statistiken.ifEmpty { _uiState.value.statistiken },
                    anzeigen = daten.anzeigen.ifEmpty { _uiState.value.anzeigen },
                    zeilen = daten.zeilen,
                )
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Topteams konnten nicht geladen werden.",
                )
            }
        }
    }
}