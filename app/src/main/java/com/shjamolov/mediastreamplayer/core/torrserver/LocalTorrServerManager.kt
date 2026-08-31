package com.shjamolov.mediastreamplayer.core.torrserver

import android.content.Context
import com.shjamolov.mediastreamplayer.domain.common.AppResult
import com.shjamolov.mediastreamplayer.domain.model.TorrServerEndpoint
import com.shjamolov.mediastreamplayer.domain.model.TorrServerMode
import com.shjamolov.mediastreamplayer.domain.repository.TorrServerRepository
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

class LocalTorrServerManager(
    context: Context,
    private val repository: TorrServerRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appContext = context.applicationContext
    private val serverFile = File(appContext.applicationInfo.nativeLibraryDir, SERVER_LIBRARY)
    private val configDir = File(appContext.filesDir, "torrserver-data")
    private val logFile = File(configDir, "torrserver.log")
    private var process: Process? = null
    private val mutableState = MutableStateFlow<LocalTorrServerState>(LocalTorrServerState.Stopped)
    val state: StateFlow<LocalTorrServerState> = mutableState.asStateFlow()

    suspend fun ensureRunning(): LocalTorrServerState = withContext(ioDispatcher) {
        if (isResponding()) return@withContext connectedState()
        runCatching {
            check(serverFile.exists() && serverFile.canExecute()) {
                "Встроенный TorrServer отсутствует или Android запретил его запуск"
            }
            mutableState.value = LocalTorrServerState.Starting
            startProcess()
            repeat(START_ATTEMPTS) {
                delay(START_RETRY_MS)
                if (isResponding()) return@withContext connectedState()
            }
            error("Локальный TorrServer не ответил после запуска")
        }.getOrElse { error ->
            LocalTorrServerState.Failed(error.message ?: "Не удалось запустить локальный TorrServer")
                .also { mutableState.value = it }
        }
    }

    fun stop() {
        process?.destroy()
        process = null
        mutableState.value = LocalTorrServerState.Stopped
    }

    private fun startProcess() {
        if (process?.let(::isRunning) == true) return
        configDir.mkdirs()
        val startedProcess = ProcessBuilder(
            serverFile.absolutePath,
            "-k",
            "--path", configDir.absolutePath,
            "--logpath", logFile.absolutePath,
            "--ip", "127.0.0.1",
            "--port", "8090",
        ).redirectErrorStream(true).start()
        process = startedProcess
        thread(name = "torrserver-log", isDaemon = true) {
            logFile.outputStream().bufferedWriter().use { writer ->
                startedProcess.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        writer.appendLine(line)
                        writer.flush()
                    }
                }
            }
        }
    }

    private fun isRunning(candidate: Process): Boolean = try {
        candidate.exitValue()
        false
    } catch (_: IllegalThreadStateException) {
        true
    }

    private suspend fun isResponding(): Boolean = repository.testConnection(LOCAL_ENDPOINT) is AppResult.Success

    private suspend fun connectedState(): LocalTorrServerState.Running {
        val result = repository.testConnection(LOCAL_ENDPOINT) as? AppResult.Success
        val version = result?.value?.version.orEmpty()
        return LocalTorrServerState.Running(version).also { mutableState.value = it }
    }

    companion object {
        val LOCAL_ENDPOINT = TorrServerEndpoint(TorrServerMode.LOCAL_MANAGED, "http://127.0.0.1:8090")
        private const val SERVER_LIBRARY = "libtorrserver.so"
        private const val START_ATTEMPTS = 15
        private const val START_RETRY_MS = 1_000L
    }
}

sealed interface LocalTorrServerState {
    data object Stopped : LocalTorrServerState
    data class Downloading(val percent: Int) : LocalTorrServerState
    data object Starting : LocalTorrServerState
    data class Running(val version: String) : LocalTorrServerState
    data class Failed(val message: String) : LocalTorrServerState
}
