package com.shjamolov.mediastreamplayer.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shjamolov.mediastreamplayer.data.local.entity.FavoriteChannelEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun favoriteChannelDao_upsertsAndObservesNewestFirst() = runBlocking {
        val dao = database.favoriteChannelDao()
        dao.upsert(FavoriteChannelEntity("first.us", "First", null, 100))
        dao.upsert(FavoriteChannelEntity("second.us", "Second", null, 200))

        assertEquals(
            listOf("second.us", "first.us"),
            dao.observeAll().first().map(FavoriteChannelEntity::channelId),
        )
    }
}
