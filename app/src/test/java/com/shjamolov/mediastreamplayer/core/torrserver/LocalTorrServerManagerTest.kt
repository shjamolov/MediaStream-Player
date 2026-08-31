package com.shjamolov.mediastreamplayer.core.torrserver

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalTorrServerManagerTest {
    @Test
    fun mapsAndroidAbisToOfficialReleaseNames() {
        assertEquals("arm64", LocalTorrServerManager.androidArchitecture("arm64-v8a"))
        assertEquals("arm7", LocalTorrServerManager.androidArchitecture("armeabi-v7a"))
        assertEquals("amd64", LocalTorrServerManager.androidArchitecture("x86_64"))
        assertEquals("386", LocalTorrServerManager.androidArchitecture("x86"))
    }
}
