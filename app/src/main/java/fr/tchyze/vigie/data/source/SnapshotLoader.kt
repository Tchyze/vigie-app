package fr.tchyze.vigie.data.source

import fr.tchyze.vigie.data.DataException
import fr.tchyze.vigie.data.VigieJson
import fr.tchyze.vigie.data.model.Snapshot

/** Un jeu de données lu et validé, avec le texte brut de chaque fichier pour le mettre en cache. */
class LoadedSnapshot(val snapshot: Snapshot, val rawFiles: Map<String, String>)

/**
 * Lit `index.json`, puis chaque fichier produit qu'il référence, puis `advisories.json`.
 * Tout est validé avant d'être renvoyé : un seul fichier en erreur fait échouer l'ensemble.
 */
class SnapshotLoader(private val source: FileSource) {

    suspend fun load(): LoadedSnapshot {
        val raw = linkedMapOf<String, String>()

        val indexText = source.read(INDEX)
        val index = VigieJson.parseIndex(INDEX, indexText)
        raw[INDEX] = indexText

        val products = index.products.map { entry ->
            val path = checkPath(entry.file, PRODUCT_PATH)
            val text = source.read(path)
            raw[path] = text
            VigieJson.parseProduct(path, text).also {
                if (it.product != entry.id) {
                    throw DataException.InvalidData(path, detail = "produit ${it.product} au lieu de ${entry.id}")
                }
            }
        }

        val advisoriesPath = checkPath(index.advisories.file, ADVISORIES_PATH)
        val advisoriesText = source.read(advisoriesPath)
        val advisories = VigieJson.parseAdvisories(advisoriesPath, advisoriesText)
        raw[advisoriesPath] = advisoriesText

        return LoadedSnapshot(Snapshot(index, products, advisories), raw)
    }

    /** Les chemins viennent du réseau et servent aussi de noms de fichiers en cache : on les borne. */
    private fun checkPath(path: String, pattern: Regex): String {
        if (!pattern.matches(path)) throw DataException.InvalidData(INDEX, detail = "chemin refusé : $path")
        return path
    }

    companion object {
        const val INDEX = "index.json"
        private val PRODUCT_PATH = Regex("^products/[a-z0-9][a-z0-9.-]*\\.json$")
        private val ADVISORIES_PATH = Regex("^advisories\\.json$")
    }
}
