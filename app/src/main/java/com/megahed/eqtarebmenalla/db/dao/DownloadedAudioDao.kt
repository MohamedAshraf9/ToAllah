package com.megahed.eqtarebmenalla.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.megahed.eqtarebmenalla.db.model.DownloadType
import com.megahed.eqtarebmenalla.db.model.DownloadedAudio

@Dao
interface DownloadedAudioDao {

    @Query("SELECT * FROM downloaded_audio WHERE readerId = :readerId AND surahId = :surahId AND verseId IS NULL")
    suspend fun getDownloadedSurahAudio(readerId: String, surahId: Int): DownloadedAudio?

    @Query("SELECT * FROM downloaded_audio WHERE readerId = :readerId AND surahId = :surahId AND verseId = :verseId")
    suspend fun getDownloadedVerseAudio(readerId: String, surahId: Int, verseId: Int): DownloadedAudio?

    @Query("SELECT * FROM downloaded_audio WHERE readerId = :readerId")
    suspend fun getDownloadedAudioByReader(readerId: String): List<DownloadedAudio>

    @Query("SELECT * FROM downloaded_audio WHERE isComplete = 1")
    suspend fun getAllDownloadedAudio(): List<DownloadedAudio>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedAudio(audio: DownloadedAudio)

    @Delete
    suspend fun deleteDownloadedAudio(audio: DownloadedAudio)

    @Query("DELETE FROM downloaded_audio WHERE readerId = :readerId")
    suspend fun deleteAllByReader(readerId: String)

    @Query("SELECT * FROM downloaded_audio WHERE readerId = :readerId AND surahId = :surahId AND (verseId = :verseId OR (:verseId IS NULL AND verseId IS NULL))")
    suspend fun getDownloadedAudio(
        readerId: String,
        surahId: Int,
        verseId: Int? = null,
    ): DownloadedAudio?

    @Query("SELECT * FROM downloaded_audio WHERE readerId = :readerId AND surahId = :surahId AND downloadType = :downloadType")
    suspend fun getDownloadedAudioByType(
        readerId: String,
        surahId: Int,
        downloadType: DownloadType,
    ): List<DownloadedAudio>

}