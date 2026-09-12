package com.onlinesoccer.app.feature.zat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.ZugabgabeElementEintrag
import com.onlinesoccer.app.data.model.ZugabgabeElementTyp
import com.onlinesoccer.app.data.model.ZugabgabeFormular
import com.onlinesoccer.app.data.model.ZugabgabeKopfinfo
import com.onlinesoccer.app.data.repository.ZugabgabeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ZatEditorUiState(
    val ladend: Boolean = false,
    val kopfinfo: ZugabgabeKopfinfo? = null,
    val elemente: List<ZugabgabeElementEintrag> = emptyList(),
    val loeschLabel: String? = null,
    val typ: ZugabgabeElementTyp? = null,
    val formular: ZugabgabeFormular? = null,
    val werte: Map<String, String> = emptyMap(),
    val auswahl: Set<String> = emptySet(),
    val speichernd: Boolean = false,
    val meldung: String? = null,
    val fehler: String? = null,
)

/** Gemeinsame Logik für die Tabs „Aktionen“ (p=1) und „Einstellungen“ (p=2). */
abstract class ZugabgabeElementeViewModel(
    private val repository: ZugabgabeRepository,
    protected val gruppe: Int,
) : ViewModel() {

    protected abstract val typen: List<ZugabgabeElementTyp>

    private val _uiState = MutableStateFlow(ZatEditorUiState())
    val uiState: StateFlow<ZatEditorUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            try {
                val seite = repository.ladeElemente(gruppe)
                _uiState.value = _uiState.value.copy(
                    ladend = false,
                    kopfinfo = seite.kopfinfo,
                    elemente = seite.elemente,
                    loeschLabel = seite.loeschLabel,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    ladend = false,
                    fehler = e.message ?: "Seite konnte nicht geladen werden.",
                )
            }
        }
    }

    fun waehleTyp(typ: ZugabgabeElementTyp) {
        if (typ == _uiState.value.typ) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(typ = typ, werte = emptyMap(), fehler = null)
            if (typ.gruppe != gruppe) return@launch
            try {
                val formular = repository.ladeFormular(typ)
                _uiState.value = _uiState.value.copy(formular = formular)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(fehler = e.message ?: "Formular konnte nicht geladen werden.")
            }
        }
    }

    fun setWert(feld: String, wert: String) {
        _uiState.value = _uiState.value.copy(werte = _uiState.value.werte + (feld to wert))
        _uiState.value = _uiState.value.copy(meldung = null)
    }

    /** Legt die ausgefüllte Aktion/Einstellung serverseitig an. */
    fun spieleAnlegen() {
        val typ = _uiState.value.typ
        val zwingend = _uiState.value.formular
        if (typ == null) return
        if (zwingend == null || !werteVollstaendig(zwingend)) {
            _uiState.value = _uiState.value.copy(
                fehler = "Bitte alle Pflichtfelder ausfüllen.",
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(speichernd = true, meldung = null, fehler = null)
            try {
                val meldung = repository.elementSpeichern(typ, _uiState.value.werte)
                _uiState.value = _uiState.value.copy(
                    speichernd = false,
                    meldung = meldung,
                    typ = null,
                    formular = null,
                    werte = emptyMap(),
                )
                lade()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    speichernd = false,
                    fehler = e.message ?: "Anlegen fehlgeschlagen.",
                )
            }
        }
    }

    fun toggleAuswahl(relaId: String) {
        val auswahl = _uiState.value.auswahl.toMutableSet()
        if (!auswahl.add(relaId)) auswahl.remove(relaId)
        _uiState.value = _uiState.value.copy(auswahl = auswahl)
    }

    /** Löscht die markierten Elemente. */
    fun loescheMarkierte() {
        val auswahl = _uiState.value.auswahl
        val label = _uiState.value.loeschLabel
        if (auswahl.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(speichernd = true, meldung = null, fehler = null)
            try {
                val meldung = repository.elementeLoeschen(gruppe, auswahl.toList(), label)
                _uiState.value = _uiState.value.copy(
                    speichernd = false,
                    meldung = meldung,
                    auswahl = emptySet(),
                )
                lade()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    speichernd = false,
                    fehler = e.message ?: "Löschen fehlgeschlagen.",
                )
            }
        }
    }

    private fun werteVollstaendig(formular: ZugabgabeFormular): Boolean {
        val werte = _uiState.value.werte
        val pflicht = when (formular.typ) {
            ZugabgabeElementTyp.EINWECHSLUNG -> listOf("zao_einspieler", "zao_spieler", "zao_minute", "zao_abhaengigkeit", "P1", "P2")
            ZugabgabeElementTyp.EINSATZ,
            ZugabgabeElementTyp.HAERTE,
            ZugabgabeElementTyp.SPIELWEISE,
            ZugabgabeElementTyp.TAKTIK_ABWEHR,
            ZugabgabeElementTyp.TAKTIK_MITTELFELD,
            ZugabgabeElementTyp.TAKTIK_STURM,
            -> listOf("zao_minute", "zao_abhaengigkeit", "P1")
            ZugabgabeElementTyp.POSITIONSWECHSEL -> listOf("zao_spieler", "P1", "P2", "zao_minute", "zao_abhaengigkeit")
            ZugabgabeElementTyp.MANNDECKUNG -> listOf("zao_spieler", "P1", "zao_minute", "zao_abhaengigkeit")
            ZugabgabeElementTyp.KAPITAEN,
            ZugabgabeElementTyp.SPIELMACHER,
            ZugabgabeElementTyp.ELFMETERSCHUETZE,
            ZugabgabeElementTyp.FREISTOSS_DIREKT,
            ZugabgabeElementTyp.FREISTOSS_INDIREKT,
            ZugabgabeElementTyp.ECKE,
            ZugabgabeElementTyp.LIBERO,
            -> listOf("spieler_id")
        }
        return pflicht.all { werte[it].orEmpty().isNotBlank() }
    }
}

@HiltViewModel
class AktionenViewModel @Inject constructor(
    repository: ZugabgabeRepository,
) : ZugabgabeElementeViewModel(repository, gruppe = 1) {
    override val typen: List<ZugabgabeElementTyp> = ZugabgabeElementTyp.aktionen
}

@HiltViewModel
class EinstellungenViewModel @Inject constructor(
    repository: ZugabgabeRepository,
) : ZugabgabeElementeViewModel(repository, gruppe = 2) {
    override val typen: List<ZugabgabeElementTyp> = ZugabgabeElementTyp.einstellungen
}