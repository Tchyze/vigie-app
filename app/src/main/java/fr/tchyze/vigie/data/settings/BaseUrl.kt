package fr.tchyze.vigie.data.settings

/** Adresse de base des JSON publiés, toujours terminée par une barre oblique. */
fun normalizeBaseUrl(url: String): String = url.trim().trimEnd('/') + "/"

fun validateBaseUrl(url: String): String? {
    val u = url.trim()
    return when {
        u.isEmpty() -> "Adresse vide"
        !u.startsWith("https://") -> "L'adresse doit commencer par https://"
        u.removePrefix("https://").substringBefore('/').isBlank() -> "Adresse incomplète"
        u.any { it.isWhitespace() } -> "L'adresse ne doit pas contenir d'espace"
        else -> null
    }
}
