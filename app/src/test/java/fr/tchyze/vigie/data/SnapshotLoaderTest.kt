package fr.tchyze.vigie.data

import fr.tchyze.vigie.data.source.SnapshotLoader
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SnapshotLoaderTest {

    private suspend inline fun <reified E : DataException> assertFails(source: MapFileSource): E {
        try {
            SnapshotLoader(source).load()
        } catch (e: DataException) {
            if (e is E) return e
            fail("attendu ${E::class.simpleName}, obtenu ${e::class.simpleName} : ${e.message}")
        }
        fail("aucune erreur")
        error("inaccessible")
    }

    @Test
    fun `tous les fichiers bruts sont conservés pour le cache`() = runTest {
        val loaded = SnapshotLoader(MapFileSource.demo()).load()
        assertTrue("index.json" in loaded.rawFiles)
        assertTrue("advisories.json" in loaded.rawFiles)
        assertEquals(loaded.snapshot.products.size + 2, loaded.rawFiles.size)
    }

    @Test
    fun `un fichier produit en version 2 fait échouer tout le jeu`() = runTest {
        val path = "products/debian.json"
        val source = MapFileSource.demo(mapOf(path to demoText(path).replace("\"schema_version\": 1", "\"schema_version\": 2")))
        assertEquals(path, assertFails<DataException.UnsupportedSchema>(source).file)
    }

    @Test
    fun `un fichier manquant est une erreur réseau`() = runTest {
        val source = MapFileSource(mapOf("index.json" to demoText("index.json")))
        assertFails<DataException.Network>(source)
    }

    @Test
    fun `un chemin de fichier hors de products est refusé`() = runTest {
        val index = demoText("index.json").replace("\"products/nginx.json\"", "\"products/../../shared_prefs/x.json\"")
        val source = MapFileSource.demo(mapOf("index.json" to index))
        assertFails<DataException.InvalidData>(source)
    }

    @Test
    fun `un fichier produit qui ne correspond pas à l'index est refusé`() = runTest {
        val source = MapFileSource.demo(mapOf("products/nginx.json" to demoText("products/openssh.json")))
        assertFails<DataException.InvalidData>(source)
    }
}
