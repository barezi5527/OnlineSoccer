package com.onlinesoccer.app.core.state

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamGenerationTest {
    @Test
    fun gleicheGenerationErlaubtSchreibvorgang() {
        assertTrue(schreibvorgangErlaubt(geladeneGeneration = 0L, aktuelleGeneration = 0L))
        assertTrue(schreibvorgangErlaubt(geladeneGeneration = 5L, aktuelleGeneration = 5L))
    }

    @Test
    fun verschiedeneGenerationVerweigertSchreibvorgang() {
        assertFalse(schreibvorgangErlaubt(geladeneGeneration = 0L, aktuelleGeneration = 1L))
        assertFalse(schreibvorgangErlaubt(geladeneGeneration = 1L, aktuelleGeneration = 0L))
        assertFalse(schreibvorgangErlaubt(geladeneGeneration = 5L, aktuelleGeneration = 6L))
    }

    @Test
    fun bestaetigungstextIstLeerWennKeineOffenenBereiche() {
        assertTrue(bestaetigungstext(emptySet()) == null)
    }

    @Test
    fun bestaetigungstextEnthaltOffeneBereiche() {
        val text = bestaetigungstext(setOf(AenderungBereich.ZUGABABE, AenderungBereich.TAKTIK))
        requireNotNull(text)
        assertTrue(text.contains("Zugababe"))
        assertTrue(text.contains("Taktik"))
        assertTrue(text.contains("wechseln"))
        assertFalse(text.contains("nichts"))
    }
}
