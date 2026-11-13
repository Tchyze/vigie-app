package fr.tchyze.vigie.watch

import fr.tchyze.vigie.data.FollowRepository
import fr.tchyze.vigie.data.InventoryRepository
import fr.tchyze.vigie.data.db.NotifiedDao
import fr.tchyze.vigie.data.model.Snapshot
import fr.tchyze.vigie.data.settings.SettingsRepository
import fr.tchyze.vigie.domain.Advisories
import fr.tchyze.vigie.domain.Watch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Compare les données à ce qui est déjà connu et notifie les nouveautés qui concernent
 * l'utilisateur. Appelé après chaque actualisation, en arrière-plan comme dans l'application.
 */
class Watcher(
    private val dao: NotifiedDao,
    private val follows: FollowRepository,
    private val inventory: InventoryRepository,
    private val settings: SettingsRepository,
    private val notifier: Notifier,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    suspend fun check(snapshot: Snapshot, demo: Boolean) = mutex.withLock {
        val devices = inventory.devices.first()
        val relevant = Advisories.relevantBranches(snapshot, follows.follows.first(), devices)
        // Passer de la démo aux vraies données (ou l'inverse) repart d'une nouvelle référence.
        val marker = if (demo) "mode:demo" else "mode:remote"
        val stored = dao.keys().toSet()
        val known = if (marker in stored) stored - marker else emptySet()

        val vendors = Advisories.relevantVendors(snapshot, relevant)
        val outcome = Watch.check(Watch.events(snapshot, devices), known, relevant, vendors)
        dao.replaceAll(outcome.known + marker, clock())
        if (outcome.notify.isNotEmpty() && settings.current().notifications) notifier.show(outcome.notify)
    }
}
