package com.onlinesoccer.app.feature.statistik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.LaenderOption
import com.onlinesoccer.app.data.model.TopteamZeile
import com.onlinesoccer.app.data.repository.StatistikRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TopteamsUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val land: String = "6",
    val liga: String = "1",
    val statistik: String = "2",
    val anzeige: String = "1",
    val laender: List<LaenderOption> = emptyList(),
    val ligas: List<LaenderOption> = emptyList(),
    val statistiken: List<LaenderOption> = emptyList(),
    val anzeigen: List<LaenderOption> = emptyList(),
    val zeilen: List<TopteamZeile> = emptyList(),
)

/** „Topteams" (`osneu/statteam`): Wertvollste Teams nach Land/Liga/Statistik. */
@HiltViewModel
class TopteamsViewModel @Inject constructor(
    private val repository: StatistikRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopteamsUiState())
    val uiState: StateFlow<TopteamsUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                val daten = repository.topteams(s.land, s.liga, s.statistik, s.anzeige)
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

    fun landWaehlen(id: String) = waehlen { it.copy(land = id) }

    fun ligaWaehlen(id: String) = waehlen { it.copy(liga = id) }

    fun statistikWaehlen(id: String) = waehlen { it.copy(statistik = id) }

    fun anzeigeWaehlen(id: String) = waehlen { it.copy(anzeige = id) }

    private fun waehlen(aendere: (TopteamsUiState) -> TopteamsUiState) {
        _uiState.value = aendere(_uiState.value)
        lade()
    }
}