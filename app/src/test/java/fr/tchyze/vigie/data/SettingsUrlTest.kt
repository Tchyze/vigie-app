package fr.tchyze.vigie.data

import fr.tchyze.vigie.data.settings.normalizeBaseUrl
import fr.tchyze.vigie.data.settings.validateBaseUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsUrlTest {
    @Test
    fun `adresse des données`() {
        assertNull(validateBaseUrl(" https://exemple.fr/vigie "))
        assertNotNull(validateBaseUrl("http://exemple.fr/"))
        assertNotNull(validateBaseUrl("https://"))
        assertNotNull(validateBaseUrl("https://exemple.fr/a b"))
        assertEquals("https://exemple.fr/vigie/", normalizeBaseUrl(" https://exemple.fr/vigie// "))
    }
}
