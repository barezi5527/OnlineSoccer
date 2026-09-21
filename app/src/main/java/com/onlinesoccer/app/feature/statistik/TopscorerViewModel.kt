package com.onlinesoccer.app.feature.statistik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.LaenderOption
import com.onlinesoccer.app.data.model.TopscorerZeile
import com.onlinesoccer.app.data.repository.StatistikRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TopscorerUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val land: String = "6",
    val liga: String = "1",
    val statistik: String = "1",
    val position: String = "0",
    val saison: String = "24",
    val art: String = "0",
    val laender: List<LaenderOption> = emptyList(),
    val ligas: List<LaenderOption> = emptyList(),
    val statistiken: List<LaenderOption> = emptyList(),
    val positionen: List<LaenderOption> = emptyList(),
    val saisons: List<LaenderOption> = emptyList(),
    val arten: List<LaenderOption> = emptyList(),
    val zeilen: List<TopscorerZeile> = emptyList(),
)

/** „Topscorer" (`topscorer.php`): Tore/Vorlagen/… nach Filter. */
@HiltViewModel
class TopscorerViewModel @Inject constructor(
    private val repository: StatistikRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopscorerUiState())
    val uiState: StateFlow<TopscorerUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                val daten = repository.topscorer(
                    land = s.land,
                    liga = s.liga,
                    statistik = s.statistik,
                    position = s.position,
                    saison = s.saison,
                    art = s.art,
                )
                _uiState.value.copy(
                    ladend = false,
                    laender = daten.laender.ifEmpty { _uiState.value.laender },
                    ligas = daten.ligas.ifEmpty { _uiState.value.ligas },
                    statistiken = daten.statistiken.ifEmpty { _uiState.value.statistiken },
                    positionen = daten.positionen.ifEmpty { _uiState.value.positionen },
                    saisons = daten.saisons.ifEmpty { _uiState.value.saisons },
                    arten = daten.arten.ifEmpty { _uiState.value.arten },
                    zeilen = daten.zeilen,
                )
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Topscorer konnten nicht geladen werden.",
                )
            }
        }
    }

    fun landWaehlen(id: String) = waehlen { it.copy(land = id) }

    fun ligaWaehlen(id: String) = waehlen { it.copy(liga = id) }

    fun statistikWaehlen(id: String) = waehlen { it.copy(statistik = id) }

    fun positionWaehlen(id: String) = waehlen { it.copy(position = id) }

    fun saisonWaehlen(id: String) = waehlen { it.copy(saison = id) }

    fun artWaehlen(id: String) = waehlen { it.copy(art = id) }

    private fun waehlen(aendere: (TopscorerUiState) -> TopscorerUiState) {
        _uiState.value = aendere(_uiState.value)
        lade()
    }
}