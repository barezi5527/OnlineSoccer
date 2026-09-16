package com.onlinesoccer.app.data.repository

import android.content.Context
import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.data.model.ElfErgebnis
import com.onlinesoccer.app.data.model.ElfKontext
import com.onlinesoccer.app.data.model.ElfSpieler
import com.onlinesoccer.app.data.model.SpielerPosition
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Elf des Spieltags: ermittelt die eingeloggte Liga dynamisch über die
 * Server-Standardansicht (`ls.php`), lädt alle Spielberichte des gewählten
 * Spieltags, wertet sie lokal aus und cached das Ergebnis je
 * Server/Liga/Saison/Spieltag (plus Nutzer, falls Team-ID bekannt).
 *
 * Es gibt keinen zentralen Server für die Elf – alles läuft lokal im Gerät.
 */
@Singleton
class ElfDesSpieltagsRepository @Inject constructor(
    private val bewerbeRepository: BewerbeRepository,
    private val berichtRepository: BerichtRepository,
    private val dashboardRepository: DashboardRepository,
    @ApplicationContext private val context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Lädt den Kontext des angemeldeten Benutzers: eigene Liga/Land/Saison,
     * Standard-Spieltag und die wählbaren Spieltage aus der Server-Standard-
     * ansicht. Liefert `null`, wenn die Liga nicht ermittelbar ist (z. B. kein
     * Login, Server nicht erreichbar).
     */
    suspend fun ladeKontext(): ElfKontext? = withContext(Dispatchers.IO) {
        val spieltag = bewerbeRepository.ladeSpieltag() ?: return@withContext null
        if (spieltag.liga <= 0 || spieltag.land <= 0 || spieltag.zat <= 0) {
            return@withContext null
        }

        val teamId = ladeEigeneTeamId()

        ElfKontext(
            teamId = teamId,
            landId = spieltag.land,
            ligaId = spieltag.liga,
            saison = spieltag.saison,
            landLabel = spieltag.landOptionen.firstOrNull { it.wert == spieltag.land }?.label ?: "Land",
            ligaLabel = spieltag.ligaOptionen.firstOrNull { it.wert == spieltag.liga }?.label ?: "Liga",
            saisonLabel = spieltag.saisonOptionen.firstOrNull { it.wert == spieltag.saison }?.label,
            zat = spieltag.zat,
            zatOptionen = spieltag.zatOptionen,
        )
    }

    /**
     * Ermittelt die Elf des Spieltags. Erst wird der lokale Cache geprüft;
     * fehlt dieser, werden alle Spielberichte des Spieltags geladen, ausgewertet
     * und das Ergebnis gespeichert.
     *
     * @param onFortschritt wird nach jedem geprüften Bericht mit (geprüft, gesamt)
     *   aufgerufen; bei 0 Begegnungen bleibt der Aufruf aus.
     */
    suspend fun ermittleElf(
        kontext: ElfKontext,
        zat: Int,
        onFortschritt: (geprueft: Int, gesamt: Int) -> Unit = { _, _ -> },
    ): ElfErgebnis = withContext(Dispatchers.IO) {
        val cacheSchluessel = ElfCache.schluessel(kontext.teamId, kontext.ligaId, kontext.landId, kontext.saison, zat)
        prefs.getString(cacheSchluessel, null)?.let { gespeichert ->
            ElfCache.deserialisieren(gespeichert)?.let { return@withContext it }
        }

        val leeres = ElfErgebnis(
            land = kontext.landLabel,
            liga = kontext.ligaLabel,
            saison = kontext.saison,
            spieltag = zat,
        )

        val spieltag = bewerbeRepository.ladeSpieltag(
            zat = zat,
            liga = kontext.ligaId,
            land = kontext.landId,
            saison = kontext.saison,
        ) ?: return@withContext leeres

        val spiele = spieltag.spiele.filter { it.gespielt && it.berichtUrl != null }
        val gesamt = spiele.size

        val kandidaten = mutableListOf<ElfKandidat>()
        var erfolgreich = 0

        spiele.forEachIndexed { index, spiel ->
            onFortschritt(index + 1, gesamt)
            val bericht = berichtRepository.ladeBericht(sid = null, url = spiel.berichtUrl)
            if (bericht != null) {
                erfolgreich++
                kandidaten += ElfAuswertung.kandidatenAusBericht(bericht)
            }
        }

        val aufstellung = ElfAuswahl.erstelleElf(kandidaten)
        val ergebnis = ElfErgebnis(
            land = kontext.landLabel,
            liga = kontext.ligaLabel,
            saison = kontext.saison,
            spieltag = zat,
            spieler = aufstellung.spieler,
            formation = aufstellung.formation,
            begegnungen = gesamt,
            berichteErfolgreich = erfolgreich,
            vollstaendig = gesamt > 0 && erfolgreich == gesamt && aufstellung.vollstaendig,
        )

        if (erfolgreich > 0) {
            prefs.edit().putString(cacheSchluessel, ElfCache.serialisieren(ergebnis)).apply()
        }
        ergebnis
    }

    /** Leert den Cache (alle Server/Nutzer/Ligen). */
    fun leereCache() {
        prefs.edit().clear().apply()
    }

    private suspend fun ladeEigeneTeamId(): Long? = try {
        dashboardRepository.fetchDashboard().teamId?.toLong()
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val PREFS_NAME = "elf_des_spieltags_cache"
    }
}

/**
 * Persistenz der Elf-Ergebnisse ohne zusätzliche Bibliothek: deterministische
 * Klartext-Serialisierung mit Kontrollzeichen (kein JSON-Parser nötig).
 * Trennzeichen können in Vereins-/Spielernamen nicht vorkommen.
 */
object ElfCache {

    private const val FELD = '\u241F'
    private const val SPIELER = '\u241E'
    private const val PREFIX = "ElfDesSpieltags|v6"
    /** Anzahl gespeicherter Felder je Spieler (inkl. Bewertungs-Eingaben, teamId, kapitän). */
    private const val SPIELER_FELDER = 25

    /** Eindeutiger Cache-Schlüssel je Server, Nutzer (falls bekannt), Liga, Land, Saison, Spieltag. */
    fun schluessel(teamId: Long?, ligaId: Int, landId: Int, saison: Int, zat: Int): String {
        val server = OsApi.HOST
        return buildString {
            append(server)
            append('|')
            append(teamId?.toString() ?: "-")
            append('|')
            append(ligaId)
            append('|')
            append(landId)
            append('|')
            append(saison)
            append('|')
            append(zat)
        }
    }

    fun serialisieren(ergebnis: ElfErgebnis): String {
        val meta = listOf(
            PREFIX,
            ergebnis.land,
            ergebnis.liga,
            ergebnis.saison.toString(),
            ergebnis.spieltag.toString(),
            ergebnis.formation.orEmpty(),
            ergebnis.begegnungen.toString(),
            ergebnis.berichteErfolgreich.toString(),
            ergebnis.vollstaendig.toString(),
        ).joinToString(FELD.toString())

        val spieler = ergebnis.spieler.joinToString(SPIELER.toString()) { spieler ->
            listOf(
                spieler.name,
                spieler.verein,
                spieler.position.name,
                spieler.bewertung.toString(),
                spieler.teamId?.toString().orEmpty(),
                spieler.spielerId?.toString().orEmpty(),
                spieler.startelf.toString(),
                spieler.minuten?.toString().orEmpty(),
                spieler.tore.toString(),
                spieler.vorlagen.toString(),
                spieler.gelbeKarten.toString(),
                spieler.roteKarten.toString(),
                spieler.sieg.toString(),
                spieler.unentschieden.toString(),
                spieler.niederlage.toString(),
                spieler.gegenTore.toString(),
                spieler.hatErgebnis.toString(),
                spieler.elfmeter.toString(),
                spieler.zweikaempfe.toString(),
                spieler.zweikampfQuote.toString(),
                spieler.schuesse.toString(),
                spieler.aufsTor.toString(),
                spieler.auffaelligkeit.toString(),
                spieler.gehalteneBalle?.toString().orEmpty(),
                spieler.kapitän.toString(),
            ).joinToString(FELD.toString())
        }
        return meta + SPIELER + spieler
    }

    fun deserialisieren(gespeichert: String): ElfErgebnis? {
        val teile = gespeichert.split(SPIELER.toString(), limit = 2)
        val meta = teile.getOrNull(0) ?: return null
        val felder = meta.split(FELD.toString())
        if (felder.size < 9 || felder[0] != PREFIX) return null

        val saison = felder[3].toIntOrNull() ?: return null
        val spieltag = felder[4].toIntOrNull() ?: return null
        val formation = felder[5].takeIf { it.isNotBlank() }
        val begegnungen = felder[6].toIntOrNull() ?: 0
        val erfolgreich = felder[7].toIntOrNull() ?: 0
        val vollstaendig = felder[8].toBoolean()

        val spieler = teile.getOrNull(1)
            ?.split(SPIELER.toString())
            ?.mapNotNull { block -> deserialisiereSpieler(block) }
            ?: emptyList()

        if (spieler.any { it.name.isBlank() || it.verein.isBlank() }) return null

        return ElfErgebnis(
            land = felder[1],
            liga = felder[2],
            saison = saison,
            spieltag = spieltag,
            spieler = spieler,
            formation = formation,
            begegnungen = begegnungen,
            berichteErfolgreich = erfolgreich,
            vollstaendig = vollstaendig,
        )
    }

    private fun deserialisiereSpieler(block: String): ElfSpieler? {
        val felder = block.split(FELD.toString())
        if (felder.size != SPIELER_FELDER) return null
        val name = felder[0]
        val verein = felder[1]
        val position = SpielerPosition.entries.firstOrNull { it.name == felder[2] } ?: return null
        val bewertung = felder[3].toDoubleOrNull() ?: return null
        return ElfSpieler(
            name = name,
            verein = verein,
            position = position,
            bewertung = bewertung,
            teamId = felder[4].toLongOrNull(),
            spielerId = felder[5].toLongOrNull(),
            startelf = felder[6].toBooleanStrictOrNull() ?: true,
            minuten = felder[7].toIntOrNull(),
            tore = felder[8].toIntOrNull() ?: 0,
            vorlagen = felder[9].toIntOrNull() ?: 0,
            gelbeKarten = felder[10].toIntOrNull() ?: 0,
            roteKarten = felder[11].toIntOrNull() ?: 0,
            sieg = felder[12].toBooleanStrictOrNull() ?: false,
            unentschieden = felder[13].toBooleanStrictOrNull() ?: false,
            niederlage = felder[14].toBooleanStrictOrNull() ?: false,
            gegenTore = felder[15].toIntOrNull() ?: 0,
            hatErgebnis = felder[16].toBooleanStrictOrNull() ?: true,
            elfmeter = felder[17].toBooleanStrictOrNull() ?: false,
            zweikaempfe = felder[18].toIntOrNull() ?: 0,
            zweikampfQuote = felder[19].toDoubleOrNull() ?: 0.0,
            schuesse = felder[20].toIntOrNull() ?: 0,
            aufsTor = felder[21].toIntOrNull() ?: 0,
            auffaelligkeit = felder[22].toIntOrNull() ?: 0,
            gehalteneBalle = felder[23].toIntOrNull(),
            kapitän = felder[24].toBooleanStrictOrNull() ?: false,
        )
    }
}