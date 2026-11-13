package fr.tchyze.vigie.data.backup

import fr.tchyze.vigie.data.db.Device
import fr.tchyze.vigie.data.db.DeviceDao
import fr.tchyze.vigie.data.db.FollowDao
import fr.tchyze.vigie.data.db.FollowedBranch
import fr.tchyze.vigie.data.db.FollowedProduct
import kotlinx.coroutines.flow.first
import java.time.Instant

data class ImportResult(val added: Int, val updated: Int, val follows: Int) {
    val message: String
        get() = buildList {
            add(if (added > 1) "$added équipements ajoutés" else "$added équipement ajouté")
            if (updated > 0) add(if (updated > 1) "$updated mis à jour" else "1 mis à jour")
            if (follows > 0) add("$follows technologies suivies")
        }.joinToString(", ")
}

object BackupMerge {
    /**
     * Équipements à enregistrer : un équipement de même nom (sans tenir compte de la casse) est
     * mis à jour, les autres sont ajoutés. Rien n'est supprimé.
     */
    fun devices(existing: List<Device>, imported: List<BackupDevice>, now: Long): Pair<List<Device>, Int> {
        val byName = existing.associateBy { it.name.trim().lowercase() }
        var updated = 0
        val result = imported.distinctBy { it.name.trim().lowercase() }.map { d ->
            val current = byName[d.name.trim().lowercase()]
            if (current != null) updated++
            Device(
                id = current?.id ?: 0,
                name = d.name.trim(),
                site = d.site?.trim()?.ifEmpty { null },
                productId = d.productId,
                branchId = d.branchId,
                installedVersion = d.installedVersion.trim(),
                createdAt = current?.createdAt ?: now,
                updatedAt = now,
            )
        }
        return result to updated
    }
}

class BackupRepository(
    private val devices: DeviceDao,
    private val follows: FollowDao,
    private val clock: () -> Instant = Instant::now,
) {
    suspend fun export(): String = BackupCodec.encode(
        BackupFile(
            exportedAt = clock().toString(),
            devices = devices.all().first().map {
                BackupDevice(it.name, it.site, it.productId, it.branchId, it.installedVersion)
            },
            followedProducts = follows.products().first().sorted(),
            followedBranches = follows.allBranches().map { BackupBranch(it.branchId, it.productId) },
        ),
    )

    /** @throws BackupException si le fichier n'est pas un export valide (rien n'est alors modifié). */
    suspend fun import(text: String): ImportResult {
        val file = BackupCodec.decode(text)
        val (toSave, updated) = BackupMerge.devices(devices.all().first(), file.devices, clock().toEpochMilli())
        toSave.forEach { devices.upsert(it) }
        file.followedProducts.forEach { follows.insertProduct(FollowedProduct(it)) }
        file.followedBranches.forEach { follows.insertBranch(FollowedBranch(it.branchId, it.productId)) }
        return ImportResult(
            added = toSave.size - updated,
            updated = updated,
            follows = file.followedProducts.size + file.followedBranches.size,
        )
    }
}
