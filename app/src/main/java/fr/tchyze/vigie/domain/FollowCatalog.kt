package fr.tchyze.vigie.domain

import fr.tchyze.vigie.data.model.Branch
import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.data.model.Category
import fr.tchyze.vigie.data.model.ProductFile
import fr.tchyze.vigie.data.model.Snapshot
import java.time.LocalDate

/** Ce que l'écran « Technologies suivies » propose : catégorie, puis produit, puis branche. */
data class CatalogCategory(val category: Category, val products: List<CatalogProduct>)

data class CatalogProduct(
    val product: ProductFile,
    /** Branches encore supportées, seules proposées au suivi. */
    val branches: List<CatalogBranch>,
    val eolCount: Int,
    val wholeProduct: Boolean,
) {
    val followedBranchCount: Int get() = branches.count { it.followed }
}

data class CatalogBranch(val branch: Branch, val status: BranchStatus, val followed: Boolean, val explicit: Boolean)

object FollowCatalog {
    fun build(snapshot: Snapshot, follows: Follows, today: LocalDate = LocalDate.now()): List<CatalogCategory> =
        Category.entries.mapNotNull { category ->
            val products = snapshot.products
                .filter { Category.of(it.category) == category }
                .sortedBy { it.label.lowercase() }
                .map { product ->
                    val whole = follows.followsWholeProduct(product.product)
                    val withStatus = product.branches.map { it to snapshot.statusOf(it, today) }
                    CatalogProduct(
                        product = product,
                        branches = withStatus.filter { it.second != BranchStatus.EOL }.map { (b, status) ->
                            CatalogBranch(b, status, followed = follows.isFollowed(b), explicit = b.id in follows.branches)
                        },
                        eolCount = withStatus.count { it.second == BranchStatus.EOL },
                        wholeProduct = whole,
                    )
                }
            if (products.isEmpty()) null else CatalogCategory(category, products)
        }
}
