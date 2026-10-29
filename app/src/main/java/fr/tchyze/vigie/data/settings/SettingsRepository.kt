package fr.tchyze.vigie.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class Settings(
    val baseUrl: String = DEFAULT_BASE_URL,
    val refreshHours: Int = DEFAULT_REFRESH_HOURS,
    /** Activé par défaut tant que le collecteur n'est pas publié. */
    val demoMode: Boolean = true,
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://tchyze.github.io/vigie-collector/"
        const val DEFAULT_REFRESH_HOURS = 6
        val REFRESH_CHOICES = listOf(1, 3, 6, 12, 24)
    }
}

private val Context.dataStore by preferencesDataStore(name = "reglages")

class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): Settings = settings.first()

    suspend fun setBaseUrl(url: String) = context.dataStore.edit { it[BASE_URL] = url.trim() }

    suspend fun setRefreshHours(hours: Int) = context.dataStore.edit { it[REFRESH_HOURS] = hours }

    suspend fun setDemoMode(enabled: Boolean) = context.dataStore.edit { it[DEMO_MODE] = enabled }

    private fun Preferences.toSettings() = Settings(
        baseUrl = this[BASE_URL]?.takeIf { it.isNotBlank() } ?: Settings.DEFAULT_BASE_URL,
        refreshHours = this[REFRESH_HOURS] ?: Settings.DEFAULT_REFRESH_HOURS,
        demoMode = this[DEMO_MODE] ?: true,
    )

    private companion object {
        val BASE_URL = stringPreferencesKey("base_url")
        val REFRESH_HOURS = intPreferencesKey("refresh_hours")
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
    }
}
