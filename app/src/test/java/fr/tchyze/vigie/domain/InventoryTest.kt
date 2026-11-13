package fr.tchyze.vigie.domain

import fr.tchyze.vigie.data.db.Device
import fr.tchyze.vigie.data.demoDir
import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.data.model.Release
import fr.tchyze.vigie.data.source.DirFileSource
import fr.tchyze.vigie.data.source.SnapshotLoader
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class InventoryTest {
    private val today = LocalDate.of(2026, 10, 29)

    private suspend fun snapshot() = SnapshotLoader(DirFileSource(demoDir)).load().snapshot

    private fun device(name: String, product: String, branch: String, version: String, id: Long = 1) =
        Device(id, name, null, product, branch, version, createdAt = 0, updatedAt = 0)

    @Test
    fun `à jour quand la version installée est la dernière`() = runTest {
        val check = Inventory.check(device("fw", "fortios", "fortios-7.4", "7.4.13"), snapshot(), today)
        assertEquals(VersionState.UP_TO_DATE, check.versionState)
        assertEquals(emptyList<Release>(), check.missed)
        assertEquals(Verdict.UP_TO_DATE, check.verdict)
    }

    @Test
    fun `une version de retard avec correctif de sécurité`() = runTest {
        val check = Inventory.check(device("fw", "fortios", "fortios-7.4", "7.4.12"), snapshot(), today)
        assertEquals(VersionState.BEHIND, check.versionState)
        assertEquals(listOf("7.4.13"), check.missed.map { it.version })
        assertEquals(Verdict.SECURITY_UPDATE, check.verdict)
    }

    @Test
    fun `les versions manquées comprennent les correctifs à suffixe`() = runTest {
        val check = Inventory.check(device("pa", "panos", "panos-12.1", "12.1.9-h3"), snapshot(), today)
        assertEquals(listOf("12.1.10-h2", "12.1.10"), check.missed.map { it.version })
    }

    @Test
    fun `retard sans correctif de sécurité`() = runTest {
        val base = Inventory.check(device("fw", "fortios", "fortios-7.4", "7.4.12"), snapshot(), today)
        val check = base.copy(missed = listOf(Release("7.4.13", null, securityFix = false, summary = null, url = null)))
        assertEquals(Verdict.UPDATE, check.verdict)
    }

    @Test
    fun `à jour sur une branche en fin de support proche`() = runTest {
        val check = Inventory.check(device("fw", "fortios", "fortios-7.2", "7.2.14"), snapshot(), today)
        assertEquals(BranchStatus.EOL_SOON, check.status)
        assertEquals(Verdict.EOL_SOON, check.verdict)
    }

    @Test
    fun `une branche hors support l'emporte sur tout le reste`() = runTest {
        val check = Inventory.check(device("srv", "debian", "debian-11", "11.11"), snapshot(), today)
        assertEquals(Verdict.EOL, check.verdict)
    }

    @Test
    fun `sans dernière version publiée, impossible de conclure`() = runTest {
        val check = Inventory.check(device("sns", "stormshield-sns", "stormshield-sns-5.0", "5.0.2"), snapshot(), today)
        assertEquals(VersionState.UNKNOWN, check.versionState)
        assertEquals(Verdict.UNKNOWN, check.verdict)
    }

    @Test
    fun `une branche absente des données reste affichée`() = runTest {
        val check = Inventory.check(device("old", "fortios", "fortios-6.4", "6.4.15"), snapshot(), today)
        assertNull(check.branch)
        assertEquals("FortiOS", check.product?.label)
        assertEquals(Verdict.UNKNOWN, check.verdict)
        assertEquals(Verdict.UNKNOWN, Inventory.check(check.device, null, today).verdict)
    }

    @Test
    fun `les plus urgents d'abord, puis par nom, et le résumé`() = runTest {
        val devices = listOf(
            device("b-ok", "fortios", "fortios-7.4", "7.4.13", 1),
            device("a-ok", "debian", "debian-13", "13.7", 2),
            device("eol", "debian", "debian-11", "11.11", 3),
            device("secu", "fortios", "fortios-7.4", "7.4.12", 4),
            device("bientot", "nginx", "nginx-1.30", "1.30.6", 5),
        )
        val checks = Inventory.build(snapshot(), devices, today)
        assertEquals(listOf("eol", "secu", "bientot", "a-ok", "b-ok"), checks.map { it.device.name })
        assertEquals(InventorySummary(toFix = 2, toWatch = 1, upToDate = 2, unknown = 0), Inventory.summary(checks))
    }
}
