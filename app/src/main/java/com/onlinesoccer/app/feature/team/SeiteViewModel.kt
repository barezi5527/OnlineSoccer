package com.onlinesoccer.app.feature.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.core.storage.StadionnameStore
import com.onlinesoccer.app.data.model.AktionForm
import com.onlinesoccer.app.data.model.SeitenAnsicht
import com.onlinesoccer.app.data.model.StadionnameLogik
import com.onlinesoccer.app.data.repository.BewerbeRepository
import com.onlinesoccer.app.data.repository.DashboardRepository
import com.onlinesoccer.app.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StadionnameUiState(
    val teamId: Long? = null,
    val serverName: String? = null,
    val eingabe: String = "",
    val gespeicherterName: String = "",
    val ladeFehler: String? = null,
    val eingabeFehler: String? = null,
    val speichernd: Boolean = false,
    val meldung: String? = null,
) {
    val geaendert: Boolean
        get() = eingabe.trim() != gespeicherterName.trim()

    val kannSpeichern: Boolean
        get() = teamId != null && !speichernd && geaendert && eingabeFehler == null
}

data class SeiteUiState(
    val ladend: Boolean = true,
    val fehler: String? = null,
    val seite: SeitenAnsicht? = null,
    val sendend: Boolean = false,
    val aktioFehler: String? = null,
    /** In einer Zwischenansicht geladenes Formular (z. B. Scouting-Gebot). */
    val dialogFormular: AktionForm? = null,
    /** Aktuelle Saison (Server-Standard) – steuert den Rasenmuster-Wechsel. */
    val saison: Int = 0,
    val stadionname: StadionnameUiState? = null,
)

@HiltViewModel
class SeiteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TeamRepository,
    private val bewerbeRepository: BewerbeRepository,
    private val dashboardRepository: DashboardRepository,
    private val stadionnameStore: StadionnameStore,
) : ViewModel() {

    private val path: String = savedStateHandle["path"] ?: ""
    private val istStadionseite: Boolean = path.trimEnd('/').endsWith("osneu/stadion")

    private val _uiState = MutableStateFlow(SeiteUiState())
    val uiState: StateFlow<SeiteUiState> = _uiState.asStateFlow()

    init {
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = SeiteUiState(ladend = true)
            val saison = ladeAktuelleSaison()
            _uiState.value = try {
                val ansicht = repository.ladeSeite(path)
                if (ansicht == null) {
                    SeiteUiState(
                        ladend = false,
                        fehler = "Seite nicht verfügbar – bitte anmelden und erneut versuchen.",
                        saison = saison,
                    )
                } else {
                    val stadionname = if (istStadionseite) ladeStadionname() else null
                    SeiteUiState(
                        ladend = false,
                        seite = ansicht,
                        saison = saison,
                        stadionname = stadionname,
                    )
                }
            } catch (e: Exception) {
                SeiteUiState(ladend = false, fehler = e.message ?: "Seite konnte nicht geladen werden.", saison = saison)
            }
        }
    }

    /** Aktuelle Saison des Servers (Standard ohne Filter); 0, wenn sie unbekannt bleibt. */
    private suspend fun ladeAktuelleSaison(): Int =
        runCatching { bewerbeRepository.ladeSpieltag()?.saison ?: 0 }.getOrDefault(0)

    private suspend fun ladeStadionname(): StadionnameUiState {
        val teamId = runCatching {
            dashboardRepository.fetchDashboard(forceRefresh = true).teamId?.takeIf { it > 0L }
        }.getOrNull()
        val serverName = runCatching { repository.ladeTeaminfo().stadionname }.getOrNull()
        val gespeichert = teamId?.let {
            runCatching { stadionnameStore.lesen(it) }.getOrNull()
        }
        return StadionnameUiState(
            teamId = teamId,
            serverName = serverName,
            eingabe = gespeichert?.name.orEmpty(),
            gespeicherterName = gespeichert?.name.orEmpty(),
            ladeFehler = if (teamId == null) {
                "Der eigene Verein konnte nicht bestimmt werden. Bitte erneut laden."
            } else {
                null
            },
        )
    }

    fun stadionnameGeaendert(wert: String) {
        val aktuellerStand = _uiState.value
        if (aktuellerStand.seite == null) return
        val stadionname = aktuellerStand.stadionname ?: return
        _uiState.value = aktuellerStand.copy(
            stadionname = stadionname.copy(
                eingabe = wert,
                eingabeFehler = StadionnameLogik.fehler(wert),
                meldung = null,
            ),
        )
    }

    fun stadionnameSpeichern() {
        val aktuellerStand = _uiState.value
        if (aktuellerStand.seite == null) return
        val stadionname = aktuellerStand.stadionname ?: return
        val teamId = stadionname.teamId ?: return
        if (!stadionname.geaendert) return
        val name = StadionnameLogik.normalisiere(stadionname.eingabe)
        val eingabeFehler = StadionnameLogik.fehler(name)
        if (eingabeFehler != null) {
            _uiState.value = aktuellerStand.copy(
                stadionname = stadionname.copy(eingabeFehler = eingabeFehler, meldung = null),
            )
            return
        }

        viewModelScope.launch {
            setStadionname { it.copy(speichernd = true, eingabeFehler = null, meldung = null) }
            val gespeichert = if (name.isEmpty()) {
                stadionnameStore.loeschen(teamId)
            } else {
                stadionnameStore.speichern(teamId, name)
            }
            if (gespeichert) {
                setStadionname {
                    it.copy(
                        eingabe = name,
                        gespeicherterName = name,
                        speichernd = false,
                        meldung = if (name.isEmpty()) {
                            "Servername wird verwendet."
                        } else {
                            "Anzeigename gespeichert."
                        },
                    )
                }
            } else {
                setStadionname {
                    it.copy(
                        speichernd = false,
                        eingabeFehler = "Der Anzeigename konnte nicht gespeichert werden.",
                    )
                }
            }
        }
    }

    fun servernamenVerwenden() {
        val aktuellerStand = _uiState.value
        if (aktuellerStand.seite == null) return
        val stadionname = aktuellerStand.stadionname ?: return
        if (stadionname.teamId == null || !stadionname.geaendert) return
        _uiState.value = aktuellerStand.copy(
            stadionname = stadionname.copy(eingabe = "", eingabeFehler = null, meldung = null),
        )
        stadionnameSpeichern()
    }

    private fun setStadionname(transform: (StadionnameUiState) -> StadionnameUiState) {
        val aktuellerStand = _uiState.value
        val stadionname = aktuellerStand.stadionname ?: return
        _uiState.value = aktuellerStand.copy(stadionname = transform(stadionname))
    }

    /** Sendet ein ausgefülltes Formular und lädt die Seite danach frisch. */
    fun sendeAktion(ziel: String, felder: List<Pair<String, String>>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sendend = true, aktioFehler = null)
            val ok = repository.fuehreAktionAus(ziel, felder)
            if (ok) {
                _uiState.value = _uiState.value.copy(sendend = false, dialogFormular = null)
                lade()
            } else {
                _uiState.value = _uiState.value.copy(
                    sendend = false,
                    aktioFehler = "Aktion fehlgeschlagen – bitte anmelden und erneut versuchen.",
                )
            }
        }
    }

    /** Öffnet ein separates Formular (z. B. das Scouting-Gebot einer Angebotszeile). */
    fun ladeFormular(ziel: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(dialogFormular = repository.ladeAktionFormular(ziel))
        }
    }

    fun schliesseDialog() {
        _uiState.value = _uiState.value.copy(dialogFormular = null)
    }
}