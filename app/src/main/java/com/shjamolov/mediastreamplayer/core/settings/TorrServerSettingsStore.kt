package com.shjamolov.mediastreamplayer.core.settings

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.shjamolov.mediastreamplayer.core.security.AndroidCredentialCipher
import com.shjamolov.mediastreamplayer.core.security.EncryptedValue
import com.shjamolov.mediastreamplayer.domain.model.TorrServerEndpoint
import com.shjamolov.mediastreamplayer.domain.model.TorrServerMode

class TorrServerSettingsStore(
    context: Context,
    private val cipher: AndroidCredentialCipher,
) {
    private val preferences = context.getSharedPreferences("torrserver_settings", Context.MODE_PRIVATE)

    fun load(): TorrServerEndpoint {
        val storedMode = runCatching { TorrServerMode.valueOf(preferences.getString(KEY_MODE, null).orEmpty()) }
            .getOrDefault(TorrServerMode.LOCAL_MANAGED)
        val storedUrl = preferences.getString(KEY_URL, null)
        val mode = if (storedMode == TorrServerMode.LOCAL_EXTERNAL &&
            (storedUrl == null || storedUrl.trimEnd('/') == "http://127.0.0.1:8090")) {
            TorrServerMode.LOCAL_MANAGED
        } else storedMode
        val defaultUrl = if (mode == TorrServerMode.REMOTE) "http://192.168.1.2:8090" else "http://127.0.0.1:8090"
        val username = preferences.getString(KEY_USERNAME, null)?.takeIf(String::isNotBlank)
        val password = username?.let { readPassword() }
        return TorrServerEndpoint(mode, storedUrl ?: defaultUrl,
            username = username?.takeIf { password != null }, password = password)
    }

    fun save(endpoint: TorrServerEndpoint) {
        val encrypted = endpoint.password?.let(cipher::encrypt)
        preferences.edit {
            putString(KEY_MODE, endpoint.mode.name)
            putString(KEY_URL, endpoint.baseUrl)
            putString(KEY_USERNAME, endpoint.username)
            if (encrypted == null) {
                remove(KEY_PASSWORD_IV); remove(KEY_PASSWORD_DATA)
            } else {
                putString(KEY_PASSWORD_IV, Base64.encodeToString(encrypted.iv, Base64.NO_WRAP))
                putString(KEY_PASSWORD_DATA, Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP))
            }
        }
    }

    private fun readPassword(): String? = runCatching {
        val iv = Base64.decode(preferences.getString(KEY_PASSWORD_IV, null), Base64.NO_WRAP)
        val data = Base64.decode(preferences.getString(KEY_PASSWORD_DATA, null), Base64.NO_WRAP)
        cipher.decrypt(EncryptedValue(iv, data))
    }.getOrNull()

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_URL = "url"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD_IV = "password_iv"
        const val KEY_PASSWORD_DATA = "password_data"
    }
}
