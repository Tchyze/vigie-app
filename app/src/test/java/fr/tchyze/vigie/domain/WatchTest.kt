package fr.tchyze.vigie.domain

import fr.tchyze.vigie.data.db.Device
import fr.tchyze.vigie.data.demoDir
import fr.tchyze.vigie.data.model.Snapshot
import fr.tchyze.vigie.data.source.DirFileSource
import fr.tchyze.vigie.data.source.SnapshotLoader
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WatchTest {
    private val today = LocalDate.of(2026, 10, 29)

    private suspend fun snapshot() = SnapshotLoader(DirFileSource(demoDir)).load().snapshot

    private fun check(s: Snapshot, known: Set<String>, relevant: Set<String>, devices: List<Device> = emptyList()) =
        Watch.check(Watch.events(s, devices, today), known, relevant, Advisories.relevantVendors(s, relevant))

    /** Les données démo avec une nouvelle version 7.4.14 sur FortiOS 7.4. */
    private fun withNewFortiOS(s: Snapshot): Snapshot {
        val products = s.products.map { p ->
            if (p.product != "fortios") return@map p
            p.copy(branches = p.branches.map { b ->
                if (b.id != "fortios-7.4") return@map b
                val release = b.releases.first().copy(version = "7.4.14", securityFix = true)
                b.copy(latestVersion = "7.4.14", releases = listOf(release) + b.releases)
            })
        }
        return s.copy(products = products)
    }

    @Test
    fun `la première vérification sert de référence et ne notifie rien`() = runTest {
        val outcome = check(snapshot(), known = emptySet(), relevant = setOf("fortios-7.4"))
        assertTrue(outcome.notify.isEmpty())
        assertTrue("release:fortios-7.4:7.4.13" in outcome.known)
        assertTrue("advisory:fortinet/FG-IR-26-187" in outcome.known)
    }

    @Test
    fun `une nouvelle version d'une branche suivie est notifiée, avec les équipements concernés`() = runTest {
        val s = snapshot()
        val known = check(s, emptySet(), emptySet()).known
        val devices = listOf(Device(1, "fw-siege", null, "fortios", "fortios-7.4", "7.4.13", 0, 0))
        val outcome = check(withNewFortiOS(s), known, setOf("fortios-7.4"), devices)
        assertEquals(listOf("release:fortios-7.4:7.4.14"), outcome.notify.map { it.key })
        val event = outcome.notify.single()
        assertEquals("FortiOS 7.4.14", event.title)
        assertTrue(event.text.contains("correctifs de sécurité"))
        assertTrue(event.text.contains("Concerne : fw-siege"))
    }

    @Test
    fun `rien n'est notifié pour une branche non suivie, mais le fait est mémorisé`() = runTest {
        val s = snapshot()
        val known = check(s, emptySet(), emptySet()).known
        val outcome = check(withNewFortiOS(s), known, setOf("debian-12"))
        assertTrue(outcome.notify.isEmpty())
        assertTrue("release:fortios-7.4:7.4.14" in outcome.known)
    }

    @Test
    fun `suivre une branche ne notifie pas son historique`() = runTest {
        val s = snapshot()
        val known = check(s, emptySet(), emptySet()).known
        assertTrue(check(s, known, setOf("fortios-7.4", "debian-12", "fortios-7.2")).notify.isEmpty())
    }

    @Test
    fun `fin de support et avis sans branche de l'éditeur suivi`() = runTest {
        val s = snapshot()
        val known = check(s, emptySet(), emptySet()).known - "status:fortios-7.2:eol_soon" - "advisory:fortinet/FG-IR-26-187"
        val keys = check(s, known, setOf("fortios-7.2")).notify.map { it.key }.toSet()
        assertEquals(setOf("status:fortios-7.2:eol_soon", "advisory:fortinet/FG-IR-26-187"), keys)
    }
}
