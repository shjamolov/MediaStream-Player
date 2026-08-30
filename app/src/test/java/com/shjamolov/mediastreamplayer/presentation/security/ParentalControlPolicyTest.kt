package com.shjamolov.mediastreamplayer.presentation.security

import org.junit.Assert.assertEquals
import org.junit.Test

class ParentalControlPolicyTest {
    @Test
    fun lockoutEscalatesAfterFiveFailures() {
        assertEquals(0L, lockDurationMillis(4))
        assertEquals(60_000L, lockDurationMillis(5))
        assertEquals(5 * 60_000L, lockDurationMillis(7))
        assertEquals(15 * 60_000L, lockDurationMillis(9))
    }
}
