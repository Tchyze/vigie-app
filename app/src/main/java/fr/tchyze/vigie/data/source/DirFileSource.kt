package fr.tchyze.vigie.data.source

import fr.tchyze.vigie.data.DataException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Fichiers d'un répertoire local : le cache, ou les exemples dans les tests. */
class DirFileSource(private val dir: File) : FileSource {
    override suspend fun read(path: String): String = withContext(Dispatchers.IO) {
        val file = File(dir, path)
        if (!file.isFile) throw DataException.InvalidData(path, detail = "fichier absent")
        file.readText()
    }
}
