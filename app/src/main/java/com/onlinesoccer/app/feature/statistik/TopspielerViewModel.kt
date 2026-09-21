package com.onlinesoccer.app.feature.statistik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.LaenderOption
import com.onlinesoccer.app.data.model.TopspielerZeile
import com.onlinesoccer.app.data.repository.StatistikRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TopspielerUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val land: String = "6",
    val liga: String = "1",
    val statistik: String = "16",
    val position: String = "0",
    val anzeige: String = "1",
    val laender: List<LaenderOption> = emptyList(),
    val ligas: List<LaenderOption> = emptyList(),
    val statistiken: List<LaenderOption> = emptyList(),
    val positionen: List<LaenderOption> = emptyList(),
    val anzeigen: List<LaenderOption> = emptyList(),
    val zeilen: List<TopspielerZeile> = emptyList(),
)

/** „Topspieler" (`osneu/statspieler`): Beste Spieler nach Land/Liga/Statistik. */
@HiltViewModel
class TopspielerViewModel @Inject constructor(
    private val repository: StatistikRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopspielerUiState())
    val uiState: StateFlow<TopspielerUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                val daten = repository.topspieler(s.land, s.liga, s.statistik, s.position, s.anzeige)
                _uiState.value.copy(
                    ladend = false,
                    laender = daten.laender.ifEmpty { _uiState.value.laender },
                    ligas = daten.ligas.ifEmpty { _uiState.value.ligas },
                    statistiken = daten.statistiken.ifEmpty { _uiState.value.statistiken },
                    positionen = daten.positionen.ifEmpty { _uiState.value.positionen },
                    anzeigen = daten.anzeigen.ifEmpty { _uiState.value.anzeigen },
                    zeilen = daten.zeilen,
                )
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Topspieler konnten nicht geladen werden.",
                )
            }
        }
    }

    fun landWaehlen(id: String) = waehlen { it.copy(land = id) }

    fun ligaWaehlen(id: String) = waehlen { it.copy(liga = id) }

    fun statistikWaehlen(id: String) = waehlen { it.copy(statistik = id) }

    fun positionWaehlen(id: String) = waehlen { it.copy(position = id) }

    fun anzeigeWaehlen(id: String) = waehlen { it.copy(anzeige = id) }

    private fun waehlen(aendere: (TopspielerUiState) -> TopspielerUiState) {
        _uiState.value = aendere(_uiState.value)
        lade()
    }
}