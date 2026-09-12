package com.onlinesoccer.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PmRepositoryParseTest {
    private val repo = PmRepository(okhttp3.OkHttpClient())

    @Test
    fun antwortFormularLiestWebsiteFelder() {
        val formular = repo.parseAntwortFormular(
            """
            <form>
              <input name="pn_empfaenger" value="Björn Weyer">
              <input name="pn_empfaenger_id" value="1062">
              <input name="pn_transfer_id" value="0">
              <input name="pn_betreff" value="RE[10]: Lars Vincez">
              <textarea name="pn_text">[quote][hr][i]QUOTE[/i][/quote]</textarea>
            </form>
            """.trimIndent(),
        )
        assertEquals("Björn Weyer", formular.empfaenger)
        assertEquals("1062", formular.empfaengerId)
        assertEquals("RE[10]: Lars Vincez", formular.betreff)
        assertTrue(formular.text.contains("QUOTE"))
    }

    @Test
    fun detailBehaeltZeilenumbrueche() {
        val detail = repo.parseDetail("<div>Erste Zeile<br><br>Zweite Zeile</div>", 1)
        assertTrue(detail.body.orEmpty().contains("Erste Zeile"))
        assertTrue(detail.body.orEmpty().contains("Zweite Zeile"))
    }

    @Test
    fun listeLiestPosteingangUndPostausgang() {
        val liste = repo.parseListe(
            """
            <div id="tab_inbox"><div class="pmrow pmunread">
              <div class="pmcell">Betreff</div><div class="pmcell">Sender</div><div class="pmcell">Team</div>
              <div class="pmcell"></div><div class="pmcell">01.01.2026</div>
              <input type="hidden" name="pmid" value="10">
            </div></div>
            <div id="tab_outbox"><div class="pmrow">
              <div class="pmcell">Antwort</div><div class="pmcell">Empfänger</div><div class="pmcell">Team</div>
              <div class="pmcell"></div><div class="pmcell">02.01.2026</div>
              <input type="hidden" name="pmid" value="11">
            </div></div>
            """.trimIndent(),
        )
        assertEquals(2, liste.size)
        assertEquals("Sender", liste.first { it.id == 10L }.sender)
        assertEquals("Empfänger", liste.first { it.id == 11L }.empfänger)
        assertTrue(!liste.first { it.id == 10L }.gelesen)
    }
}
