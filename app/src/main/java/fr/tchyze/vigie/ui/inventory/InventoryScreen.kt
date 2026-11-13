package fr.tchyze.vigie.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.domain.DeviceCheck
import fr.tchyze.vigie.domain.InventorySummary
import fr.tchyze.vigie.domain.Verdict
import fr.tchyze.vigie.domain.VersionState
import fr.tchyze.vigie.ui.common.Badge
import fr.tchyze.vigie.ui.common.BranchStatusBadge
import fr.tchyze.vigie.ui.common.DataBanner
import fr.tchyze.vigie.ui.common.NeutralBadge
import fr.tchyze.vigie.ui.common.containerFactory
import fr.tchyze.vigie.ui.common.formatDate
import fr.tchyze.vigie.ui.theme.statusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onAdd: () -> Unit,
    onOpenDevice: (Long) -> Unit,
    viewModel: InventoryViewModel = viewModel(
        factory = containerFactory { InventoryViewModel(it.repository, it.inventory) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Inventaire") })
        Box(Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = state.data.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.loading) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn(
                        // Place libre en bas pour que le bouton d'ajout ne masque pas la dernière carte.
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item { DataBanner(state.data) }
                        if (state.checks.isEmpty()) {
                            item { EmptyInventory() }
                        } else {
                            item { SummaryLine(state.summary, state.checks.size) }
                        }
                        items(state.checks, key = { it.device.id }) { check ->
                            DeviceCard(check, onClick = { onOpenDevice(check.device.id) })
                        }
                    }
                }
            }
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Ajouter") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }
}

@Composable
private fun EmptyInventory() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Aucun équipement", style = MaterialTheme.typography.titleMedium)
        Text(
            "Ajoutez vos firewalls, switchs et serveurs avec leur version installée : " +
                "Vigie indique ceux qui ont une mise à jour en attente ou qui ne sont plus supportés.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "L'inventaire reste uniquement sur ce téléphone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryLine(summary: InventorySummary, total: Int) {
    val parts = buildList {
        if (summary.toFix > 0) add("${summary.toFix} à traiter")
        if (summary.toWatch > 0) add("${summary.toWatch} à mettre à jour ou à surveiller")
        if (summary.upToDate > 0) add("${summary.upToDate} à jour")
        if (summary.unverified > 0) add("${summary.unverified} à vérifier")
        if (summary.unknown > 0) add("${summary.unknown} sans conclusion")
    }
    Text(
        (if (total > 1) "$total équipements" else "1 équipement") + " : " + parts.joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun DeviceCard(check: DeviceCheck, onClick: () -> Unit) {
    val device = check.device
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                VerdictBadge(check.verdict)
            }
            Text(
                listOfNotNull(device.site, technology(check)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Installée : ${device.installedVersion}" +
                    (check.branch?.latestVersion?.let { " · Dernière : $it" } ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            detail(check)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            // Le badge principal dit déjà « hors support » ou « fin proche » quand c'est le verdict.
            if (check.status == BranchStatus.EOL_SOON && check.verdict != Verdict.EOL_SOON) {
                FlowRow { BranchStatusBadge(check.status) }
            }
        }
    }
}

/** « FortiOS 7.4 », ou les identifiants bruts si les données ne connaissent pas l'équipement. */
fun technology(check: DeviceCheck): String {
    val product = check.product?.label ?: check.device.productId
    val branch = check.branch?.label ?: check.device.branchId
    return if (check.branch == null) "$product · $branch" else "$product $branch"
}

private fun detail(check: DeviceCheck): String? = when {
    check.branch == null -> "Cette branche ne figure pas dans les données publiées."
    check.versionState == VersionState.UNKNOWN -> when (check.status) {
        BranchStatus.EOL -> "La branche n'est plus supportée."
        else -> "Branche supportée, mais la source ne publie pas ses versions : vérifiez la dernière " +
            "version auprès de l'éditeur." + endOfSupport(check)
    }
    check.versionState == VersionState.BEHIND -> {
        val n = check.missed.size
        when {
            n == 0 -> "Une version plus récente est disponible."
            n == 1 -> "1 version de retard."
            else -> "$n versions de retard."
        } + if (check.securityMissed) " Au moins une corrige une faille de sécurité." else ""
    }
    check.status == BranchStatus.EOL -> "À jour, mais la branche n'est plus supportée."
    else -> null
}

private fun endOfSupport(check: DeviceCheck): String =
    check.branch?.eol?.triggerDate?.let { " Fin de support : ${formatDate(it)}." } ?: ""

@Composable
fun VerdictBadge(verdict: Verdict, modifier: Modifier = Modifier) {
    val c = statusColors
    when (verdict) {
        Verdict.UP_TO_DATE -> Badge("À jour", c.ok, c.onOk, modifier)
        Verdict.UNVERIFIED -> NeutralBadge("À vérifier", modifier)
        Verdict.UNKNOWN -> NeutralBadge("Inconnu", modifier)
        Verdict.EOL_SOON -> Badge("Fin de support proche", c.warning, c.onWarning, modifier)
        Verdict.UPDATE -> Badge("Mise à jour", c.warning, c.onWarning, modifier)
        Verdict.SECURITY_UPDATE -> Badge("Sécurité", c.danger, c.onDanger, modifier)
        Verdict.EOL -> Badge("Hors support", c.danger, c.onDanger, modifier)
    }
}
