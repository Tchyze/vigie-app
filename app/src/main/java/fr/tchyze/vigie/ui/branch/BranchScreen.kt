package fr.tchyze.vigie.ui.branch

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.tchyze.vigie.data.model.Eol
import fr.tchyze.vigie.data.model.EolKeys
import fr.tchyze.vigie.data.model.Release
import fr.tchyze.vigie.data.model.parseDate
import fr.tchyze.vigie.ui.common.AdvisoryRow
import fr.tchyze.vigie.ui.common.BranchStatusBadge
import fr.tchyze.vigie.ui.common.NeutralBadge
import fr.tchyze.vigie.ui.common.SecurityBadge
import fr.tchyze.vigie.ui.common.containerFactory
import fr.tchyze.vigie.ui.common.formatDate
import fr.tchyze.vigie.ui.common.rememberOpenUrl
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchScreen(
    branchId: String,
    onBack: () -> Unit,
    viewModel: BranchViewModel = viewModel(
        key = "branch-$branchId",
        factory = containerFactory { BranchViewModel(it.repository, branchId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val openUrl = rememberOpenUrl()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text((state as? BranchUiState.Ready)?.let { "${it.product.label} ${it.branch.label}" } ?: "Branche")
            },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
            },
        )
        when (val s = state) {
            BranchUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            BranchUiState.Missing -> Text(
                "Cette branche ne figure plus dans les données publiées.",
                modifier = Modifier.padding(16.dp),
            )
            is BranchUiState.Ready -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item { Header(s) }
                section("Fin de support")
                item { EolSection(s.branch.eol, openUrl) }
                section("Versions")
                releases(s.branch.releases, openUrl)
                if (s.advisories.isNotEmpty()) {
                    section("Avis de sécurité")
                    items(s.advisories, key = { "${it.source}/${it.id}" }) { AdvisoryRow(it, openUrl) }
                }
                item {
                    Text(
                        "Sources : " + s.branch.sources.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

private fun LazyListScope.section(title: String) = item {
    Column {
        HorizontalDivider(Modifier.padding(top = 8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun Header(s: BranchUiState.Ready) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(s.product.vendor, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            s.branch.latestVersion?.let { "Dernière version : $it" } ?: "Dernière version inconnue",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (s.branch.latestVersion != null) Text("Publiée le ${formatDate(s.branch.latestReleaseDate)}")
        s.branch.releaseDate?.let { Text("Branche sortie le ${formatDate(it)}", style = MaterialTheme.typography.bodySmall) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BranchStatusBadge(s.status)
            if (s.branch.lts == true) NeutralBadge("LTS")
        }
    }
}

@Composable
private fun EolSection(eol: Eol, openUrl: (String) -> Unit) {
    val today = LocalDate.now()
    val dates = listOf(
        EolKeys.ACTIVE_SUPPORT to eol.endOfActiveSupport,
        EolKeys.SECURITY_SUPPORT to eol.endOfSecuritySupport,
        EolKeys.END_OF_LIFE to eol.endOfLife,
        EolKeys.EXTENDED_SUPPORT to eol.endOfExtendedSupport,
    )
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        dates.forEach { (key, value) ->
            val isTrigger = key == eol.trigger
            if (value == null && !isTrigger && key !in eol.labels) return@forEach
            val date = parseDate(value)
            Column {
                Row {
                    Text(
                        EolKeys.label(key),
                        fontWeight = if (isTrigger) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        when {
                            value == null -> "non publiée"
                            date != null && !date.isAfter(today) -> "${formatDate(date)} (passée)"
                            else -> formatDate(date)
                        },
                        fontWeight = if (isTrigger) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                val notes = listOfNotNull(
                    eol.labels[key]?.let { "« $it » chez la source" },
                    "détermine le statut".takeIf { isTrigger },
                )
                if (notes.isNotEmpty()) {
                    Text(
                        notes.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (eol.declaredEol) Text("La source déclare cette branche en fin de vie.", fontWeight = FontWeight.Bold)
        val source = eol.sourceUrl
        if (source != null) {
            LinkText("Source : ${source.removePrefix("https://").removePrefix("http://")}") { openUrl(source) }
        } else {
            Text("Source des dates non précisée", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun LazyListScope.releases(releases: List<Release>, openUrl: (String) -> Unit) {
    if (releases.isEmpty()) {
        item {
            Text("Aucune version publiée par les sources pour cette branche.", modifier = Modifier.padding(horizontal = 16.dp))
        }
        return
    }
    items(releases, key = { "release/${it.version}" }) { release ->
        val url = release.url
        Column(
            Modifier.fillMaxWidth()
                .then(if (url != null) Modifier.clickable { openUrl(url) } else Modifier)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(release.version, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(formatDate(release.date), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (release.securityFix == true) SecurityBadge()
                if (url != null) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Note de version", tint = MaterialTheme.colorScheme.primary)
                }
            }
            release.summary?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
    )
}
