package fr.tchyze.vigie.domain

import fr.tchyze.vigie.data.db.Device
import fr.tchyze.vigie.data.model.Branch
import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.data.model.ProductFile
import fr.tchyze.vigie.data.model.Release
import fr.tchyze.vigie.data.model.Snapshot
import java.time.LocalDate

/** Version installée comparée à la dernière version publiée de la branche. */
enum class VersionState { UP_TO_DATE, BEHIND, UNKNOWN }

/** Ce qu'il faut faire d'un équipement, du moins urgent au plus urgent. */
enum class Verdict {
    /** À jour sur une branche supportée. */
    UP_TO_DATE,

    /**
     * Branche supportée, mais la source ne publie pas ses versions (Cisco IOS XE, Stormshield…) :
     * la version installée est à vérifier auprès de l'éditeur.
     */
    UNVERIFIED,

    /** Branche absente des données : impossible de conclure. */
    UNKNOWN,

    /** À jour, mais la branche arrive en fin de support : prévoir une montée de branche. */
    EOL_SOON,

    /** Des versions plus récentes existent, sans correctif de sécurité signalé. */
    UPDATE,

    /** Au moins une version plus récente corrige une faille de sécurité. */
    SECURITY_UPDATE,

    /** La branche n'est plus supportée : changer de branche. */
    EOL,
}

data class DeviceCheck(
    val device: Device,
    val product: ProductFile?,
    /** Null si la branche ne figure pas (ou plus) dans les données publiées. */
    val branch: Branch?,
    val status: BranchStatus?,
    val versionState: VersionState,
    /** Versions publiées plus récentes que la version installée, de la plus récente à la plus ancienne. */
    val missed: List<Release>,
) {
    val securityMissed: Boolean get() = missed.any { it.securityFix == true }

    val verdict: Verdict
        get() = when {
            branch == null -> Verdict.UNKNOWN
            status == BranchStatus.EOL -> Verdict.EOL
            securityMissed -> Verdict.SECURITY_UPDATE
            versionState == VersionState.BEHIND -> Verdict.UPDATE
            status == BranchStatus.EOL_SOON -> Verdict.EOL_SOON
            versionState == VersionState.UNKNOWN -> Verdict.UNVERIFIED
            else -> Verdict.UP_TO_DATE
        }
}

/** Nombre d'équipements par niveau d'urgence, pour l'en-tête de l'inventaire. */
data class InventorySummary(val toFix: Int, val toWatch: Int, val upToDate: Int, val unverified: Int, val unknown: Int)

object Inventory {

    fun check(device: Device, snapshot: Snapshot?, today: LocalDate = LocalDate.now()): DeviceCheck {
        val product = snapshot?.product(device.productId)
        val branch = snapshot?.branch(device.branchId)
            ?: return DeviceCheck(device, product, null, null, VersionState.UNKNOWN, emptyList())
        val installed = device.installedVersion.trim()
        val latest = branch.latestVersion
        val state = when {
            latest == null || installed.isEmpty() -> VersionState.UNKNOWN
            Versions.compare(installed, latest) >= 0 -> VersionState.UP_TO_DATE
            else -> VersionState.BEHIND
        }
        val missed = if (state == VersionState.BEHIND) {
            branch.releases.filter { Versions.compare(it.version, installed) > 0 }
        } else {
            emptyList()
        }
        return DeviceCheck(device, product, branch, snapshot.statusOf(branch, today), state, missed)
    }

    /** Les équipements les plus urgents d'abord, puis par nom. */
    fun build(snapshot: Snapshot?, devices: List<Device>, today: LocalDate = LocalDate.now()): List<DeviceCheck> =
        devices.map { check(it, snapshot, today) }
            .sortedWith(compareByDescending<DeviceCheck> { it.verdict }.thenBy { it.device.name.lowercase() })

    fun summary(checks: List<DeviceCheck>): InventorySummary {
        val counts = checks.groupingBy { it.verdict }.eachCount()
        fun n(vararg verdicts: Verdict) = verdicts.sumOf { counts[it] ?: 0 }
        return InventorySummary(
            toFix = n(Verdict.EOL, Verdict.SECURITY_UPDATE),
            toWatch = n(Verdict.UPDATE, Verdict.EOL_SOON),
            upToDate = n(Verdict.UP_TO_DATE),
            unverified = n(Verdict.UNVERIFIED),
            unknown = n(Verdict.UNKNOWN),
        )
    }
}
