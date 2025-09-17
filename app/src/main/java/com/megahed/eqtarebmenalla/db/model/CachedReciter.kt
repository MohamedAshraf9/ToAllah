package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "cached_reciters")
data class CachedReciter(
    @PrimaryKey val id: Int,
    val name: String,
    val audio_url_bit_rate_32_: String,
    val audio_url_bit_rate_64: String,
    val audio_url_bit_rate_128: String,
    val cachedAt: Date = Date()
)