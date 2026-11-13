package fr.tchyze.vigie.domain

import fr.tchyze.vigie.data.db.Device
import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.data.model.Severity
import fr.tchyze.vigie.data.model.Snapshot
import fr.tchyze.vigie.data.model.parseDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Un fait notifiable : nouvelle version, nouvel avis, ou changement de statut d'une branche. */
data class WatchEvent(
    /** Identifiant stable : un même fait garde la même clé d'une actualisation à l'autre. */
    val key: String,
    val title: String,
    val text: String,
    /** Branches concernées, pour savoir si l'événement intéresse l'utilisateur. */
    val branchIds: Set<String>,
    /** Éditeur d'un avis sans branche identifiée (voir [Advisories.isRelevant]). */
    val vendor: String? = null,
)

/** Ce qu'une vérification doit notifier et mémoriser. */
data class WatchOutcome(
    val notify: List<WatchEvent>,
    /** Toutes les clés actuelles, à mémoriser comme déjà connues. */
    val known: Set<String>,
)

/**
 * Détection des nouveautés à notifier.
 *
 * Chaque fait reçoit une clé. On mémorise les clés de **tous** les faits, suivis ou non : suivre une
 * nouvelle branche ne déclenche donc pas de rafale de notifications sur son historique. La toute
 * première vérification (ou un changement de source de données) sert de référence et ne notifie rien.
 */
object Watch {
    private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)

    fun events(snapshot: Snapshot, devices: List<Device>, today: LocalDate = LocalDate.now()): List<WatchEvent> {
        val devicesByBranch = devices.groupBy { it.branchId }
        fun concerned(branchIds: Collection<String>): String {
            val names = branchIds.flatMap { devicesByBranch[it].orEmpty() }.map { it.name }.distinct().sorted()
            return if (names.isEmpty()) "" else "\nConcerne : " + names.joinToString(", ")
        }

        val branchEvents = snapshot.products.flatMap { product ->
            product.branches.flatMap { b ->
                val name = "${product.label} ${b.label}"
                buildList {
                    b.latestVersion?.let { version ->
                        val security = b.latestRelease()?.securityFix == true
                        add(
                            WatchEvent(
                                key = "release:${b.id}:$version",
                                title = "${product.label} $version",
                                text = "Nouvelle version de la branche ${b.label}" +
                                    (if (security) ", avec correctifs de sécurité." else ".") + concerned(listOf(b.id)),
                                branchIds = setOf(b.id),
                            ),
                        )
                    }
                    val end = parseDate(b.eol.triggerDate)?.format(dayFormat)
                    when (snapshot.statusOf(b, today)) {
                        BranchStatus.EOL_SOON -> add(
                            WatchEvent(
                                key = "status:${b.id}:eol_soon",
                                title = "$name : fin de support proche",
                                text = (end?.let { "Fin de support le $it." } ?: "Fin de support annoncée.") +
                                    concerned(listOf(b.id)),
                                branchIds = setOf(b.id),
                            ),
                        )
                        BranchStatus.EOL -> add(
                            WatchEvent(
                                key = "status:${b.id}:eol",
                                title = "$name n'est plus supportée",
                                text = "Plus aucun correctif ne sera publié pour cette branche." + concerned(listOf(b.id)),
                                branchIds = setOf(b.id),
                            ),
                        )
                        BranchStatus.ACTIVE -> Unit
                    }
                }
            }
        }

        val advisoryEvents = snapshot.advisories.advisories.map { a ->
            val severity = Severity.of(a.severity)?.label ?: "Sévérité non précisée"
            WatchEvent(
                key = "advisory:${a.source}/${a.id}",
                title = a.title,
                text = "$severity · ${a.vendor}" + concerned(a.affected),
                branchIds = a.affected.toSet(),
                vendor = a.vendor,
            )
        }
        return branchEvents + advisoryEvents
    }

    /**
     * @param known clés déjà mémorisées ; vide lors de la première vérification.
     * @param relevant branches suivies ou présentes dans l'inventaire.
     * @param vendors éditeurs de ces branches.
     */
    fun check(events: List<WatchEvent>, known: Set<String>, relevant: Set<String>, vendors: Set<String>): WatchOutcome {
        val keys = events.mapTo(LinkedHashSet()) { it.key }
        if (known.isEmpty()) return WatchOutcome(emptyList(), keys)
        val fresh = events.filter { e ->
            e.key !in known && if (e.vendor != null) {
                Advisories.isRelevant(e.branchIds, e.vendor, relevant, vendors)
            } else {
                e.branchIds.any { it in relevant }
            }
        }
        return WatchOutcome(fresh, keys)
    }
}
