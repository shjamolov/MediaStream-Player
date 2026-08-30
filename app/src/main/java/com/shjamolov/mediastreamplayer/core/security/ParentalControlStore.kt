package com.shjamolov.mediastreamplayer.core.security

import android.content.Context
import android.util.Base64
import androidx.core.content.edit

class ParentalControlStore(context: Context) {
    private val preferences = context.getSharedPreferences("parental_control", Context.MODE_PRIVATE)

    val hasPin: Boolean get() = preferences.contains(KEY_HASH) && preferences.contains(KEY_SALT)
    val failedAttempts: Int get() = preferences.getInt(KEY_ATTEMPTS, 0)
    val lockedUntilEpochMillis: Long get() = preferences.getLong(KEY_LOCKED_UNTIL, 0L)

    fun credentials(): PinCredentials? {
        val salt = preferences.getString(KEY_SALT, null) ?: return null
        val hash = preferences.getString(KEY_HASH, null) ?: return null
        return PinCredentials(Base64.decode(salt, Base64.NO_WRAP), Base64.decode(hash, Base64.NO_WRAP))
    }

    fun savePin(salt: ByteArray, hash: ByteArray) {
        preferences.edit {
            putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            putInt(KEY_ATTEMPTS, 0)
            putLong(KEY_LOCKED_UNTIL, 0L)
        }
    }

    fun saveFailure(attempts: Int, lockedUntil: Long) {
        preferences.edit { putInt(KEY_ATTEMPTS, attempts); putLong(KEY_LOCKED_UNTIL, lockedUntil) }
    }

    fun clearFailures() {
        preferences.edit { putInt(KEY_ATTEMPTS, 0); putLong(KEY_LOCKED_UNTIL, 0L) }
    }

    private companion object {
        const val KEY_SALT = "pin_salt"
        const val KEY_HASH = "pin_hash"
        const val KEY_ATTEMPTS = "failed_attempts"
        const val KEY_LOCKED_UNTIL = "locked_until"
    }
}

data class PinCredentials(val salt: ByteArray, val hash: ByteArray)
