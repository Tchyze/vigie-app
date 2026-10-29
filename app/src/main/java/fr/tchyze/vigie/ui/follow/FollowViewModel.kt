package fr.tchyze.vigie.ui.follow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tchyze.vigie.data.FollowRepository
import fr.tchyze.vigie.data.VigieRepository
import fr.tchyze.vigie.domain.CatalogCategory
import fr.tchyze.vigie.domain.FollowCatalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FollowUiState(
    val loading: Boolean = true,
    val categories: List<CatalogCategory> = emptyList(),
)

class FollowViewModel(
    repository: VigieRepository,
    private val follows: FollowRepository,
) : ViewModel() {

    val state: StateFlow<FollowUiState> = combine(repository.state, follows.follows) { data, follows ->
        FollowUiState(
            loading = data.loading,
            categories = data.snapshot?.let { FollowCatalog.build(it, follows) }.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FollowUiState())

    fun setProduct(productId: String, followed: Boolean) {
        viewModelScope.launch { follows.setProductFollowed(productId, followed) }
    }

    fun setBranch(branchId: String, productId: String, followed: Boolean) {
        viewModelScope.launch { follows.setBranchFollowed(branchId, productId, followed) }
    }
}
