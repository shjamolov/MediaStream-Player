package com.shjamolov.mediastreamplayer.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface BootstrapDao {
    @Upsert
    suspend fun upsert(record: BootstrapRecord)

    @Query("SELECT * FROM bootstrap_records WHERE id = 1")
    suspend fun get(): BootstrapRecord?
}

