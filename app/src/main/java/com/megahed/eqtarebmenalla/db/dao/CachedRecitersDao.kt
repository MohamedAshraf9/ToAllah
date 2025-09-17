package com.megahed.eqtarebmenalla.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.megahed.eqtarebmenalla.db.model.CachedReciter
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedRecitersDao {
    @Query("SELECT * FROM cached_reciters ORDER BY name")
    suspend fun getAllCachedReciters(): List<CachedReciter>

    @Query("SELECT * FROM cached_reciters ORDER BY name")
    fun getAllCachedRecitersFlow(): Flow<List<CachedReciter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReciters(reciters: List<CachedReciter>)

    @Query("DELETE FROM cached_reciters")
    suspend fun clearCache()
}