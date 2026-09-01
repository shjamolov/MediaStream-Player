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
import kotlinx.serialization.json.JsonPrimitive
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
    override suspend fun enableBuiltInSearch(endpoint: TorrServerEndpoint): AppResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val current = settingsRequest(endpoint, buildJsonObject { put("action", "get") })
            if (current["EnableRutorSearch"]?.jsonPrimitive?.contentOrNull.toBoolean()) {
                return@withContext AppResult.Success(false)
            }
            val enabled = JsonObject(current + ("EnableRutorSearch" to JsonPrimitive(true)))
            settingsRequest(endpoint, buildJsonObject {
                put("action", "set")
                put("sets", enabled)
            }, expectJson = false)
            AppResult.Success(true)
        } catch (error: IOException) {
            AppResult.Failure(AppError.Network(error))
        } catch (error: Exception) {
            AppResult.Failure(AppError.Unexpected(error))
        }
    }

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
        val torrServerResults = try {
            val url = endpoint.baseUrl.trimEnd('/').toHttpUrl().newBuilder()
                .addPathSegment("search")
                .addQueryParameter("query", query)
                .build()
            val request = Request.Builder().url(url).apply {
                if (endpoint.username != null) header("Authorization", Credentials.basic(endpoint.username, checkNotNull(endpoint.password)))
            }.build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) emptyList() else json.parseToJsonElement(response.body?.string().orEmpty()).jsonArray.mapNotNull { element ->
                    val item = element.jsonObject
                    val source = item.string("Magnet").ifBlank { item.string("Link") }
                    if (source.isBlank()) return@mapNotNull null
                    TorrentSearchResult(
                        title = item.string("Title").ifBlank { item.string("Name") },
                        source = item.string("Tracker"),
                        size = item.string("Size"),
                        seeders = item.string("Seed").toIntOrNull() ?: 0,
                        peers = item.string("Peer").toIntOrNull() ?: 0,
                        quality = normalizeVideoQuality(item.string("VideoQuality").toIntOrNull()),
                        magnetOrLink = source,
                        sizeBytes = parseSizeBytes(item.string("Size")),
                        audioCompatibility = audioCompatibility(item.string("Title").ifBlank { item.string("Name") }),
                    )
                }
            }
        } catch (_: Exception) { emptyList() }

        val archiveResults = searchOpenArchive(query)
        val merged = mergeSearchResults(torrServerResults + archiveResults)
        if (merged.isNotEmpty()) AppResult.Success(merged)
        else AppResult.Failure(AppError.Network(IOException("No embedded search source responded")))
    }

    private fun searchOpenArchive(query: String): List<TorrentSearchResult> = try {
        val searchExpression = "title:(\"${query.replace("\"", " ")}\") AND collection:(opensource_movies) AND mediatype:(movies)"
        val url = "https://archive.org/advancedsearch.php".toHttpUrl().newBuilder()
            .addQueryParameter("q", searchExpression)
            .addQueryParameter("fl[]", "identifier")
            .addQueryParameter("fl[]", "title")
            .addQueryParameter("fl[]", "downloads")
            .addQueryParameter("fl[]", "item_size")
            .addQueryParameter("rows", "20")
            .addQueryParameter("output", "json")
            .build()
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val docs = json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject["response"]
                ?.jsonObject?.get("docs")?.jsonArray.orEmpty()
            docs.mapNotNull { element ->
                val item = element.jsonObject
                val identifier = item.string("identifier")
                if (identifier.isBlank()) return@mapNotNull null
                val title = item.string("title").ifBlank { identifier }
                val sizeBytes = item.string("item_size").toLongOrNull()
                TorrentSearchResult(
                    title = title,
                    source = "Internet Archive • Open Movies",
                    size = sizeBytes?.let(::formatSearchSize).orEmpty(),
                    seeders = item.string("downloads").toIntOrNull() ?: 0,
                    peers = 0,
                    quality = inferQuality(title),
                    magnetOrLink = "https://archive.org/download/$identifier/${identifier}_archive.torrent",
                    sizeBytes = sizeBytes,
                    audioCompatibility = audioCompatibility(title),
                )
            }
        }
    } catch (_: Exception) { emptyList() }

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

    private fun settingsRequest(endpoint: TorrServerEndpoint, payload: JsonObject, expectJson: Boolean = true): JsonObject {
        val body = json.encodeToString(JsonObject.serializer(), payload)
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(endpoint.baseUrl.trimEnd('/') + "/settings").post(body).apply {
            if (endpoint.username != null) header("Authorization", Credentials.basic(endpoint.username, checkNotNull(endpoint.password)))
        }.build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw HttpStatusException(response.code)
            if (!expectJson) JsonObject(emptyMap())
            else json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
        }
    }

    private fun normalizeVideoQuality(value: Int?): Int? = when (value) {
        null, 0 -> null
        in 100..102 -> 720
        in 200..203 -> 1080
        in 300..308 -> 2160
        else -> value
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

internal fun mergeSearchResults(results: List<TorrentSearchResult>): List<TorrentSearchResult> =
    results.sortedWith(
        compareByDescending<TorrentSearchResult> { it.audioCompatibility }
            .thenByDescending { it.quality ?: 0 }
            .thenByDescending { it.seeders }
            .thenByDescending { it.sizeBytes ?: 0L },
    ).distinctBy { result ->
        Regex("(?i)btih:([a-z0-9]+)").find(result.magnetOrLink)?.groupValues?.get(1)
            ?: result.title.lowercase().replace(Regex("[^a-zа-я0-9]+"), "")
    }

private fun audioCompatibility(title: String): Int = when {
    title.contains("AAC", true) -> 4
    title.contains("EAC3", true) || title.contains("E-AC-3", true) -> 3
    title.contains("AC3", true) || title.contains("AC-3", true) -> 2
    title.contains("DTS", true) || title.contains("TRUEHD", true) -> 1
    else -> 2
}

private fun inferQuality(title: String): Int? = when {
    title.contains("2160", true) || title.contains("4K", true) -> 2160
    title.contains("1080", true) -> 1080
    title.contains("720", true) -> 720
    title.contains("480", true) -> 480
    else -> null
}

private fun parseSizeBytes(value: String): Long? {
    val match = Regex("([0-9]+(?:[.,][0-9]+)?)\\s*(TB|GB|MB|KB|B)", RegexOption.IGNORE_CASE).find(value) ?: return null
    val number = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    val multiplier = when (match.groupValues[2].uppercase()) {
        "TB" -> 1_099_511_627_776.0
        "GB" -> 1_073_741_824.0
        "MB" -> 1_048_576.0
        "KB" -> 1_024.0
        else -> 1.0
    }
    return (number * multiplier).toLong()
}

private fun formatSearchSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.0f MB".format(bytes / 1_048_576.0)
    else -> "${bytes / 1024} KB"
}
