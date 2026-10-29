package fr.tchyze.vigie.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Produit suivi en entier : toutes ses branches non EOL, y compris celles à venir. */
@Entity(tableName = "followed_product")
data class FollowedProduct(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: String,
)

/** Branche suivie individuellement. */
@Entity(tableName = "followed_branch", indices = [Index("product_id")])
data class FollowedBranch(
    @PrimaryKey @ColumnInfo(name = "branch_id") val branchId: String,
    @ColumnInfo(name = "product_id") val productId: String,
)

/** Équipement de l'inventaire. Les identifiants renvoient aux données publiées. */
@Entity(tableName = "device", indices = [Index("branch_id")])
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val site: String?,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
    @ColumnInfo(name = "installed_version") val installedVersion: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Événement déjà notifié (ou déjà connu), pour ne jamais notifier deux fois. */
@Entity(tableName = "notified")
data class Notified(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "notified_at") val notifiedAt: Long,
)
