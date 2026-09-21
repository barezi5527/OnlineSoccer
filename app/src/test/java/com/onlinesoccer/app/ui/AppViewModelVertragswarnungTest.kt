package com.onlinesoccer.app.ui

import com.onlinesoccer.app.data.model.VertragZeile
import org.junit.Assert.assertEquals
import org.junit.Test

class AppViewModelVertragswarnungTest {

    private fun zeile(pid: Long, name: String, laufzeit: String?): VertragZeile =
        VertragZeile(pid = pid, name = name, laufzeit = laufzeit)

    @Test
    fun meldetSpielerMitHoechstensZweiZat() {
        val vertraege = listOf(
            zeile(1, "Zwei ZAT", "2"),
            zeile(2, "Ein ZAT", "1"),
            zeile(3, "Abgelaufen", "0"),
            zeile(4, "Drei ZAT", "3"),
            zeile(5, "Langer Vertrag", "23"),
        )

        val kurz = vertraegeKurzVorAuslauf(vertraege)

        assertEquals(listOf("Zwei ZAT", "Ein ZAT", "Abgelaufen"), kurz.map { it.name })
    }

    @Test
    fun ignoriertFehlendeUndNichtNumerischeLaufzeit() {
        val vertraege = listOf(
            zeile(1, "Ohne Laufzeit", null),
            zeile(2, "Leer", ""),
            zeile(3, "Text", "bald"),
            zeile(4, "Mit Leerzeichen", " 2 "),
        )

        val kurz = vertraegeKurzVorAuslauf(vertraege)

        assertEquals(listOf("Mit Leerzeichen"), kurz.map { it.name })
    }

    @Test
    fun leereListeLiefertNichts() {
        assertEquals(emptyList<VertragZeile>(), vertraegeKurzVorAuslauf(emptyList()))
    }

    @Test
    fun schwelleIstAnpassbar() {
        val vertraege = listOf(
            zeile(1, "Zwei ZAT", "2"),
            zeile(2, "Fuenf ZAT", "5"),
        )

        assertEquals(listOf("Zwei ZAT", "Fuenf ZAT"), vertraegeKurzVorAuslauf(vertraege, schwelle = 5).map { it.name })
    }
}
