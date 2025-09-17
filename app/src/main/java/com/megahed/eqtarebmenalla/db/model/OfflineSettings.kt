package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_settings")
data class OfflineSettings(
    @PrimaryKey val id: Int = 1,
    val isOfflineMemorizationEnabled: Boolean = false,
    val selectedOfflineReaderId: String? = null,
    val selectedOfflineReaderName: String? = null,
    val totalDownloadedSize: Long = 0
)