package fr.tchyze.vigie.domain

import fr.tchyze.vigie.data.demoDir
import fr.tchyze.vigie.data.model.Category
import fr.tchyze.vigie.data.source.DirFileSource
import fr.tchyze.vigie.data.source.SnapshotLoader
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FollowsTest {
    private val today = LocalDate.of(2026, 10, 29)

    private suspend fun snapshot() = SnapshotLoader(DirFileSource(demoDir)).load().snapshot

    @Test
    fun `suivre un produit entier suit toutes ses branches, même futures`() = runTest {
        val snapshot = snapshot()
        val follows = Follows(products = setOf("fortios"))
        val future = snapshot.branch("fortios-7.6")!!.copy(id = "fortios-8.0", branch = "8.0")
        assertTrue(follows.isFollowed(future))
        assertEquals(
            listOf("fortios-7.4", "fortios-7.6", "fortios-7.2"),
            Feed.build(snapshot, follows::isFollowed, today).map { it.branch.id },
        )
    }

    @Test
    fun `suivre une branche ne suit pas les autres branches du produit`() = runTest {
        val snapshot = snapshot()
        val follows = Follows(branches = setOf("debian-12"))
        assertTrue(follows.isFollowed(snapshot.branch("debian-12")!!))
        assertFalse(follows.isFollowed(snapshot.branch("debian-13")!!))
    }

    @Test
    fun `le catalogue range les produits par catégorie et cache les branches EOL`() = runTest {
        val catalog = FollowCatalog.build(snapshot(), Follows(branches = setOf("panos-11.2")), today)
        assertEquals(Category.entries, catalog.map { it.category })
        val panos = catalog.first { it.category == Category.FIREWALL }.products.first { it.product.product == "panos" }
        assertEquals(listOf("panos-12.1", "panos-11.2", "panos-11.1"), panos.branches.map { it.branch.id })
        assertEquals(1, panos.eolCount)
        assertEquals(1, panos.followedBranchCount)
        assertFalse(panos.wholeProduct)
    }

    @Test
    fun `un produit entier coche toutes ses branches sans les rendre explicites`() = runTest {
        val catalog = FollowCatalog.build(snapshot(), Follows(products = setOf("nginx")), today)
        val nginx = catalog.flatMap { it.products }.first { it.product.product == "nginx" }
        assertTrue(nginx.wholeProduct)
        assertTrue(nginx.branches.all { it.followed && !it.explicit })
    }
}
