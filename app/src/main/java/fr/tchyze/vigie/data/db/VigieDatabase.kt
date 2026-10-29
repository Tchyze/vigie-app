package fr.tchyze.vigie.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FollowedProduct::class, FollowedBranch::class, Device::class, Notified::class],
    version = 1,
    exportSchema = true,
)
abstract class VigieDatabase : RoomDatabase() {
    abstract fun followDao(): FollowDao

    companion object {
        fun create(context: Context): VigieDatabase =
            Room.databaseBuilder(context, VigieDatabase::class.java, "vigie.db").build()
    }
}
