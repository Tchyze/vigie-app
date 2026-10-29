package fr.tchyze.vigie.data.source

/** Lit un fichier publié par son chemin relatif (`index.json`, `products/debian.json`…). */
fun interface FileSource {
    suspend fun read(path: String): String
}
