package com.megahed.eqtarebmenalla.db.dao

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.Aya

@Dao
interface AyaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAya(aya: Aya)

    @Update
    suspend fun updateAya(aya: Aya)

    @Delete
    suspend fun deleteAya(aya: Aya)


    @Query("SELECT * FROM aya WHERE ayaId =:id ")
    suspend fun getAyaById(id:Int): Aya?

    @Query("SELECT * FROM aya WHERE soraId =:id ")
    suspend fun getAyaOfSoraId(id:Int): Aya?

}