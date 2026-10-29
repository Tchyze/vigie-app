package fr.tchyze.vigie.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.tchyze.vigie.data.DataError
import fr.tchyze.vigie.data.DataState
import fr.tchyze.vigie.ui.theme.statusColors

/** Origine et fraîcheur des données affichées, et erreur de la dernière actualisation. */
@Composable
fun DataBanner(state: DataState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        when (val error = state.error) {
            is DataError.UpdateRequired -> Notice(
                title = error.message,
                text = "Les données publiées utilisent un format plus récent (version ${error.version}). " +
                    "Installez la dernière version de Vigie. En attendant, les dernières données connues restent affichées.",
                danger = true,
            )
            is DataError.Network -> Notice("Actualisation impossible", error.message, danger = false)
            is DataError.Invalid -> Notice("Actualisation impossible", error.message, danger = true)
            null -> Unit
        }
        Text(
            text = freshness(state),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

private fun freshness(state: DataState): String {
    val generated = state.snapshot?.index?.generatedAt
    return when {
        state.demo -> "Mode démo : données d'exemple" + (generated?.let { " (générées le ${formatTimestamp(it)})" } ?: "")
        state.fetchedAt != null -> "Dernière actualisation : ${formatDateTime(state.fetchedAt)}" +
            (generated?.let { " · publiées le ${formatTimestamp(it)}" } ?: "")
        else -> "Jamais actualisé"
    }
}

@Composable
private fun Notice(title: String, text: String, danger: Boolean) {
    val colors = statusColors
    Surface(
        color = if (danger) colors.danger else colors.warning,
        contentColor = if (danger) colors.onDanger else colors.onWarning,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}
