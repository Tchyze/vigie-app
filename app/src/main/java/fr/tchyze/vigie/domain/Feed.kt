package fr.tchyze.vigie.domain

import fr.tchyze.vigie.data.model.Branch
import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.data.model.ProductFile
import fr.tchyze.vigie.data.model.Release
import fr.tchyze.vigie.data.model.Snapshot
import fr.tchyze.vigie.data.model.parseDate
import java.time.LocalDate

/** Une carte du fil principal. */
data class FeedItem(
    val branch: Branch,
    val product: ProductFile,
    val status: BranchStatus,
    val latest: Release?,
) {
    val latestDate: LocalDate? get() = parseDate(branch.latestReleaseDate)
    val securityFix: Boolean get() = latest?.securityFix == true
}

/** La version publiée sous `latest_version`, ou à défaut la plus récente de la liste. */
fun Branch.latestRelease(): Release? =
    releases.firstOrNull { it.version == latestVersion } ?: releases.firstOrNull()

object Feed {
    /**
     * Branches suivies encore supportées (`active` ou `eol_soon`), de la version la plus récente à
     * la plus ancienne ; les branches sans date de version viennent en dernier.
     */
    fun build(snapshot: Snapshot, isFollowed: (Branch) -> Boolean, today: LocalDate = LocalDate.now()): List<FeedItem> =
        snapshot.products.flatMap { product ->
            product.branches.map { FeedItem(it, product, snapshot.statusOf(it, today), it.latestRelease()) }
        }
            .filter { it.status != BranchStatus.EOL && isFollowed(it.branch) }
            .sortedWith(
                compareByDescending<FeedItem, LocalDate?>(nullsFirst()) { it.latestDate }
                    .thenBy { it.product.label.lowercase() }
                    .thenByDescending { it.branch.releaseDate ?: "" },
            )
}
