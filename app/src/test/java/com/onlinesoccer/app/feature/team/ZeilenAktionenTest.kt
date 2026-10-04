package com.onlinesoccer.app.feature.team

import com.onlinesoccer.app.data.model.AktionFeldTyp
import com.onlinesoccer.app.data.model.AktionForm
import com.onlinesoccer.app.data.model.AktionZeile
import com.onlinesoccer.app.data.model.AktionsButton
import com.onlinesoccer.app.data.model.AktionsOption
import com.onlinesoccer.app.data.model.UebersichtZeile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests für die Zuordnung von Zeilen-Aktionen (⋮-Menü) – nur Parser-Output, kein Netzwerk. */
class ZeilenAktionenTest {

    @Test
    fun zeilenAktionen_matchtFormularZeilenPerSchluessel() {
        val form = AktionForm(
            ziel = "https://os.ongapo.com/training.php",
            zeilen = listOf(
                AktionZeile(schluessel = "123", bezeichnung = "A", felder = emptyList()),
            ),
        )
        val zeile = UebersichtZeile(
            ueberschrift = "A",
            aktionSchluessel = "123",
            aktionTitel = "Training bearbeiten",
        )

        val aktionen = zeilenAktionen(zeile, listOf(form))

        assertEquals(1, aktionen.size)
        assertEquals("Training bearbeiten", aktionen.first().titel)
        assertEquals(form, aktionen.first().form)
        assertEquals("123", aktionen.first().aktionZeile?.schluessel)
    }

    @Test
    fun zeilenAktionen_ohneSchluesselLiefertLeer() {
        val form = AktionForm(
            ziel = "https://os.ongapo.com/training.php",
            zeilen = listOf(AktionZeile(schluessel = "123", bezeichnung = "A", felder = emptyList())),
        )
        val zeile = UebersichtZeile(ueberschrift = "A")

        assertTrue(zeilenAktionen(zeile, listOf(form)).isEmpty())
    }

    @Test
    fun zeilenAktionen_keyMatchtTrainerPerVerstecktemFeld() {
        val form = AktionForm(
            ziel = "https://os.ongapo.com/trainer.php",
            felder = listOf(
                com.onlinesoccer.app.data.model.AktionFeld(
                    name = "trainer",
                    typ = AktionFeldTyp.VERSTECKT,
                    standard = "3",
                ),
            ),
        )
        val zeile = UebersichtZeile(
            ueberschrift = "Trainer 3",
            aktionSchluessel = "3",
            aktionTitel = "Trainer einstellen",
        )

        val aktionen = zeilenAktionen(zeile, listOf(form))

        assertEquals(1, aktionen.size)
        assertEquals("Trainer einstellen", aktionen.first().titel)
        assertEquals(form, aktionen.first().form)
    }

    @Test
    fun trainerEinstellFormularWirdAuchMitRelativemPfadUndSubmitTextErkannt() {
        val form = AktionForm(
            ziel = "trainer.php/",
            felder = listOf(
                com.onlinesoccer.app.data.model.AktionFeld("trainer", typ = AktionFeldTyp.VERSTECKT, standard = "2"),
                com.onlinesoccer.app.data.model.AktionFeld("skill"),
                com.onlinesoccer.app.data.model.AktionFeld("dauer"),
            ),
            buttons = listOf(AktionsButton(name = null, text = "Einstellen als Trainer 2")),
        )

        assertTrue(istTrainerEinstellFormular(form, form.felder))
    }

    @Test
    fun trainerFeuerFormularWirdAlsEigeneAktionErkannt() {
        val form = AktionForm(
            ziel = "https://os.ongapo.com/trainer.php",
            felder = listOf(
                com.onlinesoccer.app.data.model.AktionFeld("trainer", typ = AktionFeldTyp.VERSTECKT, standard = "1"),
            ),
            buttons = listOf(AktionsButton(name = "feuern", text = "Trainer 1 feuern")),
        )

        assertTrue(istTrainerFeuerFormular(form, form.felder))
    }

    @Test
    fun zeilenAktionen_keyValidiertJugendRadioOption() {
        val form = AktionForm(
            ziel = "https://os.ongapo.com/ju.php",
            felder = listOf(
                com.onlinesoccer.app.data.model.AktionFeld(
                    name = "ziehmich",
                    typ = AktionFeldTyp.RADIO,
                    optionen = listOf(AktionsOption("246866", "18 Jahre")),
                ),
            ),
            buttons = listOf(AktionsButton("ziehen", "Ins A-Team berufen")),
        )
        val zeile = UebersichtZeile(
            ueberschrift = "18 Jahre",
            aktionSchluessel = "246866",
            aktionTitel = "Ins A-Team berufen",
        )

        val aktionen = zeilenAktionen(zeile, listOf(form))

        assertEquals(1, aktionen.size)
        assertEquals("Ins A-Team berufen", aktionen.first().titel)
    }

    @Test
    fun zeilenAktionen_ignoriertFremdenSchluessel() {
        val form = AktionForm(
            ziel = "https://os.ongapo.com/trainer.php",
            felder = listOf(
                com.onlinesoccer.app.data.model.AktionFeld(
                    name = "trainer",
                    typ = AktionFeldTyp.VERSTECKT,
                    standard = "3",
                ),
            ),
        )
        val zeile = UebersichtZeile(ueberschrift = "Trainer 5", aktionSchluessel = "5")

        assertTrue(zeilenAktionen(zeile, listOf(form)).isEmpty())
    }
}
