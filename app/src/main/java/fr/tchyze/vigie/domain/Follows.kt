package fr.tchyze.vigie.domain

import fr.tchyze.vigie.data.model.Branch

/** Technologies suivies : produits entiers et branches choisies une à une. */
data class Follows(
    val products: Set<String> = emptySet(),
    val branches: Set<String> = emptySet(),
) {
    val isEmpty: Boolean get() = products.isEmpty() && branches.isEmpty()

    fun isFollowed(branch: Branch): Boolean = branch.product in products || branch.id in branches

    fun followsWholeProduct(productId: String): Boolean = productId in products
}
