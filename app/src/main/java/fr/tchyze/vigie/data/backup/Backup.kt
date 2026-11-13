package fr.tchyze.vigie.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Fichier d'export de l'inventaire et des technologies suivies.
 *
 * C'est le seul moyen de changer de téléphone : la sauvegarde automatique Android est désactivée.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    @SerialName("format_version") val formatVersion: Int = VERSION,
    @SerialName("exported_at") val exportedAt: String,
    val devices: List<BackupDevice> = emptyList(),
    @SerialName("followed_products") val followedProducts: List<String> = emptyList(),
    @SerialName("followed_branches") val followedBranches: List<BackupBranch> = emptyList(),
) {
    companion object {
        const val FORMAT = "vigie-inventaire"
        const val VERSION = 1
    }
}

@Serializable
data class BackupDevice(
    val name: String,
    val site: String? = null,
    @SerialName("product_id") val productId: String,
    @SerialName("branch_id") val branchId: String,
    @SerialName("installed_version") val installedVersion: String,
)

@Serializable
data class BackupBranch(
    @SerialName("branch_id") val branchId: String,
    @SerialName("product_id") val productId: String,
)

class BackupException(message: String) : Exception(message)

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    fun decode(text: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw BackupException("Ce fichier n'est pas un export Vigie lisible.")
        }
        if (file.format != BackupFile.FORMAT) throw BackupException("Ce fichier n'est pas un export d'inventaire Vigie.")
        if (file.formatVersion > BackupFile.VERSION) {
            throw BackupException("Cet export vient d'une version plus récente de Vigie : mettez l'application à jour.")
        }
        val invalid = file.devices.firstOrNull {
            it.name.isBlank() || it.productId.isBlank() || it.branchId.isBlank() || it.installedVersion.isBlank()
        }
        if (invalid != null) throw BackupException("Équipement incomplet dans l'export : « ${invalid.name} ».")
        return file
    }
}
