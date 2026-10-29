package fr.tchyze.vigie.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Format publié par vigie-collector (SCHEMA.md, schema_version 1). Les noms JSON sont repris tels
// quels via @SerialName. Les valeurs énumérées restent des chaînes : une valeur inconnue ne doit pas
// empêcher la lecture du reste.

@Serializable
data class IndexFile(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("eol_soon_months") val eolSoonMonths: Int,
    val products: List<ProductEntry>,
    val advisories: AdvisoriesEntry,
    val sources: List<SourceState>,
)

@Serializable
data class ProductEntry(
    val id: String,
    val label: String,
    val vendor: String,
    val category: String,
    val file: String,
    @SerialName("branch_count") val branchCount: Int,
    @SerialName("status_counts") val statusCounts: StatusCounts,
)

@Serializable
data class StatusCounts(
    val active: Int,
    @SerialName("eol_soon") val eolSoon: Int,
    val eol: Int,
)

@Serializable
data class AdvisoriesEntry(
    val file: String,
    val count: Int,
)

@Serializable
data class SourceState(
    val name: String,
    val label: String,
    val kind: String,
    val status: String,
    val count: Int,
    @SerialName("last_attempt") val lastAttempt: String,
    @SerialName("last_success") val lastSuccess: String?,
    val error: String?,
    val home: String,
    val attribution: String?,
)

@Serializable
data class ProductFile(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("generated_at") val generatedAt: String,
    val product: String,
    val label: String,
    val vendor: String,
    val category: String,
    val branches: List<Branch>,
)

@Serializable
data class Branch(
    val id: String,
    val product: String,
    val vendor: String,
    val category: String,
    val branch: String,
    val label: String,
    val lts: Boolean?,
    @SerialName("release_date") val releaseDate: String?,
    val status: String,
    @SerialName("latest_version") val latestVersion: String?,
    @SerialName("latest_release_date") val latestReleaseDate: String?,
    val eol: Eol,
    val releases: List<Release>,
    val sources: List<String>,
)

@Serializable
data class Eol(
    @SerialName("end_of_active_support") val endOfActiveSupport: String?,
    @SerialName("end_of_security_support") val endOfSecuritySupport: String?,
    @SerialName("end_of_life") val endOfLife: String?,
    @SerialName("end_of_extended_support") val endOfExtendedSupport: String?,
    val trigger: String,
    @SerialName("trigger_date") val triggerDate: String?,
    @SerialName("declared_eol") val declaredEol: Boolean,
    val labels: Map<String, String>,
    @SerialName("source_url") val sourceUrl: String?,
)

@Serializable
data class Release(
    val version: String,
    val date: String?,
    @SerialName("security_fix") val securityFix: Boolean?,
    val summary: String?,
    val url: String?,
)

@Serializable
data class AdvisoriesFile(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("retention_days") val retentionDays: Int,
    val advisories: List<Advisory>,
)

@Serializable
data class Advisory(
    val id: String,
    val source: String,
    val vendor: String,
    val title: String,
    val published: String,
    val updated: String?,
    val severity: String?,
    val cvss: Double?,
    val cves: List<String>,
    val url: String,
    val summary: String?,
    val affected: List<String>,
    val attribution: String?,
)
