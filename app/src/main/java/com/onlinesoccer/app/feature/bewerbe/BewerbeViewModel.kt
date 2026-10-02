package com.onlinesoccer.app.feature.bewerbe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.data.model.LigaFilter
import com.onlinesoccer.app.data.model.LigaSpieltag
import com.onlinesoccer.app.data.model.LigaTabelle
import com.onlinesoccer.app.data.model.PokalAnsicht
import com.onlinesoccer.app.data.repository.BewerbeRepository
import com.onlinesoccer.app.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BewerbeUiState(
    val ladend: Boolean = false,
    val fehler: String? = null,
    val tabelle: LigaTabelle? = null,
    val spieltag: LigaSpieltag? = null,
    val pokal: PokalAnsicht? = null,
) {
    val geladen: Boolean get() = tabelle != null || spieltag != null || pokal != null
}

@HiltViewModel
class BewerbeViewModel @Inject constructor(
    private val repository: BewerbeRepository,
    private val dashboardRepository: DashboardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BewerbeUiState())
    val uiState: StateFlow<BewerbeUiState> = _uiState.asStateFlow()

    private var eigeneTeamId: Long? = null
    private var pokalJob: Job? = null
    private var pokalVersucht = false

    /**
     * Demo-Modus: Filter für Land/Liga werden nicht mit der eigenen Liga/Land des
     * Gast-Teams vorbelegt – der Nutzer muss Land und Liga selbst auswählen. Nur die
     * aktuelle Saison wird vorbelegt. Erst bei vollständiger Auswahl (Land + Liga)
     * wird vom Server geladen.
     */
    private var demo = false

    private var demoSaison: Int = 0

    fun setDemo(demo: Boolean) {
        this.demo = demo
    }

    /**
     * Aktive Auswahlen. Solange [`null`] wird die Server-Standardansicht
     * (eigene Liga/Land/Saison) geladen; nach dem ersten Laden werden die
     * tatsächlich angezeigten Werte aus der Server-Antwort übernommen.
     */
    private var tabelleFilter: LigaFilter? = null
    private var spieltagLiga: Int? = null
    private var spieltagLand: Int? = null
    private var spieltagSaison: Int? = null
    private var spieltagZat: Int? = null
    private var pokalLand: Int? = null
    private var pokalSaison: Int? = null
    private var pokalRunde: Int? = null

    private suspend fun eigeneTeamIdBestimmen(): Long? {
        eigeneTeamId?.let { return it }
        return try {
            dashboardRepository.fetchDashboard().teamId.also { eigeneTeamId = it }
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Ligatabelle ----------

    fun ladeTabelle(saison: Int? = null, force: Boolean = false) {
        if (demo) {
            demoLadeTabelle(saison, force)
            return
        }
        val gewuenscht = if (saison != null) {
            (tabelleFilter ?: _uiState.value.tabelle?.filter)?.copy(saison = saison)
        } else {
            tabelleFilter
        }
        ladeTabelleMitFilter(gewuenscht, force)
    }

    private fun demoLadeTabelle(saison: Int? = null, force: Boolean = false) {
        val aktuelle = _uiState.value.tabelle
        // Erstaufruf: nur Formular/Optionen laden (Server-Standard), Saison übernehmen,
        // aber keine eigene Liga/Land vorbelegen und keine Tabelle anzeigen.
        if (aktuelle == null) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
                _uiState.value = try {
                    val geladen = repository.ladeLigatabelle(filter = null, eigeneTeamId = null)
                    if (geladen == null) {
                        _uiState.value.copy(ladend = false, fehler = "Keine Tabelle für diese Auswahl verfügbar.")
                    } else {
                        demoSaison = geladen.saison
                        tabelleFilter = LigaFilter(liga = 0, land = 0, tab = 0, saison = geladen.saison)
                        _uiState.value.copy(
                            ladend = false,
                            tabelle = LigaTabelle(
                                saisonen = geladen.saisonen,
                                saison = geladen.saison,
                                ligaOptionen = geladen.ligaOptionen,
                                landOptionen = geladen.landOptionen,
                                tabOptionen = geladen.tabOptionen,
                                filter = tabelleFilter,
                            ),
                            fehler = null,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value.copy(ladend = false, fehler = e.message ?: "Tabelle konnte nicht geladen werden.")
                }
            }
            return
        }
        // Saison übernehmen, aber erst laden, wenn Land UND Liga gewählt sind.
        val basis = tabelleFilter ?: aktuelle.filter ?: return
        val gewuenscht = basis.copy(saison = saison ?: basis.saison)
        if (gewuenscht.liga <= 0 || gewuenscht.land <= 0) {
            tabelleFilter = gewuenscht
            _uiState.value = _uiState.value.copy(
                ladend = false,
                tabelle = aktuelle.copy(saison = gewuenscht.saison, filter = gewuenscht),
                fehler = null,
            )
            return
        }
        ladeTabelleMitFilter(gewuenscht, force)
    }

    fun waehleTabelleLiga(liga: Int) = tabelleFilterAendern { copy(liga = liga) }

    fun waehleTabelleLand(land: Int) = tabelleFilterAendern { copy(land = land) }

    fun waehleTabelleTab(tab: Int) = tabelleFilterAendern { copy(tab = tab) }

    private fun tabelleFilterAendern(transform: LigaFilter.() -> LigaFilter) {
        val basis = tabelleFilter ?: _uiState.value.tabelle?.filter ?: return
        val gewuenscht = basis.transform()
        if (demo && (gewuenscht.liga <= 0 || gewuenscht.land <= 0)) {
            tabelleFilter = gewuenscht
            _uiState.value = _uiState.value.copy(
                ladend = false,
                tabelle = _uiState.value.tabelle?.copy(filter = gewuenscht),
                fehler = null,
            )
            return
        }
        ladeTabelleMitFilter(gewuenscht)
    }

    private fun ladeTabelleMitFilter(gewuenscht: LigaFilter?, force: Boolean = false) {
        val aktuelle = _uiState.value.tabelle
        if (!force && aktuelle != null && aktuelle.filter == gewuenscht) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                val teamId = eigeneTeamIdBestimmen()
                val geladen = repository.ladeLigatabelle(filter = gewuenscht, eigeneTeamId = teamId)
                if (geladen == null) {
                    _uiState.value.copy(
                        ladend = false,
                        tabelle = aktuelle,
                        fehler = "Keine Tabelle für diese Auswahl verfügbar.",
                    )
                } else {
                    geladen.filter?.let { tabelleFilter = it }
                    _uiState.value.copy(ladend = false, tabelle = geladen, fehler = null)
                }
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, tabelle = aktuelle, fehler = e.message ?: "Tabelle konnte nicht geladen werden.")
            }
        }
    }

    // ---------- Spieltage ----------

    fun ladeSpieltag(zat: Int? = null, liga: Int? = null, land: Int? = null, saison: Int? = null, force: Boolean = false) {
        if (demo) {
            demoLadeSpieltag(zat, liga, land, saison, force)
            return
        }
        if (liga != null) spieltagLiga = liga
        if (land != null) spieltagLand = land
        if (saison != null) spieltagSaison = saison
        if (zat != null) spieltagZat = zat
        val aktuell = _uiState.value.spieltag
        if (!force && aktuell != null && zat == null && liga == null && land == null && saison == null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                val teamId = eigeneTeamIdBestimmen()
                val geladen = repository.ladeSpieltag(
                    zat = zat ?: spieltagZat,
                    liga = liga ?: spieltagLiga,
                    land = land ?: spieltagLand,
                    saison = saison ?: spieltagSaison,
                )?.also { s ->
                    // Auswahl aus der Server-Antwort übernehmen (Server setzt ggf. Defaults).
                    spieltagZat = s.zat.takeIf { it > 0 } ?: spieltagZat
                    spieltagLiga = s.liga.takeIf { it > 0 } ?: spieltagLiga
                    spieltagLand = s.land.takeIf { it > 0 } ?: spieltagLand
                    spieltagSaison = s.saison.takeIf { it > 0 } ?: spieltagSaison
                }?.let { s ->
                    if (teamId == null) s
                    else s.copy(spiele = s.spiele.map { spiel ->
                        val own = teamId
                        if (spiel.heimId == own || spiel.gastId == own) spiel.copy(eigenerVerein = true) else spiel
                    })
                }
                if (geladen == null) {
                    _uiState.value.copy(
                        ladend = false,
                        spieltag = aktuell,
                        fehler = "Keine Spieltage für diese Auswahl verfügbar.",
                    )
                } else {
                    _uiState.value.copy(ladend = false, spieltag = geladen, fehler = null)
                }
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, spieltag = aktuell, fehler = e.message ?: "Spieltage konnten nicht geladen werden.")
            }
        }
    }

    private fun demoLadeSpieltag(zat: Int? = null, liga: Int? = null, land: Int? = null, saison: Int? = null, force: Boolean = false) {
        val aktuelle = _uiState.value.spieltag
        // Erstaufruf: Formular/Optionen laden (Server-Standard), Saison übernehmen, aber
        // weder Liga noch Land vorbelegen und keine Spiele anzeigen.
        if (aktuelle == null) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
                _uiState.value = try {
                    val geladen = repository.ladeSpieltag()
                    if (geladen == null) {
                        _uiState.value.copy(ladend = false, fehler = "Keine Spieltage für diese Auswahl verfügbar.")
                    } else {
                        demoSaison = geladen.saison
                        spieltagLiga = 0
                        spieltagLand = 0
                        spieltagSaison = geladen.saison
                        _uiState.value.copy(
                            ladend = false,
                            spieltag = geladen.copy(
                                liga = 0,
                                land = 0,
                                saison = geladen.saison,
                                zat = 0,
                                spiele = emptyList(),
                            ),
                            fehler = null,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value.copy(ladend = false, fehler = e.message ?: "Spieltage konnten nicht geladen werden.")
                }
            }
            return
        }
        // Auswahl übernehmen, aber erst laden, wenn Land UND Liga gewählt sind.
        if (liga != null) spieltagLiga = liga
        if (land != null) spieltagLand = land
        if (saison != null) spieltagSaison = saison
        val ligaV = spieltagLiga ?: 0
        val landV = spieltagLand ?: 0
        if (ligaV <= 0 || landV <= 0) {
            spieltagZat = null
            _uiState.value = _uiState.value.copy(
                ladend = false,
                spieltag = aktuelle.copy(liga = ligaV, land = landV, saison = spieltagSaison ?: aktuelle.saison, spiele = emptyList()),
                fehler = null,
            )
            return
        }
        if (zat != null) spieltagZat = zat
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                val teamId = eigeneTeamIdBestimmen()
                val geladen = repository.ladeSpieltag(zat = spieltagZat, liga = ligaV, land = landV, saison = spieltagSaison)
                if (geladen == null) {
                    _uiState.value.copy(
                        ladend = false,
                        spieltag = aktuelle,
                        fehler = "Keine Spieltage für diese Auswahl verfügbar.",
                    )
                } else {
                    spieltagZat = geladen.zat.takeIf { it > 0 } ?: spieltagZat
                    spieltagLiga = geladen.liga.takeIf { it > 0 } ?: spieltagLiga
                    spieltagLand = geladen.land.takeIf { it > 0 } ?: spieltagLand
                    spieltagSaison = geladen.saison.takeIf { it > 0 } ?: spieltagSaison
                    val markiert = if (teamId == null) geladen
                    else geladen.copy(spiele = geladen.spiele.map { spiel ->
                        val own = teamId
                        if (spiel.heimId == own || spiel.gastId == own) spiel.copy(eigenerVerein = true) else spiel
                    })
                    _uiState.value.copy(ladend = false, spieltag = markiert, fehler = null)
                }
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, spieltag = aktuelle, fehler = e.message ?: "Spieltage konnten nicht geladen werden.")
            }
        }
    }

    fun waehleSpieltagLiga(liga: Int) {
        // Beim Ligawechsel den Spieltag zurücksetzen, damit der Server den
        // Standard der neuen Liga wählt (evtl. abweichende Spieltagsanzahl).
        spieltagZat = null
        ladeSpieltag(liga = liga)
    }

    fun waehleSpieltagLand(land: Int) {
        spieltagZat = null
        ladeSpieltag(land = land)
    }

    fun waehleSpieltagSaison(saison: Int) {
        spieltagZat = null
        ladeSpieltag(saison = saison)
    }

    // ---------- Landespokal ----------

    fun ladePokal(saison: Int? = null, runde: Int? = null, land: Int? = null, force: Boolean = false) {
        if (demo) {
            demoLadePokal(saison, runde, land, force)
            return
        }
        if (land != null) pokalLand = land
        val aktuell = _uiState.value.pokal
        if (!force && saison == null && runde == null && land == null && (aktuell != null || pokalVersucht)) return
        if (!force && aktuell != null && saison == aktuell.saison && runde == aktuell.runde &&
            (land == null || land == aktuell.land)
        ) {
            return
        }
        pokalVersucht = true
        pokalJob?.cancel()
        pokalJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                val geladen = repository.ladePokal(saison, runde, land ?: pokalLand)
                geladen.takeIf { it.land > 0 }?.let { pokalLand = it.land }
                _uiState.value.copy(ladend = false, pokal = geladen)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Pokal konnte nicht geladen werden.")
            }
        }
    }

    private fun demoLadePokal(saison: Int? = null, runde: Int? = null, land: Int? = null, force: Boolean = false) {
        val aktuelle = _uiState.value.pokal
        if (saison != null) pokalSaison = saison
        if (runde != null) pokalRunde = runde
        // Erstaufruf: Formular/Optionen laden, Saison übernehmen, aber kein Land
        // vorbelegen und keine Runden anzeigen.
        if (aktuelle == null) {
            pokalVersucht = true
            pokalJob?.cancel()
            pokalJob = viewModelScope.launch {
                _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
                _uiState.value = try {
                    val geladen = repository.ladePokal(null, null, 0)
                    if (geladen.saison > 0) demoSaison = geladen.saison
                    pokalLand = 0
                    pokalSaison = (pokalSaison ?: geladen.saison.takeIf { it > 0 }) ?: demoSaison
                    pokalRunde = 0
                    _uiState.value.copy(
                        ladend = false,
                        pokal = geladen.copy(land = 0, runde = 0, saison = pokalSaison ?: geladen.saison, runden = emptyList()),
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value.copy(ladend = false, fehler = e.message ?: "Pokal konnte nicht geladen werden.")
                }
            }
            return
        }
        if (land != null) pokalLand = land
        val landV = pokalLand ?: 0
        val gewaehlteSaison = pokalSaison ?: aktuelle.saison.takeIf { it > 0 } ?: demoSaison
        val gewaehlteRunde = pokalRunde?.coerceAtLeast(0) ?: 0
        if (landV <= 0) {
            _uiState.value = _uiState.value.copy(
                ladend = false,
                pokal = aktuelle.copy(land = 0, runde = gewaehlteRunde, runden = emptyList(), saison = gewaehlteSaison),
                fehler = null,
            )
            return
        }
        pokalJob?.cancel()
        pokalJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ladend = true, fehler = null)
            _uiState.value = try {
                val gewaehlteSaison = saison ?: aktuelle.saison.takeIf { it > 0 } ?: demoSaison
                val gewaehlteRunde = runde ?: aktuelle.runde.takeIf { it > 0 }
                val geladen = repository.ladePokal(gewaehlteSaison, gewaehlteRunde, landV)
                geladen.takeIf { it.land > 0 }?.let { pokalLand = it.land }
                _uiState.value.copy(ladend = false, pokal = geladen)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value.copy(ladend = false, fehler = e.message ?: "Pokal konnte nicht geladen werden.")
            }
        }
    }

    fun waehlePokalSaison(saison: Int) {
        val aktuell = _uiState.value.pokal
        ladePokal(saison = saison, runde = aktuell?.runde, land = pokalLand)
    }

    fun waehlePokalRunde(runde: Int) {
        val saison = _uiState.value.pokal?.saison
        ladePokal(saison = saison, runde = runde, land = pokalLand)
    }

    fun waehlePokalLand(land: Int) {
        val aktuell = _uiState.value.pokal
        ladePokal(land = land, saison = aktuell?.saison, runde = aktuell?.runde)
    }

    fun erneutLadePokal() {
        pokalVersucht = false
        ladePokal(force = true)
    }
}