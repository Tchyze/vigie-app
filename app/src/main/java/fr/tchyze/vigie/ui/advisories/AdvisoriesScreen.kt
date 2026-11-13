package fr.tchyze.vigie.ui.advisories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.tchyze.vigie.ui.common.AdvisoryRow
import fr.tchyze.vigie.ui.common.DataBanner
import fr.tchyze.vigie.ui.common.containerFactory
import fr.tchyze.vigie.ui.common.rememberOpenUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvisoriesScreen(
    viewModel: AdvisoriesViewModel = viewModel(
        factory = containerFactory { AdvisoriesViewModel(it.repository, it.follows, it.inventory) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val openUrl = rememberOpenUrl()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Avis de sécurité") })
        PullToRefreshBox(
            isRefreshing = state.data.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.data.loading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), modifier = Modifier.fillMaxSize()) {
                    item { DataBanner(state.data) }
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            if (!state.nothingFollowed) {
                                FilterChip(
                                    selected = state.onlyMine,
                                    onClick = { viewModel.setOnlyMine(!state.onlyMine) },
                                    label = { Text("Mes technologies") },
                                )
                            }
                            FilterChip(
                                selected = state.onlySevere,
                                onClick = { viewModel.setOnlySevere(!state.onlySevere) },
                                label = { Text("Critiques et élevés") },
                            )
                        }
                    }
                    if (state.nothingFollowed && state.total > 0) {
                        item {
                            Text(
                                "Aucune technologie suivie ni équipement : tous les avis sont affichés.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                    if (state.lines.isEmpty()) {
                        item {
                            Text(
                                when {
                                    state.data.snapshot == null -> "Aucune donnée. Tirez vers le bas pour actualiser."
                                    state.total == 0 -> "Aucun avis de sécurité publié."
                                    else -> "Aucun avis ne correspond aux filtres."
                                },
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    items(state.lines, key = { "${it.item.advisory.source}/${it.item.advisory.id}" }) { line ->
                        Column {
                            AdvisoryRow(line.item.advisory, openUrl)
                            if (line.branchesLabel.isNotEmpty()) {
                                Text(
                                    "Concerne : ${line.branchesLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
