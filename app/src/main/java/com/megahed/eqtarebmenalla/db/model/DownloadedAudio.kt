package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "downloaded_audio")
data class DownloadedAudio(
    @PrimaryKey val id: String,
    val readerId: String,
    val surahId: Int,
    val verseId: Int? = null,
    val surahName: String,
    val readerName: String,
    val localFilePath: String,
    val originalUrl: String,
    val downloadDate: Date,
    val fileSize: Long,
    val isComplete: Boolean = false,
    val downloadType: DownloadType = if (verseId != null) DownloadType.INDIVIDUAL_VERSE else DownloadType.FULL_SURAH
)
enum class DownloadType {
    FULL_SURAH,
    INDIVIDUAL_VERSE
}