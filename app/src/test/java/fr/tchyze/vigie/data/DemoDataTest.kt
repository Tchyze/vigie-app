package fr.tchyze.vigie.data

import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.data.model.Category
import fr.tchyze.vigie.data.source.DirFileSource
import fr.tchyze.vigie.data.source.SnapshotLoader
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Les exemples embarqués doivent se lire sans erreur et rester cohérents avec l'index. */
class DemoDataTest {

    private suspend fun load() = SnapshotLoader(DirFileSource(demoDir)).load().snapshot

    @Test
    fun `les exemples se chargent en entier`() = runTest {
        val snapshot = load()
        assertEquals(snapshot.index.products.size, snapshot.products.size)
        assertTrue(snapshot.branches.isNotEmpty())
        assertTrue(snapshot.advisories.advisories.isNotEmpty())
    }

    @Test
    fun `l'index décrit fidèlement chaque fichier produit`() = runTest {
        val snapshot = load()
        snapshot.index.products.forEach { entry ->
            val product = snapshot.product(entry.id) ?: error("fichier absent : ${entry.id}")
            assertEquals(entry.branchCount, product.branches.size)
            val byStatus = product.branches.groupingBy { it.status }.eachCount()
            assertEquals(entry.id, entry.statusCounts.active, byStatus["active"] ?: 0)
            assertEquals(entry.id, entry.statusCounts.eolSoon, byStatus["eol_soon"] ?: 0)
            assertEquals(entry.id, entry.statusCounts.eol, byStatus["eol"] ?: 0)
            assertNotNull(entry.id, Category.of(entry.category))
        }
    }

    @Test
    fun `chaque branche est cohérente avec ses versions`() = runTest {
        load().branches.forEach { b ->
            assertEquals(b.id, "${b.product}-${b.branch}", b.id)
            if (b.releases.isNotEmpty()) {
                assertEquals(b.id, b.releases.first().version, b.latestVersion)
                assertEquals(b.id, b.releases.first().date, b.latestReleaseDate)
                val dates = b.releases.mapNotNull { it.date }
                assertEquals(b.id, dates.sortedDescending(), dates)
            }
            b.releases.forEach { assertTrue(b.id, (it.summary?.length ?: 0) <= 300) }
        }
    }

    @Test
    fun `les branches citées par les avis existent`() = runTest {
        val snapshot = load()
        snapshot.advisories.advisories.flatMap { it.affected }.forEach {
            assertNotNull(it, snapshot.branch(it))
        }
        val ids = snapshot.advisories.advisories.map { it.source to it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `les exemples couvrent les cas à tester dans l'application`() = runTest {
        val snapshot = load()
        val branches = snapshot.branches
        assertEquals(BranchStatus.entries.toSet(), branches.map { BranchStatus.of(it.status) }.toSet())
        assertEquals(Category.entries.toSet(), branches.mapNotNull { Category.of(it.category) }.toSet())
        assertTrue("version inconnue", branches.any { it.latestVersion == null })
        val flags = branches.flatMap { it.releases }.map { it.securityFix }.toSet()
        assertEquals(setOf(true, false, null), flags)
        assertTrue("avis sans sévérité", snapshot.advisories.advisories.any { it.severity == null })
        assertTrue("source en erreur", snapshot.index.sources.any { it.status == "error" })
    }
}
