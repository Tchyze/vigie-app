package fr.tchyze.vigie.data.model

import java.time.LocalDate

/** Un jeu complet de données publiées : index, fichiers produits et avis. */
data class Snapshot(
    val index: IndexFile,
    val products: List<ProductFile>,
    val advisories: AdvisoriesFile,
) {
    val branches: List<Branch> by lazy { products.flatMap { it.branches } }

    private val branchesById by lazy { branches.associateBy { it.id } }
    private val productsById by lazy { products.associateBy { it.product } }

    fun branch(id: String): Branch? = branchesById[id]

    fun product(id: String): ProductFile? = productsById[id]

    /** Statut de chaque branche, recalculé à la date du jour (voir [effectiveStatus]). */
    fun statusOf(branch: Branch, today: LocalDate = LocalDate.now()): BranchStatus =
        branch.effectiveStatus(today, index.eolSoonMonths)
}

/**
 * Statut de la branche à la date [today].
 *
 * Le collecteur calcule `status` le jour du build. Avec des données en cache, ce calcul peut dater :
 * on refait le calcul de SCHEMA.md avec la date du téléphone et on garde le plus grave des deux.
 */
fun Branch.effectiveStatus(today: LocalDate, eolSoonMonths: Int): BranchStatus {
    val published = BranchStatus.of(status)
    val trigger = eol.triggerDate?.let(::parseDate)
    val computed = when {
        trigger != null && !trigger.isAfter(today) -> BranchStatus.EOL
        trigger != null && !trigger.isAfter(today.plusMonths(eolSoonMonths.toLong())) -> BranchStatus.EOL_SOON
        trigger == null && eol.declaredEol -> BranchStatus.EOL
        else -> BranchStatus.ACTIVE
    }
    return maxOf(published, computed)
}

/** Date `AAAA-MM-JJ`, ou null si la chaîne est absente ou mal formée. */
fun parseDate(value: String?): LocalDate? =
    value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
