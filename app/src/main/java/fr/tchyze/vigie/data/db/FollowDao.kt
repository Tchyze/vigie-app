package fr.tchyze.vigie.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowDao {
    @Query("SELECT product_id FROM followed_product")
    fun products(): Flow<List<String>>

    @Query("SELECT branch_id FROM followed_branch")
    fun branches(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProduct(product: FollowedProduct)

    @Query("DELETE FROM followed_product WHERE product_id = :productId")
    suspend fun deleteProduct(productId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBranch(branch: FollowedBranch)

    @Query("DELETE FROM followed_branch WHERE branch_id = :branchId")
    suspend fun deleteBranch(branchId: String)
}
