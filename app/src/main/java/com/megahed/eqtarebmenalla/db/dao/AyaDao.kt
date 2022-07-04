package com.megahed.eqtarebmenalla.db.dao

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.Sora
import kotlinx.coroutines.flow.Flow

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
     fun getAyaOfSoraId(id:Int): Flow<List<Aya>>

}