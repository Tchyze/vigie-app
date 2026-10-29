package fr.tchyze.vigie.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tchyze.vigie.data.DataState
import fr.tchyze.vigie.data.VigieRepository
import fr.tchyze.vigie.domain.Feed
import fr.tchyze.vigie.domain.FeedItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FeedUiState(
    val data: DataState = DataState(),
    val items: List<FeedItem> = emptyList(),
    /** Aucune technologie choisie : toutes les branches supportées sont affichées. */
    val showingAll: Boolean = true,
)

class FeedViewModel(private val repository: VigieRepository) : ViewModel() {

    val state: StateFlow<FeedUiState> = repository.state
        .map { data ->
            val snapshot = data.snapshot
            FeedUiState(
                data = data,
                items = if (snapshot == null) emptyList() else Feed.build(snapshot, { true }),
                showingAll = true,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState())

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }
}
