package fr.tchyze.vigie.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.tchyze.vigie.data.model.Advisory
import fr.tchyze.vigie.data.model.Severity
import fr.tchyze.vigie.ui.theme.statusColors

@Composable
fun SeverityBadge(severity: Severity?, cvss: Double?) {
    val colors = statusColors
    val label = (severity?.label ?: "Sévérité non précisée") + (cvss?.let { " · CVSS $it" } ?: "")
    when (severity) {
        Severity.CRITICAL, Severity.HIGH -> Badge(label, colors.danger, colors.onDanger)
        Severity.MEDIUM -> Badge(label, colors.warning, colors.onWarning)
        else -> Badge(label, colors.unknown, colors.onUnknown)
    }
}

/** Un avis de sécurité ; le toucher ouvre l'avis officiel. */
@Composable
fun AdvisoryRow(advisory: Advisory, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clickable { onOpen(advisory.url) }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SeverityBadge(Severity.of(advisory.severity), advisory.cvss)
            Text(advisory.id, style = MaterialTheme.typography.labelLarge)
        }
        Text(advisory.title, style = MaterialTheme.typography.bodyLarge)
        advisory.summary?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        val meta = buildList {
            add("${advisory.vendor} · ${formatTimestamp(advisory.published)}")
            if (advisory.cves.isNotEmpty()) add(advisory.cves.joinToString(", "))
        }
        meta.forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        advisory.attribution?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
