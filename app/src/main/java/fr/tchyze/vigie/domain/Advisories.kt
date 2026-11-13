package fr.tchyze.vigie.domain

import fr.tchyze.vigie.data.db.Device
import fr.tchyze.vigie.data.model.Advisory
import fr.tchyze.vigie.data.model.Branch
import fr.tchyze.vigie.data.model.Severity
import fr.tchyze.vigie.data.model.Snapshot

/** Un avis de sécurité et les branches qu'il concerne. */
data class AdvisoryItem(
    val advisory: Advisory,
    val branches: List<Branch>,
    /** Concerne une branche suivie ou une branche de l'inventaire. */
    val relevant: Boolean,
) {
    val severity: Severity? get() = Severity.of(advisory.severity)

    val severe: Boolean get() = severity == Severity.CRITICAL || severity == Severity.HIGH
}

object Advisories {
    /** Branches qui intéressent l'utilisateur : suivies, ou présentes dans l'inventaire. */
    fun relevantBranches(snapshot: Snapshot, follows: Follows, devices: List<Device>): Set<String> =
        snapshot.branches.filter(follows::isFollowed).mapTo(HashSet()) { it.id } +
            devices.map { it.branchId }

    /** Éditeurs des branches qui intéressent l'utilisateur. */
    fun relevantVendors(snapshot: Snapshot, relevant: Set<String>): Set<String> =
        relevant.mapNotNullTo(HashSet()) { snapshot.branch(it)?.vendor }

    /**
     * Un avis rattaché à des branches concerne l'utilisateur si l'une d'elles l'intéresse. Un avis
     * sans branche identifiée le concerne si son éditeur est celui d'une technologie suivie.
     */
    fun isRelevant(affected: Collection<String>, vendor: String, relevant: Set<String>, vendors: Set<String>): Boolean =
        if (affected.isEmpty()) vendor in vendors else affected.any { it in relevant }

    /** Tous les avis, du plus récent au plus ancien. */
    fun build(snapshot: Snapshot, relevant: Set<String>): List<AdvisoryItem> {
        val vendors = relevantVendors(snapshot, relevant)
        return snapshot.advisories.advisories
            .distinctBy { it.source to it.id }
            .map { a ->
                AdvisoryItem(
                    advisory = a,
                    branches = a.affected.mapNotNull(snapshot::branch),
                    relevant = isRelevant(a.affected, a.vendor, relevant, vendors),
                )
            }
            .sortedByDescending { it.advisory.published }
    }

    /** « FortiOS 7.4, 7.6 · PAN-OS 11.2 » */
    fun branchesLabel(item: AdvisoryItem, snapshot: Snapshot): String =
        item.branches.groupBy { it.product }.entries.joinToString(" · ") { (productId, branches) ->
            val product = snapshot.product(productId)?.label ?: productId
            product + " " + branches.joinToString(", ") { it.label }
        }
}
