package fr.tchyze.vigie.domain

import fr.tchyze.vigie.data.db.Device
import fr.tchyze.vigie.data.demoDir
import fr.tchyze.vigie.data.source.DirFileSource
import fr.tchyze.vigie.data.source.SnapshotLoader
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvisoriesTest {
    private suspend fun snapshot() = SnapshotLoader(DirFileSource(demoDir)).load().snapshot

    @Test
    fun `les branches pertinentes réunissent suivis et inventaire`() = runTest {
        val s = snapshot()
        val devices = listOf(Device(1, "srv", null, "debian", "debian-12", "12.14", 0, 0))
        val relevant = Advisories.relevantBranches(s, Follows(products = setOf("panos")), devices)
        assertEquals(setOf("panos-12.1", "panos-11.2", "panos-11.1", "panos-10.2", "debian-12"), relevant)
    }

    @Test
    fun `un avis est pertinent par branche, ou par éditeur s'il n'a pas de branche`() = runTest {
        val s = snapshot()
        val items = Advisories.build(s, setOf("fortios-7.4"))
        val relevantIds = items.filter { it.relevant }.map { it.advisory.id }
        // Les avis Fortinet sans branche identifiée ; ni Debian, ni CERT-FR (éditeur générique).
        assertEquals(listOf("FG-IR-26-152", "FG-IR-26-187"), relevantIds)
        assertEquals(items.sortedByDescending { it.advisory.published }, items)
    }

    @Test
    fun `libellé des branches concernées regroupé par produit`() = runTest {
        val s = snapshot()
        val item = Advisories.build(s, emptySet()).first { it.advisory.id == "PAN-SA-2026-0021" }
        assertEquals("PAN-OS 11.1, 12.1", Advisories.branchesLabel(item, s))
        assertTrue(item.severe)
    }
}
