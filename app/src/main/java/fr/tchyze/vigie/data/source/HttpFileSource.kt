package fr.tchyze.vigie.data.source

import fr.tchyze.vigie.data.DataException
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/** Téléchargement des JSON publics : la seule requête réseau de l'application. */
class HttpFileSource(
    private val baseUrl: String,
    private val client: HttpClient,
) : FileSource {
    override suspend fun read(path: String): String {
        val url = baseUrl.trimEnd('/') + "/" + path
        val response = try {
            client.get(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw DataException.Network(path, "connexion impossible", e)
        }
        if (!response.status.isSuccess()) {
            throw DataException.Network(path, "HTTP ${response.status.value}")
        }
        return response.bodyAsText()
    }

    companion object {
        fun createClient(userAgent: String): HttpClient = HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 60_000
            }
            defaultRequest {
                headers.append(HttpHeaders.UserAgent, userAgent)
            }
        }
    }
}
