package fr.tchyze.vigie.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.domain.FeedItem
import fr.tchyze.vigie.ui.common.BranchStatusBadge
import fr.tchyze.vigie.ui.common.DataBanner
import fr.tchyze.vigie.ui.common.NeutralBadge
import fr.tchyze.vigie.ui.common.SecurityBadge
import fr.tchyze.vigie.ui.common.containerFactory
import fr.tchyze.vigie.ui.common.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onOpenBranch: (String) -> Unit,
    onOpenFollow: () -> Unit,
    viewModel: FeedViewModel = viewModel(factory = containerFactory { FeedViewModel(it.repository, it.follows) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Vigie") },
            actions = {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Technologies suivies") },
                        onClick = { menuOpen = false; onOpenFollow() },
                    )
                }
            },
        )
        PullToRefreshBox(
            isRefreshing = state.data.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.data.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item { DataBanner(state.data) }
                    if (state.showingAll && state.items.isNotEmpty()) {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    "Vous ne suivez encore aucune technologie : toutes les branches supportées sont affichées.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                TextButton(onClick = onOpenFollow) { Text("Choisir les technologies suivies") }
                            }
                        }
                    }
                    if (state.items.isEmpty()) {
                        item {
                            Text(
                                if (state.data.snapshot == null) {
                                    "Aucune donnée. Tirez vers le bas pour actualiser."
                                } else {
                                    "Aucune branche supportée parmi les technologies suivies."
                                },
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    items(state.items, key = { it.branch.id }) { item ->
                        FeedCard(item, onClick = { onOpenBranch(item.branch.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedCard(item: FeedItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${item.product.label} ${item.branch.label}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (item.branch.lts == true) NeutralBadge("LTS")
            }
            Text(item.product.vendor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.branch.latestVersion ?: "Version inconnue",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(12.dp))
                if (item.branch.latestVersion != null) {
                    Text(formatDate(item.latestDate), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (item.securityFix || item.status != BranchStatus.ACTIVE) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.securityFix) SecurityBadge()
                    BranchStatusBadge(item.status)
                }
            }
        }
    }
}
