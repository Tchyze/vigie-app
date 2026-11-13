package fr.tchyze.vigie.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class NotifiedDao {
    @Query("SELECT `key` FROM notified")
    abstract suspend fun keys(): List<String>

    @Query("DELETE FROM notified")
    abstract suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(items: List<Notified>)

    /** Remplace les clés mémorisées par [keys] (les faits disparus des données sont oubliés). */
    @Transaction
    open suspend fun replaceAll(keys: Collection<String>, now: Long) {
        clear()
        insert(keys.map { Notified(it, now) })
    }
}
