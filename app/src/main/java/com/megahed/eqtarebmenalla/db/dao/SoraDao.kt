package com.megahed.eqtarebmenalla.db.dao

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.Sora
import kotlinx.coroutines.flow.Flow

@Dao
interface SoraDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSora(sora: Sora)

    @Update
    suspend fun updateSora(sora: Sora)

    @Delete
    suspend fun deleteSora(sora: Sora)


    @Query("SELECT * FROM sora ")
    fun getAllSora():Flow<List<Sora>>


    @Query("SELECT * FROM sora WHERE soraId =:id ")
    suspend fun getSoraById(id:Int): Sora?

}