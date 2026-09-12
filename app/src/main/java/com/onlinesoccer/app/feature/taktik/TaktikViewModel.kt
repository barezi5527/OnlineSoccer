package com.onlinesoccer.app.feature.taktik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.Taktik
import com.onlinesoccer.app.data.repository.TaktikQuelle
import com.onlinesoccer.app.data.repository.TaktikRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaktikUiState(
    val ladende: Boolean = false,
    val taktik: Taktik = Taktik(emptySet()),
    val name: String = "",
    val gewaehlteStandardId: String = "",
    val gewaehlteEigeneId: String = "",
    val ladeAktion: Boolean = false,
    val loeschende: Boolean = false,
    val speichernd: Boolean = false,
    val meldung: String? = null,
    val fehler: String? = null,
)

@HiltViewModel
class TaktikViewModel @Inject constructor(
    private val repository: TaktikRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaktikUiState())
    val uiState: StateFlow<TaktikUiState> = _uiState.asStateFlow()

    init {
        ladeTaktik()
    }

    fun ladeTaktik() {
        viewModelScope.launch {
            _uiState.value = TaktikUiState(ladende = true)
            _uiState.value = try {
                val taktik = repository.ladeTaktik()
                TaktikUiState(
                    taktik = taktik,
                )
            } catch (e: Exception) {
                TaktikUiState(
                    fehler = e.message ?: "Taktik konnte nicht geladen werden.",
                )
            }
        }
    }

    fun toggleCode(code: String) {
        val codes = _uiState.value.taktik.codes
        val neu = if (code in codes) codes - code else codes + code
        _uiState.value = _uiState.value.copy(
            taktik = _uiState.value.taktik.copy(codes = neu),
            fehler = null,
            meldung = null,
        )
    }

    fun leereRaster() {
        _uiState.value = _uiState.value.copy(
            taktik = _uiState.value.taktik.copy(codes = emptySet()),
            fehler = null,
            meldung = null,
        )
    }

    fun waehleStandard(id: String) {
        _uiState.value = _uiState.value.copy(gewaehlteStandardId = id, fehler = null, meldung = null)
    }

    fun waehleEigene(id: String) {
        _uiState.value = _uiState.value.copy(gewaehlteEigeneId = id, fehler = null, meldung = null)
    }

    fun ladeGewaehlte() {
        val state = _uiState.value
        val standardId = state.gewaehlteStandardId
        val eigeneId = state.gewaehlteEigeneId
        val quelle = when {
            standardId.isNotBlank() -> TaktikQuelle.STANDARD to standardId
            eigeneId.isNotBlank() -> TaktikQuelle.EIGENE to eigeneId
            else -> {
                _uiState.value = state.copy(fehler = "Bitte zuerst eine Taktik auswählen.")
                return
            }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladeAktion = true, fehler = null, meldung = null)
            try {
                val taktik = repository.ladeTaktik(quelle.second, quelle.first)
                val loadedLabel = when (quelle.first) {
                    TaktikQuelle.STANDARD -> taktik.standardTaktiken
                    TaktikQuelle.EIGENE -> taktik.eigeneTaktiken
                }.firstOrNull { it.id == quelle.second }?.label
                _uiState.value = _uiState.value.copy(
                    taktik = taktik,
                    name = loadedLabel ?: taktik.speichername ?: "Meine Taktik",
                    gewaehlteStandardId = "",
                    gewaehlteEigeneId = "",
                    ladeAktion = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    ladeAktion = false,
                    fehler = e.message ?: "Taktik konnte nicht geladen werden.",
                )
            }
        }
    }

    fun loescheGewaehlte() {
        val state = _uiState.value
        val id = state.gewaehlteEigeneId
        if (id.isBlank()) {
            _uiState.value = state.copy(fehler = "Bitte zuerst eine eigene Taktik auswählen.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loeschende = true, fehler = null, meldung = null)
            try {
                val taktik = repository.loescheTaktik(id)
                _uiState.value = _uiState.value.copy(
                    taktik = taktik,
                    gewaehlteEigeneId = "",
                    loeschende = false,
                    meldung = "Taktik gelöscht.",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loeschende = false,
                    fehler = e.message ?: "Taktik konnte nicht gelöscht werden.",
                )
            }
        }
    }

    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, fehler = null)
    }

    fun speichere() {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.value = state.copy(fehler = "Bitte einen Namen für die Taktik vergeben.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(speichernd = true, fehler = null, meldung = null)
            try {
                val taktik = repository.speichereTaktik(name, _uiState.value.taktik.codes)
                _uiState.value = _uiState.value.copy(
                    taktik = taktik,
                    speichernd = false,
                    meldung = "Taktik \u201e$name\u201c gespeichert.",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    speichernd = false,
                    fehler = e.message ?: "Speichern fehlgeschlagen.",
                )
            }
        }
    }
}