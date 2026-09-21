package com.onlinesoccer.app.feature.statistik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.GespeicherteAbfrage
import com.onlinesoccer.app.data.model.SpielersucheErgebnis
import com.onlinesoccer.app.data.model.SpielersucheOptionen
import com.onlinesoccer.app.data.model.SucheBasis
import com.onlinesoccer.app.data.model.SucheKriterium
import com.onlinesoccer.app.data.repository.StatistikRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpielersucheUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val optionen: SpielersucheOptionen = SpielersucheOptionen(),
    val basis: SucheBasis = SucheBasis(),
    val kriterien: List<SucheKriterium> = listOf(SucheKriterium(attributId = 1, opVon = ">", valVon = "")),
    val sucht: Boolean = false,
    val ergebnis: SpielersucheErgebnis? = null,
    val abfragen: List<GespeicherteAbfrage> = emptyList(),
    val abfrageName: String = "",
    val ausgewaehlteAbfrageId: Int = 0,
    val abfrageFehler: String? = null,
)

/** Erlaubte Operatoren je Attributtyp – wie `operatorenProTyp` der Website. */
fun erlaubteOperatoren(typ: String): List<String> = when (typ) {
    "text" -> listOf("=", "<>", "like")
    "kategorie" -> listOf("=", "<>")
    else -> listOf(">", "<", ">=", "<=", "<>", "=", "like")
}

/** Spielersuche (`osneu/spielersuche`) mit Basis-Filter, Kriterien und Abfragen. */
@HiltViewModel
class SpielersucheViewModel @Inject constructor(
    private val repository: StatistikRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpielersucheUiState())
    val uiState: StateFlow<SpielersucheUiState> = _uiState.asStateFlow()

    init {
        ladeOptionen()
    }

    fun ladeOptionen() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                val optionen = repository.spielersucheOptionen()
                _uiState.value.copy(ladend = false, optionen = optionen)
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Spielersuche konnte nicht geladen werden.")
            }
            ladeAbfragen()
        }
    }

    // ------------------------------------------------------------- Basis-Filter

    fun landWaehlen(id: String) = basisAendern { it.copy(landId = id.toIntOrNull() ?: 0) }
    fun ligaWaehlen(id: String) = basisAendern { it.copy(ligaId = id.toIntOrNull() ?: 0) }
    fun nationWaehlen(id: String) = basisAendern { it.copy(nationId = id.toIntOrNull() ?: 0) }
    fun anzeigeWaehlen(id: String) = basisAendern { it.copy(anzeigeId = id.toIntOrNull() ?: 1) }

    private fun basisAendern(wandler: (SucheBasis) -> SucheBasis) {
        _uiState.value = _uiState.value.copy(basis = wandler(_uiState.value.basis), ergebnis = null)
    }

    // --------------------------------------------------------------- Kriterien

    private fun attribut(id: Int) = _uiState.value.optionen.attribute.firstOrNull { it.id == id }

    fun kriteriumAendern(index: Int, kriterium: SucheKriterium) {
        val kriterien = _uiState.value.kriterien.toMutableList()
        if (index !in kriterien.indices) return
        val current = kriterien[index]
        val neu = if (current.attributId != kriterium.attributId) {
            val typ = attribut(kriterium.attributId)?.typ ?: "zahl"
            kriterium.copy(
                opVon = erlaubteOperatoren(typ).first(),
                valVon = "",
                opBis = "",
                valBis = "",
                sort = "",
            )
        } else {
            kriterium
        }
        kriterien[index] = neu
        _uiState.value = _uiState.value.copy(kriterien = kriterien, ergebnis = null)
    }

    fun kriteriumHinzufuegen() {
        val ersteAttributId = _uiState.value.optionen.attribute.firstOrNull()?.id ?: 1
        _uiState.value =  _uiState.value.copy(
            kriterien = _uiState.value.kriterien + SucheKriterium(attributId = ersteAttributId, opVon = ">", valVon = ""),
            ergebnis = null,
        )
    }

    fun kriteriumEntfernen(index: Int) {
        if (_uiState.value.kriterien.size <= 1) return
        _uiState.value = _uiState.value.copy(
            kriterien = _uiState.value.kriterien.filterIndexed { i, _ -> i != index },
            ergebnis = null,
        )
    }

    fun kriteriumHoch(index: Int) = kriteriumTauschen(index, index - 1)

    fun kriteriumRunter(index: Int) = kriteriumTauschen(index, index + 1)

    private fun kriteriumTauschen(a: Int, b: Int) {
        val list = _uiState.value.kriterien.toMutableList()
        if (a !in list.indices || b !in list.indices) return
        val tmp = list[a]
        list[a] = list[b]
        list[b] = tmp
        _uiState.value = _uiState.value.copy(kriterien = list, ergebnis = null)
    }

    // ------------------------------------------------------------------ Suchen

    fun suchen() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sucht = true, ergebnis = null)
            val ergebnis = repository.spielerSuchen(s.basis, s.kriterien)
            _uiState.value = _uiState.value.copy(sucht = false, ergebnis = ergebnis)
        }
    }

    // ------------------------------------------------------- Gespeicherte Abfragen

    fun ladeAbfragen() {
        viewModelScope.launch {
            val abfragen = repository.abfragen()
            _uiState.value = _uiState.value.copy(
                abfragen = abfragen,
                ausgewaehlteAbfrageId = if (_uiState.value.ausgewaehlteAbfrageId != 0) _uiState.value.ausgewaehlteAbfrageId else 0,
            )
            if (abfragen.none { it.id == _uiState.value.ausgewaehlteAbfrageId }) {
                _uiState.value = _uiState.value.copy(ausgewaehlteAbfrageId = 0)
            }
        }
    }

    fun abfrageNameAendern(name: String) {
        _uiState.value = _uiState.value.copy(abfrageName = name, abfrageFehler = null)
    }

    fun abfrageLaden(id: Int) {
        if (id <= 0) return
        viewModelScope.launch {
            val details = repository.abfrage(id) ?: return@launch
            var kriterien = details.kriterien
            if (kriterien.isEmpty()) kriterien = listOf(SucheKriterium(attributId = 1, opVon = ">", valVon = ""))
            _uiState.value = _uiState.value.copy(
                basis = details.basis,
                kriterien = kriterien,
                abfrageName = details.name,
                ausgewaehlteAbfrageId = id,
                abfrageFehler = null,
                ergebnis = null,
            )
        }
    }

    fun abfrageSpeichern(alsNeu: Boolean) {
        val s = _uiState.value
        if (s.abfrageName.isBlank()) {
            _uiState.value = _uiState.value.copy(abfrageFehler = "Bitte einen Namen angeben.")
            return
        }
        viewModelScope.launch {
            val id = repository.abfrageSpeichern(
                basis = s.basis,
                kriterien = s.kriterien,
                name = s.abfrageName,
                id = if (alsNeu) 0 else s.ausgewaehlteAbfrageId,
            )
            if (id == null) {
                _uiState.value = _uiState.value.copy(abfrageFehler = "Speichern nicht möglich.")
            } else {
                _uiState.value = _uiState.value.copy(ausgewaehlteAbfrageId = id)
                ladeAbfragen()
            }
        }
    }

    fun abfrageLoeschen(id: Int) {
        if (id <= 0) return
        viewModelScope.launch {
            repository.abfrageLoeschen(id)
            _uiState.value = _uiState.value.copy(
                ausgewaehlteAbfrageId = 0,
                abfrageName = "",
                ergebnis = null,
            )
            ladeAbfragen()
        }
    }
}