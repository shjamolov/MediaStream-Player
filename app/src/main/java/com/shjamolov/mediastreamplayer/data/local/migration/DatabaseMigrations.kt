package com.shjamolov.mediastreamplayer.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS bootstrap_records")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorite_media (
                tmdbId INTEGER NOT NULL,
                mediaType TEXT NOT NULL,
                title TEXT NOT NULL,
                posterPath TEXT,
                addedAtEpochMillis INTEGER NOT NULL,
                PRIMARY KEY(tmdbId, mediaType)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorite_channels (
                channelId TEXT NOT NULL,
                name TEXT NOT NULL,
                logoUrl TEXT,
                addedAtEpochMillis INTEGER NOT NULL,
                PRIMARY KEY(channelId)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS playback_history (
                contentKey TEXT NOT NULL,
                contentType TEXT NOT NULL,
                tmdbId INTEGER,
                channelId TEXT,
                seasonNumber INTEGER,
                episodeNumber INTEGER,
                title TEXT NOT NULL,
                posterPath TEXT,
                positionMillis INTEGER NOT NULL,
                durationMillis INTEGER,
                updatedAtEpochMillis INTEGER NOT NULL,
                PRIMARY KEY(contentKey)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_playback_history_updatedAtEpochMillis " +
                "ON playback_history(updatedAtEpochMillis)",
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `catalog_cache` (
                `tmdbId` INTEGER NOT NULL,
                `mediaType` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `originalTitle` TEXT,
                `overview` TEXT,
                `posterPath` TEXT,
                `backdropPath` TEXT,
                `releaseDate` TEXT,
                `voteAverage` REAL,
                `updatedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`tmdbId`, `mediaType`)
            )
            """.trimIndent(),
        )
    }
}
