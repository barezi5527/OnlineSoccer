package com.onlinesoccer.app.feature.statistik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.SpielerVorschlag
import com.onlinesoccer.app.data.model.Spielervergleich
import com.onlinesoccer.app.data.repository.StatistikRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpielervergleichUiState(
    val fehler: String? = null,
    val name1: String = "",
    val name2: String = "",
    val id1: Long? = null,
    val id2: Long? = null,
    val vorschlaege1: List<SpielerVorschlag> = emptyList(),
    val vorschlaege2: List<SpielerVorschlag> = emptyList(),
    val ladendVergleich: Boolean = false,
    val vergleich: Spielervergleich? = null,
    /** 0 = normal, 1 = Differenz-Ansicht. */
    val ansicht: Int = 0,
)

/** Spielervergleich (`osneu/spielervergleich`) zweier Spieler. */
@HiltViewModel
class SpielervergleichViewModel @Inject constructor(
    private val repository: StatistikRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpielervergleichUiState())
    val uiState: StateFlow<SpielervergleichUiState> = _uiState.asStateFlow()

    fun textAendern(inhalt: String, spieler2: Boolean) {
        val field = if (spieler2) "vorschlaege2" to "name2" else "vorschlaege1" to "name1"
        _uiState.value = if (spieler2) {
            _uiState.value.copy(name2 = inhalt, id2 = null, vorschlaege2 = emptyList())
        } else {
            _uiState.value.copy(name1 = inhalt, id1 = null, vorschlaege1 = emptyList())
        }
        if (inhalt.isBlank()) return
        viewModelScope.launch {
            val vorschlaege = repository.findSpieler(inhalt)
            _uiState.value = if (spieler2) {
                _uiState.value.copy(vorschlaege2 = vorschlaege)
            } else {
                _uiState.value.copy(vorschlaege1 = vorschlaege)
            }
        }
    }

    fun vorschlagWaehlen(vorschlag: SpielerVorschlag, spieler2: Boolean) {
        _uiState.value = if (spieler2) {
            _uiState.value.copy(
                name2 = vorschlag.name,
                id2 = vorschlag.id,
                vorschlaege2 = emptyList(),
                vergleich = null,
            )
        } else {
            _uiState.value.copy(
                name1 = vorschlag.name,
                id1 = vorschlag.id,
                vorschlaege1 = emptyList(),
                vergleich = null,
            )
        }
    }

    fun vergleichen() {
        val s = _uiState.value
        val id1 = s.id1 ?: return
        val id2 = s.id2 ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladendVergleich = true, fehler = null)
            val vergleich = repository.vergleiche(id1, id2, s.ansicht)
            _uiState.value = _uiState.value.copy(
                ladendVergleich = false,
                vergleich = vergleich,
                fehler = if (vergleich == null) "Vergleich konnte nicht geladen werden." else null,
            )
        }
    }

    fun ansichtWechseln(ansicht: Int) {
        _uiState.value = _uiState.value.copy(ansicht = ansicht)
        if (_uiState.value.vergleich != null) vergleichen()
    }
}