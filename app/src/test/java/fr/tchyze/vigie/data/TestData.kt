package fr.tchyze.vigie.data

import fr.tchyze.vigie.data.source.FileSource
import java.io.File

/** Les JSON d'exemple de l'application (le répertoire de travail des tests est le module `app`). */
val demoDir = File("src/main/assets/demo")

fun demoText(path: String): String = File(demoDir, path).readText()

/** Source en mémoire : les exemples, éventuellement modifiés fichier par fichier. */
class MapFileSource(private val files: Map<String, String>) : FileSource {
    override suspend fun read(path: String): String =
        files[path] ?: throw DataException.Network(path, "HTTP 404")

    companion object {
        fun demo(overrides: Map<String, String> = emptyMap()): MapFileSource {
            val all = demoDir.walkTopDown().filter { it.isFile }
                .associate { it.relativeTo(demoDir).invariantSeparatorsPath to it.readText() }
            return MapFileSource(all + overrides)
        }
    }
}
