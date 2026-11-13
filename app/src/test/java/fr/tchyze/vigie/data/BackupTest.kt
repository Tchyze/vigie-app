package fr.tchyze.vigie.data

import fr.tchyze.vigie.data.backup.BackupBranch
import fr.tchyze.vigie.data.backup.BackupCodec
import fr.tchyze.vigie.data.backup.BackupDevice
import fr.tchyze.vigie.data.backup.BackupException
import fr.tchyze.vigie.data.backup.BackupFile
import fr.tchyze.vigie.data.backup.BackupMerge
import fr.tchyze.vigie.data.db.Device
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    private val file = BackupFile(
        exportedAt = "2026-11-13T08:00:00Z",
        devices = listOf(BackupDevice("fw-siege", "Siège", "fortios", "fortios-7.4", "7.4.12")),
        followedProducts = listOf("fortios"),
        followedBranches = listOf(BackupBranch("debian-12", "debian")),
    )

    @Test
    fun `aller-retour export puis import`() {
        val text = BackupCodec.encode(file)
        assertTrue(text.contains("\"format\": \"vigie-inventaire\""))
        assertTrue(text.contains("\"installed_version\": \"7.4.12\""))
        assertEquals(file, BackupCodec.decode(text))
    }

    @Test
    fun `un fichier étranger ou trop récent est refusé`() {
        assertThrows(BackupException::class.java) { BackupCodec.decode("pas du json") }
        assertThrows(BackupException::class.java) { BackupCodec.decode("""{"format":"autre","exported_at":"x"}""") }
        val future = BackupCodec.encode(file.copy(formatVersion = 2))
        val e = assertThrows(BackupException::class.java) { BackupCodec.decode(future) }
        assertTrue(e.message!!.contains("plus récente"))
        val incomplete = BackupCodec.encode(file.copy(devices = listOf(BackupDevice("x", null, "fortios", "fortios-7.4", " "))))
        assertThrows(BackupException::class.java) { BackupCodec.decode(incomplete) }
    }

    @Test
    fun `l'import met à jour un équipement de même nom et ajoute les autres`() {
        val existing = listOf(Device(7, "FW-Siege", null, "fortios", "fortios-7.2", "7.2.13", createdAt = 1, updatedAt = 1))
        val imported = file.devices + BackupDevice("srv-web", null, "debian", "debian-12", "12.15")
        val (devices, updated) = BackupMerge.devices(existing, imported, now = 100)
        assertEquals(1, updated)
        val fw = devices.first { it.name == "fw-siege" }
        assertEquals(7L, fw.id)
        assertEquals("fortios-7.4", fw.branchId)
        assertEquals(1L, fw.createdAt)
        assertEquals(100L, fw.updatedAt)
        assertEquals(0L, devices.first { it.name == "srv-web" }.id)
    }
}
