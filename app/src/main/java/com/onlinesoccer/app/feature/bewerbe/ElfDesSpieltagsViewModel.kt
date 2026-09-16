package com.onlinesoccer.app.feature.bewerbe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.ElfErgebnis
import com.onlinesoccer.app.data.model.ElfKontext
import com.onlinesoccer.app.data.repository.ElfDesSpieltagsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ElfDesSpieltagsUiState(
    val ladend: Boolean = false,
    val kontextGeladen: Boolean = false,
    /** Fortschritts- bzw. Hinweistext unterhalb des Spinners/der Auswahl. */
    val ladephase: String? = null,
    /** (geprüfte Berichte, Gesamtzahl) während des Ladens. */
    val fortschritt: Pair<Int, Int>? = null,
    val fehler: String? = null,
    val zat: Int = 0,
    val zatOptionen: List<Int> = emptyList(),
    val land: String = "",
    val liga: String = "",
    val saison: Int = 0,
    val ergebnis: ElfErgebnis? = null,
)

@HiltViewModel
class ElfDesSpieltagsViewModel @Inject constructor(
    private val repository: ElfDesSpieltagsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ElfDesSpieltagsUiState())
    val uiState: StateFlow<ElfDesSpieltagsUiState> = _uiState.asStateFlow()

    private var kontext: ElfKontext? = null
    private var elfJob: Job? = null
    private var gestartet = false

    fun start() {
        if (gestartet) return
        gestartet = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, ladephase = "Kontext wird geladen …", fehler = null)
            val geladen = try {
                repository.ladeKontext()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (geladen == null) {
                _uiState.value = _uiState.value.copy(
                    ladend = false,
                    ladephase = null,
                    fehler = "Die Liga konnte nicht ermittelt werden. Bitte prüfe deine Anmeldung.",
                )
                return@launch
            }
            kontext = geladen
            _uiState.value = _uiState.value.copy(
                ladend = false,
                kontextGeladen = true,
                zat = geladen.zat,
                zatOptionen = geladen.zatOptionen,
                land = geladen.landLabel,
                liga = geladen.ligaLabel,
                saison = geladen.saison,
            )
            ermittle(geladen.zat)
        }
    }

    fun waehleZat(zat: Int) {
        if (zat <= 0 || zat == _uiState.value.zat) return
        _uiState.value = _uiState.value.copy(zat = zat, ergebnis = null, fehler = null)
        ermittle(zat)
    }

    fun erneutLaden() {
        val zat = _uiState.value.zat
        if (zat > 0) ermittle(zat)
    }

    /** Neustart der Kontext-Ermittlung (z. B. nach Anmeldefehler). */
    fun erneutVersuchen() {
        gestartet = false
        start()
    }

    private fun ermittle(zat: Int) {
        val k = kontext ?: return
        elfJob?.cancel()
        elfJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                ladend = true,
                ladephase = "⭐ Elf des Spieltags wird ermittelt …",
                fortschritt = null,
                fehler = null,
            )
            val ergebnis = try {
                repository.ermittleElf(k, zat) { geprueft, gesamt ->
                    _uiState.value = _uiState.value.copy(fortschritt = geprueft to gesamt)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }

            if (ergebnis == null) {
                _uiState.value = _uiState.value.copy(
                    ladend = false,
                    ladephase = null,
                    fortschritt = null,
                    fehler = "Die Elf konnte nicht erstellt werden.",
                )
                return@launch
            }

            val meldung = when {
                ergebnis.begegnungen == 0 ->
                    "Für diesen Spieltag liegen keine Begegnungen vor. Bitte wähle einen anderen Spieltag."
                !ergebnis.vollstaendig ->
                    "Nicht alle Spielberichte konnten geladen werden – die Elf kann unvollständig sein."
                else ->
                    "Auswertung abgeschlossen."
            }
            _uiState.value = _uiState.value.copy(
                ladend = false,
                ladephase = meldung,
                fortschritt = if (ergebnis.begegnungen > 0) ergebnis.begegnungen to ergebnis.begegnungen else null,
                ergebnis = ergebnis,
                fehler = null,
            )
        }
    }

    override fun onCleared() {
        elfJob?.cancel()
        super.onCleared()
    }
}