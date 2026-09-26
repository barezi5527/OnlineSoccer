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
    val land: String = "0",
    val liga: String = "0",
    val statistik: String = "1",
    val pos: String = "0",
    val saison: String = "24",
    val art: String = "1",
    val laender: List<LaenderOption> = emptyList(),
    val ligas: List<LaenderOption> = emptyList(),
    val statistiken: List<LaenderOption> = emptyList(),
    val positionen: List<LaenderOption> = emptyList(),
    val saisons: List<LaenderOption> = emptyList(),
    val arten: List<LaenderOption> = emptyList(),
    val zeilen: List<TopscorerZeile> = emptyList(),
)

/** „Topscorer" (`topscorer.php`): Beste Spieler nach Land/Liga/Statistik/Position/Saison/Art. */
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
        lade(s.land, s.liga, s.statistik, s.pos, s.saison, s.art)
    }

    fun landWaehlen(id: String) = lade(id, _uiState.value.liga, _uiState.value.statistik, _uiState.value.pos, _uiState.value.saison, _uiState.value.art)

    fun ligaWaehlen(id: String) = lade(_uiState.value.land, id, _uiState.value.statistik, _uiState.value.pos, _uiState.value.saison, _uiState.value.art)

    fun statistikWaehlen(id: String) = lade(_uiState.value.land, _uiState.value.liga, id, _uiState.value.pos, _uiState.value.saison, _uiState.value.art)

    fun posWaehlen(id: String) = lade(_uiState.value.land, _uiState.value.liga, _uiState.value.statistik, id, _uiState.value.saison, _uiState.value.art)

    fun saisonWaehlen(id: String) = lade(_uiState.value.land, _uiState.value.liga, _uiState.value.statistik, _uiState.value.pos, id, _uiState.value.art)

    fun artWaehlen(id: String) = lade(_uiState.value.land, _uiState.value.liga, _uiState.value.statistik, _uiState.value.pos, _uiState.value.saison, id)

    private fun lade(land: String, liga: String, statistik: String, pos: String, saison: String, art: String) {
        _uiState.value = _uiState.value.copy(
            ladend = true,
            fehler = null,
            land = land,
            liga = liga,
            statistik = statistik,
            pos = pos,
            saison = saison,
            art = art,
        )
        viewModelScope.launch {
            _uiState.value = try {
                val daten = repository.topscorer(land, liga, statistik, pos, saison, art)
                _uiState.value.copy(
                    ladend = false,
                    laender = daten.laender.ifEmpty { _uiState.value.laender },
                    ligas = daten.ligas.ifEmpty { _uiState.value.ligas },
                    statistiken = daten.statistiken.ifEmpty { _uiState.value.statistiken },
                    positionen = daten.positionen.ifEmpty { _uiState.value.positionen },
                    saisons = daten.saisons.ifEmpty { _uiState.value.saisons },
                    arten = daten.arten.ifEmpty { _uiState.value.arten },
                    zeilen = daten.zeilen.take(20),
                )
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Topscorer konnten nicht geladen werden.",
                )
            }
        }
    }
}
