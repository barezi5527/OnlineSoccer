package com.onlinesoccer.app.data.model

/** Buchstaben der festen Ersatzbank der klassischen Website (U..Z, U = Ersatztorwart). */
const val ERSATZBANK_BUCHSTABEN = "UVWXYZ"

/**
 * Ersatzbank-Index des Ersatztorwarts. Die klassische Zugabgabe-Website markiert
 * den Ersatztorwart fest auf dem Buchstaben **U** (gelbe Zelle in der Ersatzbank).
 */
const val ERSATZTORWART_BANK_INDEX = 0

/** Rang wie auf der Website (Kader/Teamansicht): 0 = Torwart … 5 = Sturm. */
enum class SpielerPosition(val rang: Int) {
    TOR(0), ABW(1), DMI(2), MIT(3), OMI(4), STU(5), AMATEUR(6),
}

sealed class AufstellungSlot {
    /** Beta-Grid-Slot (aus `moveFieldClone`): zeile 0..14 (0 = Sturm/gegnerisches Tor oben), spalte 0..10 (0 = links). */
    data class Feld(val zeile: Int, val spalte: Int) : AufstellungSlot()

    /** Ersatzbank-Platz; index 0 = Ersatztorwart (U), 1..5 = Feld-Ersatz (V..Z). */
    data class Ersatz(val index: Int) : AufstellungSlot()

    /** Aktiver Torwart (Slot unterhalb des Feldes). */
    object Torwart : AufstellungSlot()
}

data class RasterPosition(
    val zeile: Int,
    val spalte: Int,
)

/** Von der Website ermittelte Werte der aktuell aufgestellten Mannschaft. */
data class AufstellungsWerte(
    val optiSkill: String? = null,
    val skillSchnitt: String? = null,
    val fitness: String? = null,
    val moral: String? = null,
) {
    val istLeer: Boolean get() = optiSkill == null && skillSchnitt == null && fitness == null && moral == null
}

data class AufstellungSpieler(
    val pid: Long,
    val name: String,
    val nummer: String,
    val alter: Int,
    val skill: Double,
    val opti: Double,
    val fit: Int,
    val mor: Int,
    val position: SpielerPosition,
    val slot: AufstellungSlot?,
    /** Raster-Slot der klassischen Seite (`ra[pid]`): A–L Feld, T Torwart, U–Z Bank. */
    val raSlot: String? = null,
)

data class Aufstellung(
    val zat: Int?,
    val spielart: String?,
    val gegner: String?,
    val status: String?,
    val spieler: List<AufstellungSpieler>,
    /** Aus der klassischen Seite `raster1` (Taktikauswahl: eigene + Standard-Taktiken). */
    val taktiken: List<AuswahlOption> = emptyList(),
    /** Raster-Slot-Optionen der klassischen Seite (aus dem ersten `ra[pid]`-Select). */
    val kaderSlots: List<AuswahlOption> = emptyList(),
    /** Echte Buchstaben-Zuordnung der klassischen Website zum Beta-Raster. */
    val rasterPositionen: Map<String, RasterPosition> = emptyMap(),
    val aufstellungsWerte: AufstellungsWerte? = null,
    /** Wählbare ZATs für „Laden aus ZAT" (aus dem `lauf`-Select). */
    val zatOptionen: List<AuswahlOption> = emptyList(),
) {
    val torwart: AufstellungSpieler?
        get() = spieler.firstOrNull { it.slot == AufstellungSlot.Torwart }

    val feldspieler: List<AufstellungSpieler>
        get() = spieler.filterIsInstance<AufstellungSpieler>()
            .filter { it.slot is AufstellungSlot.Feld }
            .sortedBy { (it.slot as AufstellungSlot.Feld).zeile }

    val bank: List<AufstellungSpieler>
        get() = spieler.filter { it.slot is AufstellungSlot.Ersatz }
            .sortedBy { (it.slot as AufstellungSlot.Ersatz).index }

    /** Spieler auf einem Ersatzbank-Platz (Index 0=U … 5=Z). Erkennt den Platz über slot oder raSlot. */
    fun spielerAufBankSlot(index: Int): AufstellungSpieler? {
        if (index !in ERSATZBANK_BUCHSTABEN.indices) return null
        val buchstabe = ERSATZBANK_BUCHSTABEN[index].toString()
        return spieler.firstOrNull {
            (it.slot as? AufstellungSlot.Ersatz)?.index == index || it.raSlot == buchstabe
        }
    }
}

/** Tauscht zwei Spieler (Slot + raSlot), damit Position und Buchstabe konsistent bleiben. */
fun List<AufstellungSpieler>.tauschePlaetze(pidA: Long, pidB: Long): List<AufstellungSpieler> {
    val a = firstOrNull { it.pid == pidA } ?: return this
    val b = firstOrNull { it.pid == pidB } ?: return this
    return map { spieler ->
        when (spieler.pid) {
            pidA -> spieler.copy(slot = b.slot, raSlot = b.raSlot)
            pidB -> spieler.copy(slot = a.slot, raSlot = a.raSlot)
            else -> spieler
        }
    }
}
