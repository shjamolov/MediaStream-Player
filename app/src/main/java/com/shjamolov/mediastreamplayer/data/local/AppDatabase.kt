package com.shjamolov.mediastreamplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BootstrapRecord::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bootstrapDao(): BootstrapDao
}

