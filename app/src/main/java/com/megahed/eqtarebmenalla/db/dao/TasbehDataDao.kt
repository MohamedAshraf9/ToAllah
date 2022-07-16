package com.megahed.eqtarebmenalla.db.dao

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.TasbehData
import kotlinx.coroutines.flow.Flow

@Dao
interface TasbehDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasbehData(tasbehData: TasbehData)

    @Update
    suspend fun updateTasbehData(tasbehData: TasbehData)

    @Delete
    suspend fun deleteTasbehData(tasbehData: TasbehData)


    @Query("SELECT * FROM tasbehdata")
    fun getAllTasbehData(): Flow<List<TasbehData>>
}