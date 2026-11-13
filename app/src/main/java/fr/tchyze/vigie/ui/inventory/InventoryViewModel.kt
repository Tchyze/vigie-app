package fr.tchyze.vigie.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tchyze.vigie.data.DataState
import fr.tchyze.vigie.data.InventoryRepository
import fr.tchyze.vigie.data.VigieRepository
import fr.tchyze.vigie.domain.DeviceCheck
import fr.tchyze.vigie.domain.Inventory
import fr.tchyze.vigie.domain.InventorySummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventoryUiState(
    val data: DataState = DataState(),
    /** Vrai tant que la base locale ou les données publiées ne sont pas chargées. */
    val loading: Boolean = true,
    val checks: List<DeviceCheck> = emptyList(),
    val summary: InventorySummary = InventorySummary(0, 0, 0, 0),
)

class InventoryViewModel(
    private val repository: VigieRepository,
    inventory: InventoryRepository,
) : ViewModel() {

    val state: StateFlow<InventoryUiState> = combine(repository.state, inventory.devices) { data, devices ->
        val checks = Inventory.build(data.snapshot, devices)
        InventoryUiState(data = data, loading = data.loading, checks = checks, summary = Inventory.summary(checks))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }
}
