package fr.tchyze.vigie.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM device ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<Device>>

    @Query("SELECT * FROM device WHERE id = :id")
    suspend fun get(id: Long): Device?

    @Upsert
    suspend fun upsert(device: Device)

    @Query("DELETE FROM device WHERE id = :id")
    suspend fun delete(id: Long)
}
