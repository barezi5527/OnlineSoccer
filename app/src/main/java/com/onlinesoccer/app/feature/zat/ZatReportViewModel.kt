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
)

/** Logik für den persönlichen ZAT-Report (`zar.php`). */
@HiltViewModel
class ZatReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ZatRepository,
) : ViewModel() {

    private val zat: Int? = savedStateHandle.get<String>("zat")?.toIntOrNull()
    private val saison: Int? = savedStateHandle.get<String>("saison")?.toIntOrNull()

    private val _uiState = MutableStateFlow(ZatReportUiState())
    val uiState: StateFlow<ZatReportUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = ZatReportUiState(ladend = true)
            _uiState.value = try {
                val report = repository.ladeZatReport(zat, saison)
                if (report == null) {
                    ZatReportUiState(ladend = false, fehler = "ZAT-Report konnte nicht geladen werden.")
                } else {
                    ZatReportUiState(ladend = false, report = report)
                }
            } catch (e: Exception) {
                ZatReportUiState(
                    ladend = false,
                    fehler = e.message ?: "ZAT-Report konnte nicht geladen werden.",
                )
            }
        }
    }
}