package com.shjamolov.mediastreamplayer.presentation.torrserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shjamolov.mediastreamplayer.core.settings.TorrServerSettingsStore
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.TorrServerEndpoint
import com.shjamolov.mediastreamplayer.domain.model.TorrServerMode
import com.shjamolov.mediastreamplayer.domain.repository.TorrServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TorrServerViewModel(
    private val repository: TorrServerRepository,
    private val store: TorrServerSettingsStore,
) : ViewModel() {
    private val saved = store.load()
    private val mutableState = MutableStateFlow(
        TorrServerUiState(saved.mode, saved.baseUrl, saved.username.orEmpty(), saved.password.orEmpty()),
    )
    val state: StateFlow<TorrServerUiState> = mutableState.asStateFlow()

    fun setMode(mode: TorrServerMode) = mutableState.update {
        val url = when (mode) {
            TorrServerMode.LOCAL_EXTERNAL -> "http://127.0.0.1:8090"
            TorrServerMode.REMOTE -> if (it.mode == TorrServerMode.REMOTE) it.url else "http://192.168.1.2:8090"
            TorrServerMode.LOCAL_MANAGED -> "http://127.0.0.1:8090"
        }
        it.copy(mode = mode, url = url, result = null)
    }
    fun setUrl(value: String) = mutableState.update { it.copy(url = value, result = null) }
    fun setUsername(value: String) = mutableState.update { it.copy(username = value, result = null) }
    fun setPassword(value: String) = mutableState.update { it.copy(password = value, result = null) }

    fun save() {
        val endpoint = validatedEndpoint() ?: return
        store.save(endpoint)
        mutableState.update { it.copy(url = endpoint.baseUrl, result = ConnectionResult.Saved) }
    }

    fun testAndSave() {
        val endpoint = validatedEndpoint() ?: return
        mutableState.update { it.copy(testing = true, result = null, url = endpoint.baseUrl) }
        viewModelScope.launch {
            when (val response = repository.testConnection(endpoint)) {
                is AppResult.Success -> {
                    store.save(endpoint)
                    mutableState.update { it.copy(testing = false, result = ConnectionResult.Connected(response.value.version, response.value.isMatrix)) }
                }
                is AppResult.Failure -> mutableState.update { it.copy(testing = false, result = ConnectionResult.Failed) }
            }
        }
    }

    private fun validatedEndpoint(): TorrServerEndpoint? {
        val current = mutableState.value
        val normalizedUrl = current.url.trim().let {
            if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it"
        }.trimEnd('/')
        if (current.username.isBlank() != current.password.isBlank()) {
            mutableState.update { it.copy(result = ConnectionResult.InvalidCredentials) }
            return null
        }
        return runCatching {
            TorrServerEndpoint(current.mode, normalizedUrl, current.username.ifBlank { null }, current.password.ifBlank { null })
        }.getOrElse {
            mutableState.update { state -> state.copy(result = ConnectionResult.InvalidUrl) }
            null
        }
    }
}

data class TorrServerUiState(
    val mode: TorrServerMode,
    val url: String,
    val username: String,
    val password: String,
    val testing: Boolean = false,
    val result: ConnectionResult? = null,
)

sealed interface ConnectionResult {
    data object Saved : ConnectionResult
    data class Connected(val version: String, val isMatrix: Boolean) : ConnectionResult
    data object Failed : ConnectionResult
    data object InvalidUrl : ConnectionResult
    data object InvalidCredentials : ConnectionResult
}
