package com.shjamolov.mediastreamplayer.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    private val hasher = PinHasher(iterations = 1_000)

    @Test
    fun hash_verifiesCorrectPinAndRejectsWrongPin() {
        val salt = hasher.newSalt()
        val hash = hasher.hash("1234".toCharArray(), salt)
        assertTrue(hasher.verify("1234".toCharArray(), salt, hash))
        assertFalse(hasher.verify("4321".toCharArray(), salt, hash))
    }

    @Test
    fun hash_usesSalt() {
        val firstSalt = hasher.newSalt()
        val secondSalt = hasher.newSalt()
        val first = hasher.hash("1234".toCharArray(), firstSalt)
        val second = hasher.hash("1234".toCharArray(), secondSalt)
        assertFalse(first.contentEquals(second))
    }
}
