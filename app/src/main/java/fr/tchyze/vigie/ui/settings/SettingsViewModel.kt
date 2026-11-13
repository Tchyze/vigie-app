package fr.tchyze.vigie.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.tchyze.vigie.AppContainer
import fr.tchyze.vigie.data.DataState
import fr.tchyze.vigie.data.backup.BackupException
import fr.tchyze.vigie.data.settings.Settings
import fr.tchyze.vigie.data.settings.normalizeBaseUrl
import fr.tchyze.vigie.data.settings.validateBaseUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(private val c: AppContainer) : ViewModel() {

    val settings: StateFlow<Settings?> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val data: StateFlow<DataState> = c.repository.state

    val deviceCount: StateFlow<Int> = c.inventory.devices.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Message ponctuel à afficher (résultat d'un export, d'un import…). */
    var message by mutableStateOf<String?>(null)
        private set

    fun consumeMessage() {
        message = null
    }

    fun canNotify(): Boolean = c.notifier.canNotify()

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch { c.settings.setDemoMode(enabled) }
    }

    /** @return un message d'erreur, ou null si l'adresse est acceptée. */
    fun setBaseUrl(url: String): String? {
        val error = validateBaseUrl(url)
        if (error == null) {
            viewModelScope.launch {
                c.settings.setBaseUrl(normalizeBaseUrl(url))
                if (!c.settings.current().demoMode) c.repository.refresh()
            }
        }
        return error
    }

    fun resetBaseUrl() {
        viewModelScope.launch { c.settings.setBaseUrl(Settings.DEFAULT_BASE_URL) }
    }

    fun setRefreshHours(hours: Int) {
        viewModelScope.launch { c.settings.setRefreshHours(hours) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { c.settings.setNotifications(enabled) }
    }

    fun refresh() {
        viewModelScope.launch {
            val result = c.repository.refresh()
            message = if (result.error == null) "Données actualisées" else result.error.message
        }
    }

    fun export(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            message = try {
                val text = c.backup.export()
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                        ?: error("fichier inaccessible")
                }
                "Inventaire exporté"
            } catch (e: Exception) {
                "Export impossible : ${e.message}"
            }
        }
    }

    fun import(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            message = try {
                val text = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("fichier inaccessible")
                }
                "Import terminé : " + c.backup.import(text).message
            } catch (e: BackupException) {
                e.message
            } catch (e: Exception) {
                "Import impossible : ${e.message}"
            }
        }
    }
}
