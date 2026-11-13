package fr.tchyze.vigie

import android.app.Application
import fr.tchyze.vigie.data.FollowRepository
import fr.tchyze.vigie.data.InventoryRepository
import fr.tchyze.vigie.data.VigieRepository
import fr.tchyze.vigie.data.backup.BackupRepository
import fr.tchyze.vigie.data.cache.DataCache
import fr.tchyze.vigie.data.db.VigieDatabase
import fr.tchyze.vigie.data.settings.SettingsRepository
import fr.tchyze.vigie.data.source.AssetFileSource
import fr.tchyze.vigie.data.source.HttpFileSource
import fr.tchyze.vigie.watch.Notifier
import fr.tchyze.vigie.watch.RefreshWorker
import fr.tchyze.vigie.watch.Watcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

class VigieApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val c = AppContainer(this)
        container = c
        c.notifier.createChannel()
        c.repository.start(c.appScope)

        // Actualisation périodique, replanifiée quand la fréquence change.
        c.appScope.launch {
            c.settings.settings.map { it.refreshHours }.distinctUntilChanged().collect { hours ->
                RefreshWorker.schedule(this@VigieApp, hours)
            }
        }
        // Toute nouvelle donnée (actualisation manuelle comprise) passe par la détection des nouveautés.
        c.appScope.launch {
            c.repository.state
                .filter { it.snapshot != null && !it.loading }
                .map { it.snapshot!! to it.demo }
                .distinctUntilChanged()
                .collect { (snapshot, demo) -> runCatching { c.watcher.check(snapshot, demo) } }
        }
    }
}

/** Dépendances de l'application, créées une seule fois (pas de framework d'injection). */
class AppContainer(app: Application) {
    val appScope = CoroutineScope(SupervisorJob())

    val settings = SettingsRepository(app)

    val database = VigieDatabase.create(app)

    val follows = FollowRepository(database.followDao())

    val inventory = InventoryRepository(database.deviceDao())

    val backup = BackupRepository(database.deviceDao(), database.followDao())

    val notifier = Notifier(app)

    val watcher = Watcher(database.notifiedDao(), follows, inventory, settings, notifier)

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
