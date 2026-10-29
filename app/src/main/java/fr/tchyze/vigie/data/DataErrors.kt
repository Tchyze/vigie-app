package fr.tchyze.vigie.data

/** Erreurs de lecture des données publiées. */
sealed class DataException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** Le fichier porte un `schema_version` que cette version de l'application ne sait pas lire. */
    class UnsupportedSchema(val file: String, val version: Int) :
        DataException("$file : schema_version $version non pris en charge")

    /** Le fichier est illisible ou ne respecte pas le format attendu. */
    class InvalidData(val file: String, cause: Throwable? = null, detail: String? = null) :
        DataException("$file : données invalides" + (detail?.let { " ($it)" } ?: ""), cause)

    /** Le fichier n'a pas pu être téléchargé. */
    class Network(val file: String, detail: String, cause: Throwable? = null) :
        DataException("$file : $detail", cause)
}
