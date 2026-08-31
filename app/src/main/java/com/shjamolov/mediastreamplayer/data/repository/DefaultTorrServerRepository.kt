package com.shjamolov.mediastreamplayer.data.repository

import com.shjamolov.mediastreamplayer.domain.common.AppError
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.TorrServerEndpoint
import com.shjamolov.mediastreamplayer.domain.model.TorrentContent
import com.shjamolov.mediastreamplayer.domain.model.TorrentPlaybackSource
import com.shjamolov.mediastreamplayer.domain.model.TorrentVideoFile
import com.shjamolov.mediastreamplayer.domain.model.TorrentSearchResult
import com.shjamolov.mediastreamplayer.domain.repository.TorrServerRepository
import com.shjamolov.mediastreamplayer.domain.repository.TorrServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException

class DefaultTorrServerRepository(
    private val client: OkHttpClient,
    private val json: Json,
) : TorrServerRepository {
    override suspend fun testConnection(endpoint: TorrServerEndpoint): AppResult<TorrServerStatus> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(endpoint.baseUrl.trimEnd('/') + "/echo").apply {
                    if (endpoint.username != null) header("Authorization", Credentials.basic(endpoint.username, checkNotNull(endpoint.password)))
                }.build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val message = if (response.code == 401) "TorrServer authentication failed" else "TorrServer HTTP ${response.code}"
                        return@withContext AppResult.Failure(AppError.Configuration(message))
                    }
                    val version = response.body?.string().orEmpty().trim()
                    if (version.isBlank()) return@withContext AppResult.Failure(AppError.Configuration("Empty TorrServer version"))
                    AppResult.Success(TorrServerStatus(version, version.contains("MatriX", ignoreCase = true)))
                }
            } catch (error: IllegalArgumentException) {
                AppResult.Failure(AppError.Configuration("Invalid TorrServer URL", error))
            } catch (error: IOException) {
                AppResult.Failure(AppError.Network(error))
            } catch (error: Exception) {
                AppResult.Failure(AppError.Unexpected(error))
            }
        }

    override suspend fun addTorrent(
        endpoint: TorrServerEndpoint,
        link: String,
        title: String,
        poster: String?,
    ): AppResult<TorrentContent> = withContext(Dispatchers.IO) {
        try {
            val added = post(endpoint, buildJsonObject {
                put("action", "add")
                put("link", link)
                put("title", title)
                poster?.let { put("poster", it) }
                put("save_to_db", true)
            })
            val hash = added.string("hash")
            if (hash.isBlank()) return@withContext AppResult.Failure(AppError.Configuration("TorrServer did not return torrent hash"))

            var status = added
            repeat(30) {
                status.toContent()?.takeIf { it.files.isNotEmpty() }?.let { return@withContext AppResult.Success(it) }
                delay(500)
                status = post(endpoint, buildJsonObject { put("action", "get"); put("hash", hash) })
            }
            AppResult.Failure(AppError.Configuration("Torrent metadata timeout"))
        } catch (error: HttpStatusException) {
            AppResult.Failure(AppError.Configuration("TorrServer HTTP ${error.code}", error))
        } catch (error: IllegalArgumentException) {
            AppResult.Failure(AppError.Configuration("Invalid torrent or TorrServer URL", error))
        } catch (error: IOException) {
            AppResult.Failure(AppError.Network(error))
        } catch (error: Exception) {
            AppResult.Failure(AppError.Unexpected(error))
        }
    }

    override fun playbackSource(
        endpoint: TorrServerEndpoint,
        content: TorrentContent,
        fileId: Int,
    ): TorrentPlaybackSource {
        val file = content.files.first { it.id == fileId }
        val headers = endpoint.username?.let {
            mapOf("Authorization" to Credentials.basic(it, checkNotNull(endpoint.password)))
        }.orEmpty()
        return TorrentPlaybackSource(
            title = file.path.substringAfterLast('/').substringAfterLast('\\'),
            url = "${endpoint.baseUrl.trimEnd('/')}/play/${content.hash}/${file.id}",
            requestHeaders = headers,
        )
    }

    override suspend fun search(
        endpoint: TorrServerEndpoint,
        query: String,
    ): AppResult<List<TorrentSearchResult>> = withContext(Dispatchers.IO) {
        try {
            val url = endpoint.baseUrl.trimEnd('/').toHttpUrl().newBuilder()
                .addPathSegment("search")
                .addQueryParameter("query", query)
                .build()
            val request = Request.Builder().url(url).apply {
                if (endpoint.username != null) header("Authorization", Credentials.basic(endpoint.username, checkNotNull(endpoint.password)))
            }.build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext AppResult.Failure(AppError.Configuration("TorrServer search HTTP ${response.code}"))
                val results = json.parseToJsonElement(response.body?.string().orEmpty()).jsonArray.mapNotNull { element ->
                    val item = element.jsonObject
                    val source = item.string("Magnet").ifBlank { item.string("Link") }
                    if (source.isBlank()) return@mapNotNull null
                    TorrentSearchResult(
                        title = item.string("Title").ifBlank { item.string("Name") },
                        source = item.string("Tracker"),
                        size = item.string("Size"),
                        seeders = item.string("Seed").toIntOrNull() ?: 0,
                        peers = item.string("Peer").toIntOrNull() ?: 0,
                        quality = item.string("VideoQuality").toIntOrNull()?.takeIf { it > 0 },
                        magnetOrLink = source,
                    )
                }.sortedWith(compareByDescending<TorrentSearchResult> { it.seeders }.thenByDescending { it.quality ?: 0 })
                AppResult.Success(results)
            }
        } catch (error: IOException) {
            AppResult.Failure(AppError.Network(error))
        } catch (error: Exception) {
            AppResult.Failure(AppError.Unexpected(error))
        }
    }

    private fun post(endpoint: TorrServerEndpoint, payload: JsonObject): JsonObject {
        val body = json.encodeToString(JsonObject.serializer(), payload)
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(endpoint.baseUrl.trimEnd('/') + "/torrents").post(body).apply {
            if (endpoint.username != null) header("Authorization", Credentials.basic(endpoint.username, checkNotNull(endpoint.password)))
        }.build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw HttpStatusException(response.code)
            json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
        }
    }

    private fun JsonObject.toContent(): TorrentContent? {
        val hash = string("hash").takeIf(String::isNotBlank) ?: return null
        val videoExtensions = setOf("mkv", "mp4", "avi", "mov", "m4v", "webm", "ts", "m2ts", "mpg", "mpeg")
        val files = this["file_stats"]?.jsonArray.orEmpty().mapNotNull { element ->
            val file = element.jsonObject
            val path = file.string("path")
            val extension = path.substringAfterLast('.', "").lowercase()
            if (path.isBlank() || extension !in videoExtensions) return@mapNotNull null
            TorrentVideoFile(
                id = file["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return@mapNotNull null,
                path = path,
                sizeBytes = file["length"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0,
            )
        }
        return TorrentContent(hash, string("title").ifBlank { string("name") }, files)
    }

    private fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
}

private class HttpStatusException(val code: Int) : IOException("HTTP $code")
