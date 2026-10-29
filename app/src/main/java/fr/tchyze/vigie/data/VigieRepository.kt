package fr.tchyze.vigie.data

import fr.tchyze.vigie.data.cache.DataCache
import fr.tchyze.vigie.data.model.Snapshot
import fr.tchyze.vigie.data.settings.Settings
import fr.tchyze.vigie.data.settings.SettingsRepository
import fr.tchyze.vigie.data.source.FileSource
import fr.tchyze.vigie.data.source.SnapshotLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/** Ce que l'interface sait des données publiées à un instant donné. */
data class DataState(
    val snapshot: Snapshot? = null,
    /** Date du dernier téléchargement réussi (null en mode démo). */
    val fetchedAt: Instant? = null,
    val demo: Boolean = false,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: DataError? = null,
)

sealed interface DataError {
    val message: String

    /** Un fichier porte un `schema_version` inconnu : l'application doit être mise à jour. */
    data class UpdateRequired(val version: Int) : DataError {
        override val message = "Mise à jour de l'application nécessaire"
    }

    data class Network(override val message: String) : DataError

    data class Invalid(override val message: String) : DataError
}

/** Résultat d'une actualisation : les données avant et après, pour détecter les nouveautés. */
data class RefreshResult(val previous: Snapshot?, val current: Snapshot?, val error: DataError?)

/**
 * Point d'accès unique aux données publiées : démo embarquée, ou téléchargement avec cache local.
 */
class VigieRepository(
    private val settings: SettingsRepository,
    private val cache: DataCache,
    private val demoSource: FileSource,
    private val remoteSource: (baseUrl: String) -> FileSource,
    private val clock: () -> Instant = Instant::now,
) {
    private val _state = MutableStateFlow(DataState())
    val state: StateFlow<DataState> = _state.asStateFlow()

    private val mutex = Mutex()

    /** Recharge les données à chaque changement du mode démo ou de l'URL. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            settings.settings
                .map { it.demoMode to it.baseUrl }
                .distinctUntilChanged()
                .collect { (demo, _) ->
                    loadLocal(demo)
                    if (!demo && _state.value.snapshot == null) refresh()
                }
        }
    }

    /** Démo, ou dernier cache réseau. Ne fait aucune requête. */
    private suspend fun loadLocal(demo: Boolean) = mutex.withLock {
        _state.update { it.copy(loading = true) }
        if (demo) {
            val result = runCatching { SnapshotLoader(demoSource).load().snapshot }
            _state.value = DataState(
                snapshot = result.getOrNull(),
                demo = true,
                loading = false,
                error = result.exceptionOrNull()?.toDataError(),
            )
        } else {
            val cached = cache.read()
            _state.value = DataState(
                snapshot = cached?.loaded?.snapshot,
                fetchedAt = cached?.fetchedAt,
                demo = false,
                loading = false,
            )
        }
    }

    /**
     * Télécharge un jeu complet. En cas d'échec, les données précédentes restent affichées et
     * l'erreur est signalée. En mode démo, relit simplement les exemples.
     */
    suspend fun refresh(): RefreshResult = mutex.withLock {
        val current: Settings = settings.current()
        val previous = _state.value.snapshot
        _state.update { it.copy(refreshing = true) }

        val source = if (current.demoMode) demoSource else remoteSource(current.baseUrl)
        val error = try {
            val loaded = SnapshotLoader(source).load()
            if (current.demoMode) {
                _state.value = DataState(snapshot = loaded.snapshot, demo = true, loading = false)
            } else {
                val now = clock()
                cache.write(loaded, now)
                _state.value = DataState(snapshot = loaded.snapshot, fetchedAt = now, loading = false)
            }
            null
        } catch (e: CancellationException) {
            _state.update { it.copy(refreshing = false) }
            throw e
        } catch (e: Exception) {
            e.toDataError().also { err -> _state.update { it.copy(refreshing = false, loading = false, error = err) } }
        }
        RefreshResult(previous, _state.value.snapshot, error)
    }

    fun clearError() = _state.update { it.copy(error = null) }
}

fun Throwable.toDataError(): DataError = when (this) {
    is DataException.UnsupportedSchema -> DataError.UpdateRequired(version)
    is DataException.Network -> DataError.Network("Données injoignables ($message)")
    is DataException.InvalidData -> DataError.Invalid("Données reçues invalides ($message)")
    else -> DataError.Invalid("Erreur inattendue : ${this.message ?: this::class.simpleName}")
}
