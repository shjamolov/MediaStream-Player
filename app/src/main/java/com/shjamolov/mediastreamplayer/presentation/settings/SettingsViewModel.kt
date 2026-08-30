package com.shjamolov.mediastreamplayer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shjamolov.mediastreamplayer.BuildConfig
import com.shjamolov.mediastreamplayer.core.settings.AppSettingsStore
import com.shjamolov.mediastreamplayer.core.settings.CatalogLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class SettingsViewModel(
    private val settings: AppSettingsStore,
    private val httpClient: OkHttpClient,
) : ViewModel() {
    val language: StateFlow<CatalogLanguage> = settings.language
    private val mutableDiagnostics = MutableStateFlow<DiagnosticsState>(DiagnosticsState.Idle)
    val diagnostics: StateFlow<DiagnosticsState> = mutableDiagnostics.asStateFlow()

    fun setLanguage(language: CatalogLanguage) = settings.setLanguage(language)

    fun runDiagnostics() {
        if (mutableDiagnostics.value == DiagnosticsState.Running) return
        mutableDiagnostics.value = DiagnosticsState.Running
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                DiagnosticsResult(
                    tmdbTokenConfigured = BuildConfig.TMDB_API_TOKEN.isNotBlank(),
                    tmdbReachable = check("https://api.themoviedb.org/3/movie/popular?language=ru-RU&page=1", BuildConfig.TMDB_API_TOKEN),
                    iptvOrgReachable = check("https://iptv-org.github.io/api/channels.json"),
                )
            }
            mutableDiagnostics.value = DiagnosticsState.Complete(result)
        }
    }

    private fun check(url: String, bearerToken: String? = null): Boolean = runCatching {
        val builder = Request.Builder().url(url)
        if (!bearerToken.isNullOrBlank()) builder.header("Authorization", "Bearer $bearerToken")
        httpClient.newCall(builder.build()).execute().use { it.isSuccessful }
    }.getOrDefault(false)
}

sealed interface DiagnosticsState {
    data object Idle : DiagnosticsState
    data object Running : DiagnosticsState
    data class Complete(val result: DiagnosticsResult) : DiagnosticsState
}

data class DiagnosticsResult(
    val tmdbTokenConfigured: Boolean,
    val tmdbReachable: Boolean,
    val iptvOrgReachable: Boolean,
)
