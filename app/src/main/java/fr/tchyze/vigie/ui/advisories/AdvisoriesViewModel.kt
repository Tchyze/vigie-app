package fr.tchyze.vigie.ui.advisories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tchyze.vigie.data.DataState
import fr.tchyze.vigie.data.FollowRepository
import fr.tchyze.vigie.data.InventoryRepository
import fr.tchyze.vigie.data.VigieRepository
import fr.tchyze.vigie.domain.Advisories
import fr.tchyze.vigie.domain.AdvisoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdvisoryLine(val item: AdvisoryItem, val branchesLabel: String)

data class AdvisoriesUiState(
    val data: DataState = DataState(),
    val lines: List<AdvisoryLine> = emptyList(),
    /** Aucune branche suivie ni équipement : le filtre « Mes technologies » est sans objet. */
    val nothingFollowed: Boolean = true,
    val onlyMine: Boolean = true,
    val onlySevere: Boolean = false,
    val total: Int = 0,
)

private data class Filters(val onlyMine: Boolean = true, val onlySevere: Boolean = false)

class AdvisoriesViewModel(
    private val repository: VigieRepository,
    follows: FollowRepository,
    inventory: InventoryRepository,
) : ViewModel() {

    private val filters = MutableStateFlow(Filters())

    val state: StateFlow<AdvisoriesUiState> =
        combine(repository.state, follows.follows, inventory.devices, filters) { data, follows, devices, f ->
            val snapshot = data.snapshot ?: return@combine AdvisoriesUiState(data = data, onlyMine = f.onlyMine, onlySevere = f.onlySevere)
            val relevant = Advisories.relevantBranches(snapshot, follows, devices)
            val all = Advisories.build(snapshot, relevant)
            val nothingFollowed = relevant.isEmpty()
            val lines = all
                .filter { !f.onlyMine || nothingFollowed || it.relevant }
                .filter { !f.onlySevere || it.severe }
                .map { AdvisoryLine(it, Advisories.branchesLabel(it, snapshot)) }
            AdvisoriesUiState(data, lines, nothingFollowed, f.onlyMine, f.onlySevere, all.size)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdvisoriesUiState())

    fun setOnlyMine(value: Boolean) = filters.update { it.copy(onlyMine = value) }

    fun setOnlySevere(value: Boolean) = filters.update { it.copy(onlySevere = value) }

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }
}
