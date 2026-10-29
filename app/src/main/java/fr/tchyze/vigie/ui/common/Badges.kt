package fr.tchyze.vigie.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.ui.theme.statusColors

@Composable
fun Badge(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(6.dp), modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun SecurityBadge(modifier: Modifier = Modifier) =
    Badge("Sécurité", statusColors.danger, statusColors.onDanger, modifier)

/** Rien pour une branche active ; orange ou rouge sinon. */
@Composable
fun BranchStatusBadge(status: BranchStatus, modifier: Modifier = Modifier) {
    when (status) {
        BranchStatus.ACTIVE -> Unit
        BranchStatus.EOL_SOON -> Badge("Fin de support proche", statusColors.warning, statusColors.onWarning, modifier)
        BranchStatus.EOL -> Badge("Fin de support", statusColors.danger, statusColors.onDanger, modifier)
    }
}

@Composable
fun NeutralBadge(text: String, modifier: Modifier = Modifier) =
    Badge(text, statusColors.unknown, statusColors.onUnknown, modifier)
