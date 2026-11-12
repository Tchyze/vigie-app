package fr.tchyze.vigie.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.tchyze.vigie.data.model.BranchStatus
import fr.tchyze.vigie.ui.common.containerFactory
import fr.tchyze.vigie.ui.common.formatDate

/** Nombre de versions récentes proposées en raccourci sous le champ « Version installée ». */
private const val VERSION_SUGGESTIONS = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEditScreen(
    deviceId: Long,
    onBack: () -> Unit,
    onOpenBranch: (String) -> Unit,
    viewModel: DeviceEditViewModel = viewModel(
        key = "device-$deviceId",
        factory = containerFactory { DeviceEditViewModel(it.repository, it.inventory, deviceId) },
    ),
) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val form = viewModel.form
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.done) { if (viewModel.done) onBack() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (viewModel.isNew) "Nouvel équipement" else "Équipement") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
            },
            actions = {
                if (!viewModel.isNew) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Supprimer")
                    }
                }
            },
        )
        if (form == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        val product = catalog.product(form.productId)
        val branches = catalog.branches(form.productId)
        val branch = catalog.branch(form.branchId)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = viewModel::setName,
                label = { Text("Nom") },
                placeholder = { Text("fw-siege-01") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.site,
                onValueChange = viewModel::setSite,
                label = { Text("Site (facultatif)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Picker(
                label = "Produit",
                value = product?.let { "${it.label} (${it.vendor})" } ?: form.productId.orEmpty(),
                options = catalog.products.map { it.product to "${it.label} (${it.vendor})" },
                onSelect = viewModel::selectProduct,
                enabled = catalog.products.isNotEmpty(),
            )
            if (catalog.products.isEmpty()) {
                Text(
                    "Aucune donnée chargée : actualisez depuis le fil pour choisir un produit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Picker(
                label = "Branche",
                value = branch?.label ?: form.branchId.orEmpty(),
                options = branches.map { it.branch.id to branchLabel(it) },
                onSelect = viewModel::selectBranch,
                enabled = branches.isNotEmpty(),
            )
            if (branch != null) {
                TextButton(onClick = { onOpenBranch(branch.id) }) { Text("Voir la branche") }
            }

            OutlinedTextField(
                value = form.version,
                onValueChange = viewModel::setVersion,
                label = { Text("Version installée") },
                placeholder = { branch?.latestVersion?.let { Text(it) } },
                supportingText = {
                    val latest = branch?.latestVersion
                    if (latest != null) Text("Dernière version publiée : $latest (${formatDate(branch?.latestReleaseDate)})")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            val recent = branch?.releases.orEmpty().take(VERSION_SUGGESTIONS)
            if (recent.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recent.forEach { release ->
                        SuggestionChip(
                            onClick = { viewModel.setVersion(release.version) },
                            label = { Text(release.version) },
                        )
                    }
                }
            }

            Button(
                onClick = viewModel::save,
                enabled = form.valid,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Enregistrer") }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer l'équipement ?") },
            text = { Text("« ${form?.name.orEmpty()} » sera retiré de l'inventaire.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }
}

private fun branchLabel(option: BranchOption): String {
    val b = option.branch
    val lts = if (b.lts == true) " LTS" else ""
    return when (option.status) {
        BranchStatus.ACTIVE -> "${b.label}$lts"
        BranchStatus.EOL_SOON -> "${b.label}$lts (fin de support proche)"
        BranchStatus.EOL -> "${b.label}$lts (fin de support)"
    }
}

/** Champ en lecture seule qui ouvre une liste de choix. [options] : (identifiant, libellé). */
@Composable
private fun Picker(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    enabled: Boolean,
) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Un champ en lecture seule consomme les appuis : une surface transparente les intercepte.
        Box(Modifier.matchParentSize().clickable(enabled = enabled) { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { open = false; onSelect(id) })
            }
        }
    }
}
