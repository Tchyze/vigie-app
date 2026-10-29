package fr.tchyze.vigie.domain

import fr.tchyze.vigie.data.demoDir
import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.data.source.DirFileSource
import fr.tchyze.vigie.data.source.SnapshotLoader
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FeedTest {
    private val today = LocalDate.of(2026, 10, 29)

    private suspend fun snapshot() = SnapshotLoader(DirFileSource(demoDir)).load().snapshot

    @Test
    fun `seules les branches supportées apparaissent`() = runTest {
        val items = Feed.build(snapshot(), { true }, today)
        assertTrue(items.none { it.status == BranchStatus.EOL })
        assertTrue(items.any { it.status == BranchStatus.EOL_SOON })
        assertFalse(items.any { it.branch.id == "fortios-7.0" })
    }

    @Test
    fun `tri par date de dernière version, sans date en dernier`() = runTest {
        val items = Feed.build(snapshot(), { true }, today)
        val dates = items.map { it.latestDate }
        val dated = dates.takeWhile { it != null }.filterNotNull()
        assertEquals(dated.sortedDescending(), dated)
        assertTrue(dates.drop(dated.size).all { it == null })
        assertEquals("panos-12.1", items.first().branch.id)
    }

    @Test
    fun `seules les branches suivies sont retenues`() = runTest {
        val items = Feed.build(snapshot(), { it.product == "debian" }, today)
        assertEquals(listOf("debian-13", "debian-12"), items.map { it.branch.id })
    }

    @Test
    fun `le badge sécurité suit la dernière version`() = runTest {
        val items = Feed.build(snapshot(), { true }, today).associateBy { it.branch.id }
        assertTrue(items.getValue("fortios-7.4").securityFix)
        assertFalse(items.getValue("nginx-1.31").securityFix) // false
        assertFalse(items.getValue("proxmox-ve-9").securityFix) // null
    }

    @Test
    fun `le statut est recalculé à la date du jour`() = runTest {
        // Publiée « active », la branche 11.1 de PAN-OS passe en fin de support proche début novembre.
        val later = Feed.build(snapshot(), { true }, LocalDate.of(2026, 11, 15)).associateBy { it.branch.id }
        assertEquals(BranchStatus.EOL_SOON, later.getValue("panos-11.1").status)
    }
}
