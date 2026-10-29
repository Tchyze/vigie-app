package fr.tchyze.vigie.data

import fr.tchyze.vigie.data.db.FollowDao
import fr.tchyze.vigie.data.db.FollowedBranch
import fr.tchyze.vigie.data.db.FollowedProduct
import fr.tchyze.vigie.domain.Follows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FollowRepository(private val dao: FollowDao) {

    val follows: Flow<Follows> = combine(dao.products(), dao.branches()) { products, branches ->
        Follows(products.toSet(), branches.toSet())
    }

    suspend fun setProductFollowed(productId: String, followed: Boolean) {
        if (followed) dao.insertProduct(FollowedProduct(productId)) else dao.deleteProduct(productId)
    }

    suspend fun setBranchFollowed(branchId: String, productId: String, followed: Boolean) {
        if (followed) dao.insertBranch(FollowedBranch(branchId, productId)) else dao.deleteBranch(branchId)
    }
}
