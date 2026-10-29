package fr.tchyze.vigie.data.source

import android.content.res.AssetManager
import fr.tchyze.vigie.data.DataException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Données de démonstration embarquées dans `assets/demo/`. */
class AssetFileSource(
    private val assets: AssetManager,
    private val root: String = "demo",
) : FileSource {
    override suspend fun read(path: String): String = withContext(Dispatchers.IO) {
        try {
            assets.open("$root/$path").bufferedReader().use { it.readText() }
        } catch (e: java.io.IOException) {
            throw DataException.InvalidData(path, e, "fichier de démonstration absent")
        }
    }
}
