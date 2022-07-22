package com.megahed.eqtarebmenalla.db.dao

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.Aya
import kotlinx.coroutines.flow.Flow

@Dao
interface AyaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAya(aya: Aya)

    @Update
    suspend fun updateAya(aya: Aya)

    @Delete
    suspend fun deleteAya(aya: Aya)


    @Query("SELECT * FROM aya WHERE ayaId =:id ")
    suspend fun getAyaById(id:Int): Aya?

    @Query("SELECT * FROM aya WHERE soraId =:id ")
     fun getAyaOfSoraId(id:Int): Flow<List<Aya>>

    @Query("SELECT * FROM aya WHERE isVaForte=1 ")
    fun getFavoriteAya(): Flow<List<Aya>>

}