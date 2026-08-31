package com.shjamolov.mediastreamplayer.core.torrserver

import android.content.Context
import android.os.Build
import androidx.core.content.edit
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread

class LocalTorrServerManager(
    context: Context,
    private val client: OkHttpClient,
    private val repository: TorrServerRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appContext = context.applicationContext
    private val serverFile = File(appContext.filesDir, SERVER_FILE)
    private val configDir = File(appContext.filesDir, "torrserver-data")
    private val logFile = File(configDir, "torrserver.log")
    private var process: Process? = null
    private val mutableState = MutableStateFlow<LocalTorrServerState>(LocalTorrServerState.Stopped)
    val state: StateFlow<LocalTorrServerState> = mutableState.asStateFlow()

    suspend fun ensureRunning(): LocalTorrServerState = withContext(ioDispatcher) {
        if (isResponding()) return@withContext connectedState()
        runCatching {
            if (!serverFile.exists() || serverFile.length() == 0L) installServer()
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

    private suspend fun installServer() {
        mutableState.value = LocalTorrServerState.Downloading(0)
        val metadataRequest = Request.Builder().url(RELEASE_METADATA_URL).build()
        val metadata = client.newCall(metadataRequest).execute().use { response ->
            check(response.isSuccessful) { "Не удалось проверить версию TorrServer: HTTP ${response.code}" }
            response.body?.string() ?: error("Сервер версий TorrServer вернул пустой ответ")
        }
        val root = Json.parseToJsonElement(metadata).jsonObject
        val version = root.getValue("version").jsonPrimitive.content
        val key = "android-${androidArchitecture()}"
        val url = root.getValue("links").jsonObject[key]?.jsonPrimitive?.content
            ?: error("TorrServer не поддерживает архитектуру ${Build.SUPPORTED_ABIS.firstOrNull()}")
        val temporary = File(appContext.cacheDir, "$SERVER_FILE.download")
        val downloadRequest = Request.Builder().url(url).build()
        client.newCall(downloadRequest).execute().use { response ->
            check(response.isSuccessful) { "Не удалось скачать TorrServer: HTTP ${response.code}" }
            val body = response.body ?: error("Сервер загрузки TorrServer вернул пустой ответ")
            val total = body.contentLength().coerceAtLeast(1L)
            body.byteStream().use { input ->
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        mutableState.value = LocalTorrServerState.Downloading((downloaded * 100 / total).toInt().coerceIn(0, 100))
                    }
                }
            }
        }
        check(temporary.length() > MIN_SERVER_BYTES) { "Получен повреждённый файл TorrServer" }
        if (serverFile.exists()) serverFile.delete()
        check(temporary.renameTo(serverFile)) { "Не удалось установить TorrServer" }
        check(serverFile.setExecutable(true, true)) { "Android запретил запуск TorrServer" }
        appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit {
            putString(KEY_INSTALLED_VERSION, version)
        }
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
        const val RELEASE_METADATA_URL = "https://releases.yourok.ru/torr/server_release.json"
        private const val SERVER_FILE = "torrserver"
        private const val PREFERENCES = "local_torrserver"
        private const val KEY_INSTALLED_VERSION = "installed_version"
        private const val MIN_SERVER_BYTES = 1_000_000L
        private const val START_ATTEMPTS = 15
        private const val START_RETRY_MS = 1_000L

        fun androidArchitecture(abi: String = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()): String = when (abi) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a" -> "arm7"
            "x86_64" -> "amd64"
            "x86" -> "386"
            else -> error("Неподдерживаемая архитектура: $abi")
        }
    }
}

sealed interface LocalTorrServerState {
    data object Stopped : LocalTorrServerState
    data class Downloading(val percent: Int) : LocalTorrServerState
    data object Starting : LocalTorrServerState
    data class Running(val version: String) : LocalTorrServerState
    data class Failed(val message: String) : LocalTorrServerState
}
