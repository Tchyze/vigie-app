package fr.tchyze.vigie.ui.branch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tchyze.vigie.data.VigieRepository
import fr.tchyze.vigie.data.model.Advisory
import fr.tchyze.vigie.data.model.Branch
import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.data.model.ProductFile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface BranchUiState {
    data object Loading : BranchUiState

    /** La branche n'existe plus dans les données (retirée par le collecteur). */
    data object Missing : BranchUiState

    data class Ready(
        val branch: Branch,
        val product: ProductFile,
        val status: BranchStatus,
        val advisories: List<Advisory>,
    ) : BranchUiState
}

class BranchViewModel(repository: VigieRepository, private val branchId: String) : ViewModel() {

    val state: StateFlow<BranchUiState> = repository.state
        .map { data ->
            val snapshot = data.snapshot ?: return@map if (data.loading) BranchUiState.Loading else BranchUiState.Missing
            val branch = snapshot.branch(branchId) ?: return@map BranchUiState.Missing
            BranchUiState.Ready(
                branch = branch,
                product = snapshot.product(branch.product) ?: return@map BranchUiState.Missing,
                status = snapshot.statusOf(branch),
                advisories = snapshot.advisories.advisories
                    .filter { branchId in it.affected }
                    .sortedByDescending { it.published },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BranchUiState.Loading)
}
