package com.shjamolov.mediastreamplayer.data.repository

import com.shjamolov.mediastreamplayer.domain.common.AppError
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.TorrServerEndpoint
import com.shjamolov.mediastreamplayer.domain.repository.TorrServerRepository
import com.shjamolov.mediastreamplayer.domain.repository.TorrServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class DefaultTorrServerRepository(private val client: OkHttpClient) : TorrServerRepository {
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
}
