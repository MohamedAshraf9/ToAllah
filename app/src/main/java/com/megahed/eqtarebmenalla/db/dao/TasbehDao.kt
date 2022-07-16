package com.megahed.eqtarebmenalla.db.dao

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import com.megahed.eqtarebmenalla.db.model.TasbehWithData
import kotlinx.coroutines.flow.Flow

@Dao
interface TasbehDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasbeh(tasbeh: Tasbeh)

    @Update
    suspend fun updateTasbeh(tasbeh: Tasbeh)

    @Delete
    suspend fun deleteTasbeh(tasbeh: Tasbeh)


    @Query("SELECT * FROM tasbeh")
    fun getAllTasbeh(): Flow<List<Tasbeh>>


    @Transaction
    @Query("SELECT * FROM tasbeh")
    fun getTasbehWithData(): Flow<List<TasbehWithData>>


}