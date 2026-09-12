package com.onlinesoccer.app.feature.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.GebotInformation
import com.onlinesoccer.app.data.model.SpielerKarte
import com.onlinesoccer.app.data.repository.ServerRepository
import com.onlinesoccer.app.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpielerkarteUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val karte: SpielerKarte? = null,
    /** True, wenn die Karte aus dem Transfermarkt geöffnet wurde („Bieten" sichtbar). */
    val bietenMoeglich: Boolean = false,
)

/** Zustand des „Bieten"-Dialogs (Gebote werden erst nach Bestätigung gesendet). */
sealed interface BietDialogState {
    data object Verborgen : BietDialogState
    data object Laedt : BietDialogState
    data class Bereit(val info: GebotInformation?) : BietDialogState
    data class Ergebnis(val erfolg: Boolean, val meldung: String) : BietDialogState
}

@HiltViewModel
class SpielerkarteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TeamRepository,
    private val serverRepository: ServerRepository,
) : ViewModel() {

    private val pid: Long = (savedStateHandle["pid"] as? Long) ?: (savedStateHandle["pid"] as? String)?.toLongOrNull() ?: 0L

    /** Team-ID, nur wenn die Spielerkarte aus einem fremden Verein geöffnet wurde. */
    private val teamId: Long? = (savedStateHandle["teamId"] as? String)?.toLongOrNull()?.takeIf { it > 0 }

    /** Herkunft der Karte („tm" = Transfermarkt). */
    private val quelle: String = (savedStateHandle["quelle"] as? String).orEmpty()

    private val _uiState = MutableStateFlow(
        SpielerkarteUiState(bietenMoeglich = quelle == "tm"),
    )
    val uiState: StateFlow<SpielerkarteUiState> = _uiState.asStateFlow()

    private val _bietDialog = MutableStateFlow<BietDialogState>(BietDialogState.Verborgen)
    val bietDialog: StateFlow<BietDialogState> = _bietDialog.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true)
            _uiState.value = try {
                val karte = if (teamId != null) {
                    repository.spielerKarteFremd(pid, teamId)
                } else {
                    repository.spielerKarte(pid)
                }
                _uiState.value.copy(ladend = false, karte = karte)
            } catch (e: Exception) {
                _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Spielerkarte konnte nicht geladen werden.",
                )
            }
        }
    }

    /** Öffnet den „Bieten"-Dialog: lädt die Gebots-Vorschau (`gebot.php`, nur lesend). */
    fun onBietenKlick() {
        if (_bietDialog.value != BietDialogState.Verborgen) return
        if (!_uiState.value.bietenMoeglich) return
        _bietDialog.value = BietDialogState.Laedt
        viewModelScope.launch {
            try {
                val info = serverRepository.gebotInfo(pid)
                _bietDialog.value = if (info?.submitName.isNullOrBlank()) {
                    BietDialogState.Ergebnis(
                        erfolg = false,
                        meldung = "Der Transfermarkt erlaubt für diesen Spieler kein Gebot.",
                    )
                } else {
                    BietDialogState.Bereit(info)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _bietDialog.value = BietDialogState.Ergebnis(
                    erfolg = false,
                    meldung = e.message ?: "Gebots-Informationen konnten nicht geladen werden.",
                )
            }
        }
    }

    /** Sendet das Gebot erst nachdem der Nutzer im Dialog bestätigt hat. */
    fun gebotAbgeben() {
        val aktuell = _bietDialog.value
        if (aktuell !is BietDialogState.Bereit) return
        _bietDialog.value = BietDialogState.Laedt
        viewModelScope.launch {
            try {
                val ergebnis = serverRepository.gebotAbgeben(pid)
                _bietDialog.value = BietDialogState.Ergebnis(erfolg = ergebnis.erfolg, meldung = ergebnis.meldung)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _bietDialog.value = BietDialogState.Ergebnis(
                    erfolg = false,
                    meldung = e.message ?: "Gebot konnte nicht gesendet werden.",
                )
            }
        }
    }

    fun bietDialogSchliessen() {
        _bietDialog.value = BietDialogState.Verborgen
    }
}