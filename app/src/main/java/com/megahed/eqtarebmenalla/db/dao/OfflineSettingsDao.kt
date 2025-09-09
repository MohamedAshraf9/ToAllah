package com.megahed.eqtarebmenalla.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.megahed.eqtarebmenalla.db.model.OfflineSettings

@Dao
interface OfflineSettingsDao {
    @Query("SELECT * FROM offline_settings WHERE id = 1")
    suspend fun getOfflineSettings(): OfflineSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineSettings(settings: OfflineSettings)

    @Query("UPDATE offline_settings SET isOfflineMemorizationEnabled = :enabled WHERE id = 1")
    suspend fun setOfflineMemorizationEnabled(enabled: Boolean)

    @Query("UPDATE offline_settings SET selectedOfflineReaderId = :readerId, selectedOfflineReaderName = :readerName WHERE id = 1")
    suspend fun setSelectedOfflineReader(readerId: String?, readerName: String?)
}