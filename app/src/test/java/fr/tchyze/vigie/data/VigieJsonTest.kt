package fr.tchyze.vigie.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VigieJsonTest {

    private val product = demoText("products/fortios.json")

    @Test
    fun `un schema_version inconnu est signalé sans tenter de décoder`() {
        val text = product.replace("\"schema_version\": 1", "\"schema_version\": 2")
        val e = assertThrows(DataException.UnsupportedSchema::class.java) {
            VigieJson.parseProduct("products/fortios.json", text)
        }
        assertEquals(2, e.version)
        assertEquals("products/fortios.json", e.file)
    }

    @Test
    fun `une version 2 au format totalement différent reste une erreur de version`() {
        assertThrows(DataException.UnsupportedSchema::class.java) {
            VigieJson.parseIndex("index.json", """{"schema_version": 2, "catalogue": []}""")
        }
    }

    @Test
    fun `schema_version absent ou texte illisible sont des données invalides`() {
        assertThrows(DataException.InvalidData::class.java) {
            VigieJson.parseIndex("index.json", """{"products": []}""")
        }
        assertThrows(DataException.InvalidData::class.java) {
            VigieJson.parseIndex("index.json", "<html>404</html>")
        }
        assertThrows(DataException.InvalidData::class.java) {
            VigieJson.parseIndex("index.json", "[1, 2]")
        }
    }

    @Test
    fun `un champ ajouté par le collecteur est ignoré`() {
        val text = product.replace("\"schema_version\": 1,", "\"schema_version\": 1, \"nouveau_champ\": {\"a\": 1},")
        assertEquals("fortios", VigieJson.parseProduct("products/fortios.json", text).product)
    }

    @Test
    fun `un champ obligatoire manquant rend le fichier invalide`() {
        val text = product.replace("\"vendor\": \"Fortinet\",\n  \"category\"", "\"category\"")
        assertThrows(DataException.InvalidData::class.java) {
            VigieJson.parseProduct("products/fortios.json", text)
        }
    }

    @Test
    fun `les champs sont lus sous leur nom exact`() {
        val branch = VigieJson.parseProduct("products/fortios.json", product).branches.first { it.id == "fortios-7.4" }
        assertEquals("7.4.13", branch.latestVersion)
        assertEquals("2026-10-21", branch.latestReleaseDate)
        assertEquals("end_of_life", branch.eol.trigger)
        assertEquals("2027-11-11", branch.eol.triggerDate)
        assertEquals("End of Support", branch.eol.labels["end_of_life"])
        assertEquals(true, branch.releases.first().securityFix)
    }
}
