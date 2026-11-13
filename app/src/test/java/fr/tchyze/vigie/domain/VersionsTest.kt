package fr.tchyze.vigie.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {
    private fun assertOlder(older: String, newer: String) {
        assertTrue("$older < $newer", Versions.compare(older, newer) < 0)
        assertTrue("$newer > $older", Versions.compare(newer, older) > 0)
    }

    @Test
    fun `les blocs numériques se comparent par valeur`() {
        assertOlder("7.4.9", "7.4.13")
        assertOlder("12.15", "13.5")
        assertOlder("10.0.26100.6899", "10.0.26100.7019")
        assertOlder("1.30.6", "1.31.0")
    }

    @Test
    fun `un suffixe éditeur rend la version plus récente`() {
        assertOlder("12.1.10", "12.1.10-h2")
        assertOlder("12.1.10-h2", "12.1.10-h10")
        assertOlder("17.18.1", "17.18.1a")
        assertOlder("10.1p1", "10.2p1")
        assertOlder("10.0p2", "10.0p3")
    }

    @Test
    fun `une préversion précède la version suivante`() {
        assertOlder("1.0rc1", "1.0.1")
    }

    @Test
    fun `la casse, les espaces et les zéros initiaux sont ignorés`() {
        assertEquals(0, Versions.compare(" 7.4.13 ", "7.4.13"))
        assertEquals(0, Versions.compare("12.1.10-H2", "12.1.10-h2"))
        assertEquals(0, Versions.compare("7.04.013", "7.4.13"))
        assertEquals(0, Versions.compare("12.1.10-h2", "12.1.10.h2"))
    }
}
