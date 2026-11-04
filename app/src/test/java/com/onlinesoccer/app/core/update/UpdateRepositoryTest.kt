package com.onlinesoccer.app.core.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateRepositoryTest {
    @Test
    fun vergleichtPatchVersionenNumerisch() {
        assertEquals(1, versionVergleich("1.0.10", "1.0.9"))
        assertEquals(0, versionVergleich("1.2", "1.2.0"))
        assertEquals(-1, versionVergleich("2.0.0", "2.1.0"))
    }
}
