package com.shjamolov.mediastreamplayer.data.repository

import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.TorrServerEndpoint
import com.shjamolov.mediastreamplayer.domain.model.TorrServerMode
import kotlinx.coroutines.test.runTest
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
        val result = DefaultTorrServerRepository(client).testConnection(endpoint) as AppResult.Success
        assertEquals("TorrServer MatriX.141.1", result.value.version)
        assertTrue(result.value.isMatrix)
        assertEquals("Basic dXNlcjpwYXNz", authorization)
    }
}
