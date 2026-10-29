package fr.tchyze.vigie.ui.follow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.tchyze.vigie.domain.CatalogBranch
import fr.tchyze.vigie.domain.CatalogProduct
import fr.tchyze.vigie.ui.common.BranchStatusBadge
import fr.tchyze.vigie.ui.common.containerFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowScreen(
    onBack: () -> Unit,
    viewModel: FollowViewModel = viewModel(factory = containerFactory { FollowViewModel(it.repository, it.follows) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Catégories ouvertes par défaut ; produits ouverts seulement à la demande.
    val collapsedCategories = remember { mutableStateMapOf<String, Boolean>() }
    val expandedProducts = remember { mutableStateMapOf<String, Boolean>() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Technologies suivies") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
            },
        )
        if (state.loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        if (state.categories.isEmpty()) {
            Text("Aucune donnée : actualisez le fil d'abord.", Modifier.padding(16.dp))
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text(
                    "Cochez un produit pour suivre toutes ses branches supportées, y compris les futures, " +
                        "ou dépliez-le pour choisir des branches précises.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            state.categories.forEach { category ->
                val key = category.category.key
                val open = collapsedCategories[key] != true
                item(key = "cat/$key") {
                    ExpandableHeader(
                        title = category.category.label,
                        expanded = open,
                        onToggle = { collapsedCategories[key] = open },
                    )
                }
                if (open) {
                    category.products.forEach { product ->
                        val id = product.product.product
                        val expanded = expandedProducts[id] ?: false
                        item(key = "prod/$id") {
                            ProductRow(
                                product = product,
                                expanded = expanded,
                                onToggleExpand = { expandedProducts[id] = !expanded },
                                onCheck = { viewModel.setProduct(id, !product.wholeProduct) },
                            )
                        }
                        if (expanded) {
                            items(product.branches, key = { "branch/${it.branch.id}" }) { branch ->
                                BranchRow(
                                    branch = branch,
                                    wholeProduct = product.wholeProduct,
                                    onCheck = { viewModel.setBranch(branch.branch.id, id, !branch.explicit) },
                                )
                            }
                            item(key = "foot/$id") { ProductFooter(product) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Column {
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = if (expanded) "Replier" else "Déplier")
        }
    }
}

@Composable
private fun ProductRow(product: CatalogProduct, expanded: Boolean, onToggleExpand: () -> Unit, onCheck: () -> Unit) {
    val checkState = when {
        product.wholeProduct -> ToggleableState.On
        product.followedBranchCount > 0 -> ToggleableState.Indeterminate
        else -> ToggleableState.Off
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggleExpand).padding(start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(state = checkState, onClick = onCheck)
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(product.product.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                when {
                    product.wholeProduct -> "${product.product.vendor} · produit entier suivi"
                    product.followedBranchCount > 0 -> "${product.product.vendor} · ${product.followedBranchCount} branche(s) suivie(s)"
                    else -> product.product.vendor
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = if (expanded) "Replier" else "Voir les branches")
    }
}

@Composable
private fun BranchRow(branch: CatalogBranch, wholeProduct: Boolean, onCheck: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (wholeProduct) Modifier else Modifier.clickable(onClick = onCheck))
            .padding(start = 40.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avec le produit entier suivi, les branches sont cochées d'office.
        Checkbox(checked = branch.followed, onCheckedChange = { onCheck() }, enabled = !wholeProduct)
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(branch.branch.label)
            branch.branch.latestVersion?.let {
                Text("Dernière version : $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        BranchStatusBadge(branch.status)
    }
}

@Composable
private fun ProductFooter(product: CatalogProduct) {
    val text = buildList {
        if (product.branches.isEmpty()) add("Aucune branche supportée publiée pour l'instant.")
        if (product.eolCount > 0) add("${product.eolCount} branche(s) en fin de support, visibles dans « Caché / EOL ».")
    }
    if (text.isEmpty()) return
    Text(
        text.joinToString(" "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
    )
}
