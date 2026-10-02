package com.onlinesoccer.app.feature.bewerbe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.SpielBericht
import com.onlinesoccer.app.data.model.StadionnameLogik
import com.onlinesoccer.app.data.repository.BerichtRepository
import com.onlinesoccer.app.data.repository.DashboardRepository
import com.onlinesoccer.app.core.storage.StadionnameStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BerichtUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val bericht: SpielBericht? = null,
    val stadionAnzeigename: String? = null,
)

@HiltViewModel
class SpielberichtViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BerichtRepository,
    private val dashboardRepository: DashboardRepository,
    private val stadionnameStore: StadionnameStore,
) : ViewModel() {

    private val sid: String? = savedStateHandle["sid"]
    private val url: String? = savedStateHandle["url"]

    private val _uiState = MutableStateFlow(BerichtUiState())
    val uiState: StateFlow<BerichtUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = BerichtUiState(ladend = true)
            _uiState.value = try {
                val bericht = repository.ladeBericht(sid, url)
                    ?: error("Spielbericht konnte nicht geladen werden.")
                val teamId = runCatching {
                    dashboardRepository.fetchDashboard(forceRefresh = true).teamId?.takeIf { it > 0L }
                }.getOrNull()
                val gespeichert = teamId?.let {
                    runCatching { stadionnameStore.lesen(it) }.getOrNull()
                }
                BerichtUiState(
                    ladend = false,
                    bericht = bericht,
                    stadionAnzeigename = StadionnameLogik.anzeigename(bericht, teamId, gespeichert),
                )
            } catch (e: Exception) {
                BerichtUiState(ladend = false, fehler = e.message ?: "Spielbericht konnte nicht geladen werden.")
            }
        }
    }
}
