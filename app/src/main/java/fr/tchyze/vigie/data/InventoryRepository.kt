package fr.tchyze.vigie.data

import fr.tchyze.vigie.data.db.Device
import fr.tchyze.vigie.data.db.DeviceDao
import kotlinx.coroutines.flow.Flow

/** Inventaire des équipements, stocké uniquement sur le téléphone. */
class InventoryRepository(
    private val dao: DeviceDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val devices: Flow<List<Device>> = dao.all()

    suspend fun get(id: Long): Device? = dao.get(id)

    /** Crée l'équipement si [id] vaut 0, le met à jour sinon. */
    suspend fun save(
        id: Long,
        name: String,
        site: String?,
        productId: String,
        branchId: String,
        installedVersion: String,
    ) {
        val now = clock()
        val createdAt = if (id == 0L) now else dao.get(id)?.createdAt ?: now
        dao.upsert(
            Device(
                id = id,
                name = name.trim(),
                site = site?.trim()?.ifEmpty { null },
                productId = productId,
                branchId = branchId,
                installedVersion = installedVersion.trim(),
                createdAt = createdAt,
                updatedAt = now,
            ),
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
