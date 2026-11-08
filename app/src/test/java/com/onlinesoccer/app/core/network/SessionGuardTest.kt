package com.onlinesoccer.app.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionGuardTest {

    @Test
    fun demoManagerbueroMitHtmlEntityIstKeinePersoenlicheSitzung() {
        val html = "Kontostand Demo-Managerb&uuml;ro"

        assertFalse(SessionGuard.isPersonalView(html.toByteArray()))
    }

    @Test
    fun demoManagerbueroMitUmlautIstKeinePersoenlicheSitzung() {
        val html = "Kontostand Demo-Managerb\u00FCro"

        assertFalse(SessionGuard.isPersonalView(html.toByteArray()))
    }

    @Test
    fun demoTeamIstKeinePersoenlicheSitzung() {
        val html = "Kontostand DemoTeam"

        assertFalse(SessionGuard.isPersonalView(html.toByteArray()))
    }

    @Test
    fun persoenlicheSitzungBleibtGueltig() {
        val html = "Kontostand Mein Verein"

        assertTrue(SessionGuard.isPersonalView(html.toByteArray()))
    }
}
