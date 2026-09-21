package com.onlinesoccer.app.feature.pm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.PmDetail
import com.onlinesoccer.app.data.model.PmNachricht
import com.onlinesoccer.app.data.model.PmAntwortFormular
import com.onlinesoccer.app.data.model.PmEmpfaengerVorschlag
import com.onlinesoccer.app.data.repository.PmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PmUiState(
    val ladende: Boolean = false,
    val liste: List<PmNachricht> = emptyList(),
    val detail: PmDetail? = null,
    val detailLadend: Boolean = false,
    val antwortLadend: Boolean = false,
    val antwort: PmAntwortFormular? = null,
    val neueNachricht: PmAntwortFormular? = null,
    val empfaengerSuche: List<PmEmpfaengerVorschlag> = emptyList(),
    val empfaengerSucheLadend: Boolean = false,
    val sendend: Boolean = false,
    val meldung: String? = null,
    val fehler: String? = null,
)

@HiltViewModel
class PmViewModel @Inject constructor(
    private val pmRepository: PmRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PmUiState())
    val uiState: StateFlow<PmUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = PmUiState(ladende = true)
            _uiState.value = try {
                PmUiState(ladende = false, liste = pmRepository.liste())
            } catch (e: Exception) {
                PmUiState(ladende = false, fehler = e.message ?: "PMs konnten nicht geladen werden.")
            }
        }
    }

    fun oeffne(pmId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(detailLadend = true, fehler = null, meldung = null, antwort = null)
            _uiState.value = try {
                _uiState.value.copy(
                    detailLadend = false,
                    detail = pmRepository.lesen(pmId),
                )
            } catch (e: Exception) {
                _uiState.value.copy(
                    detailLadend = false,
                    fehler = e.message ?: "Nachricht konnte nicht geöffnet werden.",
                )
            }
        }
    }

    fun zurueck() {
        _uiState.value = _uiState.value.copy(detail = null, detailLadend = false, antwort = null, fehler = null, meldung = null)
    }

    fun starteAntwort() {
        val id = _uiState.value.detail?.nachricht?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(antwortLadend = true, fehler = null)
            try {
                _uiState.value = _uiState.value.copy(
                    antwortLadend = false,
                    antwort = pmRepository.antwortFormular(id),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    antwortLadend = false,
                    fehler = e.message ?: "Antwortformular konnte nicht geladen werden.",
                )
            }
        }
    }

    fun setAntwortBetreff(value: String) {
        _uiState.value = _uiState.value.copy(antwort = _uiState.value.antwort?.copy(betreff = value))
    }

    fun setAntwortText(value: String) {
        _uiState.value = _uiState.value.copy(antwort = _uiState.value.antwort?.copy(text = value))
    }

    fun abbrechenAntwort() {
        _uiState.value = _uiState.value.copy(antwort = null)
    }

    fun starteNeueNachricht() {
        _uiState.value = _uiState.value.copy(
            neueNachricht = PmAntwortFormular(empfaenger = "", empfaengerId = "", betreff = "", text = ""),
            empfaengerSuche = emptyList(),
            fehler = null,
            meldung = null,
        )
    }

    fun abbrechenNeueNachricht() {
        _uiState.value = _uiState.value.copy(
            neueNachricht = null,
            empfaengerSuche = emptyList(),
            empfaengerSucheLadend = false,
        )
    }

    fun sucheEmpfaenger(keyword: String) {
        val text = keyword.trim()
        if (text.length < 3) {
            _uiState.value = _uiState.value.copy(empfaengerSuche = emptyList(), empfaengerSucheLadend = false)
            return
        }
        _uiState.value = _uiState.value.copy(empfaengerSucheLadend = true)
        viewModelScope.launch {
            _uiState.value = try {
                _uiState.value.copy(
                    empfaengerSucheLadend = false,
                    empfaengerSuche = pmRepository.empfaengerSuchen(text),
                )
            } catch (e: Exception) {
                _uiState.value.copy(empfaengerSucheLadend = false, empfaengerSuche = emptyList())
            }
        }
    }

    fun waehleEmpfaenger(vorschlag: PmEmpfaengerVorschlag) {
        _uiState.value = _uiState.value.copy(
            neueNachricht = _uiState.value.neueNachricht?.copy(
                empfaenger = vorschlag.name,
                empfaengerId = vorschlag.id.toString(),
            ),
            empfaengerSuche = emptyList(),
            empfaengerSucheLadend = false,
        )
    }

    fun setNeueBetreff(value: String) {
        _uiState.value = _uiState.value.copy(neueNachricht = _uiState.value.neueNachricht?.copy(betreff = value))
    }

    fun setNeueText(value: String) {
        _uiState.value = _uiState.value.copy(neueNachricht = _uiState.value.neueNachricht?.copy(text = value))
    }

    fun sendeNeueNachricht() {
        val formular = _uiState.value.neueNachricht ?: return
        if (formular.empfaenger.trim().isEmpty() || formular.empfaengerId.isBlank()) {
            _uiState.value = _uiState.value.copy(fehler = "Bitte einen Empfänger aus der Auswahlliste wählen.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sendend = true, fehler = null)
            try {
                pmRepository.antworten(formular.copy(empfaenger = formular.empfaenger.trim()))
                val liste = pmRepository.liste()
                _uiState.value = PmUiState(liste = liste, meldung = "Nachricht gesendet.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    sendend = false,
                    fehler = e.message ?: "Nachricht konnte nicht gesendet werden.",
                )
            }
        }
    }

    fun sendeAntwort() {
        val formular = _uiState.value.antwort ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sendend = true, fehler = null)
            try {
                pmRepository.antworten(formular)
                _uiState.value = _uiState.value.copy(sendend = false, antwort = null, meldung = "Antwort gesendet.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    sendend = false,
                    fehler = e.message ?: "Antwort konnte nicht gesendet werden.",
                )
            }
        }
    }

    fun loescheDetail() {
        val id = _uiState.value.detail?.nachricht?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(detailLadend = true, fehler = null)
            try {
                pmRepository.loeschen(id)
                val liste = pmRepository.liste()
                _uiState.value = PmUiState(liste = liste)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    ladende = false,
                    detailLadend = false,
                    fehler = e.message ?: "Nachricht konnte nicht gelöscht werden.",
                )
            }
        }
    }
}
