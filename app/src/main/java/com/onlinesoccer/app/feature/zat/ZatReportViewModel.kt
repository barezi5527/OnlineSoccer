package com.onlinesoccer.app.feature.zat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.ZatReport
import com.onlinesoccer.app.data.repository.ZatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ZatReportUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val report: ZatReport? = null,
    /** Im Filter wählbare ZATs (für die aktuell gewählte Saison). */
    val zats: List<Int> = emptyList(),
    /** Im Filter wählbare Saisons. */
    val saisons: List<Int> = emptyList(),
    val ausgewaehlteSaison: Int? = null,
    val ausgewaehlterZat: Int? = null,
)

/** Logik für den persönlichen ZAT-Report (`zar.php`). */
@HiltViewModel
class ZatReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ZatRepository,
) : ViewModel() {

    private val startZat: Int? = savedStateHandle.get<String>("zat")?.toIntOrNull()
    private val startSaison: Int? = savedStateHandle.get<String>("saison")?.toIntOrNull()

    private val _uiState = MutableStateFlow(ZatReportUiState())
    val uiState: StateFlow<ZatReportUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() = ladeFuer(startZat, startSaison)

    fun saisonWaehlen(saison: Int) {
        _uiState.value = _uiState.value.copy(ausgewaehlteSaison = saison)
        ladeFuer(zat = null, saison = saison)
    }

    fun zatWaehlen(zat: Int) {
        _uiState.value = _uiState.value.copy(ausgewaehlterZat = zat)
        ladeFuer(zat = zat, saison = _uiState.value.ausgewaehlteSaison)
    }

    private fun ladeFuer(zat: Int?, saison: Int?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                val report = repository.ladeZatReport(zat, saison)
                if (report == null) {
                    _uiState.value.copy(ladend = false, fehler = "ZAT-Report konnte nicht geladen werden.")
                } else {
                    _uiState.value.copy(
                        ladend = false,
                        report = report,
                        zats = report.zats.ifEmpty { _uiState.value.zats },
                        saisons = report.saisons.ifEmpty { _uiState.value.saisons },
                        ausgewaehlteSaison = report.saison ?: _uiState.value.ausgewaehlteSaison,
                        ausgewaehlterZat = report.zat ?: _uiState.value.ausgewaehlterZat,
                    )
                }
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "ZAT-Report konnte nicht geladen werden.",
                )
            }
        }
    }
}