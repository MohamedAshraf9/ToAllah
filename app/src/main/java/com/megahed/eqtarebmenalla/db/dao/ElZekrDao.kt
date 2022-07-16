package com.megahed.eqtarebmenalla.db.dao

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.ElZekr
import kotlinx.coroutines.flow.Flow

@Dao
interface ElZekrDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertElZekr(elZekr: ElZekr)

    @Update
    suspend fun updateElZekr(elZekr: ElZekr)

    @Delete
    suspend fun deleteElZekr(elZekr: ElZekr)


    @Query("SELECT * FROM elzekr WHERE catId =:id ")
    fun getElZekrOfCatId(id:Int): Flow<List<ElZekr>>



}