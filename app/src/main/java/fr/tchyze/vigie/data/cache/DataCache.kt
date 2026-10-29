package fr.tchyze.vigie.data.cache

import fr.tchyze.vigie.data.source.DirFileSource
import fr.tchyze.vigie.data.source.LoadedSnapshot
import fr.tchyze.vigie.data.source.SnapshotLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

/** Les dernières données reçues et la date de leur téléchargement. */
class CachedSnapshot(val loaded: LoadedSnapshot, val fetchedAt: Instant)

/**
 * Copie locale des derniers JSON reçus, pour fonctionner hors ligne.
 *
 * Les fichiers sont écrits dans un répertoire temporaire puis échangés d'un bloc avec l'ancien :
 * le cache contient toujours un jeu complet et cohérent.
 */
class DataCache(private val root: File) {
    private val current = File(root, "current")
    private val incoming = File(root, "incoming")
    private val previous = File(root, "previous")

    suspend fun read(): CachedSnapshot? = withContext(Dispatchers.IO) {
        if (!current.isDirectory && previous.isDirectory) previous.renameTo(current)
        val stamp = File(current, FETCHED_AT).takeIf { it.isFile } ?: return@withContext null
        runCatching {
            CachedSnapshot(
                loaded = SnapshotLoader(DirFileSource(current)).load(),
                fetchedAt = Instant.parse(stamp.readText().trim()),
            )
        }.getOrNull()
    }

    suspend fun write(loaded: LoadedSnapshot, fetchedAt: Instant) = withContext(Dispatchers.IO) {
        root.mkdirs()
        incoming.deleteRecursively()
        loaded.rawFiles.forEach { (path, text) ->
            File(incoming, path).apply { parentFile?.mkdirs() }.writeText(text)
        }
        File(incoming, FETCHED_AT).writeText(fetchedAt.toString())

        previous.deleteRecursively()
        if (current.exists() && !current.renameTo(previous)) error("cache : échange impossible")
        if (!incoming.renameTo(current)) {
            previous.renameTo(current)
            error("cache : échange impossible")
        }
        previous.deleteRecursively()
    }

    private companion object {
        const val FETCHED_AT = "fetched_at.txt"
    }
}
