package fr.tchyze.vigie.data

import fr.tchyze.vigie.data.model.AdvisoriesFile
import fr.tchyze.vigie.data.model.IndexFile
import fr.tchyze.vigie.data.model.ProductFile
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Lecture des JSON publiés, avec contrôle de `schema_version` avant tout décodage. */
object VigieJson {
    const val SUPPORTED_SCHEMA_VERSION = 1

    /** Un champ ajouté par le collecteur ne change pas la version : on l'ignore. */
    val json = Json { ignoreUnknownKeys = true }

    fun parseIndex(file: String, text: String): IndexFile = parse(file, text, IndexFile.serializer())

    fun parseProduct(file: String, text: String): ProductFile = parse(file, text, ProductFile.serializer())

    fun parseAdvisories(file: String, text: String): AdvisoriesFile =
        parse(file, text, AdvisoriesFile.serializer())

    private fun <T> parse(file: String, text: String, strategy: DeserializationStrategy<T>): T {
        val root = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (e: Exception) {
            throw DataException.InvalidData(file, e)
        } ?: throw DataException.InvalidData(file, detail = "objet JSON attendu")

        val version = root["schema_version"]?.let { runCatching { it.jsonPrimitive.intOrNull }.getOrNull() }
            ?: throw DataException.InvalidData(file, detail = "schema_version absent")
        if (version != SUPPORTED_SCHEMA_VERSION) throw DataException.UnsupportedSchema(file, version)

        return try {
            json.decodeFromJsonElement(strategy, root)
        } catch (e: Exception) {
            throw DataException.InvalidData(file, e, e.message?.lineSequence()?.firstOrNull())
        }
    }
}
