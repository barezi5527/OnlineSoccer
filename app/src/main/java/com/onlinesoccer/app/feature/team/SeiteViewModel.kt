package com.onlinesoccer.app.feature.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.core.state.TEAMWECHSEL_VERWORFEN
import com.onlinesoccer.app.core.state.TeamGeneration
import com.onlinesoccer.app.core.state.schreibvorgangErlaubt
import com.onlinesoccer.app.core.storage.StadionnameStore
import com.onlinesoccer.app.data.model.AktionForm
import com.onlinesoccer.app.data.model.SeitenAnsicht
import com.onlinesoccer.app.data.model.StadionnameLogik
import com.onlinesoccer.app.data.model.TrainerAktionVorschau
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
    val trainerVorschau: TrainerAktionVorschauUi? = null,
)

data class TrainerAktionVorschauUi(
    val trainerNr: String,
    val aktionName: String,
    val ladend: Boolean = false,
    val vorschau: TrainerAktionVorschau? = null,
    val fehler: String? = null,
)

@HiltViewModel
class SeiteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TeamRepository,
    private val bewerbeRepository: BewerbeRepository,
    private val dashboardRepository: DashboardRepository,
    private val stadionnameStore: StadionnameStore,
    private val teamGeneration: TeamGeneration,
) : ViewModel() {

    private var path: String = savedStateHandle["path"] ?: ""
    private val istStadionseite: Boolean = path.trimEnd('/').endsWith("osneu/stadion")

    /** Training zeigt je Spieler erst mit den Einzelwerten (`s=2`) den aktuellen Skillwert. */
    private val istTrainingsseite: Boolean = path.contains("training.php")

    /**
     * Generation, unter der die zuletzt geladene Seite geholt wurde.
     *
     * Betrifft **alle** Schreibaktionen dieses Screens (Jugendspieler berufen,
     * Jugendförderung, Jugendschranke, Trainer, Verträge, Training). Nach einem
     * Teamwechsel gehören die auf der Seite angezeigten Werte zum vorherigen Team;
     * ein POST würde sie dem neuen Team schreiben. Deshalb Gate vor dem Senden
     * (Plan T36, Muster wie `ZugabgabeViewModel`/`TaktikViewModel`).
     */
    private var geladeneGeneration = teamGeneration.current()

    private fun gate(): String? {
        if (schreibvorgangErlaubt(geladeneGeneration, teamGeneration.current())) return null
        return TEAMWECHSEL_VERWORFEN
    }

    private val _uiState = MutableStateFlow(SeiteUiState())
    val uiState: StateFlow<SeiteUiState> = _uiState.asStateFlow()
    private var trainerVorschauAnfrage = 0

    init {
        lade()
    }

    /**
     * Wechselt auf einen Unterreiter (z. B. Jugendteam `?page=2`). Der Wechsel
     * bleibt innerhalb desselben Screens und lädt die Seite unter dem neuen Pfad.
     */
    fun wechsleTab(pfad: String) {
        if (pfad == path) return
        path = pfad
        lade()
    }

    fun lade() {
        viewModelScope.launch {
            _uiState.value = SeiteUiState(ladend = true)
            geladeneGeneration = teamGeneration.current()
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
            // Die Einzelwerte (`showteam.php?s=2`) sind ein zweiter, langsamer Request.
            // Sie werden erst nach dem Anzeigen der Seite ergänzt, damit das Training
            // auch bei zäher Serverantwort sofort sichtbar ist.
            if (istTrainingsseite && _uiState.value.seite != null) {
                val angereichert = anreichereSkillwerte(_uiState.value.seite!!)
                if (angereichert != _uiState.value.seite) {
                    _uiState.value = _uiState.value.copy(seite = angereichert)
                }
            }
        }
    }

    /**
     * Ergänzt die Trainingszeilen um die Einzelwerte (`showteam.php?s=2`), damit die
     * Skill-Dropdowns je Option den aktuellen Skillwert zeigen können. Die Werte
     * werden als `skillwert:<pid>`-Eintrag an die Zeile gehängt und in
     * `TrainingFormView` ausgewertet.
     */
    private suspend fun anreichereSkillwerte(ansicht: SeitenAnsicht): SeitenAnsicht {
        val staerken = runCatching { repository.ladeStaerken() }.getOrDefault(emptyList())
        if (staerken.isEmpty()) return ansicht
        val jeSpieler = staerken.associateBy { it.pid }
        return ansicht.copy(
            abschnitte = ansicht.abschnitte.map { abschnitt ->
                abschnitt.copy(
                    zeilen = abschnitt.zeilen.map { zeile ->
                        val pid = zeile.spielerPid ?: return@map zeile
                        val werte = jeSpieler[pid]?.werte ?: return@map zeile
                        val text = werte.entries.joinToString(" ") { "${it.key} ${it.value}" }
                        if (text.isBlank()) zeile
                        else zeile.copy(werte = zeile.werte + (skillWertFeld(pid.toString()) to text))
                    },
                )
            },
        )
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
        trainerVorschauAnfrage++
        _uiState.value = _uiState.value.copy(trainerVorschau = null)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sendend = true, aktioFehler = null)
            // Gate vor dem POST: die Formularwerte stammen von der zuletzt geladenen
            // Seite. Nach einem Teamwechsel gehören sie zum vorherigen Team.
            gate()?.let { abgelehnt ->
                _uiState.value = _uiState.value.copy(sendend = false, aktioFehler = abgelehnt)
                return@launch
            }
            if (istErsterJugendBerufungsschritt(ziel, felder)) {
                val formular = repository.ladeJugendBerufungsFormular(ziel, felder)
                if (formular != null) {
                    _uiState.value = _uiState.value.copy(
                        sendend = false,
                        dialogFormular = formular,
                        aktioFehler = null,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        sendend = false,
                        aktioFehler = "Die Positions-, Namens- und Vertragsauswahl konnte nicht geladen werden. Es wurde nichts berufen.",
                    )
                }
                return@launch
            }
            val ergebnis = repository.fuehreAktionAus(ziel, felder)
            if (ergebnis.erfolg) {
                _uiState.value = _uiState.value.copy(sendend = false, dialogFormular = null)
                lade()
            } else {
                // Serverseitige Ablehnung zeigen statt die Seite still neu zu laden:
                // am 05.10.2026 hat ein wirkungsloser POST scheinbar Erfolg gemeldet.
                _uiState.value = _uiState.value.copy(
                    sendend = false,
                    aktioFehler = ergebnis.meldung
                        ?: "Aktion fehlgeschlagen – bitte anmelden und erneut versuchen.",
                )
            }
        }
    }

    /** Der erste POST öffnet nur das Serverformular, er ist noch nicht die Berufung. */
    private fun istErsterJugendBerufungsschritt(ziel: String, felder: List<Pair<String, String>>): Boolean =
        ziel.substringBefore('?').endsWith("ju.php", ignoreCase = true) &&
            felder.any { it.first == "ziehmich" } &&
            felder.any { it.first == "ziehen" }

    /** Holt den unverbindlichen Abfindungs-/Kostenhinweis der Website. */
    fun ladeTrainerVorschau(ziel: String, felder: List<Pair<String, String>>) {
        val trainerNr = felder.firstOrNull { it.first == "trainer" }?.second ?: return
        val aktionName = felder.firstOrNull { it.first == "einstellen" || it.first == "feuern" }?.first ?: return
        val anfrage = ++trainerVorschauAnfrage
        _uiState.value = _uiState.value.copy(
            trainerVorschau = TrainerAktionVorschauUi(
                trainerNr = trainerNr,
                aktionName = aktionName,
                ladend = true,
            ),
        )
        viewModelScope.launch {
            val vorschau = repository.ladeTrainerAktionVorschau(ziel, felder)
            if (anfrage != trainerVorschauAnfrage) return@launch
            _uiState.value = _uiState.value.copy(
                trainerVorschau = TrainerAktionVorschauUi(
                    trainerNr = trainerNr,
                    aktionName = aktionName,
                    vorschau = vorschau,
                    fehler = if (vorschau == null) {
                        "Der Kostenhinweis der Website konnte nicht geladen werden. Es wurde nichts ausgeführt."
                    } else {
                        null
                    },
                ),
            )
        }
    }

    fun trainerVorschauZuruecksetzen() {
        trainerVorschauAnfrage++
        _uiState.value = _uiState.value.copy(trainerVorschau = null)
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

    /** Klick auf einen Spieler (öffnet die Spielerkarte). Wird vom UI übergeben. */
    val onSpielerClick: (Long) -> Unit = { /* wird vom Screen per Parameter über Navigation gelöst */ }
}
