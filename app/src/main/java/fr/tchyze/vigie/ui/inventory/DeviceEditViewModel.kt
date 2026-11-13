package fr.tchyze.vigie.ui.inventory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tchyze.vigie.data.InventoryRepository
import fr.tchyze.vigie.data.VigieRepository
import fr.tchyze.vigie.data.model.Branch
import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.data.model.ProductFile
import fr.tchyze.vigie.data.model.Snapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Saisie en cours. Les champs texte restent en état Compose pour ne pas perdre le curseur. */
data class DeviceForm(
    val name: String = "",
    val site: String = "",
    val productId: String? = null,
    val branchId: String? = null,
    val version: String = "",
) {
    val valid: Boolean
        get() = name.isNotBlank() && productId != null && branchId != null && version.isNotBlank()
}

data class BranchOption(val branch: Branch, val status: BranchStatus)

/** Produits et branches proposés, tirés des données publiées. */
data class DeviceCatalog(val snapshot: Snapshot? = null) {
    val products: List<ProductFile> = snapshot?.products?.sortedBy { it.label.lowercase() }.orEmpty()

    fun product(id: String?): ProductFile? = id?.let { snapshot?.product(it) }

    /** Toutes les branches, EOL comprises : un équipement existant peut être resté sur une vieille branche. */
    fun branches(productId: String?): List<BranchOption> =
        product(productId)?.branches?.map { BranchOption(it, snapshot!!.statusOf(it)) }.orEmpty()

    fun branch(id: String?): Branch? = id?.let { snapshot?.branch(it) }
}

class DeviceEditViewModel(
    repository: VigieRepository,
    private val inventory: InventoryRepository,
    private val deviceId: Long,
) : ViewModel() {

    val isNew: Boolean = deviceId == 0L

    /** Null pendant le chargement d'un équipement existant. */
    var form by mutableStateOf(if (isNew) DeviceForm() else null)
        private set

    /** Vrai une fois l'équipement enregistré ou supprimé (ou introuvable) : l'écran se ferme. */
    var done by mutableStateOf(false)
        private set

    val catalog: StateFlow<DeviceCatalog> = repository.state
        .map { DeviceCatalog(it.snapshot) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceCatalog())

    init {
        if (!isNew) {
            viewModelScope.launch {
                val device = inventory.get(deviceId)
                if (device == null) {
                    done = true
                } else {
                    form = DeviceForm(
                        name = device.name,
                        site = device.site.orEmpty(),
                        productId = device.productId,
                        branchId = device.branchId,
                        version = device.installedVersion,
                    )
                }
            }
        }
    }

    private fun edit(transform: (DeviceForm) -> DeviceForm) {
        form = form?.let(transform)
    }

    fun setName(value: String) = edit { it.copy(name = value) }

    fun setSite(value: String) = edit { it.copy(site = value) }

    fun setVersion(value: String) = edit { it.copy(version = value) }

    fun selectProduct(productId: String) = edit {
        if (it.productId == productId) {
            it
        } else {
            // Une seule branche encore supportée : on la présélectionne.
            val supported = catalog.value.branches(productId).filter { o -> o.status != BranchStatus.EOL }
            it.copy(productId = productId, branchId = supported.singleOrNull()?.branch?.id)
        }
    }

    fun selectBranch(branchId: String) = edit { it.copy(branchId = branchId) }

    fun save() {
        val f = form ?: return
        if (!f.valid) return
        viewModelScope.launch {
            inventory.save(deviceId, f.name, f.site, f.productId!!, f.branchId!!, f.version)
            done = true
        }
    }

    fun delete() {
        if (isNew) return
        viewModelScope.launch {
            inventory.delete(deviceId)
            done = true
        }
    }
}
