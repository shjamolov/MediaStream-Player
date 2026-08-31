package com.shjamolov.mediastreamplayer.data.repository

import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.TorrServerEndpoint
import com.shjamolov.mediastreamplayer.domain.model.TorrServerMode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultTorrServerRepositoryTest {
    @Test
    fun enableBuiltInSearch_preservesSettingsAndEnablesLocalIndex() = runTest {
        val requests = mutableListOf<String>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val requestBody = chain.request().body
            val body = okio.Buffer().also { requestBody?.writeTo(it) }.readUtf8()
            requests += body
            val responseBody = if (requests.size == 1) {
                """{"CacheSize":67108864,"ConnectionsLimit":25,"EnableRutorSearch":false}"""
            } else ""
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(responseBody.toResponseBody("application/json".toMediaType())).build()
        }.build()

        val repository = DefaultTorrServerRepository(client, Json { ignoreUnknownKeys = true })
        val result = repository.enableBuiltInSearch(
            TorrServerEndpoint(TorrServerMode.LOCAL_MANAGED, "http://127.0.0.1:8090"),
        ) as AppResult.Success

        assertTrue(result.value)
        assertTrue(requests[1].contains("\"EnableRutorSearch\":true"))
        assertTrue(requests[1].contains("\"CacheSize\":67108864"))
    }

    @Test
    fun testConnection_readsMatrixVersionAndSendsBasicAuth() = runTest {
        var authorization: String? = null
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            authorization = chain.request().header("Authorization")
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("TorrServer MatriX.141.1".toResponseBody("text/plain".toMediaType()))
                .build()
        }.build()
        val endpoint = TorrServerEndpoint(TorrServerMode.REMOTE, "http://192.168.1.2:8090", "user", "pass")
        val result = DefaultTorrServerRepository(client, Json { ignoreUnknownKeys = true }).testConnection(endpoint) as AppResult.Success
        assertEquals("TorrServer MatriX.141.1", result.value.version)
        assertTrue(result.value.isMatrix)
        assertEquals("Basic dXNlcjpwYXNz", authorization)
    }

    @Test
    fun addTorrent_returnsVideoFilesAndBuildsPlaybackUrl() = runTest {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(
                    """{"hash":"abc123","title":"Movie","file_stats":[{"id":1,"path":"Movie.mkv","length":1073741824},{"id":2,"path":"Movie.srt","length":1000}]}"""
                        .toResponseBody("application/json".toMediaType()),
                )
                .build()
        }.build()
        val repository = DefaultTorrServerRepository(client, Json { ignoreUnknownKeys = true })
        val endpoint = TorrServerEndpoint(TorrServerMode.REMOTE, "http://10.0.2.2:8090")
        val result = repository.addTorrent(endpoint, "magnet:?xt=urn:btih:test", "Movie", null) as AppResult.Success

        assertEquals(1, result.value.files.size)
        assertEquals("Movie.mkv", result.value.files.single().path)
        assertEquals("http://10.0.2.2:8090/play/abc123/1", repository.playbackSource(endpoint, result.value, 1).url)
    }

    @Test
    fun search_mapsAndSortsTorznabResultsBySeeders() = runTest {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(
                    """[{"Title":"1080p","Size":"8 GB","Tracker":"A","Seed":10,"Peer":2,"Magnet":"magnet:?a","VideoQuality":1080},{"Title":"4K","Size":"30 GB","Tracker":"B","Seed":50,"Peer":4,"Magnet":"magnet:?b","VideoQuality":2160}]"""
                        .toResponseBody("application/json".toMediaType()),
                ).build()
        }.build()
        val repository = DefaultTorrServerRepository(client, Json { ignoreUnknownKeys = true })
        val endpoint = TorrServerEndpoint(TorrServerMode.REMOTE, "http://10.0.2.2:8090")
        val result = repository.search(endpoint, "Movie 2026") as AppResult.Success

        assertEquals("4K", result.value.first().title)
        assertEquals(50, result.value.first().seeders)
        assertEquals("magnet:?b", result.value.first().magnetOrLink)
    }
}
