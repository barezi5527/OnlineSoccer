package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.AuswahlOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaktikRepositoryParseTest {
    private val repository = TaktikRepository(okhttp3.OkHttpClient())

    @Test
    fun editorRasterUndGespeicherteTaktikenWerdenGelesen() {
        val taktik = repository.parse(
            """
            <form>
              <input type="checkbox" name="taktik[]" value="O1" checked="checked">
              <input type="checkbox" name="taktik[]" value="A6" checked="checked">
              <input type="checkbox" name="taktik[]" value="O10">
              <input name="speichername" value="Mein 4-4-2">
              <select name="raster1">
                <option value="0" selected="">Standardtaktiken</option>
                <option value="1">4-4-2</option>
                <option value="2">4-4-2 Raute</option>
              </select>
              <select name="raster2">
                <option value="0" selected="">Eigene Taktiken</option>
                <option value="71598">3-6-1</option>
              </select>
            </form>
            """.trimIndent(),
        )

        assertEquals(setOf("O1", "A6"), taktik.codes)
        assertEquals("Mein 4-4-2", taktik.speichername)
        assertTrue(taktik.standardTaktiken.containsAll(
            listOf(AuswahlOption("1", "4-4-2"), AuswahlOption("2", "4-4-2 Raute")),
        ))
        assertEquals(listOf(AuswahlOption("71598", "3-6-1")), taktik.eigeneTaktiken)
    }
}