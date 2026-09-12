package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.TransferDetail
import com.onlinesoccer.app.data.model.TransferStatus
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressionstests für die „Transferstatus"-Seite (`tstatus.php`) gegen echte
 * Server-Dumps (`app/src/test/resources/dumps/tstatus*.html`).
 */
class TransferStatusRepositoryParseTest {

    private val repo = ServerRepository(okhttp3.OkHttpClient())

    private fun dump(name: String): String =
        File("src/test/resources/dumps/$name.html").readText()

    @Test
    fun transferStatus_parstAlleSpielerMitStatusMoralFitnessSkillUndOpti() {
        val ergebnis = repo.parseTransferStatus(dump("tstatus"))

        assertTrue("alle Kaderspieler", ergebnis.zeilen.size >= 30)

        val heiko = ergebnis.zeilen.first { it.name == "Heiko Kehrer" }
        assertEquals(104203L, heiko.spielerId)
        assertEquals("98", heiko.mor)
        assertEquals("99", heiko.fit)
        assertEquals("36.65", heiko.skillSchnitt)
        assertEquals("55.22", heiko.optSkill)
        assertEquals(TransferStatus.U, heiko.status)

        val egnatius = ergebnis.zeilen.first { it.name == "Egnatius Dajan" }
        assertEquals(161985L, egnatius.spielerId)
        assertEquals(TransferStatus.N, egnatius.status)
    }

    @Test
    fun transferStatus_liestMindestabloeseTransfertextUndDetails() {
        val ergebnis = repo.parseTransferStatus(dump("tstatus"))

        ergebnis.zeilen.forEach { zeile ->
            assertTrue("Mindestablöse vorhanden", zeile.mindestabloese.isNotBlank())
            assertEquals("keine gesetzten Details", emptySet<TransferDetail>(), zeile.details)
        }
    }

    @Test
    fun transferStatus_alleStatuswerteWurdenKorrektZuordnet() {
        val ergebnis = repo.parseTransferStatus(dump("tstatus"))

        val nachStatus = ergebnis.zeilen.groupBy { it.status }
        assertTrue("nur N und U im Dump", nachStatus.keys.all { it == TransferStatus.N || it == TransferStatus.U })
        assertTrue("U-Spieler vorhanden", (nachStatus[TransferStatus.U]?.size ?: 0) >= 20)
        assertTrue("N-Spieler vorhanden", (nachStatus[TransferStatus.N]?.size ?: 0) >= 5)
    }

    @Test
    fun transferStatus_ignoriertKopfzeileOhneSpielerLink() {
        val ergebnis = repo.parseTransferStatus(dump("tstatus"))

        assertTrue("keine Zeile ohne Spieler-Link", ergebnis.zeilen.none { it.spielerId == 0L || it.name.isBlank() })
    }

    @Test
    fun kontrollabfrage_wirdErkannt() {
        assertTrue("Kontrollabfrage erkannt", repo.istKontrollabfrage(dump("tstatus_kontrollabfrage")))
        assertFalse("normale Formularseite ist keine Kontrollabfrage", repo.istKontrollabfrage(dump("tstatus")))
    }

    @Test
    fun kontrollabfrage_enthaeltPruefungsfelder() {
        val html = dump("tstatus_kontrollabfrage")

        assertTrue("change-Felder vorhanden", html.contains("change[104203]"))
        assertTrue("doit-Submit vorhanden", html.contains("name=\"doit\""))
    }

    @Test
    fun parseTransferStatusAntwort_uebernahmeWirdAnhandDerStatuswerteBestaetigt() {
        val erwartet = listOf(
            repo.parseTransferStatus(dump("tstatus")).zeilen.first { it.spielerId == 104203L }.mitStatus(TransferStatus.N),
        )
        // Kontrollabfrage-Seite selbst ist noch keine Übernahme → nicht eindeutig.
        val kontroll = repo.parseTransferStatusAntwort(dump("tstatus_kontrollabfrage"), erwartet)
        assertFalse(kontroll.erfolg)
    }

    @Test
    fun parseTransferStatusAntwort_erkenntFehlerhinweis() {
        val fehler = repo.parseTransferStatusAntwort(
            "<html><body>Die Änderung ist nicht möglich, da der Spieler erst vor Kurzem auf die Transferliste gesetzt wurde.</body></html>",
            emptyList(),
        )

        assertFalse(fehler.erfolg)
        assertTrue(fehler.meldung.contains("nicht möglich"))
    }

    @Test
    fun parseTransferStatusAntwort_erkenntKonsistenteUebernahme() {
        val dumpZeilen = repo.parseTransferStatus(dump("tstatus")).zeilen
        val erwartet = dumpZeilen.map { zeile ->
            if (zeile.spielerId == 104203L) zeile.mitStatus(TransferStatus.N) else zeile
        }
        // Eine Antwort, die den geänderten Stand (104203 = Normal) spiegelt.
        val geaendertesHtml = dump("tstatus")
            .replace(
                "name=\"tstatus[104203]\" value=\"3\" CHECKED",
                "name=\"tstatus[104203]\" value=\"0\" CHECKED",
            )

        val antwort = repo.parseTransferStatusAntwort(geaendertesHtml, erwartet)
        assertTrue(antwort.erfolg)
        assertTrue(antwort.meldung.contains("gespeichert"))
    }
}