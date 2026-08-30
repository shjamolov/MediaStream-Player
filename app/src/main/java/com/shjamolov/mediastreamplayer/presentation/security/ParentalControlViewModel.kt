package com.shjamolov.mediastreamplayer.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shjamolov.mediastreamplayer.core.security.ParentalControlStore
import com.shjamolov.mediastreamplayer.core.security.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParentalControlViewModel(
    private val store: ParentalControlStore,
    private val hasher: PinHasher,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        ParentalControlState(
            hasPin = store.hasPin,
            unlocked = false,
            failedAttempts = store.failedAttempts,
            lockRemainingMillis = (store.lockedUntilEpochMillis - clock()).coerceAtLeast(0L),
            working = false,
            message = null,
            changingPin = false,
        ),
    )
    val state: StateFlow<ParentalControlState> = mutableState.asStateFlow()

    fun submit(pin: String, confirmation: String = "") {
        if (pin.length !in 4..8 || pin.any { !it.isDigit() }) {
            mutableState.value = currentState(message = PinMessage.INVALID_FORMAT)
            return
        }
        if ((!store.hasPin || mutableState.value.changingPin) && pin != confirmation) {
            mutableState.value = currentState(message = PinMessage.MISMATCH)
            return
        }
        if (store.lockedUntilEpochMillis > clock()) {
            mutableState.value = currentState(message = PinMessage.LOCKED)
            return
        }
        mutableState.value = currentState(working = true)
        viewModelScope.launch {
            if (!store.hasPin || mutableState.value.changingPin) createPin(pin) else verifyPin(pin)
        }
    }

    fun lock() { mutableState.value = currentState(unlocked = false) }
    fun startChangePin() { mutableState.value = currentState(changingPin = true, message = null) }

    private suspend fun createPin(pin: String) = withContext(Dispatchers.Default) {
        val salt = hasher.newSalt()
        val hash = hasher.hash(pin.toCharArray(), salt)
        store.savePin(salt, hash)
        mutableState.value = currentState(unlocked = true, changingPin = false, message = PinMessage.CREATED)
    }

    private suspend fun verifyPin(pin: String) = withContext(Dispatchers.Default) {
        val credentials = store.credentials() ?: return@withContext
        if (hasher.verify(pin.toCharArray(), credentials.salt, credentials.hash)) {
            store.clearFailures()
            mutableState.value = currentState(unlocked = true, message = PinMessage.UNLOCKED)
        } else {
            val attempts = store.failedAttempts + 1
            val lockDuration = lockDurationMillis(attempts)
            store.saveFailure(attempts, if (lockDuration == 0L) 0L else clock() + lockDuration)
            mutableState.value = currentState(message = if (lockDuration > 0) PinMessage.LOCKED else PinMessage.INCORRECT)
        }
    }

    private fun currentState(
        unlocked: Boolean = mutableState.value.unlocked,
        working: Boolean = false,
        message: PinMessage? = null,
        changingPin: Boolean = mutableState.value.changingPin,
    ) = ParentalControlState(store.hasPin, unlocked, store.failedAttempts,
        (store.lockedUntilEpochMillis - clock()).coerceAtLeast(0L), working, message, changingPin)

}

data class ParentalControlState(
    val hasPin: Boolean,
    val unlocked: Boolean,
    val failedAttempts: Int,
    val lockRemainingMillis: Long,
    val working: Boolean,
    val message: PinMessage?,
    val changingPin: Boolean,
)

enum class PinMessage { INVALID_FORMAT, MISMATCH, CREATED, UNLOCKED, INCORRECT, LOCKED }

internal fun lockDurationMillis(attempts: Int): Long = when {
    attempts < 5 -> 0L
    attempts < 7 -> 60_000L
    attempts < 9 -> 5 * 60_000L
    else -> 15 * 60_000L
}
