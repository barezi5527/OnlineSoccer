package com.onlinesoccer.app.feature.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.KaderSpieler
import com.onlinesoccer.app.data.model.SpielerPosition
import com.onlinesoccer.app.data.model.StaerkeZeile
import com.onlinesoccer.app.data.model.StatistikZeile
import com.onlinesoccer.app.data.model.Teaminfo
import com.onlinesoccer.app.data.model.VertragZeile
import com.onlinesoccer.app.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeamUiState(
    val ladend: Boolean = false,
    val fehler: String? = null,
    // Kader
    val kader: List<KaderSpieler> = emptyList(),
    val positionsFilter: SpielerPosition? = null,
    val sortierung: Sortierung = Sortierung.POSITION,
    // Sub-Bereiche (lazy geladen)
    val vertraege: List<VertragZeile>? = null,
    val staerken: List<StaerkeZeile>? = null,
    val statistik: List<StatistikZeile>? = null,
    val statistikGesamt: Boolean = false,
    val teaminfo: Teaminfo? = null,
) {
    val gefiltert: List<KaderSpieler>
        get() = kader
            .filter { positionsFilter == null || it.position == positionsFilter }
            .sortedWith(when (sortierung) {
                Sortierung.POSITION -> compareBy<KaderSpieler> { it.position.rang }.thenByDescending { it.skill }.thenByDescending { it.opti }
                Sortierung.SKILL -> compareByDescending<KaderSpieler> { it.skill }.thenByDescending { it.opti }
                Sortierung.OPTI -> compareByDescending<KaderSpieler> { it.opti }.thenByDescending { it.skill }
                Sortierung.ALTER -> compareBy<KaderSpieler> { it.alter }
                Sortierung.NUMMER -> compareBy<KaderSpieler> { it.nummer.toIntOrNull() ?: Int.MAX_VALUE }
            })
}

enum class Sortierung {
    POSITION, SKILL, OPTI, ALTER, NUMMER,
}

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val repository: TeamRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()
    private var ladeJob: Job? = null

    fun ladeKader() {
        ladeJob?.cancel()
        ladeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                _uiState.value.copy(ladend = false, kader = repository.ladeKader())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Kader konnte nicht geladen werden.",
                )
            }
        }
    }

    fun setzeFilter(position: SpielerPosition?) {
        _uiState.value = _uiState.value.copy(positionsFilter = position)
    }

    fun setzeSortierung(sortierung: Sortierung) {
        _uiState.value = _uiState.value.copy(sortierung = sortierung)
    }

    fun ladeVertraege() {
        if (_uiState.value.vertraege != null) return
        ladeJob?.cancel()
        ladeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                _uiState.value.copy(ladend = false, vertraege = repository.ladeVertraege())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Verträge konnten nicht geladen werden.")
            }
        }
    }

    fun ladeStaerken() {
        if (_uiState.value.staerken != null) return
        ladeJob?.cancel()
        ladeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                _uiState.value.copy(ladend = false, staerken = repository.ladeStaerken())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Stärken konnten nicht geladen werden.")
            }
        }
    }

    fun ladeStatistik(gesamt: Boolean) {
        if (_uiState.value.statistik != null && _uiState.value.statistikGesamt == gesamt) return
        ladeJob?.cancel()
        ladeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null, statistikGesamt = gesamt)
            _uiState.value = try {
                _uiState.value.copy(ladend = false, statistik = repository.ladeStatistik(gesamt))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Statistik konnte nicht geladen werden.")
            }
        }
    }

    fun ladeTeaminfo() {
        if (_uiState.value.teaminfo != null) return
        ladeJob?.cancel()
        ladeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                _uiState.value.copy(ladend = false, teaminfo = repository.ladeTeaminfo())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Teaminfo konnte nicht geladen werden.")
            }
        }
    }
}
