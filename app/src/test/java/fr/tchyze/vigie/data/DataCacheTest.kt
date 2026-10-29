package fr.tchyze.vigie.data

import fr.tchyze.vigie.data.cache.DataCache
import fr.tchyze.vigie.data.source.SnapshotLoader
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

class DataCacheTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `cache vide au premier lancement`() = runTest {
        assertNull(DataCache(tmp.root).read())
    }

    @Test
    fun `les données écrites se relisent à l'identique`() = runTest {
        val cache = DataCache(tmp.root)
        val loaded = SnapshotLoader(MapFileSource.demo()).load()
        val at = Instant.parse("2026-10-29T08:00:00Z")
        cache.write(loaded, at)

        val read = DataCache(tmp.root).read()!!
        assertEquals(at, read.fetchedAt)
        assertEquals(loaded.snapshot, read.loaded.snapshot)
    }

    @Test
    fun `une nouvelle écriture remplace l'ancienne sans laisser de restes`() = runTest {
        val cache = DataCache(tmp.root)
        val loaded = SnapshotLoader(MapFileSource.demo()).load()
        cache.write(loaded, Instant.parse("2026-10-28T08:00:00Z"))
        cache.write(loaded, Instant.parse("2026-10-29T08:00:00Z"))

        assertEquals(Instant.parse("2026-10-29T08:00:00Z"), cache.read()!!.fetchedAt)
        assertEquals(listOf("current"), tmp.root.list()!!.sorted())
        assertFalse(File(tmp.root, "incoming").exists())
    }
}
