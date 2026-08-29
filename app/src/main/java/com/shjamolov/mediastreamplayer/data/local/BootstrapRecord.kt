package com.shjamolov.mediastreamplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bootstrap_records")
data class BootstrapRecord(
    @PrimaryKey val id: Int = 1,
    val schemaVersion: Int,
)

