package com.onlinesoccer.app.feature.taktik

import org.junit.Assert.assertEquals
import org.junit.Test

class TaktikBuchstabenTest {

    @Test
    fun buchstabenWerdenInLesereihenfolgeVergeben() {
        val codes = setOf("O1", "O2", "J5", "E3", "A11")
        val buchstaben = taktikBuchstaben(codes)

        assertEquals("A", buchstaben["O1"])
        assertEquals("B", buchstaben["O2"])
        assertEquals("C", buchstaben["J5"])
        assertEquals("D", buchstaben["E3"])
        assertEquals("E", buchstaben["A11"])
    }

    @Test
    fun iUndJUebersprungen_belegt() {
        val codes = buildSet {
            for (zeile in "ONMLKJIHGFEDCBA") {
                for (spalte in 1..11) {
                    add("$zeile$spalte")
                }
            }
        }
        val buchstaben = taktikBuchstaben(codes)

        assertEquals(10, buchstaben.size)
        assertEquals("A", buchstaben["O1"])
        assertEquals("L", buchstaben.values.last())
        assertEquals(setOf("A", "B", "C", "D", "E", "F", "G", "H", "K", "L"), buchstaben.values.toSet())
    }

    @Test
    fun leereCodesErgebenLeereMap() {
        assertEquals(emptyMap<String, String>(), taktikBuchstaben(emptySet()))
    }
}