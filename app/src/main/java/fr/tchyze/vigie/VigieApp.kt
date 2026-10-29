package fr.tchyze.vigie

import android.app.Application
import fr.tchyze.vigie.data.VigieRepository
import fr.tchyze.vigie.data.cache.DataCache
import fr.tchyze.vigie.data.settings.SettingsRepository
import fr.tchyze.vigie.data.source.AssetFileSource
import fr.tchyze.vigie.data.source.HttpFileSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File

class VigieApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.repository.start(container.appScope)
    }
}

/** Dépendances de l'application, créées une seule fois (pas de framework d'injection). */
class AppContainer(app: Application) {
    val appScope = CoroutineScope(SupervisorJob())

    val settings = SettingsRepository(app)

    private val httpClient by lazy {
        HttpFileSource.createClient("Vigie-Android/${BuildConfig.VERSION_NAME}")
    }

    val repository = VigieRepository(
        settings = settings,
        cache = DataCache(File(app.filesDir, "donnees")),
        demoSource = AssetFileSource(app.assets),
        remoteSource = { baseUrl -> HttpFileSource(baseUrl, httpClient) },
    )
}
