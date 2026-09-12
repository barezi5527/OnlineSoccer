package com.onlinesoccer.app.feature.zat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.ZuzuPreise
import com.onlinesoccer.app.data.model.ZuzuSpieler
import com.onlinesoccer.app.data.repository.ZatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Eingabefeld der Eintrittspreise (`zuzu.php`). */
enum class ZuzuPreisFeld { LIGA, POKAL, INTERNATIONAL }

data class ZuzuUiState(
    val ladend: Boolean = false,
    val geladen: Boolean = false,
    val speichernd: Boolean = false,
    val meldung: String? = null,
    val fehler: String? = null,
    val preise: ZuzuPreise = ZuzuPreise(),
    val spieler: List<ZuzuSpieler> = emptyList(),
    val auswahl: Set<Long> = emptySet(),
)

/** Logik für den Tab „Zugabgabe Zusatz“ (`zuzu.php`): Preise + Physio. */
@HiltViewModel
class ZuzuViewModel @Inject constructor(
    private val repository: ZatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ZuzuUiState())
    val uiState: StateFlow<ZuzuUiState> = _uiState.asStateFlow()

    fun lade() {
        if (_uiState.value.geladen) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                val daten = repository.ladeZuzu()
                _uiState.value.copy(
                    ladend = false,
                    geladen = daten != null,
                    fehler = if (daten == null) "Zugabgabe-Zusätze konnten nicht geladen werden." else null,
                    preise = daten?.preise ?: _uiState.value.preise,
                    spieler = daten?.spieler ?: _uiState.value.spieler,
                )
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Zugabgabe-Zusätze konnten nicht geladen werden.",
                )
            }
        }
    }

    fun setPreis(feld: ZuzuPreisFeld, wert: String) {
        _uiState.value = _uiState.value.copy(
            preise = when (feld) {
                ZuzuPreisFeld.LIGA -> _uiState.value.preise.copy(liga = wert)
                ZuzuPreisFeld.POKAL -> _uiState.value.preise.copy(pokal = wert)
                ZuzuPreisFeld.INTERNATIONAL -> _uiState.value.preise.copy(international = wert)
            },
            meldung = null,
        )
    }

    /** Speichert die Eintrittspreise (Heimspiele). Reale Schreibaktion. */
    fun preiseSpeichern() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(speichernd = true, meldung = null, fehler = null)
            _uiState.value = try {
                val daten = repository.zuzuPreiseSpeichern(_uiState.value.preise)
                _uiState.value.copy(
                    speichernd = false,
                    meldung = "\u2713 Eintrittspreise gespeichert",
                    preise = daten.preise,
                    spieler = daten.spieler,
                )
            } catch (e: Exception) {
                _uiState.value.copy(
                    speichernd = false,
                    fehler = e.message ?: "Eintrittspreise konnten nicht gespeichert werden.",
                )
            }
        }
    }

    fun toggleAuswahl(pid: Long) {
        val auswahl = _uiState.value.auswahl.toMutableSet()
        if (!auswahl.add(pid)) auswahl.remove(pid)
        _uiState.value = _uiState.value.copy(auswahl = auswahl)
    }

    /** Schickt markierte Spieler zum Physio. Reale Schreibaktion. */
    fun physioSchicken() {
        val pids = _uiState.value.auswahl.toList()
        if (pids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(speichernd = true, meldung = null, fehler = null)
            _uiState.value = try {
                val daten = repository.zuzuPhysioSchicken(pids)
                _uiState.value.copy(
                    speichernd = false,
                    meldung = "\u2713 ${pids.size} Spieler zum Physio geschickt",
                    auswahl = emptySet(),
                    preise = daten.preise,
                    spieler = daten.spieler,
                )
            } catch (e: Exception) {
                _uiState.value.copy(
                    speichernd = false,
                    fehler = e.message ?: "Zum Physio schicken fehlgeschlagen.",
                )
            }
        }
    }
}