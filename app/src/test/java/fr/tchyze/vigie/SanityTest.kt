package fr.tchyze.vigie

import org.junit.Assert.assertEquals
import org.junit.Test

class SanityTest {
    @Test
    fun `la chaîne de tests JVM fonctionne`() {
        assertEquals(4, 2 + 2)
    }
}
