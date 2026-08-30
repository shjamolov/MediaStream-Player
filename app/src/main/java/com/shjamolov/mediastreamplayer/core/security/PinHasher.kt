package com.shjamolov.mediastreamplayer.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinHasher(private val iterations: Int = DEFAULT_ITERATIONS) {
    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)

    fun hash(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, iterations, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            pin.fill('\u0000')
        }
    }

    fun verify(pin: CharArray, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(hash(pin, salt), expected)

    companion object {
        const val DEFAULT_ITERATIONS = 600_000
        private const val SALT_BYTES = 16
        private const val HASH_BITS = 256
    }
}
