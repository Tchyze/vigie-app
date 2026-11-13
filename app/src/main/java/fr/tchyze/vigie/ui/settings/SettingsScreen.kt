package fr.tchyze.vigie.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.tchyze.vigie.BuildConfig
import fr.tchyze.vigie.data.settings.Settings
import fr.tchyze.vigie.ui.common.DataBanner
import fr.tchyze.vigie.ui.common.containerFactory
import fr.tchyze.vigie.ui.common.formatTimestamp
import fr.tchyze.vigie.ui.common.rememberOpenUrl
import java.time.LocalDate

private const val REPO_URL = "https://github.com/Tchyze/vigie-app"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenFollow: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = containerFactory { SettingsViewModel(it) }),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val data by viewModel.data.collectAsStateWithLifecycle()
    val deviceCount by viewModel.deviceCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val openUrl = rememberOpenUrl()
    val snackbar = remember { SnackbarHostState() }

    var canNotify by remember { mutableStateOf(viewModel.canNotify()) }
    LifecycleResumeEffect(Unit) {
        canNotify = viewModel.canNotify()
        onPauseOrDispose { }
    }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = viewModel.canNotify()
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.export(context.contentResolver, uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(context.contentResolver, uri)
    }

    val message = viewModel.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(title = { Text("Réglages") })
            val s = settings
            if (s != null) Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Section("Données")
                DataBanner(data)
                SwitchRow(
                    title = "Mode démo",
                    text = "Données d'exemple embarquées dans l'application, sans connexion. " +
                        "Désactivez-le pour télécharger les données publiées par le collecteur.",
                    checked = s.demoMode,
                    onChange = viewModel::setDemoMode,
                )
                BaseUrlField(s, viewModel)
                Row(Modifier.padding(horizontal = 16.dp)) {
                    OutlinedButton(onClick = viewModel::refresh, enabled = !data.refreshing) {
                        Text(if (data.refreshing) "Actualisation…" else "Actualiser maintenant")
                    }
                }

                Section("Actualisation automatique")
                Text(
                    "Fréquence de vérification en arrière-plan, quand le téléphone est connecté.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                    Settings.REFRESH_CHOICES.forEach { hours ->
                        FilterChip(
                            selected = s.refreshHours == hours,
                            onClick = { viewModel.setRefreshHours(hours) },
                            label = { Text(if (hours == 1) "1 heure" else "$hours heures") },
                        )
                    }
                }

                Section("Notifications")
                SwitchRow(
                    title = "Nouveautés suivies",
                    text = "Nouvelles versions, avis de sécurité et fins de support des technologies suivies " +
                        "et des branches de l'inventaire.",
                    checked = s.notifications,
                    onChange = viewModel::setNotifications,
                )
                if (s.notifications && !canNotify) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "Les notifications sont bloquées pour Vigie.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            TextButton(onClick = { askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text("Autoriser les notifications")
                            }
                        }
                    }
                }

                Section("Suivi et inventaire")
                LinkRow("Technologies suivies", "Produits et branches affichés dans le fil", onOpenFollow)
                Text(
                    "L'inventaire ($deviceCount équipement${if (deviceCount > 1) "s" else ""}) reste sur ce téléphone, " +
                        "sans sauvegarde automatique. Exportez-le pour le conserver ou changer de téléphone ; " +
                        "l'export contient aussi les technologies suivies.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
                    OutlinedButton(onClick = { exportLauncher.launch("vigie-inventaire-${LocalDate.now()}.json") }) {
                        Text("Exporter")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
                        Text("Importer")
                    }
                }

                data.snapshot?.index?.sources?.takeIf { it.isNotEmpty() }?.let { sources ->
                    Section("Sources des données")
                    sources.forEach { src ->
                        Column(
                            Modifier.fillMaxWidth().clickable { openUrl(src.home) }.padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(src.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                when (src.status) {
                                    "ok" -> "À jour"
                                    "error" -> "En erreur"
                                    else -> src.status
                                } + " · dernière réussite : " + formatTimestamp(src.lastSuccess),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            src.attribution?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Section("À propos")
                LinkRow("Vigie ${BuildConfig.VERSION_NAME}", "Code source et versions sur GitHub · licence GPL-3.0") {
                    openUrl(REPO_URL)
                }
                Text(
                    "Aucune donnée personnelle n'est collectée : la seule requête réseau télécharge les JSON publics du collecteur.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun BaseUrlField(s: Settings, viewModel: SettingsViewModel) {
    var url by rememberSaveable(s.baseUrl) { mutableStateOf(s.baseUrl) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it; error = null },
            label = { Text("Adresse des données publiées") },
            enabled = !s.demoMode,
            isError = error != null,
            supportingText = { Text(error ?: "Par défaut : ${Settings.DEFAULT_BASE_URL}") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        if (!s.demoMode) {
            Row {
                TextButton(onClick = { error = viewModel.setBaseUrl(url) }, enabled = url.trim() != s.baseUrl) {
                    Text("Enregistrer l'adresse")
                }
                if (s.baseUrl != Settings.DEFAULT_BASE_URL) {
                    TextButton(onClick = viewModel::resetBaseUrl) { Text("Adresse par défaut") }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String) {
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
private fun SwitchRow(title: String, text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LinkRow(title: String, text: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
